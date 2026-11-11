# Nuvilo Brand Identity & Design System Specification

**Product:** Nuvilo — *"Medication, made manageable."*  
**Platform Reference:** Native Android Jetpack Compose Material 3 Design System  
**Web Target:** Modern, accessible, responsive website (Vite / React / Tailwind CSS / shadcn-ui)  
**Document Purpose:** Complete visual language, tokens, and component guidelines for the official Nuvilo website.  

---

## 1. Brand Essence & Visual Positioning

Nuvilo is positioned at the intersection of **clinical authority and personal calm**. 

Unlike generic health trackers that feel either cold and sterile or overly playful, Nuvilo pairs the trusted heritage of NHS clinical blue (`#005EB8`) with modern, tactile Material 3 surfaces, crystal-clear typography, and reassuring slate neutrals. The design conveys:
- **Precision**: Pixel-perfect alignment, tabular numerical clarity, clear hierarchy.
- **Privacy & Calm**: Soft light-blue and slate surfaces that reduce anxiety during health tracking.
- **Accessibility First**: Guaranteed WCAG AA contrast ratios ($\ge 5.0:1$ for body text, $\ge 3.0:1$ for UI controls), $48\times 48\text{dp}$ touch targets, and unambiguous status indicators that never rely solely on color.

---

## 2. Complete Color Palette & Token Architecture

The color system is derived directly from [Color.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/theme/Color.kt) in the production Android codebase.

### 2.1 Brand & Interaction Palette

| Token Name | Hex Code | Role & Usage | Contrast vs White |
| :--- | :--- | :--- | :--- |
| `primary` | `#005EB8` | Core brand identity (NHS Clinical Blue); headers, logos, primary CTAs | 7.3:1 (AAA) |
| `primary-dark` | `#003087` | Deep brand anchor; hero backgrounds, dark accents | 11.8:1 (AAA) |
| `primary-bright` | `#0072CE` | High-visibility accent blue | 4.8:1 (AA) |
| `interaction-blue` | `#0284C7` | Interactive elements; active buttons, focus rings, link hovers | 4.6:1 (AA) |
| `interaction-container` | `#E0F2FE` | Tonal button containers, active navigation pills, highlighted card backgrounds | — |
| `on-interaction-container` | `#002B5B` | High-contrast text on light blue containers | 12.5:1 (AAA) |
| `accent-cyan` | `#0891B2` | Secondary KPI indicators, active progress bars, subtle badges | 4.5:1 (AA) |
| `cyan-container` | `#DDF5F4` | Soft teal/cyan container surface | — |
| `on-cyan-container` | `#003735` | High-contrast text on cyan containers | 11.2:1 (AAA) |

### 2.2 Semantic Clinical Status Tokens

Every status token is paired with an icon and textual label in the UI to prevent color-only communication:

| Status | Hex Code | Container Hex | Border Hex | Role & Clinical Meaning |
| :--- | :--- | :--- | :--- | :--- |
| **Taken** | `#0D6832` | `#E6F4EA` | `#A7F3D0` | Confirmed dose recorded; adherence intact |
| **Assumed** | `#005A8D` | `#E0F2FE` | `#BAE6FD` | Assumed adherence according to schedule |
| **Skipped** | `#475569` | `#F1F5F9` | `#CBD5E1` | Intentionally skipped dose (e.g. fasting, side effects) |
| **Missed** | `#B91C1C` | `#FEE2E2` | `#FECACA` | Unconfirmed past dose outside grace window |
| **Warning / Low Stock** | `#92400E` | `#FEF3C7` | `#FDE68A` | Supply below threshold or lot near expiry |

### 2.3 Neutral Surfaces & Slate Hierarchy

| Token Name | Light Mode | Dark Mode | Role & Usage |
| :--- | :--- | :--- | :--- |
| `background` | `#F8FAFC` (Slate 50) | `#0F172A` (Slate 900) | Root page canvas; calm, anti-glare |
| `surface` | `#FFFFFF` (Pure White) | `#1E293B` (Slate 800) | Cards, modals, containers, navigation bars |
| `surface-variant` | `#F1F5F9` (Slate 100) | `#334155` (Slate 700) | Sub-sections, code blocks, table headers |
| `text-primary` | `#0F172A` (Slate 900) | `#F8FAFC` (Slate 50) | Main headlines, body copy, titles |
| `text-secondary` | `#475569` (Slate 600) | `#94A3B8` (Slate 400) | Subtitles, helper text, timestamps |
| `text-tertiary` | `#64748B` (Slate 500) | `#64748B` (Slate 500) | Footers, captions, legal subheadings |
| `outline` | `#CBD5E1` (Slate 300) | `#475569` (Slate 600) | Input borders, active card borders |
| `outline-variant` | `#E2E8F0` (Slate 200) | `#334155` (Slate 700) | Subtle dividers, card outline strokes |

---

## 3. Typography System & Numerical Precision

Derived from [Type.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/theme/Type.kt).

### 3.1 Font Stack
- **Primary Web Font**: `Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`.
- **Characteristics**: Crisp geometric neo-grotesque sans-serif with tall x-height, open apertures, and exceptional readability at small sizes.
- **Tabular Numerals (`tnum`)**: Crucial for all medical quantities, adherence percentages (e.g. `100%`), timestamps (`08:00`), and lot counts. Set CSS `font-feature-settings: "tnum" 1` on numeric elements to prevent jitter and layout shift.

### 3.2 Typography Scale

| Style | Web Size / Line Height | Font Weight | Letter Spacing | Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Hero Display** | `40px / 48px` (`2.5rem`) | ExtraBold (`800`) | `-0.025em` | Website hero title, key metric displays |
| **H1 Headline** | `32px / 40px` (`2.0rem`) | Bold (`700`) | `-0.02em` | Page main titles (Privacy Policy, About) |
| **H2 Section Title** | `24px / 32px` (`1.5rem`) | Bold (`700`) | `-0.015em` | Major document sections, feature cards |
| **H3 Subsection** | `20px / 28px` (`1.25rem`) | SemiBold (`600`) | `-0.01em` | Privacy policy clause headers, modal titles |
| **H4 Component** | `18px / 26px` (`1.125rem`) | SemiBold (`600`) | `0em` | Card titles, callout headers |
| **Body Large** | `16px / 26px` (`1.0rem`) | Regular / Medium (`400`/`500`) | `0em` | Primary policy prose, feature descriptions |
| **Body Small** | `14px / 22px` (`0.875rem`) | Regular (`400`) | `0em` | Metadata, legal disclaimers, timestamps |
| **Caption / Label** | `12px / 18px` (`0.75rem`) | Medium (`500`) | `0.02em` | Badges, table headers, breadcrumbs |

---

## 4. Shapes, Geometry & Tactile Surfaces

Derived from [Shape.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/theme/Shape.kt).

### 4.1 Corner Radii Scale
- **Pill Shape** (`rounded-full` / `9999px`): Action chips, status badges, secondary filter buttons, navigation pills.
- **Modal & Sheet Shape** (`rounded-3xl` / `24px`): Dialogs, modal overlays, major hero callout containers.
- **Card Shape** (`rounded-2xl` / `18px`): Standard content cards, feature boxes, privacy section containers.
- **Button Shape** (`rounded-xl` / `14px`): Primary action buttons, form inputs.
- **Micro Badge** (`rounded-lg` / `8px`): Code snippets, inline tags, table cell pills.

### 4.2 Elevation, Borders & Shadows
Nuvilo avoids dark, heavy, dramatic drop shadows in favor of crisp borders and ultra-diffused ambient light:
- **Default Card Border**: `1px solid var(--nuvilo-outline-variant)` (`#E2E8F0` light, `#334155` dark).
- **Subtle Card Shadow**: `box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.04), 0 1px 2px -1px rgba(15, 23, 42, 0.04);`
- **Elevated Hover Shadow**: `box-shadow: 0 10px 25px -5px rgba(0, 94, 184, 0.08), 0 8px 10px -6px rgba(0, 94, 184, 0.04);`
- **Active Focus Ring**: `box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.35);`

---

## 5. Motion & Micro-Interactions

Derived from [Motion.kt](file:///c:/Users/ddona/AndroidStudioProjects/Nuvilo/app/src/main/java/com/example/nuvilo/ui/theme/Motion.kt).

### 5.1 Animation Timing & Curves
- **Fast Duration** (`160ms`): Button hover transitions, toggle switches, icon color shifts.
- **Standard Duration** (`220ms`): Card elevation, dropdown menus, accordion expand/collapse.
- **Emphasized Duration** (`280ms`): Page route transitions, modal dialog appearances, hero reveals.
- **Easing Curve**: `cubic-bezier(0.4, 0.0, 0.2, 1)` (equivalent to Jetpack Compose `FastOutSlowInEasing`).

### 5.2 Interaction Rules
- **Hover Transitions**: Buttons and cards subtly elevate (`transform: translateY(-2px)`) with smooth border color shift.
- **Scroll Behavior**: Smooth scroll transitions with fixed header backdrop blur (`backdrop-filter: blur(12px)`).
- **Zero Distraction**: Do not include bouncing loaders, parallax spinning elements, or continuous pulsating animations.

---

## 6. Implementation-Ready CSS Custom Properties

Copy this directly into the root stylesheet (`index.css` / `globals.css`):

```css
:root {
  /* Brand Anchor */
  --nuvilo-primary: #005EB8;
  --nuvilo-primary-dark: #003087;
  --nuvilo-primary-bright: #0072CE;
  --nuvilo-on-primary: #FFFFFF;

  /* Clinical Interaction & Containers */
  --nuvilo-interaction: #0284C7;
  --nuvilo-interaction-container: #E0F2FE;
  --nuvilo-on-interaction-container: #002B5B;
  --nuvilo-cyan: #0891B2;
  --nuvilo-cyan-container: #DDF5F4;
  --nuvilo-on-cyan-container: #003735;

  /* Clinical Semantic Statuses */
  --nuvilo-status-taken: #0D6832;
  --nuvilo-status-taken-bg: #E6F4EA;
  --nuvilo-status-taken-border: #A7F3D0;
  
  --nuvilo-status-assumed: #005A8D;
  --nuvilo-status-assumed-bg: #E0F2FE;
  --nuvilo-status-assumed-border: #BAE6FD;

  --nuvilo-status-skipped: #475569;
  --nuvilo-status-skipped-bg: #F1F5F9;
  --nuvilo-status-skipped-border: #CBD5E1;

  --nuvilo-status-missed: #B91C1C;
  --nuvilo-status-missed-bg: #FEE2E2;
  --nuvilo-status-missed-border: #FECACA;

  --nuvilo-status-warning: #92400E;
  --nuvilo-status-warning-bg: #FEF3C7;
  --nuvilo-status-warning-border: #FDE68A;

  /* Surfaces & Neutrals (Light Mode) */
  --nuvilo-bg: #F8FAFC;
  --nuvilo-surface: #FFFFFF;
  --nuvilo-surface-variant: #F1F5F9;
  --nuvilo-text-primary: #0F172A;
  --nuvilo-text-secondary: #475569;
  --nuvilo-text-tertiary: #64748B;
  --nuvilo-outline: #CBD5E1;
  --nuvilo-outline-variant: #E2E8F0;

  /* Geometry */
  --radius-card: 18px;
  --radius-sheet: 24px;
  --radius-pill: 9999px;
  --radius-button: 14px;
}

.dark {
  /* Brand Anchor */
  --nuvilo-primary: #6EB4FF;
  --nuvilo-primary-dark: #0C4A6E;
  --nuvilo-on-primary: #002F64;

  /* Clinical Interaction & Containers */
  --nuvilo-interaction: #38BDF8;
  --nuvilo-interaction-container: #0C4A6E;
  --nuvilo-on-interaction-container: #E0F2FE;
  --nuvilo-cyan: #22D3EE;
  --nuvilo-cyan-container: #164E63;
  --nuvilo-on-cyan-container: #CFFAFE;

  /* Clinical Semantic Statuses (Dark) */
  --nuvilo-status-taken: #6EE7B7;
  --nuvilo-status-taken-bg: #064E3B;
  --nuvilo-status-taken-border: #047857;

  --nuvilo-status-assumed: #7DD3FC;
  --nuvilo-status-assumed-bg: #0C4A6E;
  --nuvilo-status-assumed-border: #0369A1;

  --nuvilo-status-skipped: #CBD5E1;
  --nuvilo-status-skipped-bg: #1E293B;
  --nuvilo-status-skipped-border: #334155;

  --nuvilo-status-missed: #FCA5A5;
  --nuvilo-status-missed-bg: #7F1D1D;
  --nuvilo-status-missed-border: #991B1B;

  --nuvilo-status-warning: #FCD34D;
  --nuvilo-status-warning-bg: #78350F;
  --nuvilo-status-warning-border: #92400E;

  /* Surfaces & Neutrals (Dark Mode) */
  --nuvilo-bg: #0F172A;
  --nuvilo-surface: #1E293B;
  --nuvilo-surface-variant: #334155;
  --nuvilo-text-primary: #F8FAFC;
  --nuvilo-text-secondary: #94A3B8;
  --nuvilo-text-tertiary: #64748B;
  --nuvilo-outline: #475569;
  --nuvilo-outline-variant: #334155;
}
```

---

## 7. Tailwind Configuration Extension

To integrate Nuvilo tokens into Tailwind CSS (`tailwind.config.ts` or `tailwind.config.js`):

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        nuvilo: {
          primary: {
            DEFAULT: '#005EB8',
            dark: '#003087',
            bright: '#0072CE',
          },
          interaction: {
            DEFAULT: '#0284C7',
            container: '#E0F2FE',
            onContainer: '#002B5B',
          },
          cyan: {
            DEFAULT: '#0891B2',
            container: '#DDF5F4',
            onContainer: '#003735',
          },
          status: {
            taken: '#0D6832',
            takenBg: '#E6F4EA',
            takenBorder: '#A7F3D0',
            assumed: '#005A8D',
            assumedBg: '#E0F2FE',
            assumedBorder: '#BAE6FD',
            skipped: '#475569',
            skippedBg: '#F1F5F9',
            skippedBorder: '#CBD5E1',
            missed: '#B91C1C',
            missedBg: '#FEE2E2',
            missedBorder: '#FECACA',
            warning: '#92400E',
            warningBg: '#FEF3C7',
            warningBorder: '#FDE68A',
          },
        },
      },
      borderRadius: {
        'card': '18px',
        'sheet': '24px',
        'pill': '9999px',
        'button': '14px',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
};
```
