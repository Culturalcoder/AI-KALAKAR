// ==============================================================================
// AI KALAKAR: Pipeline Utilities & Shared Helpers
// ==============================================================================

import { createClient, SupabaseClient } from "@supabase/supabase-js";

export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-override-gate",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
};

/**
 * Creates an admin Supabase client with the service role key.
 * Used for database logging, storage management, and bypassing RLS when executing backend jobs.
 */
export function getSupabaseAdminClient(): SupabaseClient {
  const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";

  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY environment variables are missing.");
  }

  return createClient(supabaseUrl, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });
}

/**
 * Creates a user-scoped Supabase client respecting RLS.
 */
export function getSupabaseUserClient(authHeader?: string | null): SupabaseClient {
  const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") || "";

  return createClient(supabaseUrl, anonKey, {
    global: {
      headers: authHeader ? { Authorization: authHeader } : {},
    },
    auth: {
      persistSession: false,
    },
  });
}

/**
 * Generic retry logic with exponential backoff and jitter (max 2 retries).
 */
export async function withRetry<T>(
  fn: (attempt: number) => Promise<T>,
  maxRetries = 2,
  initialDelayMs = 600,
  operationName = "API Call"
): Promise<T> {
  let attempt = 0;
  while (true) {
    try {
      return await fn(attempt);
    } catch (err: any) {
      attempt++;
      if (attempt > maxRetries) {
        console.error(`[${operationName}] Failed after ${maxRetries} retries:`, err);
        throw err;
      }
      const delay = Math.round(initialDelayMs * Math.pow(2, attempt - 1) + Math.random() * 150);
      console.warn(`[${operationName}] Attempt ${attempt} failed: ${err.message || err}. Retrying in ${delay}ms...`);
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }
}

/**
 * Cleans markdown code fences (e.g. ```json ... ```) and safely parses JSON.
 */
export function safeParseJson<T = any>(raw: string): { success: boolean; data?: T; error?: string } {
  try {
    let clean = raw.trim();
    if (clean.startsWith("```json")) {
      clean = clean.replace(/^```json\s*/i, "");
    } else if (clean.startsWith("```")) {
      clean = clean.replace(/^```\s*/, "");
    }
    if (clean.endsWith("```")) {
      clean = clean.replace(/```$/, "");
    }
    clean = clean.trim();

    const data = JSON.parse(clean) as T;
    return { success: true, data };
  } catch (err: any) {
    return { success: false, error: err.message || "Invalid JSON format" };
  }
}

/**
 * Logs stage performance, latency, and status into the `pipeline_logs` table.
 */
export async function logPipelineStage(
  supabase: SupabaseClient,
  params: {
    userId?: string | null;
    stage: string;
    status: "pass" | "fail" | "skipped";
    latencyMs: number;
    errorMessage?: string | null;
    metadata?: Record<string, any>;
  }
): Promise<void> {
  try {
    const { error } = await supabase.from("pipeline_logs").insert({
      user_id: params.userId || null,
      stage: params.stage,
      status: params.status,
      latency_ms: params.latencyMs,
      error_message: params.errorMessage || null,
      metadata: params.metadata || {},
    });
    if (error) {
      console.warn(`[pipeline_logs] Failed to log stage ${params.stage}:`, error.message);
    }
  } catch (logErr) {
    console.warn(`[pipeline_logs] Exception while logging stage ${params.stage}:`, logErr);
  }
}

/**
 * Reads dynamic configuration from the `pipeline_config` table with a fallback default.
 */
export async function getConfigValue<T>(
  supabase: SupabaseClient,
  key: string,
  defaultValue: T
): Promise<T> {
  try {
    const { data, error } = await supabase
      .from("pipeline_config")
      .select("value")
      .eq("key", key)
      .maybeSingle();

    if (error || !data || data.value === undefined || data.value === null) {
      return defaultValue;
    }
    return data.value as T;
  } catch (_e) {
    return defaultValue;
  }
}

/**
 * Converts a Uint8Array into a Base64 string.
 */
export function uint8ArrayToBase64(bytes: Uint8Array): string {
  let binary = "";
  const len = bytes.byteLength;
  for (let i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

/**
 * Parses image dimensions (width, height) directly from image header bytes (PNG & JPEG).
 */
export function extractImageDimensions(bytes: Uint8Array): { width: number; height: number } | null {
  try {
    // PNG detection: [0x89, 0x50, 0x4E, 0x47]
    if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
      const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
      const width = view.getUint32(16, false);
      const height = view.getUint32(20, false);
      return { width, height };
    }

    // JPEG detection: [0xFF, 0xD8]
    if (bytes[0] === 0xff && bytes[1] === 0xd8) {
      let offset = 2;
      const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
      while (offset < bytes.length) {
        if (bytes[offset] !== 0xff) break;
        const marker = bytes[offset + 1];
        if (marker === 0xc0 || marker === 0xc2) { // SOF0 or SOF2
          const height = view.getUint16(offset + 5, false);
          const width = view.getUint16(offset + 7, false);
          return { width, height };
        }
        const length = view.getUint16(offset + 2, false);
        offset += 2 + length;
      }
    }
  } catch (_e) {
    // Silently continue if dimension parsing fails
  }
  return null;
}
