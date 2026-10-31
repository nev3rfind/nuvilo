# Novelo Brand & Visual Identity Direction

## 1. Brand Philosophy
Novelo is a precision medication management companion. Individuals taking daily medications—whether managing complex chronic illnesses, recovering from surgery, or adhering to daily wellness routines—frequently experience cognitive overload and anxiety regarding missed doses or running out of supplies.

Novelo's visual identity communicates:
- **Calm**: Reduces medical anxiety through spacious, clean, uncluttered layouts.
- **Precision**: Crisp typography and exact figures instil confidence in stock and schedule calculations.
- **Discretion**: Quiet, respectful UI that protects personal health privacy in public environments.
- **Clarity**: High contrast, unambiguous status indicators, and immediate glanceability.

> **Design Reference**: Inspired by the clean, accessible, public-service clarity of NHS digital standards (trusted blue, high-contrast white, legible sans-serif typography). **Important**: Novelo does not use NHS trademarks, proprietary logos, or wording implying official endorsement.

---

## 2. Color Palette & Emotional Mapping

### Light Mode Palette
- **Primary Brand (Healthcare Blue)**: `#005EB8` (Deep, trusted, accessible blue with 7:1 contrast on white).
- **Primary Variant (Deep Navy)**: `#003087` (Headers, focused states, and high-emphasis text).
- **Secondary / Accent (Aqua Tint)**: `#00A9CE` (Selected chips, calendar highlights, active tabs).
- **Neutral Light (Background)**: `#F4F6F8` (Cool, calming soft grey-white; eliminates stark eye strain).
- **Neutral Surface**: `#FFFFFF` (Pure white card surfaces with subtle 1dp border `#E2E8F0`).
- **Text High Emphasis**: `#0F172A` (Slate navy, maximum legibility).
- **Text Medium Emphasis**: `#475569` (Subtitles, units, secondary timestamps).

### Dark Mode Palette (Deliberately Crafted Surfaces)
Novelo avoids harsh pure-black (`#000000`) or naive color inversions:
- **Dark Background**: `#0F141C` (Deep blue-tinted slate; soothing for nighttime dose checks).
- **Dark Surface Primary**: `#18202C` (Elevated card surface; distinct from background).
- **Dark Surface Secondary**: `#222D3D` (Input fields, dialog containers, and secondary cards).
- **Primary Accent Dark**: `#3898EC` (Vibrant yet accessible blue; 6.2:1 contrast against `#18202C`).
- **Text High Emphasis Dark**: `#F8FAFC` (Near white, gentle on night-adapted eyes).
- **Text Medium Emphasis Dark**: `#94A3B8` (Slate grey for secondary details).

### Functional / Semantic Palette
Status is **never** communicated by color alone. Every semantic state is paired with an explicit icon and text label:
- **Confirmed Taken (`TAKEN`)**: `#007F3B` (Healthcare Green) / Dark: `#2ECC71` + Checkmark icon.
- **Assumed (`ASSUMED`)**: `#008299` (Teal) / Dark: `#00D2D3` + Clock-check icon.
- **Skipped (`SKIPPED`)**: `#64748B` (Neutral Slate) / Dark: `#94A3B8` + Forward arrow icon.
- **Missed / Overdue (`MISSED`)**: `#C8102E` (Medical Red) / Dark: `#FF6B6B` + Alert triangle icon.
- **Low Stock / Refill Warning**: `#D97706` (Amber Warning) / Dark: `#F59E0B` + Box warning icon.

---

## 3. Typography Direction

Novelo uses Android's native system typeface (`Roboto` / `MaterialTheme.typography`) styled for clinical legibility:
- **Display / App Header**: Bold, assertive, warm (`HeadlineMedium`, 28sp).
- **Section Headers**: Clear, scannable titles (`TitleMedium`, 18sp, SemiBold).
- **Dose & Stock Numbers**: Prominent tabular figures (`HeadlineLarge`, 32sp) for effortless reading by elderly or visually impaired users.
- **Body & Labels**: Generous line spacing (1.4x) and strong contrast (`BodyLarge`, 16sp; `BodyMedium`, 14sp).

---

## 4. Shapes, Elevation & Motion

- **Corner Radii**:
  - Cards & Containers: `16dp` (Soft, friendly, approachable).
  - Buttons & Interactive Chips: `12dp` or `24dp` (Pill shapes for quick actions).
  - Input Fields: `10dp`.
- **Elevation**:
  - Flat/Subtle: 0dp to 2dp elevation with a crisp 1dp subtle border (`#E2E8F0` light / `#2A3546` dark) to create clear spatial separation without heavy drop-shadows.
- **Motion**:
  - Restrained, purposeful feedback: 150ms–250ms ease-out transitions for card taps and dose confirmation state morphing.
  - Zero decorative bouncing or distracting celebratory particle animations.
