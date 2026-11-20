package com.example.usbqrscanner

import com.google.mlkit.vision.barcode.common.Barcode

data class QrScanResult(
    val rawValue: String,
    val typeLabel: String,
    val displayText: String,
    val urlToOpen: String? = null
)

object ScanResultFormatter {

    fun format(barcode: Barcode): QrScanResult? {
        val raw = barcode.rawValue?.trim().orEmpty()
        if (raw.isEmpty()) return null

        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url?.url ?: raw
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "URL",
                    displayText = url,
                    urlToOpen = normalizeUrl(url)
                )
            }

            Barcode.TYPE_WIFI -> {
                val ssid = barcode.wifi?.ssid.orEmpty()
                val password = barcode.wifi?.password.orEmpty()
                val text = buildString {
                    append("SSID: $ssid")
                    if (password.isNotEmpty()) append("\nPassword: $password")
                }
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "WI-FI",
                    displayText = text
                )
            }

            Barcode.TYPE_EMAIL -> {
                val address = barcode.email?.address ?: raw
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "EMAIL",
                    displayText = address
                )
            }

            Barcode.TYPE_PHONE -> {
                val number = barcode.phone?.number ?: raw
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "PHONE",
                    displayText = number
                )
            }

            Barcode.TYPE_SMS -> {
                val phone = barcode.sms?.phoneNumber.orEmpty()
                val message = barcode.sms?.message.orEmpty()
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "SMS",
                    displayText = "$phone: $message".trim(':', ' ')
                )
            }

            Barcode.TYPE_GEO -> {
                val lat = barcode.geoPoint?.lat
                val lng = barcode.geoPoint?.lng
                QrScanResult(
                    rawValue = raw,
                    typeLabel = "LOCATION",
                    displayText = if (lat != null && lng != null) "$lat, $lng" else raw
                )
            }

            else -> {
                val maybeUrl = if (raw.startsWith("http://", ignoreCase = true) ||
                    raw.startsWith("https://", ignoreCase = true)
                ) {
                    raw
                } else {
                    null
                }
                QrScanResult(
                    rawValue = raw,
                    typeLabel = if (maybeUrl != null) "URL" else "QR TEXT",
                    displayText = raw,
                    urlToOpen = maybeUrl
                )
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
        ) {
            url
        } else {
            "https://$url"
        }
    }
}
