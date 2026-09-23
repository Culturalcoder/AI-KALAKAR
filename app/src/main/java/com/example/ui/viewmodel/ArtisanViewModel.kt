package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.CatalogResult
import com.example.data.gemini.ImageAnalysisResult
import com.example.data.gemini.PricingResult
import com.example.data.image.ImageStudioProcessor
import com.example.data.image.StudioBackdrop
import com.example.data.model.ArtisanProfileEntity
import com.example.data.model.ProductEntity
import com.example.data.repository.ArtisanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WizardStep(val stepNumber: Int) {
    PHOTO_STUDIO(1),
    CATALOG_STORY(2),
    FAIR_PRICING(3),
    FINAL_REVIEW(4)
}

data class WizardState(
    val currentStep: WizardStep = WizardStep.PHOTO_STUDIO,
    val rawBitmap: Bitmap? = null,
    val enhancedBitmap: Bitmap? = null,
    val rawUri: String = "",
    val enhancedUri: String = "",
    val selectedBackdrop: StudioBackdrop = StudioBackdrop.WHITE_STUDIO,
    val isBackgroundRemoved: Boolean = true,
    val backgroundRemovalSensitivity: Float = 0.5f,
    val sliderPosition: Float = 0.5f,
    val isProcessingStudio: Boolean = false,
    val imageAnalysis: ImageAnalysisResult? = null,
    val category: String = "मिट्टी शिल्प / Pottery",
    val material: String = "प्राकृतिक टेराकोटा मिट्टी / Natural Terracotta",
    val artisanSpokenNotes: String = "",
    val isRecordingVoice: Boolean = false,
    val isGeneratingCatalog: Boolean = false,
    val catalogResult: CatalogResult? = null,
    val titleEn: String = "",
    val titleHi: String = "",
    val descriptionEn: String = "",
    val descriptionHi: String = "",
    val tags: String = "",
    val materialCost: Int = 200,
    val laborHours: Float = 4.0f,
    val craftComplexity: String = "Moderate / मध्यम",
    val isCalculatingPricing: Boolean = false,
    val pricingResult: PricingResult? = null,
    val selectedPrice: Int = 650,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val productRejectionMessage: String? = null
)

class ArtisanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArtisanRepository(application)

    // Language state: "hi" (Hindi) or "en" (English)
    private val _currentLanguage = MutableStateFlow("hi")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Walkthrough banner visibility
    private val _showWalkthrough = MutableStateFlow(false)
    val showWalkthrough: StateFlow<Boolean> = _showWalkthrough.asStateFlow()

    // Custom artisan banner URI
    private val prefs = application.getSharedPreferences("ai_kalakar_prefs", Context.MODE_PRIVATE)
    private val _customBannerUri = MutableStateFlow(prefs.getString("custom_banner_uri", null))
    val customBannerUri: StateFlow<String?> = _customBannerUri.asStateFlow()

    fun updateBannerUri(uri: String?) {
        _customBannerUri.value = uri
        prefs.edit().putString("custom_banner_uri", uri).apply()
    }

    // Search and filter
    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow("ALL") // "ALL", "LIVE", "DRAFT", "SOLD"

    // Profile
    val artisanProfile: StateFlow<ArtisanProfileEntity?> = repository.artisanProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered Products
    val allProducts: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        searchQuery,
        filterStatus
    ) { products, query, status ->
        products.filter { product ->
            val matchesStatus = (status == "ALL" || product.status.equals(status, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    product.titleHi.contains(query, ignoreCase = true) ||
                    product.titleEn.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true) ||
                    product.tags.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wizard State
    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    // Selected product for detail view
    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value == "hi") "en" else "hi"
        _currentLanguage.value = newLang
        viewModelScope.launch {
            repository.updatePreferredLanguage(newLang)
        }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        viewModelScope.launch {
            repository.updatePreferredLanguage(lang)
        }
    }

    fun toggleWalkthrough(show: Boolean) {
        _showWalkthrough.value = show
    }

    fun selectProduct(product: ProductEntity?) {
        _selectedProduct.value = product
    }

    fun loadProductById(id: Long) {
        viewModelScope.launch {
            _selectedProduct.value = repository.getProductById(id)
        }
    }

    fun updateProductStatus(product: ProductEntity, newStatus: String) {
        viewModelScope.launch {
            val updated = product.copy(status = newStatus)
            repository.updateProduct(updated)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = null
            }
        }
    }

    // -------------------------------------------------------------
    // WIZARD STEPS
    // -------------------------------------------------------------

    fun setWizardStep(step: WizardStep) {
        _wizardState.value = _wizardState.value.copy(currentStep = step)
    }

    fun dismissRejectionDialog() {
        _wizardState.value = _wizardState.value.copy(
            rawBitmap = null,
            enhancedBitmap = null,
            rawUri = "",
            enhancedUri = "",
            imageAnalysis = null,
            productRejectionMessage = null
        )
    }

    fun resetWizard() {
        _wizardState.value = WizardState()
    }

    fun onImageSelected(rawBitmap: Bitmap) {
        viewModelScope.launch {
            val currentState = _wizardState.value
            _wizardState.value = currentState.copy(
                rawBitmap = rawBitmap,
                isProcessingStudio = true,
                productRejectionMessage = null
            )

            // Save raw image
            val rawUri = ImageStudioProcessor.saveBitmapToInternalStorage(
                getApplication(), rawBitmap, "raw"
            )

            // Real client-side studio enhancement & background removal
            val enhancedBitmap = ImageStudioProcessor.enhanceToStudioQuality(
                context = getApplication(),
                rawBitmap = rawBitmap,
                backdrop = currentState.selectedBackdrop,
                removeBackground = currentState.isBackgroundRemoved,
                sensitivity = currentState.backgroundRemovalSensitivity,
                formatEcommerceSquare = true
            )
            val enhancedUri = ImageStudioProcessor.saveBitmapToInternalStorage(
                getApplication(), enhancedBitmap, "studio"
            )

            // Gemini Vision background clutter & craft analysis
            val analysis = repository.analyzeProductImage(rawBitmap)

            if (!analysis.isValidCraftProduct) {
                // Strictly Reject: clear bitmaps and URIs so non-craft item cannot proceed to catalog
                _wizardState.value = _wizardState.value.copy(
                    rawBitmap = null,
                    enhancedBitmap = null,
                    rawUri = "",
                    enhancedUri = "",
                    imageAnalysis = analysis,
                    isProcessingStudio = false,
                    productRejectionMessage = analysis.rejectionReason 
                        ?: "क्षमा करें! इस फोटो में कोई स्पष्ट हस्तशिल्प या कारीगर उत्पाद नहीं पहचान पाए। (Sorry! This does not appear to be an authentic handcrafted or artisan product)."
                )
            } else {
                _wizardState.value = _wizardState.value.copy(
                    rawBitmap = rawBitmap,
                    enhancedBitmap = enhancedBitmap,
                    rawUri = rawUri,
                    enhancedUri = enhancedUri,
                    imageAnalysis = analysis,
                    category = if (analysis.detectedCategory.isNotBlank()) analysis.detectedCategory else _wizardState.value.category,
                    material = if (analysis.detectedMaterial.isNotBlank()) analysis.detectedMaterial else _wizardState.value.material,
                    isProcessingStudio = false,
                    productRejectionMessage = null
                )
            }
        }
    }

    fun setBackdrop(backdrop: StudioBackdrop) {
        _wizardState.value = _wizardState.value.copy(selectedBackdrop = backdrop)
        reprocessStudioImage()
    }

    fun toggleBackgroundRemoval(enabled: Boolean) {
        _wizardState.value = _wizardState.value.copy(isBackgroundRemoved = enabled)
        reprocessStudioImage()
    }

    fun setRemovalSensitivity(sensitivity: Float) {
        _wizardState.value = _wizardState.value.copy(backgroundRemovalSensitivity = sensitivity)
        reprocessStudioImage()
    }

    private fun reprocessStudioImage() {
        val raw = _wizardState.value.rawBitmap ?: return
        viewModelScope.launch {
            _wizardState.value = _wizardState.value.copy(isProcessingStudio = true)
            val current = _wizardState.value
            val enhancedBitmap = ImageStudioProcessor.enhanceToStudioQuality(
                context = getApplication(),
                rawBitmap = raw,
                backdrop = current.selectedBackdrop,
                removeBackground = current.isBackgroundRemoved,
                sensitivity = current.backgroundRemovalSensitivity,
                formatEcommerceSquare = true
            )
            val enhancedUri = ImageStudioProcessor.saveBitmapToInternalStorage(
                getApplication(), enhancedBitmap, "studio"
            )
            _wizardState.value = _wizardState.value.copy(
                enhancedBitmap = enhancedBitmap,
                enhancedUri = enhancedUri,
                isProcessingStudio = false
            )
        }
    }

    fun loadSampleArtisanPhoto() {
        val sample = ImageStudioProcessor.createSampleArtisanBitmap()
        onImageSelected(sample)
    }

    fun setSliderPosition(pos: Float) {
        _wizardState.value = _wizardState.value.copy(sliderPosition = pos.coerceIn(0f, 1f))
    }

    fun updateArtisanSpokenNotes(notes: String) {
        _wizardState.value = _wizardState.value.copy(artisanSpokenNotes = notes)
    }

    fun updateCategory(cat: String) {
        _wizardState.value = _wizardState.value.copy(category = cat)
    }

    fun updateMaterial(mat: String) {
        _wizardState.value = _wizardState.value.copy(material = mat)
    }

    fun generateAutoCatalog() {
        viewModelScope.launch {
            _wizardState.value = _wizardState.value.copy(isGeneratingCatalog = true)

            val currentState = _wizardState.value
            val result = repository.generateCatalog(
                description = currentState.artisanSpokenNotes,
                category = currentState.category,
                material = currentState.material,
                bitmap = currentState.enhancedBitmap ?: currentState.rawBitmap
            )

            _wizardState.value = _wizardState.value.copy(
                isGeneratingCatalog = false,
                catalogResult = result,
                titleEn = result.titleEn,
                titleHi = result.titleHi,
                descriptionEn = result.descriptionEn,
                descriptionHi = result.descriptionHi,
                tags = result.tags.joinToString(" ")
            )
        }
    }

    fun updateCatalogFields(
        titleEn: String,
        titleHi: String,
        descriptionEn: String,
        descriptionHi: String,
        tags: String
    ) {
        _wizardState.value = _wizardState.value.copy(
            titleEn = titleEn,
            titleHi = titleHi,
            descriptionEn = descriptionEn,
            descriptionHi = descriptionHi,
            tags = tags
        )
    }

    fun updatePricingInputs(cost: Int, hours: Float, complexity: String) {
        _wizardState.value = _wizardState.value.copy(
            materialCost = cost,
            laborHours = hours,
            craftComplexity = complexity
        )
    }

    fun calculateDynamicPricing() {
        viewModelScope.launch {
            _wizardState.value = _wizardState.value.copy(isCalculatingPricing = true)

            val s = _wizardState.value
            val result = repository.calculatePricing(
                materialCost = s.materialCost,
                laborHours = s.laborHours,
                complexity = s.craftComplexity,
                category = s.category,
                title = if (s.titleHi.isNotBlank()) s.titleHi else s.titleEn
            )

            _wizardState.value = _wizardState.value.copy(
                isCalculatingPricing = false,
                pricingResult = result,
                selectedPrice = result.suggestedPrice
            )
        }
    }

    fun setSelectedPrice(price: Int) {
        _wizardState.value = _wizardState.value.copy(selectedPrice = price)
    }

    fun saveAndPublishProduct(status: String = "LIVE", onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val s = _wizardState.value
            _wizardState.value = s.copy(isSaving = true)

            val product = ProductEntity(
                titleEn = s.titleEn.ifBlank { "Handcrafted ${s.category}" },
                titleHi = s.titleHi.ifBlank { "हस्तनिर्मित ${s.category}" },
                descriptionEn = s.descriptionEn.ifBlank { "Authentic Indian artisan craft." },
                descriptionHi = s.descriptionHi.ifBlank { "पारंपरिक भारतीय हस्तशिल्प कलाकृति।" },
                category = s.category,
                priceMin = s.pricingResult?.priceMin ?: (s.selectedPrice * 0.8f).toInt(),
                priceMax = s.pricingResult?.priceMax ?: (s.selectedPrice * 1.3f).toInt(),
                selectedPrice = s.selectedPrice,
                materialCost = s.materialCost,
                laborHours = s.laborHours,
                pricingReasoning = s.pricingResult?.reasoning ?: "पारंपरिक कारीगर लागत और समय पर आधारित।",
                rawImageUri = s.rawUri,
                enhancedImageUri = s.enhancedUri.ifBlank { s.rawUri },
                tags = s.tags.ifBlank { "#AIकलाकार #HandmadeInIndia" },
                status = status
            )

            val newId = repository.saveProduct(product)
            // Asynchronously sync to live Supabase PostgreSQL database
            try {
                val savedProduct = product.copy(id = newId)
                repository.syncProductToCloud(savedProduct)
            } catch (_: Exception) {}
            _wizardState.value = _wizardState.value.copy(isSaving = false, saveSuccess = true)
            onComplete(newId)
        }
    }

    fun syncExistingProductToCloud(product: ProductEntity, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = repository.syncProductToCloud(product)
            res.fold(
                onSuccess = { onDone(true, null) },
                onFailure = { onDone(false, it.localizedMessage) }
            )
        }
    }
}

