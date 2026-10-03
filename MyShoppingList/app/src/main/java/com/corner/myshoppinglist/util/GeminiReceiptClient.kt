package com.corner.myshoppinglist.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.corner.myshoppinglist.data.model.ScannedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiException(
    message: String,
    val httpCode: Int? = null,
    val retryable: Boolean = false
) : Exception(message)

/**
 * Sends a receipt photo to the Gemini API (free tier) and gets back structured line items.
 * Uses plain REST + kotlinx.serialization, so no new dependencies are needed.
 *
 * If Google renames/retires the model, change MODEL below. Check AI Studio for current names.
 */
object GeminiReceiptClient {

    private const val MODEL = "gemini-3.5-flash-lite"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    // Receipts are tall and narrow, so keep the longest side large enough for small print.
    private const val MAX_SIDE_PX = 2560
    private const val JPEG_QUALITY = 85

    // If Gemini doesn't answer in time, we throw and the ViewModel falls back to the local scanner.
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 20_000

    // A receipt is about 60-70 output tokens per item. This stops a runaway response early.
    private const val MAX_OUTPUT_TOKENS = 4096
    private const val MAX_ATTEMPTS = 2

    private val PROMPT = """
        This is a photo of a shop receipt. It may be in Arabic, English, or both, and may be laid out right-to-left.
        Extract ONLY the purchased line items.
        Rules:
        - Ignore store name, address, phone, date, cashier, totals, subtotals, VAT/tax lines, cash, change, rounding, loyalty points and payment info.
        - Keep each product name exactly as printed, in its original script. Do not translate it.
        - Convert any Arabic-Indic digits to Western digits (0-9).
        - All prices must be plain numbers: no currency symbols and no thousands separators (e.g. 150000 or 2.5).
        - quantity is how many units/weight were bought; use 1 if the receipt does not show it.
        - unit is the unit (pc, kg, g, l, ml) only if printed, otherwise null.
        - totalPrice is the line total printed for that item (after quantity).
        - unitPrice is the price of one unit if printed, otherwise null.
        - Use at most 2 decimal places for every price. Never output long strings of digits.
        - Every item must include totalPrice.
        - If a value is not readable, use null instead of guessing.
        Return JSON only.
    """.trimIndent()

    private val RESPONSE_SCHEMA = Json.parseToJsonElement(
        """
        {
          "type": "OBJECT",
          "properties": {
            "items": {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "name": {"type": "STRING"},
                  "quantity": {"type": "NUMBER"},
                  "unit": {"type": "STRING", "nullable": true},
                  "unitPrice": {"type": "NUMBER", "nullable": true},
                  "totalPrice": {"type": "NUMBER", "nullable": true}
                },
                "propertyOrdering": ["name", "quantity", "unit", "unitPrice", "totalPrice"],
                "required": ["name", "quantity", "totalPrice"]
              }
            }
          },
          "required": ["items"]
        }
        """.trimIndent()
    )

    suspend fun parseReceipt(context: Context, uri: Uri, apiKey: String): List<ScannedItem> =
        withContext(Dispatchers.IO) {
            val imageBase64 = loadAsBase64Jpeg(context, uri)

            var lastError: GeminiException? = null
            for (attempt in 1..MAX_ATTEMPTS) {
                try {
                    return@withContext parseResponse(callGemini(imageBase64, apiKey))
                } catch (e: GeminiException) {
                    if (!e.retryable) throw e
                    Log.w("GeminiReceipt", "Attempt $attempt/$MAX_ATTEMPTS failed: ${e.message}")
                    lastError = e
                }
            }
            throw lastError ?: GeminiException("Gemini failed")
        }

    // ---------- image prep ----------

    private fun loadAsBase64Jpeg(context: Context, uri: Uri): String {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream() returns null when inJustDecodeBounds = true, so check the stream and the size instead
        val opened = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
            true
        } ?: false
        if (!opened || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw GeminiException("Cannot open the selected image")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX * 2) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw GeminiException("Cannot decode the selected image")

        val rotation = resolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val longest = maxOf(decoded.width, decoded.height)
        val scale = if (longest > MAX_SIDE_PX) MAX_SIDE_PX.toFloat() / longest else 1f

        val bitmap = if (rotation != 0f || scale != 1f) {
            val matrix = Matrix().apply {
                postRotate(rotation)
                postScale(scale, scale)
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else decoded

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ---------- network ----------

    private fun callGemini(imageBase64: String, apiKey: String): String {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", PROMPT) }
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", "image/jpeg")
                                put("data", imageBase64)
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                // No temperature override: very low values can make some Gemini models loop.
                put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                put("responseMimeType", "application/json")
                put("responseSchema", RESPONSE_SCHEMA)
                // Reading a receipt doesn't need reasoning. Thinking tokens are billed as output tokens.
                // If the API returns a 400 mentioning thinkingLevel, replace this line with:
                //   put("thinkingBudget", 0)
                putJsonObject("thinkingConfig") { put("thinkingLevel", "minimal") }
            }
        }

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey) // header, so the key never appears in a URL
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val reason = when (code) {
                429 -> "Gemini free limit reached, try again later"
                400, 403 -> "Gemini rejected the request (check the API key and model name)"
                else -> "Gemini error $code"
            }
            throw GeminiException("$reason: ${errorBody.take(300)}", code)
        } catch (e: java.net.SocketTimeoutException) {
            throw GeminiException("Gemini timed out, using the local scanner")
        } finally {
            conn.disconnect()
        }
    }

    // ---------- response -> ScannedItem ----------

    private fun parseResponse(raw: String): List<ScannedItem> {
        val root = Json.parseToJsonElement(raw).jsonObject

        root["usageMetadata"]?.jsonObject?.let { u ->
            Log.d(
                "GeminiReceipt",
                "tokens: prompt=${u["promptTokenCount"]} output=${u["candidatesTokenCount"]} " +
                        "thoughts=${u["thoughtsTokenCount"]} total=${u["totalTokenCount"]}"
            )
        }

        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
        val finishReason = candidate?.get("finishReason")?.jsonPrimitive?.contentOrNull

        val text = candidate
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            ?: throw GeminiException("Gemini returned no result (the image may have been blocked)")

        // Log what Gemini actually said (first 3000 chars, Logcat cuts long lines anyway).
        Log.d("GeminiReceipt", "finishReason=$finishReason, chars=${text.length}")
        text.take(3000).chunked(1000).forEachIndexed { i, part ->
            Log.d("GeminiReceipt", "raw[$i]: $part")
        }

        if (finishReason == "MAX_TOKENS") {
            throw GeminiException("Gemini output was cut off", retryable = true)
        }

        val cleaned = text.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val itemsJson = try {
            Json.parseToJsonElement(cleaned).jsonObject["items"]?.jsonArray
        } catch (e: Exception) {
            throw GeminiException("Gemini returned malformed JSON", retryable = true)
        } ?: return emptyList()

        return itemsJson.mapNotNull { element ->
            val o = element.jsonObject
            val name = o["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null

            val quantity = o["quantity"]?.jsonPrimitive?.doubleOrNull?.takeIf { it > 0 } ?: 1.0
            val unit = o["unit"]?.jsonPrimitive?.contentOrNull?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            val unitPrice = round2(o["unitPrice"]?.jsonPrimitive?.doubleOrNull)
            val totalPrice = round2(
                o["totalPrice"]?.jsonPrimitive?.doubleOrNull ?: unitPrice?.let { it * quantity }
            )

            ScannedItem(
                name = name,
                quantity = quantity,
                unit = unit,
                unitPrice = unitPrice ?: round2(totalPrice?.let { it / quantity }),
                totalPrice = totalPrice
            )
        }
    }

    // Gemini sometimes returns float noise like 680000.00000000006
    private fun round2(value: Double?): Double? = value?.let { Math.round(it * 100.0) / 100.0 }
}