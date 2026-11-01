# Open-Source Repositories Audit Report

This document audits the exact open-source candidates investigated for the Circle to Search for Xiaomi engineering effort.

---

## 1. `h3nc4/HyperGesture`
- **URL**: `https://github.com/h3nc4/HyperGesture`
- **License**: GNU General Public License v3.0 (GPL-3.0)
- **Primary Strength**: Elegant solution to Xiaomi HyperOS gesture limitations. Employs a calibrated `TYPE_ACCESSIBILITY_OVERLAY` edge zone that intercepts gestures before the window manager dismisses them.
- **Weakness**: Focused solely on navigation gesture restoration (Back/Home/Recents), not visual search or screenshot management.
- **Adopted Concept**: Bottom-center touch interception layout, overlay flags (`FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`), and stationary hold discrimination.

---

## 2. `parallelcc/MiCTS`
- **URL**: `https://github.com/parallelcc/MiCTS`
- **License**: GPL-3.0 / Open Source
- **Primary Strength**: Extensive exploration of Google's Omni/Contextual Search entry points (`CSHelper`, `CSService`, `VoiceInteractionService`).
- **Weakness**: Relies on LSPosed framework and Xposed hook (`Device spoof for Google`) to bypass Google's hardware whitelist. On stock unrooted HyperOS China ROM, the native trigger is ignored by Google App.
- **Adopted Concept**: Discovery of fallback Google Lens share entry points (`LensShareEntryPointActivity` and `SEARCH_IMAGE` action).

---

## 3. `chenxiaolong/CTSLauncher`
- **URL**: `https://github.com/chenxiaolong/CTSLauncher`
- **License**: GPL-3.0
- **Primary Strength**: Zero-permission launcher and Quick Settings tile implementation.
- **Weakness**: Merely dispatches an intent to Google's contextual search. Completely non-functional if Google disables the feature on the device.
- **Adopted Concept**: Clean Quick Settings tile configuration and assistant routing metadata.

---

## 4. `AKS-Labs/CircleToSearch`
- **URL**: `https://github.com/AKS-Labs/CircleToSearch`
- **License**: GNU General Public License v3.0 (GPL-3.0)
- **Primary Strength**: Most complete open-source visual search implementation. Includes multi-engine search (Google Lens, Bing, Yandex, TinEye, AI search), offline OCR, and QR code detection.
- **Weakness**: Relies on standard assistant invocation or manual quick tile rather than native Xiaomi bottom gesture interception.
- **Adopted Concept**: Multi-engine visual search provider abstraction, offline crop processing, and text/QR recognition pipeline.

---

## 5. `quantavil/QuickLens` & `Bankairim/DirectLens`
- **URLs**: `https://github.com/quantavil/QuickLens`, `https://github.com/Bankairim/DirectLens`
- **License**: GPL-3.0
- **Primary Strength**: Lightweight Jetpack Compose overlay and local Room search history.
- **Weakness**: Static screenshot handling and limited Xiaomi HyperOS specific gesture calibration.
- **Adopted Concept**: Intent flag optimization (`FLAG_GRANT_READ_URI_PERMISSION`, `ClipData` URI passing).

