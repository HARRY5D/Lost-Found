-- SIMPLE MIGRATION: Add missing columns to existing tables
-- Run this in Supabase Dashboard -> SQL Editor
-- This preserves your existing data and just adds the missing columns

-- Check if tables exist and add missing columns
DO $$
BEGIN
    -- Add missing columns to lost_items table if they don't exist
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lost_items' AND table_schema = 'public') THEN
        -- Check for dateLost column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lost_items' AND LOWER(column_name) = 'datelost' AND table_schema = 'public') THEN
            ALTER TABLE public.lost_items ADD COLUMN dateLost BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000;
            RAISE NOTICE 'Added dateLost column to lost_items table';
        ELSE
            RAISE NOTICE 'dateLost column already exists in lost_items table';
        END IF;

        -- Check for reportedDate column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lost_items' AND LOWER(column_name) = 'reporteddate' AND table_schema = 'public') THEN
            ALTER TABLE public.lost_items ADD COLUMN reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000;
            RAISE NOTICE 'Added reportedDate column to lost_items table';
        ELSE
            RAISE NOTICE 'reportedDate column already exists in lost_items table';
        END IF;

        -- Check for reportedByEmail column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lost_items' AND LOWER(column_name) = 'reportedbyemail' AND table_schema = 'public') THEN
            ALTER TABLE public.lost_items ADD COLUMN reportedByEmail TEXT NOT NULL DEFAULT '';
            RAISE NOTICE 'Added reportedByEmail column to lost_items table';
        ELSE
            RAISE NOTICE 'reportedByEmail column already exists in lost_items table';
        END IF;

        -- Check for imageUrl column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lost_items' AND LOWER(column_name) = 'imageurl' AND table_schema = 'public') THEN
            ALTER TABLE public.lost_items ADD COLUMN imageUrl TEXT DEFAULT '';
            RAISE NOTICE 'Added imageUrl column to lost_items table';
        ELSE
            RAISE NOTICE 'imageUrl column already exists in lost_items table';
        END IF;

        -- Check for reportedBy column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lost_items' AND LOWER(column_name) = 'reportedby' AND table_schema = 'public') THEN
            ALTER TABLE public.lost_items ADD COLUMN reportedBy UUID REFERENCES auth.users(id) ON DELETE CASCADE;
            RAISE NOTICE 'Added reportedBy column to lost_items table';
        ELSE
            RAISE NOTICE 'reportedBy column already exists in lost_items table';
        END IF;
    ELSE
        -- Create lost_items table if it doesn't exist
        CREATE TABLE public.lost_items (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name TEXT NOT NULL,
            description TEXT,
            category TEXT NOT NULL,
            location TEXT NOT NULL,
            imageUrl TEXT DEFAULT '',
            reportedBy UUID REFERENCES auth.users(id) ON DELETE CASCADE,
            reportedByEmail TEXT NOT NULL,
            reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
            dateLost BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
        RAISE NOTICE 'Created new lost_items table';
    END IF;

    -- Add missing columns to found_items table if they don't exist
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'found_items' AND table_schema = 'public') THEN
        -- Check for dateFound column (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'datefound' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN dateFound BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000;
            RAISE NOTICE 'Added dateFound column to found_items table';
        ELSE
            RAISE NOTICE 'dateFound column already exists in found_items table';
        END IF;

        -- Check for other missing columns for found_items (case-insensitive)
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'reporteddate' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000;
            RAISE NOTICE 'Added reportedDate column to found_items table';
        ELSE
            RAISE NOTICE 'reportedDate column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'reportedbyemail' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN reportedByEmail TEXT NOT NULL DEFAULT '';
            RAISE NOTICE 'Added reportedByEmail column to found_items table';
        ELSE
            RAISE NOTICE 'reportedByEmail column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'imageurl' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN imageUrl TEXT DEFAULT '';
            RAISE NOTICE 'Added imageUrl column to found_items table';
        ELSE
            RAISE NOTICE 'imageUrl column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'keptat' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN keptAt TEXT NOT NULL DEFAULT '';
            RAISE NOTICE 'Added keptAt column to found_items table';
        ELSE
            RAISE NOTICE 'keptAt column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'claimed' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN claimed BOOLEAN DEFAULT FALSE;
            RAISE NOTICE 'Added claimed column to found_items table';
        ELSE
            RAISE NOTICE 'claimed column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'claimedby' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN claimedBy UUID REFERENCES auth.users(id) ON DELETE SET NULL;
            RAISE NOTICE 'Added claimedBy column to found_items table';
        ELSE
            RAISE NOTICE 'claimedBy column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'claimedbyemail' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN claimedByEmail TEXT DEFAULT '';
            RAISE NOTICE 'Added claimedByEmail column to found_items table';
        ELSE
            RAISE NOTICE 'claimedByEmail column already exists in found_items table';
        END IF;

        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'found_items' AND LOWER(column_name) = 'reportedby' AND table_schema = 'public') THEN
            ALTER TABLE public.found_items ADD COLUMN reportedBy UUID REFERENCES auth.users(id) ON DELETE CASCADE;
            RAISE NOTICE 'Added reportedBy column to found_items table';
        ELSE
            RAISE NOTICE 'reportedBy column already exists in found_items table';
        END IF;
    ELSE
        -- Create found_items table if it doesn't exist
        CREATE TABLE public.found_items (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name TEXT NOT NULL,
            description TEXT,
            category TEXT NOT NULL,
            location TEXT NOT NULL,
            imageUrl TEXT DEFAULT '',
            reportedBy UUID REFERENCES auth.users(id) ON DELETE CASCADE,
            reportedByEmail TEXT NOT NULL,
            reportedDate BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
            keptAt TEXT NOT NULL,
            claimed BOOLEAN DEFAULT FALSE,
            claimedBy UUID REFERENCES auth.users(id) ON DELETE SET NULL,
            claimedByEmail TEXT DEFAULT '',
            dateFound BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM NOW()) * 1000,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
        RAISE NOTICE 'Created new found_items table';
    END IF;
END $$;

-- Enable Row Level Security
ALTER TABLE public.lost_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.found_items ENABLE ROW LEVEL SECURITY;

-- Create policies for lost_items
DROP POLICY IF EXISTS "Allow authenticated users to view all lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to insert their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to update their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to delete their own lost items" ON public.lost_items;

CREATE POLICY "Allow authenticated users to view all lost items" ON public.lost_items FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Allow users to insert their own lost items" ON public.lost_items FOR INSERT WITH CHECK (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);
CREATE POLICY "Allow users to update their own lost items" ON public.lost_items FOR UPDATE USING (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);
CREATE POLICY "Allow users to delete their own lost items" ON public.lost_items FOR DELETE USING (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);

-- Create policies for found_items
DROP POLICY IF EXISTS "Allow authenticated users to view all found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to insert their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to update their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to delete their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to claim found items" ON public.found_items;

CREATE POLICY "Allow authenticated users to view all found items" ON public.found_items FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Allow users to insert their own found items" ON public.found_items FOR INSERT WITH CHECK (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);
CREATE POLICY "Allow users to update their own found items" ON public.found_items FOR UPDATE USING (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);
CREATE POLICY "Allow users to delete their own found items" ON public.found_items FOR DELETE USING (auth.uid() = reportedBy OR auth.uid()::text = reportedBy::text);
CREATE POLICY "Allow users to claim found items" ON public.found_items FOR UPDATE USING (auth.role() = 'authenticated');

-- Create storage bucket for images if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('item-images', 'item-images', true)
ON CONFLICT (id) DO NOTHING;

-- Create storage policy to allow uploads
DROP POLICY IF EXISTS "Allow authenticated users to upload images" ON storage.objects;
CREATE POLICY "Allow authenticated users to upload images" ON storage.objects FOR INSERT WITH CHECK (
    bucket_id = 'item-images' AND auth.role() = 'authenticated'
);

DROP POLICY IF EXISTS "Allow public access to images" ON storage.objects;
CREATE POLICY "Allow public access to images" ON storage.objects FOR SELECT USING (
    bucket_id = 'item-images'
);

RAISE NOTICE 'Database schema migration completed successfully!';

-- Let's check the actual column structure to understand the case mismatch
DO $$
DECLARE
    col_info RECORD;
BEGIN
    RAISE NOTICE 'Current column structure for lost_items:';
    FOR col_info IN
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_name = 'lost_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: % (Type: %)', col_info.column_name, col_info.data_type;
    END LOOP;

    RAISE NOTICE 'Current column structure for found_items:';
    FOR col_info IN
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_name = 'found_items' AND table_schema = 'public'
        ORDER BY ordinal_position
    LOOP
        RAISE NOTICE 'Column: % (Type: %)', col_info.column_name, col_info.data_type;
    END LOOP;
END $$;

