// ==============================================================================
// STAGE 5: UPSCALE & SHARPEN (Resolution Enhancer)
// Triggers if image is below target threshold (e.g. 1500x1500px) using Clipdrop / Cloudinary
// ==============================================================================

import { SupabaseClient } from "@supabase/supabase-js";
import {
  extractImageDimensions,
  getConfigValue,
  logPipelineStage,
  withRetry,
} from "../utils.ts";

export interface UpscaleResult {
  finalBytes: Uint8Array;
  mimeType: string;
  wasUpscaled: boolean;
  providerUsed?: string;
}

/**
 * Checks resolution and upscales the product photo to high-resolution e-commerce standards.
 */
export async function upscaleImage(
  supabase: SupabaseClient,
  params: {
    imageBytes: Uint8Array;
    mimeType?: string;
    userId?: string | null;
  }
): Promise<UpscaleResult> {
  const startTime = Date.now();
  const { imageBytes, mimeType = "image/png", userId } = params;

  const minDimension = await getConfigValue<number>(supabase, "upscale_min_dimension", 1500);

  // Check dimensions from bytes
  const dimensions = extractImageDimensions(imageBytes);
  const currentMax = dimensions ? Math.max(dimensions.width, dimensions.height) : 0;

  // If already at or above target resolution, skip upscale
  if (dimensions && currentMax >= minDimension) {
    const latencyMs = Date.now() - startTime;
    await logPipelineStage(supabase, {
      userId,
      stage: "upscale",
      status: "skipped",
      latencyMs,
      metadata: {
        reason: `Image already at or above target resolution (${dimensions.width}x${dimensions.height} >= ${minDimension})`,
      },
    });
    return {
      finalBytes: imageBytes,
      mimeType,
      wasUpscaled: false,
    };
  }

  let upscaledBytes: Uint8Array | null = null;
  let providerUsed = "";
  let lastError: Error | null = null;

  // Provider 1: Clipdrop API
  const tryClipdrop = async (): Promise<Uint8Array | null> => {
    const apiKey = Deno.env.get("CLIPDROP_API_KEY");
    if (!apiKey) return null;

    return await withRetry(
      async () => {
        const formData = new FormData();
        const blob = new Blob([imageBytes], { type: mimeType });
        formData.append("image_file", blob, "image.png");
        formData.append("target_width", "2048");
        formData.append("target_height", "2048");

        const res = await fetch("https://clipdrop-api.co/image-upscaling/v1/upscale", {
          method: "POST",
          headers: {
            "x-api-key": apiKey,
          },
          body: formData,
        });

        if (!res.ok) {
          const err = await res.text();
          throw new Error(`Clipdrop HTTP ${res.status}: ${err}`);
        }

        const buffer = await res.arrayBuffer();
        return new Uint8Array(buffer);
      },
      1,
      500,
      "Clipdrop Upscale"
    );
  };

  // Provider 2: Cloudinary Super-Resolution
  const tryCloudinaryUpscale = async (): Promise<Uint8Array | null> => {
    const cloudName = Deno.env.get("CLOUDINARY_CLOUD_NAME");
    const apiKey = Deno.env.get("CLOUDINARY_API_KEY");
    const apiSecret = Deno.env.get("CLOUDINARY_API_SECRET");

    if (!cloudName || !apiKey || !apiSecret) return null;

    return await withRetry(
      async () => {
        const timestamp = Math.round(Date.now() / 1000).toString();
        const eager = "e_upscale,q_auto:best";

        const strToSign = `eager=${eager}&timestamp=${timestamp}${apiSecret}`;
        const encoder = new TextEncoder();
        const hashBuffer = await crypto.subtle.digest("SHA-1", encoder.encode(strToSign));
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const signature = hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");

        const formData = new FormData();
        const blob = new Blob([imageBytes], { type: mimeType });
        formData.append("file", blob, "upscale.png");
        formData.append("api_key", apiKey);
        formData.append("timestamp", timestamp);
        formData.append("eager", eager);
        formData.append("signature", signature);

        const uploadRes = await fetch(
          `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`,
          { method: "POST", body: formData }
        );

        if (!uploadRes.ok) throw new Error(`Cloudinary upload failed: ${uploadRes.status}`);

        const data = await uploadRes.json();
        const resultUrl = data?.eager?.[0]?.secure_url || data?.secure_url;
        if (!resultUrl) throw new Error("No URL returned from Cloudinary");

        const dlRes = await fetch(resultUrl);
        if (!dlRes.ok) throw new Error(`Download failed: ${dlRes.status}`);

        const buffer = await dlRes.arrayBuffer();
        return new Uint8Array(buffer);
      },
      1,
      500,
      "Cloudinary Upscale"
    );
  };

  // Execute
  try {
    const clipdropRes = await tryClipdrop();
    if (clipdropRes && clipdropRes.byteLength > 0) {
      upscaledBytes = clipdropRes;
      providerUsed = "clipdrop";
    }
  } catch (e: any) {
    console.warn("[upscaleImage] Clipdrop failed:", e.message);
    lastError = e;
  }

  if (!upscaledBytes) {
    try {
      const cloudinaryRes = await tryCloudinaryUpscale();
      if (cloudinaryRes && cloudinaryRes.byteLength > 0) {
        upscaledBytes = cloudinaryRes;
        providerUsed = "cloudinary_upscale";
      }
    } catch (e: any) {
      console.warn("[upscaleImage] Cloudinary upscale failed:", e.message);
      lastError = e;
    }
  }

  const wasUpscaled = upscaledBytes !== null;
  const finalBytes = upscaledBytes || imageBytes;
  const latencyMs = Date.now() - startTime;

  await logPipelineStage(supabase, {
    userId,
    stage: "upscale",
    status: wasUpscaled ? "pass" : "skipped",
    latencyMs,
    errorMessage: lastError?.message || null,
    metadata: {
      wasUpscaled,
      providerUsed: providerUsed || "none",
      initialDimensions: dimensions,
      minDimension,
    },
  });

  return {
    finalBytes,
    mimeType,
    wasUpscaled,
    providerUsed: providerUsed || undefined,
  };
}
