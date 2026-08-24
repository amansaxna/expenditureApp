# Expenditure App UI Improvement Plan

This plan outlines the UI/UX improvements to align the current Expenditure App with a high-end finance app aesthetic (Copilot-style).

## User Review Required

- **Design Aesthetic:** Deep Navy background with vibrant accents (Material 3).
- **Navigation:** Standard bottom nav will remain, but the Dashboard and Analytics screens will be revamped.

## Open Questions

- None at this time.

## Proposed Changes

### Dashboard UI
- [MODIFY] [DashboardScreen.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/DashboardScreen.kt)
    - Add top balance card with line chart.
    - Implement "To Review" section.
    - Add horizontal scrollable circular progress indicator for Budgets.

### Transaction Detail/Review
- [MODIFY] [TransactionScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/TransactionScreens.kt)
    - Add a "Transaction Review" flow with quick-action buttons ("Exclude", "Split", "Recurring", "Review").

### Analytics & Lists
- [MODIFY] [AnalyticsScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/AnalyticsScreens.kt)
    - Enhance visuals with Dark Navy backgrounds and high-contrast colors.
- [MODIFY] [TransactionScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/TransactionScreens.kt)
    - Update list UI with category icons.

## Verification Plan

### Manual Verification
- Verify the Dashboard reflects the new design (chart, review, circular budgets).
- Verify the Transaction Review screen for the "Lyft" transaction example.
- Check Analytics screens for contrast and color palette updates.
