# APK License & Source Mapping Report

**Audit Target:** Discovered external APK binaries vs. Open-Source Repositories  

---

## 1. Mapping Matrix

| Binary | Source Repository | License | Source Match Confidence | Reusability in Shovo Circle Search |
| :--- | :--- | :--- | :---: | :--- |
| **`MiCTS_2.6.apk`** | `https://github.com/parallelcc/MiCTS` | GPL-3.0 | **High (95%)** | Reference architecture only. Binary excluded. No direct decompiled code copied. |
| **`NotiGuard-v2.1.apk`** | Private / Unmapped | Proprietary / Unspecified | **Low (0%)** | **Strictly Excluded.** Unrelated functionality. |

---

## 2. Licensing Compliance Statement
- **GPL-3.0 Compliance:** Shovo Circle Search adopts architectural concepts from `h3nc4/HyperGesture` and `AKS-Labs/CircleToSearch`. Full legal copyright notices and GPL-3.0 attribution are preserved in `THIRD_PARTY_NOTICES.md`.
- **Clean Implementation:** All source code in `dev.shovo.circlesearch` is authored cleanly for Xiaomi 13 HyperOS 3 without copying decompiled proprietary code.
- **Zero Binary Bundling:** No external APKs (`.apk`) or third-party DEX files are packaged into the output APK of Shovo Circle Search.

