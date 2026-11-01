# License Audit Report

## 1. Primary License
This application is distributed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)** to comply with upstream architectural derivatives and open-source dependencies.

---

## 2. Third-Party Code Usage & Compatibility Matrix

| Project | Author | License | Usage in Circle to Search for Xiaomi | Compatible with GPL-3.0? |
| :--- | :--- | :--- | :--- | :---: |
| **HyperGesture** | Henrique Almeida (`h3nc4`) | GPL-3.0-or-later | Bottom pill overlay architecture & gesture state machine concepts | Yes |
| **MiCTS** | `parallelcc` | GPL-3.0 | Intent analysis & Google App package interaction research | Yes |
| **CTSLauncher** | Andrew Gunnerson (`chenxiaolong`) | GPL-3.0 | Quick Settings tile interaction design | Yes |
| **AKS CircleToSearch** | AKS-Labs | GPL-3.0 | Multi-engine search concept & UI cropping patterns | Yes |
| **Android Jetpack** | Google LLC | Apache-2.0 | Core, AppCompat, Material, WindowManager | Yes (Permissive) |

---

## 3. Compliance Action Items
1. Retain full GPL-3.0 text in root `LICENSE`.
2. Provide explicit copyright and attribution notices in `THIRD_PARTY_NOTICES.md`.
3. Ensure no proprietary Samsung, Google, or Xiaomi binary blobs are embedded.
4. Guarantee corresponding source code is available in the public repository.

