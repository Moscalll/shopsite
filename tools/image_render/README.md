# 批量文生图（`render_batch.py`）

从 `prompts.jsonl` 逐行出图，保存为 **`imageBasename`**，供 `db_import/import_products.py` 使用。

## 后端选择

### 1) `pollinations_get`（默认，云端 API）

- 端点：`https://gen.pollinations.ai/image/{urlencoded_prompt}?width&height&seed&model&negative_prompt`
- **说明**：Pollinations 当前文档要求多数生成请求携带 **API Key**（[enter.pollinations.ai](https://enter.pollinations.ai) 注册，可用免费额度策略以官网为准）。
- 环境变量：`POLLINATIONS_API_KEY=sk_...` 或 `pk_...`
- 配置：复制 [`config.example.yaml`](config.example.yaml) 为 `config.yaml`，可调 `model`（如 `zimage`）、`delay_seconds`。

```powershell
cd D:\GitHub\shopsite\tools
$env:POLLINATIONS_API_KEY="你的key"
python image_render\render_batch.py --prompts out\pipeline_run\generated\prompts.jsonl --images-dir out\pipeline_run\generated\images --config image_render\config.yaml
```

### 2) `pollinations_openai`

- `POST /v1/images/generations`，`response_format: b64_json`
- **必须**设置 `POLLINATIONS_API_KEY`。

在 `config.yaml` 中设置：`backend: pollinations_openai`。

### 3) `comfyui`（本机免费，需显卡）

1. 启动 ComfyUI：`--listen` 使 `http://127.0.0.1:8188` 可访问。
2. 导出 API 格式工作流到 `image_render/workflows/exported_api.json`（见 [workflows/README.md](workflows/README.md)）。
3. `config.yaml`：`backend: comfyui`，并填写 `node_positive` 等与 JSON 中节点 id 一致。

## WebP

若 `imageBasename` 以 `.webp` 结尾，脚本会尽量用 **Pillow** 将 PNG/JPEG 转为 WebP；失败则直接写入原始字节（可能扩展名与内容不一致，导入前请检查）。

## 与流水线串联

见根目录 [`README.md`](../README.md) 与 [`pipeline/run_all.ps1`](../pipeline/run_all.ps1) 的可选「自动出图」环境变量。
