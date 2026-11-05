## UI Pro Max Stack Guidelines
**Stack:** jetpack-compose | **Query:** premium Material 3 native Android interfaces accessible typography responsive layouts information hierarchy component states motion
**Source:** stacks/jetpack-compose.csv | **Found:** 9 results

### Result 1
- **Category:** Theming
- **Guideline:** No hardcoded text style
- **Description:** Use typography
- **Do:** MaterialTheme.typography
- **Don't:** Hardcode sp
- **Code Good:** Scalable
- **Code Bad:** Inconsistent
- **Severity:** Medium
- **Docs URL:** 
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 2
- **Category:** Accessibility
- **Guideline:** Content description
- **Description:** Accessible UI
- **Do:** contentDescription
- **Don't:** Ignore a11y
- **Code Good:** Inclusive
- **Code Bad:** A11y fail
- **Severity:** Medium
- **Docs URL:** https://developer.android.com/jetpack/compose/accessibility
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 3
- **Category:** Preview
- **Guideline:** Multiple previews
- **Description:** Preview multiple states
- **Do:** @Preview
- **Don't:** Single preview
- **Code Good:** Better dev UX
- **Code Bad:** Misleading
- **Severity:** Low
- **Docs URL:** https://developer.android.com/jetpack/compose/tooling/preview
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 4
- **Category:** Layout
- **Guideline:** Extract reusable layout patterns
- **Description:** Repeated layouts should be shared
- **Do:** Create layout composable
- **Don't:** Copy-paste layouts
- **Code Good:** Reusable scaffold
- **Code Bad:** Duplicated layout
- **Severity:** High
- **Docs URL:** https://developer.android.com/develop/ui/compose/components
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 5
- **Category:** Layout
- **Guideline:** Avoid deep layout nesting
- **Description:** Deep layout trees increase measure & layout cost
- **Do:** Keep layout flat
- **Don't:** Box ? Column ? Box ? Row
- **Code Good:** Flat hierarchy
- **Code Bad:** Deep nested tree
- **Severity:** High
- **Docs URL:** https://developer.android.com/develop/ui/compose/layouts/basics
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 6
- **Category:** State
- **Guideline:** Single source of truth
- **Description:** UI state comes from one source
- **Do:** StateFlow from VM
- **Don't:** Multiple states
- **Code Good:** Unified UiState
- **Code Bad:** Scattered state
- **Severity:** High
- **Docs URL:** https://developer.android.com/topic/architecture/ui-layer
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 7
- **Category:** Layout
- **Guideline:** Prefer LazyColumn over Column scroll
- **Description:** Lazy layouts are virtualized and efficient
- **Do:** LazyColumn
- **Don't:** Column.verticalScroll()
- **Code Good:** Lazy list
- **Code Bad:** Scrollable Column
- **Severity:** High
- **Docs URL:** https://developer.android.com/jetpack/compose/lists
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 8
- **Category:** Layout
- **Guideline:** Use Row/Column for linear layout
- **Description:** Linear layouts are simpler and more performant
- **Do:** Use Row / Column
- **Don't:** Custom layout for simple cases
- **Code Good:** Row/Column usage
- **Code Bad:** Over-engineered layout
- **Severity:** High
- **Docs URL:** https://developer.android.com/develop/ui/compose/layouts/basics#row
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

### Result 9
- **Category:** Layout
- **Guideline:** Prefer Modifier over extra layouts
- **Description:** Use Modifier to adjust layout instead of adding wrapper composables
- **Do:** Use Modifier.padding()
- **Don't:** Wrap content with extra Box
- **Code Good:** Padding via modifier
- **Code Bad:** Box just for padding
- **Severity:** High
- **Docs URL:** https://developer.android.com/jetpack/compose/modifiers
- **Applies To:** jetpack-compose 1.11.4 (current stable)
- **Status:** active
- **Verified At:** 2026-08-13

