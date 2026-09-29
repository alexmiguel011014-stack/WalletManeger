# ALLU Finanças

An original Android finance-companion prototype inspired by public product workflows, with the ALLU ERP visual identity authorized by the owner. It is an **offline demonstration using fictional data**. Balances, transactions, alerts, and assistant answers are not from a bank. There is no login, Open Finance connection, payment, WhatsApp integration, investment advice, or personal-data upload.

The current deliverable is a debug APK for local evaluation, not a production-signed financial app. “ALLU Finanças” is a working name. The final release name, application ID, signing arrangement, and distribution decision remain open; the code uses a temporary development ID.

## Build

Prerequisites: JDK 21 and Android SDK Platform 36 with build tools, with `JAVA_HOME` and `ANDROID_HOME` set. On Windows, from this directory:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The APK is produced at `app\build\outputs\apk\debug\app-debug.apk`. Install on an emulator or Android device with `adb install -r app\build\outputs\apk\debug\app-debug.apk`. The instrumented suite requires a booted emulator or device and was exercised on the Android 17/API 37 emulator. Only debug is intended for use in this phase. Do not create a release keystore or publish the APK as a financial service.

## Evaluation

Open the app and choose to explore the demo. Verify that the dashboard, transactions, budgets, insights, assistant, and settings consistently say the values are fictional. Edit a budget and a transaction category, dismiss and reopen an alert, ask a supported and an unsupported question, then reset the demo data. Those edits should remain local and reset only the bundled fictional seed.

The local assistant supports six demo intents: total spent in the month, total by category, largest expense, recurring charges, remaining category budget, and an explanation of the fictional data source. Other questions receive a limitation message; no model or external API is called.

The [product brief](PRODUCT.md), [design brief](DESIGN.md), and [execution plan](GOALS.md) describe decisions and remaining acceptance work. Final visual sign-off requires the owner to review the installed Android screens. Real integrations, signing, and publication would require a separate security/legal/product plan.

## Future release gate — not part of this build

- Confirm the final name, application ID, supported devices, and owner-approved visual baseline.
- Assign a signing-key custodian and store the release keystore outside the repository.
- Complete accessibility, privacy/data-safety, policy, and device checks for a production build.
- Authorize and implement release signing and distribution only in a separate plan.
