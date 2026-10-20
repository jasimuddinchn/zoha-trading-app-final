package com.example

import com.example.data.model.APPLE_STORAGE_OPTIONS
import com.example.data.model.AppSettings
import com.example.data.model.BATTERY_HEALTH_OPTIONS
import com.example.data.model.COLOR_OPTIONS
import com.example.data.model.ConditionGrade
import com.example.data.model.DEFAULT_STORAGE_OPTIONS
import com.example.data.model.EXPENSE_CATEGORIES_EN
import com.example.data.model.PACKAGING_OPTIONS
import com.example.data.model.PhoneItem
import com.example.data.model.PhoneStatus
import com.example.data.model.STORAGE_OPTIONS
import com.example.data.model.VARIANT_OPTIONS
import com.example.data.model.getStorageOptionsForBrand
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testInitialAppSettings() {
    val settings = AppSettings()
    assertTrue(settings.isAdminPinSetupCompleted)
    assertEquals("1111", settings.ownerPin)
    assertEquals("2222", settings.staff1Pin)
    assertEquals("3333", settings.staff2Pin)
  }

  @Test
  fun testHardwareDropdownOptions() {
    assertEquals("Good", BATTERY_HEALTH_OPTIONS.first())
    assertEquals("100%", BATTERY_HEALTH_OPTIONS[1])
    assertEquals("99%", BATTERY_HEALTH_OPTIONS[2])
    assertTrue(BATTERY_HEALTH_OPTIONS.contains("Custom..."))
    assertEquals("Custom...", BATTERY_HEALTH_OPTIONS.last())

    assertTrue(COLOR_OPTIONS.contains("Black"))
    assertTrue(COLOR_OPTIONS.contains("Custom..."))
    assertEquals("Custom...", COLOR_OPTIONS.last())

    assertTrue(DEFAULT_STORAGE_OPTIONS.contains("12-128GB"))
    assertTrue(DEFAULT_STORAGE_OPTIONS.contains("Custom..."))
    assertEquals("Custom...", DEFAULT_STORAGE_OPTIONS.last())

    assertTrue(APPLE_STORAGE_OPTIONS.contains("128GB"))
    assertTrue(APPLE_STORAGE_OPTIONS.contains("1TB"))
    assertTrue(APPLE_STORAGE_OPTIONS.contains("Custom..."))
    assertEquals("Custom...", APPLE_STORAGE_OPTIONS.last())

    assertEquals(APPLE_STORAGE_OPTIONS, getStorageOptionsForBrand("Apple"))
    assertEquals(APPLE_STORAGE_OPTIONS, getStorageOptionsForBrand("apple"))
    assertEquals(DEFAULT_STORAGE_OPTIONS, getStorageOptionsForBrand("Samsung"))
    assertEquals(DEFAULT_STORAGE_OPTIONS, getStorageOptionsForBrand("Xiaomi"))

    assertTrue(VARIANT_OPTIONS.contains("Global"))
    assertTrue(VARIANT_OPTIONS.contains("Custom..."))
    assertEquals("Custom...", VARIANT_OPTIONS.last())

    val expectedPackaging = listOf(
      "Full Box",
      "With Box",
      "Only Set",
      "Set & Charger",
      "Set & Cover",
      "Custom..."
    )
    assertEquals(expectedPackaging, PACKAGING_OPTIONS)
  }

  @Test
  fun testConditionGrades() {
    val grades = ConditionGrade.values().map { it.labelEn }
    assertEquals(listOf("Grade A", "Grade B", "Grade C"), grades)
  }

  @Test
  fun testExpenseCategories() {
    val expected = listOf(
      "Shop Rent",
      "Service Charge",
      "Electricity Bill",
      "Internet Bill",
      "Shop Cleaner",
      "Courier Bill",
      "Salary",
      "Commission",
      "Guest",
      "Parts & Repairs",
      "Accessories",
      "Brac Bank Deposit",
      "City Bank Deposit",
      "UCB Bank Deposit",
      "Custom..."
    )
    assertEquals(expected, EXPENSE_CATEGORIES_EN)
  }

  @Test
  fun testCustomerPurchaseHistoryLedger() {
    val phones = listOf(
      PhoneItem(
        id = 1,
        brand = "Apple",
        model = "iPhone 13",
        imei = "111111111111111",
        entryDate = "2026-08-01",
        status = PhoneStatus.SOLD.code,
        customerName = "Mr. Rahman",
        customerPhone = "01711000000",
        sellingPriceBdt = 65000.0,
        saleDate = "2026-08-10"
      ),
      PhoneItem(
        id = 2,
        brand = "Samsung",
        model = "Galaxy S23",
        imei = "222222222222222",
        entryDate = "2026-08-05",
        status = PhoneStatus.SOLD.code,
        customerName = "Mr. Rahman",
        customerPhone = "01711000000",
        sellingPriceBdt = 75000.0,
        saleDate = "2026-08-15"
      ),
      PhoneItem(
        id = 3,
        brand = "Google",
        model = "Pixel 7 Pro",
        imei = "333333333333333",
        entryDate = "2026-08-12",
        status = PhoneStatus.SOLD.code,
        customerName = "Tanvir Ahmed",
        customerPhone = "01811999999",
        sellingPriceBdt = 52000.0,
        saleDate = "2026-08-18"
      )
    )

    val rahmanPurchases = phones.filter { it.customerPhone == "01711000000" }
    assertEquals(2, rahmanPurchases.size)
    assertEquals(140000.0, rahmanPurchases.sumOf { it.sellingPriceBdt }, 0.001)

    val tanvirPurchases = phones.filter { it.customerPhone == "01811999999" }
    assertEquals(1, tanvirPurchases.size)
    assertEquals(52000.0, tanvirPurchases.sumOf { it.sellingPriceBdt }, 0.001)
  }

  @Test
  fun testDueAndCashReconciliationCalculations() {
    val creditSale = PhoneItem(
      id = 101,
      brand = "Apple",
      model = "iPhone 15 Pro",
      imei = "869281048291048",
      entryDate = "2026-10-01",
      status = PhoneStatus.SOLD.code,
      sellingPriceBdt = 120000.0,
      cashReceivedBdt = 70000.0,
      dueAmountBdt = 50000.0,
      customerName = "Hasan Mahmud",
      customerPhone = "01712345678",
      saleDate = "2026-10-05"
    )

    // Rule 1: Effective Cash Received is strictly the cash portion
    assertEquals(70000.0, creditSale.effectiveCashReceived, 0.001)
    // Rule 2: Effective Due Amount is strictly the remaining due portion
    assertEquals(50000.0, creditSale.effectiveDueAmount, 0.001)

    // Rule 3: Dynamic Total Net Balance = Baseline Opening Capital + All-Time Cash Received + Due Collections - All-Time Expenses
    val startingCapital = 16845.0
    val cashReceivedFromSale = creditSale.effectiveCashReceived // 70,000
    val dueCollectedPayment = 20000.0 // Collected payment
    val expenses = 5000.0

    val netBalance = startingCapital + cashReceivedFromSale + dueCollectedPayment - expenses
    assertEquals(101845.0, netBalance, 0.001)

    // Remaining customer due after payment
    val remainingCustomerDue = (creditSale.effectiveDueAmount - dueCollectedPayment).coerceAtLeast(0.0)
    assertEquals(30000.0, remainingCustomerDue, 0.001)
  }

  @Test
  fun testWholesaleMarginAndBoxCalculations() {
    val phoneUnder40k = PhoneItem(
      id = 1,
      brand = "Xiaomi",
      model = "Redmi Note 13",
      imei = "123456789012345",
      entryDate = "2026-10-01",
      packaging = "With Box",
      totalCostBdt = 25000.0
    )

    val phoneAt40k = PhoneItem(
      id = 2,
      brand = "Samsung",
      model = "Galaxy A35",
      imei = "223456789012345",
      entryDate = "2026-10-01",
      packaging = "Full Box",
      totalCostBdt = 40000.0
    )

    val phoneOver40k = PhoneItem(
      id = 3,
      brand = "Apple",
      model = "iPhone 15",
      imei = "323456789012345",
      entryDate = "2026-10-01",
      packaging = "Only Set",
      totalCostBdt = 68000.0
    )

    val phoneWithoutBox = PhoneItem(
      id = 4,
      brand = "OnePlus",
      model = "Nord 4",
      imei = "423456789012345",
      entryDate = "2026-10-01",
      packaging = "Set & Charger",
      totalCostBdt = 32000.0
    )

    // Margin rule: <= 40,000 BDT -> Base + 5,000 BDT
    assertEquals(5000.0, com.example.util.WholesalePdfGenerator.calculateDefaultMargin(25000.0), 0.001)
    assertEquals(5000.0, com.example.util.WholesalePdfGenerator.calculateDefaultMargin(40000.0), 0.001)
    assertEquals(30000.0, com.example.util.WholesalePdfGenerator.calculateDefaultWholesalePrice(phoneUnder40k), 0.001)
    assertEquals(45000.0, com.example.util.WholesalePdfGenerator.calculateDefaultWholesalePrice(phoneAt40k), 0.001)

    // Margin rule: > 40,000 BDT -> Base + 7,000 BDT
    assertEquals(7000.0, com.example.util.WholesalePdfGenerator.calculateDefaultMargin(40001.0), 0.001)
    assertEquals(7000.0, com.example.util.WholesalePdfGenerator.calculateDefaultMargin(68000.0), 0.001)
    assertEquals(75000.0, com.example.util.WholesalePdfGenerator.calculateDefaultWholesalePrice(phoneOver40k), 0.001)

    // Box status conditions
    assertTrue(com.example.util.WholesalePdfGenerator.hasBox(phoneUnder40k))
    assertEquals("With Box", com.example.util.WholesalePdfGenerator.getBoxStatusString(phoneUnder40k))

    assertTrue(com.example.util.WholesalePdfGenerator.hasBox(phoneAt40k))
    assertEquals("With Box", com.example.util.WholesalePdfGenerator.getBoxStatusString(phoneAt40k))

    assertFalse(com.example.util.WholesalePdfGenerator.hasBox(phoneOver40k))
    assertEquals("Only Set", com.example.util.WholesalePdfGenerator.getBoxStatusString(phoneOver40k))

    assertFalse(com.example.util.WholesalePdfGenerator.hasBox(phoneWithoutBox))
    assertEquals("Set & Charger", com.example.util.WholesalePdfGenerator.getBoxStatusString(phoneWithoutBox))

    // Round figure price calculation (nearest 500 / 1000)
    // ৳10,480 -> ৳10,500; ৳10,050 -> ৳10,000
    assertEquals(10500.0, com.example.util.WholesalePdfGenerator.roundToNearest500(10480.0), 0.001)
    assertEquals(10000.0, com.example.util.WholesalePdfGenerator.roundToNearest500(10050.0), 0.001)

    // Smart duplicate item filtering
    val duplicatesList = listOf(
      PhoneItem(id = 1, brand = "Xiaomi", model = "Redmi Note 13", color = "Black", ramRom = "8/256GB", packaging = "With Box", totalCostBdt = 25000.0, entryDate = "2026-10-01", imei = "111"),
      PhoneItem(id = 2, brand = "Xiaomi", model = "Redmi Note 13", color = "Black", ramRom = "8/256GB", packaging = "With Box", totalCostBdt = 25000.0, entryDate = "2026-10-01", imei = "222"), // duplicate
      PhoneItem(id = 3, brand = "Xiaomi", model = "Redmi Note 13", color = "Black", ramRom = "8/256GB", packaging = "Only Set", totalCostBdt = 25000.0, entryDate = "2026-10-01", imei = "333"), // different box status ("No Box")
      PhoneItem(id = 4, brand = "Xiaomi", model = "Redmi Note 13", color = "Black", ramRom = "12/256GB", packaging = "With Box", totalCostBdt = 25000.0, entryDate = "2026-10-01", imei = "444"), // different storage
      PhoneItem(id = 5, brand = "Xiaomi", model = "Redmi Note 13", color = "Black", ramRom = "8/256GB", packaging = "With Box", totalCostBdt = 28000.0, entryDate = "2026-10-01", imei = "555") // different price
    )

    val deduplicated = com.example.util.WholesalePdfGenerator.filterSmartDuplicates(duplicatesList)
    assertEquals(4, deduplicated.size) // id 1 & 2 deduplicated to 1; id 3, 4, 5 retained as separate entries
    assertEquals(listOf(1L, 3L, 4L, 5L), deduplicated.map { it.id })

    // Test formatBoxStatus word spacing
    assertEquals("With Box", com.example.util.WholesalePdfGenerator.formatBoxStatus("WithBox"))
    assertEquals("With Box", com.example.util.WholesalePdfGenerator.formatBoxStatus("With Box"))
    assertEquals("Only Set", com.example.util.WholesalePdfGenerator.formatBoxStatus("OnlySet"))
    assertEquals("No Box", com.example.util.WholesalePdfGenerator.formatBoxStatus("NoBox"))
    assertEquals("Set & Charger", com.example.util.WholesalePdfGenerator.formatBoxStatus("Set&Charger"))

    // Test filterPdfItemDuplicates: duplicates ONLY if name AND price (with ram/rom and box) match 100%
    val pdfItems = listOf(
      com.example.util.WholesalePdfItem(id = 1, brand = "Apple", mobileName = "iPhone 13", color = "Black", storageRam = "128GB", boxStatus = "With Box", wholesalePrice = 60000.0),
      com.example.util.WholesalePdfItem(id = 2, brand = "Apple", mobileName = "iPhone 13", color = "White", storageRam = "128GB", boxStatus = "With Box", wholesalePrice = 60000.0), // duplicate
      com.example.util.WholesalePdfItem(id = 3, brand = "Apple", mobileName = "iPhone 13", color = "Black", storageRam = "128GB", boxStatus = "With Box", wholesalePrice = 58000.0), // different price -> NOT duplicate
      com.example.util.WholesalePdfItem(id = 4, brand = "Apple", mobileName = "iPhone 13", color = "Black", storageRam = "256GB", boxStatus = "With Box", wholesalePrice = 60000.0), // different storage -> NOT duplicate
      com.example.util.WholesalePdfItem(id = 5, brand = "Apple", mobileName = "iPhone 13", color = "Black", storageRam = "128GB", boxStatus = "No Box", wholesalePrice = 60000.0)   // different box -> NOT duplicate
    )
    val dedupPdfItems = com.example.util.WholesalePdfGenerator.filterPdfItemDuplicates(pdfItems)
    assertEquals(4, dedupPdfItems.size)
    assertEquals(listOf(1L, 3L, 4L, 5L), dedupPdfItems.map { it.id })
  }

  @Test
  fun testInAppUpdateVersionComparison() {
    // 1. Version extraction from filenames
    assertEquals("1.0.1", com.example.util.InAppUpdateManager.extractVersionFromFilename("Zoha Trading_1.0.1.apk"))
    assertEquals("1.0.4", com.example.util.InAppUpdateManager.extractVersionFromFilename("Zoha_Trading_v1.0.4.apk"))
    assertEquals("2.0.0", com.example.util.InAppUpdateManager.extractVersionFromFilename("Zoha Trading 2.0.0.apk"))

    // 2. Drive version equals installed version -> returns 0 (No download, popup No New Update)
    assertEquals(0, com.example.util.InAppUpdateManager.compareVersionStrings("1.0.3", "1.0.3"))

    // 3. Drive version is lower than installed version -> returns -1 (No download, popup No New Update)
    assertTrue(com.example.util.InAppUpdateManager.compareVersionStrings("1.0.1", "1.0.3") < 0)

    // 4. Drive version is higher than installed version -> returns 1 (Will download & install)
    assertTrue(com.example.util.InAppUpdateManager.compareVersionStrings("1.0.4", "1.0.3") > 0)
    assertTrue(com.example.util.InAppUpdateManager.compareVersionStrings("2.0.0", "1.0.3") > 0)
  }
}

