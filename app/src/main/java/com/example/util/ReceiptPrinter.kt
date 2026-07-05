package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import com.example.data.Sale
import com.example.data.Product
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    /**
     * Formats receipt details into a standard 32-character thermal paper layout.
     * Perfect for 58mm POS receipt printers.
     */
    fun formatThermalReceipt(
        sale: Sale,
        cartItems: List<Pair<Product, Int>>?,
        discountAmount: Double = 0.0,
        taxAmount: Double = 0.0,
        storeName: String = "GH POS & RETAILS LTD",
        storePhone: String = "+233 (0) 244-123456",
        storeLocation: String = "Accra Mall Road, Accra-Ghana",
        storeFooter: String = "THANK YOU FOR YOUR PATRONAGE!",
        storeTaxId: String = "",
        width: Int = 32
    ): String {
        val sb = StringBuilder()
        
        // 1. Centered Header
        sb.appendLine(centerText(storeName, width))
        sb.appendLine(centerText(storeLocation, width))
        if (storePhone.isNotBlank()) {
            sb.appendLine(centerText("Tel: $storePhone", width))
        }
        if (storeTaxId.isNotBlank()) {
            sb.appendLine(centerText("TIN/TAX ID: $storeTaxId", width))
        }
        sb.appendLine(customLine('-', width))

        // 2. Metadata
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = format.format(Date(sale.timestamp))
        sb.appendLine(leftRightAlign("DATE:", dateStr, width))
        sb.appendLine(leftRightAlign("INVOICE ID:", "#S-${sale.id}", width))
        sb.appendLine(leftRightAlign("CASHIER:", "GENERAL_STAFF", width))
        if (sale.customerName.isNotBlank() && !sale.customerName.equals("Walk-in Customer", ignoreCase = true)) {
            sb.appendLine(leftRightAlign("CUSTOMER:", sale.customerName, width))
        }
        sb.appendLine(customLine('-', width))

        // 3. Header Row for Items
        sb.appendLine(formatThreeColumns("ITEM DESCRIPTION", "QTY", "TOTAL", width))
        sb.appendLine(customLine('-', width))

        // 4. Print items
        if (!cartItems.isNullOrEmpty()) {
            // Precise item logging from active basket
            cartItems.forEach { (product, quantity) ->
                val lineTotal = product.price * quantity
                // If product name is too long, we display it on its own line first, or truncate
                if (product.name.length > 16) {
                    sb.appendLine(product.name)
                    sb.appendLine(formatThreeColumns("  (${quantity}x " + String.format(Locale.getDefault(), "%.2f", product.price) + ")", "", "GH₵" + String.format(Locale.getDefault(), "%.2f", lineTotal), width))
                } else {
                    val subDesc = "${product.name} (${quantity}x)"
                    sb.appendLine(formatThreeColumns(subDesc, "", "GH₵" + String.format(Locale.getDefault(), "%.2f", lineTotal), width))
                }
            }
        } else {
            // Attempt secondary parsing from the historic text-description field
            val desc = sale.description
            if (desc.startsWith("Products: ")) {
                // Parse products segment
                // Format: "Products: 2x Soap, 1x Bread | Discount: 10%" or similar
                val mainPart = desc.removePrefix("Products: ").split("|").firstOrNull()?.trim() ?: ""
                val items = mainPart.split(",")
                items.forEach { itemText ->
                    val cleanItem = itemText.trim()
                    if (cleanItem.isNotEmpty()) {
                        // Find quantity (e.g. "2x Soap" or "10 Soap")
                        val regex = "^(\\d+)x?\\s+(.*)".toRegex()
                        val match = regex.find(cleanItem)
                        if (match != null) {
                            val qty = match.groupValues[1]
                            val name = match.groupValues[2]
                            sb.appendLine(formatThreeColumns(name, qty, "", width))
                        } else {
                            sb.appendLine(leftRightAlign(cleanItem, "", width))
                        }
                    }
                }
            } else {
                // Generic string backup
                val chunks = splitStringInChunks(desc, width - 2)
                chunks.forEach { chunk ->
                    sb.appendLine(centerText(chunk, width))
                }
            }
        }
        sb.appendLine(customLine('-', width))

        // 5. Calculations summary details
        val calculatedSubtotal = sale.amount + discountAmount - taxAmount
        val subtotalToPrint = if (calculatedSubtotal > 0) calculatedSubtotal else sale.amount
        
        sb.appendLine(leftRightAlign("SUBTOTAL:", "GH₵" + String.format(Locale.getDefault(), "%,.2f", subtotalToPrint), width))
        if (discountAmount > 0) {
            sb.appendLine(leftRightAlign("DISCOUNT:", "-GH₵" + String.format(Locale.getDefault(), "%,.2f", discountAmount), width))
        }
        if (taxAmount > 0) {
            sb.appendLine(leftRightAlign("TAX AMOUNT:", "GH₵" + String.format(Locale.getDefault(), "%,.2f", taxAmount), width))
        }
        sb.appendLine(customLine('-', width))
        
        val typeLabel = if (sale.isCredit) "CREDIT TOTAL (BNPL):" else "TOTAL AMOUNT PAID:"
        sb.appendLine(leftRightAlign(typeLabel, "GH₵" + String.format(Locale.getDefault(), "%,.2f", sale.amount), width))
        
        if (sale.isCredit) {
            val statusStr = if (sale.creditPaid) "PAID/SETTLED" else "UNPAID/CREDIT ARREARS"
            sb.appendLine(leftRightAlign("CREDIT STATUS:", statusStr, width))
        }
        
        sb.appendLine(customLine('=', width))

        // 6. Centered Footer Note
        sb.appendLine(centerText(storeFooter, width))
        sb.appendLine(centerText("Goods once sold can only be", width))
        sb.appendLine(centerText("exchanged within 7 business days", width))
        sb.appendLine(centerText("accompanied by this receipt.", width))
        sb.appendLine(customLine('-', width))
        sb.appendLine(centerText("Powered by Gh POS Solutions", width))
        sb.appendLine()
        sb.appendLine()

        return sb.toString()
    }

    /**
     * Standard visual helper utilities
     */
    fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.substring(0, width)
        val spaces = (width - text.length) / 2
        return " ".repeat(spaces) + text + " ".repeat(width - text.length - spaces)
    }

    fun leftRightAlign(left: String, right: String, width: Int): String {
        val totalLength = left.length + right.length
        if (totalLength >= width) {
            val truncatedLeft = if (left.length > (width - right.length - 2)) {
                left.substring(0, (width - right.length - 2).coerceAtLeast(4)) + ".."
            } else {
                left
            }
            val spacing = (width - truncatedLeft.length - right.length).coerceAtLeast(1)
            return truncatedLeft + " ".repeat(spacing) + right
        }
        val spaces = width - totalLength
        return left + " ".repeat(spaces) + right
    }

    fun formatThreeColumns(col1: String, col2: String, col3: String, width: Int): String {
        // Col1 (Item): 18 chars padding, Col2 (Qty): 5 chars padding, Col3 (Price): 9 chars
        val col1Width = (width * 0.55).toInt() // 17 for width 32
        val col3Width = (width * 0.30).toInt() // 9 for width 32
        val col2Width = width - col1Width - col3Width // 6 for width 32

        val c1 = if (col1.length > col1Width) col1.substring(0, col1Width - 2) + ".." else col1.padEnd(col1Width)
        val c2 = if (col2.length > col2Width) col2.substring(0, col2Width) else col2.padStart(col2Width)
        val c3 = if (col3.length > col3Width) col3.substring(0, col3Width) else col3.padStart(col3Width)
        
        return c1 + c2 + c3
    }

    fun customLine(char: Char, width: Int): String {
        return char.toString().repeat(width)
    }

    private fun splitStringInChunks(str: String, chunkSize: Int): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < str.length) {
            val end = (i + chunkSize).coerceAtMost(str.length)
            list.add(str.substring(i, end))
            i += chunkSize
        }
        return list
    }

    /**
     * Native Android print spooler system. Builds an HTML visual receipt representation,
     * and sends it straight to the OS system print service context.
     * Compatible with virtual PDF, Bluetooth, Wi-Fi, and native system printers.
     */
    fun printNativeSystemsReceipt(
        context: Context,
        sale: Sale,
        cartItems: List<Pair<Product, Int>>?,
        discountAmount: Double = 0.0,
        taxAmount: Double = 0.0
    ) {
        val sessionManager = com.example.data.local.SessionManager(context)
        val sName = sessionManager.storeName
        val sPhone = sessionManager.storePhone
        val sLoc = sessionManager.storeLocation
        val sFooter = sessionManager.storeFooter
        val sTaxId = sessionManager.storeTaxId

        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "System Printing services not available on this device.", Toast.LENGTH_LONG).show()
                return
            }

            // HTML receipt string
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = format.format(Date(sale.timestamp))
            
            // Build item list HTML
            val itemsHtml = StringBuilder()
            if (!cartItems.isNullOrEmpty()) {
                cartItems.forEach { (product, quantity) ->
                    val lineTotal = product.price * quantity
                    itemsHtml.append("""
                        <tr>
                            <td class="left">${product.name} (x$quantity)</td>
                            <td class="right">GH₵${String.format(Locale.getDefault(), "%.2f", lineTotal)}</td>
                        </tr>
                    """.trimIndent())
                }
            } else {
                val desc = sale.description
                if (desc.startsWith("Products: ")) {
                    val mainPart = desc.removePrefix("Products: ").split("|").firstOrNull()?.trim() ?: ""
                    val items = mainPart.split(",")
                    items.forEach { itemText ->
                        val cleanItem = itemText.trim()
                        if (cleanItem.isNotEmpty()) {
                            itemsHtml.append("""
                                <tr>
                                    <td class="left">$cleanItem</td>
                                    <td class="right">-</td>
                                </tr>
                            """.trimIndent())
                        }
                    }
                } else {
                    itemsHtml.append("""
                        <tr>
                            <td class="left">${sale.description}</td>
                            <td class="right">-</td>
                        </tr>
                    """.trimIndent())
                }
            }

            val typeText = if (sale.isCredit) "CREDIT TOTAL (BNPL)" else "TOTAL CASH COMPLETED"
            val calculatedSubtotal = sale.amount + discountAmount - taxAmount
            val subtotalToPrint = if (calculatedSubtotal > 0) calculatedSubtotal else sale.amount

            val htmlContent = """
                <html>
                <head>
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 14px;
                        color: #000;
                        background: #fff;
                        padding: 10px;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 12px;
                    }
                    .header h2 {
                        margin: 0;
                        font-size: 18px;
                    }
                    .header p {
                        margin: 2px 0;
                        font-size: 12px;
                    }
                    .divider {
                        border-top: 1px dashed #000;
                        margin: 8px 0;
                    }
                    .meta-table, .items-table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 13px;
                    }
                    .meta-table td, .items-table td {
                        padding: 3px 0;
                    }
                    .left {
                        text-align: left;
                    }
                    .right {
                        text-align: right;
                    }
                    .bold {
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 18px;
                        font-size: 11px;
                    }
                </style>
                </head>
                <body>
                    <div class="header">
                        <h2>$sName</h2>
                        <p>$sLoc</p>
                        ${if (sPhone.isNotBlank()) "<p>Tel: $sPhone</p>" else ""}
                        ${if (sTaxId.isNotBlank()) "<p>TIN/TAX ID: $sTaxId</p>" else ""}
                    </div>
                    
                    <div class="divider"></div>
                    
                    <table class="meta-table">
                        <tr><td class="left">DATE:</td><td class="right">$dateStr</td></tr>
                        <tr><td class="left">INVOICE ID:</td><td class="right">#S-${sale.id}</td></tr>
                        <tr><td class="left">CASHIER:</td><td class="right">GENERAL_STAFF</td></tr>
                        ${if (sale.customerName.isNotBlank() && !sale.customerName.equals("Walk-in Customer", ignoreCase = true)) "<tr><td class='left'>CUSTOMER:</td><td class='right'>${sale.customerName}</td></tr>" else ""}
                    </table>
                    
                    <div class="divider"></div>
                    
                    <table class="items-table">
                        <thead>
                            <tr>
                                <th class="left">ITEM</th>
                                <th class="right">TOTAL</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>
                    
                    <div class="divider"></div>
                    
                    <table class="meta-table">
                        <tr><td class="left">SUBTOTAL:</td><td class="right">GH₵${String.format(Locale.getDefault(), "%,.2f", subtotalToPrint)}</td></tr>
                        ${if (discountAmount > 0) "<tr><td class='left'>DISCOUNT:</td><td class='right'>-GH₵${String.format(Locale.getDefault(), "%,.2f", discountAmount)}</td></tr>" else ""}
                        ${if (taxAmount > 0) "<tr><td class='left'>TAX VAT:</td><td class='right'>GH₵${String.format(Locale.getDefault(), "%,.2f", taxAmount)}</td></tr>" else ""}
                        <tr class="bold"><td class="left">$typeText:</td><td class="right">GH₵${String.format(Locale.getDefault(), "%,.2f", sale.amount)}</td></tr>
                    </table>
                    
                    <div class="divider"></div>
                    
                    <div class="footer">
                        <p class="bold">$sFooter</p>
                        <p>Goods once sold can only be exchanged within 7 business days accompanied by this receipt.</p>
                        <p>Software Powered by Gh POS Solutions</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            // Run on standard Android webView print framework
            val webView = android.webkit.WebView(context)
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    // Get print adapter from webview
                    val printAdapter = webView.createPrintDocumentAdapter("Receipt_#S-${sale.id}")
                    val printJobName = "Receipt_Sale_#S-${sale.id}"
                    printManager.print(printJobName, printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Spooling system failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
