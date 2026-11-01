# Release & Verification Checklist (RELEASE_CHECKLIST)

**Project Name:** `Circle-to-Search-for-Xiaomi`  
**Application ID:** `dev.shovo.circlesearch`  
**Target Hardware:** Xiaomi 13 (`2211133C`, `fuxi`)  
**Firmware:** HyperOS 3 China ROM (`OS3.0.309.0.WMCCNXM`, Android 16 / API 36)  

---

## 1. Static Verification & Architecture Checklist
- [x] **Canonical Directory Confirmed**: `D:\CIRCLE TO SEARCH FOR XIAOMI` authoritative.
- [x] **Redundant Duplicate Resolved**: Redundant folder safely moved to Windows Recycle Bin.
- [x] **Absolute Project Identity Preserved**: Name `Circle to Search for Xiaomi`, ID `dev.shovo.circlesearch`.
- [x] **Open-Source Benchmarks Audited**: `h3nc4/HyperGesture`, `parallelcc/MiCTS`, `chenxiaolong/CTSLauncher`, `AKS-Labs/CircleToSearch`, `quantavil/QuickLens`.
- [x] **Request-Scoped Session Architecture Implemented**: `SearchSessionManager` concurrent registry with reference-counted leases; single global mutable bitmap eradicated.
- [x] **Mathematical Coordinate Mapping**: `ivBackground.imageMatrix.invert()` verified; bounds clamped to prevent `createBitmap` crashes.
- [x] **Official AOSP Assistant Framework Implemented**: `ShovoVoiceInteractionService` and `ShovoVoiceSession` with `onHandleScreenshot` integrated.
- [x] **Navigation Delegation Calibrated**: Upward swipes from bottom pill delegate Home / Recents without dead zones.
- [x] **Quick Settings Modernized**: Removed deprecated/restricted `ACTION_CLOSE_SYSTEM_DIALOGS`; updated to `startActivityAndCollapse`.
- [x] **Licensing Compliant**: GPL-3.0-or-later confirmed, `THIRD_PARTY_NOTICES.md` fully documented.
- [x] **Static Security Audit Documented**: Scoped FileProvider, `canRetrieveWindowContent="false"`, permissions minimized to `VIBRATE`.
- [x] **Off-Device Compilation Verified**: `assembleDebug` builds cleanly with 0 compilation errors.

---

## 2. Gated Verification Checklist (Awaiting User Authorization)
- [ ] **Physical Device ADB Connection Authorization**: Pending user approval.
- [ ] **Device Test Execution Authorization**: Pending user approval (Test Suites TS-01 through TS-10 strictly gated).
- [ ] **Device APK Installation Authorization**: Pending user approval.
