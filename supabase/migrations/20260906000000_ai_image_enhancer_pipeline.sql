-- ==============================================================================
-- AI KALAKAR: AI Image Enhancer & Studio Pipeline Migration
-- Database schema for artisan handicraft image verification, enhancement & logging
-- ==============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------------------------
-- 1. STORAGE BUCKETS ("raw-uploads" & "enhanced-products")
-- ------------------------------------------------------------------------------

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES 
  ('raw-uploads', 'raw-uploads', true, 26214400, ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/heic']),
  ('enhanced-products', 'enhanced-products', true, 26214400, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET
  public = EXCLUDED.public,
  file_size_limit = EXCLUDED.file_size_limit,
  allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Storage RLS: raw-uploads
CREATE POLICY "Allow authenticated users to upload raw photos"
  ON storage.objects FOR INSERT
  TO authenticated
  WITH CHECK (bucket_id = 'raw-uploads');

CREATE POLICY "Allow public read of raw uploads"
  ON storage.objects FOR SELECT
  TO public
  USING (bucket_id = 'raw-uploads');

CREATE POLICY "Allow users to manage own raw uploads"
  ON storage.objects FOR ALL
  TO authenticated
  USING (bucket_id = 'raw-uploads' AND (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid() IS NOT NULL));

-- Storage RLS: enhanced-products
CREATE POLICY "Allow public read of enhanced products"
  ON storage.objects FOR SELECT
  TO public
  USING (bucket_id = 'enhanced-products');

CREATE POLICY "Allow authenticated or service role to write enhanced photos"
  ON storage.objects FOR INSERT
  TO authenticated, service_role
  WITH CHECK (bucket_id = 'enhanced-products');

CREATE POLICY "Allow service role full access to enhanced products"
  ON storage.objects FOR ALL
  TO service_role
  USING (bucket_id = 'enhanced-products');


-- ------------------------------------------------------------------------------
-- 2. PIPELINE CONFIG TABLE (Dynamic thresholds & toggles)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.pipeline_config (
  key TEXT PRIMARY KEY,
  value JSONB NOT NULL,
  description TEXT,
  updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- Seed default pipeline configurations
INSERT INTO public.pipeline_config (key, value, description)
VALUES
  ('niche_gate_enabled', 'true'::jsonb, 'Toggle niche classification gate on/off without redeploying'),
  ('confidence_threshold', '0.70'::jsonb, 'Minimum confidence score required to pass craft classification'),
  ('upscale_min_dimension', '1500'::jsonb, 'Target minimum dimension (px) before triggering super-resolution'),
  ('primary_bg_remover', '"photoroom"'::jsonb, 'Primary background removal provider: photoroom, removebg, or cloudinary'),
  ('allowed_categories', '["textile", "pottery", "jewelry", "woodwork", "embroidery", "weaving", "basketry", "leatherwork", "metalwork", "other"]'::jsonb, 'Allowed craft niches'),
  ('max_retries', '2'::jsonb, 'External API call maximum retries')
ON CONFLICT (key) DO UPDATE SET
  description = EXCLUDED.description;

-- Enable RLS for pipeline_config
ALTER TABLE public.pipeline_config ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow anyone to read pipeline config"
  ON public.pipeline_config FOR SELECT
  TO authenticated, anon, service_role
  USING (true);

CREATE POLICY "Allow service role to modify config"
  ON public.pipeline_config FOR ALL
  TO service_role
  USING (true);


-- ------------------------------------------------------------------------------
-- 3. REJECTED UPLOADS TABLE (Gate Rejections)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.rejected_uploads (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  image_path TEXT NOT NULL,
  category TEXT,
  confidence NUMERIC(4, 3),
  reason TEXT NOT NULL,
  reviewed BOOLEAN DEFAULT false NOT NULL,
  timestamp TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rejected_uploads_user ON public.rejected_uploads(user_id);
CREATE INDEX IF NOT EXISTS idx_rejected_uploads_timestamp ON public.rejected_uploads(timestamp DESC);

ALTER TABLE public.rejected_uploads ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Artisans can view their own rejections"
  ON public.rejected_uploads FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Service role can manage rejected uploads"
  ON public.rejected_uploads FOR ALL
  TO service_role
  USING (true);


-- ------------------------------------------------------------------------------
-- 4. FLAGGED FOR REVIEW TABLE (Manual Overrides)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.flagged_for_review (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  image_path TEXT NOT NULL,
  category TEXT,
  confidence NUMERIC(4, 3),
  reason TEXT NOT NULL,
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'dismissed')),
  timestamp TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_flagged_user ON public.flagged_for_review(user_id);
CREATE INDEX IF NOT EXISTS idx_flagged_status ON public.flagged_for_review(status);

ALTER TABLE public.flagged_for_review ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Artisans can view their own flagged submissions"
  ON public.flagged_for_review FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Service role can manage flagged submissions"
  ON public.flagged_for_review FOR ALL
  TO service_role
  USING (true);


-- ------------------------------------------------------------------------------
-- 5. PRODUCT PHOTOS TABLE (Final Enhanced Images)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.product_photos (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  category TEXT NOT NULL,
  raw_image_path TEXT NOT NULL,
  enhanced_image_path TEXT NOT NULL,
  confidence NUMERIC(4, 3),
  status TEXT DEFAULT 'ready' CHECK (status IN ('processing', 'ready', 'failed')),
  created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_photos_user ON public.product_photos(user_id);
CREATE INDEX IF NOT EXISTS idx_product_photos_created ON public.product_photos(created_at DESC);

ALTER TABLE public.product_photos ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Artisans can view their own product photos"
  ON public.product_photos FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Public can view ready product photos"
  ON public.product_photos FOR SELECT
  TO anon
  USING (status = 'ready');

CREATE POLICY "Artisans can insert their product photos"
  ON public.product_photos FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role has full access to product photos"
  ON public.product_photos FOR ALL
  TO service_role
  USING (true);


-- ------------------------------------------------------------------------------
-- 6. PIPELINE LOGS TABLE (Performance & Latency Tracking)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.pipeline_logs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  stage TEXT NOT NULL, -- e.g. 'niche_gate', 'bg_removal', 'enhancement', 'upscale', 'pipeline_complete'
  status TEXT NOT NULL, -- 'pass', 'fail', 'skipped'
  latency_ms INTEGER NOT NULL,
  error_message TEXT,
  metadata JSONB DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pipeline_logs_stage ON public.pipeline_logs(stage);
CREATE INDEX IF NOT EXISTS idx_pipeline_logs_status ON public.pipeline_logs(status);
CREATE INDEX IF NOT EXISTS idx_pipeline_logs_created ON public.pipeline_logs(created_at DESC);

ALTER TABLE public.pipeline_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Artisans can view their own pipeline logs"
  ON public.pipeline_logs FOR SELECT
  TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "Service role can insert and view pipeline logs"
  ON public.pipeline_logs FOR ALL
  TO service_role
  USING (true);


-- ------------------------------------------------------------------------------
-- 7. PIPELINE RETRY QUEUE TABLE (Rate-limiting and asynchronous retries)
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.pipeline_retry_queue (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  image_path TEXT NOT NULL,
  stage TEXT NOT NULL,
  payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  attempts INTEGER DEFAULT 0 NOT NULL,
  max_attempts INTEGER DEFAULT 3 NOT NULL,
  last_error TEXT,
  next_retry_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
  created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_retry_queue_status ON public.pipeline_retry_queue(status, next_retry_at);

ALTER TABLE public.pipeline_retry_queue ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Service role manages retry queue"
  ON public.pipeline_retry_queue FOR ALL
  TO service_role
  USING (true);

-- ------------------------------------------------------------------------------
-- Helper function to trigger updated_at
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_pipeline_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pipeline_config_timestamp ON public.pipeline_config;
CREATE TRIGGER trg_pipeline_config_timestamp
BEFORE UPDATE ON public.pipeline_config
FOR EACH ROW EXECUTE FUNCTION update_pipeline_config_timestamp();
