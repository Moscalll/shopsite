# ComfyUI / Colab 批量出图（与 `tools/product_pipeline` 对接）

Windows 启动若报 **WinError 127 / torchaudio**，见 **[TROUBLESHOOTING_WINDOWS.md](TROUBLESHOOTING_WINDOWS.md)**，并运行 **`diagnose_pytorch.py`** 自检。

## 输入 / 输出

- **输入**：`out/<run>/prompts.jsonl`（由 `product_pipeline/generate.py` 生成）
- **输出**：多张图片文件，**文件名必须等于**每行的 `imageBasename`（默认多为 **`*.webp`**；也可能为 `png/jpg`，以后缀为准）

将这些图片保存到与导入脚本一致的目录，例如：

`tools/out/run1/images/<imageBasename>`

然后执行：

`python db_import/import_products.py --catalog tools/out/run1/catalog.jsonl --images-dir tools/out/run1/images`

## 推荐栈（免费 Colab）

1. **Google Colab**（GPU 类型以实际分配为准，常见为 T4）
2. **ComfyUI**（开源节点化工作流）
3. **底模**：**SDXL 1.0**（电商产品图较常用）；显存紧张可换 SD1.5
4. **风格参考**：**IP-Adapter for SDXL**（用你自己的品牌参考图锁风格）

## 工作流要点（检查清单）

- 读取 `prompts.jsonl` 每行字段：`prompt`、`negativePrompt`、`seed`、`imageBasename`，以及 **`width` / `height`**（当前默认 **768×1024**，竖版 3:4）
- 在 ComfyUI 中：将 **Empty Latent Image**（或等价节点）设为该行 **`width` × `height`**，不要依赖 prompt 里的 `--ar`（那是 Midjourney 语法，SD/ComfyUI 一般不解析）
- 关闭随机种子漂移：固定 `seed`，并固定 sampler / steps / cfg
- 底模若用 **SDXL**：768×1024 宽高均为 8 的倍数，通常可正常出图；若显存不足再考虑降到 **640×896** 等，但比例仍建议维持约 3:4
- 负面词：务必包含 `text, watermark, logo`（减少文字水印）
- 批量：用 ComfyUI 的队列/脚本循环写入相同输出目录即可

## 注意

- Colab 免费 GPU **不稳定**：大批量建议拆批 + 本地缓存中间结果。
- 本仓库不内置 `.ipynb`：你可自行保存 Colab Notebook 到本地任意路径（不建议提交大文件到 git）。
