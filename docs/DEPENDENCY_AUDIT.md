# Dependency Audit & Supply Chain Review

## 1. Direct Production Dependencies
- `androidx.core:core-ktx:1.15.0`: Standard Android KTX primitives. Free of known CVEs.
- `androidx.appcompat:appcompat:1.7.0`: Backward compatible Material components.
- `com.google.android.material:material:1.12.0`: Material 3 components and dynamic themes.
- `androidx.constraintlayout:constraintlayout:2.2.0`: Modern responsive layout primitives.

## 2. Supply Chain Verification
- All dependencies sourced exclusively from official Google Maven (`maven.google.com`) and Maven Central (`repo1.maven.org`).
- No untrusted third-party ad networks, trackers, or closed binary SDKs.
- Gradle dependency verification configured to validate checksums and signatures.

