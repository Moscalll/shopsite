#!/usr/bin/env python3
"""
读取 catalog.jsonl，拷贝图片到 uploads 目录，并 INSERT 到 MySQL product 表。

用法:
  python import_products.py --catalog ..\\out\\run1\\catalog.jsonl --images-dir ..\\out\\run1\\images
  python import_products.py ... --skip-if-name-exists

新增参数 --skip-if-name-exists：同一商户下商品名已存在则跳过（上新时避免重复插入）。

环境变量（也可用命令行覆盖部分）:
  MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE
  SHOPSITE_UPLOAD_DIR  默认 ./uploads
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
import time
from pathlib import Path

try:
    import pymysql
except ImportError:
    pymysql = None  # type: ignore

# region agent log
_DEBUG_LOG = Path(__file__).resolve().parents[2] / "debug-9c611e.log"
_SESSION_ID = "9c611e"


def _agent_log(message: str, hypothesis_id: str, data: dict) -> None:
    try:
        line = json.dumps(
            {
                "sessionId": _SESSION_ID,
                "hypothesisId": hypothesis_id,
                "location": "db_import/import_products.py",
                "message": message,
                "data": data,
                "timestamp": int(time.time() * 1000),
            },
            ensure_ascii=False,
        )
        with _DEBUG_LOG.open("a", encoding="utf-8") as _f:
            _f.write(line + "\n")
    except Exception:
        pass


_agent_log(
    "pymysql_import_probe",
    "H1",
    {"pymysql_available": pymysql is not None, "executable": sys.executable},
)
# endregion agent log


ALLOWED_IMAGE_EXT = {".png", ".jpg", ".jpeg", ".gif", ".webp"}


def _load_dotenv(dotenv_path: Path) -> None:
    if not dotenv_path.is_file():
        return
    with dotenv_path.open("r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            k, v = line.split("=", 1)
            k = k.strip()
            v = v.strip().strip('"').strip("'")
            if k and k not in os.environ:
                os.environ[k] = v


def _assert_safe_filename(name: str) -> None:
    if name in (".", ".."):
        raise ValueError(f"Invalid filename: {name}")
    if "/" in name or "\\" in name or ".." in name:
        raise ValueError(f"Path components not allowed in imageFilename: {name}")


def _connect(args: argparse.Namespace):
    if pymysql is None:
        raise RuntimeError(
            "pymysql is not installed. From the repository tools/ folder run: "
            "pip install -r requirements.txt   (or: pip install pymysql)"
        )
    return pymysql.connect(
        host=args.mysql_host,
        port=args.mysql_port,
        user=args.mysql_user,
        password=args.mysql_password,
        database=args.mysql_database,
        charset="utf8mb4",
        autocommit=False,
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Import catalog.jsonl into MySQL + copy images to uploads/")
    parser.add_argument("--catalog", required=True, help="catalog.jsonl path")
    parser.add_argument("--images-dir", required=True, help="Directory containing image files")
    parser.add_argument("--upload-dir", default=os.environ.get("SHOPSITE_UPLOAD_DIR", "uploads"), help="Physical uploads dir")
    parser.add_argument("--dotenv", default=str(Path(__file__).resolve().parents[1] / ".env"), help="Optional .env path")
    parser.add_argument("--dry-run", action="store_true", help="Validate only; no DB writes / no file copies")
    parser.add_argument(
        "--skip-if-name-exists",
        action="store_true",
        help="Skip rows where (merchant_id, name) already exists in product table (上新时避免同名重复上架)",
    )

    parser.add_argument("--mysql-host", default=os.environ.get("MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--mysql-port", type=int, default=int(os.environ.get("MYSQL_PORT", "3306")))
    parser.add_argument("--mysql-user", default=os.environ.get("MYSQL_USER", "root"))
    parser.add_argument("--mysql-password", default=os.environ.get("MYSQL_PASSWORD", ""))
    parser.add_argument("--mysql-database", default=os.environ.get("MYSQL_DATABASE", "shopsite"))

    args = parser.parse_args()

    dotenv_path = Path(args.dotenv)
    _load_dotenv(dotenv_path)

    # Re-read after dotenv
    args.mysql_host = os.environ.get("MYSQL_HOST", args.mysql_host)
    args.mysql_port = int(os.environ.get("MYSQL_PORT", str(args.mysql_port)))
    args.mysql_user = os.environ.get("MYSQL_USER", args.mysql_user)
    args.mysql_password = os.environ.get("MYSQL_PASSWORD", args.mysql_password)
    args.mysql_database = os.environ.get("MYSQL_DATABASE", args.mysql_database)
    args.upload_dir = os.environ.get("SHOPSITE_UPLOAD_DIR", args.upload_dir)

    catalog_path = Path(args.catalog)
    images_dir = Path(args.images_dir)
    upload_dir = Path(args.upload_dir)

    if not catalog_path.is_file():
        print(f"Catalog not found: {catalog_path}", file=sys.stderr)
        return 2
    if not images_dir.is_dir():
        print(f"Images dir not found: {images_dir}", file=sys.stderr)
        return 2

    rows: list[dict] = []
    with catalog_path.open("r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                row = json.loads(line)
            except json.JSONDecodeError as e:
                print(f"Line {line_no}: invalid JSON: {e}", file=sys.stderr)
                return 2
            rows.append((line_no, row))

    # Validate all before touching DB
    for line_no, row in rows:
        for k in ("name", "description", "price", "stock", "categoryId", "merchantId", "imageFilename"):
            if k not in row:
                print(f"Line {line_no}: missing field {k}", file=sys.stderr)
                return 2
        fn = str(row["imageFilename"])
        _assert_safe_filename(fn)
        ext = Path(fn).suffix.lower()
        if ext not in ALLOWED_IMAGE_EXT:
            print(f"Line {line_no}: unsupported image extension {ext}", file=sys.stderr)
            return 2
        src = images_dir / fn
        if not src.is_file():
            print(f"Line {line_no}: image not found: {src}", file=sys.stderr)
            return 2
        if not bool(row.get("isAvailable", True)):
            print(f"Line {line_no}: isAvailable=false may hide product on storefront lists", file=sys.stderr)
        if int(row["stock"]) <= 0:
            print(f"Line {line_no}: stock<=0 may hide product on storefront (needs stock>0)", file=sys.stderr)

    seen_key: set[tuple[int, str]] = set()
    for line_no, row in rows:
        key = (int(row["merchantId"]), str(row["name"]))
        if key in seen_key:
            print(
                f"Line {line_no}: duplicate name for same merchantId in catalog: {row['name']!r}",
                file=sys.stderr,
            )
            return 2
        seen_key.add(key)

    if args.dry_run:
        print(f"Dry-run OK: {len(rows)} rows validated.")
        print(f"Would copy -> {upload_dir.resolve()}")
        print(f"Would insert into {args.mysql_database} @ {args.mysql_host}:{args.mysql_port}")
        if args.skip_if_name_exists:
            print("(With --skip-if-name-exists, run again without --dry-run to skip existing names.)")
        return 0

    upload_dir.mkdir(parents=True, exist_ok=True)

    conn = _connect(args)
    imported = 0
    skipped = 0
    try:
        with conn.cursor() as cur:
            for line_no, row in rows:
                mid = int(row["merchantId"])
                pname = str(row["name"])
                if args.skip_if_name_exists:
                    cur.execute(
                        "SELECT 1 FROM product WHERE merchant_id = %s AND name = %s LIMIT 1",
                        (mid, pname),
                    )
                    if cur.fetchone():
                        print(f"Line {line_no}: skip (name exists for merchant): {pname!r}")
                        skipped += 1
                        continue

                fn = str(row["imageFilename"])
                src = images_dir / fn
                dst = upload_dir / fn
                shutil.copy2(src, dst)

                image_url = f"/uploads/{fn}"
                sql = (
                    "INSERT INTO product "
                    "(name, description, price, stock, is_available, image_url, category_id, merchant_id) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"
                )
                cur.execute(
                    sql,
                    (
                        pname,
                        str(row["description"]),
                        str(row["price"]),
                        int(row["stock"]),
                        1 if bool(row.get("isAvailable", True)) else 0,
                        image_url,
                        int(row["categoryId"]),
                        mid,
                    ),
                )
                imported += 1

        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"Import failed: {e}", file=sys.stderr)
        return 1
    finally:
        conn.close()

    print(f"Done: imported {imported} products, skipped {skipped}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
