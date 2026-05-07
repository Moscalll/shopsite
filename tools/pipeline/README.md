# 自动化上新流水线（读取已有 + LLM 新品 + 生成 prompt + 入库）

## 前置

1. `tools/.env`：配置 `MYSQL_*`（与 `db_import` 相同）。
2. Python 依赖：在 `tools` 目录执行 `pip install -r requirements.txt`（**已不包含** `llama-cpp-python`，避免 Windows 上源码编译失败）。
3. **推荐：Ollama（本机已安装时）**  
   - 拉取与 `config.yaml` 中 `remote_model` 一致的模型，例如：  
     `ollama pull qwen2.5:3b`  
   - 保持 Ollama 在默认端口 **11434** 运行。  
   - `config.example.yaml` 默认已使用：  
     `backend: http`、`base_url: http://127.0.0.1:11434/v1`、`remote_model: qwen2.5:3b`  
   - 你磁盘上的 **GGUF 文件不必再给 Python 用**；若一定要用那份 GGUF，可在 Ollama 里用 `Modelfile` 从该路径建模型，或换用下文的 **llama-server**。

4. `pipeline/config.yaml`：从 `config.example.yaml` 复制，按需改 `merchant_id`、`allowed_category_ids`、`llm.remote_model` 等。

### Windows 上 `pip install llama-cpp-python` 失败（C4819 / C2001 / CMake failed）

这是 **MSVC 在临时目录编译 llama.cpp** 时，源文件 UTF-8 与系统代码页混用导致的典型问题；**不要强装**。请改用 **Ollama HTTP** 或 **llama-server 的 OpenAI 兼容 API**（见上）。仅当你能成功安装预编译 wheel 时，才可选：

`pip install -r requirements-optional-gguf.txt`

## 一键顺序脚本

在 `tools` 目录：

```powershell
.\pipeline\run_all.ps1
```

等价手动步骤见下文「等价手动步骤」。

## 一键上新（含确认停顿 + 控制数量）

脚本：**[`batch_new_products.ps1`](batch_new_products.ps1)**（在 `tools` 目录执行）

- **`-MaxProducts N`**：最多生成 **N** 条 LLM 商品（传给 `generate_candidates_llm.py --max-products`）。与 `config.yaml` 里 `items_per_category` × `allowed_category_ids` 的关系：**`--max-products` 为硬上限**，循环仍按配置顺序走，写满 `N` 条即停；若配置理论条数不足 `N`，则写满理论条数为止。
- **`-SkipRender`**：跳过 ComfyUI / `render_batch.py`。
- **`-SkipImport`**：跳过 `import_products.py`（脚本末尾会打印手动导入命令）。
- **`-NonInteractive`**：不在 prompts 与出图后 `Read-Host` 暂停。

示例（生成 5 条、默认会停两次确认后再出图、再导入）：

```powershell
cd D:\GitHub\shopsite\tools
.\pipeline\batch_new_products.ps1 -MaxProducts 5
```

无人值守（需已确认配置与 ComfyUI 可用）：

```powershell
.\pipeline\batch_new_products.ps1 -MaxProducts 10 -NonInteractive
```

依赖：**`tools/.env`**、**`pipeline/config.yaml`**、出图需 **`image_render/config.yaml`** + 本机 ComfyUI（或 Pollinations 等）；导入需 **`pip install -r requirements.txt`**（含 `pymysql`）。

## 运行时商品图与 Git

批量导入的商品图落在 Spring 默认 **`uploads/`**（与 `SHOPSITE_UPLOAD_DIR` 一致）。仓库根 **`.gitignore` 已忽略 `uploads/*`**，仅保留 **`uploads/.gitkeep`** 占位目录，避免把大量生成图提交进 Git。

## 改用 llama.cpp 自带服务（非 Ollama）

在 `config.yaml` 中设置与 `llama-server` 一致的地址，例如：

```yaml
llm:
  backend: http
  base_url: "http://127.0.0.1:8080/v1"
  remote_model: "..."  # 与启动参数一致
```

## 等价手动步骤

1. `python pipeline/fetch_existing.py --config pipeline/config.yaml`  
   → 写出 `out/pipeline_run/categories.json`、`out/pipeline_run/existing.json`
2. `python pipeline/generate_candidates_llm.py --config pipeline/config.yaml`  
   （可选上限：`--max-products 5`）  
   → 写出 `out/pipeline_run/product_spec.auto.jsonl`
3. `python product_pipeline/generate.py --in out/pipeline_run/product_spec.auto.jsonl --out-dir out/pipeline_run/generated`  
   → `prompts.jsonl`、`catalog.jsonl`
4. **自动出图**（推荐）：`python image_render/render_batch.py`（见 [`image_render/README.md`](../image_render/README.md)）；或设置 **`SHOPSITE_AUTO_RENDER=1`** 后运行 `run_all.ps1` 会尝试执行本步。需配置 `POLLINATIONS_API_KEY` 或 ComfyUI `workflow_json`。
5. `python db_import/import_products.py --catalog out/pipeline_run/generated/catalog.jsonl --images-dir out/pipeline_run/generated/images --skip-if-name-exists`

## 故障排查

- **连接 Ollama 失败**：确认 `ollama serve` 在运行且 `http://127.0.0.1:11434` 可访问；`ollama list` 中已有 `remote_model` 对应模型。
- **`remote_model` 与已装模型名不一致**：把 `config.yaml` 里 `remote_model` 改成 `ollama list` 里显示的名称（区分大小写与 tag）。
- **模型输出非 JSON**：降低 `temperature` 或保持 `items_per_category: 1`；查看终端里脚本打印的原始输出。
- **每条都像「纯棉 T 恤」**：脚本已按类目名识别是否服饰类，并在非服饰类目下禁止服装主体 + 轮换「多样性提示」。若类目名本身含「服饰/鞋」等仍总撞款，可把 `llm.temperature` 提到 `0.8` 左右或换更大模型；并务必每次先 `fetch_existing.py` 再生成。
- **名称仍与已有冲突**：导入时使用 `--skip-if-name-exists`。
