# Phase 9 Known Limitations & Future Considerations

**Product:** Nuvilo ("*Medication, made manageable.*")  
**Date:** 2026-11-12  
**Status:** Documented  

---

## 1. Domain & Clinical Invariant Boundaries

1. **No Implicit Medical Conversions**:
   - Nuvilo intentionally does not assume arbitrary density conversions (e.g. `20 drops = 1 ml`, or `1 tube = 30 applications`). Dropper bottle orifice sizes, viscosity, surface tension, and manufacturer variations vary significantly.
   - When a user chooses drops as their consumption unit, package capacity must be defined in drops by the user (or uses the recommended 300 drops/10ml preset with an `ESTIMATED` precision badge).
2. **Single Active Opened Package Per Medication**:
   - The current FEFO consumption model maintains at most one currently `OPENED` package per medication at a time.
   - When an opened package is emptied or discarded, the next eligible unopened package is automatically or manually transitioned to `OPENED`. Simultaneous multiple open bottles of the same medication (e.g., one bottle kept at home and another at work) are tracked under the combined opened pool until future multi-site tracking is introduced.
3. **Early Package Depletion Accounting Boundary**:
   - When an opened bottle is marked as depleted early (e.g. 240 drops consumed of 300 theoretical capacity), the remaining 60 drops are accounted for as a package wastage adjustment (`MovementType.WASTAGE`). The system does not attempt to distribute these 60 drops retrospectively across past dose events, which protects adherence records from synthetic alteration.
4. **Manual Physical Next-Bottle Opening**:
   - The next eligible unopened package is staged as "Ready to Open", but the app never automatically unseals it until the user taps "Open Next Bottle". This intentional constraint prevents software inventory drift if the patient delays starting the new bottle.
5. **Hardware Vibrator Differences**:
   - On Android devices running Android 10 (API 29) or lower, or budget devices lacking `Vibrator.areAllPrimitivesSupported(...)` / amplitude control, haptic feedback falls back to standard `VibrationEffect.createOneShot` or `VibrationEffect.createWaveform` durations without variable tactile amplitudes.

---

## 2. Platform & Storage Invariants

1. **Strict Offline Operation**:
   - Nuvilo has zero network access. Barcode scanning or automatic FDA/NHS product packaging lookups via cloud APIs remain strictly prohibited to preserve privacy invariants.
   - All package configurations and presets are derived from local templates.
2. **Room Database Schema v4**:
   - Room migrations are strictly forward-compatible. Downgrading an app from schema v4 back to schema v3 is not supported by Room, but v4 JSON backups preserve full compatibility across all past and future app revisions.
