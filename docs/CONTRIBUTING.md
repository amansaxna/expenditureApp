# 🤝 Contributing to MyExpenditureApp

Thank you for contributing to **MyExpenditureApp**! To maintain architectural purity, double-entry financial correctness, and Material 3 design excellence, please follow these guidelines.

---

## 💻 Development Environment Setup

1. **Prerequisites**:
   - Android Studio Ladybug (2024.2.1+) or newer.
   - JDK 17 or 21 configured in Android Studio Gradle settings.
   - Android SDK Platform 35 and Build Tools installed.
2. **Clone & Verify**:
   ```bash
   git clone https://github.com/your-username/MyExpenditureApp.git
   cd MyExpenditureApp
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

---

## 📐 Code Style & Architecture Guidelines

### 1. Kotlin & Coroutines
- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Enforce explicit return types for public API methods and ViewModel state flows.
- Use `viewModelScope` for structured coroutine concurrency; never launch unconfined coroutines with `GlobalScope`.

### 2. Jetpack Compose & Material 3
- **Design System Consistency**:
  - Use tokens from `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
  - Avoid ad-hoc magic numbers for dimensions; use standard 4dp grid multipliers (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `20.dp`, `24.dp`).
  - Use 48dp minimum touch targets for all interactive icons and buttons.
- **Compose Performance**:
  - Always provide `key` in `items(...)` blocks for `LazyColumn` and `LazyRow`.
  - Prefer derived state or `remember(key) { ... }` for expensive calculations.
  - Collect state flows using `collectAsStateWithLifecycle()`.

### 3. Financial & Accounting Correctness
- **Mandatory `BigDecimal`**: Always use `java.math.BigDecimal` for currency values, account balances, and budget calculations.
- **Explicit Rounding**: Specify explicit rounding modes (`RoundingMode.HALF_UP`) and strip trailing zeros for clean presentation.
- **Double-Entry Transactions**: Balance adjustments must be performed atomically inside Room database transactions.

---

## 🧪 Testing Requirements

Every Pull Request must include automated unit tests:

1. **Domain Logic**: If modifying `InsightsEngine`, `evaluateExpression`, or pacing logic, add tests to `app/src/test/java/com/example/myexpenditureapp/insights/`.
2. **Database Migrations**: If modifying schema, verify migration in `app/src/test/java/com/example/myexpenditureapp/`.
3. **Backup & Serialization**: Ensure `BackupSerializationTest.kt` passes with 100% success rate.

Run all tests prior to committing:
```bash
./gradlew testDebugUnitTest
```

---

## 🔀 Pull Request Process

1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Commit your changes using Conventional Commits:
   - `feat: add parent category rollup toggle in analytics`
   - `fix: resolve duplicate currency prefix in calculator field`
   - `refactor: optimize lazy column key generation`
   - `test: add unit test for budget alert worker`
3. Push to your fork and submit a Pull Request targeting the `develop` branch.
4. Ensure all automated CI checks and unit test suites pass.
