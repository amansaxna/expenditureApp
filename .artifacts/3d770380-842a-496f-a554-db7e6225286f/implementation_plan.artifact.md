# Enhanced Category System Implementation Plan

This plan outlines the steps to enhance the category system with a granular hierarchy, improved visual consistency (emojis), and better UI handling of a larger category set.

## Proposed Changes

### 1. Data Layer & Seeding

#### [MODIFY] [DataSeeder.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/data/DataSeeder.kt)
- Update the `seedData` function to implement the requested multi-level hierarchy:
    - **Housing**: Rent 🏠, Utilities ⚡, Maintenance 🛠️, Home Insurance 🛡️
    - **Food & Dining**: Groceries 🛒, Restaurants 🍔, Cafes ☕, Delivery 🍕
    - **Transportation**: Fuel ⛽, Public Transit 🚌, Parking 🅿️, Car Service 🔧
    - **Personal Care**: Grooming 💇‍♂️, Pharmacy 💊, Gym 🏋️‍♂️, Wellness 🧴
    - **Entertainment**: Streaming 📺, Movies 🍿, Gaming 🎮, Concerts 🎸
    - **Shopping**: Clothes 👕, Electronics 💻, Books 📚, Gifts 🎁
    - **Finance**: Salary 💰, Dividends 📈, Tax Return 🏛️, Interest 🏦
    - **Education**: Tuition 🎓, Courses 📖, Stationaries ✏️
- Ensure all default categories use actual emoji characters.

### 2. UI Components & Screens

#### [MODIFY] [CategoryScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/CategoryScreens.kt)
- **CategoryListScreen**:
    - Update `CategoryHierarchyItem` to use more distinct indentation for sub-categories.
    - Add a subtle vertical line or bullet to visually connect sub-categories to their parent.
- **CategoryEditScreen**:
    - Expand the `emojis` list to include a wider variety of relevant icons.

#### [MODIFY] [TransactionScreens.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/ui/screen/TransactionScreens.kt)
- **QuickEditBottomSheet**:
    - Update `FilterChip` to display the category emoji alongside the name.
    - Improve the category selection logic (maybe allow scrolling through more than 8).
- **FilterDialog**:
    - Update `FilterChip` to display the category emoji.
- **TransactionEditScreen**:
    - Improve the category dropdown (`ExposedDropdownMenu`) to group categories by their parent.
    - Show emojis in the dropdown items.
- **TransactionItem**:
    - (Verification) Ensure it already prominently displays the emoji.

### 3. Verification Plan

#### Automated Tests
- Run existing unit tests to ensure no regressions in category handling.
- Run `gradlew :app:assembleDebug` to verify compilation.

#### Manual Verification
- Fresh install (or clear app data) to trigger `DataSeeder`.
- Navigate to the Categories screen and verify the hierarchy and indentation.
- Open Transaction Edit and verify the grouped category dropdown.
- Check Quick Edit and Filter dialogs for emoji display.
- Verify that sub-categories are visually distinct from parent categories.
