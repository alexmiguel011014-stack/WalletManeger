# ALLU Finanças

An offline personal-finance ledger using the ALLU visual identity authorized by the owner. New installs contain no financial records; the app keeps only built-in categories so a user can begin adding transactions and budgets. Data is stored in a local Room database on the device.

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

Start from an empty ledger. Add an income and an expense, check that the balance and current-month totals update, edit and delete a transaction, then create a category budget. Restart the app to confirm local data persists. Settings can delete local transactions, budgets, goals, alerts, review decisions, and assistant messages while keeping built-in categories and display preferences.

The local assistant calculates monthly totals, category totals, largest expense, recurring transactions, remaining category budget, and explains its local data source. It gives setup guidance when the requested period has no records. No model, bank connection, or external API is called.

The [product brief](PRODUCT.md), [design brief](DESIGN.md), and [execution plan](GOALS.md) describe decisions and remaining acceptance work. Final visual sign-off requires the owner to review the installed Android screens. Real integrations, signing, and publication would require a separate security/legal/product plan.

## Future release gate — not part of this build

- Confirm the final name, application ID, supported devices, and owner-approved visual baseline.
- Assign a signing-key custodian and store the release keystore outside the repository.
- Complete accessibility, privacy/data-safety, policy, and device checks for a production build.
- Authorize and implement release signing and distribution only in a separate plan.
