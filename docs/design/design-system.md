# Novelo Design System Specification

This document details the reusable design tokens, component specifications, and interaction guidelines for **Novelo** in Jetpack Compose.

---

## 1. Design Tokens

### Spacing Scale (8dp Grid)
```kotlin
object NoveloSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

### Shape Tokens
```kotlin
object NoveloShapes {
    val card = RoundedCornerShape(16.dp)
    val button = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(50)
    val input = RoundedCornerShape(10.dp)
    val badge = RoundedCornerShape(6.dp)
}
```

### Elevation Tokens
```kotlin
object NoveloElevation {
    val flat = 0.dp
    val card = 1.dp
    val modal = 4.dp
    val dialog = 8.dp
}
```

---

## 2. Reusable Core Components

### 2.1 Medication Card (`MedicationCard`)
Displays a medication item in the list or dashboard timeline:
- **Leading Element**: Form-specific icon (pill, bottle, inhaler) inside a soft tinted circular container (`44x44dp`).
- **Title**: Medication name (`TitleMedium`, Bold, 16sp).
- **Subtitle**: Strength and form (e.g., "500 mg · Capsule").
- **Stock & Schedule Badges**: Compact row showing current stock badge (e.g., `42 left · 14 days`) and schedule chips (e.g., `08:00`, `20:00`).
- **Trailing Element**: Optional quick-action button or chevron.

### 2.2 Dose Status Indicator (`DoseStatusBadge`)
Always pairs an icon with high-contrast text:
- `TAKEN`: Green container (`#E8F5E9` light / `#1B3828` dark), check icon, text "Taken 08:05".
- `ASSUMED`: Teal container (`#E0F2F1` light / `#13383B` dark), clock-check icon, text "Assumed 08:00".
- `PENDING`: Blue container (`#E3F2FD` light / `#1A2F4C` dark), bell icon, text "Due in 25 min".
- `OVERDUE`: Red container (`#FFEBEE` light / `#3D1B22` dark), warning icon, text "Due 08:00 (Overdue)".
- `SKIPPED`: Slate container (`#ECEFF1` light / `#263238` dark), forward icon, text "Skipped".

### 2.3 Stock Indicator (`StockBadge`)
- **Normal**: Soft blue background, text `48 tablets (16 days remaining)`.
- **Low Stock**: Amber warning background (`#FFF8E1` light / `#3D2E14` dark), alert icon, text `Low: 4 left (2 days)`.
- **Out of Stock**: Bold red container, alert icon, text `0 left · Refill Needed`.
- **Indeterminate / Estimated**: Grey badge, text `Est. 1 bottle remaining`.

### 2.4 Buttons & Action Controls
- **Primary Action Button**: Full-width or generous pill (`heightIn(min = 48.dp)`), Healthcare Blue background, bold white text, subtle ripple.
- **Secondary Action Button**: Outlined button with 1.5dp border, transparent background, blue text.
- **Dose Confirmation Button**: Large action chip ("Take Dose") with haptic feedback (`HapticFeedbackType.LongPress`).

### 2.5 Empty States (`EmptyStateView`)
- Calming healthcare illustration / clean line vector.
- Reassuring header (e.g., "All caught up for today").
- Clear explanatory body text ("No remaining doses scheduled for this evening. Rest well!").
- Optional action button ("Add Medication" or "View Schedule").

### 2.6 Error & Warning Banners
- Non-blocking inline card with medical warning icon, clear description of error (e.g., "Exact alarm permission disabled"), and a direct action button ("Enable in Settings").

---

## 3. Dark Theme Specification

Dark theme surfaces are rendered using elevated tonal palettes rather than simple negative inversion:

| UI Element | Light Value | Dark Value | Notes |
| :--- | :--- | :--- | :--- |
| **Window Background** | `#F4F6F8` | `#0F141C` | Deep navy-slate background |
| **Card Surface** | `#FFFFFF` | `#18202C` | High contrast against background |
| **Input Background** | `#F8FAFC` | `#222D3D` | Clear interactive bounds |
| **Primary Text** | `#0F172A` | `#F8FAFC` | 15.8:1 contrast |
| **Secondary Text**| `#475569` | `#94A3B8` | 7.1:1 contrast |
| **Card Border** | `#E2E8F0` | `#263345` | Subtle definition |
| **Primary Button**| `#005EB8` | `#3898EC` | Brightened for dark surface contrast |
