# Nabih Wallet

An Android app for personal expense and debt management, built with Kotlin and Jetpack Compose, featuring a calm modern design and an Arabic-first interface.

---

## Overview

Nabih Wallet helps you track your daily income and expenses, keep tabs on money you owe or are owed, and export professional PDF reports for everything, all in one simple, fast app.

---

## Features

### Transaction management
- Add, edit, and delete transactions (income / expense)
- Separate category lists per transaction type (income categories differ from expense categories)
- Multiple payment methods (cash, bank card, e-wallet)
- Attach a receipt (image or PDF) to each transaction
- Text notes and pinning important transactions to the top

### Debt management
- Track "owed to me" (money others still owe you) and "I owe" (money you owe others)
- Record partial payments with automatic remaining-balance updates
- Full payment history linked to each debt
- Optional reminder before the due date

### Export and reports
- Professional PDF export with clean Arabic layout (correct RTL direction, no digit reversal)
- A dedicated report for debts
- CSV export for opening in Excel
- Files are saved directly to the Downloads folder (no need to go through a share sheet), with an optional share button alongside

### Identity and design
- Arabic is the default language (full RTL support)
- Default currency: Egyptian Pound (EGP)
- Warm color palette (cream and burnt orange) with a calm, easy-on-the-eyes design

---

## Tech stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material Design 3) |
| Database | Room |
| Export | android.graphics.pdf.PdfDocument + BidiFormatter for correct Arabic text handling |
| Build | Gradle (Kotlin DSL / Version Catalog) |
| CI/CD | GitHub Actions |

---

## Requirements

- Android Studio (latest stable release)
- JDK 17
- Minimum Android SDK version: defined by minSdk in app/build.gradle

---

## Running locally

```
git clone https://github.com/ynabih1/Nabih-Wallet-.git
cd Nabih-Wallet-
./gradlew assembleDebug
```

The resulting APK will be located at:
```
app/build/outputs/apk/debug/
```

Note: always use ./gradlew (the Gradle Wrapper) instead of the global gradle command, to ensure the exact Gradle version specified by the project is used.

---

## Building via GitHub Actions

The project includes a ready workflow (.github/workflows/build.yml) that automatically builds a debug APK on every push and uploads it as a downloadable artifact under the Actions tab.

---

## Project structure (summary)

```
app/
 src/main/
   java/com/example/
     data/
       database/     Room entities and databases
       export/       ExportUtils.kt (PDF/CSV export)
     ui/              Jetpack Compose screens
   res/
     drawable/        App icon and drawable resources
     mipmap-anydpi-v26/   Adaptive icon
     font/            Custom Arabic fonts (Cairo)
```

---

## Notable technical details

- Arabic text direction in PDF: BidiFormatter is used with explicit TextDirectionHeuristics for each text segment, to avoid the common digit-reversal issue that occurs when drawing mixed Arabic/numeric text directly with Canvas.drawText.
- KSP and Kotlin version matching: the KSP version in libs.versions.toml must exactly match the Kotlin version, in the format kotlin-version-ksp-version.
- File saving: successful writes are verified through MediaStore (bytes copied greater than 0) before any success message is shown to the user.

---

## License

This project is currently private. Add an appropriate license file (MIT, Apache 2.0, etc.) if you intend to open-source it.
