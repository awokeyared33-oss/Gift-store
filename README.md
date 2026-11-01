# MAKI BRANDS — Professional Offline Android Gift Shop Management Application

> **Every Sale. Every Customer. Every Celebration. Under Control.**

**MAKI BRANDS (V1.0)** is an offline, native Android application engineered for gift shops, retail boutiques, and celebratory merchandise businesses. It provides high-speed inventory tracking, individual-product sales execution, exact decimal-safe financial calculations (`BigDecimal`), a genuine 13-month Ethiopian (Ge'ez) Calendar engine, customer relationship management (CRM) with 7-day advance celebration reminders, and full JSON database backup & restore.

---

## 🌟 Key Capabilities & Features

### 1. Dashboard & Real-Time Business Analytics
- **Live Financial Summary**: Real-time calculations of Today's and Lifetime Revenue, Gross Profit, and Sales count.
- **Inventory Status**: Total in-stock units, total inventory cost valuation (ETB), and proactive low-stock warning alerts.
- **Ethiopian Date Presets**: Today, Yesterday, This Week, This Month, This Year, All Time.
- **Upcoming Celebrations Carousel**: Displays customers with birthdays or anniversaries arriving within the next 7 to 14 days, with instant Call and WhatsApp action shortcuts.

### 2. Product & Inventory Management
- **Complete Inventory Records**: Product name, category, purchase price, selling price, current stock, and low-stock threshold.
- **Financial Protection**: Potential loss warning displayed automatically when the selling price is set below the purchase price.
- **Inventory Adjustments**: Fast restock (stock additions) and adjustment workflows (damaged/lost items) with an audit trail recorded permanently in the database.
- **Stock Indicators**: Dynamic visual badges for *In Stock*, *Low Stock*, and *Out of Stock*.

### 3. Fast Individual-Product Sales System
- **Streamlined Workflow**: Search and select any product from inventory.
- **Stock Check**: Prevents selling more items than available in inventory.
- **Live Calculations**: Instant calculation of Total Revenue, Cost of Goods Sold, Gross Profit, and Profit Margin % using decimal-safe `BigDecimal` math.
- **Customer Association**: Optional link to existing customers for CRM tracking.
- **Immutable Snapshots**: Preserves historical purchase price and selling price snapshots permanently inside each transaction.
- **Duplicate Prevention**: Confirmation modal prevents accidental multiple taps.
- **Digital Receipt Modal**: Clean receipt summary ready to view, print, or share.

### 4. Lifetime Sales History & Safe Void / Refund
- **Structured Audit Table**: Date (Ethiopian Calendar), Product, Quantity, Purchase Price, Selling Price, Revenue, and Gross Profit.
- **Filtering & Search**: Filter by date presets, product name, or customer name with instant aggregate recalculation.
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
- **UI Framework**: Jetpack Compose with Material 3 (White/Teal corporate clean theme)
- **Architecture**: MVVM with Repository Pattern & Clean Architecture separation
- **Local Persistence**: Android Room Database (SQLite) with KSP & Compound Indexing (Schema v4)
- **Concurrency**: Kotlin Coroutines & Flow
- **Math Engine**: Decimal-safe `BigDecimal` for currency and inventory
- **Target Platform**: Android API 24 (Nougat) up to API 36 (Android 16)
- **Application ID**: `com.aistudio.makibrands.mkgft`

---

## 🚀 How to Open, Build, and Run in Android Studio

1. **Prerequisites**:
   - Android Studio Ladybug (2024.2+) or newer.
   - Java Development Kit (JDK) 11, 17, or 21.
   - Android SDK with API 34+ installed.

2. **Opening the Project**:
   - Extract `MAKI_BRANDS_V1_COMPLETE_SOURCE_CODE.zip` to your desired folder.
   - Launch Android Studio.
   - Select **File > Open...** and choose the extracted root folder.
   - Wait for Android Studio to index the project and sync Gradle files.

3. **Building the APK**:
   - Via Terminal in Android Studio:
     ```bash
     ./gradlew assembleDebug
     ```
   - Or in the menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - The compiled installable APK will be in `app/build/outputs/apk/debug/app-debug.apk`.

4. **Running Unit & Stress Tests**:
   - Run in terminal:
     ```bash
     ./gradlew test
     ```
   - Includes 26 automated unit and 10-year scalability stress tests (all passing).

5. **Release Signing**:
   - To sign a release build, configure your keystore path, store password, and key alias in your environment or `app/build.gradle.kts`.
