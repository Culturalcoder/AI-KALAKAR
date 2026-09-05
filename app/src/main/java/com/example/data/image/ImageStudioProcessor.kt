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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

enum class StudioBackdrop(val id: String, val titleHi: String, val titleEn: String, val colorHex: Long) {
    WHITE_STUDIO("white", "सफेद स्टूडियो", "Clean White", 0xFFFFFFFF),
    WARM_LINEN("linen", "हस्तशिल्प लिनन", "Warm Linen", 0xFFF9F5EE),
    FESTIVE_AMBER("amber", "उत्सव अंबर", "Festive Amber", 0xFFFFF3E0),
    LUXURY_SLATE("slate", "रॉयल डार्क", "Luxury Dark", 0xFF1E2129),
    TRANSPARENT("transparent", "पारदर्शी (PNG)", "Transparent", 0x00000000),
    ORIGINAL_ENHANCED("original", "मूल निखारा", "Original Enhanced", 0xFF8D6E63)
}

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
     * Removes background from product image and places it on a professional studio backdrop.
     * Features:
     * - Automatic multi-zone background color & border sampling
     * - Saliency and center-weighted craft object segmentation
     * - Smooth alpha edge feathering (removes green/gray fringe artifacts)
     * - Realistic studio contact shadow generation
     * - Selectable studio backdrops (Seamless White, Warm Linen, Festive Amber, Luxury Slate, Transparent PNG)
     * - Color, warmth, and micro-contrast calibration for Indian crafts
     */
    suspend fun enhanceToStudioQuality(
        context: Context,
        rawBitmap: Bitmap,
        backdrop: StudioBackdrop = StudioBackdrop.WHITE_STUDIO,
        removeBackground: Boolean = true,
        sensitivity: Float = 0.5f,
        warmthLevel: Float = 1.08f,
        contrastLevel: Float = 1.20f,
        brightnessBoost: Float = 10f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = rawBitmap.width
        val height = rawBitmap.height

        // If user wants original background with enhanced studio lighting:
        if (!removeBackground || backdrop == StudioBackdrop.ORIGINAL_ENHANCED) {
            return@withContext enhanceWithOriginalBackground(
                rawBitmap, warmthLevel, contrastLevel, brightnessBoost
            )
        }

        // 1. Generate Intelligent Foreground Alpha Cutout
        val cutoutBitmap = generateForegroundCutout(rawBitmap, sensitivity)

        // 2. Composite Foreground onto Selected Studio Backdrop
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // A. Render Backdrop
        when (backdrop) {
            StudioBackdrop.TRANSPARENT -> {
                // Clear transparent canvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            StudioBackdrop.WHITE_STUDIO -> {
                // E-commerce Pure Clean White with subtle studio spotlight
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.45f, max(width, height) * 0.7f,
                        intArrayOf(
                            Color.parseColor("#FFFFFF"),
                            Color.parseColor("#FDFDFE"),
                            Color.parseColor("#F2F4F7")
                        ),
                        floatArrayOf(0f, 0.6f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            StudioBackdrop.WARM_LINEN -> {
                // Indian Craft Warm Linen Studio Canvas
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.45f, max(width, height) * 0.75f,
                        intArrayOf(
                            Color.parseColor("#FCF9F3"),
                            Color.parseColor("#F4ECE1"),
                            Color.parseColor("#E8DCCB")
                        ),
                        floatArrayOf(0f, 0.55f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            StudioBackdrop.FESTIVE_AMBER -> {
                // Warm Terracotta & Amber Festival Spotlight
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.45f, max(width, height) * 0.75f,
                        intArrayOf(
                            Color.parseColor("#FFFBF0"),
                            Color.parseColor("#FFF0D4"),
                            Color.parseColor("#FAD8A5")
                        ),
                        floatArrayOf(0f, 0.5f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            StudioBackdrop.LUXURY_SLATE -> {
                // Luxury Dark Charcoal Gallery Stage with subtle golden spotlight
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.40f, max(width, height) * 0.7f,
                        intArrayOf(
                            Color.parseColor("#2C2D35"),
                            Color.parseColor("#1C1D24"),
                            Color.parseColor("#121318")
                        ),
                        floatArrayOf(0f, 0.55f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            else -> {
                canvas.drawColor(Color.WHITE)
            }
        }

        // B. Render Realistic Ground Drop Shadow (if not transparent)
        if (backdrop != StudioBackdrop.TRANSPARENT) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val shadowColor = if (backdrop == StudioBackdrop.LUXURY_SLATE) {
                    Color.argb(120, 0, 0, 0)
                } else {
                    Color.argb(45, 60, 45, 35)
                }
                shader = RadialGradient(
                    width * 0.5f, height * 0.82f, width * 0.38f,
                    intArrayOf(
                        shadowColor,
                        Color.argb(Color.alpha(shadowColor) / 2, 60, 45, 35),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.55f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawOval(
                RectF(width * 0.15f, height * 0.76f, width * 0.85f, height * 0.88f),
                shadowPaint
            )
        }

        // C. Render Enhanced Foreground Cutout onto the Canvas
        val productPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(cutoutBitmap, 0f, 0f, productPaint)

        // D. Clean Studio Border Frame (Subtle)
        if (backdrop != StudioBackdrop.TRANSPARENT) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = if (backdrop == StudioBackdrop.LUXURY_SLATE) {
                    Color.argb(30, 255, 215, 0) // Gold accent
                } else {
                    Color.argb(25, 196, 98, 45) // Terracotta accent
                }
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        }

        outputBitmap
    }

    /**
     * High-precision foreground object segmentation and background removal.
     */
    private fun generateForegroundCutout(rawBitmap: Bitmap, sensitivity: Float): Bitmap {
        val width = rawBitmap.width
        val height = rawBitmap.height

        // Downsampled working image for fast segmentation
        val targetDim = 512
        val scale = min(1.0f, targetDim.toFloat() / max(width, height))
        val workW = (width * scale).toInt().coerceAtLeast(64)
        val workH = (height * scale).toInt().coerceAtLeast(64)

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(rawBitmap, workW, workH, true)
        } else {
            rawBitmap
        }

        val pixels = IntArray(workW * workH)
        scaledBitmap.getPixels(pixels, 0, workW, 0, 0, workW, workH)

        // 1. Sample Background Colors from the Outer Perimeter (Borders & 4 Corners)
        val bgSamples = mutableListOf<Int>()
        val borderThicknessX = max(2, (workW * 0.08f).toInt())
        val borderThicknessY = max(2, (workH * 0.08f).toInt())

        // Top and Bottom borders
        for (x in 0 until workW step 3) {
            for (y in 0 until borderThicknessY step 2) {
                bgSamples.add(pixels[y * workW + x])
            }
            for (y in (workH - borderThicknessY) until workH step 2) {
                bgSamples.add(pixels[y * workW + x])
            }
        }
        // Left and Right borders
        for (y in 0 until workH step 3) {
            for (x in 0 until borderThicknessX step 2) {
                bgSamples.add(pixels[y * workW + x])
            }
            for (x in (workW - borderThicknessX) until workW step 2) {
                bgSamples.add(pixels[y * workW + x])
            }
        }

        // Calculate average background color in RGB space
        var sumR = 0L; var sumG = 0L; var sumB = 0L
        for (c in bgSamples) {
            sumR += Color.red(c)
            sumG += Color.green(c)
            sumB += Color.blue(c)
        }
        val count = bgSamples.size.coerceAtLeast(1)
        val avgBgR = (sumR / count).toFloat()
        val avgBgG = (sumG / count).toFloat()
        val avgBgB = (sumB / count).toFloat()

        // 2. Compute Distance & Saliency Mask
        val mask = FloatArray(workW * workH)
        val centerX = workW * 0.5f
        val centerY = workH * 0.52f
        val maxRadiusSq = (workW * 0.48f).pow(2) + (workH * 0.48f).pow(2)

        // Threshold base influenced by sensitivity (0.0 = conservative, 1.0 = aggressive removal)
        val baseThreshold = (35f + sensitivity * 45f)

        for (y in 0 until workH) {
            for (x in 0 until workW) {
                val idx = y * workW + x
                val p = pixels[idx]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                // Color difference to background model
                val diffR = r - avgBgR
                val diffG = g - avgBgG
                val diffB = b - avgBgB
                val colorDist = sqrt(diffR * diffR + diffG * diffG + diffB * diffB)

                // Spatial center weighting (craft is centered, borders are background)
                val dx = (x - centerX)
                val dy = (y - centerY)
                val distFromCenterSq = dx * dx + dy * dy
                val centerWeight = 1.0f - (distFromCenterSq / maxRadiusSq).coerceIn(0f, 1f)

                // Combined foreground probability
                val effectiveThreshold = baseThreshold * (1.2f - centerWeight * 0.6f)
                val alphaVal = when {
                    colorDist > effectiveThreshold * 1.3f -> 1.0f
                    colorDist < effectiveThreshold * 0.7f -> 0.0f
                    else -> ((colorDist - effectiveThreshold * 0.7f) / (effectiveThreshold * 0.6f)).coerceIn(0f, 1f)
                }

                mask[idx] = alphaVal
            }
        }

        // 3. Smooth Mask with 3x3 Box Blur for Anti-Aliased Edges
        val smoothMask = FloatArray(workW * workH)
        for (y in 1 until workH - 1) {
            for (x in 1 until workW - 1) {
                var sum = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        sum += mask[(y + dy) * workW + (x + dx)]
                    }
                }
                smoothMask[y * workW + x] = sum / 9f
            }
        }

        // 4. Apply Mask & Color Enhancement to Full Resolution Bitmap
        val fullPixels = IntArray(width * height)
        rawBitmap.getPixels(fullPixels, 0, width, 0, 0, width, height)

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        val scaleX = workW.toFloat() / width
        val scaleY = workH.toFloat() / height

        for (y in 0 until height) {
            val sy = (y * scaleY).toInt().coerceIn(0, workH - 1)
            for (x in 0 until width) {
                val sx = (x * scaleX).toInt().coerceIn(0, workW - 1)
                val p = fullPixels[y * width + x]
                val origA = Color.alpha(p)
                val origR = Color.red(p)
                val origG = Color.green(p)
                val origB = Color.blue(p)

                val maskA = smoothMask[sy * workW + sx]
                val finalAlpha = (origA * maskA).toInt().coerceIn(0, 255)

                if (finalAlpha == 0) {
                    outPixels[y * width + x] = 0
                } else {
                    // Subtle enhancement to product pixels: boost vibrancy and clarity
                    val enhR = (origR * 1.04f + 4f).toInt().coerceIn(0, 255)
                    val enhG = (origG * 1.02f + 2f).toInt().coerceIn(0, 255)
                    val enhB = (origB * 0.98f).toInt().coerceIn(0, 255)
                    outPixels[y * width + x] = Color.argb(finalAlpha, enhR, enhG, enhB)
                }
            }
        }

        resultBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Fallback studio lighting enhancement keeping the original background.
     */
    private fun enhanceWithOriginalBackground(
        rawBitmap: Bitmap,
        warmthLevel: Float,
        contrastLevel: Float,
        brightnessBoost: Float
    ): Bitmap {
        val width = rawBitmap.width
        val height = rawBitmap.height

        val colorMatrix = ColorMatrix()
        val scale = contrastLevel
        val translate = (1f - scale) * 128f + brightnessBoost

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

        // Soft Studio Vignette Lighting Overlay
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

        return enhancedBitmap
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

        // Background: Craft workshop floor texture with lighting
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#EFE5D8"),
                Color.parseColor("#DFD2C0"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Wooden workshop table surface
        val tablePaint = Paint().apply {
            color = Color.parseColor("#6D4021")
        }
        canvas.drawRect(0f, size * 0.65f, size.toFloat(), size.toFloat(), tablePaint)

        // Table highlight line
        val tableHighlight = Paint().apply {
            color = Color.parseColor("#804D29")
        }
        canvas.drawRect(0f, size * 0.65f, size.toFloat(), size * 0.67f, tableHighlight)

        // Contact shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#441A0E1A")
        }
        canvas.drawOval(RectF(size * 0.18f, size * 0.62f, size * 0.82f, size * 0.73f), shadowPaint)

        // Clay pot body
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

        // Traditional hand-painted white & turmeric geometric artisan patterns
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
        for (i in 0..6) {
            val cx = size * (0.32f + i * 0.06f)
            val cy = size * 0.47f
            canvas.drawCircle(cx, cy, 7f, goldMotifPaint)
        }

        return bitmap
    }
}

