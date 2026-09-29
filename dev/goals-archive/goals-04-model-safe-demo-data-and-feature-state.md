# GOALS 4 — Model safe demo data and feature state

```mermaid
flowchart LR
  A[Fictional seed dataset] --> B[Room repositories]
  B --> C[ViewModel StateFlow]
  C --> D[Predictable Compose UI]
```

Suggested: gpt-5.6-terra · medium — typed local data and deterministic rules provide a useful demo without the cost or risk of a financial backend.

- [x] G4.1 — Define domain models for fictional `AccountSummary`, `Transaction`, `Category`, `Budget`, `Insight`, `Alert`, `Goal`, and `AssistantMessage`. Add `isDemo`/`dataSource` metadata so every financial-looking value can be disclosed as fictional. **Done when:** the domain model cannot accidentally label demo content as synchronized bank data.
- [x] G4.2 — Seed a Brazilian Portuguese fictional dataset with several accounts, credit card purchases, installments, recurring subscriptions, income, categories, a budget overage, a suspected duplicate, and at least one empty-state scenario. Do not use a real person, bank statement, CPF, institution account number, merchant transaction, or user financial history. **Done when:** all primary UI flows have stable data and tests can reset it exactly.
- [x] G4.3 — Implement Room as the local source of truth for editable demo entities (budget, alert dismissal, category correction, saved assistant conversation) and DataStore for display/settings preferences. **Done when:** app recreation preserves the intended demo edits, and “Reset demo data” restores only the bundled fictional seed.
- [x] G4.4 — Expose immutable screen `UiState` from screen-level ViewModels, use lifecycle-aware collection in Compose, and model loading/empty/error states rather than relying on nullable data. **Done when:** ViewModels can be unit tested without an Activity/Context and each feature survives rotation without losing the visible state.
- [x] G4.5 — Implement rule-based local assistant answers for only documented demo intents: monthly total, category total, largest expense, recurring charge, available budget, and demo-data disclaimer. Do not call an LLM or present an invented answer as financial advice. **Done when:** unsupported questions receive a clear limitation message and all supported questions return reproducible, explainable calculations.

## Verification record — 2026-09-28

- `:app:lintDebug` passed.
- `:app:testDebugUnitTest` passed: 12 tests, including fake data-source state tests and deterministic finance rules.
- `:app:connectedDebugAndroidTest` passed: 3 tests on Medium_Phone API 37, including month-selection restoration after recreating the Activity.
- `:app:assembleDebug` passed; SDK levels reported compile=36, target=36, min=26.

