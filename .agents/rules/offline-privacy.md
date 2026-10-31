---
trigger: always_on
---

# Offline-First & Privacy Constraints for Novelo

1. **Strict Offline Environment**:
   - `android.permission.INTERNET` is forbidden.
   - Merged manifest must never declare internet permission.
   - Any dependency attempting to pull network libraries (OkHttp, Retrofit, remote analytics) must be rejected or stripped.

2. **No Tracking or Third-Party SDKs**:
   - No Firebase Analytics, Crashlytics, Google Play Services Ads, Sentry, Mixpanel, or third-party tracking libraries.
   - Any crash handling must remain strictly local (e.g. local file logging in app private storage if enabled by user for debugging).

3. **Sensitive Data Protection**:
   - Medication names, dosage, schedule, and prescription details represent sensitive health data.
   - Logs (`Log.d`, `Log.e`, etc.) must NEVER print drug names, user notes, or health identifiers in release builds. Use a custom timber tree or debug-only log wrapper.
   - Notifications must support Privacy Mode masking.
   - Android auto-backup rules (`backup_rules.xml` and `data_extraction_rules.xml`) must exclude the Room database and sensitive DataStore keys from automatic cloud backups unless encrypted with user credentials.
