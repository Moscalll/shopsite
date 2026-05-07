# ComfyUI 工作流（API 格式）

1. 在本机打开 ComfyUI，搭好 SDXL（或你用的底模）+ CLIP 编码 + Empty Latent + KSampler + Save Image。
2. 菜单选择 **Save (API Format)**，将 JSON 保存为 **`exported_api.json`** 放到本目录。
3. 在 `image_render/config.yaml` 里填写 **`node_positive` / `node_negative` / `node_latent` / `node_sampler`** 为 JSON 里对应节点的 **字符串 id**（例如 `"6"`）。
4. 启动 ComfyUI 时建议：`python main.py --listen 127.0.0.1 --port 8188`（或默认端口），保证 `http://127.0.0.1:8188` 可访问。

`render_batch.py` 会写入：

- `CLIPTextEncode` 的 `inputs.text`（正/负）
- `EmptyLatentImage` 的 `width` / `height`
- `KSampler` 的 `inputs.seed`

若你的图里节点 class 不同，请相应改 `render_batch.py` 里的节点类型检测逻辑，或把节点 id 配成与脚本一致的结构。
