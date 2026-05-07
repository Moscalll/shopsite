#!/usr/bin/env python3
"""
读取 prompts.jsonl，批量文生图，写入 images 目录（文件名 = imageBasename）。

后端:
  - pollinations_get: GET https://gen.pollinations.ai/image/{prompt}?width&height&seed&model&negative_prompt
  - pollinations_openai: POST /v1/images/generations (b64_json)，需 POLLINATIONS_API_KEY
  - comfyui: POST ComfyUI /prompt + 轮询 /history

用法:
  python render_batch.py --prompts ../out/pipeline_run/generated/prompts.jsonl --images-dir ../out/pipeline_run/generated/images
  python render_batch.py --prompts ... --images-dir ... --config image_render/config.yaml
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import random
import sys
import time
import uuid
from pathlib import Path
from typing import Any
from urllib.parse import quote

import httpx
import yaml

ROOT = Path(__file__).resolve().parents[1]

# region agent log
_DEBUG_LOG = ROOT.parent / "debug-9c611e.log"
_SESSION = "9c611e"


def _agent_log(hypothesis_id: str, message: str, data: dict) -> None:
    line = json.dumps(
        {
            "sessionId": _SESSION,
            "hypothesisId": hypothesis_id,
            "location": "image_render/render_batch.py",
            "message": message,
            "data": data,
            "timestamp": int(time.time() * 1000),
        },
        ensure_ascii=False,
    )
    try:
        with _DEBUG_LOG.open("a", encoding="utf-8") as f:
            f.write(line + "\n")
    except Exception:
        pass


# endregion agent log


def _load_yaml(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {}
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def _pollinations_headers(api_key: str | None) -> dict[str, str]:
    h = {"User-Agent": "shopsite-tools/1.0"}
    if api_key:
        h["Authorization"] = f"Bearer {api_key}"
    return h


def render_pollinations_get(
    client: httpx.Client,
    base: str,
    row: dict[str, Any],
    cfg: dict[str, Any],
    api_key: str | None,
) -> bytes:
    p = (row.get("prompt") or "").strip()
    if len(p) > 1800:
        p = p[:1800] + "..."
    neg = (row.get("negativePrompt") or "").strip()
    w = int(row.get("width") or 768)
    h = int(row.get("height") or 1024)
    seed = int(row.get("seed") or random.randint(0, 2**31 - 2))
    model = cfg.get("model", "zimage")

    # Path segment must be encoded; query carries params (per Pollinations APIDOCS)
    path_prompt = quote(p, safe="")
    url = f"{base.rstrip('/')}/image/{path_prompt}"
    params: dict[str, Any] = {
        "width": w,
        "height": h,
        "seed": seed,
        "model": model,
        "nologo": "true",
    }
    if neg:
        params["negative_prompt"] = neg

    r = client.get(url, params=params, headers=_pollinations_headers(api_key), timeout=300.0)
    if r.status_code == 401 and not api_key:
        raise RuntimeError(
            "Pollinations returned 401. Set env POLLINATIONS_API_KEY (get key at https://enter.pollinations.ai ) "
            "or try backend=comfyui for fully local free generation."
        )
    r.raise_for_status()
    return r.content


def render_pollinations_openai(
    client: httpx.Client,
    base: str,
    row: dict[str, Any],
    cfg: dict[str, Any],
    api_key: str,
) -> bytes:
    w = int(row.get("width") or 768)
    h = int(row.get("height") or 1024)
    seed = int(row.get("seed") or random.randint(0, 2**31 - 2))
    model = cfg.get("model", "zimage")
    size = f"{w}x{h}"
    body = {
        "model": model,
        "prompt": (row.get("prompt") or "").strip(),
        "n": 1,
        "size": size,
        "response_format": "b64_json",
        "seed": seed,
    }
    neg = (row.get("negativePrompt") or "").strip()
    if neg:
        body["negative_prompt"] = neg

    url = f"{base.rstrip('/')}/v1/images/generations"
    r = client.post(
        url,
        json=body,
        headers={**_pollinations_headers(api_key), "Content-Type": "application/json"},
        timeout=300.0,
    )
    r.raise_for_status()
    data = r.json()
    b64 = data["data"][0]["b64_json"]
    return base64.b64decode(b64)


def _find_node_by_class(workflow: dict, class_type: str) -> str | None:
    for nid, node in workflow.items():
        if not isinstance(node, dict):
            continue
        if node.get("class_type") == class_type:
            return str(nid)
    return None


def _patch_comfy_workflow(
    workflow: dict[str, Any],
    row: dict[str, Any],
    nodes_cfg: dict[str, str],
) -> dict[str, Any]:
    wf = json.loads(json.dumps(workflow))
    prompt = (row.get("prompt") or "").strip()
    neg = (row.get("negativePrompt") or "").strip()
    w = int(row.get("width") or 768)
    h = int(row.get("height") or 1024)
    seed = int(row.get("seed") or 0)
    basename = row.get("imageBasename") or "out.png"

    np = nodes_cfg.get("node_positive") or _find_node_by_class(wf, "CLIPTextEncode")
    nn = nodes_cfg.get("node_negative") or None
    nl = nodes_cfg.get("node_latent") or _find_node_by_class(wf, "EmptyLatentImage")
    ns = nodes_cfg.get("node_sampler") or _find_node_by_class(wf, "KSampler")

    def _patch_clip_node(nid: str, text: str) -> None:
        if not nid or nid not in wf:
            return
        node = wf[nid]
        inp = node.setdefault("inputs", {})
        if node.get("class_type") == "CLIPTextEncodeSDXL" or "text_g" in inp:
            inp["text_g"] = text
            inp["text_l"] = text
            if "width" in inp:
                inp["width"] = w
            if "height" in inp:
                inp["height"] = h
            if "target_width" in inp:
                inp["target_width"] = w
            if "target_height" in inp:
                inp["target_height"] = h
        else:
            inp["text"] = text

    if np:
        _patch_clip_node(np, prompt)
    if nn:
        _patch_clip_node(nn, neg)
    if nl and nl in wf:
        wf[nl].setdefault("inputs", {})["width"] = w
        wf[nl].setdefault("inputs", {})["height"] = h
    if ns and ns in wf:
        wf[ns].setdefault("inputs", {})["seed"] = seed

    # SaveImage filename_prefix / filename — structure varies; try common keys
    save_id = _find_node_by_class(wf, "SaveImage")
    if save_id:
        stem = Path(str(basename)).stem
        inp = wf[save_id].setdefault("inputs", {})
        if "filename_prefix" in inp or any(k for k in inp):
            inp["filename_prefix"] = stem
    return wf


def render_comfyui(
    client: httpx.Client,
    base: str,
    row: dict[str, Any],
    workflow_path: Path,
    nodes_cfg: dict[str, str],
) -> bytes:
    """Queue prompt on ComfyUI, poll /history, download first output image."""
    wf_raw = json.loads(workflow_path.read_text(encoding="utf-8"))
    # API format may be {"prompt": {...}} or raw dict of nodes
    if "prompt" in wf_raw and isinstance(wf_raw["prompt"], dict):
        wf = wf_raw["prompt"]
    else:
        wf = wf_raw

    wf = _patch_comfy_workflow(wf, row, nodes_cfg)
    client_id = str(uuid.uuid4())
    pr_url = f"{base.rstrip('/')}/prompt"
    max_retries = 5
    last_status = 0
    last_body = ""
    r: httpx.Response | None = None
    for attempt in range(max_retries):
        try:
            r = client.post(pr_url, json={"prompt": wf, "client_id": client_id}, timeout=120.0)
        except httpx.RequestError as e:
            # region agent log
            _agent_log(
                "H2",
                "comfyui_prompt_request_error",
                {"attempt": attempt, "error": repr(e), "url": pr_url},
            )
            # endregion agent log
            if attempt + 1 >= max_retries:
                raise
            time.sleep(2.0 * (attempt + 1))
            continue
        last_status = r.status_code
        if r.status_code in (502, 503, 504):
            try:
                last_body = (r.text or "")[:1200]
            except Exception:
                last_body = ""
            # region agent log
            _agent_log(
                "H1",
                "comfyui_prompt_transient_http",
                {
                    "attempt": attempt,
                    "status_code": r.status_code,
                    "body_prefix": last_body[:500],
                    "workflow_nodes": len(wf) if isinstance(wf, dict) else 0,
                },
            )
            # endregion agent log
            if attempt + 1 < max_retries:
                time.sleep(2.0 * (attempt + 1))
                continue
            # region agent log
            _agent_log(
                "H3",
                "comfyui_prompt_final_transient_http",
                {
                    "attempt": attempt,
                    "status_code": r.status_code,
                    "body_prefix": last_body[:800],
                },
            )
            # endregion agent log
        r.raise_for_status()
        # region agent log
        _agent_log(
            "H_ok",
            "comfyui_prompt_ok",
            {"attempt": attempt, "status_code": r.status_code, "runId": "post-fix"},
        )
        # endregion agent log
        break
    assert r is not None
    data = r.json()
    prompt_id = data.get("prompt_id")
    if not prompt_id and isinstance(data.get("result"), dict):
        prompt_id = data["result"].get("prompt_id")
    if data.get("node_errors"):
        raise RuntimeError(f"ComfyUI node_errors: {data.get('node_errors')}")
    if not prompt_id:
        raise RuntimeError(f"No prompt_id in ComfyUI response: {data}")

    hist_base = f"{base.rstrip('/')}/history"
    for _ in range(600):
        time.sleep(0.5)
        hr = client.get(hist_base, timeout=60.0)
        if hr.status_code != 200:
            continue
        hist = hr.json()
        entry = hist.get(prompt_id) if isinstance(hist, dict) else None
        if not entry:
            continue
        out = entry.get("outputs") or {}
        for _nid, odata in out.items():
            images = (odata or {}).get("images") or []
            for img in images:
                fn = img.get("filename")
                sub = img.get("subfolder", "")
                typ = img.get("type", "output")
                if not fn:
                    continue
                vu = f"{base.rstrip('/')}/view"
                gr = client.get(vu, params={"filename": fn, "subfolder": sub, "type": typ}, timeout=120.0)
                gr.raise_for_status()
                return gr.content
    raise TimeoutError("ComfyUI generation timed out")


def _save_image_bytes(data: bytes, dest: Path, want_ext: str) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    want_ext = want_ext.lower()
    if want_ext == ".webp":
        try:
            from PIL import Image
            import io

            im = Image.open(io.BytesIO(data)).convert("RGB")
            im.save(dest, format="WEBP", quality=88)
            return
        except Exception:
            pass
    dest.write_bytes(data)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prompts", type=Path, required=True, help="prompts.jsonl from generate.py")
    parser.add_argument("--images-dir", type=Path, required=True, help="Output directory for images")
    parser.add_argument("--config", type=Path, default=ROOT / "image_render" / "config.yaml")
    parser.add_argument("--limit", type=int, default=0, help="Max rows (0 = all)")
    args = parser.parse_args()

    cfg_all = _load_yaml(args.config) if args.config.is_file() else {}
    backend = (cfg_all.get("backend") or "pollinations_get").lower()
    delay = float(cfg_all.get("delay_seconds") or 2.0)
    pol = cfg_all.get("pollinations") or {}
    base = pol.get("base_url") or "https://gen.pollinations.ai"
    api_key = os.environ.get("POLLINATIONS_API_KEY", "").strip() or None
    if pol.get("use_auth") and not api_key and backend.startswith("pollinations"):
        print(
            "Warning: pollinations.use_auth is true but POLLINATIONS_API_KEY is empty. "
            "If requests fail with 401, set the key from https://enter.pollinations.ai",
            file=sys.stderr,
        )

    comfy = cfg_all.get("comfyui") or {}
    comfy_base = comfy.get("base_url", "http://127.0.0.1:8188")
    wf_rel = comfy.get("workflow_json")
    wf_path = ROOT / wf_rel if wf_rel else None

    rows: list[dict[str, Any]] = []
    with args.prompts.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    if args.limit > 0:
        rows = rows[: args.limit]

    args.images_dir.mkdir(parents=True, exist_ok=True)

    with httpx.Client() as client:
        for i, row in enumerate(rows):
            name = row.get("imageBasename")
            if not name:
                print(f"Row {i}: missing imageBasename", file=sys.stderr)
                return 2
            dest = args.images_dir / str(name)
            want_ext = dest.suffix.lower() or ".png"

            print(f"[{i+1}/{len(rows)}] {name} ({backend})...")
            try:
                if backend == "pollinations_get":
                    data = render_pollinations_get(client, base, row, pol, api_key)
                elif backend == "pollinations_openai":
                    if not api_key:
                        print("pollinations_openai requires POLLINATIONS_API_KEY", file=sys.stderr)
                        return 2
                    data = render_pollinations_openai(client, base, row, pol, api_key)
                elif backend == "comfyui":
                    if not wf_path or not wf_path.is_file():
                        print(f"comfyui requires workflow_json file: {wf_path}", file=sys.stderr)
                        return 2
                    data = render_comfyui(client, comfy_base, row, wf_path, comfy)
                else:
                    print(f"Unknown backend: {backend}", file=sys.stderr)
                    return 2

                _save_image_bytes(data, dest, want_ext)

            except Exception as e:
                print(f"Row {i} failed: {e}", file=sys.stderr)
                return 1

            if i < len(rows) - 1 and delay > 0:
                time.sleep(delay)

    print(f"Wrote {len(rows)} images to {args.images_dir.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
