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

Start from an empty ledger. Add an income and an expense, check that the balance and current-month totals update, edit and delete a transaction, then create a category budget. Restart the app to confirm local data persists. Settings can delete local transactions, budgets, goals, alerts, review decisions, pending imports, and assistant messages while keeping built-in categories and display preferences.

## Optional Rico notification review

This is a local notification-review flow, not a bank connection. The owner explicitly selects Rico, then grants Android notification access. Android exposes a broad system permission, but the listener checks the callback package before it reads notification text and accepts only the selected Rico package. It never scans notifications from other apps.

When the Rico notice reliably displays a linked account or card ending, the owner can add exactly its last four digits in **Ajustes**. The app never asks for or stores a full account/card number. A Rico notice that does not show the configured ending in a recognized account/card context is discarded before it reaches the local inbox. Leave this filter empty when the message does not reliably identify a feed.

The original notification title, body, expanded text, authentication codes, Pix keys, email addresses, phone numbers, and full account/card identifiers are never stored. The pending record contains only the selected source, a fixed review label, event type, Brazilian-real value, observed date/time, status, and opaque one-way identifiers for replay handling. A repeated delivery of the same event creates at most one draft; a changed/reposted notice is reviewed as a separate draft. Removing an Android notification from the shade does not remove a draft already saved by ALLU.

Supported synthetic patterns are: a purchase/charge with one unambiguous Brazilian-real amount, incoming money including supported Pix-received wording, card-bill payment, internal transfer, and refund/reversal. Purchases and incoming money stay pending until the owner edits, categorizes, and confirms them. Bill payments and internal transfers are neutral and never change totals. Refunds/reversals stay review-only. Declined/pending authorizations, codes, promotions, foreign-currency notices, missing/truncated text, multiple amounts, and ambiguous installments are ignored. An installment is accepted only when its single amount is explicitly identified as the current installment; ALLU never multiplies it or creates future rows.

Before an expense/income reaches the ledger, the owner must confirm it and choose a compatible category. A possible duplicate is shown for review and requires a separate acknowledgement; it is checked again when the confirmation is saved, so a manual transaction added after the draft cannot be silently duplicated. There is no Rico API, Open Finance link, credential collection, account-history import, SMS access, screen scraping, or network call. Disconnecting stops new capture and lets the owner keep or discard pending drafts; kept drafts remain reviewable. Revoking Android notification access is done in Android settings.

For the platform boundary, see Android's [NotificationListenerService reference](https://developer.android.com/reference/android/service/notification/NotificationListenerService) and [security guidance](https://developer.android.com/privacy-and-security/security-tips). The Rico/XP path is intentionally limited to local notices because XP documents its developer APIs and Open Finance material for partners and participants rather than providing a personal in-app connection for this prototype: [XP developer portal](https://dev.xpinc.com/) and [XP Open Finance](https://developer.xpinc.com/open-finance).

### Device validation

Use only a real transaction the owner already planned or has explicitly approved; do not spend money to create a test. On a connected device/emulator, first confirm the installed source package with `adb shell pm path br.com.rico.mobile`, then test: permission denied/allowed/revoked; another app notification; a purchase that stays out of totals until confirmation; a received-money draft; neutral bill/transfer; ignored declined/OTP/promotion notices; duplicate/replay; Android-shade dismissal; disconnect while keeping and discarding drafts; reset; and large-font/TalkBack readability. Record the device/API level and hide personal values in any evidence.

The local assistant calculates monthly totals, category totals, largest expense, recurring transactions, remaining category budget, and explains its local data source. It gives setup guidance when the requested period has no records. No model, bank connection, or external API is called.

The [product brief](PRODUCT.md), [design brief](DESIGN.md), and [execution plan](GOALS.md) describe decisions and remaining acceptance work. Final visual sign-off requires the owner to review the installed Android screens. Direct bank integrations, signing, and publication would require a separate security/legal/product plan.

## Future release gate — not part of this build

- Confirm the final name, application ID, supported devices, and owner-approved visual baseline.
- Assign a signing-key custodian and store the release keystore outside the repository.
- Complete accessibility, privacy/data-safety, policy, and device checks for a production build.
- Authorize and implement release signing and distribution only in a separate plan.
