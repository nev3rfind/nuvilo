# Novelo Screen Specifications & Wireframe Layouts

This document defines the 12 core screens for **Novelo**, including detailed structural descriptions, interactive elements, and a design trade-off comparison between two dashboard layouts for user review.

---

## 1. Dashboard Layout Exploration (For User Review)

Before finalizing the home screen layout, two alternative architectural designs have been developed:

### Option A: Chronological Timeline-First (Recommended Default)
Focuses on the entire day's sequence, displaying doses grouped chronologically (Morning, Afternoon, Evening, Night).

```
+-------------------------------------------------------------+
| Novelo                   [Privacy Mask: OFF]  [Settings ⚙️]  |
| Thursday, 29 October                                        |
| "Medication, made manageable."                              |
+-------------------------------------------------------------+
| [Quick Stats Banner]                                        |
| 3 of 4 Doses Taken (75%)   ·   1 Low-Stock Warning ⚠️        |
+-------------------------------------------------------------+
| MORNING · 08:00 AM                                          |
| +---------------------------------------------------------+ |
| | [Pill Icon]  Atorvastatin 20mg                          | |
| | 1 Tablet · With breakfast                               | |
| | [Status: TAKEN at 08:05 AM  ✓]                          | |
| +---------------------------------------------------------+ |
|                                                             |
| AFTERNOON · 01:00 PM                                        |
| +---------------------------------------------------------+ |
| | [Drop Icon]  Eye Drops (Artificial Tears)               | |
| | 1 Drop in both eyes                                     | |
| | [Status: TAKEN at 01:15 PM  ✓]                          | |
| +---------------------------------------------------------+ |
|                                                             |
| EVENING · 08:00 PM (DUE NEXT in 45 min)                     |
| +---------------------------------------------------------+ |
| | [Pill Icon]  Metformin 500mg                            | |
| | 1 Tablet · Stock: 14 left (7 days)                      | |
| |                                                         | |
| | [ SNOOZE 15m ]         [ TAKE DOSE (1 Tablet) ]         | |
| +---------------------------------------------------------+ |
|                                                             |
| NIGHT · 10:00 PM                                            |
| +---------------------------------------------------------+ |
| | [Moon Icon]  Melatonin 3mg                              | |
| | 1 Capsule · Before bed                                  | |
| | [Status: PENDING]                                       | |
| +---------------------------------------------------------+ |
|                                                             |
| [Bottom Nav: Today (selected) | Meds | Stock | Rx | History] |
+-------------------------------------------------------------+
```

**Trade-offs (Option A)**:
- *Pros*: Gives users a complete mental picture of their entire day; easy to see past adherence and upcoming evening doses simultaneously; familiar timeline model.
- *Cons*: Pushes upcoming evening doses lower down the screen if the user has many morning medications.

---

### Option B: Hero Next-Dose + Metric Grid
Focuses exclusively on the immediate next action required, with a large "Hero" card at the top and a secondary metric grid below.

```
+-------------------------------------------------------------+
| Novelo                                        [Settings ⚙️]  |
| Thursday, 29 October                                        |
+-------------------------------------------------------------+
| NEXT SCHEDULED DOSE                                         |
| +---------------------------------------------------------+ |
| | [Hero Card - High Elevation]                            | |
| | 08:00 PM · Due in 45 minutes                            | |
| |                                                         | |
| | Metformin 500 mg                                        | |
| | Form: Tablet  ·  Take 1 Tablet with dinner              | |
| | Stock: 14 Tablets remaining (7 days)                    | |
| |                                                         | |
| | [ SNOOZE 15m ]     [ SKIP ]    [ ★ TAKE DOSE NOW ★ ]    | |
| +---------------------------------------------------------+ |
|                                                             |
| AT A GLANCE TODAY                                           |
| +---------------------------+ +---------------------------+ |
| | Doses Completed           | | Low Stock Warnings        | |
| | 3 / 4 (75%)               | | 1 Medication Needs Refill | |
| +---------------------------+ +---------------------------+ |
|                                                             |
| REMAINING DOSES TODAY                                       |
| • 10:00 PM: Melatonin 3mg (1 Capsule)                       |
|                                                             |
| [Bottom Nav: Today (selected) | Meds | Stock | Rx | History] |
+-------------------------------------------------------------+
```

**Trade-offs (Option B)**:
- *Pros*: Hyper-focused on the single most urgent action; very large touch targets for older users or those with motor tremors; minimal visual clutter.
- *Cons*: Hides earlier morning doses (requires scrolling or tapping into history to see what was taken earlier).

> *Note for Review*: Both options can be supported or refined. The architecture cleanly decouples the `DashboardViewModel` state from the Composable UI rendering, so layout refinements require zero database or domain logic rewrites.

---

## 2. Core Screen Specifications

### 1. Onboarding & Initial Empty State
- **Purpose**: Welcomes new users, explains offline-first privacy guarantees, and guides creation of their first medication.
- **Components**: Calming healthcare graphic, 3 bullet privacy guarantees (No cloud, No ads, Stored securely on this phone), and primary CTA button "Add My First Medication".

### 2. Today Dashboard
- **Purpose**: Primary daily hub for taking, snoozing, or reviewing scheduled doses.
- **Components**: Date header, privacy mode indicator toggle, low-stock warning banners, and either Timeline (Option A) or Hero (Option B) dose cards.

### 3. Medication List
- **Purpose**: Catalog of all tracked active and archived medications.
- **Components**: Search/filter bar, categorized list (Active vs Archived), medication cards with stock badges and schedule chips, FAB for "Add Medication".

### 4. Add/Edit Medication Flow
- **Purpose**: Step-by-step or unified wizard to configure medication properties.
- **Fields**:
  - Name (autofocus text field).
  - Strength & unit (e.g. 500, mg).
  - Form selector (chips for Tablet, Capsule, Liquid, Inhaler, Drops, etc.).
  - Stock unit selector (Tablets, ml, Puffs, Bottles).
  - Opening stock balance and low-stock alert threshold.
  - Dose recording preference (`CONFIRM` vs `AUTO_ASSUME`).
  - Color & Icon theme tag.

### 5. Medication Details
- **Purpose**: Deep-dive into a single medication.
- **Components**: Top header with drug details, current stock balance card with "Restock" and "Adjust" buttons, active schedules list, recent 30-day adherence chart, and linked prescription card.

### 6. Schedule Editor
- **Purpose**: Configure when reminders fire.
- **Components**: Schedule type picker (Daily, Specific Days, Cyclical, PRN), time picker list (add multiple times: 08:00, 14:00, 20:00), start/end date selectors, and instructions field ("Take with food").

### 7. Stock Management & Restocking
- **Purpose**: Physical inventory audit and refill management.
- **Components**:
  - Summary cards: Total medications tracked, low-stock count, days remaining.
  - Action buttons: "Restock Shipment" (adds inventory lot with expiry), "Manual Adjustment" (corrects count with reason).
  - Audit Ledger: Chronological list of all stock movements with source badges (`DOSE_TAKEN`, `RESTOCK`, `ADJUSTMENT`).

### 8. Dose History
- **Purpose**: Adherence auditing and doctor review.
- **Components**: Filter by medication or status (`ALL`, `TAKEN`, `SKIPPED`, `MISSED`), daily grouped adherence list showing exact confirmed timestamps.

### 9. Calendar
- **Purpose**: High-level visual map of adherence across weeks and months.
- **Components**: Month grid with color-coded dots (Green = 100% adherence, Amber = partial/skipped, Red = missed), tapping a day opens the bottom sheet with that day's dose log.

### 10. Prescription Tracker
- **Purpose**: Manage scripts, refills remaining, and doctor details.
- **Components**: Prescription cards displaying script number, doctor name, pharmacy contact, authorized repeats gauge (e.g., "Refill 2 of 5"), and expiry countdown badge.

### 11. Notification Settings & Diagnostics
- **Purpose**: Tune alarm delivery and diagnose OEM battery optimization blockers.
- **Components**: Sound and vibration toggles, quiet hours range picker, exact alarm permission status check, battery optimization whitelist shortcut, test alarm button.

### 12. Privacy, App Lock, Backup & Export Settings
- **Purpose**: Security configuration and data sovereignty.
- **Components**:
  - Biometric App Lock toggle (Immediate, 1 min, 5 min lock delay).
  - Notification Privacy Mode toggle.
  - Local Export: Button to export JSON/CSV to user-chosen storage URI via SAF.
  - Local Import: Button to restore database from backup file with schema validation.
