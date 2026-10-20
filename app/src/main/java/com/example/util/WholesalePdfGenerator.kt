package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.PhoneItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

/**
 * Data representation of a device included in the Wholesale Price List export
 */
data class WholesalePdfItem(
    val id: Long = 0L,
    val brand: String = "",
    val mobileName: String,
    val color: String = "",
    val grade: String = "A", // Device Grade/Condition e.g. "A", "A+", "Fresh", etc.
    val storageRam: String,
    val boxStatus: String, // Exact box condition e.g. "With Box", "No Box", "Only Set", etc.
    val wholesalePrice: Double
)

object WholesalePdfGenerator {

    /**
     * Determines whether a stock device has a box based on packaging specification.
     * If packaging contains "Box" (e.g. "Full Box", "With Box"), returns true.
     * Otherwise returns false (e.g. "Only Set", "Set & Charger", "Set & Cover").
     */
    fun hasBox(phone: PhoneItem): Boolean {
        val pkg = phone.packaging.trim()
        return pkg.contains("box", ignoreCase = true)
    }

    /**
     * Formats grade string to clean Grade letter/indicator (e.g. "A", "A+", "Fresh", "B", etc.)
     */
    fun formatGradeDisplay(rawGrade: String): String {
        val trimmed = rawGrade.trim()
        if (trimmed.isBlank()) return "A"
        val letter = com.example.data.model.formatGradeLetter(trimmed)
        return if (letter.isNotBlank()) letter else trimmed
    }

    /**
     * Formats box condition string to ensure words never run together without spaces
     * (e.g. guarantees "With Box" with clear visible space, "Only Set", "No Box", "Set & Charger").
     */
    fun formatBoxStatus(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.equals("WithBox", ignoreCase = true) || trimmed.equals("With Box", ignoreCase = true) -> "With Box"
            trimmed.equals("FullBox", ignoreCase = true) || trimmed.equals("Full Box", ignoreCase = true) -> "With Box"
            trimmed.equals("NoBox", ignoreCase = true) || trimmed.equals("No Box", ignoreCase = true) -> "No Box"
            trimmed.equals("OnlySet", ignoreCase = true) || trimmed.equals("Only Set", ignoreCase = true) -> "Only Set"
            trimmed.equals("Set&Charger", ignoreCase = true) || trimmed.equals("Set & Charger", ignoreCase = true) -> "Set & Charger"
            trimmed.equals("Set&Cover", ignoreCase = true) || trimmed.equals("Set & Cover", ignoreCase = true) -> "Set & Cover"
            trimmed.contains("box", ignoreCase = true) -> "With Box"
            trimmed.isNotBlank() -> trimmed.replace("&", " & ").replace(Regex("\\s+"), " ")
            else -> "No Box"
        }
    }

    /**
     * Automatically fetches the exact box condition string saved in the device entry.
     * (e.g. "With Box", "No Box", "Only Set", etc.).
     */
    fun getBoxStatusString(phone: PhoneItem): String {
        val pkg = phone.packaging.trim()
        return formatBoxStatus(pkg)
    }

    /**
     * Rounds price to nearest 500 / 1,000 round figure:
     * Examples: ৳10,480 -> ৳10,500; ৳10,050 -> ৳10,000.
     */
    fun roundToNearest500(price: Double): Double {
        return round(price / 500.0) * 500.0
    }

    /**
     * Calculates default margin based on landing cost:
     * - Base Cost <= 40,000 BDT -> 5,000 BDT
     * - Base Cost > 40,000 BDT  -> 7,000 BDT
     */
    fun calculateDefaultMargin(baseCost: Double): Double {
        return if (baseCost <= 40000.0) 5000.0 else 7000.0
    }

    /**
     * Calculates the automatic wholesale price based on landing/base cost:
     * - Base Cost <= 40,000 BDT -> Base Cost + 5,000 BDT
     * - Base Cost > 40,000 BDT  -> Base Cost + 7,000 BDT
     * Rounded to nearest 500 / 1,000 round figure.
     */
    fun calculateDefaultWholesalePrice(phone: PhoneItem): Double {
        val baseCost = if (phone.totalCostBdt > 0.0) {
            phone.totalCostBdt
        } else if (phone.buyingPriceBdt > 0.0) {
            phone.buyingPriceBdt
        } else {
            0.0
        }
        val margin = calculateDefaultMargin(baseCost)
        val rawPrice = baseCost + margin
        return roundToNearest500(rawPrice)
    }

    /**
     * Smart Duplicate Item Filtering:
     * When processing stock items for PDF export, items are considered duplicates ONLY if their name AND price
     * (along with RAM/ROM, grade, and box status) match 100%.
     * If two items share the same mobile model name but have different prices, they are NOT duplicates and must BOTH be listed.
     * If multiple identical devices (same model name, same RAM/ROM, same grade, same box status, same price) exist, display ONLY ONE entry.
     */
    fun filterSmartDuplicates(phones: List<PhoneItem>): List<PhoneItem> {
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<PhoneItem>()

        for (phone in phones) {
            val modelName = "${phone.brand.trim()} ${phone.model.trim()}".trim().lowercase()
            val storage = phone.ramRom.trim().replace("/", "-").lowercase()
            val grade = formatGradeDisplay(phone.conditionGrade).lowercase()
            val boxStatus = getBoxStatusString(phone).trim().lowercase()
            val price = calculateDefaultWholesalePrice(phone).toLong()

            // Key distinguishing identity: Name + RAM/ROM + Grade + Box Status + Price (100% match)
            val key = "$modelName|$storage|$grade|$boxStatus|$price"
            if (seenKeys.add(key)) {
                deduplicated.add(phone)
            }
        }
        return deduplicated
    }

    /**
     * Refined Duplicate Filtering for WholesalePdfItem list:
     * When processing stock items for PDF export, items are considered duplicates ONLY if
     * their name AND price (along with RAM/ROM, grade, and box status) match 100%.
     * If two items share the same mobile model name but have different prices, they are NOT duplicates and must BOTH be listed.
     * If multiple identical devices (same model name, same RAM/ROM, same grade, same box status, same price) exist, display ONLY ONE entry.
     */
    fun filterPdfItemDuplicates(items: List<WholesalePdfItem>): List<WholesalePdfItem> {
        val seenKeys = mutableSetOf<String>()
        val deduplicated = mutableListOf<WholesalePdfItem>()

        for (item in items) {
            val name = item.mobileName.trim().lowercase()
            val storage = item.storageRam.trim().replace("/", "-").lowercase()
            val grade = formatGradeDisplay(item.grade).lowercase()
            val boxStatus = formatBoxStatus(item.boxStatus).lowercase()
            val price = item.wholesalePrice.toLong()

            val key = "$name|$storage|$grade|$boxStatus|$price"
            if (seenKeys.add(key)) {
                deduplicated.add(item)
            }
        }
        return deduplicated
    }

    /**
     * Generates a multi-page compact mobile-optimized PDF file for wholesale items with Brand Section Headers.
     * Document width is sized specifically for native smartphone viewing (360pt width) so users never need to pinch-zoom.
     * Saves to app cache directory and copies to Documents folder.
     */
    fun generateWholesalePdf(
        context: Context,
        items: List<WholesalePdfItem>,
        settings: AppSettings,
        updatedDateStr: String = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())
    ): File? {
        val uniqueItems = filterPdfItemDuplicates(items)
        if (uniqueItems.isEmpty()) return null

        val document = PdfDocument()

        // 1. Native Mobile Screen Dimensions: 360pt width fits 100% on any smartphone without pinch-zooming
        val pageWidth = 360 // Compact mobile-first width in points
        val pageHeight = 640 // Compact mobile-first height in points
        val leftMargin = 16f
        val rightMargin = 344f
        val contentWidth = rightMargin - leftMargin // 328pt

        // Colors
        val colorOrange = Color.rgb(255, 111, 0) // #FF6F00 Vibrant Orange accent
        val colorNavy = Color.rgb(15, 23, 42) // #0F172A
        val colorPrimary = Color.rgb(30, 58, 138) // #1E3A8A
        val colorSky = Color.rgb(2, 132, 199) // #0284C7
        val colorMuted = Color.rgb(100, 116, 139) // #64748B
        val colorBorder = Color.rgb(226, 232, 240) // #E2E8F0
        val colorRowEven = Color.rgb(248, 250, 252) // #F8FAFC
        val colorPriceBg = Color.rgb(239, 246, 255) // #EFF6FF
        val colorPriceBorder = Color.rgb(191, 219, 254) // #BFDBFE
        val colorBrandHeaderBg = Color.rgb(241, 245, 249) // #F1F5F9

        // Paints
        val brandBarPaint = Paint().apply {
            color = colorOrange
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Header Paints adjusted for compact mobile width
        val storeNamePaint = Paint().apply {
            color = colorOrange
            textSize = 21.5f // Noticeably enlarged & bold main brand title
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val storeNameSmallOrangePaint = Paint().apply {
            color = colorOrange
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        // Clean plain text date paint (no button container or outline)
        val datePaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600 - crisp, elegant contrast
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val addressCardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // #F8FAFC
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val addressCardStrokePaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // #E2E8F0
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val addressLabelPaint = Paint().apply {
            color = colorNavy
            textSize = 7.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.025f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val addressValPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 7.2f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.025f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val brandHeaderBgPaint = Paint().apply {
            color = colorBrandHeaderBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val brandHeaderStrokePaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // #CBD5E1
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        // 4. Enlarged Brand Headings with prominent bold typography and clear letter spacing
        val brandHeaderPaint = Paint().apply {
            color = colorNavy
            textSize = 15.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.04f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = colorBorder
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val rowBgPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Mobile Model Name: clear letter spacing and subpixel rendering
        val itemNamePaint = Paint().apply {
            color = colorNavy
            textSize = 12.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        // Specification details: crisp letter spacing, no character overlap
        val specDetailPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 8.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val priceBoxBgPaint = Paint().apply {
            color = colorPriceBg
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val priceBoxStrokePaint = Paint().apply {
            color = colorPriceBorder
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        // Wholesale Price: bold prominent styling, centered without label clutter
        val priceValPaint = Paint().apply {
            color = colorPrimary
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = colorMuted
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.02f
            isLinearText = true
            isSubpixelText = true
            isAntiAlias = true
        }

        // Group items by Brand Name
        val groupedItems: Map<String, List<WholesalePdfItem>> = uniqueItems
            .groupBy { it.brand.trim().ifBlank { "Other" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

        // Layout measurements optimized for mobile smartphone screen
        val itemHeight = 46f
        val brandHeaderHeight = 28f
        val page1StartY = 124f
        val subsequentStartY = 42f
        val maxY = 604f
        val footerY = 624f

        // Simulation pass to calculate exact total page count
        var simulatedPages = 1
        var simY = page1StartY
        for ((_, bItems) in groupedItems) {
            if (simY + brandHeaderHeight + itemHeight > maxY) {
                simulatedPages++
                simY = subsequentStartY
            }
            simY += brandHeaderHeight

            for (ignored in bItems) {
                if (simY + itemHeight > maxY) {
                    simulatedPages++
                    simY = subsequentStartY
                }
                simY += itemHeight
            }
            simY += 4f // Brand spacing
        }
        val totalPages = simulatedPages

        var currentPageNumber = 1
        var currentY = page1StartY
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
        var canvas = page.canvas

        fun drawPageHeader(c: Canvas, isFirstPage: Boolean) {
            if (isFirstPage) {
                // 1. Primary store header line: noticeably enlarged and bold "Zoha Trading" in vibrant orange
                c.drawText("Zoha Trading", leftMargin, 31f, storeNamePaint)

                // 2. Right header: display "Updated: [Current Date]" strictly as clean, plain text (no button container or border)
                val updatedText = "Updated: $updatedDateStr"
                val updatedWidth = datePaint.measureText(updatedText)
                c.drawText(updatedText, rightMargin - updatedWidth, 29.5f, datePaint)

                // 3. Stacked Address Cards: Executive business card styling for mobile clarity without left decorative lines
                // Card 1: Dhaka Office/Shop
                val dhakaCardTop = 40f
                val dhakaCardBottom = 73f
                val dhakaRect = RectF(leftMargin, dhakaCardTop, rightMargin, dhakaCardBottom)
                c.drawRoundRect(dhakaRect, 4f, 4f, addressCardBgPaint)
                c.drawRoundRect(dhakaRect, 4f, 4f, addressCardStrokePaint)

                c.drawText("Dhaka Office / Shop", leftMargin + 8f, 50f, addressLabelPaint)
                c.drawText("Shop 609, Tropical Alauddin Tower, Uttara, Dhaka.", leftMargin + 8f, 60.5f, addressValPaint)
                val dhakaHotlineLabel = "Hotline: "
                val dhakaHotlineVal = "+8801635510135, +8801971100786"
                c.drawText(dhakaHotlineLabel, leftMargin + 8f, 69.5f, addressLabelPaint)
                val dhakaHlWidth = addressLabelPaint.measureText(dhakaHotlineLabel)
                c.drawText(dhakaHotlineVal, leftMargin + 8f + dhakaHlWidth, 69.5f, addressValPaint)

                // Card 2: Shenzhen Office
                val szCardTop = 77f
                val szCardBottom = 110f
                val szRect = RectF(leftMargin, szCardTop, rightMargin, szCardBottom)
                c.drawRoundRect(szRect, 4f, 4f, addressCardBgPaint)
                c.drawRoundRect(szRect, 4f, 4f, addressCardStrokePaint)

                c.drawText("Shenzhen Office", leftMargin + 8f, 87f, addressLabelPaint)
                c.drawText("702, Unit 2, Building 2, HC Town Garden, Pingshan District, Shenzhen.", leftMargin + 8f, 97.5f, addressValPaint)
                val szHotlineLabel = "Hotline: "
                val szHotlineVal = "+8618565695152 (WhatsApp, WeChat)"
                c.drawText(szHotlineLabel, leftMargin + 8f, 106.5f, addressLabelPaint)
                val szHlWidth = addressLabelPaint.measureText(szHotlineLabel)
                c.drawText(szHotlineVal, leftMargin + 8f + szHlWidth, 106.5f, addressValPaint)

                // Clean divider line below address cards
                c.drawLine(leftMargin, 116f, rightMargin, 116f, dividerPaint)
            } else {
                c.drawText("Zoha Trading", leftMargin, 28f, storeNameSmallOrangePaint)

                // Right header on subsequent pages: cleanly aligned date badge
                val updatedText = "Updated: $updatedDateStr"
                val updatedWidth = datePaint.measureText(updatedText)
                c.drawText(updatedText, rightMargin - updatedWidth, 28f, datePaint)
                c.drawLine(leftMargin, 34f, rightMargin, 34f, dividerPaint)
            }
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            c.drawLine(leftMargin, footerY - 8f, rightMargin, footerY - 8f, dividerPaint)
            val footerLeft = "Zoha Trading"
            c.drawText(footerLeft, leftMargin, footerY, footerPaint)

            val pageStr = "Page $pageNum of $totalPages"
            val pageStrWidth = footerPaint.measureText(pageStr)
            c.drawText(pageStr, rightMargin - pageStrWidth, footerY, footerPaint)
        }

        // Draw initial Page 1 header
        drawPageHeader(canvas, true)

        var isEvenRow = false

        try {
            for ((brand, bItems) in groupedItems) {
                // Check if brand header + 1 item fits on current page
                if (currentY + brandHeaderHeight + itemHeight > maxY) {
                    drawFooter(canvas, currentPageNumber)
                    document.finishPage(page)
                    currentPageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
                    canvas = page.canvas
                    drawPageHeader(canvas, false)
                    currentY = subsequentStartY
                }

                // 4. Enlarged Highlighted Brand Section Header with NO count numbers (e.g. "Vivo", "Xiaomi", "Apple")
                val headerTop = currentY
                val headerBottom = currentY + brandHeaderHeight - 4f
                val headerRect = RectF(leftMargin, headerTop, rightMargin, headerBottom)

                canvas.drawRoundRect(headerRect, 5f, 5f, brandHeaderBgPaint)
                canvas.drawRoundRect(headerRect, 5f, 5f, brandHeaderStrokePaint)

                // Vibrant Orange accent pill/bar on left edge of brand banner
                canvas.drawRoundRect(
                    RectF(leftMargin, headerTop, leftMargin + 4.5f, headerBottom),
                    2.5f,
                    2.5f,
                    brandBarPaint
                )

                canvas.drawText(brand.trim().uppercase(), leftMargin + 10f, headerTop + 17f, brandHeaderPaint)
                currentY += brandHeaderHeight

                // Draw items under this brand strictly UNNUMBERED
                for (item in bItems) {
                    if (currentY + itemHeight > maxY) {
                        drawFooter(canvas, currentPageNumber)
                        document.finishPage(page)
                        currentPageNumber++
                        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
                        canvas = page.canvas
                        drawPageHeader(canvas, false)
                        currentY = subsequentStartY
                    }

                    val rowTop = currentY
                    val rowBottom = currentY + itemHeight

                    // Alternating background for rows
                    isEvenRow = !isEvenRow
                    if (isEvenRow) {
                        rowBgPaint.color = colorRowEven
                        canvas.drawRoundRect(
                            RectF(leftMargin, rowTop, rightMargin, rowBottom),
                            4f,
                            4f,
                            rowBgPaint
                        )
                    }

                    // 3. Compact right-aligned Wholesale Price Block (NO "Wholesale" text above price)
                    val priceBoxWidth = 84f
                    val priceBoxHeight = 26f
                    val priceBoxLeft = rightMargin - priceBoxWidth
                    val priceBoxTop = rowTop + (itemHeight - priceBoxHeight) / 2f
                    val priceBoxRight = rightMargin
                    val priceBoxBottom = priceBoxTop + priceBoxHeight

                    canvas.drawRoundRect(
                        RectF(priceBoxLeft, priceBoxTop, priceBoxRight, priceBoxBottom),
                        5f,
                        5f,
                        priceBoxBgPaint
                    )
                    canvas.drawRoundRect(
                        RectF(priceBoxLeft, priceBoxTop, priceBoxRight, priceBoxBottom),
                        5f,
                        5f,
                        priceBoxStrokePaint
                    )

                    val formattedPrice = "৳ " + String.format(Locale.US, "%,d", item.wholesalePrice.toLong())
                    val priceTextWidth = priceValPaint.measureText(formattedPrice)
                    val priceX = priceBoxLeft + (priceBoxWidth - priceTextWidth) / 2f
                    val priceY = priceBoxTop + (priceBoxHeight / 2f) + 4.5f
                    canvas.drawText(formattedPrice, priceX, priceY, priceValPaint)

                    // Reduced middle gap: left text takes available width up to priceBoxLeft - 6f
                    val maxNameWidth = priceBoxLeft - leftMargin - 6f

                    // 1. Mobile Name Headline (significantly INCREASED font size for mobile reading)
                    val itemHeadline = item.mobileName.trim()
                    val displayHeadline = truncateToWidth(itemHeadline, itemNamePaint, maxNameWidth)
                    canvas.drawText(displayHeadline, leftMargin + 4f, rowTop + 17f, itemNamePaint)

                    // 2. Middle Specification Line: Storage: [RAM-ROM] . Grade: [Grade] . [Box Status] (larger crisp font)
                    val storageClean = item.storageRam.trim().replace("/", "-").ifBlank { "Standard" }
                    val gradeClean = formatGradeDisplay(item.grade)
                    val boxClean = formatBoxStatus(item.boxStatus)
                    val specLine = "Storage: $storageClean . Grade: $gradeClean . $boxClean"
                    val displaySpecLine = truncateToWidth(specLine, specDetailPaint, maxNameWidth)
                    canvas.drawText(displaySpecLine, leftMargin + 4f, rowTop + 34f, specDetailPaint)

                    // Divider
                    canvas.drawLine(leftMargin, rowBottom, rightMargin, rowBottom, dividerPaint)

                    currentY += itemHeight
                }
                currentY += 4f // spacing after brand group
            }

            // Finish the last page
            drawFooter(canvas, currentPageNumber)
            document.finishPage(page)

            // Save PDF file
            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val filename = "Zoha_Trading_Wholesale_Price_List_$dateStamp.pdf"
            val cacheFile = File(context.cacheDir, filename)

            FileOutputStream(cacheFile).use { out ->
                document.writeTo(out)
            }

            try {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                val localFile = File(docsDir, filename)
                cacheFile.copyTo(localFile, overwrite = true)
            } catch (_: Exception) {}

            return cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }

    private fun truncateToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    /**
     * View/Open PDF file via an external viewer app
     */
    fun viewPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Wholesale Price List PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open PDF: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share PDF file via WhatsApp, Email, Drive, etc.
     */
    fun sharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Wholesale Price List (${file.name})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
