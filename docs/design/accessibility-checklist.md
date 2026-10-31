# Novelo Accessibility & Inclusion Checklist (WCAG 2.1 AA)

Medication applications serve diverse user demographics, including elderly adults, individuals with visual impairments, tremor or dexterity challenges, and neurodivergent users. Accessibility is not an afterthought; it is a foundational architectural requirement.

---

## 1. Touch Targets & Motor Accessibility
- [ ] **Minimum Target Size**: All buttons, checkboxes, chips, and interactive cards must have a touch target of at least `48 x 48 dp` (`Modifier.minimumInteractiveComponentSize()`).
- [ ] **Spacing Between Targets**: Interactive targets must maintain at least `8dp` of visual spacing to prevent accidental mis-taps.
- [ ] **Large Confirmation Buttons**: High-importance actions (such as "Take Dose") must feature prominent touch surfaces across the full card width or generous pill boundaries (`heightIn(min = 52.dp)`).

---

## 2. Color Contrast & Visual Perception
- [ ] **Normal Text Contrast**: Body text, labels, and icons must achieve a contrast ratio of at least **4.5:1** against their background in both Light Mode and Dark Mode.
- [ ] **Large Text Contrast**: Headlines and numbers (above 18sp bold or 24sp regular) must achieve at least **3.0:1** contrast.
- [ ] **No Color-Only Communication**:
  - Doses marked `TAKEN` use green color **AND** a distinct checkmark icon **AND** the explicit text "Taken".
  - Doses marked `MISSED` use red color **AND** a warning triangle icon **AND** the explicit text "Missed".
  - Low stock uses amber **AND** an alert box icon **AND** text "Low Stock".
- [ ] **Color Blindness Testing**: Verify all screens under Protanopia, Deuteranopia, and Tritanopia color blindness simulation filters.

---

## 3. Screen Reader & TalkBack Semantics
- [ ] **Content Descriptions**: Every `Icon` and `Image` must have a descriptive, localized `contentDescription` explaining its function, or explicitly declare `contentDescription = null` for purely decorative visual elements.
- [ ] **Merged Tree Semantics**: Medication cards must merge their inner semantics (`Modifier.semantics(mergeDescendants = true)`) so TalkBack announces a coherent sentence:
  *"Metformin 500mg, 1 tablet due at 8:00 PM, 14 tablets remaining, Double tap to take dose."*
- [ ] **Live Announcements for Actions**: When a dose is recorded or snoozed, use `LocalAccessibilityManager` or snackbar announcements so blind users receive immediate auditory confirmation of the action.

---

## 4. Typography & Display Scaling
- [ ] **Font Scaling Support**: All text styles must use scalable units (`sp`), never hardcoded pixel dimensions (`dp`).
- [ ] **200% Font Scaling Test**: Test all 12 core screens with Android system font size set to Maximum (200%). Verify that:
  - Text wraps gracefully without truncating critical drug names or dosages.
  - Buttons and cards expand dynamically without clipping text.
- [ ] **High Contrast Text Support**: UI elements must maintain visual hierarchy when Android's "High Contrast Text" system toggle is activated.

---

## 5. Focus & Assistive Input
- [ ] **Keyboard / Switch Access Navigation**: Full navigation possible using external keyboards, D-pads, or switch access devices without requiring touch screen input.
- [ ] **Logical Focus Order**: Focus traverses top-to-bottom, left-to-right matching reading direction.

---

## 6. Motion & Cognitive Load
- [ ] **Reduced Motion Support**: Honor the system setting `accessibilityManager.isReduceMotionEnabled`. Disable non-essential transitions when active.
- [ ] **Clear Language**: Avoid medical jargon where plain language suffices (e.g., "Refills Remaining" rather than "Dispensing Repeats").
- [ ] **Reversible Actions**: Provide immediate "Undo" snackbars for accidental dose confirmations or skips.
