#!/usr/bin/env python3
"""检测当前 Python 环境中 torch / torchaudio 是否可加载（ComfyUI 启动失败时常用）。"""
# region agent log
import json
import sys
import time
from pathlib import Path

_LOG = Path(__file__).resolve().parents[2] / "debug-9c611e.log"
_SESSION = "9c611e"


def _log(hypothesis_id: str, message: str, data: dict) -> None:
    line = json.dumps(
        {
            "sessionId": _SESSION,
            "hypothesisId": hypothesis_id,
            "location": "tools/comfyui/diagnose_pytorch.py",
            "message": message,
            "data": data,
            "timestamp": int(time.time() * 1000),
        },
        ensure_ascii=False,
    )
    with _LOG.open("a", encoding="utf-8") as f:
        f.write(line + "\n")


# endregion agent log


def main() -> int:
    exe = sys.executable
    ver = sys.version.split()[0]

    # region agent log
    _log("H_env", "diagnose_start", {"executable": exe, "python_version": ver})
    # endregion agent log

    try:
        import torch

        tv = getattr(torch, "__version__", "?")
        cuda = getattr(torch.version, "cuda", None)
        cuda_avail = torch.cuda.is_available()
        cur_err = None
        try:
            torch.cuda.current_device()
        except Exception as e:
            cur_err = repr(e)
        # region agent log
        _log(
            "H_cuda_build",
            "torch_cuda_probe",
            {
                "torch_version": tv,
                "torch_version_cuda": str(cuda),
                "cuda_is_available": cuda_avail,
                "current_device_error": cur_err,
            },
        )
        # endregion agent log
        # region agent log
        _log("H_torch", "torch_import_ok", {"torch_version": tv, "torch_cuda": str(cuda)})
        # endregion agent log
    except Exception as e:
        # region agent log
        _log("H_torch", "torch_import_fail", {"error": repr(e)})
        # endregion agent log
        print("torch import failed:", e)
        return 1

    try:
        import torchaudio

        tav = getattr(torchaudio, "__version__", "?")
        # region agent log
        _log("H_torchaudio", "torchaudio_import_ok", {"torchaudio_version": tav})
        # endregion agent log
        print("OK: torch", tv, "| torchaudio", tav, "| exe:", exe)
        return 0
    except Exception as e:
        # region agent log
        _log(
            "H_torchaudio",
            "torchaudio_import_fail",
            {"error": repr(e), "torch_version": tv},
        )
        # endregion agent log
        print("FAIL: torchaudio:", e)
        print("Hint: WinError 127 often means torch/torchaudio CUDA wheels mismatch.")
        print("Fix: use ComfyUI's .venv + only pip install -r requirements.txt;")
        print("     or reinstall: pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu126")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
