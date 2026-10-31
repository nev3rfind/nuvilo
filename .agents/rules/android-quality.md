---
trigger: always_on
---

# Android Engineering & Quality Guidelines for Novelo

1. **Architecture & Layers**:
   - Follow clean unidirectional data flow: UI (Compose) -> ViewModel (StateFlow) -> UseCase/Repository -> DataSource (Room/DataStore).
   - ViewModels expose a single immutable `uiState: StateFlow<UiState>`.
   - Never expose mutable state flows (`MutableStateFlow`) outside ViewModel boundaries.
   - Database entities must never be leaked directly to Composable UI layers without domain mapping.

2. **Jetpack Compose Best Practices**:
   - Strictly adhere to Edge-to-Edge (`enableEdgeToEdge()`) with proper window insets handling (`WindowInsets.safeDrawing`, `statusBarsPadding()`, `navigationBarsPadding()`).
   - Composable functions must accept a `Modifier = Modifier` parameter as their first optional parameter and chain off it.
   - Avoid unstable parameters in composable signatures to prevent unnecessary recompositions.
   - Use `@Preview(showBackground = true)` for UI previews in light and dark mode.

3. **Accessibility (WCAG AA)**:
   - All interactive touch targets must meet the minimum 48x48dp size (`minimumInteractiveComponentSize`).
   - All icons and images must provide a localized `contentDescription` or explicitly declare `contentDescription = null` for decorative assets.
   - Color contrast ratio must achieve at least 4.5:1 for body text and 3:1 for large text / key UI elements.
   - Status must never be conveyed by color alone; pair colors with icons or textual labels.

4. **Concurrency & Coroutines**:
   - Inject dispatchers (`CoroutineDispatcher`) to enable deterministic unit testing with `StandardTestDispatcher` / `TestScope`.
   - Never hardcode `Dispatchers.IO` or `Dispatchers.Default` directly in repository or use case implementations.
   - Collect flows in UI with `collectAsStateWithLifecycle()` to avoid resource leakage when the UI is stopped.

5. **Testing Requirements**:
   - Every domain use case and stock calculation must have comprehensive JUnit tests.
   - Room DAOs and schema migrations must be tested with AndroidJUnit4 in-memory/test database.
   - Test names should be descriptive, following the `given_when_then` or `condition_expectedResult` pattern.
