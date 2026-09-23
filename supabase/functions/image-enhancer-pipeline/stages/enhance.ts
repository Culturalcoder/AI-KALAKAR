// ==============================================================================
// STAGE 4: AI ENHANCEMENT (Studio Lighting, Seamless Backdrop, Polish)
// Uses Gemini Image Studio + Cloudinary Studio Composition fallback
// ==============================================================================

import { SupabaseClient } from "@supabase/supabase-js";
import {
  logPipelineStage,
  uint8ArrayToBase64,
  withRetry,
} from "../utils.ts";

export interface EnhancementResult {
  enhancedBytes: Uint8Array;
  mimeType: string;
  providerUsed: string;
}

/**
 * Enhances the cutout craft product with professional e-commerce studio lighting,
 * clean backdrop, soft grounding drop shadow, and micro-contrast clarity.
 */
export async function enhanceImage(
  supabase: SupabaseClient,
  params: {
    cutoutBytes: Uint8Array;
    category?: string;
    userId?: string | null;
    backdropStyle?: "white" | "linen" | "amber" | "slate";
  }
): Promise<EnhancementResult> {
  const startTime = Date.now();
  const { cutoutBytes, category = "handicraft", userId, backdropStyle = "white" } = params;

  let enhancedBytes: Uint8Array | null = null;
  let providerUsed = "";
  let lastError: Error | null = null;

  const studioPrompt = `
Place this authentic handcrafted ${category} artisan product on a clean white e-commerce studio background.
Even out the lighting smoothly, remove harsh reflections and uneven shadows, and add a soft, natural grounding contact shadow directly beneath the product base.
Preserve the genuine, rich handmade textures, true natural colors, and fine details of the craft. Professional e-commerce catalog studio photo.
`.trim();

  // Provider 1: Gemini Image Studio ("gemini-2.5-flash-image" / Imagen / Multimodal generation)
  const tryGeminiEnhance = async (): Promise<Uint8Array | null> => {
    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) return null;

    return await withRetry(
      async () => {
        // We attempt the Gemini image editing/generation endpoint
        const base64Data = uint8ArrayToBase64(cutoutBytes);
        const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateImages?key=${apiKey}`;

        const body = {
          prompt: studioPrompt,
          image: {
            imageBytes: base64Data,
          },
          sampleCount: 1,
          aspectRatio: "1:1",
          outputMimeType: "image/png",
        };

        const res = await fetch(endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });

        if (res.ok) {
          const data = await res.json();
          const b64 =
            data?.generatedImages?.[0]?.image?.imageBytes ||
            data?.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;

          if (b64) {
            const binary = atob(b64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
              bytes[i] = binary.charCodeAt(i);
            }
            return bytes;
          }
        }
        return null;
      },
      1,
      500,
      "Gemini Studio Enhance"
    );
  };

  // Provider 2: Cloudinary Studio Composition (Generative Fill / Studio Background)
  const tryCloudinaryEnhance = async (): Promise<Uint8Array | null> => {
    const cloudName = Deno.env.get("CLOUDINARY_CLOUD_NAME");
    const apiKey = Deno.env.get("CLOUDINARY_API_KEY");
    const apiSecret = Deno.env.get("CLOUDINARY_API_SECRET");

    if (!cloudName || !apiKey || !apiSecret) return null;

    return await withRetry(
      async () => {
        const timestamp = Math.round(Date.now() / 1000).toString();
        // E-commerce studio transformation:
        // c_pad,b_white,e_dropshadow,e_improve,q_auto:best
        const transformation = "c_pad,w_1500,h_1500,b_white,e_dropshadow:50,e_improve";

        const strToSign = `eager=${transformation}&timestamp=${timestamp}${apiSecret}`;
        const encoder = new TextEncoder();
        const hashBuffer = await crypto.subtle.digest("SHA-1", encoder.encode(strToSign));
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const signature = hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");

        const formData = new FormData();
        const blob = new Blob([cutoutBytes], { type: "image/png" });
        formData.append("file", blob, "cutout.png");
        formData.append("api_key", apiKey);
        formData.append("timestamp", timestamp);
        formData.append("eager", transformation);
        formData.append("signature", signature);

        const uploadRes = await fetch(
          `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`,
          { method: "POST", body: formData }
        );

        if (!uploadRes.ok) {
          throw new Error(`Cloudinary enhance failed: HTTP ${uploadRes.status}`);
        }

        const data = await uploadRes.json();
        const resultUrl = data?.eager?.[0]?.secure_url || data?.secure_url;
        if (!resultUrl) throw new Error("No URL in Cloudinary response");

        const downloadRes = await fetch(resultUrl);
        if (!downloadRes.ok) throw new Error(`Download failed: HTTP ${downloadRes.status}`);

        const buffer = await downloadRes.arrayBuffer();
        return new Uint8Array(buffer);
      },
      1,
      500,
      "Cloudinary Studio Composition"
    );
  };

  // Try Gemini first, then Cloudinary Studio
  try {
    const geminiRes = await tryGeminiEnhance();
    if (geminiRes && geminiRes.byteLength > 0) {
      enhancedBytes = geminiRes;
      providerUsed = "gemini_studio";
    }
  } catch (err: any) {
    console.warn("[enhanceImage] Gemini enhance error:", err.message);
    lastError = err;
  }

  if (!enhancedBytes) {
    try {
      const cloudinaryRes = await tryCloudinaryEnhance();
      if (cloudinaryRes && cloudinaryRes.byteLength > 0) {
        enhancedBytes = cloudinaryRes;
        providerUsed = "cloudinary_studio";
      }
    } catch (err: any) {
      console.warn("[enhanceImage] Cloudinary studio composition error:", err.message);
      lastError = err;
    }
  }

  // Graceful fallback to cutout bytes
  if (!enhancedBytes) {
    enhancedBytes = cutoutBytes;
    providerUsed = "cutout_passthrough";
  }

  const latencyMs = Date.now() - startTime;
  await logPipelineStage(supabase, {
    userId,
    stage: "enhancement",
    status: providerUsed !== "cutout_passthrough" ? "pass" : "skipped",
    latencyMs,
    errorMessage: lastError?.message || null,
    metadata: {
      providerUsed,
      sizeBytes: enhancedBytes.byteLength,
      backdropStyle,
    },
  });

  return {
    enhancedBytes,
    mimeType: "image/png",
    providerUsed,
  };
}
