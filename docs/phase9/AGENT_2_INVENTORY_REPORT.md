# NUVILO PHASE 9 – AGENT 2 REPORT: INVENTORY ARCHITECTURE, ACCOUNTING & STOCK LEDGER
**Author:** Swarm Worker 2 (`inventory-eng`) – Medication Inventory & Stock Ledger Specialist  
**Domain Invariant Authority:** `AGENTS.md`, `novelo-medication-safety`, `docs/phase9/AGENT_1_DOMAIN_AUDIT.md`  
**Repository Branch:** `feat/16-intelligent-medication-inventory`  
**Status:** COMPLETE & VERIFIED

---

## 1. Executive Summary & Inventory Mandate

In standard medication tracking applications, stock is treated as a single naive scalar counter. When a bottle or package runs out earlier than expected, users are forced into destructive workarounds: either creating fake "taken" doses (corrupting medical adherence history) or manually editing arbitrary integers (breaking auditable accounting).

Phase 9 Addendum introduces **Early Package Depletion** and **Intelligent Package Transitions** into Nuvilo's immutable double-entry ledger architecture.

### Key Capabilities Delivered:
1. **Physical Reality vs Theoretical Capacity**: An opened container (e.g. eye-drop bottle with 300 estimated drops) can be marked finished when it physically runs dry (e.g. after 240 drops consumed), cleanly writing off the remaining 60 drops as `MovementType.WASTAGE`.
2. **Zero Adherence Contamination**: Adherence calculations, taken dose occurrences, and scheduled dose events remain 100% untouched. No synthetic doses are ever created.
3. **Deterministic FEFO Stage Next**: Unopened inventory is scanned using First-Expired, First-Out rules. The next container is staged as "Ready to Open" with expiry date and batch label, but is never auto-opened until the user physically breaks the seal.
4. **Non-Destructive Reversibility**: Accidental depopulation can be reversed via `reopenDepletedPackage(...)`, which logs a compensating `ADJUSTMENT_IN` entry and restores container state.
5. **Zero Schema Migration Risk (Option B)**: The contract is persisted within Room Schema v4 by utilizing structured metadata within existing columns (`package_status = 'EMPTY'`, `discard_reason = "DEPLETED[code]: userNote"`, `discarded_at_epoch_ms = timestamp`).

---

## 2. Invariant Verification & Compliance Matrix

| Invariant | Specification Rule | Agent 2 Verification Result |
|---|---|---|
| **Zero Float/Double Arithmetic** | All stock math must use scaled integer `ScaledQuantity` ($10^3$ micro-units) or raw `Long`. | **PASSED**: All depletion, wastage delta, and balance calculations in `StockLedgerCalculator.kt` operate strictly on `ScaledQuantity`. |
| **Append-Only Ledger** | Stock movements are immutable; entries are never deleted or modified in-place. | **PASSED**: Depletions append `MovementType.WASTAGE`. Reversals append `MovementType.ADJUSTMENT_IN`. Historical ledger remains immutable. |
| **Zero Dose Fabrication** | Marking a container finished must never record a medication administration. | **PASSED**: In `NoveloRepository.kt`, `depletePackage(...)` operates strictly across `stock_lots` and `stock_ledger_entries`. `dose_events` is never touched. |
| **FEFO Allocation Integrity** | Earliest expiring unopened package is staged next; expired lots are disqualified. | **PASSED**: `findStagedNextPackage(...)` filters by `UNOPENED`, verifies `expiryDateEpochMs > now`, and sorts by earliest expiry date then creation timestamp. |
| **Single Source of Truth** | Balances are derived from movements, not mutable counters. | **PASSED**: Usable balance is computed by `StockLedgerCalculator.calculateBalance(...)` summing ledger inflows and outflows. |
| **Encrypted Backup Compatibility** | Full compatibility with Room v4 JSON/Zip backup encryption without format bump. | **PASSED**: Option B encodes depletion reason in existing `discard_reason` column. Backup validators pass without changes. |

---

## 3. Mathematical & Accounting Formalism

### 3.1 The Early Depletion Transaction
Let container $L$ have initial capacity $Q_{\text{initial}}$ and current balance $Q_{\text{remaining}}$.
Recorded cumulative consumption is:
$$Q_{\text{consumed}} = Q_{\text{initial}} - Q_{\text{remaining}}$$

When the user triggers `depletePackageEarly(L, \text{reason}, \text{note})` at timestamp $T$:
1. If $Q_{\text{remaining}} > 0$:
   A compensating wastage entry $E_{\text{waste}}$ is created:
   $$E_{\text{waste}} = \langle \text{id}=\text{UUID}, \text{type}=\text{WASTAGE}, \text{qty}=Q_{\text{remaining}}, \text{lotId}=L.\text{id}, \text{reason}=\text{"Early package depletion [}\text{reason.code}\text{]: }\text{note"} \rangle$$
2. The package lot is updated to terminal state:
   $$L' = L \text{ with } \begin{cases} \text{packageStatus} = \text{EMPTY} \\ \text{remainingQuantity} = \text{ScaledQuantity.ZERO} \\ \text{depletedAtEpochMs} = T \\ \text{depletionReason} = \text{reason} \\ \text{depletionNote} = \text{note} \end{cases}$$
3. Usable stock balance is instantaneously adjusted:
   $$B_{\text{usable}} \leftarrow B_{\text{usable}} - Q_{\text{remaining}}$$

### 3.2 Accidental Depletion Reversal
If marked finished in error:
1. Historical wastage entry $E_{\text{waste}}$ is preserved in the ledger.
2. A compensating reversal entry $E_{\text{reversal}}$ is created:
   $$E_{\text{reversal}} = \langle \text{id}=\text{UUID}, \text{type}=\text{ADJUSTMENT\_IN}, \text{qty}=Q_{\text{restored}}, \text{lotId}=L.\text{id}, \text{reason}=\text{"Reopen depleted package: }\text{reasonNote"} \rangle$$
3. The lot is restored:
   $$L'' = L' \text{ with } \begin{cases} \text{packageStatus} = \text{OPENED} \\ \text{remainingQuantity} = Q_{\text{restored}} \\ \text{depletedAtEpochMs} = \text{null} \\ \text{depletionReason} = \text{null} \end{cases}$$

---

## 4. Implementation Artifacts

- **Domain Model**: `PackagingModels.kt` defines `enum class DepletionReason` with display names and ledger reason codes (`BOTTLE_EMPTY`, `EXCESSIVE_WASTAGE`, `LEAKING_OR_DAMAGED`, `LOST`, `DISCARDED`, `OTHER`).
- **Calculator Engine**: `StockLedgerCalculator.kt` implements:
  - `depletePackageEarly(lot, reason, userNote, nowEpochMs, availableLots): EarlyDepletionOutcome`
  - `reopenDepletedPackage(lot, reversalReason, nowEpochMs): ReopenDepletionOutcome`
  - `findStagedNextPackage(lots, nowEpochMs): StockLot?`
- **Data Repository**: `NoveloRepository.kt` exposes:
  - `depletePackage(lotId, reason, userNote, nowEpochMs): Result<EarlyDepletionOutcome>`
  - `reopenDepletedPackage(lotId, reason, nowEpochMs): Result<ReopenDepletionOutcome>`
- **Notification Sync**: `SupplyNotificationSync.kt` updates system notifications and reminders immediately upon inventory changes.

---

## 5. Verification Sign-Off
Agent 2 confirms the inventory architecture is mathematically sound, conforms to medical safety guidelines, and introduces zero floating-point arithmetic or schema regression risks.
