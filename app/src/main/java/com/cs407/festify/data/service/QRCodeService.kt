package com.cs407.festify.data.service

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating and validating QR codes for event check-ins
 */
@Singleton
class QRCodeService @Inject constructor() {

    private val SECRET_KEY = "festify_checkin_secret_2024" // In production, use secure key storage

    /**
     * Generate QR code bitmap for event check-in
     * @param eventId The event ID to encode
     * @return Bitmap of the QR code, or null if generation fails
     */
    fun generateEventCheckInQR(eventId: String): Bitmap? {
        return try {
            // Generate QR payload with signature
            val timestamp = System.currentTimeMillis()
            val signature = generateSignature(eventId, timestamp)
            val payload = JSONObject().apply {
                put("eventId", eventId)
                put("ts", timestamp)
                put("sig", signature)
            }.toString()

            // Generate QR code using ZXing
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1
            )
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(
                payload,
                BarcodeFormat.QR_CODE,
                512, 512,
                hints
            )

            // Convert to Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Validate QR code payload and extract event ID
     * @param payload The scanned QR code data
     * @return QRValidation result
     */
    fun validateQRPayload(payload: String): QRValidation {
        return try {
            val json = JSONObject(payload)
            val eventId = json.getString("eventId")
            val timestamp = json.getLong("ts")
            val signature = json.getString("sig")

            // Validate signature
            val expectedSignature = generateSignature(eventId, timestamp)
            if (signature != expectedSignature) {
                return QRValidation.InvalidSignature
            }

            // Optional: Check timestamp expiration (5 minutes)
            // Commented out for flexibility as per user requirements
            // val currentTime = System.currentTimeMillis()
            // if (currentTime - timestamp > 300_000) { // 5 minutes
            //     return QRValidation.Expired
            // }

            QRValidation.Valid(eventId)
        } catch (e: Exception) {
            e.printStackTrace()
            QRValidation.Malformed
        }
    }

    /**
     * Generate signature for QR code security
     * Uses SHA-256 hash of eventId + timestamp + secret key
     */
    private fun generateSignature(eventId: String, timestamp: Long): String {
        val input = "$eventId$timestamp$SECRET_KEY"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * QR validation result
 */
sealed class QRValidation {
    data class Valid(val eventId: String) : QRValidation()
    object Expired : QRValidation()
    object InvalidSignature : QRValidation()
    object Malformed : QRValidation()
}
