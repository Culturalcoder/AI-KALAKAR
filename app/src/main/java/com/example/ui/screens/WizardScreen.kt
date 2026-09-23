package com.example.ui.screens
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.image.ImageStudioProcessor
import com.example.data.image.StudioBackdrop
import com.example.ui.components.BeforeAfterComparisonSlider
import com.example.ui.components.VoiceRecordingButton
import com.example.ui.theme.CharcoalMuted
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CraftBorder
import com.example.ui.theme.CraftRed
import com.example.ui.theme.CraftRedContainer
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
import com.example.ui.viewmodel.ArtisanViewModel
import com.example.ui.viewmodel.WizardStep
import java.io.File
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(
    viewModel: ArtisanViewModel,
    onNavigateBack: () -> Unit,
    onProductPublished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isHindi = currentLang == "hi"

    val wizardState by viewModel.wizardState.collectAsState()
    val currentStep = wizardState.currentStep

    // Temp Uri for high-resolution Camera capture
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            coroutineScope.launch {
                val bitmap = ImageStudioProcessor.loadBitmapFromUri(context, cameraTempUri!!)
                if (bitmap != null) {
                    viewModel.onImageSelected(bitmap)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "captured_craft_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val photoFile = File(context.cacheDir, "captured_craft_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraTempUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Android Photo Picker launcher (zero storage permission, Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = ImageStudioProcessor.loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    viewModel.onImageSelected(bitmap)
                }
            }
        }
    }

    // Voice recognition launcher
    val voiceSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                val spokenText = spokenMatches[0]
                val currentText = wizardState.artisanSpokenNotes
                val newText = if (currentText.isBlank()) spokenText else "$currentText $spokenText"
                viewModel.updateArtisanSpokenNotes(newText)
            }
        }
    }

    // Rejection / Error Dialog for non-craft capture
    if (wizardState.productRejectionMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRejectionDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(TerracottaContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isHindi) "हस्तशिल्प उत्पाद नहीं है / अस्वीकृत" else "Non-Handicraft Detected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DeepIndigo,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = TerracottaContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = wizardState.productRejectionMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = DeepIndigo
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        text = if (isHindi) 
                            "AI कलाकार विशेष रूप से भारतीय पारंपरिक कारीगरों, बुनकरों, हस्तशिल्पकारों और हस्तनिर्मित कलाकृतियों (जैसे वस्त्र, मिट्टी के बर्तन, कशीदाकारी, लकड़ी या धातु शिल्प) के लिए है। इलेक्ट्रॉनिक सामान, दीवार, व्यक्तिगत फोटो या गैर-शिल्प वस्तुएं स्वीकार्य नहीं हैं।"
                            else "AI Kalakar is exclusively for traditional artisans, weavers, handloom textiles, pottery, and authentic handmade crafts. Electronics, blank walls, personal selfies, or general non-craft items are not permitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissRejectionDialog()
                        launchCamera()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isHindi) "कैमरा से पुनः फोटो लें" else "Retake with Camera")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.dismissRejectionDialog()
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    border = BorderStroke(1.dp, DeepIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "गैलरी से चुनें" else "Pick from Gallery",
                        color = DeepIndigo
                    )
                }
            },
            containerColor = LinenCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NaturalLinen,
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                                text = "कलाकार",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            )
                        }
                        Text(
                            text = when (currentStep) {
                                WizardStep.PHOTO_STUDIO -> if (isHindi) "चरण 1: फोटो स्टूडियो व सुधार" else "Step 1: AI Photo Studio"
                                WizardStep.CATALOG_STORY -> if (isHindi) "चरण 2: द्विभाषी विवरण व कहानी" else "Step 2: Bilingual Catalog"
                                WizardStep.FAIR_PRICING -> if (isHindi) "चरण 3: उचित कारीगर मूल्य" else "Step 3: Fair Craft Pricing"
                                WizardStep.FINAL_REVIEW -> if (isHindi) "चरण 4: समीक्षा व प्रकाशन" else "Step 4: Review & Publish"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                WizardStep.PHOTO_STUDIO -> onNavigateBack()
                                WizardStep.CATALOG_STORY -> viewModel.setWizardStep(WizardStep.PHOTO_STUDIO)
                                WizardStep.FAIR_PRICING -> viewModel.setWizardStep(WizardStep.CATALOG_STORY)
                                WizardStep.FINAL_REVIEW -> viewModel.setWizardStep(WizardStep.FAIR_PRICING)
                            }
                        },
                        modifier = Modifier.testTag("wizard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturalLinen)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Progress Bar
            val progress = currentStep.stepNumber / 4f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = TerracottaPrimary,
                trackColor = CraftBorder
            )

            // Step Indicator Bubbles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WizardStepItem(
                    stepNum = 1,
                    title = if (isHindi) "स्टूडियो" else "Studio",
                    isActive = currentStep.stepNumber >= 1,
                    isCurrent = currentStep == WizardStep.PHOTO_STUDIO
                )
                StepConnector(isPassed = currentStep.stepNumber > 1)
                WizardStepItem(
                    stepNum = 2,
                    title = if (isHindi) "विवरण" else "Catalog",
                    isActive = currentStep.stepNumber >= 2,
                    isCurrent = currentStep == WizardStep.CATALOG_STORY
                )
                StepConnector(isPassed = currentStep.stepNumber > 2)
                WizardStepItem(
                    stepNum = 3,
                    title = if (isHindi) "मूल्य" else "Pricing",
                    isActive = currentStep.stepNumber >= 3,
                    isCurrent = currentStep == WizardStep.FAIR_PRICING
                )
                StepConnector(isPassed = currentStep.stepNumber > 3)
                WizardStepItem(
                    stepNum = 4,
                    title = if (isHindi) "प्रकाशन" else "Publish",
                    isActive = currentStep.stepNumber >= 4,
                    isCurrent = currentStep == WizardStep.FINAL_REVIEW
                )
            }

            // Step Contents
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    WizardStep.PHOTO_STUDIO -> {
                        PhotoStudioStepView(
                            wizardState = wizardState,
                            isHindi = isHindi,
                            onCaptureCamera = { launchCamera() },
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onLoadSample = { viewModel.loadSampleArtisanPhoto() },
                            onSliderChange = { viewModel.setSliderPosition(it) },
                            onBackdropChange = { viewModel.setBackdrop(it) },
                            onToggleBackgroundRemoval = { viewModel.toggleBackgroundRemoval(it) },
                            onSensitivityChange = { viewModel.setRemovalSensitivity(it) },
                            onNext = { viewModel.setWizardStep(WizardStep.CATALOG_STORY) }
                        )
                    }

                    WizardStep.CATALOG_STORY -> {
                        CatalogStoryStepView(
                            wizardState = wizardState,
                            isHindi = isHindi,
                            onStartVoice = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-IN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "अपनी कलाकृति के बारे में बोलें..." else "Speak about your handcrafted creation...")
                                }
                                try {
                                    voiceSpeechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Fallback sample spoken notes if speech intent not installed in emulator
                                    viewModel.updateArtisanSpokenNotes(
                                        if (isHindi) "यह मिट्टी का पारंपरिक कुल्हड़ चाक पर हाथ से बनाया गया है। इसमें चाय का असली स्वाद आता है और यह बिना केमिकल का है।"
                                        else "This authentic earthenware tea kulhad was hand-thrown on the potter wheel in Khurja with organic clay."
                                    )
                                }
                            },
                            onNotesChange = { viewModel.updateArtisanSpokenNotes(it) },
                            onCategoryChange = { viewModel.updateCategory(it) },
                            onMaterialChange = { viewModel.updateMaterial(it) },
                            onGenerateCatalog = { viewModel.generateAutoCatalog() },
                            onFieldsChange = { enTitle, hiTitle, enDesc, hiDesc, tags ->
                                viewModel.updateCatalogFields(enTitle, hiTitle, enDesc, hiDesc, tags)
                            },
                            onNext = {
                                // Pre-calculate pricing if empty
                                if (wizardState.pricingResult == null) {
                                    viewModel.calculateDynamicPricing()
                                }
                                viewModel.setWizardStep(WizardStep.FAIR_PRICING)
                            }
                        )
                    }

                    WizardStep.FAIR_PRICING -> {
                        FairPricingStepView(
                            wizardState = wizardState,
                            isHindi = isHindi,
                            onPricingInputsChange = { cost, hours, complexity ->
                                viewModel.updatePricingInputs(cost, hours, complexity)
                                viewModel.calculateDynamicPricing()
                            },
                            onRecalculate = { viewModel.calculateDynamicPricing() },
                            onSelectedPriceChange = { viewModel.setSelectedPrice(it) },
                            onNext = { viewModel.setWizardStep(WizardStep.FINAL_REVIEW) }
                        )
                    }

                    WizardStep.FINAL_REVIEW -> {
                        FinalReviewStepView(
                            wizardState = wizardState,
                            isHindi = isHindi,
                            onPublish = { status ->
                                viewModel.saveAndPublishProduct(status) { newId ->
                                    onProductPublished(newId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 1: PHOTO STUDIO
// -------------------------------------------------------------------------------------------------

@Composable
fun PhotoStudioStepView(
    wizardState: com.example.ui.viewmodel.WizardState,
    isHindi: Boolean,
    onCaptureCamera: () -> Unit,
    onPickPhoto: () -> Unit,
    onLoadSample: () -> Unit,
    onSliderChange: (Float) -> Unit,
    onBackdropChange: (StudioBackdrop) -> Unit,
    onToggleBackgroundRemoval: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onNext: () -> Unit
) {
    var showSensitivityControls by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isHindi) "उत्पाद की तस्वीर लें या अपलोड करें" else "Capture or Upload Product Photo",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                )
                Text(
                    text = if (isHindi) "AI बैकग्राउंड रिमूवर अपने आप फर्श/वर्कशॉप का बैकग्राउंड हटाकर ई-कॉमर्स स्टूडियो फिनिश देता है।"
                    else "AI Background Remover automatically eliminates floor/clutter and creates a studio catalog shot.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalMuted
                )
            }
        }

        // E-Commerce 1:1 Standard Studio Badge
        if (wizardState.enhancedBitmap != null) {
            item {
                Surface(
                    color = CraftGreenContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CraftGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CraftGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "ई-कॉमर्स 1:1 मानक: बैकग्राउंड हटाया गया, स्टूडियो लाइटिंग व कंट्रास्ट संतुलित"
                            else "E-Commerce 1:1 Standard: Clutter removed, studio lighting & centering formatted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CraftGreen
                        )
                    }
                }
            }
        }

        // Before / After Slider Interactive Canvas
        item {
            BeforeAfterComparisonSlider(
                rawBitmap = wizardState.rawBitmap,
                enhancedBitmap = wizardState.enhancedBitmap,
                sliderPosition = wizardState.sliderPosition,
                onSliderPositionChange = onSliderChange,
                isHindi = isHindi
            )
        }

        // Action Buttons: Camera, Gallery & Sample
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Real Camera Capture Button
                Button(
                    onClick = onCaptureCamera,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(50.dp)
                        .testTag("capture_camera_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "कैमरा (फोटो लें)" else "Take Photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Gallery Picker Button
                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pick_photo_button"),
                    border = BorderStroke(1.dp, DeepIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHindi) "गैलरी" else "Gallery",
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo,
                        fontSize = 13.sp
                    )
                }

                // Sample Craft Button
                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                        .testTag("load_sample_photo_button"),
                    border = BorderStroke(1.dp, TurmericGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TurmericGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isHindi) "नमूना" else "Sample",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Dedicated Background Removal & Studio Backdrops Card
        if (wizardState.rawBitmap != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header: Background Removal Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "AI बैकग्राउंड हटाएं (Remove Background)" else "AI Background Remover",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DeepIndigo
                                    )
                                    Text(
                                        text = if (isHindi) "उत्पाद को अलग कर बैकड्रॉप जोड़ें" else "Isolate craft & apply studio backdrop",
                                        fontSize = 11.sp,
                                        color = CharcoalMuted
                                    )
                                }
                            }

                            Switch(
                                checked = wizardState.isBackgroundRemoved,
                                onCheckedChange = { onToggleBackgroundRemoval(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TerracottaPrimary,
                                    uncheckedThumbColor = CharcoalMuted,
                                    uncheckedTrackColor = NaturalLinen
                                ),
                                modifier = Modifier.testTag("toggle_background_removal_switch")
                            )
                        }

                        if (wizardState.isBackgroundRemoved) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isHindi) "स्टूडियो बैकड्रॉप चुनें:" else "Select Studio Backdrop:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CharcoalText
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Horizontal Backdrop selector chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(StudioBackdrop.values()) { backdrop ->
                                    val isSelected = wizardState.selectedBackdrop == backdrop
                                    Surface(
                                        color = if (isSelected) TerracottaPrimary else NaturalLinen,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) TerracottaPrimary else CraftBorder
                                        ),
                                        modifier = Modifier
                                            .clickable { onBackdropChange(backdrop) }
                                            .testTag("backdrop_chip_${backdrop.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Preview Dot
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (backdrop == StudioBackdrop.TRANSPARENT) Color.LightGray
                                                        else Color(backdrop.colorHex)
                                                    )
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHindi) backdrop.titleHi else backdrop.titleEn,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else CharcoalText
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = TurmericGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Fine Tuning Sensitivity Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSensitivityControls = !showSensitivityControls },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isHindi) "किनारों की संवेदनशीलता ट्यून करें" else "Fine-tune Edge Sensitivity",
                                    fontSize = 12.sp,
                                    color = TerracottaPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (showSensitivityControls) "▲" else "▼",
                                    fontSize = 11.sp,
                                    color = TerracottaPrimary
                                )
                            }

                            if (showSensitivityControls) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isHindi) "हल्का (Soft)" else "Soft Cutout",
                                            fontSize = 11.sp,
                                            color = CharcoalMuted
                                        )
                                        Text(
                                            text = if (isHindi) "सख्त (Sharp)" else "Sharp Cutout",
                                            fontSize = 11.sp,
                                            color = CharcoalMuted
                                        )
                                    }
                                    Slider(
                                        value = wizardState.backgroundRemovalSensitivity,
                                        onValueChange = { onSensitivityChange(it) },
                                        valueRange = 0.1f..0.9f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = TerracottaPrimary,
                                            activeTrackColor = TerracottaPrimary,
                                            inactiveTrackColor = NaturalLinen
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Studio Processing Status or Analysis Result Card
        if (wizardState.isProcessingStudio) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TurmericContainer),
                    border = BorderStroke(1.dp, TurmericGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = TerracottaPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isHindi) "AI स्टूडियो बैकग्राउंड हटाकर लाइटिंग तैयार कर रहा है..."
                            else "AI Studio removing background and generating studio lighting...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CharcoalText
                        )
                    }
                }
            }
        }

        wizardState.imageAnalysis?.let { analysis ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "AI विज़न विश्लेषण (Gemini Analysis)" else "Gemini Vision Analysis",
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• ${if (isHindi) "पहचानी गई श्रेणी:" else "Category:"} ${analysis.detectedCategory}",
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = "• ${if (isHindi) "सामग्री:" else "Material:"} ${analysis.detectedMaterial}",
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = "• ${if (isHindi) "बैकग्राउंड स्थिति:" else "Background:"} ${analysis.backgroundCondition}",
                            fontSize = 13.sp,
                            color = CharcoalMuted
                        )
                        Text(
                            text = "• ${if (isHindi) "लागू सुधार:" else "Adjustment:"} ${analysis.suggestedLightingAdjust}",
                            fontSize = 13.sp,
                            color = CraftGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Proceed Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNext,
                enabled = wizardState.rawBitmap != null && wizardState.productRejectionMessage == null && !wizardState.isProcessingStudio,
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("step1_next_button")
            ) {
                Text(
                    text = if (isHindi) "अगला: विवरण व कहानी (Next)" else "Next: Catalog Story",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 2: CATALOG STORY
// -------------------------------------------------------------------------------------------------

@Composable
fun CatalogStoryStepView(
    wizardState: com.example.ui.viewmodel.WizardState,
    isHindi: Boolean,
    onStartVoice: () -> Unit,
    onNotesChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMaterialChange: (String) -> Unit,
    onGenerateCatalog: () -> Unit,
    onFieldsChange: (String, String, String, String, String) -> Unit,
    onNext: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val categories = listOf(
        "मिट्टी शिल्प / Pottery",
        "हथकरघा / Handloom",
        "काष्ठकला / Woodcraft",
        "धातु शिल्प / Brass",
        "आभूषण / Jewelry",
        "चित्रकारी / Painting"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isHindi) "कलाकृति की कहानी व विवरण" else "Craft Story & Description",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                )
                Text(
                    text = if (isHindi) "माइक दबाकर अपनी बोली में बताएं या लिखें। AI दोनों भाषाओं में ई-कॉमर्स विवरण तैयार करेगा।"
                    else "Speak or type in your native language. AI generates SEO listings in both Hindi & English.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalMuted
                )
            }
        }

        // Voice Microphone Interaction Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LinenCard),
                border = BorderStroke(1.dp, CraftBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VoiceRecordingButton(
                        isRecording = wizardState.isRecordingVoice,
                        onClick = onStartVoice,
                        isHindi = isHindi
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = wizardState.artisanSpokenNotes,
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text(
                                text = if (isHindi) "अपनी कलाकृति के बारे में बताएं (जैसे: चाक पर बनी प्राकृतिक मिट्टी, कोई केमिकल नहीं, 2 दिन की मेहनत)..."
                                else "Describe your handcrafted piece (materials, technique, care)...",
                                fontSize = 13.sp,
                                color = CharcoalMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("spoken_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerracottaPrimary,
                            unfocusedBorderColor = CraftBorder,
                            focusedContainerColor = NaturalLinen,
                            unfocusedContainerColor = NaturalLinen
                        )
                    )
                }
            }
        }

        // Craft Category Selector Chips
        item {
            Column {
                Text(
                    text = if (isHindi) "शिल्प श्रेणी चुनें:" else "Select Craft Category:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DeepIndigo
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val selected = wizardState.category == cat
                        FilterChip(
                            selected = selected,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TerracottaPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = LinenCard
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selected) TerracottaPrimary else CraftBorder,
                                selectedBorderColor = TerracottaPrimary,
                                enabled = true,
                                selected = selected
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // Material input
        item {
            OutlinedTextField(
                value = wizardState.material,
                onValueChange = onMaterialChange,
                label = { Text(if (isHindi) "प्राथमिक सामग्री (Material)" else "Primary Material") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("material_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerracottaPrimary,
                    unfocusedBorderColor = CraftBorder,
                    focusedContainerColor = LinenCard,
                    unfocusedContainerColor = LinenCard
                )
            )
        }

        // Generate AI Catalog Button
        item {
            Button(
                onClick = onGenerateCatalog,
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("generate_catalog_button"),
                enabled = !wizardState.isGeneratingCatalog
            ) {
                if (wizardState.isGeneratingCatalog) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isHindi) "Gemini सूची तैयार कर रहा है..." else "Generating with Gemini...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TurmericGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "AI से द्विभाषी सूची तैयार करें" else "Generate Bilingual Catalog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Generated Catalog Fields (with Hindi / English tabs)
        if (wizardState.titleHi.isNotBlank() || wizardState.titleEn.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = LinenCard,
                            contentColor = TerracottaPrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = TerracottaPrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("हिंदी (Hindi)", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("English (E-Commerce)", fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (selectedTab == 0) {
                            // Hindi Title
                            OutlinedTextField(
                                value = wizardState.titleHi,
                                onValueChange = {
                                    onFieldsChange(
                                        wizardState.titleEn,
                                        it,
                                        wizardState.descriptionEn,
                                        wizardState.descriptionHi,
                                        wizardState.tags
                                    )
                                },
                                label = { Text("हिंदी शीर्षक (Hindi Title)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Hindi Description
                            OutlinedTextField(
                                value = wizardState.descriptionHi,
                                onValueChange = {
                                    onFieldsChange(
                                        wizardState.titleEn,
                                        wizardState.titleHi,
                                        wizardState.descriptionEn,
                                        it,
                                        wizardState.tags
                                    )
                                },
                                label = { Text("हिंदी विवरण व शिल्प कहानी") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            // English Title
                            OutlinedTextField(
                                value = wizardState.titleEn,
                                onValueChange = {
                                    onFieldsChange(
                                        it,
                                        wizardState.titleHi,
                                        wizardState.descriptionEn,
                                        wizardState.descriptionHi,
                                        wizardState.tags
                                    )
                                },
                                label = { Text("English Title (SEO)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // English Description
                            OutlinedTextField(
                                value = wizardState.descriptionEn,
                                onValueChange = {
                                    onFieldsChange(
                                        wizardState.titleEn,
                                        wizardState.titleHi,
                                        it,
                                        wizardState.descriptionHi,
                                        wizardState.tags
                                    )
                                },
                                label = { Text("English Story & Care Specs") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tags
                        OutlinedTextField(
                            value = wizardState.tags,
                            onValueChange = {
                                onFieldsChange(
                                    wizardState.titleEn,
                                    wizardState.titleHi,
                                    wizardState.descriptionEn,
                                    wizardState.descriptionHi,
                                    it
                                )
                            },
                            label = { Text("ई-कॉमर्स हैशटैग (Marketplace Tags)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Bottom Proceed Button
        item {
            Button(
                onClick = onNext,
                enabled = wizardState.titleHi.isNotBlank() || wizardState.titleEn.isNotBlank() || wizardState.artisanSpokenNotes.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("step2_next_button")
            ) {
                Text(
                    text = if (isHindi) "अगला: उचित मूल्य निर्धारण (Next)" else "Next: Fair Pricing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 3: FAIR PRICING
// -------------------------------------------------------------------------------------------------

@Composable
fun FairPricingStepView(
    wizardState: com.example.ui.viewmodel.WizardState,
    isHindi: Boolean,
    onPricingInputsChange: (Int, Float, String) -> Unit,
    onRecalculate: () -> Unit,
    onSelectedPriceChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    val complexities = listOf(
        "Simple / सरल",
        "Moderate / मध्यम",
        "Intricate / बारीक नक्काशी",
        "Masterpiece / उत्कृष्ट कला"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isHindi) "पारदर्शी एवं उचित कारीगर मूल्य" else "Dynamic Fair Craft Pricing",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                )
                Text(
                    text = if (isHindi) "कारीगर को कभी भी लागत से कम न बेचना पड़े — कच्चा माल, श्रम घंटे व मेला/ऑनलाइन मार्जिन के आधार पर पारदर्शी गणना।"
                    else "Ensure artisans never undersell. Transparent calculation of materials, labor, and platform overhead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalMuted
                )
            }
        }

        // Inputs Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LinenCard),
                border = BorderStroke(1.dp, CraftBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Input 1: Material Cost Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "कच्चा माल खर्च (Material Cost):" else "Raw Material Cost:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = "₹${wizardState.materialCost}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TerracottaPrimary
                        )
                    }

                    Slider(
                        value = wizardState.materialCost.toFloat(),
                        onValueChange = {
                            onPricingInputsChange(it.toInt(), wizardState.laborHours, wizardState.craftComplexity)
                        },
                        valueRange = 50f..3000f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = TerracottaPrimary,
                            activeTrackColor = TerracottaPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 2: Labor Hours Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "कारीगरी का समय (Labor Time):" else "Artisan Labor Time:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CharcoalText
                        )
                        Text(
                            text = "${wizardState.laborHours} ${if (isHindi) "घंटे (hrs)" else "hrs"}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = DeepIndigo
                        )
                    }

                    Slider(
                        value = wizardState.laborHours,
                        onValueChange = {
                            onPricingInputsChange(wizardState.materialCost, it, wizardState.craftComplexity)
                        },
                        valueRange = 1f..20f,
                        steps = 37,
                        colors = SliderDefaults.colors(
                            thumbColor = DeepIndigo,
                            activeTrackColor = DeepIndigo
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 3: Complexity Chips
                    Text(
                        text = if (isHindi) "कारीगरी की जटिलता (Craft Complexity):" else "Craft Complexity:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CharcoalText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(complexities) { comp ->
                            val selected = wizardState.craftComplexity == comp
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onPricingInputsChange(wizardState.materialCost, wizardState.laborHours, comp)
                                },
                                label = { Text(comp, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Calculation Breakdown Display
        wizardState.pricingResult?.let { pricing ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerracottaContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, TerracottaPrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "सुझाया गया मूल्य दायरा" else "Suggested Price Range",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnTerracottaContainer
                            )
                            Surface(
                                color = CraftGreenContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "उचित पारिश्रमिक सुरक्षित" else "Fair Wage Guard",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CraftGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Big Price Range Highlight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isHindi) "न्यूनतम उचित सीमा" else "Minimum Fair Price",
                                    fontSize = 11.sp,
                                    color = CharcoalMuted
                                )
                                Text(
                                    text = "₹${pricing.priceMin}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalText
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(CraftBorder)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isHindi) "अनुशंसित खुदरा मूल्य" else "Recommended Retail",
                                    fontSize = 11.sp,
                                    color = CharcoalMuted
                                )
                                Text(
                                    text = "₹${pricing.priceMax}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Formula transparency items
                        Surface(
                            color = LinenCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (pricing.breakdown.isNotEmpty()) {
                                    pricing.breakdown.forEach { (categoryLabel, amount) ->
                                        PricingLineItem(
                                            label = "• $categoryLabel:",
                                            value = "₹$amount"
                                        )
                                    }
                                } else {
                                    PricingLineItem(
                                        label = if (isHindi) "• न्यूनतम उचित मूल्य (Base):" else "• Minimum Fair Price:",
                                        value = "₹${pricing.priceMin}"
                                    )
                                    PricingLineItem(
                                        label = if (isHindi) "• अनुशंसित विक्रय मूल्य:" else "• Recommended Price:",
                                        value = "₹${pricing.suggestedPrice}"
                                    )
                                    PricingLineItem(
                                        label = if (isHindi) "• प्रीमियम बाजार मूल्य:" else "• Premium Market Price:",
                                        value = "₹${pricing.priceMax}"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = pricing.reasoning,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CharcoalText,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }

            // Final Price Slider to pick selling price
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LinenCard),
                    border = BorderStroke(1.dp, CraftBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "आपका चुना हुआ विक्रय मूल्य:" else "Your Final Selling Price:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepIndigo
                            )
                            Text(
                                text = "₹${wizardState.selectedPrice}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TerracottaPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }

                        val minVal = (pricing.priceMin * 0.9f).coerceAtLeast(100f)
                        val maxVal = (pricing.priceMax * 1.3f).coerceAtLeast(minVal + 100f)

                        Slider(
                            value = wizardState.selectedPrice.toFloat().coerceIn(minVal, maxVal),
                            onValueChange = { onSelectedPriceChange(it.toInt()) },
                            valueRange = minVal..maxVal,
                            colors = SliderDefaults.colors(
                                thumbColor = TerracottaPrimary,
                                activeTrackColor = TerracottaPrimary
                            )
                        )
                    }
                }
            }
        }

        // Bottom Proceed Button
        item {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("step3_next_button")
            ) {
                Text(
                    text = if (isHindi) "अगला: समीक्षा व पुष्टि (Next)" else "Next: Review & Publish",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun PricingLineItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = CharcoalMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
    }
}

// -------------------------------------------------------------------------------------------------
// STEP 4: FINAL REVIEW & PUBLISH
// -------------------------------------------------------------------------------------------------

@Composable
fun FinalReviewStepView(
    wizardState: com.example.ui.viewmodel.WizardState,
    isHindi: Boolean,
    onPublish: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (isHindi) "समीक्षा व प्रकाशन (Review & Publish)" else "Review & Publish Product",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                )
                Text(
                    text = if (isHindi) "सब कुछ सही है? सीधे अपने कैटलॉग व मार्केटप्लेस पर लाइव करें।"
                    else "Check details before publishing to your artisan catalog & marketplace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalMuted
                )
            }
        }

        // Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LinenCard),
                border = BorderStroke(1.dp, CraftBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Studio Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .background(NaturalLinen)
                    ) {
                        val imgUri = wizardState.enhancedUri.ifBlank { wizardState.rawUri }
                        if (imgUri.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(imgUri),
                                contentDescription = "Enhanced Product",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Surface(
                            color = TerracottaPrimary,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "AI Studio Enhanced",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Content
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = wizardState.category,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TerracottaPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "₹${wizardState.selectedPrice}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = DeepIndigo,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isHindi && wizardState.titleHi.isNotBlank()) wizardState.titleHi else wizardState.titleEn,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isHindi && wizardState.descriptionHi.isNotBlank()) wizardState.descriptionHi else wizardState.descriptionEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CharcoalMuted,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = wizardState.tags,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DeepIndigo,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPublish("LIVE") },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("publish_live_button"),
                    enabled = !wizardState.isSaving
                ) {
                    if (wizardState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.PublishedWithChanges, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "कैटलॉग में लाइव करें (Publish Live)" else "Publish to Live Catalog",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onPublish("DRAFT") },
                    border = BorderStroke(1.5.dp, DeepIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_draft_button"),
                    enabled = !wizardState.isSaving
                ) {
                    Text(
                        text = if (isHindi) "ड्राफ्ट के रूप में सहेजें (Save as Draft)" else "Save as Draft",
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SUB-COMPONENTS
// -------------------------------------------------------------------------------------------------

@Composable
fun WizardStepItem(
    stepNum: Int,
    title: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    when {
                        isCurrent -> TerracottaPrimary
                        isActive -> DeepIndigo
                        else -> LinenCard
                    },
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isActive || isCurrent) Color.Transparent else CraftBorder,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActive && !isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$stepNum",
                    color = if (isCurrent || isActive) Color.White else CharcoalMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) TerracottaPrimary else CharcoalMuted
        )
    }
}

@Composable
fun StepConnector(isPassed: Boolean) {
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(2.dp)
            .background(if (isPassed) DeepIndigo else CraftBorder)
    )
}
