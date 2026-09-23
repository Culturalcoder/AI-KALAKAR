// ==============================================================================
// AI KALAKAR: AI Image Enhancer & Studio Pipeline
// Supabase Edge Function Orchestrator (Deno/TypeScript)
// ==============================================================================

import { SupabaseClient } from "@supabase/supabase-js";
import {
  corsHeaders,
  getSupabaseAdminClient,
  logPipelineStage,
} from "./utils.ts";
import { classifyImage, ClassificationResult } from "./stages/classify.ts";
import { removeBackground, BackgroundRemovalResult } from "./stages/background.ts";
import { enhanceImage, EnhancementResult } from "./stages/enhance.ts";
import { upscaleImage, UpscaleResult } from "./stages/upscale.ts";

// Re-export modular functions for independent unit testing
export { classifyImage, removeBackground, enhanceImage, upscaleImage };

export interface PipelineRequest {
  image_path?: string; // Path within 'raw-uploads' bucket
  override?: boolean; // Manual override for borderline craft items
  user_id?: string; // Optional user ID passed explicitly
  backdrop?: "white" | "linen" | "amber" | "slate";
}

export interface PipelineResponse {
  success: boolean;
  category?: string;
  enhancedImageUrl?: string;
  rawImageUrl?: string;
  confidence?: number;
  rejectionReason?: string;
  photoId?: string;
  flaggedForReview?: boolean;
}

/**
 * Executes the end-to-end 6-stage AI Image Enhancer & Studio pipeline.
 */
export async function runPipeline(
  supabase: SupabaseClient,
  params: {
    rawImagePath: string;
    userId?: string | null;
    override?: boolean;
    backdrop?: "white" | "linen" | "amber" | "slate";
    rawBytes?: Uint8Array;
    mimeType?: string;
  }
): Promise<PipelineResponse> {
  const pipelineStartTime = Date.now();
  const { rawImagePath, userId, override = false, backdrop = "white" } = params;

  console.log(`[runPipeline] Initiating pipeline for ${rawImagePath} (user: ${userId || "anon"}, override: ${override})`);

  // ----------------------------------------------------------------------------
  // STAGE 1 & 2: FETCH RAW IMAGE & NICHE GATE CLASSIFICATION
  // ----------------------------------------------------------------------------
  let rawBytes = params.rawBytes;
  let mimeType = params.mimeType || "image/jpeg";

  if (!rawBytes) {
    const { data: downloadData, error: downloadError } = await supabase.storage
      .from("raw-uploads")
      .download(rawImagePath);

    if (downloadError || !downloadData) {
      throw new Error(`Failed to download raw image from storage (${rawImagePath}): ${downloadError?.message}`);
    }

    rawBytes = new Uint8Array(await downloadData.arrayBuffer());
    mimeType = downloadData.type || (rawImagePath.endsWith(".png") ? "image/png" : "image/jpeg");
  }

  const classification = await classifyImage(supabase, {
    rawImagePath,
    rawBytes,
    mimeType,
    userId,
    override,
  });

  // Rejection check: If Niche Gate rejected and override is false, halt pipeline immediately
  if (!classification.pass) {
    console.warn(`[runPipeline] Image rejected by Niche Gate: ${classification.reason}`);
    return {
      success: false,
      category: classification.category,
      confidence: classification.confidence,
      rejectionReason: classification.reason,
    };
  }

  // ----------------------------------------------------------------------------
  // STAGE 3: BACKGROUND REMOVAL (Precision Cutout)
  // ----------------------------------------------------------------------------
  const bgResult = await removeBackground(supabase, {
    rawBytes,
    mimeType,
    userId,
    rawImagePath,
  });

  // ----------------------------------------------------------------------------
  // STAGE 4: AI ENHANCEMENT (Lighting, Seamless Backdrop, Soft Ground Shadow)
  // ----------------------------------------------------------------------------
  const enhanceResult = await enhanceImage(supabase, {
    cutoutBytes: bgResult.cutoutBytes,
    category: classification.category,
    userId,
    backdropStyle: backdrop,
  });

  // ----------------------------------------------------------------------------
  // STAGE 5: UPSCALE & SHARPEN (Optional resolution boost)
  // ----------------------------------------------------------------------------
  const upscaleResult = await upscaleImage(supabase, {
    imageBytes: enhanceResult.enhancedBytes,
    mimeType: enhanceResult.mimeType,
    userId,
  });

  // ----------------------------------------------------------------------------
  // STAGE 6: FINALIZE & UPLOAD TO 'enhanced-products' BUCKET
  // ----------------------------------------------------------------------------
  const timestamp = Date.now();
  const folder = userId || "anonymous";
  const enhancedStoragePath = `artisan-products/${folder}/${timestamp}_enhanced.png`;

  const { error: uploadError } = await supabase.storage
    .from("enhanced-products")
    .upload(enhancedStoragePath, upscaleResult.finalBytes, {
      contentType: "image/png",
      upsert: true,
    });

  if (uploadError) {
    throw new Error(`Failed to upload final enhanced image: ${uploadError.message}`);
  }

  // Get public or signed URL
  const { data: publicUrlData } = supabase.storage
    .from("enhanced-products")
    .getPublicUrl(enhancedStoragePath);

  const enhancedImageUrl = publicUrlData?.publicUrl || "";

  // Insert into product_photos table
  const { data: photoRow, error: photoInsertError } = await supabase
    .from("product_photos")
    .insert({
      user_id: userId || null,
      category: classification.category,
      raw_image_path: rawImagePath,
      enhanced_image_path: enhancedStoragePath,
      confidence: Number((classification.confidence || 0.95).toFixed(3)),
      status: "ready",
    })
    .select("id")
    .single();

  if (photoInsertError) {
    console.warn("[runPipeline] Failed to insert product_photos row:", photoInsertError.message);
  }

  const totalLatencyMs = Date.now() - pipelineStartTime;
  await logPipelineStage(supabase, {
    userId,
    stage: "pipeline_complete",
    status: "pass",
    latencyMs: totalLatencyMs,
    metadata: {
      category: classification.category,
      enhancedStoragePath,
      flagged: classification.flaggedForReview || false,
    },
  });

  console.log(`[runPipeline] Pipeline completed successfully in ${totalLatencyMs}ms`);

  return {
    success: true,
    category: classification.category,
    enhancedImageUrl,
    rawImageUrl: rawImagePath,
    confidence: classification.confidence,
    photoId: photoRow?.id || undefined,
    flaggedForReview: classification.flaggedForReview,
  };
}

// ------------------------------------------------------------------------------
// HTTP SERVER (Deno runtime handler)
// ------------------------------------------------------------------------------

Deno.serve(async (req: Request) => {
  // 1. Handle CORS Preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const supabase = getSupabaseAdminClient();

  try {
    // 2. Resolve Authenticated User from JWT header
    const authHeader = req.headers.get("Authorization");
    let authenticatedUserId: string | null = null;

    if (authHeader) {
      try {
        const token = authHeader.replace(/^Bearer\s+/i, "");
        const { data: userData } = await supabase.auth.getUser(token);
        if (userData?.user?.id) {
          authenticatedUserId = userData.user.id;
        }
      } catch (_authErr) {
        // Continue with anon user
      }
    }

    const contentType = req.headers.get("content-type") || "";

    // 3. Handle Multipart File Upload (Direct camera upload)
    if (contentType.includes("multipart/form-data")) {
      const formData = await req.formData();
      const file = formData.get("file") as File | null;
      const override = formData.get("override") === "true";
      const backdrop = (formData.get("backdrop") as any) || "white";
      const explicitUserId = (formData.get("user_id") as string) || authenticatedUserId;

      if (!file) {
        return new Response(
          JSON.stringify({ success: false, error: "Missing 'file' field in multipart request" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const rawBytes = new Uint8Array(await file.arrayBuffer());
      const timestamp = Date.now();
      const safeName = file.name ? file.name.replace(/[^a-zA-Z0-9._-]/g, "_") : "photo.jpg";
      const rawStoragePath = `uploads/${explicitUserId || "anonymous"}/${timestamp}_${safeName}`;

      // Upload raw photo to 'raw-uploads' bucket
      const { error: rawUploadErr } = await supabase.storage
        .from("raw-uploads")
        .upload(rawStoragePath, rawBytes, {
          contentType: file.type || "image/jpeg",
          upsert: true,
        });

      if (rawUploadErr) {
        throw new Error(`Failed to save raw upload to storage: ${rawUploadErr.message}`);
      }

      const result = await runPipeline(supabase, {
        rawImagePath: rawStoragePath,
        rawBytes,
        mimeType: file.type || "image/jpeg",
        userId: explicitUserId,
        override,
        backdrop,
      });

      return new Response(JSON.stringify(result), {
        status: result.success ? 200 : 422,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 4. Handle JSON Payload (Path in raw-uploads or Storage Webhook Trigger)
    const bodyText = await req.text();
    let bodyJson: Record<string, any> = {};
    if (bodyText) {
      try {
        bodyJson = JSON.parse(bodyText);
      } catch (_e) {
        return new Response(
          JSON.stringify({ success: false, error: "Malformed JSON body" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }
    }

    // Check if triggered via Supabase Storage Webhook
    // Storage webhooks pass { record: { name: '...', bucket_id: 'raw-uploads' } }
    let rawImagePath = bodyJson.image_path;
    let override = Boolean(bodyJson.override || req.headers.get("x-override-gate") === "true");
    const backdrop = bodyJson.backdrop || "white";
    const explicitUserId = bodyJson.user_id || authenticatedUserId;

    if (!rawImagePath && bodyJson.record?.name) {
      rawImagePath = bodyJson.record.name;
    }

    if (!rawImagePath) {
      return new Response(
        JSON.stringify({
          success: false,
          error: "Missing required 'image_path' parameter in request body",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const result = await runPipeline(supabase, {
      rawImagePath,
      userId: explicitUserId,
      override,
      backdrop,
    });

    return new Response(JSON.stringify(result), {
      status: result.success ? 200 : 422,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err: any) {
    console.error("[image-enhancer-pipeline] Unhandled exception:", err);

    // Rate-limit resilience check: if external APIs fail due to rate limits (HTTP 429), enqueue into retry queue
    if (err.message && (err.message.includes("429") || err.message.toLowerCase().includes("rate limit") || err.message.toLowerCase().includes("quota"))) {
      try {
        await supabase.from("pipeline_retry_queue").insert({
          image_path: "unhandled",
          stage: "external_rate_limit",
          last_error: err.message,
          next_retry_at: new Date(Date.now() + 60000).toISOString(),
          status: "pending",
        });
      } catch (_qErr) {
        // queue insert failed
      }
    }

    return new Response(
      JSON.stringify({
        success: false,
        error: err.message || "Internal pipeline error",
      }),
      {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  }
});
