package com.corner.myshoppinglist.util

import com.corner.myshoppinglist.data.model.ScannedItem
import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object ReceiptParser {

    fun parse(visionText: Text): List<ScannedItem> {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return emptyList()

        // 1. Spatial Grouping: Group lines that are on the same vertical level
        val rows = groupLinesIntoRows(allLines)
        
        return parseRows(rows)
    }

    fun parseRows(rows: List<String>): List<ScannedItem> {
        // 2. Filter rows to find the "Items Section"
        val itemRows = extractItemSection(rows)
        
        val items = mutableListOf<ScannedItem>()
        var i = 0
        while (i < itemRows.size) {
            val currentRow = itemRows[i]
            
            // Heuristic A: Look for Name on one line, and Qty info on the next
            if (i + 1 < itemRows.size) {
                val nextRow = itemRows[i + 1]
                if (isQuantityLine(nextRow)) {
                    val scanned = parseQuantityLine(currentRow, nextRow)
                    if (scanned != null) {
                        items.add(scanned)
                        i += 2
                        continue
                    }
                }
            }
            
            // Heuristic B: Single line item
            val scanned = parseSingleLineItem(currentRow)
            if (scanned != null) {
                items.add(scanned)
            }
            
            i++
        }
        
        return items
    }

    private fun groupLinesIntoRows(lines: List<Text.Line>): List<String> {
        // Sort lines by their top coordinate
        val sortedLines = lines.sortedBy { it.boundingBox?.top ?: 0 }
        val rows = mutableListOf<MutableList<Text.Line>>()
        
        for (line in sortedLines) {
            val center = (line.boundingBox?.top ?: 0) + (line.boundingBox?.height() ?: 0) / 2
            
            // Find a row where this line fits vertically
            val existingRow = rows.find { row ->
                val rowCenter = row.map { (it.boundingBox?.top ?: 0) + (it.boundingBox?.height() ?: 0) / 2 }.average()
                val rowHeight = row.map { it.boundingBox?.height() ?: 0 }.average()
                abs(center - rowCenter) < rowHeight * 0.5 // Tolerance: half height of the row
            }
            
            if (existingRow != null) {
                existingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }
        
        // Convert groups to sorted strings (left-to-right)
        return rows.map { row ->
            row.sortedBy { it.boundingBox?.left ?: 0 }
               .joinToString(" ") { it.text }
               .trim()
        }
    }

    private fun extractItemSection(rows: List<String>): List<String> {
        var startIndex = 0
        var stopIndex = rows.size
        
        for ((index, row) in rows.withIndex()) {
            val lower = row.lowercase()
            if (lower.contains("description") || lower.contains("amount") || lower.contains("item name")) {
                startIndex = index + 1
            }
            if (lower.startsWith("total") || lower.contains("cash") || lower.contains("rounding") || lower.contains("number of items")) {
                stopIndex = index
                break
            }
        }
        
        if (startIndex >= stopIndex) return rows.filter { !isMetadata(it) }
        
        return rows.subList(startIndex, stopIndex).filter { it.isNotBlank() && !isMetadata(it) }
    }

    private fun isQuantityLine(row: String): Boolean {
        // Starts with numbers, optional units, and may contain @ or prices
        return row.matches(Regex("""^(\d+[.,\s]?\d*)\s*(pc|kg|pck\d+|g|l|ml|unit)?.*""", RegexOption.IGNORE_CASE))
    }

    private fun parseQuantityLine(nameRow: String, qtyRow: String): ScannedItem? {
        val prefixRegex = Regex("""^(\d+[.,\s]?\d*)\s*(pc|kg|pck\d+|g|l|ml|unit)?\s*[@a©]?""", RegexOption.IGNORE_CASE)
        val match = prefixRegex.find(qtyRow) ?: return null
        
        val quantity = parseNumber(match.groupValues[1]) ?: 1.0
        val unit = match.groupValues[2].takeIf { it.isNotBlank() }
        
        val numbers = extractNumbers(qtyRow)
        // Skip the quantity if it's the first number
        val prices = if (numbers.isNotEmpty() && abs(numbers[0] - quantity) < 0.01) numbers.drop(1) else numbers
        
        if (prices.isNotEmpty()) {
            return ScannedItem(
                name = nameRow,
                quantity = quantity,
                unit = unit,
                unitPrice = prices[0],
                totalPrice = if (prices.size >= 2) prices[1] else prices[0],
            )
        }
        return null
    }

    private fun parseSingleLineItem(row: String): ScannedItem? {
        val numbers = extractNumbers(row)
        if (numbers.isEmpty()) return null
        
        val lastPrice = numbers.last()
        // Handle VAT markers at end (0, 1, *)
        val effectivePrice = if (lastPrice < 5.0 && numbers.size >= 2) numbers[numbers.size - 2] else lastPrice
        
        if (effectivePrice < 100.0) return null // Too small to be a primary item price in this context

        // Get the name by taking everything before the first number that contributes to the price
        val firstPriceStr = findFirstPriceLikeString(row) ?: return null
        val namePart = row.substring(0, row.indexOf(firstPriceStr)).trim()
        
        // Clean up name from units at the end
        val unitRegex = Regex("""\s+(pc|kg|pck\d+|g|l|ml)\s*$""", RegexOption.IGNORE_CASE)
        val unitMatch = unitRegex.find(namePart)
        val name = if (unitMatch != null) namePart.substring(0, unitMatch.range.first).trim() else namePart
        val unit = unitMatch?.groupValues?.get(1)

        if (name.length > 2 && !isMetadata(name)) {
            return ScannedItem(
                name = name,
                unit = unit,
                totalPrice = effectivePrice,
                unitPrice = effectivePrice,
            )
        }
        return null
    }

    private fun findFirstPriceLikeString(row: String): String? {
        val regex = Regex("""[\d,.\s]+[\.,]\d{2}|[\d,.\s]{5,}""")
        return regex.find(row)?.value
    }

    private fun extractNumbers(text: String): List<Double> {
        val parts = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val numbers = mutableListOf<Double>()
        var i = 0
        while (i < parts.size) {
            var current = parts[i]
            if (i + 1 < parts.size && current.matches(Regex("""\d{1,3}""")) && parts[i + 1].matches(Regex("""\d{3}([\.,]\d{2})?"""))) {
                current += parts[i + 1]
                i++
            }
            parseNumber(current)?.let { numbers.add(it) }
            i++
        }
        return numbers
    }

    private fun parseNumber(text: String): Double? {
        val cleaned = text.replace(Regex("""[^\d.,]"""), "")
        if (cleaned.isEmpty()) return null
        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')
        val lastSep = maxOf(lastDot, lastComma)
        return if (lastSep != -1 && cleaned.length - lastSep <= 3) {
            val integerPart = cleaned.substring(0, lastSep).replace(Regex("""[.,]"""), "")
            val decimalPart = cleaned.substring(lastSep + 1)
            "$integerPart.$decimalPart".toDoubleOrNull()
        } else {
            cleaned.replace(Regex("""[.,]"""), "").toDoubleOrNull()
        }
    }

    private fun isMetadata(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("total") || lower.contains("cash") || lower.contains("rounding") || 
               lower.contains("exchange") || lower.contains("vat") || lower.contains("phone") || 
               lower.contains("slip") || lower.contains("staff") || lower.contains("date") || 
               lower.contains("trans") || lower.contains("subtotal") || lower.contains("items") || 
               lower.contains("points") || lower.contains("account") || lower.contains("member") || 
               lower.contains("facebook") || lower.contains("webpage") || lower.contains("net.amt")
    }
}
