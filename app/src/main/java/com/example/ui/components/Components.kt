package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CharcoalMuted
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CraftBorder
import com.example.ui.theme.CraftGreen
import com.example.ui.theme.CraftGreenContainer
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.LinenCard
import com.example.ui.theme.NaturalLinen
import com.example.ui.theme.OnTerracottaContainer
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TurmericContainer
import com.example.ui.theme.TurmericGold
import kotlin.math.roundToInt

/**
 * Non-negotiable Brand Typography Wordmark Header
 * "AI कलाकार" - always prominent, never replaced with an icon only.
 */
@Composable
fun AIKalakarBrandWordmark(
    modifier: Modifier = Modifier,
    isHindi: Boolean = true,
    onLanguageToggle: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wordmark lockup
                Text(
                    text = "AI ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        color = TerracottaPrimary,
                        fontSize = 26.sp
                    )
                )
                Text(
                    text = "कलाकार",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepIndigo,
                        fontSize = 26.sp
                    )
                )
            }
            Text(
                text = if (isHindi) "कारीगरों का डिजिटल साथी • E-Commerce Manager" else "Artisan Business Manager • Vocal for Local",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CharcoalMuted,
                    fontSize = 11.sp,
                    letterSpacing = 0.3.sp
                )
            )
        }

        // Language toggle pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LinenCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, CraftBorder),
            modifier = Modifier
                .clickable { onLanguageToggle() }
                .testTag("language_toggle_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Language",
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHindi) "हिंदी" else "English",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DeepIndigo
                )
            }
        }
    }
}

/**
 * Hard Constraint #3: Labeled Placeholder Slot for User-Supplied Asset
 * "Wherever I have not yet supplied an asset (logo, splash graphic, banner),
 * leave a clearly labeled placeholder slot (e.g., a dashed-border box saying
 * 'Insert provided logo here') — never fill the gap with an AI-generated stand-in."
 */
@Composable
fun UserAssetPlaceholderSlot(
    slotLabel: String,
    modifier: Modifier = Modifier,
    height: Int = 100,
    hintText: String = "User supplied brand asset slot"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(vertical = 4.dp)
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawRoundRect(
                    color = TerracottaPrimary.copy(alpha = 0.55f),
                    style = stroke
                )
            }
            .background(TerracottaContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = TerracottaPrimary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "[$slotLabel]",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OnTerracottaContainer
                )
            )
            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = CharcoalMuted
                )
            )
        }
    }
}

/**
 * Feature 1: Interactive Real Before / After Comparison Slider
 * Lets the artisan smoothly slide between Raw Photo and Studio Enhanced photo.
 */
@Composable
fun BeforeAfterComparisonSlider(
    rawBitmap: Bitmap?,
    enhancedBitmap: Bitmap?,
    sliderPosition: Float,
    onSliderPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isHindi: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(NaturalLinen)
            .border(1.dp, CraftBorder, RoundedCornerShape(16.dp))
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val splitWidth = totalWidth * sliderPosition

        if (rawBitmap != null && enhancedBitmap != null) {
            // Enhanced image is underneath (full width)
            Image(
                bitmap = enhancedBitmap.asImageBitmap(),
                contentDescription = "Studio Enhanced Product",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Raw image is clipped on the left portion based on sliderPosition
            Box(
                modifier = Modifier
                    .width(splitWidth)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp,
                            topEnd = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
            ) {
                Image(
                    bitmap = rawBitmap.asImageBitmap(),
                    contentDescription = "Raw Unedited Product",
                    modifier = Modifier
                        .width(totalWidth)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
            }

            // Divider vertical line
            Box(
                modifier = Modifier
                    .offset { IntOffset(splitWidth.toPx().roundToInt() - 2.dp.toPx().roundToInt(), 0) }
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )

            // Draggable Thumb handle
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            splitWidth.toPx().roundToInt() - 24.dp.toPx().roundToInt(),
                            (totalHeight.toPx() / 2 - 24.dp.toPx()).roundToInt()
                        )
                    }
                    .size(48.dp)
                    .shadow(6.dp, CircleShape)
                    .background(DeepIndigo, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newFraction = (splitWidth.toPx() + dragAmount.x) / totalWidth.toPx()
                            onSliderPositionChange(newFraction.coerceIn(0.05f, 0.95f))
                        }
                    }
                    .testTag("before_after_slider_handle"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIos,
                        contentDescription = "Slide Left",
                        tint = TurmericGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Slide Right",
                        tint = TurmericGold,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Left badge: Raw
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isHindi) "कच्ची तस्वीर (RAW)" else "RAW PHOTO",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Right badge: Studio Enhanced
            Surface(
                color = TerracottaPrimary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TurmericGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "स्टूडियो फिनिश (STUDIO)" else "STUDIO ENHANCED",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Empty state placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "उत्पाद की फोटो चुनें या खींचें" else "Take or upload product photo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Accessible Voice Microphone Action Button with visual pulse
 */
@Composable
fun VoiceRecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHindi: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .background(TerracottaPrimary.copy(alpha = 0.25f), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(4.dp, CircleShape)
                    .background(if (isRecording) TerracottaPrimary else DeepIndigo, CircleShape)
                    .clickable { onClick() }
                    .testTag("voice_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Record",
                    tint = if (isRecording) TurmericGold else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRecording) {
                if (isHindi) "सुन रहे हैं... (Listening...)" else "Listening..."
            } else {
                if (isHindi) "माइक दबाकर बोलें (Tap to Speak)" else "Tap to Speak"
            },
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isRecording) TerracottaPrimary else CharcoalText,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Product status pill (LIVE, DRAFT, SOLD)
 */
@Composable
fun ProductStatusBadge(status: String, isHindi: Boolean = true) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "LIVE" -> Triple(
            CraftGreenContainer,
            CraftGreen,
            if (isHindi) "लाइव / Live" else "Live"
        )
        "DRAFT" -> Triple(
            TurmericContainer,
            DeepIndigo,
            if (isHindi) "ड्राफ्ट / Draft" else "Draft"
        )
        "SOLD" -> Triple(
            Color(0xFFEDE8E3),
            CharcoalMuted,
            if (isHindi) "बिका हुआ / Sold" else "Sold"
        )
        else -> Triple(
            IndigoContainer,
            DeepIndigo,
            status
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
