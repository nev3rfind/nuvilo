# Nuvilo Phase 11 — Design Refinement Report & Token Specification

**Author:** Agent 1 (UI/UX Design Director)  
**Scope:** `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`, `Theme.kt`, `ui/components/NoveloComponents.kt`

---

## 1. Brand & Layered Surface Palette

Nuvilo pairs NHS-inspired clinical trust blue with calm, high-contrast neutral surfaces:

| Token | Light Mode | Dark Mode (Phase 11 Refined) | Usage |
| :--- | :--- | :--- | :--- |
| **Primary Brand Anchor** | `#005EB8` (`NoveloBlue`) | `#6EB4FF` (`NoveloBlueDark`) | Primary CTAs, active tabs, brand accents |
| **Clinical Interaction** | `#0284C7` (`NoveloInteractionBlue`) | `#38BDF8` (`NoveloInteractionBlueDark`) | Focused borders, active rings, interactive controls |
| **Secondary Cyan Accent** | `#0891B2` (`NoveloCyanAccent`) | `#22D3EE` (`NoveloCyanAccentDark`) | Secondary KPI bars, privacy shield accents |
| **Primary Container** | `#E0F2FE` (`NoveloLightBlueContainer`) | `#0C4A6E` (`NoveloBlueContainerDark`) | Active pills, date chips, hero banner tints |
| **Window Background** | `#F8FAFC` (`NeutralBackgroundLight`) | `#0C1015` (`NeutralBackgroundDark`) | Deepest canvas layer (OLED-friendly obsidian-slate) |
| **Primary Card Surface** | `#FFFFFF` (`NeutralSurfaceLight`) | `#121820` (`NeutralSurfaceDark`) | Cards, bottom bar, dialogs |
| **Elevated Surface** | `#F1F5F9` (`NeutralSurfaceVariantLight`) | `#1E293B` (`NeutralSurfaceVariantDark`) | Nested tonal containers, chips, input wells |
| **Card Border** | `#E2E8F0` (`NeutralOutlineVariantLight`) | `#263244` (`NeutralOutlineVariantDark`) | Crisp 1dp hairline card separation |
| **Primary Text** | `#0F172A` / `#1E293B` | `#F8FAFC` | Headings, medication names, primary values |
| **Secondary Text** | `#475569` | `#94A3B8` | Subtitles, metadata, timestamps |

### Semantic Clinical Status Tokens (WCAG AA >= 4.5:1)
All status indicators pair color with an icon and explicit text label:
- **Taken / Active**: `#0D6832` on `#E6F4EA` (Light) | `#6EE7B7` on `#064E3B` (Dark)
- **Assumed / Scheduled**: `#005A8D` on `#E0F2FE` (Light) | `#7DD3FC` on `#0C4A6E` (Dark)
- **Warning / Expiring Soon / Low Stock**: `#92400E` on `#FEF3C7` (Light) | `#FCD34D` on `#78350F` (Dark)
- **Missed / Expired / Out of Stock**: `#B91C1C` on `#FEE2E2` (Light) | `#FCA5A5` on `#7F1D1D` (Dark)
- **Skipped / Neutral**: `#475569` on `#F1F5F9` (Light) | `#CBD5E1` on `#1E293B` (Dark)

---

## 2. Typography Hierarchy & Tabular Numerals (`Type.kt`)

- **Font Stack**: Strict offline `FontFamily.SansSerif` (zero downloadable/network fonts).
- **Tabular Figures (`fontFeatureSettings = "tnum"`)**:
  - Enabled by default on `displayLarge`, `displayMedium`, `displaySmall`, and all `NoveloNumericTypography` styles.
  - Exposed via `TextStyle.tabular` extension for inline timestamps (`HH:mm`), adherence percentages (`62%`), stock counts (`282 drops`), and PPC remaining days (`"Active · 42 days left"`).
- **Scale**:
  - `displayLarge` (`40sp/48sp ExtraBold`) for hero KPIs
  - `headlineMedium` (`23sp/29sp Bold`) for screen titles
  - `titleMedium` (`16sp/22sp SemiBold`) for medication names
  - `bodyMedium` (`14sp/20sp`) and `bodySmall` (`12sp/17sp`) for clinical instructions & disclaimers
  - `labelLarge` (`14sp`) / `labelSmall` (`11sp`) for pills, badges, and buttons

---

## 3. Component Geometry (`Shape.kt` & `NoveloComponents.kt`)

- `NoveloShapes`: `extraSmall = 8.dp`, `small = 12.dp`, `medium = 16.dp`, `large = 22.dp`, `extraLarge = 28.dp`.
- `NoveloPillShape = RoundedCornerShape(999.dp)` for CTAs, status badges, and chips.
- `NoveloCardShape = RoundedCornerShape(18.dp)` for primary cards.
- `NoveloBannerShape = RoundedCornerShape(14.dp)` (Phase 11 addition) for compact clinical banners and `PpcStatusBannerCard`.
- `NoveloSheetShape = RoundedCornerShape(24.dp)` for sheets and modals.

### Phase 11 Component Refinements in `NoveloComponents.kt`
1. **Added `PpcStatusBannerCard`**: Reusable Dashboard banner supporting `ACTIVE`, `EXPIRING_SOON`, `EXPIRED`, `NOT_YET_VALID`, and `NO_RECORD` states with icon+text+pill badge, tabular days remaining, `>= 48.dp` touch target, and mandatory `"Based on your recorded certificate dates"` notice.
2. **Fixed `+ + Stock` Redundancy & Touch Target**: Changed `MedicationItemCard` restock button label from `"+ Stock"` to `"Stock"` (alongside `Icons.Default.Add`) and increased `heightIn(min = 48.dp)` for WCAG AA compliance.
3. **Fixed Singular/Plural Package Label**: Updated `HeroDoseCard` to render `"1 Bottle total"` vs `"2 Bottles total"` instead of `"1 Bottles total"`.

---

## 4. Icon Audit

- **Bottom Navigation**: `Today`, `Medications`, `Adherence`, `Settings` — `contentDescription` on `Icon` should be `null` when paired with `NavigationBarItem` `label` to prevent duplicate TalkBack announcements.
- **Clinical Status Pairing**: Every status badge (`DoseStatusBadge`, `StockBadge`, `TrackingPrecisionBadge`, `PpcStatusBannerCard`) pairs an `Icons.Default.*` vector with high-contrast text.

---

## 5. Motion & Reduced-Motion Guidelines (`Motion.kt` & `Theme.kt`)

- **Durations**: `DURATION_FAST = 160ms`, `DURATION_STANDARD = 220ms`, `DURATION_EMPHASIZED = 280ms`, using `FastOutSlowInEasing` and `DampingRatioNoBouncy` springs.
- **Reduced-Motion Support**:
  - Added `LocalMotionEnabled` (`staticCompositionLocalOf { true }`) and `rememberMotionEnabled()` (backed by `ValueAnimator.areAnimatorsEnabled()`).
  - Provided `LocalMotionEnabled` in `NoveloTheme(darkTheme, motionEnabled)`.
  - Added `motionEnabled: Boolean = true` parameter to `NoveloMotion.screenTransition`, `heroTransition`, `directionalHorizontalTransition`, `sectionExpand`, and `sectionCollapse` — returning `EnterTransition.None` / `ExitTransition.None` when `motionEnabled == false`.
  - Dose confirmation buttons remain instant and never block on transition animations.
