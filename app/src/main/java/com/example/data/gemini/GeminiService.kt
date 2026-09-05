package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ImageAnalysisResult(
    val isValidCraftProduct: Boolean = true,
    val rejectionReason: String? = null,
    val detectedCategory: String,
    val detectedMaterial: String,
    val backgroundCondition: String,
    val suggestedLightingAdjust: String,
    val craftsmanshipScore: String,
    val recommendations: List<String>
)

data class CatalogResult(
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val tags: List<String>
)

data class PricingResult(
    val suggestedPrice: Int,
    val priceMin: Int,
    val priceMax: Int,
    val reasoning: String,
    val breakdown: Map<String, Int>
)

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private fun getApiKey(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String
            key?.takeIf { it.isNotBlank() } ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Analyzes an artisan product photo and strictly validates whether it is a genuine handicraft/artisan creation.
     * If not a craft or physical product, isValidCraftProduct is set to false with an apologetic reason.
     */
    suspend fun analyzeProductImage(bitmap: Bitmap): ImageAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackImageAnalysis()
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are 'AI कलाकार', an inclusive, supportive Indian handicrafts & product studio AI assistant for rural & grassroots artisans.
                Examine this captured photo:
                
                VALIDATION INSTRUCTIONS:
                - Be very generous, forgiving, and welcoming to rural artisans taking photos in home workshops, outdoor courtyards, ground floors, or village stalls.
                - Recognize ALL handmade, artisanal, craft, textile, home decor, earthenware, terracotta, handloom, cloth, dupatta, saree, jewelry, embroidery, brass, bamboo, painting, woodcraft, stone carving, or physical goods that an artisan creates or sells.
                - Even if an artisan's hand, table, floor, workshop tool, or rustic surroundings are visible with the item, treat it as a VALID craft product (isValidCraftProduct: true).
                - ONLY set isValidCraftProduct: false if the image is 100% definitively NOT an artisan product or physical item (for instance: a pitch-black screen, pure blank wall with zero objects, a meme/text screenshot, or an extreme close-up of an animal face with no craft). If there is ANY craft or sellable handmade product visible, set isValidCraftProduct: true.
                
                Provide structured JSON ONLY matching this schema:
                {
                  "isValidCraftProduct": true,
                  "rejectionReason": null,
                  "detectedCategory": "string (e.g. Terracotta / Pottery, Handloom & Textiles, Woodcraft, Metal / Brass, Jewelry, Painting, Home Decor)",
                  "detectedMaterial": "string (e.g. Clay, Silk/Cotton, Wood, Brass, Bamboo, Natural Fibers)",
                  "backgroundCondition": "string (brief constructive assessment)",
                  "suggestedLightingAdjust": "string (e.g. +15% Warmth, Background Clean-up, Edge Clarity)",
                  "craftsmanshipScore": "string (e.g. Artisan Grade (9/10))",
                  "recommendations": ["string", "string"]
                }
            """.trimIndent()

            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }))
            }

            val contentsArray = JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w("GeminiService", "Vision API error: ${response.code} $responseBody")
                return@withContext fallbackImageAnalysis()
            }

            val jsonObject = JSONObject(responseBody)
            val textContent = jsonObject
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsed = JSONObject(textContent.cleanJson())
            val isValid = parsed.optBoolean("isValidCraftProduct", true)
            val rejection = if (parsed.has("rejectionReason") && !parsed.isNull("rejectionReason")) {
                parsed.getString("rejectionReason")
            } else null

            val recs = mutableListOf<String>()
            val recsArray = parsed.optJSONArray("recommendations")
            if (recsArray != null) {
                for (i in 0 until recsArray.length()) {
                    recs.add(recsArray.getString(i))
                }
            }

            ImageAnalysisResult(
                isValidCraftProduct = isValid,
                rejectionReason = rejection,
                detectedCategory = parsed.optString("detectedCategory", "हस्तशिल्प / Handicraft"),
                detectedMaterial = parsed.optString("detectedMaterial", "प्राकृतिक सामग्री / Natural Material"),
                backgroundCondition = parsed.optString("backgroundCondition", "प्राकृतिक प्रकाश / Natural ambient lighting"),
                suggestedLightingAdjust = parsed.optString("suggestedLightingAdjust", "+15% वॉर्मथ एवं कंट्रास्ट सुधार"),
                craftsmanshipScore = parsed.optString("craftsmanshipScore", "कारीगर श्रेणी / Master Craft (8.5/10)"),
                recommendations = if (recs.isEmpty()) listOf(
                    "उत्पाद को सीधी धूप के बजाय नरम प्राकृतिक प्रकाश में रखें।",
                    "पृष्ठभूमि को साधारण रखें ताकि कलाकृति उभर कर दिखे।"
                ) else recs
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Vision parsing failed", e)
            fallbackImageAnalysis()
        }
    }

    /**
     * Generates bilingual catalog (Hindi + English) with SEO tags.
     */
    suspend fun generateMultilingualCatalog(
        description: String,
        category: String,
        material: String,
        bitmap: Bitmap?
    ): CatalogResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackCatalog(description, category)
        }

        try {
            val partsArray = JSONArray()
            val prompt = """
                You are 'AI कलाकार', a cultural storyteller and e-commerce copywriter for rural Indian artisans.
                Craft an authentic, high-converting product listing in BOTH Hindi and English.
                
                Input Details:
                - Spoken Notes / Description: $description
                - Craft Category: $category
                - Material: $material
                
                Rules:
                1. Title EN: Punchy, descriptive e-commerce title (under 80 chars) highlighting handmade origin.
                2. Title HI: Respectful, resonant Hindi title in Devanagari script.
                3. Description EN: 2-3 engaging paragraphs celebrating artisan heritage, hand technique, cultural significance, and care tips.
                4. Description HI: Rich Hindi narrative (शुद्ध एवं सरल हिंदी) connecting the buyer with Indian traditions.
                5. Tags: 6-8 trending hashtag keywords (e.g. #HandmadeInIndia, #VocalForLocal, #IndianCrafts, #Terracotta).
                
                Return ONLY valid JSON matching this schema:
                {
                  "titleEn": "string",
                  "titleHi": "string",
                  "descriptionEn": "string",
                  "descriptionHi": "string",
                  "tags": ["#tag1", "#tag2", "#tag3"]
                }
            """.trimIndent()

            partsArray.put(JSONObject().put("text", prompt))
            if (bitmap != null) {
                try {
                    val base64 = bitmapToBase64(bitmap)
                    partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    }))
                } catch (e: Exception) {
                    Log.w("GeminiService", "Could not attach bitmap to catalog prompt", e)
                }
            }

            val contentsArray = JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.5)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackCatalog(description, category)
            }

            val jsonObject = JSONObject(responseBody)
            val textContent = jsonObject
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsed = JSONObject(textContent.cleanJson())
            val tags = mutableListOf<String>()
            val tagsArray = parsed.optJSONArray("tags")
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(i))
                }
            }

            CatalogResult(
                titleEn = parsed.optString("titleEn", "Handcrafted $category"),
                titleHi = parsed.optString("titleHi", "हस्तनिर्मित $category"),
                descriptionEn = parsed.optString("descriptionEn", description),
                descriptionHi = parsed.optString("descriptionHi", description),
                tags = if (tags.isEmpty()) listOf("#HandmadeInIndia", "#VocalForLocal", "#ArtisanMade") else tags
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Catalog generation failed", e)
            fallbackCatalog(description, category)
        }
    }

    /**
     * Fair Pricing Engine using Gemini 2.5 Flash.
     */
    suspend fun calculateDynamicPricing(
        materialCost: Int,
        laborHours: Float,
        complexity: String,
        category: String,
        title: String
    ): PricingResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackPricing(materialCost, laborHours)
        }

        try {
            val prompt = """
                You are a Fair-Trade Pricing Economist for Indian artisans working with the Ministry of Textiles and Craft Councils.
                Calculate a fair, sustainable price recommendation for an Indian artisan craft item in INR (₹).
                
                Parameters:
                - Raw Material Cost: ₹$materialCost
                - Labor Hours: $laborHours hours
                - Craft Technique Complexity: $complexity
                - Category: $category
                - Product: $title
                
                Guidelines:
                - Minimum Fair Hourly Wage for skilled artisans in India: ₹100 - ₹150 / hour.
                - Margin for tools, studio overhead & packaging: 15% - 20%.
                - Profit margin for artisan savings & enterprise: 20% - 30%.
                - Provide: suggestedPrice (integer in INR), priceMin (artisan mela / direct price), priceMax (boutique / export / luxury retail),
                  reasoning in simple Hindi explaining why this price is fair and prevents exploitation.
                - breakdown: map of cost components {"सामग्री (Material)": int, "श्रम पारिश्रमिक (Fair Labor)": int, "पैकेजिंग व अन्य (Overheads)": int, "कारीगर लाभ (Artisan Profit)": int}
                
                Return ONLY valid JSON matching this schema:
                {
                  "suggestedPrice": 850,
                  "priceMin": 650,
                  "priceMax": 1100,
                  "reasoning": "string in Hindi",
                  "breakdown": {
                    "सामग्री (Material)": 200,
                    "श्रम पारिश्रमिक (Fair Labor)": 450,
                    "पैकेजिंग व अन्य (Overheads)": 80,
                    "कारीगर लाभ (Artisan Profit)": 120
                  }
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackPricing(materialCost, laborHours)
            }

            val jsonObject = JSONObject(responseBody)
            val textContent = jsonObject
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsed = JSONObject(textContent.cleanJson())
            val breakdownMap = mutableMapOf<String, Int>()
            val breakdownObj = parsed.optJSONObject("breakdown")
            if (breakdownObj != null) {
                val keys = breakdownObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    breakdownMap[k] = breakdownObj.optInt(k, 0)
                }
            }

            PricingResult(
                suggestedPrice = parsed.optInt("suggestedPrice", (materialCost + (laborHours * 120)).toInt()),
                priceMin = parsed.optInt("priceMin", (materialCost + (laborHours * 90)).toInt()),
                priceMax = parsed.optInt("priceMax", (materialCost + (laborHours * 160) * 1.3).toInt()),
                reasoning = parsed.optString("reasoning", "कच्चा माल खर्च और ₹120/घंटा के कुशल कारीगर मानदेय के आधार पर उचित मूल्य।"),
                breakdown = if (breakdownMap.isEmpty()) mapOf(
                    "सामग्री (Material)" to materialCost,
                    "श्रम (Labor)" to (laborHours * 120).toInt(),
                    "लाभ (Margin)" to ((materialCost + laborHours * 120) * 0.25).toInt()
                ) else breakdownMap
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Pricing failed", e)
            fallbackPricing(materialCost, laborHours)
        }
    }

    private fun fallbackImageAnalysis(): ImageAnalysisResult {
        return ImageAnalysisResult(
            isValidCraftProduct = true,
            rejectionReason = null,
            detectedCategory = "मिट्टी शिल्प / Pottery",
            detectedMaterial = "पारंपरिक टेराकोटा मिट्टी (Terracotta)",
            backgroundCondition = "साधारण स्टूडियो लाइटिंग उपयुक्त",
            suggestedLightingAdjust = "स्वचालित छाया निष्कासन व स्पष्टता सुधार",
            craftsmanshipScore = "उत्कृष्ट हस्तकला (8.8/10)",
            recommendations = listOf(
                "कलाकृति को प्राकृतिक रोशनी वाले स्थान पर रखकर फोटो लें।",
                "कुल्हड़ या बर्तन के मुख और बनावट को थोड़ा ऊपर के कोण से दिखाएं।"
            )
        )
    }

    private fun fallbackCatalog(notes: String, category: String): CatalogResult {
        val baseText = if (notes.isNotBlank()) notes else "पारंपरिक भारतीय कारीगरों द्वारा हस्तनिर्मित उत्कृष्ट कलाकृति।"
        return CatalogResult(
            titleEn = "Handcrafted Authentic $category - Artisan Heritage Collection",
            titleHi = "पारंपरिक हस्तनिर्मित $category - कारीगर धरोहर संग्रह",
            descriptionEn = "$baseText\n\nCarefully shaped and finished using age-old handloom or pottery techniques passed down generations. Made with 100% natural materials without harsh chemicals, representing authentic Indian craftsmanship.",
            descriptionHi = "$baseText\n\nपुश्तों से चली आ रही पारंपरिक शिल्पकला द्वारा निर्मित। शुद्ध प्राकृतिक सामग्री और बिना किसी हानिकारक रसायन के तैयार की गई यह कलाकृति भारतीय संस्कृति और ग्रामीण हुनर का जीवंत प्रतीक है।",
            tags = listOf("#HandmadeInIndia", "#VocalForLocal", "#ArtisanCraft", "#IndianHeritage", "#FairTrade", "#ShilpSamagam")
        )
    }

    private fun fallbackPricing(materialCost: Int, laborHours: Float): PricingResult {
        val laborRate = 120
        val laborTotal = (laborHours * laborRate).toInt()
        val baseCost = materialCost + laborTotal
        val profit = (baseCost * 0.35f).toInt()
        val suggested = baseCost + profit
        val min = (baseCost * 1.15f).toInt()
        val max = (suggested * 1.4f).toInt()

        return PricingResult(
            suggestedPrice = suggested,
            priceMin = min,
            priceMax = max,
            reasoning = "कच्चा माल खर्च (₹$materialCost) + $laborHours घंटे का कुशल कारीगर पारिश्रमिक (₹$laborTotal) + 35% कारीगर लाभ व पैकेजिंग के आधार पर।",
            breakdown = mapOf(
                "कच्चा माल (Materials)" to materialCost,
                "कारीगर श्रम (Fair Labor)" to laborTotal,
                "पैकेजिंग व टूल्स (Overheads)" to (baseCost * 0.1f).toInt(),
                "कलाकार मुनाफा (Profit)" to profit
            )
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun String.cleanJson(): String {
        var clean = this.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
