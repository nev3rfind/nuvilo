# Nuvilo Intelligent Notification & Stock Haptics Specification
**Phase 10 Notification Engineering Document**  
**Author:** Agent 3 (`notif-haptics-eng`)  
**Status:** Approved & Implemented  
**Date:** 2026-11-14  

---

## 1. Overview & Architectural Goals

Medication notifications in Nuvilo are context-aware clinical alerts designed to eliminate cognitive burden. Rather than displaying detached dose reminders or raw unformatted numbers, notifications communicate:
1. What dose to take and how to administer it.
2. The exact remaining units in the currently opened container.
3. The count of unopened physical containers remaining in reserve.
4. The projected lifespan of the total medication supply.
5. Critical lifecycle events (such as container depletion or sterility expiration).

---

## 2. Notification Formatting Specification

### 2.1 Standard Scheduled Dose Notification

#### Content Layout
- **Title**: `<Medication Name> <Strength>` (e.g. `Hydro Forte 0.3%`)
- **SubText**: `<Package Label> · <Units Remaining> left · <Unopened Count> sealed reserve`  
  *(e.g., `Bottle #1 · 180 left · 6 sealed reserve`)*
- **BigContentTitle**: Expanded dose summary header
- **BigText (Expanded Body)**:
  ```
  Take 2 drops · Both eyes (0.3%) · Scheduled for 08:00
  📦 Current bottle: 180 drops remaining.
  📦 Unopened bottles: 6.
  ⏳ Estimated total supply: 198 days.
  ```

#### Clinical Reasoning
- The user immediately knows if they need to fetch a new bottle from the cupboard before heading out for the day.
- Communicating both the opened quantity and unopened reserve prevents false panic about running out of medication.

---

### 2.2 Finished Container Alert (`buildFinishedPackageAlert`)

Triggered immediately when an opened container reaches 0 units or is marked finished early by the user.

- **Title**: `<Medication Name> – Current bottle finished`
- **Text / BigText**: `<Unopened Count> unopened bottles available. Open the next bottle when ready.`
- **Action**: Opens the medication detail screen directly into the "Ready to Open" container confirmation card.
- **Safety Rule**: The next container is **never** automatically opened in the background. The user must physically take the new container, unseal it, and confirm opening.

---

### 2.3 Privacy Mode Masking

When Privacy Mode is enabled in settings (or when the device is locked on an unsecured screen):
- **Public Title**: `Medication Reminder`
- **Public Body**: `Time for your scheduled dose.`
- **Masking Rules**:
  - Drug name, strength, instructions, package numbers, and stock counts are completely redacted.
  - Action buttons ("Take", "Snooze") remain functional without disclosing drug identities.

---

## 3. Tactile Haptic Patterns

Nuvilo incorporates tactile feedback for inventory operations using `Vibrator` and `VibrationEffect` on supported Android hardware:

| Haptic Event | Waveform / Effect | Target Emotion / Feedback |
|---|---|---|
| `RESTOCK_SAVED` | Double pulse (`[0, 60, 80, 100]ms` at moderate amplitude) | Crisp confirmation of inventory delivery recorded |
| `PACKAGE_OPENED` | Ascending sequence (`[0, 40, 60, 80]ms` increasing amplitude) | Satisfying physical unsealing / tamper-band broken |
| `PACKAGE_DEPLETED` | Heavy low-frequency thump (`[0, 120]ms` full amplitude) | Definitive tactile acknowledgment that container is empty |
| `DOSE_CONFIRMED` | Sharp single click (`VibrationEffect.EFFECT_CLICK`) | Immediate positive affirmation of adherence |

---

## 4. Verification Evidence

- `NotificationTextBuilderTest.kt` verifies:
  - Accurate BigText generation with package breakdown and days supply.
  - SubText formatting with bottle label and unopened reserve.
  - Finished container alert text matching exact specification.
  - Privacy masking redacting sensitive health identifiers.
