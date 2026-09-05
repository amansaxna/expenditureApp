# Implementation Plan: Harden SmsParser and Update App Icons

This plan outlines the steps to improve the SMS parsing logic for Indian bank formats and create a modern, minimalist adaptive app icon.

## Proposed Changes

### SMS Parsing logic [SmsParser]

#### [MODIFY] [SmsParser.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/main/java/com/example/myexpenditureapp/sms/SmsParser.kt)
- Update regex patterns to handle a wider variety of Indian bank SMS formats (HDFC, ICICI, SBI, AXIS, etc.).
- Improve amount extraction to handle variations like "INR", "Rs.", "Rs", "₹", "re." with flexible spacing and decimal points.
- Implement more robust merchant extraction by looking for patterns like "at [Merchant]", "to [Merchant]", "on [Merchant]", "info:[Merchant]", "purchased at [Merchant]".
- Better detection of "Credit" vs "Debit" to automatically categorize as Income or Expense.
- Add support for common UPI transaction formats (e.g., "VPA [Name] ...").

#### [MODIFY] [SmsParserTest.kt](file:///Users/amansaxena/AndroidStudioProjects/MyExpenditureApp/app/src/test/java/com/example/myexpenditureapp/sms/SmsParserTest.kt)
- Add comprehensive test cases for various bank SMS formats and UPI transactions to ensure the new regex patterns work correctly.

### App Icons

- Create a minimalist, high-quality adaptive launcher icon.
- Background: Deep Navy (#0B1019).
- Foreground: A simple, clean, and modern white Rupee symbol (₹) with a subtle shadow.
- Use `app_icon_agent` to generate and apply the icons.

## Verification Plan

### Automated Tests
- Run `SmsParserTest` to verify that all new and old test cases pass.
- Run `./gradlew testDebugUnitTest` to ensure overall project health.

### Manual Verification
- Inspect the generated app icon in the project resources.
- (Optional) Build the app to see the icon on a device/emulator.
