# ALLU Finanças — acceptance flows

This checklist applies to the offline local-ledger debug APK. New installs and the v2-to-v3 migration must start without financial rows.

| Flow | User action | Expected visible result | Empty/error and accessibility expectation |
| --- | --- | --- | --- |
| First launch | Open app, read disclosure, tap “Começar a cadastrar” | Dashboard opens without account, permission, or credential prompt; only built-in categories exist | On local-data failure, show a readable error, not a permanent spinner; ALLU symbol has a spoken label on welcome |
| Dashboard | Start empty, add income and expense in Extrato, return to Início | Balance is the net sum of all saved transactions; current-month income, expenses, recent rows and period update together | Empty state explains how to add a first transaction; currency is read as text and never conveyed only by color |
| Extrato | Add a transaction, search/filter, open details, edit then delete | Type, amount, date, category, merchant and description persist locally; edits update the same row | Invalid amount/date/category and empty merchant cannot save; no matches explains how to clear filters; row is a reachable touch target |
| Insight/alert | Add enough transactions to trigger a local rule, archive and re-open | Rule, source transaction IDs, local-data attribution, and reversible state are visible | Empty inbox explains that records are needed; severity has text, not color alone |
| Assistant | Ask about a month with data, then an empty month and an unsupported question | Supported answer uses deterministic local arithmetic and cites transaction IDs; empty data gives setup guidance; unsupported answer states limitation | Blank prompt cannot send; conversation order and roles are spoken |
| Budget | Open “Planos”, create a category limit, edit it and save | Current-month category spending and overage state update | No budget prompts category selection; invalid/negative/overlarge amounts cannot save; warning is a text label plus progress |
| Delete local data | Open “Ajustes”, cancel once, then confirm deletion | Cancel leaves local rows intact; confirm clears financial records and chat while retaining default categories and display preferences | Confirmation names exact impact and has cancel/confirm controls |
| Migration | Upgrade a v2 database containing demo rows and customized display preferences | Old accounts, transactions, budgets, goals, alerts, reconciliation decisions and chat are cleared once; default categories are available | Theme, motion and currency preferences remain unchanged |
| Preferences | Toggle dark mode, centavos, reduce motion; recreate app | Chosen settings persist locally | Theme must keep readable contrast at large font; no animation is needed to understand a number |

Automated tests and APK build do not replace on-device UI/visual review. Record the emulator API and owner visual sign-off separately in GOALS.md.
