package com.example.builder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import java.io.ByteArrayOutputStream

object IconGenerator {

    enum class Density(val folderSuffix: String, val size: Int) {
        MDPI("mdpi-v4", 48),
        HDPI("hdpi-v4", 72),
        XHDPI("xhdpi-v4", 96),
        XXHDPI("xxhdpi-v4", 144),
        XXXHDPI("xxxhdpi-v4", 192)
    }

    data class GeneratedIcons(
        val squareIcons: Map<Density, ByteArray>,
        val roundIcons: Map<Density, ByteArray>
    )

    fun generateAllMipmaps(source: Bitmap? = null, appName: String = "App"): GeneratedIcons {
        val baseBitmap = source ?: createDefaultAppIcon(appName)
        val squareMap = mutableMapOf<Density, ByteArray>()
        val roundMap = mutableMapOf<Density, ByteArray>()

        for (density in Density.values()) {
            val square = resizeBitmap(baseBitmap, density.size, density.size)
            val round = createRoundBitmap(square)

            squareMap[density] = bitmapToPngBytes(square)
            roundMap[density] = bitmapToPngBytes(round)
        }

        return GeneratedIcons(squareMap, roundMap)
    }

    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun createRoundBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        val radius = source.width.coerceAtMost(source.height) / 2f
        canvas.drawCircle(source.width / 2f, source.height / 2f, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun createDefaultAppIcon(appName: String): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#1E3A8A"),
                Color.parseColor("#3B82F6"),
                Shader.TileMode.CLAMP
            )
        }
        val cornerRadius = size * 0.22f
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, bgPaint)

        // Subtle inner ring
        val ringPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.argb(60, 255, 255, 255)
        }
        canvas.drawRoundRect(
            RectF(16f, 16f, size - 16f, size - 16f),
            cornerRadius - 8f, cornerRadius - 8f, ringPaint
        )

        // Initial letter
        val letter = appName.trim().take(1).uppercase().ifEmpty { "W" }
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = size * 0.48f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(16f, 0f, 6f, Color.argb(100, 0, 0, 0))
        }

        val textBounds = Rect()
        textPaint.getTextBounds(letter, 0, letter.length, textBounds)
        val yPos = (size / 2f) - textBounds.exactCenterY()
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bitmap
    }
}
