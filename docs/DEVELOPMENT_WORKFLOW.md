# Novelo Development & Release Workflow

This document details the engineering lifecycle, quality gates, and release procedures for **Novelo**.

---

## 1. Issue-Driven Development

Every unit of development begins with a clear GitHub Issue detailing:
1. **Scope & Objectives**: Problem statement and boundaries.
2. **Acceptance Criteria**: Verifiable requirements list.
3. **Safety & Privacy Considerations**: Impact on stock ledger, alarms, biometric auth, or SAF.
4. **Testing Deliverables**: Unit, connected, and manual test requirements.

---

## 2. Branching & Commit Discipline

```text
main (protected default branch)
  └── chore/1-phase-07-release-hardening (topic branch)
        ├── commit 1: docs(workflow): establish contributing guidelines (Refs #1)
        ├── commit 2: test(accessibility): add automated semantics tests (Refs #1)
        └── ...
```

- **Branch Naming**: `<type>/<issue-number>-<description>`
- **No Direct Commits to `main`**: All changes enter through reviewed Pull Requests.
- **Atomic Commits**: Small, focused commits with meaningful messages adhering to Conventional Commits.
- **Issue References**: Include `Refs #<issue-number>` in commit messages.

---

## 3. Mandatory Quality Gates

Every phase and release candidate must pass all four quality gates:

| Gate | Command | Requirement |
| :--- | :--- | :--- |
| **Unit Tests** | `.\gradlew.bat testDebugUnitTest` | 100% pass rate, fast deterministic feedback |
| **Connected Tests** | `.\gradlew.bat connectedDebugAndroidTest` | 100% pass rate on physical device or active emulator (`emulator-5554`) |
| **Lint & Static Analysis** | `.\gradlew.bat lintDebug` | 0 errors, 0 warnings in production code |
| **Debug Build** | `.\gradlew.bat assembleDebug` | Clean compilation and APK packaging |
| **Release Build** | `.\gradlew.bat assembleRelease` | Optimized R8 minification, resource shrinking, zero broken reflections |

---

## 4. Release Candidate Protocol

Prior to tagging or publishing a release:
1. **Accessibility Verification**: Complete TalkBack screen reader walkthrough and automated semantics validation.
2. **Performance & Memory Profiling**: Cold startup, frame stability, zero retained activity/view leaks.
3. **R8 Keep Rule Audit**: Verify ProGuard rules preserve Room DAOs, WorkManager workers, and cryptographic serializers without blanket wildcards.
4. **Security & Manifest Audit**: Confirm `android.permission.INTERNET` is absent in merged release manifest and `allowBackup="false"` is preserved.
5. **Full Regression**: Round-trip verification of dose confirmations, stock deductions, reminders, and encrypted SAF backup/restore.
6. **Documentation Freeze**: Update `CHANGELOG.md`, `README.md`, `ARCHITECTURE.md`, `SECURITY.md`, and generate release checklists.
