# MAKI BRANDS — Professional Offline Android Gift Shop Management Application

Every Sale. Every Customer. Every Celebration. Under Control.

**MAKI BRANDS** is an offline, native Android application engineered for gift shops and retail businesses. It provides full inventory tracking, individual product sales execution, decimal-safe financial calculations, a genuine 13-month Ethiopian (Ge'ez) Calendar engine, customer celebration relationship management (CRM) with 7-day advance reminders, and full JSON database backup & restore.

---

## 🌟 Key Capabilities & Features

### 1. Dashboard & Real-Time Business Analytics
- **Live Financial Summary**: Today's and lifetime Revenue, Gross Profit, and Sales count.
- **Inventory Status**: Total in-stock items, total inventory cost valuation (ETB), and low-stock warning alerts.
- **Ethiopian Date Filters**: Today, Yesterday, This Week, This Month, This Year, All Time.
- **Upcoming Celebrations**: Displays customers with birthdays or anniversaries arriving within the next 7 to 14 days, with instant Call and WhatsApp action buttons.

### 2. Product & Inventory Management
- **Complete Inventory Records**: Product name, category, purchase price, selling price, current stock, and low-stock threshold.
- **Financial Protection**: Potential loss warning displayed automatically when the selling price is set below the purchase price.
- **Inventory Adjustments**: Fast restock (stock additions) and adjustment workflows (damaged/lost items) with an audit trail recorded permanently in the database.
- **Stock Indicators**: Dynamic visual badges for *In Stock*, *Low Stock*, and *Out of Stock*.

### 3. Fast Individual-Product Sales System (CRITICAL)
- **Streamlined Workflow**: Search and select any product from inventory.
- **Stock Check**: Prevents selling more items than available in inventory.
- **Live Calculations**: Instant calculation of Total Revenue, Cost of Goods Sold, Gross Profit, and Profit Margin % using decimal-safe `BigDecimal` math.
- **Customer Association**: Optional link to existing customers for CRM tracking.
- **Immutable Snapshots**: Preserves historical purchase price and selling price snapshots permanently inside each transaction.
- **Duplicate Prevention**: Confirmation modal prevents accidental multiple taps.
- **Receipt Modal**: Digital receipt summary ready to share or print.

### 4. Lifetime Sales History & Safe Void / Refund
- **Structured Audit Table**: Date (Ethiopian Calendar), Product, Quantity, Purchase Price, Selling Price, Revenue, and Gross Profit.
- **Filtering**: Filter by date presets, product name, or customer name with instant aggregate recalculation.
- **Void / Refund Workflow**: Allows voiding accidental sales with a recorded reason; restores inventory stock back to the product automatically inside a database transaction.

### 5. Genuine Ethiopian (Ge'ez) Calendar Engine
- **13-Month Support**: 12 months of 30 days + *Pagume* (5 or 6 days depending on Ethiopian leap years).
- **Leap Year Algorithm**: Precise calculation (`year % 4 == 3`).
- **Interactive Date Picker**: Custom Compose date picker with English and Amharic month names (*መስከረም* through *ጳጉሜ*).
- **Canonical Gregorian Conversion**: Bidirectional conversions for scheduling and filtering.

### 6. Customer CRM & Automated Re-engagement
- **Customer Profiles**: Full name, phone number, purchase history, and personal notes.
- **Celebration Management**: Record celebrations (e.g. *Mother's Birthday*, *Wedding Anniversary*, *Graduation*) with recurrence cycles (Yearly, Monthly, Once).
- **7-Day Advance Reminders**: Automated local notifications triggered 7 days ahead of celebrations.
- **Contact Shortcuts**: Native Android phone dialer integration (`ACTION_DIAL`) and WhatsApp pre-filled marketing message sharing in English and Amharic.

### 7. Complete Data Ownership (JSON Backup & Restore)
- **Zero Cloud / No Subscriptions**: 100% offline, local SQLite database backed by Android Room.
- **SAF JSON Export**: Exports complete database (products, sales, customers, celebrations, movements, settings) to a portable JSON file.
- **Validation & Preview**: Previews record counts before restoring; executes restoration inside a database transaction.

### 8. English & Amharic (አማርኛ) Localization
- Instant in-app language switcher in Settings.
- Full UI translation across all headers, cards, tables, dialogs, receipts, and validation messages.

---

## 🛠 Architecture & Tech Stack

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose with Material 3 (Light/White professional corporate theme)
- **Architecture**: Clean Architecture with MVVM & Repository pattern
- **Local Persistence**: Android Room Database (SQLite) with KSP
- **Concurrency**: Kotlin Coroutines & Flow
- **Math Engine**: Decimal-safe `BigDecimal` for currency and inventory
- **Platform**: Native Android (minSdk 24, targetSdk 36)

---

## 🚀 How to Build and Run

1. Open project in **Android Studio** or AI Studio.
2. Ensure JDK 11 or higher is selected.
3. Run `gradle assembleDebug` to compile the debug APK.
4. Run unit and Robolectric tests: `gradle :app:testDebugUnitTest`.
