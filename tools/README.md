# shopsite 批量上新工具链（`tools/`）

## 定位（上新专用）

- **作用**：为 **网站目前还没有的商品** 批量生成主图提示词与目录数据，并通过 **`INSERT`** 写入数据库，用于 **上新**。
- **不会**：自动读取或对照当前已上架商品列表；`product_spec.jsonl` 里写什么，就只生成/导入这些 **新 SKU**。
- **避免误重复**：若担心 `name` 与某商户下已有商品重名，导入时请加 **`--skip-if-name-exists`**（同名同商户则跳过该行，不插入、不拷图）。

本目录**不修改**站点 Java/模板代码，只提供：

1. **提示词 + 商品目录生成**：`product_pipeline/generate.py`
2. **（人工/Colab）出图**：见 `comfyui/README.md`
3. **写入 MySQL + 拷贝图片到 `uploads/`**：`db_import/import_products.py`

## 前台展示条件（写入前请满足）

站点前台「可售」商品常见条件为 **`is_available = true` 且 `stock > 0`**（见 `ProductRepository`）。  
导入脚本默认：`isAvailable=true`，请保证 **`stock >= 1`**。

## 快速开始

```powershell
cd D:\GitHub\shopsite\tools
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

### 1）准备 `product_spec.jsonl`

参考 `samples/product_spec.sample.jsonl`（最短示例），或 **`samples/product_spec.shop467_8categories.jsonl`**（同一商户、8 个分类各一条，含完整英文 `fullPromptEn`）。

若每条已有**整段英文 prompt**（按分类固定），在 JSON 里写 **`fullPromptEn`**，生成脚本会原样采用（并默认追加一段防包装文字的英文后缀，可用 `appendSdSafetySuffix: false` 关闭）。

**英文主图风格（默认）**：`stylePreset` 默认为 `english_minimal_wood_table_3x4`（浅灰绿背景、白木台面、正视 straight-on，与你历史英文 prompt 一致）。每条建议填写：

- **`productDetailEn`**：英文商品短语（例如 `a clear glass pour-over coffee carafe with a wood collar`）
- **`showFrontLabel`**：`true` 时会在 prompt 中加入 `showing the full front label`（适合纸盒包装）
- **`width` / `height`**：可选；不设则用 preset 的 **768×1024**

若省略 `productDetailEn`，脚本会用 `a {productType}` 兜底（请尽量填英文 `productDetailEn` 以保证质量）。

### 2）生成 `prompts.jsonl` + `catalog.jsonl`

```powershell
python product_pipeline\generate.py --in samples\product_spec.sample.jsonl --out-dir out\run1
```

**生成图片文件名格式**：默认 **`webp`**（与站内示例静态图常用 `.webp`、上传接口也支持 `image/webp` 一致）。  
可调：`--image-format png`，或在每条 spec 里写 `"outputFormat": "png"`。ComfyUI 导出时请与 **`imageBasename` 后缀一致**（例如导出为 `demo-001.webp`）。

### 3）出图（自动化推荐）

使用 **[`image_render/render_batch.py`](image_render/README.md)** 读取 `prompts.jsonl`，调用 **Pollinations**（需 `POLLINATIONS_API_KEY`）或 **本机 ComfyUI HTTP API** 批量保存到 `images/`。

```powershell
$env:POLLINATIONS_API_KEY="你的key"
python image_render\render_batch.py --prompts out\pipeline_run\generated\prompts.jsonl --images-dir out\pipeline_run\generated\images --config image_render\config.yaml
```

一键流水线中若设置 **`$env:SHOPSITE_AUTO_RENDER="1"`** 再运行 `.\pipeline\run_all.ps1`，会在生成 `prompts.jsonl` 后自动执行出图（需事先配置 `image_render\config.yaml`）。

**交互式一键上新（推荐）**：在 `tools` 目录运行 **`.\pipeline\batch_new_products.ps1 -MaxProducts <数量>`**，顺序为拉库 → LLM → `prompts.jsonl`/`catalog.jsonl` → 暂停确认 → 出图 → 暂停确认 → 导入；参数说明见 [`pipeline/README.md`](pipeline/README.md)。

仍可选用 Colab 手动 ComfyUI；文件名须与 `catalog.jsonl` 的 `imageFilename` 一致。

### 4）导入数据库 + 拷贝图片

复制 `tools\.env.example` 为 `tools\.env` 并填写连接信息（**勿提交 .env**）。

```powershell
python db_import\import_products.py --catalog out\run1\catalog.jsonl --images-dir out\run1\images
```

上新时建议（可选）避免与已有商品同名：

```powershell
python db_import\import_products.py --catalog out\run1\catalog.jsonl --images-dir out\run1\images --skip-if-name-exists
```

常用参数：

- `--upload-dir`：站点运行时图片目录（默认读环境变量 `SHOPSITE_UPLOAD_DIR`，否则 `./uploads`）
- `--dry-run`：只校验并打印 SQL，不写库、不拷贝
- `--skip-if-name-exists`：同一 `merchantId` 下若 **`name` 已在 `product` 表存在**，则跳过该行（不上新）

## 环境变量

见 `tools/.env.example`。

---

## 完整自动化上新（读取已有 + LLM + prompt + 入库）

使用 **[`pipeline/`](pipeline/README.md)**：

1. 将 `pipeline/config.example.yaml` 复制为 `pipeline/config.yaml`（已加入 .gitignore）。**默认推荐 Ollama**：先 `ollama pull qwen2.5:3b`，配置里使用 `backend: http` 与 `base_url: http://127.0.0.1:11434/v1`（无需安装 `llama-cpp-python`）。若坚持用本机 GGUF 进程内加载，见 `requirements-optional-gguf.txt`（Windows 上常编译失败，不推荐）。
2. **一键（带数量与确认）**：在 `tools` 目录执行 **`.\pipeline\batch_new_products.ps1 -MaxProducts 8`**；或分步执行 **`.\pipeline\run_all.ps1`**（若缺少 `config.yaml` 脚本会从 example 自动复制一份）。
   - 从 MySQL 拉取该商户已有商品名（去重）与分类表  
   - LLM 按分类生成新品 `product_spec.auto.jsonl`（`--max-products` 控制条数上限）  
   - 调用现有 `generate.py` 生成 `prompts.jsonl` / `catalog.jsonl`  
3. ComfyUI 出图后放入 `out/pipeline_run/generated/images/`（`batch_new_products.ps1` 会调用 `render_batch.py`，除非 `-SkipRender`）  
4. `python db_import\import_products.py ... --skip-if-name-exists`（`batch_new_products.ps1` 默认包含此步，除非 `-SkipImport`）

**Git**：仓库根 `.gitignore` 已忽略 **`uploads/*`**（运行时导入的商品图），保留 **`uploads/.gitkeep`** 以占位空目录。

详见 [`pipeline/README.md`](pipeline/README.md)。