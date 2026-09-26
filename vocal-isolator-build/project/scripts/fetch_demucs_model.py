#!/usr/bin/env python3
"""Fetch the pinned HT-Demucs f16 GGML model and verify it before install."""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import tempfile
import urllib.request

MODEL_URL = "https://huggingface.co/ogbabydiesal/demucs-ggml/resolve/main/ggml-htdemucs-4s-f16.bin?download=true"
MODEL_SIZE = 83_994_361
MODEL_SHA256 = "72b17c42d308982ddb5069bc3bf48b81a5aac4cb6516e4366c0fa7cef6df0064"
DEFAULT_OUTPUT = Path("app/src/main/assets/models/ggml-htdemucs-4s-f16.bin")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify(path: Path) -> None:
    size = path.stat().st_size
    if size != MODEL_SIZE:
        raise RuntimeError(f"model size mismatch: expected {MODEL_SIZE}, got {size}")
    actual = sha256_file(path)
    if actual != MODEL_SHA256:
        raise RuntimeError(f"model SHA-256 mismatch: expected {MODEL_SHA256}, got {actual}")


def fetch(output: Path, url: str) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        verify(output)
        print(f"verified existing model: {output}")
        return

    fd, tmp_name = tempfile.mkstemp(prefix=output.name + ".", suffix=".tmp", dir=output.parent)
    os.close(fd)
    tmp = Path(tmp_name)
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "Vocal-Isolator-build/1.0"})
        with urllib.request.urlopen(request, timeout=120) as response, tmp.open("wb") as dst:
            shutil.copyfileobj(response, dst, length=1024 * 1024)
        verify(tmp)
        tmp.replace(output)
        print(f"downloaded and verified model: {output}")
    finally:
        tmp.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--url", default=os.environ.get("DEMUCS_MODEL_URL", MODEL_URL))
    args = parser.parse_args()
    fetch(args.output, args.url)


if __name__ == "__main__":
    main()
