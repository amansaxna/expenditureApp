# 🏗️ System Architecture & Engineering Blueprint

## 1. Architectural Philosophy & Design Principles

**MyExpenditureApp** is architected around four non-negotiable engineering tenets:

1. **Local-First & Offline Resilience**: Zero dependence on remote servers. Full ACID database transactions via SQLite/Room.
2. **Double-Entry & Numerical Precision**: All currency math uses `java.math.BigDecimal` with explicit scale and `RoundingMode.HALF_UP` to prevent IEEE 754 floating-point truncation.
3. **Unidirectional Data Flow (UDF)**: Presentation layer consumes immutable `StateFlow` streams exposed by ViewModels, emitting user intents back to ViewModels.
4. **Ergonomic Material 3 Experience**: Native Compose styling using calm fintech palette tokens, tabular monospaced numbers, and responsive layouts.

---

## 2. High-Level Layered Architecture

The application implements Clean Architecture with strict separation of concerns across three core tiers:

```mermaid
graph TB
    subgraph Presentation Layer ["Presentation Layer (Jetpack Compose + Material 3)"]
        UI_Screens[Screens & Bottom Sheets]
        UI_VM[ViewModels - StateFlow & Channels]
        Nav[AndroidX Navigation 3 EntryProvider]
    end

    subgraph Domain Layer ["Domain Layer (Pure Kotlin Business Logic)"]
        IE[InsightsEngine - Commentary & Pacing]
        RR[RecurringRadar - Subscription Detection]
        DM[Domain Models: BudgetWithProgress, Insights]
        RI[Repository Interfaces]
    end

    subgraph Data Layer ["Data Layer (Persistence & OS Ingestion)"]
        RoomDB[(Room SQLite Database)]
        DAOs[AccountDao, TransactionDao, CategoryDao, etc.]
        Repos[Repository Implementations]
        SMS[SmsReceiver - BroadcastReceiver]
        NL[TransactionNotificationListener - NotificationListenerService]
        WM[WorkManager - Daily & Budget Workers]
        BM[BackupManager - SAF JSON Serializer]
    end

    UI_Screens --> UI_VM
    UI_VM --> Nav
    UI_VM --> RI
    RI -.-> Repos
    Repos --> DAOs
    DAOs --> RoomDB
    SMS --> Repos
    NL --> Repos
    IE --> DM
    RR --> DM
    Repos --> RoomDB
    WM --> Repos
    BM --> RoomDB
```

---

## 3. Data Flow & Transaction Lifecycle

### A. Autonomous Ingestion Flow (SMS / UPI Notification)

```mermaid
sequenceDiagram
    participant OS as Android System (SMS / Notification)
    participant Ingest as SmsReceiver / NotificationListener
    participant Parser as Heuristic Regex Engine
    participant Repo as TransactionRepository
    participant Rule as AutoCategoryRuleRepository
    participant DB as Room AppDatabase
    participant Notif as NotificationHelper

    OS->>Ingest: Incoming SMS / Notification
    Ingest->>Parser: Extract Body & Sender
    Parser->>Parser: Match Debit/Credit, Merchant, Amount
    alt Match Succeeded
        Ingest->>Rule: Lookup Rule by Merchant
        Rule-->>Ingest: Category ID (or null)
        Ingest->>Repo: createTransaction(isReviewed = (Category != null))
        Repo->>DB: Atomic Insert + Update Account Balance
        opt If Unreviewed
            Ingest->>Notif: showReviewNotification()
        end
    else Match Failed
        Ingest->>Ingest: Discard or ignore non-financial notification
    end
```

### B. Manual Entry & Arithmetic Evaluation Flow

```mermaid
sequenceDiagram
    participant User as User Interaction
    participant View as TransactionEditScreen
    participant Calc as CalculatorTextField
    participant VM as TransactionViewModel
    participant Repo as TransactionRepository
    participant DB as Room AppDatabase

    User->>Calc: Type "450 + 80 * 2"
    Calc->>Calc: evaluateExpression() -> "610"
    User->>View: Select Category & Account
    User->>View: Tap "Save Transaction"
    View->>VM: saveTransaction(amount=610, merchant, categoryId, accountId)
    VM->>Repo: insertTransaction(Transaction)
    Repo->>DB: Atomic Insert Transaction & Update Account Balance
    DB-->>VM: Flow emits updated Transaction List
    VM-->>View: StateFlow updates, screen navigates back
```

---

## 4. Module & Package Decomposition

### `com.example.myexpenditureapp`
- **`data/`**:
  - **`dao/`**: Room DAOs (`AccountDao`, `TransactionDao`, `CategoryDao`, `BudgetDao`, `AutoCategoryRuleDao`, `GoalDao`, `SubscriptionDao`).
  - **`entity/`**: Room SQLite table entities with foreign keys and indices.
  - **`repository/`**: Concrete implementations of domain repository interfaces.
  - **`backup/`**: `BackupManager` for lossless JSON schema serialization and SAF file I/O.
  - **`AppDatabase.kt`**: SQLite database definition and migration registry (`MIGRATION_1_2` through `MIGRATION_7_8`).
  - **`Graph.kt`**: Lightweight Service Locator providing singleton repository instances.
- **`domain/`**:
  - **`insights/`**: `InsightsEngine.kt` calculating burn rates, month-to-date net flows, pacing flags, and human commentary.
  - **`model/`**: Pure domain data models (`BudgetWithProgress`, `FinancialDigest`, `PacingStatus`).
  - **`repository/`**: Clean repository interfaces separating domain logic from SQLite implementation.
- **`notifications/`**:
  - **`NotificationHelper.kt`**: Notification channel registration, budget alerts, and WorkManager task scheduling.
  - **`TransactionNotificationListener.kt`**: Notification listener service capturing bank notification text.
- **`sms/`**:
  - **`SmsReceiver.kt`**: BroadcastReceiver listening for `android.provider.Telephony.SMS_RECEIVED`.
- **`ui/`**:
  - **`component/`**: Shared Compose UI components (`StandardTransactionRow`, `CategorySelectionBottomSheet`, `CalculatorTextField`, `DashboardPacedHeroCard`, `FinancialDigestCard`).
  - **`navigation/`**: Type-safe `Route` sealed hierarchy.
  - **`screen/`**: Composable screens (`AccountScreens`, `TransactionScreens`, `AnalyticsScreens`, `CategoryScreens`, `BudgetScreens`, `SettingsScreen`, `SubscriptionScreens`, `GoalScreens`).
  - **`theme/`**: Color tokens, Typography, Shapes, and dynamic Day/Night theme switcher.
  - **`viewmodel/`**: MVVM ViewModels exposing immutable `StateFlow` state.
- **`widget/`**:
  - **`ExpenditureWidget.kt`**: Jetpack Glance home screen widget.

---

## 5. State Management & Navigation Strategy

### Navigation 3 (AndroidX Navigation3)
The app adopts AndroidX Navigation 3 using `rememberNavBackStack(Route.AccountList)` and `NavDisplay`:
- **Adaptive Scene Strategy**: Uses `ListDetailSceneStrategy` for foldables and large tablets.
- **Top-Level Screen Guard**: Bottom navigation bar is rendered only on the four primary destinations (`AccountList`, `TransactionList`, `Analytics`, `Settings`) and automatically hidden during detail/form flows.

### ViewModels & StateFlow Lifecycle
- ViewModels expose read-only `StateFlow<UiState>` backed by private `MutableStateFlow`.
- Composables consume state via `collectAsStateWithLifecycle()` to automatically halt collection when in the background.
