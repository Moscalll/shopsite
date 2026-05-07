# ComfyUI 在 Windows 上常见问题

## `OSError: [WinError 127] 找不到指定的程序`（加载 torchaudio 时）

**含义**：`torchaudio` 自带的原生扩展 DLL 与当前 **PyTorch / CUDA 运行时** 不匹配，或混用了不同来源安装的包。

**常见原因**：

1. 使用了 **全局 Python**（例如 `D:\Python\`）里自己装的 `torch` / `torchaudio`，与 ComfyUI 仓库 `requirements.txt` 期望的版本不一致。
2. 只升级了 `torch` 没有同步升级 `torchaudio`（或相反）。

**推荐修复（优先）**：在 **ComfyUI 仓库根目录** 使用独立虚拟环境，只按官方依赖安装：

```powershell
cd D:\GitHub\comfyUI
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -U pip
pip install -r requirements.txt
python main.py --listen 127.0.0.1 --port 8188
```

**不要用**未激活 venv 时的 `D:\Python\...`，除非你确认那里的 torch/torchaudio 与 ComfyUI 完全一致。

**仍失败时**：在同一 venv 内强制重装与 CUDA 12.6 对齐的官方轮子（版本以 PyTorch 官网为准）：

```powershell
pip uninstall torch torchvision torchaudio -y
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu126
pip install -r requirements.txt
```

## `Windows fatal exception: code 0xc0000139`

常与 **DLL 入口点找不到** 同类问题一起出现；按上一节处理 **torchaudio / torch** 后多可消失。

## `AssertionError: Torch not compiled with CUDA enabled`

**含义**：当前 venv 里的 **PyTorch 是 CPU 版**（或未带 CUDA 的构建），ComfyUI 在 `model_management` 里会访问 `torch.cuda`，因此直接崩溃。

**常见原因**：在 Windows 上仅用 `pip install torch` 时，默认从 PyPI 拉到的常是 **CPU wheel**，不会出现 `+cu126` 等后缀。

**修复**（在 **已激活** 的 ComfyUI `.venv` 中执行）：

```powershell
pip uninstall torch torchvision torchaudio -y
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu126
pip install -r requirements.txt
```

然后用同一 venv 再运行 `python main.py ...`。若 `python -c "import torch; print(torch.__version__, torch.version.cuda)"` 中第二项为 **None**，说明仍是 CPU 版，需检查是否误用了别的解释器或未加 `--index-url`。

## `WARNING: You need pytorch with cu130 or higher...`

多为 **ComfyUI 新特性/可选加速** 的提示；你当前为 **cu126** 仍可运行 SDXL，不必强行升到 cu130，除非你需要该优化路径。

## 诊断脚本（写入 shopsite 调试日志）

在 **任意** 你打算用来启动 ComfyUI 的 Python 下执行：

```powershell
cd D:\GitHub\shopsite\tools
python comfyui\diagnose_pytorch.py
```

会在仓库根目录生成/追加 `debug-9c611e.log`（NDJSON）。若 `torchaudio_import_fail`，说明该环境不应直接跑 `main.py`，应先修好 venv。
