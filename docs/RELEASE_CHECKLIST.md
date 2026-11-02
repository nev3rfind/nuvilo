# Novelo Release Checklist & Build Hardening Verification

**Application:** Novelo (Personal Medication Management)  
**Package:** `com.example.nuvilo`  
**Platform:** Native Android (Kotlin 2.2, Jetpack Compose, Material 3, Room v3)  
**Min SDK:** 24 | **Target/Compile SDK:** 37  
**Build Variant:** `release` (R8 minified, shrunk resources, Proguard rules enforced)  
**Verification Date:** November 2, 2026 (Phase 7.1)

---

## 1. Continuous Integration (GitHub Actions)

A dedicated GitHub Actions CI pipeline is configured in `.github/workflows/ci.yml`:

- **Triggers:** Push to `main`, `release/**`, `chore/1-phase-07-release-hardening`; Pull Request to `main`.
- **Environment:** `ubuntu-latest`, Temurin JDK 21, Android SDK (API 37).
- **Concurrency:** Cancels stale in-progress runs on the same branch.
- **Verification Jobs:**
  1. `unit-tests`: Runs `./gradlew testDebugUnitTest` and uploads test result artifacts.
  2. `lint`: Runs `./gradlew lintDebug` and uploads HTML lint reports.
  3. `release-build-and-audit`:
     - Compiles full R8 minified release APK (`./gradlew assembleRelease`).
     - **Manifest Security Gate:** Automatically verifies that `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` are absent in the merged release manifest.
     - Publishes `app-release-unsigned.apk` and `mapping.txt` as build artifacts.

---

## 2. Release Quality Gates & Verification Commands

All core verification tasks execute cleanly and pass 100%:

| Command | Action / Scope | Result | Test Count / Details |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat testDebugUnitTest` | Unit Test Suite | **PASS** | 49 unit tests across 9 test classes |
| `.\gradlew.bat connectedDebugAndroidTest` | On-Device Test Suite | **PASS** | 26 on-device tests on API 37 emulator |
| `.\gradlew.bat lintDebug` | Android Lint | **PASS** | 0 errors, 0 actionable warnings |
| `.\gradlew.bat assembleDebug` | Debug APK Build | **PASS** | `app-debug.apk` built successfully |
| `.\gradlew.bat assembleRelease` | Release APK Build | **PASS** | `app-release-unsigned.apk` with R8 minification |

---

## 3. QA Release Signing & APK Signature Verification

A temporary QA keystore was generated outside the git working tree:
- **Keystore Location:** `$env:USERPROFILE\.android\novelo-qa.keystore`
- **Distinguished Name:** `CN=Novelo QA, OU=Quality Assurance, O=Novelo, L=London, C=GB`
- **Key Algorithm / Size:** RSA 2048-bit (Validity: 10,000 days)
- **Certificate SHA-256 Digest:** `4dd27e24e977e2a8657ecae44358ad7adb57d1d9cce38b20bcb98fac8e6461d1`
- **Certificate SHA-1 Digest:** `d9bf417c3389452ea162b81c6b7465f7d1bfdeaa`

### APK Signature Verification via `apksigner`

```powershell
apksigner verify --verbose --print-certs "$env:TEMP\novelo-qa-signed.apk"
```
**Verification Output:**
- Verified using v1 scheme (JAR signing): `false`
- Verified using v2 scheme (APK Signature Scheme v2): `true`
- Verified using v3 scheme (APK Signature Scheme v3): `true`
- Status: **VERIFIED**

### On-Device Release Runtime Validation
- Signed release APK installed on `emulator-5554` (Android 16 / API 37).
- Logcat verified during full navigation cycle: **0 crashes**, **0 R8 reflection errors**, **0 ClassNotFoundException / NoSuchMethodError**.
- Proguard mapping file preserved at: `app/build/outputs/mapping/release/mapping.txt`.

---

## 4. R8 Minification & Keep Rules Configuration (`app/proguard-rules.pro`)

The release configuration combines AGP optimization with dedicated keep rules:

```pro
# Preserve Kotlin Sequences, Coroutines and internal state machines
-keep class kotlin.sequences.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }

# Room SQLite Database & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# WorkManager Workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Novelo Domain Models & Room Entities
-keep class com.example.nuvilo.domain.model.** { *; }
-keep class com.example.nuvilo.data.local.entity.** { *; }
```

- **Debug APK Size:** ~23.1 MB
- **Release APK Size:** **~3.2 MB**
- **Size Reduction:** **~86% reduction** achieved with zero runtime degradation.

---

## 5. Strict Offline & Zero-Network Boundary Verification

The release merged manifest was dumped and audited using `aapt dump permissions`:

```
package: com.example.nuvilo
uses-permission: name='android.permission.SCHEDULE_EXACT_ALARM'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
uses-permission: name='android.permission.USE_BIOMETRIC'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
uses-permission: name='android.permission.USE_FINGERPRINT'
permission: com.example.nuvilo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.example.nuvilo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

- **`android.permission.INTERNET`:** **STRICTLY ABSENT**.
- **`android.permission.ACCESS_NETWORK_STATE`:** **STRIPPED** via `tools:node="remove"`.
- **Remote SDKs:** No Firebase, no analytics, no crash trackers, no advertising libraries.

---

## 6. Privacy & Data Protection Safeguards

1. **Auto-Backup Cloud Exclusion:**
   - `android:allowBackup="false"` declared.
   - `backup_rules.xml` and `data_extraction_rules.xml` exclude `databases/` and `shared_prefs/` from cloud synchronization.
2. **Screen Privacy & Recents Masking (`FLAG_SECURE`):**
   - Active while locked or if privacy mode is enabled.
3. **Biometric App Lock:**
   - AndroidX `BiometricPrompt` with device credential fallback (PIN, Pattern, Password).
   - Automatic locking supported (Immediately, 1 min, 5 min, 15 min).
4. **Encrypted Local Backup (SAF):**
   - User-chosen passphrase with PBKDF2 (100,000 iterations) + AES-256-GCM authenticated encryption.
   - Zero temporary plaintext files on public storage.
