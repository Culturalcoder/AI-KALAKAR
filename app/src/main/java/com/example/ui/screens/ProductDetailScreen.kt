package com.example.ui.screens
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductEntity
import com.example.ui.components.ProductStatusBadge
import com.example.ui.theme.CharcoalMuted
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CraftBorder
import com.example.ui.theme.CraftGreen
import com.example.ui.theme.CraftGreenContainer
import com.example.ui.theme.CraftRed
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.LinenCard
import com.example.ui.theme.NaturalLinen
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.viewmodel.ArtisanViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    viewModel: ArtisanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isHindi = currentLang == "hi"

    val products by viewModel.allProducts.collectAsState()
    val product = products.find { it.id == productId }

    var displayLanguageIsHindi by remember { mutableStateOf(isHindi) }
    var isSyncingToCloud by remember { mutableStateOf(false) }

    if (product == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("उत्पाद नहीं मिला / Product not found")
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NaturalLinen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (displayLanguageIsHindi && product.titleHi.isNotBlank()) product.titleHi else product.titleEn,
                        maxLines = 1,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepIndigo
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { displayLanguageIsHindi = !displayLanguageIsHindi },
                        modifier = Modifier.testTag("detail_lang_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Switch Language",
                            tint = TerracottaPrimary
                        )
                    }
                    IconButton(
                        onClick = { shareProductToMarketplace(context, product, displayLanguageIsHindi) },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
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
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Hero Image
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .background(NaturalLinen)
                    ) {
                        val img = product.enhancedImageUri.ifBlank { product.rawImageUri }
                        if (img.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(img),
                                contentDescription = product.titleEn,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Badges
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            ProductStatusBadge(status = product.status, isHindi = displayLanguageIsHindi)
                        }

                        Surface(
                            color = TerracottaPrimary,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "AI Verified Fair Price",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Status Toggler (LIVE / DRAFT / SOLD)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (displayLanguageIsHindi) "स्थिति बदलें:" else "Listing Status:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CharcoalText
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LIVE", "DRAFT", "SOLD").forEach { st ->
                                val selected = product.status.equals(st, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.updateProductStatus(product, st) },
                                    label = { Text(st, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (st == "LIVE") CraftGreen else DeepIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Title & Price Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = TerracottaPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (displayLanguageIsHindi && product.titleHi.isNotBlank()) product.titleHi else product.titleEn,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (displayLanguageIsHindi) "उचित विक्रय मूल्य" else "Artisan Selling Price",
                                    fontSize = 11.sp,
                                    color = CharcoalMuted
                                )
                                Text(
                                    text = "₹${product.selectedPrice}",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepIndigo,
                                        fontSize = 28.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (displayLanguageIsHindi) "मूल्य सीमा" else "Fair Range",
                                    fontSize = 11.sp,
                                    color = CharcoalMuted
                                )
                                Text(
                                    text = "₹${product.priceMin} - ₹${product.priceMax}",
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Pricing Rationale Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TerracottaContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, TerracottaPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (displayLanguageIsHindi) "मूल्य निर्धारण का आधार (Fair Wage Rationale)" else "Fair Wage Rationale",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = product.pricingReasoning,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CharcoalText,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Description & Heritage Story
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (displayLanguageIsHindi) "उत्पाद की कहानी व विवरण" else "Product Story & Description",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (displayLanguageIsHindi && product.descriptionHi.isNotBlank()) product.descriptionHi else product.descriptionEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CharcoalText,
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = product.tags,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TerracottaPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Supabase Cloud Sync Action
            item {
                OutlinedButton(
                    onClick = {
                        isSyncingToCloud = true
                        viewModel.syncExistingProductToCloud(product) { success, err ->
                            isSyncingToCloud = false
                            val msg = if (success) {
                                if (displayLanguageIsHindi) "सुपबेस क्लाउड पर सफलतापूर्वक सिंक हो गया!" else "Synced to Supabase Cloud successfully!"
                            } else {
                                "Sync error: " + (err ?: "Check connection")
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSyncingToCloud,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("detail_cloud_sync_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CraftGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CraftGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = CraftGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncingToCloud) {
                            if (displayLanguageIsHindi) "क्लाउड पर अपलोड हो रहा है..." else "Syncing to Supabase..."
                        } else {
                            if (displayLanguageIsHindi) "Supabase क्लाउड पर सिंक करें" else "Sync to Supabase Cloud"
                        },
                        fontWeight = FontWeight.Bold,
                        color = CraftGreen
                    )
                }
            }

            // Action Buttons: Share & Delete
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { shareProductToMarketplace(context, product, displayLanguageIsHindi) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_bottom_share_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (displayLanguageIsHindi) "मार्केटप्लेस पर भेजें" else "Share to Market",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteProduct(product)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(0.7f)
                            .height(50.dp)
                            .testTag("detail_delete_button"),
                        border = BorderStroke(1.dp, CraftRed),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CraftRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (displayLanguageIsHindi) "हटाएं" else "Delete",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
