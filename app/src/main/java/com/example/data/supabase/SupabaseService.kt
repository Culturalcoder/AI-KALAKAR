package com.example.data.supabase

import android.util.Log
import com.example.data.model.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Synchronizes an artisan craft product to the remote Supabase PostgreSQL `products` table.
     */
    suspend fun syncProductToSupabase(product: ProductEntity): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(Exception("Supabase credentials are not configured."))
        }

        try {
            val tagsArray = JSONArray()
            product.tags.split(",", "#", " ")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .forEach { tagsArray.put("#$it") }
            if (tagsArray.length() == 0) {
                tagsArray.put("#handmade")
                tagsArray.put("#indiancraft")
            }

            val titleEnglish = product.titleEn.ifBlank { "Handmade Artisan Craft" }
            val titleHindi = product.titleHi.ifBlank { "पारंपरिक हस्तशिल्प कलाकृति" }
            val descEnglish = product.descriptionEn.ifBlank { "Authentic traditional handicraft crafted with natural materials." }
            val descHindi = product.descriptionHi.ifBlank { "पारंपरिक कारीगरों द्वारा हस्तनिर्मित उत्कृष्ट कलाकृति।" }

            val rawCost = if (product.materialCost > 0) product.materialCost.toDouble() else 150.0
            val labor = if (product.laborHours > 0f) product.laborHours.toDouble() else 4.0
            val recPrice = if (product.selectedPrice > 0) product.selectedPrice.toDouble() else 450.0
            val minPrice = if (product.priceMin > 0) product.priceMin.toDouble() else (recPrice * 0.8)
            val maxPrice = if (product.priceMax > 0) product.priceMax.toDouble() else (recPrice * 1.3)

            val displayImageUrl = when {
                product.enhancedImageUri.isNotBlank() && (product.enhancedImageUri.startsWith("http://") || product.enhancedImageUri.startsWith("https://")) -> product.enhancedImageUri
                product.rawImageUri.isNotBlank() && (product.rawImageUri.startsWith("http://") || product.rawImageUri.startsWith("https://")) -> product.rawImageUri
                else -> "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=600&q=80"
            }

            val payload = JSONObject().apply {
                put("title_hindi", titleHindi)
                put("title_english", titleEnglish)
                put("description_hindi", descHindi)
                put("description_english", descEnglish)
                put("category", product.category.ifBlank { "Handicraft" })
                put("subcategory", "Artisan Collection")
                put("material", "प्राकृतिक सामग्री / Natural Material")
                put("color", "प्राकृतिक रंग / Natural Earthy")
                put("craft_type", "हस्तशिल्प / Handmade Craft")
                put("image_url", displayImageUrl)
                put("original_image_url", displayImageUrl)
                put("raw_material_cost", rawCost)
                put("labor_hours", labor)
                put("recommended_price", recPrice)
                put("selling_price", recPrice)
                put("min_price_range", minPrice)
                put("max_price_range", maxPrice)
                put("seo_keywords", tagsArray)
                put("is_published", !product.status.equals("DRAFT", ignoreCase = true))
            }

            val jsonBody = payload.toString()
            val url = "${SupabaseConfig.supabaseUrl.trimEnd('/')}/rest/v1/products"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.supabaseAnonKey}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Log.d("SupabaseService", "Product synced to Supabase: $bodyStr")
                    Result.success(bodyStr)
                } else {
                    Log.w("SupabaseService", "Supabase HTTP ${response.code}: $bodyStr")
                    Result.failure(Exception("Supabase HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Sync failure", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches live marketplace listings from Supabase `products` table.
     */
    suspend fun fetchLiveListingsFromSupabase(): Result<List<SupabaseProductResponse>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(Exception("Supabase is not configured."))
        }

        try {
            val url = "${SupabaseConfig.supabaseUrl.trimEnd('/')}/rest/v1/products?select=*&order=created_at.desc"

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.supabaseAnonKey)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.supabaseAnonKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyStr)
                    val list = mutableListOf<SupabaseProductResponse>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val price = if (obj.has("selling_price") && !obj.isNull("selling_price")) {
                            obj.optDouble("selling_price", 0.0)
                        } else {
                            obj.optDouble("recommended_price", 0.0)
                        }
                        list.add(
                            SupabaseProductResponse(
                                id = obj.optString("id", ""),
                                artisanId = obj.optString("artisan_id", null),
                                titleEn = obj.optString("title_english", obj.optString("title_en", "Artisan Craft")),
                                titleHi = obj.optString("title_hindi", obj.optString("title_hi", null)),
                                descriptionEn = obj.optString("description_english", null),
                                descriptionHi = obj.optString("description_hindi", null),
                                category = obj.optString("category", null),
                                material = obj.optString("material", null),
                                selectedPrice = price,
                                imageUrl = obj.optString("image_url", null),
                                status = if (obj.optBoolean("is_published", true)) "LIVE" else "DRAFT",
                                createdAt = obj.optString("created_at", null)
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
