# ADR 0004: Dose Occurrence Identity and Idempotent Recording

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Senior Android Lead, QA Lead

## Context
Medication reminders are actionable: notifications feature interactive buttons ("Take", "Snooze", "Skip"), and the in-app dashboard features quick-action cards. Users may double-tap buttons, rapid-fire click during UI lag, or an alarm receiver and background worker might execute concurrently. Without idempotency, a single dose might be recorded twice, deducting double the stock and corrupting adherence records.

## Decision
1. **Occurrence Identity**: Every planned dose occurrence has a deterministic, globally unique `occurrenceId`:
   $$\text{occurrenceId} = \text{SHA256}(\text{medicationId} + \text{":"} + \text{scheduledEpochSecond})$$
2. **Database Constraint**: The `dose_events` table enforces a SQLite `UNIQUE(occurrence_id)` constraint.
3. **Idempotency in Application Logic**: `RecordDoseUseCase` operates under a Room transaction:
   - Queries `dose_events` for `occurrenceId`.
   - If an event already exists with status `TAKEN` or `ASSUMED`, the operation is a no-op (or returns the existing event) and immediately aborts before any stock deduction can occur.
   - If no event exists, it inserts the `DoseEvent` and appends the corresponding `StockLedgerEntry` atomically.
4. **Non-Deducting Actions**: Snoozing, dismissing, or marking a dose `SKIPPED` creates an adherence record where appropriate, but **never** creates a stock deduction ledger movement.

## Alternatives Considered
1. **Auto-Incrementing Event IDs**: Generate arbitrary IDs on insert. Fails to prevent duplicate inserts when the same logical occurrence is triggered multiple times across different processes.
2. **UI Button Debouncing Only**: Client-side throttle (e.g. 500ms delay). Easily bypassed by concurrent notification actions or background service triggers; does not solve data layer safety.

## Consequences
- **Positive**: Zero risk of duplicate dose recording; immune to notification shade race conditions; rock-solid adherence statistics.
- **Negative**: Requires schedule generators and notification intents to compute and carry deterministic `occurrenceId` tokens.
