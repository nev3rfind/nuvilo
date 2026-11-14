# Nuvilo Inventory UI/UX Audit & Design Specification
**Phase 10 UX Architecture & Accessibility Review**  
**Author:** Agent 4 (`uiux-specialist`)  
**Status:** Approved & Implemented  
**Date:** 2026-11-14  

---

## 1. Executive Summary & Design Rationale

Prior to Phase 10, Nuvilo presented patients with an unnatural purchasing interface: restocking eye drops requested raw drop quantities with arbitrary presets such as `+150 drops`, `+300 drops`, and `+600 drops`. Patients do not purchase liquids in drops; they purchase physical bottles.

Phase 10 aligns the digital experience with the physical world:
1. Patients buy and count discrete containers (bottles, tubes, boxes, blister packs).
2. Drops, sprays, and milligrams are tracked internally from the currently unsealed container.
3. Every screen (Restock dialog, Medication Detail, Dashboard, Recount modal) presents clear, human-understandable physical inventory counts.

---

## 2. Redesigned Restock Supplies Experience

### 2.1 Before vs. After Comparison

| Element | Legacy (Flawed) | Phase 10 (Redesigned) |
|---|---|---|
| **Primary Input** | "Quantity Added (Drops)" | "Packages Received (Bottles)" |
| **Presets** | `+150`, `+300`, `+600` drops | `+1`, `+2`, `+3`, `+5`, `+7` bottles |
| **Capacity Input** | Hidden / hardcoded | "Capacity per Bottle (e.g. 300 drops)" |
| **Unsealing Control** | None (implicitly pooled) | "Open first bottle immediately" switch |
| **Live Forecast** | None | Real-time summary card showing total drops, package breakdown, and days of supply |

### 2.2 Live Restock Summary Card

As the user adjusts package count or capacity, a Material 3 container displays a real-time summary:

```
┌────────────────────────────────────────────────────────┐
│ 📦 7 bottles received                                  │
│ 💧 2,100 estimated drops in total                      │
│ 🔓 1 opened bottle + 6 unopened bottles                │
│ ⏳ Estimated supply: 210 days at 10 drops/day          │
└────────────────────────────────────────────────────────┘
```

---

## 3. Medication Detail Screen: Package-Aware Stock Cards

The stock overview on `MedicationDetailScreen.kt` is partitioned into distinct visual hierarchy layers:

### 3.1 Header: Total Usable Supply
- **Title**: `TOTAL ESTIMATED USABLE SUPPLY`
- **Primary Value**: `1,980 drops`
- **Physical Badge**: `7 total physical bottles (6 unopened)`
- **Duration**: `~198 days supply remaining`

### 3.2 Active Opened Package Card
- **Label**: `Bottle #1 (Active)`
- **Visual Progress Bar**: Shows `180 / 300 drops remaining (60%)`.
- **Sterility Timeline**: Displays opened date and remaining days until the 28-day sterility window requires disposal.
- **Actions**:
  - `Recount Bottle`: Adjust balance with before/after diff preview.
  - `Mark as Finished`: For early bottle depletion without fabricating doses.
  - `Revert Opening`: Available if bottle was opened by mistake and zero drops were taken.

### 3.3 Next Staged Bottle ("Ready to Open")
When the active bottle is exhausted or depleted:
- A high-contrast callout card appears:
  ```
  ┌────────────────────────────────────────────────────────┐
  │ 📦 Bottle #2 is ready to open                          │
  │ Lot #2026-A · Exp: 12/2027                             │
  │ 300 drops nominal capacity                             │
  │                                                        │
  │ [ Open Bottle Now ]                                    │
  └────────────────────────────────────────────────────────┘
  ```
- Tapping **Open Bottle Now** unseals the bottle, stamps `openedAtEpochMs`, and starts the sterility countdown.

---

## 4. Dashboard Screen: Compact Package Inventory Row

On `DashboardScreen.kt`, both `HeroDoseCard` and `TimelineDoseItem` display the compact physical package status line:

`Open bottle: 180 / 300 drops · Unopened: 6 bottles · Total: 7 bottles · ~198 days supply`

### Status Badges:
- **Bottle nearly empty**: Rendered in Amber when opened bottle has $\le 2$ days of doses remaining.
- **No unopened reserve**: Rendered in Red when `unopenedCount == 0` and opened bottle is low.
- **Sterility warning**: Rendered when opened bottle is within 3 days of discard deadline.

---

## 5. Accessibility (WCAG 2.1 AA Compliance)

- **Touch Targets**: All preset chips, switches, and buttons strictly adhere to the $48 \times 48\text{dp}$ interactive target guideline (`minimumInteractiveComponentSize`).
- **Contrast**: Text and container contrast ratios exceed $4.5:1$ in both light and dark themes.
- **Non-Color Indicators**: Every status color (Amber, Red, Green) is paired with a distinct Material icon and descriptive text.
- **TalkBack Semantics**: Clear accessibility labels on all custom progress indicators and stepper buttons.
