# Third-Party Notices

## demucs.cpp

- Project: `sevagh/demucs.cpp`
- Pinned release: `v0.0.4-alpha`
- License: MIT
- Upstream: https://github.com/sevagh/demucs.cpp

The vendoring script copies the upstream `LICENSE` into `app/src/main/cpp/third_party/demucs_cpp/LICENSE`.
No source code is copied from the archived GPL-3.0 `demucs-android` demo application.

## Eigen

Eigen headers required by `demucs.cpp` are vendored from the `demucs.cpp` release submodule. Eigen's license files remain with the vendored distribution where supplied upstream.

## HT-Demucs GGML model

- Artifact: `ggml-htdemucs-4s-f16.bin`
- Source: `ogbabydiesal/demucs-ggml`
- Expected size: `83,994,361` bytes
- SHA-256: `72b17c42d308982ddb5069bc3bf48b81a5aac4cb6516e4366c0fa7cef6df0064`
- Repository license: MIT

The build helper refuses to install a model whose exact byte count or SHA-256 differs from the pinned values.
