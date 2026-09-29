# ALLU Finanças — visual and interaction brief

This Android app has its own product identity. Pierre informs the choice of finance workflows, not its copy, screenshots, illustrations, or trade dress. The owner chose ALLU ERP's existing visual identity on 23 September 2026. “ALLU Finanças” is a working label for a fictional, offline prototype.

## Provenance

| Reference | Source | Android adaptation |
| --- | --- | --- |
| White ALLU symbol | `D:\ProjetosPessoais\ERP\frontend\public\images\logo\allu-mark-white.png` (454 × 601) | Display on a navy brand surface; keep proportions and clear space. |
| ALLU wordmarks | `D:\ProjetosPessoais\ERP\frontend\public\images\logo\allu-logo-dark.png`, `allu-logo.png` (1571 × 698) | Reference only; prefer native text alongside the symbol at phone scale. |
| Brand palette | `D:\ProjetosPessoais\ERP\frontend\src\app\globals.css` | Brand 500 `#00006B`, 600 `#00005E`, 700 `#00004D`; tint 50 `#E9E9F5`, 100 `#D3D3EB`, 200 `#A8A8D6`. |
| Neutral palette | Same `globals.css` | Ink `#101828`, canvas `#F9FAFB`, white `#FFFFFF`. |
| Navigation and type | `D:\ProjetosPessoais\ERP\frontend\src\layout\AppSidebar.tsx` and `globals.css` | ERP sidebar navy `#0F172A`; Outfit is the ERP font, but use Android system sans until a redistributable font asset is supplied. |

Do not import ERP customer data, business imagery, proprietary page layouts, or a web sidebar. Only the owner-authorized mark and visual tokens are used. Do not imply Pierre endorsement or a live financial connection.

## Direction

An ALLU operations board adapted for one hand: crisp light canvas, deep-navy header, deliberate high-contrast numbers, rounded but compact cards, and small indigo accents. Money is the dominant visual signal; labels and provenance sit close to each value. Warnings use neutral, explainable language rather than alarmist fraud claims.

The first phone viewport must show the demo disclosure, period, total balance, income/expense comparison, and at least one useful next action without overwhelming the user. Bottom navigation is native-sized and always identifies the current destination. The conversational screen is a local question-and-answer tool, not a fake messaging-app clone.

## Tokens and behavior

- Spacing follows a 4dp grid; page gutters 20dp; card gaps 12dp; minimum touch target 48dp.
- Surfaces: white cards on `#F9FAFB`; navy hero on `#00004D`; white-on-navy brand elements; primary actions `#00006B`.
- Typography: system sans, tabular figures for money where available; primary balance large, transaction amounts medium, provenance compact but legible.
- Radii: 14dp cards, 10dp controls, 8dp chips; elevation subtle and secondary to spacing/borders.
- Semantic color must be paired with a text label and icon. No hue alone communicates overspending, duplicate suspicion, or dismissal.
- Brief motion may confirm press/navigation; respect reduce-motion preference. Never delay or animate away a money value.
- Edge-to-edge/insets, system back, keyboard, 360dp/412dp widths, large font, and TalkBack order are acceptance checks.

## Acceptance frame

Evaluate rendered Android screens for welcome, dashboard, transaction list/detail, budget edit, alert inbox, assistant, and settings. Also inspect empty/error, long BRL values, large font, and dark mode. Automated checks prove calculations and navigation, but final visual approval remains with the owner after an actual installed build.
