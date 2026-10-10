# Currency Formatting & Comma Rule

## 1. Indian Numbering System & Comma Formatting
All monetary numbers and amounts displayed in the app MUST be formatted with proper comma separation using the `formatIndian()` utility (e.g. `2000` -> `2,000`, `15000` -> `15,000`, `100000` -> `1,00,000`).

## 2. No Duplicate Currency Symbols
Do NOT manually prepend `₹` when using `formatIndian()` with `includeSymbol = true` (which is the default).
- ❌ **INCORRECT**: `"₹${amount.formatIndian()}"` -> Produces double symbol: `₹₹15,000`
- ✅ **CORRECT**: `amount.formatIndian(includeSymbol = true)` -> Produces single symbol: `₹15,000`
- ✅ **CORRECT**: `"₹${amount.formatIndian(includeSymbol = false)}"` -> Produces single symbol: `₹15,000`
