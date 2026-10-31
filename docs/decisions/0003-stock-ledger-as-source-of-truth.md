# ADR 0003: Stock Ledger as the Single Source of Truth

- **Status**: Accepted
- **Date**: 2026-10-29
- **Deciders**: Senior Android Lead, Domain Modeler

## Context
A major failure mode in medication tracking applications is "stock drift": an internal mutable integer counter is decremented directly on doses, but when doses are rescheduled, deleted, skipped retroactively, or duplicated by notifications, the inventory balance becomes out of sync with physical reality. Users lose trust when the app claims they have 4 tablets remaining when their pillbox is empty.

## Decision
Physical medication inventory will be modeled as an **auditable, append-only double-entry stock ledger**.
1. Current stock balance is deterministically computed from the sum of all historical stock movements:
   $$\text{Balance} = \text{Opening Balance} + \sum(\text{Restocks}) - \sum(\text{Deductions}) \pm \sum(\text{Adjustments})$$
2. Quantities must use **exact scaled integer representation** (e.g. integer units or scaled by $10^3$). **Floating-point types (`Float`, `Double`) are strictly forbidden.**
3. Incompatible units can never be combined.
4. Historical records are never deleted; corrections are entered as compensating `ADJUSTMENT` transactions with reason strings.
5. Cached stock balance columns on the `medications` table exist strictly as read-optimized snapshots and must be updated transactionally within the Room `@Transaction` creating the ledger entry.

## Alternatives Considered
1. **Simple Mutable Stock Counter**: Decrement a `current_stock` column directly on dose events. Simpler to write initially, but completely un-auditable, prone to silent drift, and impossible to reconcile safely after historical edits.
2. **Periodic Reconciliation Rebuilds**: Periodic background tasks that re-scan all events to repair mutable counters. Complex, race-condition prone, and masks underlying architectural flaws.

## Consequences
- **Positive**: Complete audit trail; mathematical certainty; impossible for stock to drift silently; trivial to audit and debug; supports retroactive dose changes cleanly.
- **Negative**: Requires writing compensating entries rather than simple deletes; queries calculating lifetime balances require efficient SQLite indexing on `medication_id`.
