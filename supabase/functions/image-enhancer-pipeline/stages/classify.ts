// ==============================================================================
// STAGE 2: NICHE GATE (Artisan Product Classification)
// Uses Gemini 2.5 Flash with strict JSON schema validation, retry & override support
// ==============================================================================

import { SupabaseClient } from "@supabase/supabase-js";
import {
  getConfigValue,
  logPipelineStage,
  safeParseJson,
  uint8ArrayToBase64,
  withRetry,
} from "../utils.ts";

export interface ClassificationResult {
  pass: boolean;
  isArtisanProduct: boolean;
  category: string;
  confidence: number;
  reason: string;
  flaggedForReview?: boolean;
}

const VALID_CATEGORIES = [
  "textile",
  "pottery",
  "jewelry",
  "woodwork",
  "embroidery",
  "weaving",
  "basketry",
  "leatherwork",
  "metalwork",
  "other",
];

/**
 * Classifies an uploaded photo to ensure it is exclusively an authentic artisan / handicraft / textile item.
 */
export async function classifyImage(
  supabase: SupabaseClient,
  params: {
    rawImagePath: string;
    rawBytes?: Uint8Array;
    mimeType?: string;
    userId?: string | null;
    override?: boolean;
  }
): Promise<ClassificationResult> {
  const startTime = Date.now();
  const { rawImagePath, userId, override = false } = params;

  try {
    // 1. Check Dynamic Configurations from pipeline_config
    const gateEnabled = await getConfigValue<boolean>(supabase, "niche_gate_enabled", true);
    const confidenceThreshold = await getConfigValue<number>(supabase, "confidence_threshold", 0.70);
    const configuredCategories = await getConfigValue<string[]>(supabase, "allowed_categories", VALID_CATEGORIES);

    // If gate is disabled globally for testing:
    if (!gateEnabled) {
      const latency = Date.now() - startTime;
      await logPipelineStage(supabase, {
        userId,
        stage: "niche_gate",
        status: "skipped",
        latencyMs: latency,
        metadata: { reason: "Niche gate disabled via pipeline_config" },
      });
      return {
        pass: true,
        isArtisanProduct: true,
        category: "other",
        confidence: 1.0,
        reason: "Niche gate temporarily bypassed via admin configuration.",
      };
    }

    // 2. Fetch image bytes from Storage if not passed in memory
    let imageBytes = params.rawBytes;
    let mimeType = params.mimeType || "image/jpeg";

    if (!imageBytes) {
      const { data, error: downloadError } = await supabase.storage
        .from("raw-uploads")
        .download(rawImagePath);

      if (downloadError || !data) {
        throw new Error(`Failed to download raw image from storage (${rawImagePath}): ${downloadError?.message}`);
      }

      imageBytes = new Uint8Array(await data.arrayBuffer());
      mimeType = data.type || (rawImagePath.endsWith(".png") ? "image/png" : "image/jpeg");
    }

    const base64Data = uint8ArrayToBase64(imageBytes);

    // 3. Call Gemini 2.5 Flash API with retry logic and JSON safety
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      throw new Error("GEMINI_API_KEY is not configured in Supabase Edge Function secrets.");
    }

    const systemPrompt = `
You are 'AI Kalakar Niche Gate', an AI classifier for an e-commerce platform focused EXCLUSIVELY on authentic artisan, handicraft, handloom, and textile products.

Analyze the provided image carefully and determine:
1. is_artisan_product: true if the photo contains an authentic handmade craft, handloom textile, pottery/terracotta, handcrafted jewelry, woodwork, embroidery, weaving, basketry, leatherwork, or brass/metal craft.
2. If the photo contains human selfies/portraits, electronic gadgets (laptops, phones, TVs, keyboards, cables), vehicles, blank walls, floors, empty surfaces, or generic mass-produced factory goods, is_artisan_product MUST BE false.
3. category: MUST BE one of [${configuredCategories.join(", ")}].
4. confidence: float between 0.00 and 1.00 indicating confidence that this is an authentic artisan product.
5. reason: A concise explanation (in English or Hindi) explaining the classification decision.

Output ONLY a raw, valid JSON object matching this schema exactly:
{
  "is_artisan_product": boolean,
  "category": "textile" | "pottery" | "jewelry" | "woodwork" | "embroidery" | "weaving" | "basketry" | "leatherwork" | "metalwork" | "other",
  "confidence": float,
  "reason": "string"
}
`.trim();

    const callGemini = async (repromptNotes?: string) => {
      const modelsToTry = ["gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-flash-latest"];
      const userPrompt = repromptNotes
        ? `Previous attempt was not valid JSON. Ensure strict JSON matching the schema without markdown fences: ${repromptNotes}`
        : systemPrompt;

      const body = {
        contents: [
          {
            parts: [
              { text: userPrompt },
              {
                inlineData: {
                  mimeType: mimeType,
                  data: base64Data,
                },
              },
            ],
          },
        ],
        generationConfig: {
          temperature: 0.1,
          responseMimeType: "application/json",
        },
      };

      let lastError: any = null;
      for (const model of modelsToTry) {
        try {
          const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiApiKey}`;
          const res = await fetch(endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
          });

          if (!res.ok) {
            const errorText = await res.text();
            lastError = new Error(`Gemini ${model} HTTP ${res.status}: ${errorText}`);
            continue;
          }

          const jsonResponse = await res.json();
          const rawText =
            jsonResponse?.candidates?.[0]?.content?.parts?.[0]?.text;
          if (!rawText) {
            throw new Error(`Empty response from Gemini model ${model}`);
          }
          return rawText;
        } catch (e) {
          lastError = e;
        }
      }
      throw lastError || new Error("All Gemini models failed");
    };

    // Execute with retry
    let rawText = await withRetry(
      () => callGemini(),
      2,
      600,
      "Gemini Classification"
    );

    // Safe JSON Parsing with single reprompt on syntax error
    let parsed = safeParseJson<{
      is_artisan_product: boolean;
      category: string;
      confidence: number;
      reason: string;
    }>(rawText);

    if (!parsed.success || !parsed.data) {
      console.warn("[classifyImage] JSON parse failed, re-prompting once:", parsed.error);
      const rePromptedText = await callGemini("Return ONLY raw valid JSON.");
      parsed = safeParseJson(rePromptedText);
    }

    if (!parsed.success || !parsed.data) {
      throw new Error(`Failed to parse valid JSON from Gemini response: ${parsed.error}`);
    }

    const {
      is_artisan_product = false,
      category = "other",
      confidence = 0.0,
      reason = "Classification completed",
    } = parsed.data;

    // Normalizing category to valid categories
    const normalizedCategory = configuredCategories.includes(category.toLowerCase())
      ? category.toLowerCase()
      : "other";

    const isConfidenceSufficient = confidence >= confidenceThreshold;
    const isArtisan = Boolean(is_artisan_product);
    const passesGate = isArtisan && isConfidenceSufficient;

    const latencyMs = Date.now() - startTime;

    // 4. Handle Rejection or Manual Override
    if (!passesGate) {
      if (override) {
        // User forced submission -> Log into flagged_for_review table and continue pipeline
        await supabase.from("flagged_for_review").insert({
          user_id: userId || null,
          image_path: rawImagePath,
          category: normalizedCategory,
          confidence: Number(confidence.toFixed(3)),
          reason: `Manual Override Applied: ${reason}`,
          status: "pending",
        });

        await logPipelineStage(supabase, {
          userId,
          stage: "niche_gate",
          status: "pass",
          latencyMs,
          metadata: {
            override: true,
            originalPassed: false,
            confidence,
            category: normalizedCategory,
          },
        });

        return {
          pass: true,
          isArtisanProduct: isArtisan,
          category: normalizedCategory,
          confidence,
          reason,
          flaggedForReview: true,
        };
      } else {
        // Gate rejection -> Log into rejected_uploads table
        await supabase.from("rejected_uploads").insert({
          user_id: userId || null,
          image_path: rawImagePath,
          category: normalizedCategory,
          confidence: Number(confidence.toFixed(3)),
          reason: reason || "Photo does not appear to be an authentic artisan craft or confidence is below threshold.",
          reviewed: false,
        });

        await logPipelineStage(supabase, {
          userId,
          stage: "niche_gate",
          status: "fail",
          latencyMs,
          errorMessage: reason,
          metadata: { confidence, category: normalizedCategory, threshold: confidenceThreshold },
        });

        return {
          pass: false,
          isArtisanProduct: isArtisan,
          category: normalizedCategory,
          confidence,
          reason: reason || "This photo does not appear to be an authentic handcrafted or artisan product.",
        };
      }
    }

    // Gate Passed!
    await logPipelineStage(supabase, {
      userId,
      stage: "niche_gate",
      status: "pass",
      latencyMs,
      metadata: { confidence, category: normalizedCategory },
    });

    return {
      pass: true,
      isArtisanProduct: true,
      category: normalizedCategory,
      confidence,
      reason,
    };
  } catch (err: any) {
    const latencyMs = Date.now() - startTime;
    await logPipelineStage(supabase, {
      userId,
      stage: "niche_gate",
      status: "fail",
      latencyMs,
      errorMessage: err.message || "Classification error",
    });
    throw err;
  }
}
