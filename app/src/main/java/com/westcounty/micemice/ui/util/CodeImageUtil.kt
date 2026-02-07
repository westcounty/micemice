package com.westcounty.micemice.ui.util

import android.content.Context
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale

fun generateQrCodeBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
        put(EncodeHintType.MARGIN, 1)
    }
    val bitMatrix = runCatching {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    }.getOrNull() ?: return null

    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bmp
}

fun writeBitmapPngToAppFiles(context: Context, prefix: String, bitmap: Bitmap): Result<String> {
    return runCatching {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "${prefix}_${formatter.format(Date())}.png"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, name)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file.absolutePath
    }
}
