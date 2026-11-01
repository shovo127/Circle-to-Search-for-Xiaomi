# Comprehensive Quality Assurance Test Plan (TEST_PLAN)

> **STATUS**: PREPARED ONLY — NOT EXECUTED.
> In accordance with Absolute Execution Rule #3, no tests, test suites, or device experiments will be executed until the user explicitly issues test authorization.

---

## 1. Test Suite Matrix

| Test Suite ID | Domain | Test Objective | Execution Method (Gated) |
| :--- | :--- | :--- | :--- |
| **TS-01** | Gesture Accuracy | Verify 450ms hold triggers overlay; verify quick swipe (<200ms) delegates Home | Automated touch injection (Pending Approval) |
| **TS-02** | Swipe Cancellation | Verify vertical displacement `dy > touchSlop * 3.5f` cancels hold timer immediately | Unit & Motion simulation |
| **TS-03** | Android 16 Screenshot | Verify `takeScreenshot` succeeds and `HardwareBuffer` closes without memory leaks | Profiler & AS Callback validation |
| **TS-04** | Selection Bounds | Verify freeform circle correctly calculates bounding box and scales to physical pixels | Geometric unit tests |
| **TS-05** | Search Provider Dispatch | Verify Intent generation with `ClipData` and `FLAG_GRANT_READ_URI_PERMISSION` | Intent resolution tests |
| **TS-06** | Navigation Integrity | Verify Home, Recents, and Back gestures remain 100% operational in host apps | Manual device protocol |
| **TS-07** | Memory & Lifecycle | Verify overlay destruction releases screenshot bitmap; check for memory leaks | Android Memory Profiler |
| **TS-08** | Multi-Engine Fallback | Verify fallback to secondary search providers if Google App is missing | Component mock tests |
| **TS-09** | Secure Screen Handling | Verify app does not crash when encountering `FLAG_SECURE` banking windows | Secure activity test harness |
| **TS-10** | Rotation & Config | Verify overlay responds correctly to screen orientation changes | Configuration change harness |

---

## 2. Test Execution Prerequisites (Pending User Approval)
- User explicit sign-off on Architecture Decision Records.
- Test phase authorization.
- Device authorization for controlled test execution.

