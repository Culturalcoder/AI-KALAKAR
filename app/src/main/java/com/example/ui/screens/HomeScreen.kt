package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.net.Uri
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.R
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TurmericGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun HomeScreen(
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Floating menu state
    var isMenuOpen by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Sequential Word Animation for Tagline
    val taglineWords = remember {
        listOf("Haathon", "Ka", "Hunar,", "Ab", "Duniya", "Ka", "Bazaar.")
    }
    val wordsRevealed = remember { mutableStateListOf<Boolean>() }
    var isMainTitleVisible by remember { mutableStateOf(false) }
    var isSupportingTextVisible by remember { mutableStateOf(false) }
    var isCtaVisible by remember { mutableStateOf(false) }

    // Tap press micro-interaction scale for CTA
    val ctaPressScale = remember { Animatable(1f) }
    var isNavigating by remember { mutableStateOf(false) }

    // Infinite transitions for ambient glow, video breathing, and CTA effects
    val infiniteTransition = rememberInfiniteTransition(label = "home_ambient")

    // Video-like breathing scale for background loom visual
    val videoBreathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loom_video_scale"
    )
    val videoBreathingY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loom_video_y"
    )

    // Brand name glow pulsation
    val brandGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brand_glow"
    )

    // Three-dot menu animated wave phases
    val dotWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_wave"
    )

    // CTA subtle outer glow pulsation
    val ctaGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_glow"
    )

    // CTA gentle breathing scale
    val ctaBreathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )

    // CTA light-sweep shimmer offset
    val ctaLightSweep by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cta_light_sweep"
    )

    // Sequential trigger for Tagline words and elements
    LaunchedEffect(Unit) {
        wordsRevealed.clear()
        repeat(taglineWords.size) { wordsRevealed.add(false) }

        delay(300)
        // Reveal each word sequentially with smooth motion
        for (i in taglineWords.indices) {
            wordsRevealed[i] = true
            delay(140)
        }

        delay(200)
        isMainTitleVisible = true

        delay(350)
        isSupportingTextVisible = true

        delay(250)
        isCtaVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0A08))
            .testTag("home_screen")
    ) {
        // 1. Cinematic Background (Artisan Loom Weaving Video Effect)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = videoBreathingScale
                    scaleY = videoBreathingScale
                    translationY = videoBreathingY
                }
        ) {
            LoomBackgroundVideoView(modifier = Modifier.fillMaxSize())
        }

        // Dark Vignette & Gradient Scrims for High Contrast Typography
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0D0A08),
                            Color(0x400D0A08),
                            Color(0x800D0A08),
                            Color(0xEB0D0A08),
                            Color(0xFC0A0806)
                        )
                    )
                )
        )

        // Subtle Ambient Floating Luminous Particles (Echoing handloom weaving magic)
        LoomParticleCanvas(modifier = Modifier.fillMaxSize())

        // Content Column Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HEADER: Top Navigation Bar
            HomeHeader(
                brandGlowAlpha = brandGlowAlpha,
                dotWavePhase = dotWavePhase,
                isMenuOpen = isMenuOpen,
                onToggleMenu = { isMenuOpen = !isMenuOpen },
                onDismissMenu = { isMenuOpen = false },
                onMenuHomeClick = {
                    isMenuOpen = false
                },
                onMenuKalakarClick = {
                    isMenuOpen = false
                    showAboutDialog = true
                },
                onMenuProfileClick = {
                    isMenuOpen = false
                    showProfileDialog = true
                }
            )

            // MAIN TYPOGRAPHY & TEXT (Centered Content)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sequential Animated Tagline: “Haathon Ka Hunar, Ab Duniya Ka Bazaar.”
                TaglineFlow(
                    words = taglineWords,
                    revealedList = wordsRevealed
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Main Title: AI कलाकार (Extremely bold, rounded, large, dominant typeface)
                this@Column.AnimatedVisibility(
                    visible = isMainTitleVisible,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
                            )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.testTag("home_main_title")
                    ) {
                        // Ambient Title Underglow
                        Text(
                            text = "AI कलाकार",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = TerracottaPrimary.copy(alpha = 0.5f)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .offset(y = 2.dp)
                                .blur(8.dp)
                        )

                        // Dominant Gradient Title Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "AI ",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 50.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = Color(0xFFFF9E80)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "कलाकार",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 50.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = Color(0xFFFFD54F)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Supporting Text: “For every artisan, weaver & craft maker.” (Soft fade-in transition)
                this@Column.AnimatedVisibility(
                    visible = isSupportingTextVisible,
                    enter = fadeIn(animationSpec = tween(700)) +
                            slideInVertically(
                                initialOffsetY = { 20 },
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                            )
                ) {
                    Text(
                        text = "For every artisan, weaver & craft maker.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFFEADCC9)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // CALL TO ACTION (CTA) BUTTON: Market Live !
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCtaVisible,
                    enter = fadeIn(animationSpec = tween(500)) +
                            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.7f))
                ) {
                    MarketLiveCtaButton(
                        glowAlpha = ctaGlowAlpha,
                        breathingScale = ctaBreathingScale * ctaPressScale.value,
                        lightSweepProgress = ctaLightSweep,
                        enabled = !isNavigating,
                        onClick = {
                            if (isNavigating) return@MarketLiveCtaButton
                            isNavigating = true
                            coroutineScope.launch {
                                // Micro-interaction: Simulated tap press animation (scale down and spring back)
                                ctaPressScale.animateTo(0.92f, animationSpec = tween(100))
                                ctaPressScale.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.6f))
                                delay(120)
                                onNavigateToDashboard()
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Dialog for "AI कलाकार" info from Floating Menu
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("समझ गया / Got it", color = TerracottaPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TurmericGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI कलाकार मंच",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Text(
                    text = "AI कलाकार भारत के लाखों पारंपरिक कारीगरों, बुनकरों और हस्तशिल्पकारों को सशक्त बनाने वाला डिजिटल प्लेटफॉर्म है। फोटो खींचकर AI द्वारा हिंदी व अंग्रेजी विवरण, निष्पक्ष मूल्य सुझाव और Supabase क्लाउड सिंक की सुविधा प्रदान करता है।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal Dialog for "Profile" info from Floating Menu
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        onNavigateToDashboard()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("डैशबोर्ड पर जाएं / Go to Dashboard", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("बंद करें / Close")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("कारीगर प्रोफाइल (Artisan Profile)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "नमस्ते कारीगर जी!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "आपका शिल्प और हस्तकरघा कौशल भारत की अनमोल धरोहर है। मार्केट लाइव बटन दबाकर अपने उत्पाद सीधे डिजिटल बाजार में पेश करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Header with brand glow, animated three-dot icon, and floating menu.
 */
@Composable
private fun HomeHeader(
    brandGlowAlpha: Float,
    dotWavePhase: Float,
    isMenuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuHomeClick: () -> Unit,
    onMenuKalakarClick: () -> Unit,
    onMenuProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("home_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Name: “AI कलाकार” with premium glow and animation
        Box(
            contentAlignment = Alignment.CenterStart
        ) {
            // Soft animated outer glow behind brand name
            Text(
                text = "AI कलाकार",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 0.8.sp,
                    color = TurmericGold.copy(alpha = brandGlowAlpha * 0.7f)
                ),
                modifier = Modifier
                    .blur(6.dp)
                    .offset(x = 0.dp, y = 0.dp)
            )

            // Brand text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(TurmericGold, CircleShape)
                        .blur(1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI कलाकार",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                )
            }
        }

        // Right Side: Animated Three-Dot Icon & Floating Menu
        Box {
            IconButton(
                onClick = onToggleMenu,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .testTag("home_three_dot_menu_button")
            ) {
                // Subtle animated three dots (⋮)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.5.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    for (i in 0..2) {
                        val phaseOffset = (dotWavePhase + (i * 0.25f)) % 1f
                        val dotAlpha = 0.5f + (0.5f * kotlin.math.sin(phaseOffset * Math.PI).toFloat())
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color.White.copy(alpha = dotAlpha), CircleShape)
                        )
                    }
                }
            }

            // Floating Menu with soft scale-and-fade animation
            if (isMenuOpen) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(x = 0, y = 110),
                    onDismissRequest = onDismissMenu,
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xF21C1612), // Deep translucent obsidian linen
                        border = BorderStroke(1.dp, TurmericGold.copy(alpha = 0.35f)),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .width(180.dp)
                            .testTag("home_floating_menu")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            FloatingMenuItem(
                                title = "Home",
                                icon = Icons.Default.Home,
                                onClick = onMenuHomeClick
                            )
                            FloatingMenuItem(
                                title = "AI कलाकार",
                                icon = Icons.Default.AutoAwesome,
                                onClick = onMenuKalakarClick
                            )
                            FloatingMenuItem(
                                title = "Profile",
                                icon = Icons.Default.AccountCircle,
                                onClick = onMenuProfileClick
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item in the floating menu.
 */
@Composable
private fun FloatingMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TurmericGold,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}

/**
 * Sequential word-by-word reveal for the dramatic tagline:
 * “Haathon Ka Hunar, Ab Duniya Ka Bazaar.”
 */
@Composable
private fun TaglineFlow(
    words: List<String>,
    revealedList: List<Boolean>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_tagline_flow")
    ) {
        // Line 1: Haathon Ka Hunar,
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..2) {
                if (i < words.size) {
                    val isWordRevealed = revealedList.getOrNull(i) == true
                    val isHighlight = i == 2 // "Hunar,"
                    TaglineWord(
                        word = words[i],
                        isWordRevealed = isWordRevealed,
                        isHighlight = isHighlight,
                        highlightColor = Color(0xFFFFD54F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Line 2: Ab Duniya Ka Bazaar.
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 3..6) {
                if (i < words.size) {
                    val isWordRevealed = revealedList.getOrNull(i) == true
                    val isHighlight = i == 6 // "Bazaar."
                    TaglineWord(
                        word = words[i],
                        isWordRevealed = isWordRevealed,
                        isHighlight = isHighlight,
                        highlightColor = Color(0xFFFF9E80)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaglineWord(
    word: String,
    isWordRevealed: Boolean,
    isHighlight: Boolean,
    highlightColor: Color
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isWordRevealed,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { 28 },
                    animationSpec = spring(dampingRatio = 0.75f)
                )
    ) {
        Text(
            text = "$word ",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 23.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlight) highlightColor else Color(0xFFF7F3EC),
                letterSpacing = 0.8.sp
            )
        )
    }
}

/**
 * Prominent CTA Button: Market Live !
 * Features:
 * - Subtle outer glow around button
 * - Gentle scale up animation
 * - Soft light-sweep across surface
 * - Simulated tap press animation
 */
@Composable
private fun MarketLiveCtaButton(
    glowAlpha: Float,
    breathingScale: Float,
    lightSweepProgress: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(breathingScale),
        contentAlignment = Alignment.Center
    ) {
        // 1. Subtle Outer Glow Aura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TurmericGold.copy(alpha = glowAlpha * 0.6f),
                            TerracottaPrimary.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
                .blur(14.dp)
        )

        // 2. Primary Rounded Rectangular Button
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(30.dp),
            color = TerracottaPrimary,
            border = BorderStroke(1.5.dp, TurmericGold.copy(alpha = 0.85f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("cta_market_live_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFC45A25),
                                Color(0xFFD66934),
                                Color(0xFFBA4F1E)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 3. Soft Light-Sweep Canvas across the surface
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepX = size.width * lightSweepProgress
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            start = Offset(sweepX - 80f, 0f),
                            end = Offset(sweepX + 80f, size.height)
                        ),
                        size = size
                    )
                }

                // 4. Button Text & Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Market Live !",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Decorative floating particle canvas that simulates radiant threads of light.
 */
@Composable
private fun LoomParticleCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        List(20) {
            ParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2.5f + 1f,
                alpha = Random.nextFloat() * 0.4f + 0.1f,
                speed = Random.nextFloat() * 0.0006f + 0.0002f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "particles")
    val tick by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_tick"
    )

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val curY = ((p.y - (tick * p.speed * 500f)) % 1f + 1f) % 1f
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = p.alpha),
                radius = p.radius.dp.toPx(),
                center = Offset(p.x * size.width, curY * size.height)
            )
        }
    }
}

private data class ParticleData(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float
)

@Composable
private fun LoomBackgroundVideoView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isRunningOnEmulator = remember { isEmulatorDevice() }
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.loom_video}")
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // High-definition artisan backdrop
        Image(
            painter = painterResource(id = R.drawable.artisan_handloom_weaving_1788564549464),
            contentDescription = "Handloom Weaving",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Native looping background video player for real devices (bypasses emulator GPU decoder warnings)
        if (!isRunningOnEmulator) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            try {
                                mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            } catch (_: Exception) {}
                            start()
                        }
                        setOnErrorListener { _, _, _ -> true }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) {
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Cinematic dark gradient scrim for contrast & legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC120707),
                            Color(0x55000000),
                            Color(0xDD120707)
                        )
                    )
                )
        )
    }
}

private fun isEmulatorDevice(): Boolean {
    return (android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.BOARD == "QC_Reference_Phone"
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MANUFACTURER.contains("Genymotion"))
}

