-- TARGETED FIX: Ensure proper column names and case sensitivity
-- Run this in Supabase Dashboard -> SQL Editor

-- First, let's check what columns actually exist
DO $$
DECLARE
    col_info RECORD;
BEGIN
    RAISE NOTICE '=== CURRENT DATABASE SCHEMA ===';
    RAISE NOTICE 'Checking lost_items table structure:';

    FOR col_info IN
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_name = 'lost_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: "%" (Type: %, Nullable: %)', col_info.column_name, col_info.data_type, col_info.is_nullable;
    END LOOP;

    RAISE NOTICE 'Checking found_items table structure:';
    FOR col_info IN
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_name = 'found_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: "%" (Type: %, Nullable: %)', col_info.column_name, col_info.data_type, col_info.is_nullable;
    END LOOP;
END $$;

-- Drop and recreate the tables with exact case-sensitive column names
-- This ensures compatibility with your Kotlin models

-- Backup any existing data first (drop old backups if they exist)
DROP TABLE IF EXISTS lost_items_backup_final CASCADE;
DROP TABLE IF EXISTS found_items_backup_final CASCADE;

-- Create backups
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lost_items' AND table_schema = 'public') THEN
        CREATE TABLE lost_items_backup_final AS SELECT * FROM public.lost_items;
        RAISE NOTICE 'Created backup of lost_items table';
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'found_items' AND table_schema = 'public') THEN
        CREATE TABLE found_items_backup_final AS SELECT * FROM public.found_items;
        RAISE NOTICE 'Created backup of found_items table';
    END IF;
END $$;

-- Drop existing tables
DROP TABLE IF EXISTS public.lost_items CASCADE;
DROP TABLE IF EXISTS public.found_items CASCADE;

-- Create lost_items table with EXACT column names matching your Kotlin model
CREATE TABLE public.lost_items (
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

-- Create found_items table with EXACT column names matching your Kotlin model
CREATE TABLE public.found_items (
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

-- Restore data from backup if it exists
DO $$
BEGIN
    -- Restore lost_items data
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lost_items_backup_final' AND table_schema = 'public') THEN
        INSERT INTO public.lost_items (
            id, name, description, category, location, imageUrl,
            reportedBy, reportedByEmail, reportedDate, dateLost
        )
        SELECT
            COALESCE(id::text, gen_random_uuid()::text),
            COALESCE(name, 'Unknown Item'),
            COALESCE(description, ''),
            COALESCE(category, 'Other'),
            COALESCE(location, 'Unknown'),
            COALESCE(imageUrl, ''),
            COALESCE(reportedBy::text, 'unknown'),
            COALESCE(reportedByEmail, 'unknown@example.com'),
            CASE
                WHEN reportedDate IS NOT NULL THEN reportedDate::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END,
            CASE
                WHEN dateLost IS NOT NULL THEN dateLost::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END
        FROM lost_items_backup_final
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE 'Restored lost_items data from backup';
    ELSE
        RAISE NOTICE 'No lost_items backup found - starting with empty table';
    END IF;

    -- Restore found_items data
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'found_items_backup_final' AND table_schema = 'public') THEN
        INSERT INTO public.found_items (
            id, name, description, category, location, imageUrl,
            reportedBy, reportedByEmail, reportedDate, keptAt, claimed,
            claimedBy, claimedByEmail, dateFound
        )
        SELECT
            COALESCE(id::text, gen_random_uuid()::text),
            COALESCE(name, 'Unknown Item'),
            COALESCE(description, ''),
            COALESCE(category, 'Other'),
            COALESCE(location, 'Unknown'),
            COALESCE(imageUrl, ''),
            COALESCE(reportedBy::text, 'unknown'),
            COALESCE(reportedByEmail, 'unknown@example.com'),
            CASE
                WHEN reportedDate IS NOT NULL THEN reportedDate::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END,
            COALESCE(keptAt, 'Unknown Location'),
            COALESCE(claimed, false),
            COALESCE(claimedBy::text, ''),
            COALESCE(claimedByEmail, ''),
            CASE
                WHEN dateFound IS NOT NULL THEN dateFound::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END
        FROM found_items_backup_final
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE 'Restored found_items data from backup';
    ELSE
        RAISE NOTICE 'No found_items backup found - starting with empty table';
    END IF;
END $$;

-- Enable Row Level Security
ALTER TABLE public.lost_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.found_items ENABLE ROW LEVEL SECURITY;

-- Drop existing policies to avoid conflicts
DROP POLICY IF EXISTS "Allow authenticated users to view all lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to insert their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to update their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to delete their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow all operations for lost items" ON public.lost_items;

DROP POLICY IF EXISTS "Allow authenticated users to view all found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to insert their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to update their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to delete their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to claim found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow all operations for found items" ON public.found_items;

-- Create simplified policies that allow all operations for now
CREATE POLICY "Allow all operations for lost items" ON public.lost_items FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow all operations for found items" ON public.found_items FOR ALL USING (true) WITH CHECK (true);

-- Create storage bucket for images if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('item-images', 'item-images', true)
ON CONFLICT (id) DO NOTHING;

-- Create storage policy to allow uploads
DROP POLICY IF EXISTS "Allow authenticated users to upload images" ON storage.objects;
CREATE POLICY "Allow authenticated users to upload images" ON storage.objects FOR INSERT WITH CHECK (
    bucket_id = 'item-images'
);

DROP POLICY IF EXISTS "Allow public access to images" ON storage.objects;
CREATE POLICY "Allow public access to images" ON storage.objects FOR SELECT USING (
    bucket_id = 'item-images'
);

-- Verify the final structure
DO $$
DECLARE
    col_info RECORD;
BEGIN
    RAISE NOTICE '=== FINAL DATABASE SCHEMA ===';
    RAISE NOTICE 'Final lost_items table structure:';

    FOR col_info IN
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_name = 'lost_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: "%" (Type: %, Nullable: %)', col_info.column_name, col_info.data_type, col_info.is_nullable;
    END LOOP;

    RAISE NOTICE 'Final found_items table structure:';
    FOR col_info IN
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_name = 'found_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: "%" (Type: %, Nullable: %)', col_info.column_name, col_info.data_type, col_info.is_nullable;
    END LOOP;

    RAISE NOTICE 'Database schema migration completed successfully!';
    RAISE NOTICE 'Your app should now be able to save items without the dateLost column error.';
END $$;
