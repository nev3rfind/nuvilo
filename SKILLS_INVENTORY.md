# Skills & Workflows Inventory

This document tracks all external and internal agent skills and workflows configured for **Novelo**, their provenance, license compliance, local installation paths, and verification results.

---

## 1. Official Android Skills
- **Canonical Repository**: [https://github.com/android/skills](https://github.com/android/skills)
- **Revision / Commit**: `6daa53ef27fa2aa87827d183a85c06cedceca833` (Bump plugin version to v1.0.19, Oct 29, 2026)
- **License**: Apache License 2.0
- **Installation Method**: Project-scoped curated copy to `.agents/skills/<skill-name>/`
- **Installed Skills**:
  1. `edge-to-edge`: Adaptive edge-to-edge, window insets, system bar legibility (`.agents/skills/edge-to-edge/SKILL.md`)
  2. `jetpack-compose-performance`: Recomposition optimization, stability, performance profiling (`.agents/skills/jetpack-compose-performance/SKILL.md`)
  3. `android-intent-security`: Intent sanitization, pending intent flags, broadcast receiver safety (`.agents/skills/android-intent-security/SKILL.md`)
  4. `android-permissions-security`: Permission checks, exact alarm permissions, notification runtime permission (`.agents/skills/android-permissions-security/SKILL.md`)
  5. `testing-setup`: Unit and UI testing configuration for Android (`.agents/skills/testing-setup/SKILL.md`)
  6. `adaptive`: Adaptive layout building for varied Android form factors (`.agents/skills/adaptive/SKILL.md`)
- **Verification Status**: Verified. All `SKILL.md` files possess valid YAML frontmatter and are discoverable by Antigravity in `.agents/skills/`.

---

## 2. Matt Pocock's Engineering Skills
- **Canonical Repository**: [https://github.com/mattpocock/skills](https://github.com/mattpocock/skills)
- **Revision / Commit**: `a496f5513d4a4cf889bc48e189e579f4b0bdf6aa` (Oct 29, 2026)
- **License**: MIT License
- **Installation Method**: Project-scoped curated copy to `.agents/skills/<skill-name>/`
- **Installed Skills**:
  1. `grill-with-docs`: Rigorous architectural and requirements interview creating ADRs and glossary entries (`.agents/skills/grill-with-docs/SKILL.md`)
  2. `grill-me`: Interactive user requirements grilling (`.agents/skills/grill-me/SKILL.md`)
  3. `grilling`: Core grilling workflow engine (`.agents/skills/grilling/SKILL.md`)
  4. `writing-for-agents`: Guidelines for structuring clean instructions for LLMs (`.agents/skills/writing-for-agents/SKILL.md`)
  5. `code-review`: Objective engineering code review checklist (`.agents/skills/code-review/SKILL.md`)
  6. `improve-codebase-architecture`: Modular architecture refactoring workflow (`.agents/skills/improve-codebase-architecture/SKILL.md`)
  7. `domain-modeling`: Ubiquitous language definition and entity-relationship boundary modeling (`.agents/skills/domain-modeling/SKILL.md`)
- **Verification Status**: Verified. Tested frontmatter parsing and agent dependencies.

---

## 3. Superpowers Engineering Framework
- **Canonical Repository**: [https://github.com/obra/superpowers](https://github.com/obra/superpowers)
- **Revision / Commit**: `2ce8aa15d05bd57213e6f746f953aafb7ded7a8b` (v7.0.1, Oct 21, 2026)
- **License**: MIT License
- **Installation Method**: Project-scoped curated copy to `.agents/skills/<skill-name>/`
- **Installed Skills**:
  1. `writing-plans`: Implementation-ready task decomposition with exact file paths and test expectations (`.agents/skills/writing-plans/SKILL.md`)
  2. `test-driven-development`: Red-Green-Refactor discipline for domain logic (`.agents/skills/test-driven-development/SKILL.md`)
  3. `systematic-debugging`: Root-cause hypothesis and evidence-based bug isolation (`.agents/skills/systematic-debugging/SKILL.md`)
  4. `verification-before-completion`: Evidence requirement before marking tasks complete (`.agents/skills/verification-before-completion/SKILL.md`)
  5. `requesting-code-review`: Structured peer-review preparation (`.agents/skills/requesting-code-review/SKILL.md`)
  6. `using-superpowers`: Foundation workflow orchestrator (`.agents/skills/using-superpowers/SKILL.md`)
- **Verification Status**: Verified. Full workflow definitions ready for use during implementation phases.

---

## 4. Supplementary Community Android Skills
- **Canonical Repository**: [https://github.com/almasumdev/awesome-kotlin-android-agent-skills](https://github.com/almasumdev/awesome-kotlin-android-agent-skills)
- **Revision / Commit**: `1c24ece6c276822f54f6f1d1ba82bc996d3ccd08` (Apr 19, 2026)
- **License**: Dual Apache 2.0 / CC-BY-SA 4.0
- **Installation Method**: Project-scoped curated copy into `.agents/skills/<skill-name>/`
- **Installed Skills**:
  1. `compose-accessibility`: TalkBack, semantic trees, touch targets, and accessible text scaling (`.agents/skills/compose-accessibility/SKILL.md`)
  2. `kotlin-data-layer`: Room, DataStore, and offline cache repository architecture (`.agents/skills/kotlin-data-layer/SKILL.md`)
- **Verification Status**: Verified. Fills key offline architecture and accessibility gaps.

---

## 5. Custom Domain Skills (Internal)
- **Skill**: `novelo-medication-safety`
- **Path**: `.agents/skills/novelo-medication-safety/SKILL.md`
- **Purpose**: Defines non-negotiable domain rules for dose recording modes (`CONFIRM` vs `AUTO_ASSUME`), stock ledger exact arithmetic, idempotency, alarm reconciliation, zero-network privacy, and medical guardrails.
- **Verification Status**: Verified. Frontmatter valid, active for all medication, stock, and alarm tasks.

---

## 6. Inventory Summary Table

| Skill Identifier | Source Repository | License | Scope | Status |
| :--- | :--- | :--- | :--- | :--- |
| `edge-to-edge` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `jetpack-compose-performance` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `android-intent-security` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `android-permissions-security` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `testing-setup` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `adaptive` | android/skills | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `grill-with-docs` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `grill-me` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `grilling` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `writing-for-agents` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `code-review` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `improve-codebase-architecture`| mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `domain-modeling` | mattpocock/skills | MIT | Workspace (`.agents/skills`) | Verified |
| `writing-plans` | obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `test-driven-development` | obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `systematic-debugging` | obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `verification-before-completion`| obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `requesting-code-review` | obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `using-superpowers` | obra/superpowers | MIT | Workspace (`.agents/skills`) | Verified |
| `compose-accessibility` | awesome-kotlin-android | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `kotlin-data-layer` | awesome-kotlin-android | Apache 2.0 | Workspace (`.agents/skills`) | Verified |
| `novelo-medication-safety` | Internal (Novelo) | Proprietary | Workspace (`.agents/skills`) | Verified |
