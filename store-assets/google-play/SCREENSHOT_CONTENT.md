# Nuvilo — Google Play Store Screenshot Content & Feature Mapping (`en-GB`)

**Official Application Name:** Nuvilo  
**Tagline:** *Medication, made manageable.*  
**Package Name:** `com.example.nuvilo`  
**Target Locale:** `en-GB`

---

## 1. Complete 8-Slide Campaign Narrative & Mapping

| Slide | Export Filename | Source Capture | Headline (Accented Word in Bold) | Supporting Copy | Real Nuvilo Screen & Route | Genuine Features Demonstrated |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- |
| **01** | `01-dashboard.png` | `dashboard.png` | Your day, in **view.** | See what's next and what's already recorded. | `DashboardScreen` (`today`) | `Nuvilo` top bar wordmark, `Offline Protected` badge, `5 / 8 taken (62%)` adherence bar, low-stock alert banner, Next Due Dose hero card (`Metformin 500mg · Take 1 tablet` at `19:00`), `Confirm Dose Taken (1 tab)`, `Snooze 15m`, `Skip Dose`, and Rest of Today's Schedule. |
| **02** | `02-medications.png` | `medications.png` | Every medicine, **organised.** | Keep details, doses and schedules together. | `MedicationListScreen` (`medications`) | `Active (5)` / `Archived (0)` segmented tabs, search bar, `Low Stock (1)` filter chip, and medication cards for `Amoxicillin 500mg Capsule`, `Atorvastatin 20mg Tablet` (`Auto` mode), `Lisinopril 10mg Tablet` (`5 tab left` warning), and `Metformin 500mg Tablet`. |
| **03** | `03-setup.png` | `setup.png` | Build your **routine,** simply. | Set up medication, timing and stock. | `AddMedicationDialog` (`medications` → `+ Add Medication`) | Guided 3-step setup wizard (`1. Identity`, `2. Schedule`, `3. Stock` — `Step 3 of 3`), `Opening Stock (tab)` = `28`, `Low Alert (tab)` = `7`, and live `Regimen Summary` card for `Ramipril · 5mg (Tablet)`. |
| **04** | `04-stock.png` | `stock.png` | Know your **supply.** | Track quantities, lots and expiry dates. | `MedicationDetailScreen` (`medication_detail/med_amox_500`) | Expanded `Batch Lots & FEFO Allocation (2)` showing `Lot #NHS-BATCH-409A` and `Lot #NHS-BATCH-512B` (`✓ Active FEFO Lot`, `Remaining: 25 of 28 cap`, `Exp: 23 Apr 2027`), plus expanded `Append-Only Stock Ledger (20)`. |
| **05** | `05-calendar.png` | `calendar.png` | See your routine **over time.** | Review daily outcomes in one calendar. | `AdherenceHistoryScreen` (`adherence`) | `Last 30 Days Clinical Adherence: 92%` (`✓ Therapeutic Target Met (≥80%)`), `Today 100%`, `Last 7 Days 92% (`39v + 6a / 49`)`, `Last 30 Days 92%`, rolling window selector (`7D`, `14D`, `30D`, `90D`), and `November 2026` monthly adherence calendar. |
| **06** | `06-history.png` | `history.png` | Every dose, **clearly** recorded. | See scheduled times and recorded outcomes. | `MedicationDoseHistoryScreen` (`medication_history/med_amox_500`) | `Amoxicillin Dose History` (`500mg · Explicit Confirmation`), `Last 30 Days Adherence: 97%` (`31 Confirmed`, `0 Assumed`, `1 Skipped`, `0 Missed`), date/outcome filters, and chronological dose cards (`Scheduled 14:00`, `1 capsule · Recorded 9 Nov, 14:04`, `✓ Confirmed Taken`, `Correct`). |
| **07** | `07-prescriptions.png` | `prescriptions.png` | **Prescriptions,** kept organised. | Track expiry and repeat collections. | `MedicationDetailScreen` (`PrescriptionOverviewSection`) | `Amoxicillin 500mg · Capsule` (`25 Capsules`, `~8 day(s) supply`), `Dosage Regimen & Schedule`, and `Prescription & Repeat Tracking` (`Ref: NHS-RX-94820-B`, `✓ Active`, `Repeats Remaining: 4 of 6 authorised`, `Pharmacy: St Mary's High Street Pharmacy`, `1. Request Repeat`, `2. Collect Repeat`). |
| **08** | `08-privacy.png` | `privacy.png` | **Privacy,** built in. | App lock and encrypted backups. | `SettingsScreen` (`settings`) | `Appearance & Theme` (`Light · NHS Blue`), `Privacy & App Lock` (`Biometric / Device Lock`, `Mask Lock-Screen Notifications` enabled), and `Local Data Backup & Restore` (`Encrypted JSON Backup (.nuvilo.json)` with AES-256-GCM + PBKDF2 encryption and `Export Encrypted Backup`). |

---

## 2. Google Play Policy & Medical Safety Compliance

1. **Zero Unverified Medical Claims:** All copy describes personal organisation, schedule tracking, stock accounting, and local privacy. No claims of curing, diagnosing, or preventing disease are made.
2. **Zero Prohibited Store Badges or Fake Testimonials:** No star ratings, `#1` rankings, awards, user testimonials, or Google Play / NHS endorsement logos appear anywhere in the campaign.
3. **100% Authentic UI:** Every screenshot embeds a real empirical capture from `com.example.nuvilo` running on Android 16 (`API 36`) with zero soft keyboards covering content and zero external webviews.
