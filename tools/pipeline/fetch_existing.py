#!/usr/bin/env python3
"""
从 MySQL 读取分类表 + 指定商户已有商品，用于新品生成去重。

依赖 tools/.env 中的 MYSQL_*（与 db_import 相同）。

用法:
  python fetch_existing.py --config pipeline/config.yaml
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import pymysql
import yaml

from db_env import load_dotenv, mysql_connect_args


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=root / "pipeline" / "config.yaml")
    parser.add_argument("--dotenv", type=Path, default=root / ".env")
    args = parser.parse_args()

    load_dotenv(args.dotenv)

    if not args.config.is_file():
        print(f"Config not found: {args.config}", file=sys.stderr)
        print("Copy pipeline/config.example.yaml -> pipeline/config.yaml", file=sys.stderr)
        return 2

    cfg = yaml.safe_load(args.config.read_text(encoding="utf-8"))
    merchant_id = int(cfg["merchant_id"])
    out_dir = root / cfg.get("paths", {}).get("out_dir", "out/pipeline_run")
    out_dir.mkdir(parents=True, exist_ok=True)

    kw = mysql_connect_args()
    conn = pymysql.connect(**kw)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id, name FROM category ORDER BY id")
            cats = {int(r[0]): str(r[1]) for r in cur.fetchall()}

            cur.execute(
                "SELECT id, name, description, category_id FROM product WHERE merchant_id = %s ORDER BY id",
                (merchant_id,),
            )
            rows = cur.fetchall()
            products = [
                {"id": int(r[0]), "name": r[1], "description": r[2], "categoryId": int(r[3])}
                for r in rows
            ]
    finally:
        conn.close()

    names = [p["name"] for p in products]
    payload = {
        "merchant_id": merchant_id,
        "categories": cats,
        "product_names": names,
        "products": products,
    }

    cat_path = out_dir / "categories.json"
    ex_path = out_dir / "existing.json"
    cat_path.write_text(
        json.dumps({"categories": cats}, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    ex_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Wrote {cat_path} ({len(cats)} categories)")
    print(f"Wrote {ex_path} ({len(products)} existing products for merchant {merchant_id})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
