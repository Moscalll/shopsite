#!/usr/bin/env python3
"""
从 ProductSpec JSONL 生成 Prompts JSONL + Catalog JSONL（不落库、不出图）。

常用字段:
  - fullPromptEn: 整条英文正提示词（按分类写死一段话时使用）
  - productDetailEn / showFrontLabel: 使用预设模板 english_minimal_wood_table_3x4 时填写

用法:
  python generate.py --in samples/product_spec.sample.jsonl --out-dir out/run1
"""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import re
import sys
from pathlib import Path
from typing import Any


def _load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def _normalize_prompt(s: str) -> str:
    s = s.lower().strip()
    s = re.sub(r"\s+", " ", s)
    return s


def _stable_seed(spec_id: str, prompt: str) -> int:
    h = hashlib.sha256(f"{spec_id}|{prompt}".encode("utf-8")).digest()
    return int.from_bytes(h[:4], "big") % (2**31 - 1)


def _safe_filename_base(spec_id: str) -> str:
    base = re.sub(r"[^A-Za-z0-9._-]+", "_", spec_id).strip("._-")
    return base or "item"


def _image_extension(spec: dict[str, Any], default_fmt: str) -> str:
    """Returns filename suffix including dot, e.g. '.webp'."""
    raw = spec.get("outputFormat") or spec.get("imageFormat") or default_fmt
    key = str(raw).lower().lstrip(".")
    mapping = {"png": ".png", "webp": ".webp", "jpg": ".jpg", "jpeg": ".jpg", "gif": ".gif"}
    if key in mapping:
        return mapping[key]
    dkey = str(default_fmt).lower().lstrip(".")
    return mapping.get(dkey, ".webp")


_SD_SAFE_SUFFIX = (
    ", minimal clean neutral aesthetic, commercial product photo, no text, no watermark, no logo"
)


def resolve_prompt(spec: dict[str, Any], presets: dict[str, Any]) -> tuple[str, str, str]:
    """
    If `fullPromptEn` (or `promptEn`) is set, it becomes the full positive prompt — use this for
    per-category custom prose (e.g. one fixed English paragraph per category).
    Otherwise falls back to `build_prompt` templates + productDetailEn.
    """
    full = (spec.get("fullPromptEn") or spec.get("promptEn") or "").strip()
    if full:
        style = spec.get("stylePreset") or "english_minimal_wood_table_3x4"
        preset = presets.get(style)
        if not preset:
            raise ValueError(f"Unknown stylePreset: {style}")
        neg = (spec.get("negativePromptEn") or "").strip()
        if not neg:
            neg = str(preset.get("negative", ""))
        if spec.get("appendSdSafetySuffix", True):
            low = full.lower()
            if "no text" not in low and "no watermark" not in low:
                full = full.rstrip(". ") + _SD_SAFE_SUFFIX
        return full, neg, str(style)
    return build_prompt(spec, presets)


def build_prompt(spec: dict[str, Any], presets: dict[str, Any]) -> tuple[str, str, str]:
    style = spec.get("stylePreset") or "english_minimal_wood_table_3x4"
    preset = presets.get(style)
    if not preset:
        raise ValueError(f"Unknown stylePreset: {style}")

    fmt = preset.get("format") or "comma_join"

    if fmt == "english_paragraph":
        # Aligns with historical MJ-style English lines: straight-on, wood table, pale gray-green BG, minimal/clean/neutral.
        detail = (spec.get("productDetailEn") or "").strip()
        if not detail:
            pt = spec.get("productType") or "product"
            detail = f"a {pt}"
        label_clause = ", showing the full front label" if spec.get("showFrontLabel") else ""
        template = str(preset["template"])
        prompt = template.format(detail=detail, label_clause=label_clause)
        negative = str(preset.get("negative", ""))
        return prompt, negative, str(style)

    product_type = spec.get("productType") or "product"
    features = spec.get("keyFeatures") or []
    materials = spec.get("materials") or ""
    colorway = spec.get("colorway") or ""
    audience = spec.get("targetAudience") or ""

    feat_text = ""
    if isinstance(features, list) and features:
        feat_text = "Key features: " + "; ".join(str(x) for x in features) + ". "

    extra = " ".join(
        part
        for part in (
            f"Materials: {materials}." if materials else "",
            f"Colorway: {colorway}." if colorway else "",
            f"Audience: {audience}." if audience else "",
        )
        if part
    )

    subject = f"Subject: {product_type}. {feat_text}{extra}".strip()
    prompt = ", ".join(
        [
            preset["prompt_prefix"],
            subject,
            preset["prompt_suffix"],
        ]
    )
    negative = str(preset.get("negative", ""))
    return prompt, negative, str(style)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate prompts.jsonl + catalog.jsonl from product_spec.jsonl")
    parser.add_argument("--in", dest="in_path", required=True, help="Input product_spec.jsonl")
    parser.add_argument("--out-dir", required=True, help="Output directory")
    parser.add_argument(
        "--presets",
        default=str(Path(__file__).with_name("presets.json")),
        help="Path to presets.json",
    )
    parser.add_argument(
        "--image-format",
        default="webp",
        help="Default output image extension for imageBasename (png|webp|jpg|gif). Per-line override: outputFormat in JSON.",
    )
    args = parser.parse_args()

    in_path = Path(args.in_path)
    out_dir = Path(args.out_dir)
    presets_path = Path(args.presets)

    if not in_path.is_file():
        print(f"Input not found: {in_path}", file=sys.stderr)
        return 2

    presets = _load_json(presets_path)
    if not isinstance(presets, dict):
        print("presets.json must be an object", file=sys.stderr)
        return 2

    out_dir.mkdir(parents=True, exist_ok=True)
    prompts_path = out_dir / "prompts.jsonl"
    catalog_path = out_dir / "catalog.jsonl"

    seen_hashes: set[str] = set()
    n = 0

    with in_path.open("r", encoding="utf-8") as fin, prompts_path.open(
        "w", encoding="utf-8"
    ) as fprompt, catalog_path.open("w", encoding="utf-8") as fcat:
        for line_no, line in enumerate(fin, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                spec = json.loads(line)
            except json.JSONDecodeError as e:
                print(f"Line {line_no}: invalid JSON: {e}", file=sys.stderr)
                return 2

            spec_id = str(spec.get("id") or f"line{line_no}")
            category_id = spec.get("categoryId")
            merchant_id = spec.get("merchantId")
            if category_id is None:
                print(f"Line {line_no}: categoryId is required", file=sys.stderr)
                return 2
            if merchant_id is None:
                print(f"Line {line_no}: merchantId is required", file=sys.stderr)
                return 2

            prompt, negative, style_used = resolve_prompt(spec, presets)

            preset_used = presets.get(style_used) or {}
            w = int(spec.get("width") or preset_used.get("width") or 768)
            img_h = int(spec.get("height") or preset_used.get("height") or 1024)

            norm = _normalize_prompt(prompt)
            p_hash = hashlib.sha256(norm.encode("utf-8")).hexdigest()
            if p_hash in seen_hashes:
                # 轻量去重：追加随机构图词（仍保持风格前缀）
                jitter = random.choice(
                    [
                        "slightly higher camera angle",
                        "slightly lower camera angle",
                        "a bit closer framing",
                        "a bit wider framing",
                        "gentler side key light",
                        "slightly stronger rim light",
                    ]
                )
                prompt = f"{prompt}, {jitter}"
                norm = _normalize_prompt(prompt)
                p_hash = hashlib.sha256(norm.encode("utf-8")).hexdigest()
            seen_hashes.add(p_hash)

            seed = int(spec.get("seed")) if spec.get("seed") is not None else _stable_seed(spec_id, prompt)
            base = _safe_filename_base(spec_id)
            ext = _image_extension(spec, args.image_format)
            image_filename = f"{base}{ext}"

            name = spec.get("name") or f"{spec.get('productType') or '商品'} {spec_id}"
            description = spec.get("description") or (
                (spec.get("productType") or "商品") + "。" + " ".join(str(x) for x in (spec.get("keyFeatures") or []) if x)
            )
            price = spec.get("price")
            stock = spec.get("stock")
            if price is None or stock is None:
                print(f"Line {line_no}: price and stock are required", file=sys.stderr)
                return 2

            prompt_row = {
                "imageBasename": image_filename,
                "prompt": prompt,
                "negativePrompt": negative,
                "seed": seed,
                "width": w,
                "height": img_h,
                "outputFormat": ext[1:] if ext.startswith(".") else ext,
                "specId": spec_id,
                "stylePreset": style_used,
            }
            fprompt.write(json.dumps(prompt_row, ensure_ascii=False) + "\n")

            cat_row = {
                "name": name,
                "description": description,
                "price": price,
                "stock": stock,
                "isAvailable": bool(spec.get("isAvailable", True)),
                "categoryId": int(category_id),
                "merchantId": int(merchant_id),
                "imageFilename": image_filename,
            }
            fcat.write(json.dumps(cat_row, ensure_ascii=False) + "\n")
            n += 1

    print(f"Wrote {n} rows -> {prompts_path} , {catalog_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
