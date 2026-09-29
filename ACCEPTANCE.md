# ALLU Finanças — acceptance flows

This checklist applies only to the offline, fictional-data debug APK. Each amount and alert must be understood as an example, never a live bank fact.

| Flow | User action | Expected visible result | Empty/error and accessibility expectation |
| --- | --- | --- | --- |
| First launch | Open app, read disclosure, tap “Explorar demonstração” | Dashboard opens without account, permission, or credential prompt; privacy explanation remains available | On local-data failure, show a readable error, not a permanent spinner; ALLU symbol has a spoken label on welcome |
| Dashboard | Read balance, select another period, open a section | Income, expenses, recent transactions, and period label change together; balance remains clearly identified as example total | Empty period says no movements; currency is read as text and never conveyed only by color |
| Extrato | Search merchant, filter category/month, open a row | Only matching records appear; detail states amount, date, source, installment and category; correction updates local data | No matches explains how to clear filters; row is a reachable touch target |
| Insight/alert | Open “Avisos”, read a rule, archive and re-open | Alert explanation, generated date, fictional source, and reversible state are visible | Empty inbox says no active/archived alerts; severity has text, not color alone |
| Assistant | Ask a supported question and an unsupported one | Supported answer has deterministic local arithmetic and transaction IDs; unsupported answer states limitation | Blank prompt cannot send; conversation order and roles are spoken |
| Budget | Open “Planos”, alter one limit and save | Progress and overage state update; invalid/negative/overlarge amounts cannot save | No budget shows an empty state; warning is a text label plus progress |
| Reset | Open “Ajustes”, choose restore, cancel once, then confirm | Cancel leaves local edits intact; confirm restores only bundled fictional budgets, categories, alerts and chat | Confirmation names exact impact and has cancel/confirm controls |
| Preferences | Toggle dark mode, centavos, reduce motion; recreate app | Chosen settings persist locally | Theme must keep readable contrast at large font; no animation is needed to understand a number |

Automated tests and APK build do not replace on-device UI/visual review. Record the emulator API and owner visual sign-off separately in GOALS.md.
