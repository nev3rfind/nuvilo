# Nuvilo Phase 8 — Product Design & Motion System Specification

**Brand Tagline:** *"Medication, made manageable."*  
**Aesthetic Direction:** **Clinical Warmth & Editorial Precision** — combining NHS-inspired clinical trust (`#005EB8`) with calm, tactile surface layering, high-contrast semantic clarity, and purposeful micro-motion.

---

## 1. Brand & Tonal Color System (`Color.kt` & `Theme.kt`)

### 1.1 Primary Brand & Tonal Surfaces
| Token | Light Mode Hex | Dark Mode Hex | Role & Usage |
|---|---|---|---|
| `primary` | `#005EB8` (Nuvilo Clinical Blue) | `#6EB4FF` (Luminous Sky Blue) | Primary CTAs (`Take Dose`), active indicators, key clinical metrics |
| `onPrimary` | `#FFFFFF` | `#002F64` | Text and icons on primary actions |
| `primaryContainer` | `#E1EFFF` | `#00448A` | Hero accents, active filter chips, selected state containers |
| `onPrimaryContainer` | `#002B5B` | `#D8E9FF` | High-contrast text inside primary containers |
| `secondary` | `#007F7B` (Calm Clinical Teal) | `#5AD6D0` | Secondary accents, adherence rings, schedule indicators |
| `secondaryContainer` | `#DDF5F4` | `#004F4D` | Subtle tonal pills for dosage forms and schedule frequency |
| `background` | `#F6F8FB` (Cool Alabaster) | `#0B111A` (Deep Slate Navy) | Root screen canvas behind elevated cards |
| `surface` | `#FFFFFF` (Pure White) | `#131C27` (Elevated Slate) | Primary card surface (`Card`, `TopAppBar`, `NavigationBar`) |
| `surfaceVariant` | `#EDF2F7` (Soft Slate Tint) | `#1D2938` (Muted Slate Container) | Secondary nested containers, subtle input backgrounds, dividers |
| `onSurface` | `#0F172A` (Ink Slate 900) | `#F1F5F9` (Crisp Ice 100) | Primary headings, medication names, dose quantities |
| `onSurfaceVariant` | `#475569` (Slate 600) | `#94A3B8` (Slate 400) | Supporting metadata, timestamps, clinical disclaimers |
| `outline` / `outlineVariant` | `#CBD5E1` / `#E2E8F0` | `#334155` / `#1E293B` | Crisp `1.dp` card borders and hairline dividers |

### 1.2 Theme-Aware Semantic Status Tokens (`LocalNoveloColors`)
To eliminate hardcoded light-mode pastels in Dark Mode, all semantic status colors are provided via `MaterialTheme.noveloColors` (`NoveloSemanticColors`):

| Semantic State | Light Foreground | Light Container | Dark Foreground | Dark Container | Required Pairing |
|---|---|---|---|---|---|
| **Taken / Verified** (`taken`) | `#0D6832` | `#E6F4EA` | `#6EE7B7` | `#064E3B` | Checkmark icon + `"Taken"` / `"Active"` label |
| **Assumed (Auto)** (`assumed`) | `#005A8D` | `#E0F2FE` | `#7DD3FC` | `#0C4A6E` | Schedule/Auto icon + `"Assumed"` label |
| **Skipped / Neutral** (`skipped`) | `#475569` | `#F1F5F9` | `#CBD5E1` | `#1E293B` | Dash/Skip icon + `"Skipped"` / `"Archived"` label |
| **Missed / Danger** (`missed`) | `#B91C1C` | `#FEE2E2` | `#FCA5A5` | `#7F1D1D` | Warning/Close icon + `"Missed"` / `"Expired"` label |
| **Warning / Low Stock** (`warning`) | `#92400E` | `#FEF3C7` | `#FCD34D` | `#78350F` | Alert icon + `"Low Stock"` / `"Expiring Soon"` label |

> **Accessibility Guarantee:** Every foreground/container pair achieves $\ge 5.2:1$ contrast in both Light and Dark themes (exceeding the WCAG AA $4.5:1$ threshold), and status is **never** communicated by color alone.

---

## 2. Typography Hierarchy (`Type.kt`)

Nuvilo uses a clean, high-legibility geometric/humanist sans-serif scale engineered for effortless scanning of drug names, strengths, and quantities:

| Style | Size / Line Height | Weight | Letter Spacing | Primary Use Case |
|---|---|---|---|---|
| `displaySmall` | `32.sp` / `38.sp` | `ExtraBold (800)` | `-0.5.sp` | Hero stock balance numbers, large adherence percentage |
| `headlineMedium` | `26.sp` / `32.sp` | `Bold (700)` | `-0.3.sp` | Top-level screen titles (`Today`, `Medications`, `Adherence`) |
| `headlineSmall` | `22.sp` / `28.sp` | `Bold (700)` | `-0.2.sp` | Hero Dose medication name, detail header title |
| `titleLarge` | `19.sp` / `25.sp` | `Bold (700)` | `-0.1.sp` | Dialog headers, wizard step titles, major card titles |
| `titleMedium` | `16.sp` / `22.sp` | `SemiBold (600)` | `0.sp` | Medication list card names, section headers |
| `titleSmall` | `14.sp` / `20.sp` | `SemiBold (600)` | `0.1.sp` | Sub-card titles, lot numbers, prescription references |
| `bodyLarge` | `16.sp` / `24.sp` | `Normal (400)` | `0.1.sp` | Primary body copy, settings item labels |
| `bodyMedium` | `14.sp` / `20.sp` | `Normal (400)` | `0.15.sp` | Secondary descriptions, schedule summaries, dialog notes |
| `bodySmall` | `12.sp` / `17.sp` | `Normal (400)` | `0.2.sp` | Clinical disclaimers, audit timestamps, subtle hints |
| `labelLarge` | `14.sp` / `20.sp` | `SemiBold (600)` | `0.1.sp` | Primary & secondary button labels, segmented tab labels |
| `labelMedium` | `12.sp` / `16.sp` | `SemiBold (600)` | `0.3.sp` | Status badges, filter chips, pill tags |
| `labelSmall` | `11.sp` / `15.sp` | `Medium (500)` | `0.4.sp` | Overlines (`NEXT SCHEDULED DOSE`), axis labels, compact tags |

---

## 3. Spacing, Shape & Surface Elevation System (`Shape.kt`)

### 3.1 4dp / 8dp Spatial Grid
- **Screen Horizontal Padding:** `16.dp` standard (`20.dp` on Hero banners).
- **Vertical Section Spacing:** `16.dp` between primary cards; `10.dp` between grouped timeline/history items.
- **Internal Card Padding:** `20.dp` for Hero/Primary cards; `16.dp` for standard cards; `12.dp` for compact rows.
- **Minimum Touch Target:** `48.dp x 48.dp` on all interactive buttons, chips, and icon actions.

### 3.2 Corner Radius Scale (`Shape.kt`)
- `extraSmall` (`8.dp`): Compact status badges, inline code/lot pills, weekday chips.
- `small` (`12.dp`): Buttons, text fields, nested info banners, timeline rows.
- `medium` (`16.dp`): Standard medication cards, settings group cards, calendar day cells.
- `large` (`22.dp`): Hero Dose card, Stock Runway hero card, modal dialogs.
- `extraLarge` (`28.dp`): Bottom sheets, full-width pill bars.

### 3.3 Surface Layering vs Card Overuse
Instead of wrapping every piece of text in an identical bordered card:
1. **Level 1 — Hero Elevated Surface:** Rich primary or tonal container (`22.dp` radius, subtle accent border, prominent typography) reserved for **at most one focal point per screen** (e.g., `HeroDoseCard` on Today, `StockRunwayHeroCard` on Medication Detail, `MonthlyAdherenceHeroCard` on Calendar).
2. **Level 2 — Structured Section Surface:** Clean `surface` card (`16.dp` radius, `1.dp` `outlineVariant` border) for interactive groupings.
3. **Level 3 — Tonal Inline Row / Collapsible Drawer:** Soft `surfaceVariant` background (`12.dp` radius, no heavy border) for secondary audit trails, calculation explanations, and disclaimers.

---

## 4. Motion, Micro-Animations & Haptics (`Motion.kt`)

All animations use deterministic, fast-out-slow-in spring and tween curves via `NoveloMotion`:
- **Screen & Tab Transitions:** `220ms` crossfade + subtle `12.dp` vertical/horizontal slide (`AnimatedContent`).
- **Hero Dose Resolution:** Smooth `AnimatedContent` transition (`fadeIn(240ms) + scaleIn(0.96f)` / `fadeOut(180ms)`) when the final pending dose is marked Taken and transitions to `CompletedDayCard`.
- **Progress Bars & Counters:** `animateFloatAsState` (`Spring.StiffnessLow`) for daily adherence progress and repeat prescription progress bars.
- **Collapsible Sections:** `AnimatedVisibility` with `expandVertically() + fadeIn()` / `shrinkVertically() + fadeOut()` and animated chevron rotation (`animateFloatAsState` `0f -> 180f`) for Batch Lots, Stock Ledger Audit Log, and System Diagnostics.
- **Haptic Feedback:** `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` on primary clinical confirmations (`Take Dose`, `Confirm Restock`, `Save Medication`).
