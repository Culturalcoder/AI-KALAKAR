# AI Kalakar: AI Image Enhancer & Studio Pipeline (Supabase)

This directory contains the database migration, storage provisioning, and modular Edge Function code for the 6-stage AI Image Enhancer & Studio backend pipeline.

---

## Architecture & Stages

1. **Upload Trigger**: Mobile camera photo is saved into the `raw-uploads` bucket.
2. **Niche Gate (`classifyImage`)**: Gemini 2.5 Flash classifies whether the image is an authentic artisan product with confidence >= 0.70 (configurable in `pipeline_config`). Rejections logged to `rejected_uploads`; manual overrides to `flagged_for_review`.
3. **Background Removal (`removeBackground`)**: PhotoRoom API (using `sk_pr_...`), Remove.bg, Cloudinary, and self-hosted Rembg with automated fallback. Cutouts saved to `enhanced-products/temp/cutouts/`.
4. **AI Enhancement (`enhanceImage`)**: Studio lighting, crisp white e-commerce backdrop, soft grounding contact shadow, and natural texture enhancement.
5. **Upscale / Sharpen (`upscaleImage`)**: Clipdrop or Cloudinary super-resolution if resolution < 1500px.
6. **Finalize**: Final asset uploaded to `enhanced-products`, cataloged in `product_photos`, and a unified response returned to the app.

---

## Deployment & Setup Guide

### 1. Apply the Database Migration
Run the SQL migration in your Supabase SQL Editor or via CLI:

```bash
# Via Supabase CLI:
supabase db push

# Or execute the SQL file directly in Supabase SQL Editor:
# supabase/migrations/20260906000000_ai_image_enhancer_pipeline.sql
```

This sets up:
- Storage buckets: `raw-uploads` & `enhanced-products`
- Tables: `pipeline_config`, `rejected_uploads`, `flagged_for_review`, `product_photos`, `pipeline_logs`, `pipeline_retry_queue`
- Row Level Security (RLS) policies for artisan data protection

---

### 2. Configure Edge Function Secrets
Set your secrets using the Supabase CLI or dashboard:

```bash
# Copy template and fill secrets
cp supabase/.env.example supabase/.env

# Push secrets to your Supabase project:
supabase secrets set --env-file supabase/.env
```

Or individual commands:
```bash
supabase secrets set GEMINI_API_KEY=AQ.Ab8RN6L_S_fDO57ZMsBbVQ4ZzqqNWSHTc6AEUody9yhDqgM8DA
supabase secrets set PHOTOROOM_API_KEY=sk_pr_aikalakar_1f50b971edc3a7bc7cbf3d3f497ca7f7932cdf17
supabase secrets set CLOUDINARY_API_KEY=918318891644845
supabase secrets set CLOUDINARY_API_SECRET=_e3Zf8mOsVlJHTNijTiFrASmlEM
supabase secrets set CLOUDINARY_CLOUD_NAME=llxyvvqp
```

---

### 3. Deploy the Edge Function

```bash
supabase functions deploy image-enhancer-pipeline --no-verify-jwt
```

---

### 4. Invoking the Function

#### Option A: Direct JSON Request (After saving to `raw-uploads`)
```bash
curl -X POST 'https://<your-project-ref>.supabase.co/functions/v1/image-enhancer-pipeline' \
  -H 'Authorization: Bearer <user_jwt_or_anon_key>' \
  -H 'Content-Type: application/json' \
  -d '{
    "image_path": "uploads/user_123/terracotta_pot.jpg",
    "override": false,
    "backdrop": "white"
  }'
```

#### Option B: Direct Multipart Form Upload (Testing from Mobile / Postman)
```bash
curl -X POST 'https://<your-project-ref>.supabase.co/functions/v1/image-enhancer-pipeline' \
  -H 'Authorization: Bearer <user_jwt_or_anon_key>' \
  -F 'file=@pottery_sample.jpg;type=image/jpeg' \
  -F 'override=false' \
  -F 'backdrop=white'
```

#### Unified Response:
```json
{
  "success": true,
  "category": "pottery",
  "enhancedImageUrl": "https://<your-project>.supabase.co/storage/v1/object/public/enhanced-products/artisan-products/user_123/1725612345678_enhanced.png",
  "confidence": 0.94,
  "photoId": "e3b0c442-98fc-1c14-9afc-000000000000"
}
```

Or on rejection:
```json
{
  "success": false,
  "category": "other",
  "confidence": 0.12,
  "rejectionReason": "Photo contains an electronic laptop screen. AI Kalakar is exclusively for handcrafted artisan creations."
}
```

---

### 5. Runtime Configuration via SQL
Change settings dynamically without redeploying code:

```sql
-- Disable gate temporarily for testing:
UPDATE public.pipeline_config SET value = 'false'::jsonb WHERE key = 'niche_gate_enabled';

-- Lower or raise confidence threshold:
UPDATE public.pipeline_config SET value = '0.65'::jsonb WHERE key = 'confidence_threshold';
```
