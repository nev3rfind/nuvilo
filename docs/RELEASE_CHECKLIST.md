# Novelo Release Checklist & Build Hardening Verification

**Application:** Novelo (Personal Medication Management)  
**Package:** `com.example.nuvilo`  
**Platform:** Native Android (Kotlin 2.2, Jetpack Compose, Material 3, Room v3)  
**Min SDK:** 24 | **Target/Compile SDK:** 37  
**Build Variant:** `release` (R8 minified, shrunk resources, optimization enabled)

---

## 1. Release Quality Gates & Verification Commands

All 5 core Gradle verification tasks have been executed and verified cleanly:

| Command | Action / Scope | Result | Test Count / Output |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat testDebugUnitTest` | Unit Test Suite | **PASS** | 49 unit tests across 9 test classes |
| `.\gradlew.bat connectedDebugAndroidTest` | Instrumented Test Suite | **PASS** | 23 on-device tests on API 37 emulator |
| `.\gradlew.bat lintDebug` | Android Lint | **PASS** | 0 errors, 0 actionable warnings |
| `.\gradlew.bat assembleDebug` | Debug APK Build | **PASS** | Build successful (`app-debug.apk`) |
| `.\gradlew.bat assembleRelease` | Release APK Build | **PASS** | Build successful (`app-release-unsigned.apk`) |

---

## 2. Release APK Metrics & R8 Optimization

- **Debug APK Size:** ~23.1 MB (unoptimized, unminified, debug test manifests included).
- **Release APK Size:** **3,097,805 bytes (~3.09 MB)**.
- **Size Reduction:** **86.6% reduction** achieved through R8 dead-code stripping and resource optimization.

### R8 Keep Rules Configuration (`app/src/main/keepRules/rules.keep`)
The release configuration explicitly preserves:
1. **Room Database & Schema:**
   - Database implementation: `com.example.nuvilo.data.local.NoveloDatabase_Impl`
   - Entities and DAOs: `@androidx.room.Entity class *`, `@androidx.room.Dao interface *`
   - Migrations: `androidx.room.migration.Migration`
2. **WorkManager Background Workers:**
   - `com.example.nuvilo.system.work.LowStockCheckWorker`
   - `* extends androidx.work.ListenableWorker`
3. **Encrypted SAF Backup/Restore Models:**
   - JSON envelopes: `com.example.nuvilo.data.backup.EncryptedBackupEnvelope`
   - Backup data models: `com.example.nuvilo.data.backup.JsonBackupData`
   - Domain transfer models: `com.example.nuvilo.domain.model.**`
4. **Broadcast Receivers:**
   - Exact alarm receiver: `com.example.nuvilo.system.alarm.AlarmReceiver`
   - Action receiver: `com.example.nuvilo.system.notification.NotificationActionReceiver`
   - Boot receiver: `com.example.nuvilo.system.receiver.BootAndChangeReceiver`

---

## 3. Strict Offline & Zero-Network Boundary Verification

The release merged manifest (`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`) was verified:

```xml
<!-- Confirmed Permissions in Merged Release Manifest -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

- **`android.permission.INTERNET`:** **STRICTLY ABSENT**.
- **`android.permission.ACCESS_NETWORK_STATE`:** **REMOVED** via `tools:node="remove"` (preventing passive network monitoring from dependencies).
- **Remote SDKs:** No Firebase, no analytics, no crash trackers, no advertising libraries.

---

## 4. Privacy & Data Protection Safeguards

1. **Auto-Backup Exclusion (`android:allowBackup="false"`):**
   - Cloud backup is strictly disabled on the application tag.
   - `backup_rules.xml` and `data_extraction_rules.xml` explicitly exclude `databases/` and `shared_prefs/` from cloud synchronization.
2. **Recents Preview Masking (`FLAG_SECURE`):**
   - Masking enabled by default and dynamically enforced while device is locked.
3. **Biometric App Lock:**
   - AndroidX `BiometricPrompt` with device credential fallback (PIN, Pattern, Password).
   - Configurable automatic lock grace periods (Immediately, 1 min, 5 min, 15 min).
4. **Encrypted Local Backup (SAF):**
   - User-chosen passphrase with PBKDF2 (100,000 iterations) + AES-256-GCM authenticated encryption.
   - Zero temporary files or intermediate plaintext leakage to public storage.
