# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin, Jetpack Compose, Material 3, a single Android app module, and local demo data, as specified in the approved GOALS.md. The user delegated implementation choices by invoking `$execgoals`.

## Users

Primary user: a Portuguese-speaking person exploring a simpler view of personal spending and budgets. This audience is inferred from the request for a Pierre-like finance app and remains a product assumption until validated with users.

## Product Purpose

Deliver an installable Android prototype that combines a concise money overview, transaction exploration, budgeting, explanatory alerts, and a conversational entry point. The first deliverable uses fictional local data and is evaluated as a demo APK.

## Positioning

The product shows where each finance summary came from and how each demo calculation was made, while using the user's existing ALLU identity across the app.

## Operating Context

The owner evaluates a locally installed APK on an Android emulator or phone. A user can explore the demo without registration or bank access.

## Capabilities and Constraints

- Offline fictional dataset, local calculations, local assistant responses, and resettable demo state.
- No live bank connection, payments, investment execution, or WhatsApp interaction in this release.
- Brazilian Portuguese and BRL display.
- Debug APK is the current deliverable. A final release name, package identifier, signing method, and publication route are open decisions.

## Brand Commitments

On 23 September 2026 the owner requested an original app identity using ALLU ERP's visual colors/style and ALLU symbol. The temporary app label is “ALLU Finanças”. The source assets and tokens are in `D:\ProjetosPessoais\ERP\frontend\public\images\logo` and `D:\ProjetosPessoais\ERP\frontend\src\app\globals.css`. Pierre remains a feature/workflow reference only.

## Evidence on Hand

`REPERTOIRE.md` and `Relatório — Pierre.md` summarize public Pierre capabilities. `GOALS.md` is the approved execution plan. ALLU ERP source files provide the owner's visual reference. No real customer data or owner-approved Android screenshot baseline is present.

## Product Principles

1. Make money data legible quickly.
2. Show sources and arithmetic behind summaries.
3. Keep edits reversible and demo state resettable.
4. Respect native Android navigation and accessibility.
