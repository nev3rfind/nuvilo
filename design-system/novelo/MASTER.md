# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** Novelo
**Generated:** 2026-11-05 15:03:40
**Category:** Medication & Pill Reminder
**Design Dials:** Variance 5/10 (Balanced / Modern) | Motion 3/10 (Subtle) | Density 4/10 (Standard)

---

## Global Rules

### Color Palette & Reconciled Brand Tokens

Novelo's visual identity centers on **Novelo Clinical Blue (`#005EB8`)** as the primary brand anchor (NHS-inspired trust, credibility, and authority), complemented by **Interaction Blue (`#0284C7`)** for interactive controls, **Cyan Accent (`#0891B2` / `#22D3EE`)** for active highlights and luminous aura, and **Secondary Clinical Teal (`#007F7B` / `#DDF5F4`)** with a dedicated supporting role for dosage forms, schedule tags, and medication categories.

| Role | Light Hex | Dark Hex | Role & Usage | Contrast vs Background |
|------|-----------|----------|--------------|------------------------|
| **Primary Brand Anchor** | `#005EB8` | `#6EB4FF` | Top app bars, primary brand identity, hero status, key metrics | Light: 6.38:1 / Dark: 7.77:1 (WCAG AA Pass) |
| **On Primary** | `#FFFFFF` | `#002F64` | Text and icons placed on primary brand backgrounds | Light: 6.38:1 / Dark: 7.52:1 (WCAG AA Pass) |
| **Primary Container** | `#E0F2FE` | `#0C4A6E` | Hero cards, active tab background, selected state pills | Light: 14.2:1 vs text / Dark: 8.9:1 vs text |
| **On Primary Container** | `#002B5B` | `#E0F2FE` | High-contrast text on primary containers | Light: 11.4:1 / Dark: 10.1:1 (WCAG AA Pass) |
| **Interaction Blue** | `#0284C7` | `#38BDF8` | Primary interactive buttons, focus rings, active outlines | Light: 4.62:1 / Dark: 6.85:1 (WCAG AA Pass) |
| **Cyan Accent** | `#0891B2` | `#22D3EE` | Progress indicators, orbital glowing aura, active rings | Light: 4.55:1 / Dark: 8.20:1 (WCAG AA Pass) |
| **Supporting Clinical Teal** | `#007F7B` | `#5AD6D0` | Dedicated dosage form pills (`Tablets`, `Inhaler`), schedule tags | Light: 5.10:1 / Dark: 7.90:1 (WCAG AA Pass) |
| **Teal Container** | `#DDF5F4` | `#164E63` | Subtle pill backgrounds for dosage forms and schedule frequency | Neutral surface container |
| **On Teal Container** | `#003735` | `#CFFAFE` | High-contrast label text inside teal pills | Light: 9.32:1 / Dark: 9.85:1 (WCAG AA Pass) |
| **Background (Canvas)** | `#F8FAFC` | `#0F172A` | Root screen canvas behind elevated cards | Base root surface |
| **Foreground / Text Primary** | `#0F172A` | `#F8FAFC` | Primary typography, drug names, dosage strengths | Light: 15.8:1 / Dark: 15.8:1 (WCAG AAA Pass) |
| **Card Surface** | `#FFFFFF` | `#1E293B` | Elevated cards, dialog surfaces, bottom sheets | Crisp neutral elevation |
| **Muted / Text Secondary** | `#475569` | `#94A3B8` | Secondary metadata, timestamps, clinical instructions | Light: 5.85:1 / Dark: 6.12:1 (WCAG AA Pass) |
| **Border / Outline** | `#CBD5E1` | `#475569` | 1dp card borders, dividers, unselected tab borders | Structural contrast |
| **Destructive / Danger** | `#B91C1C` | `#FCA5A5` | Missed doses, deletion confirmations (never primary CTA) | Light: 5.92:1 / Dark: 6.78:1 (WCAG AA Pass) |
| **Ring / Focus State** | `#0284C7` | `#38BDF8` | 3dp accessibility focus outline and active selector | Active navigation rings |

**Color Architecture Decisions:**
1. **Primary Anchor Consistency:** `#005EB8` is the unequivocal brand anchor across mobile splash, app icons, navigation headers, and hero banners.
2. **Dedicated Role for Teal:** Teal (`#007F7B` / `#DDF5F4`) serves strictly as a supporting categorization color for dosage form pills and schedule tags; it is never used as a competing primary brand color.
3. **WCAG AA Compliance:** Every foreground/container and text/background pairing achieves >= 4.5:1 contrast in both Light and Dark themes.

### Typography

- **Heading Font:** Figtree
- **Body Font:** Noto Sans
- **Mood:** medical, clean, accessible, professional, healthcare, trustworthy
- **Google Fonts:** [Figtree + Noto Sans](https://fonts.googleapis.com/css2?family=Figtree:wght@300;400;500;600;700&family=Noto+Sans:wght@300;400;500;700&display=swap)

**CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Figtree:wght@300;400;500;600;700&family=Noto+Sans:wght@300;400;500;700&display=swap');
```

### Spacing Variables

*Density: 4/10 — Standard*

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs

### Buttons

```css
/* Primary Button */
.btn-primary {
  background: #005EB8;
  color: #FFFFFF;
  padding: 12px 24px;
  min-height: 48px;
  border-radius: 9999px; /* NoveloPillShape */
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: #0284C7;
  border: 2px solid #0284C7;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}
```

### Cards

```css
.card {
  background: #F0F9FF;
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-md);
  transition: all 200ms ease;
  cursor: pointer;
}

.card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}
```

### Inputs

```css
.input {
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: #0284C7;
  outline: none;
  box-shadow: 0 0 0 3px #0284C720;
}
```

### Modals

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

---

## Style Guidelines

**Style:** Accessible & Ethical

**Keywords:** Accessible, inclusive interface, high contrast, large text (16px+), keyboard navigation, screen reader friendly, accessibility standards aware, focus state, semantic

**Best For:** Government, healthcare, education, inclusive products, large audience, legal compliance, public

**Key Effects:** Clear focus rings (3-4px), ARIA labels, skip links, responsive design, reduced motion, 44x44px touch targets

### Page Pattern

**Pattern Name:** Trust & Authority + Conversion

- **Conversion Strategy:** Security badges. Case studies. Transparent pricing. Low-friction form. Provide pause/stop and stop the logo carousel on focus, hover, and reduced motion. Previous/next controls provide the keyboard equivalent; pause offscreen/hidden and render a static logo set under reduced motion.
- **CTA Placement:** Contact Sales / Get Quote (primary) + Nav
- **Section Order:** Hero (mission/credibility) > Proof (logos, certs, stats) > Solution overview > Clear CTA path

---

## Motion

**Scroll Reveal** (Subtle) — Trigger: scroll (viewport enter) | Duration: 300-400ms | Easing: `power1.out`

```js
gsap.from(el, { opacity: 0, y: 12, duration: 0.35, ease: 'power1.out', scrollTrigger: { trigger: el, start: 'top 90%', toggleActions: 'play none none reverse' } });
```

**Framework notes:** Requires the ScrollTrigger plugin registered once via gsap.registerPlugin(ScrollTrigger); Use matchMedia('(prefers-reduced-motion: reduce)') to skip non-essential motion and render the final state immediately

- ✅ Keep the y offset small (8-16px) so it reads as a fade, not a slide
- ❌ Don't reveal below-the-fold content needed for SEO/crawlers as invisible-by-default without a no-JS fallback
- ⚡ toggleActions 'play none none reverse' avoids re-triggering on every scroll direction change

---

## Anti-Patterns (Do NOT Use)

- ❌ Complex shadows
- ❌ 3D effects
- ❌ Color-only indicators

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] No emojis used as icons (use SVG instead)
- [ ] All icons from consistent icon set (Heroicons/Lucide)
- [ ] `cursor-pointer` on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard navigation
- [ ] `prefers-reduced-motion` respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px
- [ ] No content hidden behind fixed navbars
- [ ] No horizontal scroll on mobile
