# Reduce Excessive Top Padding and Whitespace

The user reports excessive whitespace above the "Categories" title. This is likely due to double-padding of the status bar in a nested `Scaffold` setup with `enableEdgeToEdge()`.

## Proposed Changes

### [Component Name] UI Structure Fix

#### [MODIFY] [MainActivity.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/MainActivity.kt)
- Update the `Scaffold` in `MainScreen` to only apply bottom padding to the `NavDisplay`. This allows inner screens' `Scaffold` and `TopAppBar` to handle the status bar insets themselves, avoiding double-padding.

#### [MODIFY] [CategoryScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/CategoryScreens.kt)
- Reduce the `contentPadding` top value in the `LazyColumn` for `CategoryListScreen` from `12.dp` to `4.dp` to move the list items closer to the top bar.

#### [MODIFY] [AccountScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/AccountScreens.kt)
- Reduce `contentPadding` top value in `LazyColumn` for `AccountListScreen`.

#### [MODIFY] [BudgetScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/BudgetScreens.kt)
- Reduce `contentPadding` top value in `LazyColumn` for `BudgetListScreen`.

#### [MODIFY] [AnalyticsScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/AnalyticsScreens.kt)
- Reduce `contentPadding` top value in `LazyColumn` for `AnalyticsScreen`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the app still builds.

### Manual Verification
- The user will verify the UI on the Categories screen. The title should be closer to the status bar, and the list should start higher.
