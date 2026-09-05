# Implementation Plan - Fix Crash, CSV Export, and App Widget

This plan addresses the startup crash, implements CSV export functionality, adds an app widget, and ensures currency consistency.

## User Review Required

> [!IMPORTANT]
> The app widget will require the user to manually add it to their home screen after the update.
> CSV Export will save the file to the app's cache directory and then use a share intent for the user to save or send it.

## Proposed Changes

### [Component] Data & Database
Fixing the startup crash by moving data seeding to a safer place and ensuring non-blocking execution.

#### [MODIFY] [CategoryDao.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/data/dao/CategoryDao.kt)
- Add a suspend function `getAllCategoriesList(): List<Category>` to avoid using Flow in seeding logic.

#### [MODIFY] [DataSeeder.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/data/DataSeeder.kt)
- Update `seedData` to use the new `getAllCategoriesList()` method.

#### [MODIFY] [AppDatabase.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/data/AppDatabase.kt)
- Move `DataSeeder.seedData` from `onOpen` to `onCreate` to ensure it only runs once.
- Ensure `INSTANCE` is used safely.

---

### [Component] Analytics & CSV Export
Adding functionality to export transactions to a CSV file.

#### [MODIFY] [AnalyticsViewModel.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/viewmodel/AnalyticsViewModel.kt)
- Implement `exportTransactionsToCsv(context: Context)` which generates a CSV and launches a share intent.

#### [MODIFY] [AnalyticsScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/AnalyticsScreens.kt)
- Add an "Export" icon button in the `TopAppBar`.

---

### [Component] App Widget
Implementing an Android App Widget using Glance to show financial metrics.

#### [MODIFY] [libs.versions.toml](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/gradle/libs.versions.toml)
- Add Glance dependencies.

#### [MODIFY] [build.gradle.kts](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/build.gradle.kts)
- Add Glance implementation.

#### [NEW] [ExpenditureWidget.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/widget/ExpenditureWidget.kt)
- Implement the Glance widget showing "Total Balance" or "Net Flow".

#### [NEW] [ExpenditureWidgetReceiver.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/widget/ExpenditureWidgetReceiver.kt)
- Implement the GlanceAppWidgetReceiver.

#### [MODIFY] [AndroidManifest.xml](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/xml/AndroidManifest.xml)
- Register the widget receiver and provider info.

---

### [Component] UI & Currency Consistency
Ensuring ₹ is used everywhere.

#### [MODIFY] [AccountScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/AccountScreens.kt)
- Verify and update currency symbols.

#### [MODIFY] [TransactionScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/TransactionScreens.kt)
- Verify and update currency symbols.

#### [MODIFY] [BudgetScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/BudgetScreens.kt)
- Verify and update currency symbols.

## Verification Plan

### Automated Tests
- Run `./gradlew build` to ensure the project compiles.

### Manual Verification
- Launch the app and verify it doesn't crash.
- Navigate to Analytics and click "Export to CSV".
- Add the app widget to the home screen and verify it shows the correct balance.
- Check all screens for the ₹ symbol.
