# AI KALAKAR (AI कलाकार) — Tech Stack & Architecture Keywords

A categorized reference list of all technical keywords, libraries, models, and protocols used in **AI Kalakar** for your Technical Approach document and hackathon submission.

---

## 1. Mobile Client (Android)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose, Material 3 (Material Design)
- **Architecture Pattern:** MVVM (Model-View-ViewModel), Clean Architecture, Repository Pattern
- **Reactive Streams & Concurrency:** Kotlin Coroutines, StateFlow, SharedFlow, Dispatchers (IO, Default, Main)
- **Local Persistence:** Android Room Database, SQLite, Data Access Objects (DAOs)
- **Offline-First:** Android WorkManager, Local Entity Caching, Optimistic UI
- **Camera & Hardware:** Android CameraX API, SurfaceProvider, ImageCapture, Torch Control
- **Networking & Serialization:** OkHttp 4, Retrofit, Kotlinx Serialization, Gson, Org.Json
- **Image Handling:** Coil (Compose Image Loader), Bitmap Processing, Hardware Bitmaps, Base64 Encoding
- **Security & Config:** Android Secrets Gradle Plugin, BuildConfig Injection, ProGuard / R8 Shrinker
- **Target SDK & Platform:** Android SDK (Min SDK 24, Target SDK 36), Android Go compatibility

---

## 2. Feature 1: AI Image Enhancer & Studio
- **Client Heuristics:** Laplacian Variance (Blur Detection), Grayscale Histogram Analysis, Bilinear Downsampling
- **Niche Gate / Craft Validation:** Google Gemini 2.5 Flash, Gemini 3.8 Flash, Multimodal LLM, Zero-Shot Vision Classifier, System Prompting, Confidence Threshold Gating
- **Background Segmentation:** PhotoRoom API, Remove.bg API, Cloudinary AI Background Removal, Rembg (U^2-Net / BiRefNet)
- **Studio Compositing:** Contact Shadows, Drop Shadows, Pure White E-commerce Canvas (`#FFFFFF`), Lighting Normalization, Contrast-Limited Adaptive Histogram Equalization (CLAHE)
- **Super-Resolution:** Clipdrop API, Cloudinary AI Super-Resolution, Real-ESRGAN-compact
- **Asset Formats:** WebP Compression, Transparent PNG, JPEG (80% Quality), 1500×1500px Square Aspect Ratio

---

## 3. Feature 2: Multilingual Auto-Cataloger
- **Speech Capture & Recognition (ASR):** Android `SpeechRecognizer`, Indic ASR, Digital India Bhashini (ULCA API), Whisper Indic
- **Supported Indian Locales:** `hi-IN` (Hindi), `bn-IN` (Bengali), `ta-IN` (Tamil), `te-IN` (Telugu), `mr-IN` (Marathi), `gu-IN` (Gujarati), `en-IN` (Indian English)
- **Audio Output (TTS):** Android Native `TextToSpeech` (TTS), Bhashini IndicTTS, Audio Readback Loop
- **Language Models (LLM):** Google Gemini 3.8 Flash, Gemini 2.5 Flash, Generative Language REST API (`generateContent`)
- **Prompt Engineering & Output Parsing:** Structured JSON Schema Output (`responseMimeType: application/json`), Few-Shot Craft Prompting, Entity Extraction / NER (Named Entity Recognition)
- **Catalog Artifacts Generated:** Bilingual Titles (`titleEn`, `titleHi`), Bulleted Specifications (`descriptionEn`, `descriptionHi`), Cultural Heritage Storytelling, SEO E-commerce Search Tags

---

## 4. Feature 3: Dynamic Pricing Assistant
- **Machine Learning Algorithm:** Linear Regression, LightGBM Regressor, Scikit-Learn Estimator
- **Model Serialization:** Joblib (`pricing_model.joblib`), Pickle
- **Model Performance Metrics:** Coefficient of Determination ($R^2 = 0.9998$), Mean Absolute Error ($\text{MAE} = ₹22.18$)
- **Feature Vector / Signals:** Category Multipliers, Quality Tiers (Standard, Masterpiece, Fine Heritage), Raw Material Cost, Labor Cost, Packaging Cost, Overhead Costs, Competitor Benchmark Price, Average Market Price, Demand Elasticity Score (1–10)
- **Edge Inference:** On-Device Kotlin Inference (`HandicraftPricingMLModel.kt`), Zero Latency (0ms), Offline Execution
- **Backend Serving:** Python 3.10+, FastAPI, Uvicorn ASGI Server, Pydantic Schema Validation, Pandas, NumPy
- **Ethical Safeguards:** Fair Living Wage Floor, Rule-Constrained Heuristic Guardrail, Minimum Profit Margin Clamp (15%–25%)

---

## 5. Backend & Cloud Infrastructure
- **BaaS (Backend as a Service):** Supabase
- **Primary Database:** PostgreSQL 15, PostgREST API
- **Database Security:** Row Level Security (RLS) Policies, PostgreSQL Functions & Triggers
- **Database Tables:** `product_photos`, `pipeline_config`, `rejected_uploads`, `flagged_for_review`, `pipeline_logs`, `pipeline_retry_queue`
- **Blob / Object Storage:** Supabase Storage, S3-Compatible Buckets (`raw-uploads`, `enhanced-products`)
- **Serverless Edge Compute:** Deno Runtime, TypeScript, Supabase Edge Functions (`image-enhancer-pipeline`)
- **CDN & Edge Delivery:** Cloudflare CDN, WebP Edge Caching, Zero Egress Bandwidth

---

## 6. Protocols, Standards & Market Linkage
- **Decentralized Commerce:** ONDC (Open Network for Digital Commerce), Beckn Protocol, BPP (Beckn Provider Platform)
- **Government Portals:** GeM (Government e-Marketplace), UNSPSC Taxonomy Mapping, MSME Public Procurement
- **Data Exchange Schemas:** JSON-LD, OpenAPI 3.1, RESTful APIs, WebSockets
- **Accessibility & Compliance:** Oral-First UI, Zero-Text Entry, WCAG 2.1 AAA High Contrast, DPDP Act 2023 (Digital Personal Data Protection)
- **Payment & Settlement:** UPI vPA, Jan Dhan Direct Benefit Transfer, Escrow Splitting (Razorpay Route / Cashfree)
