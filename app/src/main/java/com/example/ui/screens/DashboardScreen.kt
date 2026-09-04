package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductEntity
import com.example.ui.components.AIKalakarBrandWordmark
import com.example.ui.components.ProductStatusBadge
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
fun DashboardScreen(
    viewModel: ArtisanViewModel,
    onNavigateToCreate: () -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onOpenWalkthrough: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isHindi = currentLang == "hi"

    val products by viewModel.allProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val profile by viewModel.artisanProfile.collectAsState()

    val totalCatalogValue = products.sumOf { it.selectedPrice }
    val liveCount = products.count { it.status.equals("LIVE", ignoreCase = true) }

    val customBannerUri by viewModel.customBannerUri.collectAsState()
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateBannerUri(uri.toString())
        }
    }

    val bannerModel: Any = remember(customBannerUri) {
        if (!customBannerUri.isNullOrBlank()) {
            Uri.parse(customBannerUri)
        } else {
            var assetFound: String? = null
            try {
                val list = context.assets.list("") ?: emptyArray()
                assetFound = list.firstOrNull { 
                    it.contains("WhatsApp", ignoreCase = true) || 
                    it.contains("artisan_banner", ignoreCase = true) ||
                    it.contains("banner", ignoreCase = true)
                }
            } catch (_: Exception) {}

            if (assetFound != null) {
                "file:///android_asset/$assetFound"
            } else {
                R.drawable.img_artisan_banner
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NaturalLinen,
        topBar = {
            Column {
                AIKalakarBrandWordmark(
                    isHindi = isHindi,
                    onLanguageToggle = { viewModel.toggleLanguage() }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = TerracottaPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("add_product_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "नया उत्पाद जोड़ें" else "Add Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Provided artisan banner (Supports exact static asset, assets folder, or direct photo picker)
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("artisan_banner_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = bannerModel,
                            contentDescription = "AI कलाकार - आपका, बाज़ार हमारा — बस एक क्लिक करें!",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            contentScale = ContentScale.Fit
                        )

                        // Action badge allowing instant selection of exact user static image
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CharcoalText.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clickable {
                                    bannerPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("change_banner_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "असली फोटो चुनें" else "Choose Static File",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!customBannerUri.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = CharcoalText.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clickable { viewModel.updateBannerUri(null) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Reset banner",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. High-Impact Stats Cards
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Stat 1: Live Products
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = LinenCard),
                        border = BorderStroke(1.dp, CraftBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHindi) "लाइव उत्पाद" else "Live Listings",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CharcoalMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(CraftGreenContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CraftGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$liveCount",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = DeepIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Stat 2: Total Catalog Value
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = LinenCard),
                        border = BorderStroke(1.dp, CraftBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHindi) "कैटलॉग मूल्य" else "Catalog Value",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CharcoalMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(TurmericContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹$totalCatalogValue",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = TerracottaPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // 3. Low-Literacy Quick Help Walkthrough Trigger Card
            item(span = { GridItemSpan(2) }) {
                Surface(
                    color = TerracottaContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TerracottaPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWalkthrough() }
                        .testTag("open_walkthrough_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(TerracottaPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "AI कलाकार कैसे काम करता है?" else "How AI कलाकार works?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepIndigo
                                )
                                Text(
                                    text = if (isHindi) "3 आसान चरण: फोटो • आवाज • सही मूल्य" else "3 Easy steps: Photo • Voice • Fair Price",
                                    fontSize = 12.sp,
                                    color = CharcoalMuted
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4. Search and Filter Section
            item(span = { GridItemSpan(2) }) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "उत्पाद खोजें (जैसे कुल्हड़, साड़ी, दीया)..." else "Search products (e.g. Kulhad, Shawl)...",
                                color = CharcoalMuted,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = DeepIndigo
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = CharcoalMuted
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LinenCard,
                            unfocusedContainerColor = LinenCard,
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = CraftBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips
                    val filterOptions = listOf(
                        "ALL" to (if (isHindi) "सभी (All)" else "All"),
                        "LIVE" to (if (isHindi) "लाइव (Live)" else "Live"),
                        "DRAFT" to (if (isHindi) "ड्राफ्ट (Draft)" else "Draft"),
                        "SOLD" to (if (isHindi) "बिका हुआ (Sold)" else "Sold")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterOptions) { (key, label) ->
                            val selected = filterStatus == key
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.filterStatus.value = key },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepIndigo,
                                    selectedLabelColor = Color.White,
                                    containerColor = LinenCard,
                                    labelColor = CharcoalText
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (selected) DeepIndigo else CraftBorder,
                                    selectedBorderColor = DeepIndigo,
                                    enabled = true,
                                    selected = selected
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            }

            // 5. Products Section Heading
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "आपकी कलाकृतियां (${products.size})" else "Your Creations (${products.size})",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = CharcoalText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 6. Product Cards or Empty State
            if (products.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = LinenCard),
                        border = BorderStroke(1.dp, CraftBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isHindi) "कोई उत्पाद नहीं मिला" else "No products found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "निचले बटन से अपनी पहली हस्तशिल्प कलाकृति जोड़ें!" else "Tap below to add your first handcrafted creation!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "नया उत्पाद जोड़ें" else "Add Product")
                            }
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    ProductCardItem(
                        product = product,
                        isHindi = isHindi,
                        onClick = { onProductClick(product) },
                        onShare = { shareProductToMarketplace(context, product, isHindi) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCardItem(
    product: ProductEntity,
    isHindi: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = LinenCard),
        border = BorderStroke(1.dp, CraftBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .background(NaturalLinen)
            ) {
                val imageUri = if (product.enhancedImageUri.isNotBlank()) {
                    product.enhancedImageUri
                } else {
                    product.rawImageUri
                }

                if (imageUri.isNotBlank()) {
                    AsyncImage(
                        model = Uri.parse(imageUri),
                        contentDescription = product.titleEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = TerracottaPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Status Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    ProductStatusBadge(status = product.status, isHindi = isHindi)
                }

                // Share quick action
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable { onShare() }
                        .testTag("share_product_button_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }

            // Info section
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TerracottaPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isHindi && product.titleHi.isNotBlank()) product.titleHi else product.titleEn,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = CharcoalText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "उचित मूल्य" else "Fair Price",
                            fontSize = 10.sp,
                            color = CharcoalMuted
                        )
                        Text(
                            text = "₹${product.selectedPrice}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DeepIndigo,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Surface(
                        color = TerracottaContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "AI Verified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Real Android Share Action (Share to Marketplace / WhatsApp / Web)
 */
fun shareProductToMarketplace(context: Context, product: ProductEntity, isHindi: Boolean) {
    val title = if (isHindi && product.titleHi.isNotBlank()) product.titleHi else product.titleEn
    val desc = if (isHindi && product.descriptionHi.isNotBlank()) product.descriptionHi else product.descriptionEn

    val shareText = """
        🏺 *AI कलाकार | प्रामाणिक हस्तशिल्प सूची*
        
        ✨ *${title}*
        📂 श्रेणी / Category: ${product.category}
        💰 उचित मूल्य / Fair Price: ₹${product.selectedPrice}
        
        📜 *विवरण / Description:*
        $desc
        
        🏷️ ${product.tags}
        
        🇮🇳 _सीधे भारतीय कारीगर से खरीदें | #VocalForLocal #AIकलाकार_
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_TITLE, title)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "मार्केटप्लेस पर साझा करें / Share to Marketplace")
    context.startActivity(shareIntent)
}
