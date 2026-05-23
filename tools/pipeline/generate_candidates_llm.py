#!/usr/bin/env python3
"""
读取 existing.json + config.yaml，调用本地 GGUF（llama-cpp-python）或 HTTP OpenAI 兼容接口，
按分类生成新品 product_spec 行（JSONL）。

用法:
  python generate_candidates_llm.py --config pipeline/config.yaml
  python generate_candidates_llm.py --config pipeline/config.yaml --out out/pipeline_run/product_spec.auto.jsonl
  python generate_candidates_llm.py --config pipeline/config.yaml --max-products 5
  python generate_candidates_llm.py --config pipeline/config.yaml --items-per-category 2
  python generate_candidates_llm.py --config pipeline/config.yaml --items-per-category-each 2,2,1,3,1,1,1,2
  python generate_candidates_llm.py --config pipeline/config.yaml --items-per-category-by-id "6=2,7=3"

--max-products N: 可选硬上限；省略时条数由 config 或下面的命令行覆盖决定。

--items-per-category / --items-per-category-each / --items-per-category-by-id:
  可选，覆盖 config.yaml 对应项；三者互斥。

前置: 先运行 fetch_existing.py
"""

from __future__ import annotations

import argparse
import json
import random
import re
import sys
import time
import uuid
from pathlib import Path
from typing import Any

import yaml

from db_env import load_dotenv

ROOT = Path(__file__).resolve().parents[1]
# region dbg 5578c5
_DBG_5578C5_PATH = ROOT.parent / "debug-5578c5.log"


def _dbg_5578c5(
    hypothesis_id: str,
    location: str,
    message: str,
    data: dict[str, Any],
    run_id: str = "pre",
) -> None:
    try:
        payload = {
            "sessionId": "5578c5",
            "runId": run_id,
            "hypothesisId": hypothesis_id,
            "location": location,
            "message": message,
            "data": data,
            "timestamp": int(time.time() * 1000),
        }
        with _DBG_5578C5_PATH.open("a", encoding="utf-8") as _df:
            _df.write(json.dumps(payload, ensure_ascii=False) + "\n")
    except OSError:
        pass


# endregion
# region agent log
_DEBUG_LOG_PATH = ROOT.parent / "debug-2b0c01.log"


def _agent_dbg(hypothesis_id: str, location: str, message: str, data: dict[str, Any]) -> None:
    try:
        payload = {
            "sessionId": "2b0c01",
            "hypothesisId": hypothesis_id,
            "location": location,
            "message": message,
            "data": data,
            "timestamp": int(time.time() * 1000),
        }
        with _DEBUG_LOG_PATH.open("a", encoding="utf-8") as _lf:
            _lf.write(json.dumps(payload, ensure_ascii=False) + "\n")
    except OSError:
        pass


# endregion


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


def _cjk_char_count(s: str) -> int:
    return len(re.findall(r"[\u4e00-\u9fff]", s or ""))


def _name_format_error(name: str) -> str | None:
    """
    商品名应为一段式短标题；模型常把卖点副标题写进 name（如「马甲 | 轻薄透气…」），在此拦下。
    """
    nm = (name or "").strip()
    if not nm:
        return "商品名为空"
    if "\n" in nm or "\r" in nm:
        return "商品名不得换行"
    if "|" in nm or "｜" in nm:
        return "商品名禁止竖线「|」及副标题式拼接，卖点请只写在 description"
    if "：" in nm or ":" in nm:
        return "商品名禁止冒号副标题，说明请只写在 description"
    # 逗号堆叠多为把多句卖点塞进标题
    if nm.count("，") >= 2 or nm.count(",") >= 2:
        return "商品名禁止多个逗号串联卖点，请改为 4~14 字左右的单段短名"
    cjk = _cjk_char_count(nm)
    if cjk > 14:
        return f"商品名过长（当前汉字约 {cjk} 个）；请压缩为一段式短标题（不超过 14 个汉字）"
    if len(nm) > 32:
        return "商品名总字符过长；请缩短为短标题"
    return None


def _batch_bamboo_count(bundles: list[tuple[int, str]]) -> int:
    """已接受条目中，名称+描述里含「竹」的条数（用于抑制跨类目扎堆竹材/竹纹）。"""
    n = 0
    for _, text in bundles:
        if "竹" in (text or ""):
            n += 1
    return n


def _non_apparel_material_kernel(cat_id: int, idx: int, attempt: int) -> str:
    """
    按槽位轮换「主材质/形态」提示，引导模型不要总套竹纹/竹纤维。
    刻意不包含「竹」字，避免与英文 wood table 场景叠加强化竹意象。
    """
    kernels = (
        "不锈钢拉丝或磨砂铝合金小件",
        "哑光陶瓷或搪瓷釉面",
        "高硼硅玻璃或透明 PC",
        "ABS / PP 注塑件（纯色或双色拼接）",
        "亚麻、帆布或涤棉混纺布艺",
        "榉木、橡木等实木纹理（非竹材）",
        "亚克力高透或雾面磨砂",
        "软木、再生纸浆或瓦楞纸结构",
        "尼龙编织、牛津布或 Cordura",
        "食品级硅胶一体成型（素色几何纹理即可）",
        "铸铁、碳钢或不粘涂层金属",
        "密胺仿瓷或三聚氰胺贴面",
        "锌合金、黄铜小五金件",
        "EVA、TPE 发泡或橡胶防滑底",
        "树脂仿石材或水磨石颗粒感",
        "PU / 超纤皮革压纹",
        "PET、RPET 透明或半透明注塑",
        "珊瑚绒、超细纤维或雪尼尔",
        "小麦秸秆降解料或咖啡渣复合材",
        "蜂窝铝夹层或碳纤维纹理贴片",
        "磨砂钢化玻璃面板",
        "电镀亮铬或枪灰金属",
        "釉面裂纹陶瓷",
        "拉丝钛色 PVD 金属",
        "双层真空不锈钢（杯壶类）",
    )
    slot = (cat_id * 23 + idx * 5 + attempt * 11) % len(kernels)
    return kernels[slot]


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


def _diversity_nudge(cat_id: int, idx: int, attempt: int) -> str:
    """按类目 + 位次 + 重试轮次轮换提示，减少模型总套同一类实体。"""
    nudges = [
        "优先选功能型小件、结构清晰、适合白底台面的实体商品（如厨房工具、收纳、文具、清洁小物）。",
        "优先选数码周边、线缆整理、支架、桌面小电器配件等。",
        "优先选家居装饰、香薰蜡烛、相框、绿植盆栽容器、灯具小配件等。",
        "优先选宠物用品、美容个护小工具、运动配件（非整套服装）等。",
        "优先选餐厨器皿、杯壶、餐垫、密封罐等（非服装）。",
        "优先选工具五金、测量、黏合修补、园艺手工具等（非服装）。",
        "优先选母婴耐用品、学习桌配件、安全收纳盒等（非服装）。",
        "优先选车载小物、便携电源配件、旅行收纳分装瓶等。",
        "优先选健康监测小设备、按摩放松小器具、口腔护理工具等。",
        "优先选咖啡茶周边、烘焙度量、吧台展示架等（非服装）。",
    ]
    slot = (cat_id * 17 + idx * 3 + attempt * 11) % len(nudges)
    return nudges[slot]


def _lighting_texture_hint(cat_id: int, idx: int, attempt: int) -> str:
    """英文 prompt 末段可并入的质感/光线差异（开头仍须 Straight-on…）。"""
    hints = [
        "Soft diffused key light from camera-left, gentle fill, crisp edge definition; subtle wood grain visible.",
        "Large softbox overhead key, shallow contact shadow, cool-neutral balance on product edges.",
        "Warm low-angle rim light plus soft frontal fill; faint specular highlight on primary material.",
        "Even high-key wrap, minimal shadow pool; emphasize matte vs glossy material contrast.",
        "Slight gradient backdrop falloff; micro-texture on tabletop; product centered with breathing room.",
    ]
    return hints[(cat_id + idx * 2 + attempt) % len(hints)]


def _apparel_angle_hint(cat_id: int, idx: int) -> str:
    """服饰类：版型/场景差异提示。"""
    hints = [
        "强调具体版型（宽松/修身/落肩）、面料混纺比例或功能性（透气/防风），不要泛泛「纯棉基础款」。",
        "强调图案工艺（刺绣/提花/条纹间距）或配色组合，与常见纯白款拉开差距。",
        "强调季节场景（轻薄叠穿/中层保暖）或配件组合（可拆卸帽/多口袋结构）。",
        "强调剪裁结构线、门襟类型、下摆处理等可辨识细节。",
    ]
    return hints[(cat_id + idx) % len(hints)]


def _resolve_category_counts(allowed: list[int], cfg: dict) -> tuple[list[int], str]:
    """
    每个 allowed 类目对应生成几条。
    二选一扩展：items_per_category_each（与 allowed 顺序等长）或
    items_per_category_by_id（按类目 id 覆盖，未写的 id 用 items_per_category）。
    否则：全部类目都用 items_per_category。
    """
    ipc = int(cfg.get("items_per_category", 1))
    if ipc < 1:
        raise ValueError("items_per_category must be >= 1")

    each = cfg.get("items_per_category_each")
    by_id = cfg.get("items_per_category_by_id")

    if each is not None and by_id is not None:
        raise ValueError("config: 只能二选一 — items_per_category_each 或 items_per_category_by_id")

    if each is not None:
        if not isinstance(each, (list, tuple)):
            raise ValueError("items_per_category_each 须为与 allowed_category_ids 等长的整数列表")
        lst = [int(x) for x in list(each)]
        if len(allowed) > 0 and len(lst) == 0:
            raise ValueError("items_per_category_each 不能为空列表")
        if len(lst) != len(allowed):
            raise ValueError(
                f"items_per_category_each 长度须与 allowed_category_ids 一致（{len(lst)} != {len(allowed)}）"
            )
        if any(x < 1 for x in lst):
            raise ValueError("items_per_category_each 每项须 >= 1")
        return lst, "items_per_category_each"

    if by_id is not None:
        if not isinstance(by_id, dict):
            raise ValueError("items_per_category_by_id 须为字典：类目 id -> 条数")
        norm: dict[int, int] = {}
        for k, v in by_id.items():
            ki = int(k)
            vi = int(v)
            if vi < 1:
                raise ValueError(f"items_per_category_by_id[{k}] 须 >= 1")
            norm[ki] = vi
        out = [norm.get(cid, ipc) for cid in allowed]
        return out, "items_per_category_by_id (+ items_per_category 默认)"

    return [ipc] * len(allowed), "items_per_category"


def _parse_by_id_cli(s: str) -> dict[int, int]:
    out: dict[int, int] = {}
    for part in s.split(","):
        part = part.strip()
        if not part:
            continue
        if "=" not in part:
            raise ValueError(f"无效的 --items-per-category-by-id 片段: {part!r}（应为 id=count）")
        a, b = part.split("=", 1)
        out[int(a.strip())] = int(b.strip())
    if not out:
        raise ValueError("--items-per-category-by-id 解析结果为空")
    return out


def _build_count_cfg(cfg: dict, args: argparse.Namespace) -> dict:
    """浅拷贝 cfg 并合并命令行对「每类条数」的覆盖（--items-per-category* 三者互斥）。"""
    cli_each = getattr(args, "items_per_category_each", None)
    cli_by_id = getattr(args, "items_per_category_by_id", None)
    cli_ipc = getattr(args, "items_per_category", None)
    n_cli = sum(x is not None for x in (cli_each, cli_by_id, cli_ipc))
    if n_cli > 1:
        raise ValueError(
            "命令行互斥：请只使用 --items-per-category、--items-per-category-each 或 --items-per-category-by-id 之一"
        )

    merged = dict(cfg)
    if cli_each is not None:
        parts = [int(x.strip()) for x in cli_each.split(",") if x.strip() != ""]
        merged["items_per_category_each"] = parts
        merged.pop("items_per_category_by_id", None)
    elif cli_by_id is not None:
        merged["items_per_category_by_id"] = _parse_by_id_cli(cli_by_id)
        merged.pop("items_per_category_each", None)
    elif cli_ipc is not None:
        if int(cli_ipc) < 1:
            raise ValueError("--items-per-category must be >= 1")
        merged["items_per_category"] = int(cli_ipc)
        merged.pop("items_per_category_each", None)
        merged.pop("items_per_category_by_id", None)
    return merged


def _strip_cjk(s: str) -> str:
    return "".join(re.findall(r"[\u4e00-\u9fff]", s or ""))


def _shared_cjk_substrings(a: str, b: str, min_len: int = 4, max_len: int = 14) -> set[str]:
    """两段文字中同时出现的 CJK 子串（长度在 [min_len, max_len]），从长到短枚举 a 侧窗口。"""
    ca, cb = _strip_cjk(a), _strip_cjk(b)
    out: set[str] = set()
    if len(ca) < min_len or len(cb) < min_len:
        return out
    upper = min(max_len, len(ca))
    for L in range(upper, min_len - 1, -1):
        for i in range(len(ca) - L + 1):
            sub = ca[i : i + L]
            if sub in cb:
                out.add(sub)
    return out


def _pairwise_cross_category_cjk(
    rows: list[tuple[int, str]], min_len: int = 4, max_len: int = 14
) -> set[str]:
    """仅统计「不同 categoryId」条目之间的公共 CJK 子串（同类目多条不两两比对，避免误伤「衬衫采用」等同类话术）。"""
    out: set[str] = set()
    for i in range(len(rows)):
        ci, bi = rows[i]
        xi = _strip_cjk(bi)
        for j in range(i + 1, len(rows)):
            cj, bj = rows[j]
            if ci == cj:
                continue
            xj = _strip_cjk(bj)
            out |= _shared_cjk_substrings(xi, xj, min_len, max_len)
    return out


def _forbidden_phrase_hint_for_user(rows: list[tuple[int, str]]) -> str:
    if len(rows) < 2:
        return "（当前仅 0~1 条：仍须避免与后续类目套同一套「万能材质」如连续使用「竹炭纤维」等。）"
    phrases = _pairwise_cross_category_cjk(rows, 4, 14)
    if not phrases:
        return (
            "（跨类目尚未检出 ≥4 字共同片段；若本类目有多条，须**子品类错开**；"
            "仍须与摘要每条在材质/形态上明显不同。）"
        )
    top = sorted(phrases, key=len, reverse=True)[:24]
    joined = "、".join(top)
    return (
        "下列中文片段已在**不同类目**的已生成内容（名称+描述）中共同出现，"
        "本轮输出在 **name 与 description 中均不得再包含其中任一片段**"
        "（勿仅换序、插空格或加后缀规避）："
        f"{joined}"
    )


def _batch_cjk_overlap_error_cross_only(
    prev_rows: list[tuple[int, str]],
    new_cat_id: int,
    new_name: str,
    new_desc: str,
    min_len: int = 6,
    max_len: int = 14,
) -> str | None:
    """仅与「其他 categoryId」的已接受条目比对长片段，同类目内多条靠提示词要求子品类错开。"""
    # 假设 A：失败来自 description↔description 的通用套话（≥6 字）被误判为跨类重复。
    # 假设 B：失败来自 name↔name 或 name↔desc 的真实撞车，应保持拦截。
    # 假设 C：整段 nf 与 pb 拼接比对会把「名+描」边界上的子串也算进去，放大误报。
    desc_desc_min = max(min_len + 6, 12)
    desc_desc_max = max(max_len, 22)
    for cid, pb in prev_rows:
        if cid == new_cat_id:
            continue
        parts = (pb or "").split("\n", 1)
        prev_nm = (parts[0] or "").strip()
        prev_desc = (parts[1] or "").strip() if len(parts) > 1 else ""
        checks: list[tuple[str, str, int, int, str]] = [
            (new_name, prev_nm, min_len, max_len, "newName-prevName"),
            (new_name, prev_desc, min_len, max_len, "newName-prevDesc"),
            (new_desc, prev_nm, min_len, max_len, "newDesc-prevName"),
            (new_desc, prev_desc, desc_desc_min, desc_desc_max, "newDesc-prevDesc"),
        ]
        for a, b, m_lo, m_hi, zone in checks:
            if not _strip_cjk(a) or not _strip_cjk(b):
                continue
            sh = _shared_cjk_substrings(a, b, m_lo, m_hi)
            if not sh:
                continue
            longest = max(sh, key=len)
            # region dbg 5578c5
            _dbg_5578c5(
                "A,B",
                "generate_candidates_llm._batch_cjk_overlap_error_cross_only",
                "cross_category_overlap_hit",
                {
                    "new_cat_id": new_cat_id,
                    "prev_cat_id": cid,
                    "zone": zone,
                    "min_len_used": m_lo,
                    "max_len_used": m_hi,
                    "longest": longest,
                    "longest_len": len(longest),
                    "new_name_snip": (new_name or "")[:48],
                    "prev_name_snip": (prev_nm or "")[:48],
                },
            )
            # endregion
            return (
                f"与同批**其他类目**已接受商品在中文名称/描述中存在 ≥{m_lo} 字的相同片段（{zone}）：{longest!r}；"
                "请更换材质体系、商品形态与命名，禁止套娃。"
            )
    return None


def _intraclass_diversity_block(
    cat_name: str,
    apparel: bool,
    idx: int,
    n_slots: int,
    lines_out: list[str],
    cat_id: int,
) -> str:
    """同一类目本轮第 2 条及以后：提示与已写入同类目条目子品类错开。"""
    if n_slots <= 1 or idx == 0:
        return ""
    prev_lines: list[str] = []
    for raw in lines_out:
        try:
            o = json.loads(raw)
        except json.JSONDecodeError:
            continue
        if int(o.get("categoryId", -1)) != cat_id:
            continue
        nm = str(o.get("name", "")).strip()
        desc = str(o.get("description", "")).strip()
        snip = desc[:100] + ("…" if len(desc) > 100 else "")
        prev_lines.append(f"  - {nm} | {snip}")
    if not prev_lines:
        return ""
    if apparel:
        rule = (
            "本条必须与上述同批条目在**子品类**上拉开距离：若上条为衬衫/衬衣类，本条禁止再以衬衫为主体；"
            "若上条为裤装，本条勿再做裤装；可选用外套、半裙、背心、针织开衫、马甲、配饰（领带/腰带/袜品）等同类目内另一实体。"
            "description 勿照搬上条常用开头（如连续「本款衬衫采用…」「本品采用…」雷同句式）。"
        )
    else:
        rule = (
            "本条必须与上述同批条目在**具体单品形态/用途**上明显不同，勿只改形容词；"
            "避免 description 与上条前半段句式高度一致。"
        )
    joined = "\n".join(prev_lines)
    return (
        f"类目「{cat_name}」本轮第 {idx + 1}/{n_slots} 条；须与下列**同批已写入本类目**条目错开子品类/形态：\n"
        f"{joined}\n"
        f"{rule}"
    )


def _prompt_stem(en: str) -> str:
    """用于同批去重：归一化后取前缀，避免 fullPromptEn 高度雷同仍通过。"""
    s = re.sub(r"\s+", " ", (en or "").strip().lower())
    return s[:220] if s else ""


def _batch_summaries(lines_out: list[str], max_lines: int = 14) -> str:
    """把本批已写入的 name + 英文截断列出来，供下一轮 user 提示避让。"""
    if not lines_out:
        return "（尚无）"
    tail = lines_out[-max_lines:]
    parts: list[str] = []
    for raw in tail:
        try:
            o = json.loads(raw)
        except json.JSONDecodeError:
            continue
        nm = str(o.get("name", "")).strip()
        en = str(o.get("fullPromptEn", "")).strip()
        snip = en[:100] + ("…" if len(en) > 100 else "")
        parts.append(f"- {nm} | EN: {snip}")
    return "\n".join(parts) if parts else "（尚无）"


def _call_http(cfg: dict, messages: list[dict[str, str]]) -> str:
    import httpx

    llm = cfg["llm"]
    base = llm["base_url"].rstrip("/")
    url = f"{base}/chat/completions"
    req_mt = int(llm.get("max_tokens", 1400))
    req_mt = max(1200, min(req_mt, 8192))
    body = {
        "model": llm.get("remote_model", "gpt-3.5-turbo"),
        "messages": messages,
        "temperature": float(llm.get("temperature", 0.65)),
        "max_tokens": req_mt,
    }
    headers = {"Content-Type": "application/json"}
    key = llm.get("api_key")
    if key is not None and str(key).strip() != "":
        headers["Authorization"] = f"Bearer {key}"
    with httpx.Client(timeout=600.0) as client:
        r = client.post(url, json=body, headers=headers)
        r.raise_for_status()
        data = r.json()
    # region agent log
    try:
        ch0 = data.get("choices", [{}])[0]
        _agent_dbg(
            "D",
            "generate_candidates_llm._call_http",
            "openai_compat_response",
            {
                "finish_reason": ch0.get("finish_reason"),
                "usage": data.get("usage"),
                "request_max_tokens": body["max_tokens"],
            },
        )
    except (KeyError, IndexError, TypeError):
        pass
    # endregion
    return data["choices"][0]["message"]["content"]


def _call_gguf(llm: Any, messages: list[dict[str, str]], cfg: dict) -> str:
    lc = cfg["llm"]
    req_mt = int(lc.get("max_tokens", 1400))
    req_mt = max(1200, min(req_mt, 8192))
    out = llm.create_chat_completion(
        messages=messages,
        temperature=float(lc.get("temperature", 0.65)),
        max_tokens=req_mt,
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
        help="Stop after writing N lines (hard cap; order = allowed_category_ids × 每类条数，见 items_per_category / each / by_id)。",
    )
    parser.add_argument(
        "--items-per-category",
        type=int,
        default=None,
        metavar="N",
        help="覆盖 config：allowed_category_ids 中每个类目各生成 N 条（并忽略 yaml 中的 items_per_category_each / items_per_category_by_id）。",
    )
    parser.add_argument(
        "--items-per-category-each",
        type=str,
        default=None,
        metavar="LIST",
        help="覆盖 config：逗号分隔整数，长度须与 allowed_category_ids 一致，例如 2,2,1,3,1,1,1,2。",
    )
    parser.add_argument(
        "--items-per-category-by-id",
        type=str,
        default=None,
        metavar="MAP",
        help='覆盖 config：按类目 id 指定条数，例如 "6=2,7=3"；未写到的 id 仍用 yaml 的 items_per_category。',
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
    try:
        count_cfg = _build_count_cfg(cfg, args)
        counts_list, counts_mode = _resolve_category_counts(allowed, count_cfg)
    except ValueError as e:
        print(str(e), file=sys.stderr)
        return 2
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
- name: 中文**一段式**短标题（约 4~14 个汉字；超出视为不合格），不含违禁词，不要与禁忌列表重复或极度相似；也不要与同批次已列出的商品同名或仅改一两个字。**禁止**在 name 里使用竖线「|」、冒号副标题、换行、多个逗号串联卖点；禁止把长句卖点写进标题（那些内容必须写在 description）。
- description: 中文 2~4 句卖点，必须包含至少 1 个可验证的具体特征（尺寸层级、材质工艺、使用场景、兼容对象等），禁止空洞套话堆砌。
- price: 数字，必须在用户给定区间内
- stock: 整数
- fullPromptEn: 英文一整段，电商主图 prompt，建议总长度不少于 380 个英文字符（更长更好）；**过短会导致整轮 JSON 被拒**。必须以 "Straight-on view product photography of" 开头；主体后依次展开：
  (1) 商品具体形态、材质或表面处理（如 matte ceramic / brushed aluminum / ribbed silicone 等，勿泛泛 product）；
  (2) 至少 1 处结构或配件细节（卡扣、接口、纹理分区、透明视窗等）；
  (3) 场景：白色木质台面或地板（按商品类型）；背景 pale grayish green；minimal clean neutral；commercial product photo；
  (4) 将 user 消息中「光线质感句」自然并入句末（可稍改写但勿删掉 straight-on 要求）；
  (5) 明确写 no text, no watermark, no logo。不要包含 --ar 或 --v 等多余参数。
- stylePreset: 固定为 english_minimal_wood_table_3x4
- appendSdSafetySuffix: false

类目与多样性（必须遵守）：
- name、description、fullPromptEn 描述的主体商品必须与 user 消息里的 categoryName 类目一致，不得跨类偷换成其他货架「万能款」。
- 同一运行批次内，不同类目商品在中文名称与卖点中 **禁止复用相同的核心材质/营销套话**（例如多件都用「竹炭纤维」作主卖点词）；英文 fullPromptEn 也不得用同一材料英文词串硬套到无关类目。
- **材质多样性**：除非 user 明确允许，否则不要把「竹、竹纹、竹节、竹纤维、竹炭、竹制」作为多个不同类目共用的命名主轴；家居、文具、厨具等应优先轮换金属、塑料、陶瓷、玻璃、织物、实木（非竹）等。整批运行中若已有商品在中文名或描述里出现「竹」字，后续条目须避免再用「竹」系词，除非 user 指定该条为竹制品。
- 若 user 提供「同批避让」或「本批次已生成摘要」，你必须在主体商品形态、材质、用途上与其中每一条都明显不同，禁止只换同义词或微调形容词。
- 若 user 标明「本类目非服饰鞋包」，则禁止以 T恤、卫衣、衬衫、裤子、裙子、连衣裙、内衣、袜子、鞋、帽、双肩包等穿戴/出行为唯一主体；禁止用「纯棉」「圆领短袖」「基础款打底」等话术作为卖点主轴；英文 fullPromptEn 也不得只描述 generic cotton tee。
- 若 user 标明「本类目为服饰鞋包」，则商品须贴合该类目，但避免每轮都输出雷同的「纯白纯棉基础款 T 恤」，应更具体（款式、材质组合、场景差异等）。
- 若 user 标明「同一类目本轮多条」，则每条须为**不同子品类/款式**（例如不要两条都以衬衫为主体），中文卖点句式也要错开，避免套同一模板。
- 若 user 提供「本槽位已因重名被拒的 name」列表，**禁止**再次使用该列表中的任一名称作为 name（含去空格后相同）。
"""

    lines_out: list[str] = []
    accepted_cat_bundles: list[tuple[int, str]] = []
    batch_prompt_stems: set[str] = set()
    lc = cfg.get("llm") or {}
    min_full_prompt_en = int(lc.get("min_full_prompt_en_chars", 200))
    min_full_prompt_en = max(120, min(min_full_prompt_en, 600))
    if lc.get("max_attempts_per_slot") is not None:
        max_attempts_per_slot = int(lc["max_attempts_per_slot"])
    else:
        # 未配置时：在旧 max_retries_per_slot 基础上至少 25 次，避免跨类目/重名等校验一失败就整脚本退出
        max_attempts_per_slot = max(int(lc.get("max_retries_per_slot", 3)), 25)
    max_attempts_per_slot = max(1, min(max_attempts_per_slot, 200))

    for pos, cat_id in enumerate(allowed):
        cat_name = categories.get(cat_id, str(cat_id))
        n_slots = counts_list[pos]
        for idx in range(n_slots):
            forbid_sample = list(existing.get("product_names", []))[:80]
            # 本槽位内曾因「与库/本批重名」被拒的名称，必须写回 prompt（单次展示的禁忌名有限，易漏掉库中已有名）
            slot_rejected_names: list[str] = []

            short_uid = uuid.uuid4().hex[:8]
            suggested_id = f"auto-{cat_id}-{short_uid}"
            apparel = _category_looks_like_apparel(cat_name)

            obj = None
            last_err: str | None = None
            for attempt in range(max_attempts_per_slot):
                width = 40
                if forbid_sample:
                    if len(forbid_sample) <= width:
                        forbid_hint = ", ".join(forbid_sample)
                    else:
                        step = len(forbid_sample) - width + 1
                        off = (attempt * 7 + cat_id * 3 + idx * 5) % step
                        forbid_hint = ", ".join(forbid_sample[off : off + width])
                else:
                    forbid_hint = "(无)"

                nudge = _diversity_nudge(cat_id, idx, attempt)
                light_en = _lighting_texture_hint(cat_id, idx, attempt)
                batch_block = _batch_summaries(lines_out)
                dup_block = _forbidden_phrase_hint_for_user(accepted_cat_bundles)
                intra_block = _intraclass_diversity_block(
                    cat_name, apparel, idx, n_slots, lines_out, cat_id
                )

                bamboo_note = ""
                if _batch_bamboo_count(accepted_cat_bundles) >= 1:
                    bamboo_note = (
                        "【本批「竹」系已出现】本条在 name 与 description 中均不得再出现汉字「竹」。\n"
                    )
                if not apparel:
                    mk = _non_apparel_material_kernel(cat_id, idx, attempt)
                    material_section = (
                        f"{bamboo_note}"
                        f"【本轮材质/造型参考（非服饰）】请让商品主体落到下列方向之一"
                        f"（择一写具体；名称轻点即可，细节放 description；勿整套文案照抄）：「{mk}」。\n\n"
                    )
                else:
                    material_section = bamboo_note + ("\n" if bamboo_note else "")

                if apparel:
                    multi = ""
                    if n_slots > 1:
                        multi = (
                            "若本 user 消息含「本类目同批多样性」，则须严格遵守其中子品类错开要求；"
                        )
                    cat_rule = (
                        "【类目类型】本类目为服饰/鞋包/配件相关。请生成符合该类目的商品；"
                        "中文名与英文主体必须与该类目相符；"
                        "不要与禁忌列表同名或仅改一两个字的变体；"
                        f"{multi}"
                        "避免连续多轮都输出「纯白圆领纯棉短袖 T 恤」这类完全同质的基础款，应更具体（例如明确版型、图案、材质组合或配件形态）。"
                        f"{_apparel_angle_hint(cat_id, idx)}"
                    )
                else:
                    multi_nc = ""
                    if n_slots > 1:
                        multi_nc = "若本 user 消息含「本类目同批多样性」，须遵守其中与上条错开形态/用途的要求；"
                    cat_rule = (
                        "【类目类型】本类目非服饰鞋包专营。"
                        f"{multi_nc}"
                        "请先在心里列举该「类目」下常见的 5 种不同实体商品（不要服装），再任选其中一种生成，且不要总选列表里的第一项；"
                        "禁止把 T恤、卫衣、衬衫、裤子、裙子、内衣、袜子、鞋、帽、包袋作为商品主体；"
                        "禁止用「纯棉」「短袖」「基础款」作为唯一卖点主轴。"
                        f"多样性提示：{nudge}"
                    )

                retry_tail = ""
                if last_err:
                    retry_tail = (
                        f"\n\n【重要修正】上一轮未通过校验：{last_err}\n"
                        "请完全更换实体商品（不同形态/材质/用途），并重写 name、description、fullPromptEn，禁止只做同义词替换。"
                    )
                    if "fullPromptEn" in last_err or "过短" in last_err:
                        retry_tail += (
                            f"\n【fullPromptEn 专条】英文字段 **fullPromptEn** 在 JSON 内单字符串长度须 **≥ {min_full_prompt_en}**（建议 380+）；"
                            "请写连贯长段英文，展开材质/高光/倒角或分型线、局部结构、木台面与 pale grayish green 背景、光线句，"
                            "并包含 no text, no watermark, no logo；禁止缩写、禁止仅用两三句敷衍。\n"
                        )

                intra_section = (
                    f"【本类目同批多样性】\n{intra_block}\n\n" if intra_block else ""
                )
                reject_section = ""
                if slot_rejected_names:
                    rej_lines = "\n".join(f"  - {x}" for x in slot_rejected_names)
                    reject_section = (
                        f"【本槽位已因重名被拒的 name（禁止再输出下列全文，也不要去空格/插字后相同）】\n"
                        f"{rej_lines}\n\n"
                    )

                user_prompt = f"""merchantId={merchant_id}, categoryId={cat_id}, categoryName={cat_name}
【fullPromptEn 长度】字段 fullPromptEn 英文字符总数必须 ≥ {min_full_prompt_en}（建议 ≥380），否则本请求会被判失败。
price between {pmin} and {pmax}, stock={stock_def}
Forbidden existing names (do not copy or trivially vary): {forbid_hint}
{reject_section}Suggested id: {suggested_id}
request_uid={suggested_id}-{idx}-a{attempt}

【本批次已生成摘要（主体必须与下列每一条明显不同）】
{batch_block}

{material_section}{intra_section}【同批跨类目中文避让（name/description 须遵守）】
{dup_block}

【光线质感句（请并入 fullPromptEn 英文末段，保持语法自然）】
{light_en}

{cat_rule}
{retry_tail}

请为该类目原创一个全新的实体商品（中文名必须明显区别于禁忌列表与本批次摘要；英文 fullPromptEn 的主体必须与中文商品一致）。输出单个 JSON 对象。"""

                messages = [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ]

                if args.dry_run:
                    print("--- dry-run ---")
                    print(user_prompt[:1200])
                    break

                raw = ""
                try:
                    if backend == "http":
                        raw = _call_http(cfg, messages)
                    else:
                        raw = _call_gguf(llm_model, messages, cfg)
                    obj = _extract_json_object(raw)
                    # region agent log
                    en0 = str(obj.get("fullPromptEn", "") or "").strip()
                    _agent_dbg(
                        "A,B,C,E",
                        "generate_candidates_llm.main:post_extract",
                        "after_json_extract",
                        {
                            "cat_id": cat_id,
                            "idx": idx,
                            "attempt": attempt,
                            "raw_len": len(raw),
                            "obj_keys": sorted(obj.keys()),
                            "fullPromptEn_len": len(en0),
                            "has_fullPromptEn_key": "fullPromptEn" in obj,
                            "alt_prompt_keys": [
                                k
                                for k in obj.keys()
                                if isinstance(k, str) and "prompt" in k.lower() and k != "fullPromptEn"
                            ],
                        },
                    )
                    # endregion
                except Exception as e:
                    print(f"LLM failed category {cat_id} idx {idx} attempt {attempt}: {e}", file=sys.stderr)
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
                en = str(obj.get("fullPromptEn", "")).strip()
                stem = _prompt_stem(en)

                if not nm:
                    last_err = "商品名为空"
                    continue
                nf_err = _name_format_error(nm)
                if nf_err:
                    last_err = nf_err
                    continue
                if _norm_name(nm) in forbidden:
                    nnm = _norm_name(nm)
                    if nm and not any(_norm_name(x) == nnm for x in slot_rejected_names):
                        slot_rejected_names.append(nm)
                    last_err = f"商品名与禁忌或本批已用重复: {nm!r}"
                    continue
                if len(en) < min_full_prompt_en:
                    # region agent log
                    _agent_dbg(
                        "A",
                        "generate_candidates_llm.main:validate_en",
                        "fullPromptEn_below_threshold",
                        {
                            "cat_id": cat_id,
                            "idx": idx,
                            "attempt": attempt,
                            "len_en": len(en),
                            "min_full_prompt_en": min_full_prompt_en,
                            "en_prefix": en[:160],
                        },
                    )
                    # endregion
                    # region dbg 5578c5
                    _dbg_5578c5(
                        "E1,E2,E3",
                        "generate_candidates_llm.main:validate_en",
                        "fullPromptEn_below_threshold",
                        {
                            "cat_id": cat_id,
                            "idx": idx,
                            "attempt": attempt,
                            "len_en": len(en),
                            "min_full_prompt_en": min_full_prompt_en,
                            "en_prefix": en[:200],
                        },
                    )
                    # endregion
                    last_err = (
                        f"fullPromptEn 过短（当前约 {len(en)} 英文字符，硬下限 {min_full_prompt_en}；"
                        "建议 ≥380）：请写满材质、结构细节与光线句"
                    )
                    continue
                if stem and stem in batch_prompt_stems:
                    last_err = "fullPromptEn 与同批已有商品英文描述高度雷同（开头大段重复），请换完全不同的实体与句式"
                    continue

                desc_cn = str(obj.get("description", "")).strip()
                bundle_cn = f"{nm}\n{desc_cn}"
                if "竹" in bundle_cn and _batch_bamboo_count(accepted_cat_bundles) >= 1:
                    last_err = (
                        "本批已有条目含「竹」字；为避免跨类目命名与材质风格雷同，"
                        "本条请改用其它材质/纹样，且 name 与 description 均勿再用「竹」"
                    )
                    continue
                ov = _batch_cjk_overlap_error_cross_only(accepted_cat_bundles, cat_id, nm, desc_cn)
                if ov:
                    last_err = ov
                    continue

                last_err = None
                break

            if args.dry_run:
                continue

            if last_err is not None or obj is None:
                print(
                    f"Give up after {max_attempts_per_slot} attempts category {cat_id} idx {idx}: {last_err}",
                    file=sys.stderr,
                )
                return 1

            nm = str(obj.get("name", "")).strip()
            en = str(obj.get("fullPromptEn", "")).strip()
            forbidden.add(_norm_name(nm))
            batch_prompt_stems.add(_prompt_stem(en))
            accepted_cat_bundles.append(
                (cat_id, f"{nm}\n{str(obj.get('description', '')).strip()}")
            )

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
    cap = sum(counts_list)
    if args.max_products is not None and args.max_products > cap:
        print(
            f"Note: --max-products={args.max_products} is only an upper cap; "
            f"this config yields at most {cap} rows ({counts_mode}; sum per category = {cap}). "
            f"Raise per-category counts or add allowed_category_ids to reach ~{args.max_products}.",
            file=sys.stderr,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
