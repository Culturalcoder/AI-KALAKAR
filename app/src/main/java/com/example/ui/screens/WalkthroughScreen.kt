package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.UserAssetPlaceholderSlot
import com.example.ui.theme.CharcoalMuted
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CraftBorder
import com.example.ui.theme.CraftGreen
import com.example.ui.theme.CraftGreenContainer
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.LinenCard
import com.example.ui.theme.NaturalLinen
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.theme.TurmericContainer
import com.example.ui.theme.TurmericGold
import com.example.ui.viewmodel.ArtisanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen(
    viewModel: ArtisanViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isHindi = currentLang == "hi"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NaturalLinen,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaPrimary
                            )
                        )
                        Text(
                            text = "कलाकार मार्गदर्शन",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("walkthrough_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DeepIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturalLinen)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Artisan Emblem / Logo Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kalakar_emblem),
                        contentDescription = "AI कलाकार Artisan Emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Headline
            item {
                Column {
                    Text(
                        text = if (isHindi) "अपने हुनर को सीधे बाज़ार से जोड़ें" else "Connect Your Craft Directly to Market",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "बिना किसी बिचौलिए के, अपनी हस्तनिर्मित कलाकृतियों को ऑनलाइन बेचना अब बेहद आसान है।"
                        else "Digitize, price, and sell handcrafted products online without middlemen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalMuted
                    )
                }
            }

            // Step 1 Guide Card
            item {
                WalkthroughStepCard(
                    stepNumber = "1",
                    icon = Icons.Default.PhotoCamera,
                    iconBg = TerracottaContainer,
                    iconTint = TerracottaPrimary,
                    title = if (isHindi) "फोटो खींचें — AI स्टूडियो करेगा सुधार" else "Step 1: AI Photo Studio",
                    description = if (isHindi) "अपनी कार्यशाला या टेबल पर उत्पाद की साधारण फोटो लें। AI खुद-ब-खुद रोशनी (Lighting), कंट्रास्ट और किनारों को ई-कॉमर्स गुणवत्ता में बदल देगा।"
                    else "Snap a photo of your craft. AI automatically enhances lighting, contrast, and craft details for an e-commerce studio look."
                )
            }

            // Step 2 Guide Card
            item {
                WalkthroughStepCard(
                    stepNumber = "2",
                    icon = Icons.Default.Mic,
                    iconBg = IndigoContainer,
                    iconTint = DeepIndigo,
                    title = if (isHindi) "माइक में बोलें — AI लिखेगा सूची" else "Step 2: Voice Auto-Cataloger",
                    description = if (isHindi) "माइक दबाकर अपनी बोली में कलाकृति की खासियत बताएं। AI हिंदी और अंग्रेज़ी दोनों में आकर्षक शीर्षक, सांस्कृतिक कहानी और हैशटैग तैयार करेगा।"
                    else "Speak in your native language. AI generates SEO-ready titles, descriptions, and hashtags in both Hindi & English."
                )
            }

            // Step 3 Guide Card
            item {
                WalkthroughStepCard(
                    stepNumber = "3",
                    icon = Icons.Default.Payments,
                    iconBg = TurmericContainer,
                    iconTint = TerracottaPrimary,
                    title = if (isHindi) "उचित मूल्य — कभी नुकसान में न बेचें" else "Step 3: Fair Dynamic Pricing",
                    description = if (isHindi) "कच्चा माल खर्च और अपनी मेहनत के घंटे दर्ज करें। AI आपके हुनर का सम्मान करते हुए सही और सुरक्षित मूल्य दायरा तय करेगा।"
                    else "Enter raw materials and craft hours. AI calculates fair artisan wages, packaging buffers, and market prices."
                )
            }

            // Get Started Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("walkthrough_get_started_button")
                ) {
                    Text(
                        text = if (isHindi) "समझ आ गया • शुरू करें (Get Started)" else "Got it • Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WalkthroughStepCard(
    stepNumber: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LinenCard),
        border = BorderStroke(1.dp, CraftBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CharcoalMuted,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}
