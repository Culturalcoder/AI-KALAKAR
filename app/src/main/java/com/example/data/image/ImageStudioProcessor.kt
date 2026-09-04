package com.example.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageStudioProcessor {

    /**
     * Loads a Bitmap safely from Uri with downsampling to prevent OutOfMemory.
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri, targetSize: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimension = maxOf(options.outWidth, options.outHeight)
            var inSampleSize = 1
            if (maxDimension > targetSize) {
                inSampleSize = (maxDimension.toFloat() / targetSize).toInt().coerceAtLeast(1)
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Performs studio lighting enhancement:
     * - Adjusts contrast, brightness, and craft warmth (+warm terracotta/amber tone calibration)
     * - Blurs background edges / creates a subtle clean studio spotlight backdrop
     * - Enhances product clarity and texture
     */
    suspend fun enhanceToStudioQuality(
        context: Context,
        rawBitmap: Bitmap,
        warmthLevel: Float = 1.12f,
        contrastLevel: Float = 1.25f,
        brightnessBoost: Float = 15f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = rawBitmap.width
        val height = rawBitmap.height

        // 1. Color and Contrast Enhancement matrix
        val colorMatrix = ColorMatrix()

        // Contrast scale
        val scale = contrastLevel
        val translate = (1f - scale) * 128f + brightnessBoost

        // Color temperature adjustment: subtly enrich warm tones (red & amber channels)
        val rScale = scale * warmthLevel
        val gScale = scale * 1.03f
        val bScale = scale * 0.96f

        colorMatrix.set(
            floatArrayOf(
                rScale, 0f, 0f, 0f, translate,
                0f, gScale, 0f, 0f, translate,
                0f, 0f, bScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(rawBitmap, 0f, 0f, paint)

        // 2. Soft Studio Vignette Lighting Overlay (Radial gradient to draw focus to center)
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = maxOf(width, height) * 0.75f
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    Color.argb(0, 255, 255, 255),
                    Color.argb(15, 245, 239, 230),
                    Color.argb(55, 42, 38, 34)
                ),
                floatArrayOf(0.4f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)

        // 3. Subtle E-Commerce Border / Studio Rim reflection
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(40, 196, 98, 45) // Warm Terracotta tint
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        enhancedBitmap
    }

    /**
     * Saves a Bitmap to the app's internal files directory and returns a persistent file URI.
     */
    suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val filename = "${prefix}_${System.currentTimeMillis()}.jpg"
        val directory = File(context.filesDir, "artisan_products")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file).toString()
    }

    /**
     * Generates a sample authentic artisan craft image bitmap if user wants to try out the app
     * immediately (traditional terracotta pot with turmeric flowers motif drawn cleanly via Canvas).
     */
    fun createSampleArtisanBitmap(): Bitmap {
        val size = 720
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Warm craft workshop background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#F4ECE1"),
                Color.parseColor("#EADCC9"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Wooden workbench surface
        val tablePaint = Paint().apply {
            color = Color.parseColor("#7A4D2B")
        }
        canvas.drawRect(0f, size * 0.65f, size.toFloat(), size.toFloat(), tablePaint)

        // Bench woodgrain highlight
        val tableHighlight = Paint().apply {
            color = Color.parseColor("#8C5B36")
        }
        canvas.drawRect(0f, size * 0.65f, size.toFloat(), size * 0.68f, tableHighlight)

        // Clay pot body (Handcrafted Terracotta Matka/Vessel)
        val shadowPaint = Paint().apply {
            color = Color.parseColor("#331F1420")
        }
        canvas.drawOval(RectF(size * 0.20f, size * 0.62f, size * 0.80f, size * 0.72f), shadowPaint)

        val potPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.45f, size * 0.42f, size * 0.35f,
                intArrayOf(
                    Color.parseColor("#E6783D"), // Terracotta highlight
                    Color.parseColor("#C4622D"), // Base Terracotta
                    Color.parseColor("#8B3D15")  // Shadow Terracotta
                ),
                floatArrayOf(0f, 0.6f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(size * 0.22f, size * 0.28f, size * 0.78f, size * 0.66f), potPaint)

        // Pot neck and rim
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9C4719")
        }
        canvas.drawOval(RectF(size * 0.36f, size * 0.22f, size * 0.64f, size * 0.30f), rimPaint)

        val rimInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A250B")
        }
        canvas.drawOval(RectF(size * 0.40f, size * 0.24f, size * 0.60f, size * 0.28f), rimInner)

        // Traditional hand-painted white & turmeric geometric artisan patterns on the vessel
        val tribalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8EE")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawArc(RectF(size * 0.25f, size * 0.40f, size * 0.75f, size * 0.50f), 10f, 160f, false, tribalPaint)

        val goldMotifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8A33D")
            style = Paint.Style.FILL
        }
        // Traditional dots pattern
        for (i in 0..6) {
            val cx = size * (0.32f + i * 0.06f)
            val cy = size * 0.47f
            canvas.drawCircle(cx, cy, 7f, goldMotifPaint)
        }

        return bitmap
    }
}
