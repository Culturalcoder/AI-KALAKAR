package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.gemini.CatalogResult
import com.example.data.gemini.GeminiService
import com.example.data.gemini.ImageAnalysisResult
import com.example.data.gemini.PricingResult
import com.example.data.image.ImageStudioProcessor
import com.example.data.local.ArtisanDatabase
import com.example.data.model.ArtisanProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ArtisanRepository(
    private val context: Context,
    private val database: ArtisanDatabase = ArtisanDatabase.getInstance(context),
    private val geminiService: GeminiService = GeminiService(),
    private val supabaseService: SupabaseService = SupabaseService()
) {
    private val productDao = database.productDao()
    private val profileDao = database.artisanProfileDao()

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val artisanProfile: Flow<ArtisanProfileEntity?> = profileDao.getProfile()
    val totalProductCount: Flow<Int> = productDao.getProductCount()
    val liveProductCount: Flow<Int> = productDao.getLiveProductCount()

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    suspend fun saveProduct(product: ProductEntity): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) = productDao.deleteProductById(id)

    suspend fun updatePreferredLanguage(lang: String) = profileDao.updateLanguage(lang)

    suspend fun setWalkthroughCompleted(completed: Boolean) = profileDao.updateWalkthrough(completed)

    suspend fun syncProductToCloud(product: ProductEntity): Result<String> {
        val result = supabaseService.syncProductToSupabase(product)
        if (result.isSuccess) {
            try {
                productDao.updateProduct(product.copy(syncStatus = "SYNCED"))
            } catch (e: Exception) {
                // local update failure is non-fatal
            }
        }
        return result
    }

    suspend fun analyzeProductImage(bitmap: Bitmap): ImageAnalysisResult {
        return geminiService.analyzeProductImage(bitmap)
    }

    suspend fun generateCatalog(
        description: String,
        category: String,
        material: String,
        bitmap: Bitmap?
    ): CatalogResult {
        return geminiService.generateMultilingualCatalog(description, category, material, bitmap)
    }

    suspend fun calculatePricing(
        materialCost: Int,
        laborHours: Float,
        complexity: String,
        category: String,
        title: String
    ): PricingResult {
        return geminiService.calculateDynamicPricing(materialCost, laborHours, complexity, category, title)
    }

    suspend fun seedSampleDataIfEmpty() {
        val count = totalProductCount.firstOrNull() ?: 0
        if (count == 0) {
            profileDao.insertOrUpdateProfile(
                ArtisanProfileEntity(
                    id = 1,
                    artisanName = "रमेश प्रजापति (Ramesh Prajapati)",
                    phone = "+91 98765 43210",
                    craftType = "पारंपरिक टेराकोटा एवं मिट्टी कला (Terracotta Craft)",
                    region = "खुर्जा, उत्तर प्रदेश (Khurja, UP)",
                    preferredLanguage = "hi",
                    completedWalkthrough = false,
                    totalEarnings = 42500L,
                    fairsAttended = "शिल्प समागम 2026, सूरजकुंड अंतरराष्ट्रीय मेला"
                )
            )

            val sampleBitmap = ImageStudioProcessor.createSampleArtisanBitmap()
            val sampleRawUri = ImageStudioProcessor.saveBitmapToInternalStorage(context, sampleBitmap, "sample_raw")
            val sampleEnhanced = ImageStudioProcessor.enhanceToStudioQuality(context, sampleBitmap)
            val sampleEnhancedUri = ImageStudioProcessor.saveBitmapToInternalStorage(context, sampleEnhanced, "sample_enhanced")

            productDao.insertProduct(
                ProductEntity(
                    titleEn = "Handcrafted Terracotta Chai Kulhad Set (Set of 6) - Eco-Friendly Clay",
                    titleHi = "हस्तनिर्मित पारंपरिक टेराकोटा कुल्हड़ कप सेट (6 का सेट) - प्राकृतिक मिट्टी",
                    descriptionEn = "Handcrafted on the traditional potter's wheel in Khurja, this set of 6 terracotta kulhad cups preserves authentic earthen tea flavors. Each piece is fired in traditional kiln furnaces without chemical glazes, offering 100% natural, biodegradable elegance.\n\nHand-finished with subtle geometric lines by master potters. Ideal for hot spiced chai, traditional lassi, or rustic home dining decor.",
                    descriptionHi = "खुर्जा के पारंपरिक चाक पर तैयार किए गए 6 टेराकोटा कुल्हड़ का यह सेट चाय के असली सोंधे स्वाद को बनाए रखता है। बिना किसी केमिकल या कृत्रिम रंग के, पारंपरिक भट्टी में पकाए गए ये कुल्हड़ 100% पर्यावरण-अनुकूल हैं।\n\nभारतीय शिल्पकला की मिठास और ग्रामीण कुम्हारों के हुनर का सुंदर नमूना।",
                    category = "मिट्टी शिल्प / Pottery",
                    priceMin = 450,
                    priceMax = 750,
                    selectedPrice = 650,
                    materialCost = 140,
                    laborHours = 3.5f,
                    pricingReasoning = "कच्चा माल खर्च (₹140) + 3.5 घंटे का कुशल कुम्हार पारिश्रमिक (₹420) + सुरक्षित थर्माकोल पैकेजिंग (₹90) के आधार पर।",
                    rawImageUri = sampleRawUri,
                    enhancedImageUri = sampleEnhancedUri,
                    tags = "#Terracotta #KulhadSet #HandmadeInIndia #VocalForLocal #EcoFriendly #ShilpSamagam",
                    status = "LIVE",
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                )
            )

            productDao.insertProduct(
                ProductEntity(
                    titleEn = "Handwoven Tussar Silk Stole with Zari Border - Generational Loom",
                    titleHi = "पारंपरिक हस्तकरघा तसर सिल्क दुपट्टा/स्टोल (ज़री बॉर्डर सहित)",
                    descriptionEn = "Woven on wooden pit-looms using wild organic tussar silk yarns. This exquisite stole features delicate antique zari weaving along the selvedge, taking 2 days of focused shuttle work.\n\nLightweight, breathable, and rich in natural golden sheen, perfect for festive occasions and formal heritage wear.",
                    descriptionHi = "प्राकृतिक तसर रेशम से लकड़ी के पारंपरिक हथकरघे पर बुना गया यह दुपट्टा 2 दिनों के अनथक परिश्रम का फल है। इसके किनारों पर महीन ज़री का काम किया गया है।\n\nत्योहारों और सांस्कृतिक अवसरों के लिए अत्यंत गरिमापूर्ण एवं सदाबहार वस्त्र।",
                    category = "हथकरघा / Handloom",
                    priceMin = 1400,
                    priceMax = 2200,
                    selectedPrice = 1850,
                    materialCost = 550,
                    laborHours = 8.0f,
                    pricingReasoning = "तसर सिल्क सूत (₹550) + 8 घंटे हथकरघा बुनाई (₹1,000) + ज़री बॉर्डर व फिनिशिंग (₹300) के आधार पर मेला व ऑनलाइन मूल्य।",
                    rawImageUri = sampleRawUri,
                    enhancedImageUri = sampleEnhancedUri,
                    tags = "#TussarSilk #HandloomWeaving #VocalForLocal #IndianHeritage #ArtisanMade",
                    status = "LIVE",
                    createdAt = System.currentTimeMillis() - 86400000L * 5
                )
            )
        }
    }
}
