// ==============================================================================
// STAGE 3: BACKGROUND REMOVAL (Precision Cutout)
// Multi-provider support: PhotoRoom, Remove.bg, Cloudinary, and self-hosted rembg fallback
// ==============================================================================

import { SupabaseClient } from "@supabase/supabase-js";
import { getConfigValue, logPipelineStage, withRetry } from "../utils.ts";

export interface BackgroundRemovalResult {
  cutoutBytes: Uint8Array;
  mimeType: string;
  tempStoragePath?: string;
  providerUsed: string;
}

/**
 * Executes precision background removal on product images with automated failover chaining.
 */
export async function removeBackground(
  supabase: SupabaseClient,
  params: {
    rawBytes: Uint8Array;
    mimeType?: string;
    userId?: string | null;
    rawImagePath: string;
  }
): Promise<BackgroundRemovalResult> {
  const startTime = Date.now();
  const { rawBytes, mimeType = "image/jpeg", userId, rawImagePath } = params;

  const primaryProvider = await getConfigValue<string>(supabase, "primary_bg_remover", "removebg");

  let cutoutBytes: Uint8Array | null = null;
  let providerUsed = "";
  let lastError: Error | null = null;

  // Provider 1: PhotoRoom API (Using AI Kalakar / PhotoRoom key: sk_pr_...)
  const tryPhotoRoom = async (): Promise<Uint8Array | null> => {
    const apiKey = Deno.env.get("PHOTOROOM_API_KEY") || Deno.env.get("PHOTOROOM_SANDBOX_KEY");
    if (!apiKey) return null;

    return await withRetry(
      async () => {
        const formData = new FormData();
        const blob = new Blob([rawBytes], { type: mimeType });
        formData.append("image_file", blob, "raw_product.jpg");

        const res = await fetch("https://sdk.photoroom.com/v1/segment", {
          method: "POST",
          headers: {
            "x-api-key": apiKey,
            "Accept": "image/png",
          },
          body: formData,
        });

        if (!res.ok) {
          const errText = await res.text();
          throw new Error(`PhotoRoom HTTP ${res.status}: ${errText}`);
        }

        const buffer = await res.arrayBuffer();
        return new Uint8Array(buffer);
      },
      2,
      500,
      "PhotoRoom BG Removal"
    );
  };

  // Strict Provider: Remove.bg API Key (ueACXwCv2kaLtkAvr39bvspb)
  const tryRemoveBg = async (): Promise<Uint8Array | null> => {
    const apiKey = Deno.env.get("REMOVEBG_API_KEY") || "ueACXwCv2kaLtkAvr39bvspb";
    if (!apiKey) {
      throw new Error("Remove.bg API key is missing");
    }

    return await withRetry(
      async () => {
        const formData = new FormData();
        const blob = new Blob([rawBytes], { type: mimeType });
        formData.append("image_file", blob, "raw_product.jpg");
        formData.append("size", "auto");
        formData.append("format", "png");

        const res = await fetch("https://api.remove.bg/v1.0/removebg", {
          method: "POST",
          headers: {
            "X-Api-Key": apiKey,
          },
          body: formData,
        });

        if (!res.ok) {
          const errText = await res.text();
          throw new Error(`Remove.bg HTTP ${res.status}: ${errText}`);
        }

        const buffer = await res.arrayBuffer();
        return new Uint8Array(buffer);
      },
      2,
      500,
      "Remove.bg BG Removal"
    );
  };

  // Execution with Remove.bg API strictly
  const providersToTry = [
    { name: "removebg", fn: tryRemoveBg }
  ];

  for (const p of providersToTry) {
    try {
      const res = await p.fn();
      if (res && res.byteLength > 0) {
        cutoutBytes = res;
        providerUsed = p.name;
        break;
      }
    } catch (err: any) {
      console.warn(`[removeBackground] Provider ${p.name} failed:`, err.message || err);
      lastError = err;
    }
  }

  // Graceful fallback to original image if all removal providers fail
  if (!cutoutBytes) {
    console.warn("[removeBackground] All background removal providers failed or keys not set. Falling back to original image.");
    cutoutBytes = rawBytes;
    providerUsed = "original_passthrough";
  }

  // Store intermediate cutout in "enhanced-products" bucket under temp/cutouts/
  let tempStoragePath = "";
  try {
    const timestamp = Date.now();
    const folder = userId || "anonymous";
    tempStoragePath = `temp/cutouts/${folder}/${timestamp}_cutout.png`;

    const { error: uploadError } = await supabase.storage
      .from("enhanced-products")
      .upload(tempStoragePath, cutoutBytes, {
        contentType: "image/png",
        upsert: true,
      });

    if (uploadError) {
      console.warn("[removeBackground] Failed to store intermediate cutout to storage:", uploadError.message);
    }
  } catch (storageErr) {
    console.warn("[removeBackground] Intermediate storage exception:", storageErr);
  }

  const latencyMs = Date.now() - startTime;
  await logPipelineStage(supabase, {
    userId,
    stage: "bg_removal",
    status: providerUsed !== "original_passthrough" ? "pass" : "skipped",
    latencyMs,
    errorMessage: lastError?.message || null,
    metadata: {
      providerUsed,
      cutoutSize: cutoutBytes.byteLength,
      tempStoragePath,
    },
  });

  return {
    cutoutBytes,
    mimeType: "image/png",
    tempStoragePath,
    providerUsed,
  };
}
