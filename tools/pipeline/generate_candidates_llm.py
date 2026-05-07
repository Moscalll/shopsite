#!/usr/bin/env python3
"""
读取 existing.json + config.yaml，调用本地 GGUF（llama-cpp-python）或 HTTP OpenAI 兼容接口，
按分类生成新品 product_spec 行（JSONL）。

用法:
  python generate_candidates_llm.py --config pipeline/config.yaml
  python generate_candidates_llm.py --config pipeline/config.yaml --out out/pipeline_run/product_spec.auto.jsonl
  python generate_candidates_llm.py --config pipeline/config.yaml --max-products 5

--max-products N: 最多写入 N 条商品（按 allowed_category_ids 顺序 × items_per_category 循环，达到 N 即停）。

前置: 先运行 fetch_existing.py
"""

from __future__ import annotations

import argparse
import json
import random
import re
import sys
import uuid
from pathlib import Path
from typing import Any

import yaml

from db_env import load_dotenv

ROOT = Path(__file__).resolve().parents[1]


def _extract_json_object(text: str) -> dict[str, Any]:
    text = text.strip()
    if text.startswith("```"):
        text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.IGNORECASE)
        text = re.sub(r"\s*```\s*$", "", text)
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass
    m = re.search(r"\{[\s\S]*\}", text)
    if not m:
        raise ValueError(f"No JSON object found in model output: {text[:500]}...")
    return json.loads(m.group(0))


def _norm_name(s: str) -> str:
    return re.sub(r"\s+", "", (s or "").lower())


def _clamp_price(v: Any, pmin: float, pmax: float) -> float:
    try:
        x = float(v)
    except (TypeError, ValueError):
        x = random.uniform(pmin, pmax)
    return round(max(pmin, min(pmax, x)), 2)


def _category_looks_like_apparel(cat_name: str) -> bool:
    """类目名是否明显为服饰鞋包（用于放宽/收紧「禁止总出 T 恤」规则）。"""
    s = (cat_name or "").strip().lower()
    if not s:
        return False
    keys = (
        "服饰",
        "服装",
        "男装",
        "女装",
        "童装",
        "内衣",
        "女鞋",
        "男鞋",
        "童鞋",
        "靴子",
        "皮鞋",
        "袜子",
        "帽子",
        "t恤",
        "卫衣",
        "外套",
        "裤子",
        "长裤",
        "短裤",
        "牛仔裤",
        "连衣裙",
        "半身裙",
        "短裙",
        "长裙",
        "箱包",
        "包袋",
        "运动服",
        "上装",
        "下装",
    )
    return any(k.lower() in s for k in keys)


def _diversity_nudge(cat_id: int, idx: int) -> str:
    """按类目循环位次轮换提示，减少模型总套「纯棉 T 恤」模板。"""
    nudges = [
        "优先选功能型小件、结构清晰、适合白底台面的实体商品（如厨房工具、收纳、文具、清洁小物）。",
        "优先选数码周边、线缆整理、支架、桌面小电器配件等。",
        "优先选家居装饰、香薰蜡烛、相框、绿植盆栽容器、灯具小配件等。",
        "优先选宠物用品、美容个护小工具、运动配件（非整套服装）等。",
        "优先选餐厨器皿、杯壶、餐垫、密封罐等（非服装）。",
    ]
    return nudges[(cat_id * 17 + idx * 3) % len(nudges)]


def _call_http(cfg: dict, messages: list[dict[str, str]]) -> str:
    import httpx

    llm = cfg["llm"]
    base = llm["base_url"].rstrip("/")
    url = f"{base}/chat/completions"
    body = {
        "model": llm.get("remote_model", "gpt-3.5-turbo"),
        "messages": messages,
        "temperature": float(llm.get("temperature", 0.65)),
        "max_tokens": int(llm.get("max_tokens", 900)),
    }
    headers = {"Content-Type": "application/json"}
    key = llm.get("api_key")
    if key is not None and str(key).strip() != "":
        headers["Authorization"] = f"Bearer {key}"
    with httpx.Client(timeout=600.0) as client:
        r = client.post(url, json=body, headers=headers)
        r.raise_for_status()
        data = r.json()
    return data["choices"][0]["message"]["content"]


def _call_gguf(llm: Any, messages: list[dict[str, str]], cfg: dict) -> str:
    lc = cfg["llm"]
    out = llm.create_chat_completion(
        messages=messages,
        temperature=float(lc.get("temperature", 0.65)),
        max_tokens=int(lc.get("max_tokens", 900)),
    )
    return out["choices"][0]["message"]["content"]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=ROOT / "pipeline" / "config.yaml")
    parser.add_argument("--out", type=Path, default=None, help="Output JSONL path")
    parser.add_argument("--dotenv", type=Path, default=ROOT / ".env")
    parser.add_argument("--dry-run", action="store_true", help="Print prompts only, no LLM")
    parser.add_argument(
        "--max-products",
        type=int,
        default=None,
        metavar="N",
        help="Stop after writing N lines (hard cap; order = config allowed_category_ids × items_per_category).",
    )
    args = parser.parse_args()

    if args.max_products is not None and args.max_products < 1:
        print("--max-products must be >= 1", file=sys.stderr)
        return 2

    load_dotenv(args.dotenv)

    if not args.config.is_file():
        print(f"Missing {args.config}", file=sys.stderr)
        return 2

    cfg = yaml.safe_load(args.config.read_text(encoding="utf-8"))
    out_dir = ROOT / cfg.get("paths", {}).get("out_dir", "out/pipeline_run")
    out_dir.mkdir(parents=True, exist_ok=True)

    existing_path = out_dir / "existing.json"
    if not existing_path.is_file():
        print(f"Run fetch_existing.py first. Missing {existing_path}", file=sys.stderr)
        return 2

    existing = json.loads(existing_path.read_text(encoding="utf-8"))
    forbidden = {_norm_name(n) for n in existing.get("product_names", [])}
    categories: dict[int, str] = {}
    for k, v in existing.get("categories", {}).items():
        categories[int(k)] = str(v)

    merchant_id = int(cfg["merchant_id"])
    allowed = [int(x) for x in cfg["allowed_category_ids"]]
    ipc = int(cfg.get("items_per_category", 1))
    pmin = float(cfg["price"]["min"])
    pmax = float(cfg["price"]["max"])
    stock_def = int(cfg["stock_default"])
    defaults = cfg.get("defaults", {})
    style_preset = defaults.get("stylePreset", "english_minimal_wood_table_3x4")

    out_path = args.out or (out_dir / "product_spec.auto.jsonl")

    backend = cfg["llm"]["backend"].lower()
    if backend == "http" and not cfg["llm"].get("base_url"):
        print("config llm.base_url is required when backend=http", file=sys.stderr)
        return 2

    llm_model = None
    if backend == "gguf":
        try:
            from llama_cpp import Llama
        except ImportError:
            print(
                "Install: pip install llama-cpp-python",
                file=sys.stderr,
            )
            return 2
        mp = cfg["llm"]["model_path"]
        if not Path(mp).is_file():
            print(f"GGUF not found: {mp}", file=sys.stderr)
            return 2
        llm_model = Llama(
            model_path=mp,
            n_ctx=int(cfg["llm"].get("n_ctx", 8192)),
            n_gpu_layers=int(cfg["llm"].get("n_gpu_layers", 0)),
            verbose=False,
        )
    elif backend != "http":
        print(f"Unknown llm.backend: {backend}", file=sys.stderr)
        return 2

    system_prompt = """你是电商运营助手。用户会给你分类与禁忌品名，你必须只输出一个合法的 JSON 对象（不要 markdown，不要解释）。
字段要求：
- id: 字符串，唯一
- categoryId: 整数（用户给定）
- merchantId: 整数（用户给定）
- name: 中文短标题，不含违禁词，不要与禁忌列表重复或极度相似
- description: 中文 1~3 句卖点
- price: 数字，必须在用户给定区间内
- stock: 整数
- fullPromptEn: 英文一整段，电商主图 prompt。必须以 "Straight-on view product photography of" 开头；包含具体商品英文描述；包含场景：白色木质台面或地板（按商品类型）；背景 pale grayish green；minimal clean neutral；commercial product photo；明确写 no text, no watermark, no logo。不要包含 --ar 或 --v 等多余参数。
- stylePreset: 固定为 english_minimal_wood_table_3x4
- appendSdSafetySuffix: false

类目与多样性（必须遵守）：
- name、description、fullPromptEn 描述的主体商品必须与 user 消息里的 categoryName 类目一致，不得跨类偷换成其他货架「万能款」。
- 若 user 标明「本类目非服饰鞋包」，则禁止以 T恤、卫衣、衬衫、裤子、裙子、连衣裙、内衣、袜子、鞋、帽、双肩包等穿戴/出行为唯一主体；禁止用「纯棉」「圆领短袖」「基础款打底」等话术作为卖点主轴；英文 fullPromptEn 也不得只描述 generic cotton tee。
- 若 user 标明「本类目为服饰鞋包」，则商品须贴合该类目，但避免每轮都输出雷同的「纯白纯棉基础款 T 恤」，应更具体（款式、材质组合、场景差异等）。
"""

    lines_out: list[str] = []
    for cat_id in allowed:
        cat_name = categories.get(cat_id, str(cat_id))
        for idx in range(ipc):
            forbid_sample = list(existing.get("product_names", []))[:80]
            forbid_hint = ", ".join(forbid_sample[:40]) if forbid_sample else "(无)"

            short_uid = uuid.uuid4().hex[:8]
            suggested_id = f"auto-{cat_id}-{short_uid}"
            apparel = _category_looks_like_apparel(cat_name)
            nudge = _diversity_nudge(cat_id, idx)
            if apparel:
                cat_rule = (
                    "【类目类型】本类目为服饰/鞋包/配件相关。请生成符合该类目的商品；"
                    "中文名与英文主体必须与该类目相符；"
                    "不要与禁忌列表同名或仅改一两个字的变体；"
                    "避免连续多轮都输出「纯白圆领纯棉短袖 T 恤」这类完全同质的基础款，应更具体（例如明确版型、图案、材质组合或配件形态）。"
                )
            else:
                cat_rule = (
                    "【类目类型】本类目非服饰鞋包专营。"
                    "请先在心里列举该「类目」下常见的 3 种不同实体商品（不要服装），再任选其中一种生成；"
                    "禁止把 T恤、卫衣、衬衫、裤子、裙子、内衣、袜子、鞋、帽、包袋作为商品主体；"
                    "禁止用「纯棉」「短袖」「基础款」作为唯一卖点主轴。"
                    f"多样性提示：{nudge}"
                )

            user_prompt = f"""merchantId={merchant_id}, categoryId={cat_id}, categoryName={cat_name}
price between {pmin} and {pmax}, stock={stock_def}
Forbidden existing names (do not copy or trivially vary): {forbid_hint}
Suggested id: {suggested_id}
request_uid={suggested_id}-{idx}

{cat_rule}

请为该类目原创一个全新的实体商品（中文名必须明显区别于禁忌列表；英文 fullPromptEn 的主体必须与中文商品一致）。输出单个 JSON 对象。"""

            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ]

            if args.dry_run:
                print("--- dry-run ---")
                print(user_prompt[:800])
                continue

            raw = ""
            try:
                if backend == "http":
                    raw = _call_http(cfg, messages)
                else:
                    raw = _call_gguf(llm_model, messages, cfg)
                obj = _extract_json_object(raw)
            except Exception as e:
                print(f"LLM failed category {cat_id} idx {idx}: {e}", file=sys.stderr)
                if raw:
                    print(f"Raw: {raw[:1200]}", file=sys.stderr)
                return 1

            obj["merchantId"] = merchant_id
            obj["categoryId"] = cat_id
            obj["stylePreset"] = style_preset
            obj["appendSdSafetySuffix"] = False
            obj["price"] = _clamp_price(obj.get("price"), pmin, pmax)
            obj["stock"] = int(obj.get("stock", stock_def))
            if "id" not in obj or not str(obj["id"]).strip():
                obj["id"] = suggested_id

            nm = str(obj.get("name", "")).strip()
            if not nm or _norm_name(nm) in forbidden:
                print(f"Reject duplicate or empty name at category {cat_id}: {nm!r}", file=sys.stderr)
                return 1
            forbidden.add(_norm_name(nm))

            if defaults.get("outputFormat"):
                obj["outputFormat"] = defaults["outputFormat"]

            lines_out.append(json.dumps(obj, ensure_ascii=False))
            if args.max_products is not None and len(lines_out) >= args.max_products:
                break
        if args.max_products is not None and len(lines_out) >= args.max_products:
            break

    if args.dry_run:
        print("Dry-run done.")
        return 0

    out_path.write_text("\n".join(lines_out) + ("\n" if lines_out else ""), encoding="utf-8")
    print(f"Wrote {len(lines_out)} lines -> {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
