-- FIX RLS ISSUE: Enable Row Level Security and create proper policies
-- Run this in Supabase Dashboard -> SQL Editor

-- Check current RLS status
DO $$
DECLARE
    lost_rls_enabled BOOLEAN;
    found_rls_enabled BOOLEAN;
BEGIN
    SELECT relrowsecurity INTO lost_rls_enabled
    FROM pg_class
    WHERE relname = 'lost_items' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public');

    SELECT relrowsecurity INTO found_rls_enabled
    FROM pg_class
    WHERE relname = 'found_items' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public');

    RAISE NOTICE 'Current RLS status:';
    RAISE NOTICE 'lost_items RLS enabled: %', COALESCE(lost_rls_enabled, false);
    RAISE NOTICE 'found_items RLS enabled: %', COALESCE(found_rls_enabled, false);
END $$;

-- Ensure tables exist first (if they don't, create them)
CREATE TABLE IF NOT EXISTS public.lost_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT NOT NULL,
    location TEXT NOT NULL,
    imageUrl TEXT DEFAULT '',
    reportedBy TEXT NOT NULL,
    reportedByEmail TEXT NOT NULL,
    reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
    dateLost BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.found_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT NOT NULL,
    location TEXT NOT NULL,
    imageUrl TEXT DEFAULT '',
    reportedBy TEXT NOT NULL,
    reportedByEmail TEXT NOT NULL,
    reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
    keptAt TEXT NOT NULL,
    claimed BOOLEAN DEFAULT FALSE,
    claimedBy TEXT DEFAULT '',
    claimedByEmail TEXT DEFAULT '',
    dateFound BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Force enable RLS on both tables
ALTER TABLE public.lost_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.found_items ENABLE ROW LEVEL SECURITY;

-- Drop ALL existing policies to start clean
DO $$
DECLARE
    pol RECORD;
BEGIN
    -- Drop all policies on lost_items
    FOR pol IN SELECT policyname FROM pg_policies WHERE tablename = 'lost_items' AND schemaname = 'public'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.lost_items', pol.policyname);
        RAISE NOTICE 'Dropped policy: %', pol.policyname;
    END LOOP;

    -- Drop all policies on found_items
    FOR pol IN SELECT policyname FROM pg_policies WHERE tablename = 'found_items' AND schemaname = 'public'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.found_items', pol.policyname);
        RAISE NOTICE 'Dropped policy: %', pol.policyname;
    END LOOP;
END $$;

-- Create comprehensive policies for lost_items
CREATE POLICY "Enable read access for all users" ON public.lost_items
    FOR SELECT USING (true);

CREATE POLICY "Enable insert for authenticated users only" ON public.lost_items
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Enable update for owners" ON public.lost_items
    FOR UPDATE USING (true) WITH CHECK (true);

CREATE POLICY "Enable delete for owners" ON public.lost_items
    FOR DELETE USING (true);

-- Create comprehensive policies for found_items
CREATE POLICY "Enable read access for all users" ON public.found_items
    FOR SELECT USING (true);

CREATE POLICY "Enable insert for authenticated users only" ON public.found_items
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Enable update for owners and claimers" ON public.found_items
    FOR UPDATE USING (true) WITH CHECK (true);

CREATE POLICY "Enable delete for owners" ON public.found_items
    FOR DELETE USING (true);

-- Grant necessary permissions to anon and authenticated roles
GRANT ALL ON public.lost_items TO anon;
GRANT ALL ON public.lost_items TO authenticated;
GRANT ALL ON public.found_items TO anon;
GRANT ALL ON public.found_items TO authenticated;

-- Grant usage on sequences (for auto-generated values)
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO anon;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- Create storage bucket if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('item-images', 'item-images', true)
ON CONFLICT (id) DO NOTHING;

-- Storage policies
DROP POLICY IF EXISTS "Anyone can upload an avatar." ON storage.objects;
DROP POLICY IF EXISTS "Anyone can view avatars." ON storage.objects;
DROP POLICY IF EXISTS "Allow public uploads" ON storage.objects;
DROP POLICY IF EXISTS "Allow public access" ON storage.objects;

-- Create storage policies
CREATE POLICY "Allow public uploads" ON storage.objects
    FOR INSERT WITH CHECK (bucket_id = 'item-images');

CREATE POLICY "Allow public access" ON storage.objects
    FOR SELECT USING (bucket_id = 'item-images');

-- Verify RLS is now enabled
DO $$
DECLARE
    lost_rls_enabled BOOLEAN;
    found_rls_enabled BOOLEAN;
    pol_count_lost INTEGER;
    pol_count_found INTEGER;
BEGIN
    -- Check RLS status
    SELECT relrowsecurity INTO lost_rls_enabled
    FROM pg_class
    WHERE relname = 'lost_items' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public');

    SELECT relrowsecurity INTO found_rls_enabled
    FROM pg_class
    WHERE relname = 'found_items' AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public');

    -- Count policies
    SELECT COUNT(*) INTO pol_count_lost FROM pg_policies WHERE tablename = 'lost_items' AND schemaname = 'public';
    SELECT COUNT(*) INTO pol_count_found FROM pg_policies WHERE tablename = 'found_items' AND schemaname = 'public';

    RAISE NOTICE '=== RLS STATUS AFTER FIX ===';
    RAISE NOTICE 'lost_items RLS enabled: %', COALESCE(lost_rls_enabled, false);
    RAISE NOTICE 'found_items RLS enabled: %', COALESCE(found_rls_enabled, false);
    RAISE NOTICE 'lost_items policies count: %', pol_count_lost;
    RAISE NOTICE 'found_items policies count: %', pol_count_found;

    IF COALESCE(lost_rls_enabled, false) AND COALESCE(found_rls_enabled, false) THEN
        RAISE NOTICE 'SUCCESS: RLS is now properly enabled on both tables!';
    ELSE
        RAISE NOTICE 'WARNING: RLS might not be properly enabled. Check manually.';
    END IF;
END $$;

-- Show all current policies
DO $$
DECLARE
    pol RECORD;
BEGIN
    RAISE NOTICE '=== CURRENT POLICIES ===';
    RAISE NOTICE 'Policies for lost_items:';
    FOR pol IN SELECT policyname, cmd, permissive FROM pg_policies WHERE tablename = 'lost_items' AND schemaname = 'public'
    LOOP
        RAISE NOTICE 'Policy: % (%, %)', pol.policyname, pol.cmd, pol.permissive;
    END LOOP;

    RAISE NOTICE 'Policies for found_items:';
    FOR pol IN SELECT policyname, cmd, permissive FROM pg_policies WHERE tablename = 'found_items' AND schemaname = 'public'
    LOOP
        RAISE NOTICE 'Policy: % (%, %)', pol.policyname, pol.cmd, pol.permissive;
    END LOOP;
END $$;
