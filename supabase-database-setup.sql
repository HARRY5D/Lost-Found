-- Supabase Database Setup for Campus Lost & Found App
-- Run these commands in Supabase Dashboard -> SQL Editor

-- MIGRATION SCRIPT: This will safely update your existing database structure
-- If you have existing data, it will be preserved during the migration

-- Step 1: Drop existing policies if they exist (to avoid conflicts)
DROP POLICY IF EXISTS "Allow all operations" ON lost_items;
DROP POLICY IF EXISTS "Allow all operations" ON found_items;
DROP POLICY IF EXISTS "Allow authenticated users to view all lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to insert their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to update their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow users to delete their own lost items" ON public.lost_items;
DROP POLICY IF EXISTS "Allow authenticated users to view all found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to insert their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to update their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to delete their own found items" ON public.found_items;
DROP POLICY IF EXISTS "Allow users to claim found items" ON public.found_items;

-- Step 2: Backup existing data (if tables exist)
DO $$
BEGIN
    -- Check if old tables exist and create backup tables
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lost_items' AND table_schema = 'public') THEN
        BEGIN
            CREATE TABLE IF NOT EXISTS lost_items_backup AS SELECT * FROM lost_items;
            ALTER TABLE lost_items_backup ENABLE ROW LEVEL SECURITY;
            DROP POLICY IF EXISTS "Backup table access" ON lost_items_backup;
            CREATE POLICY "Backup table access" ON lost_items_backup FOR ALL USING (true);
            RAISE NOTICE 'Backed up existing lost_items data';
        EXCEPTION
            WHEN OTHERS THEN
                RAISE NOTICE 'Could not backup lost_items: %', SQLERRM;
        END;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'found_items' AND table_schema = 'public') THEN
        BEGIN
            CREATE TABLE IF NOT EXISTS found_items_backup AS SELECT * FROM found_items;
            ALTER TABLE found_items_backup ENABLE ROW LEVEL SECURITY;
            DROP POLICY IF EXISTS "Backup table access" ON found_items_backup;
            CREATE POLICY "Backup table access" ON found_items_backup FOR ALL USING (true);
            RAISE NOTICE 'Backed up existing found_items data';
        EXCEPTION
            WHEN OTHERS THEN
                RAISE NOTICE 'Could not backup found_items: %', SQLERRM;
        END;
    END IF;
END $$;

-- Step 3: Drop existing tables to recreate with new structure
DROP TABLE IF EXISTS public.lost_items CASCADE;
DROP TABLE IF EXISTS public.found_items CASCADE;

-- Step 4: Create lost_items table with new structure
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

-- Step 5: Create found_items table with new structure
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

-- Step 6: Restore data from backup tables (if they exist)
DO $$
BEGIN
    -- Restore lost_items data with data type conversion
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lost_items_backup' AND table_schema = 'public') THEN
        INSERT INTO public.lost_items (
            id, name, description, category, location, imageUrl,
            reportedByEmail, reportedDate, dateLost
        )
        SELECT
            COALESCE(id::uuid, gen_random_uuid()),
            name,
            description,
            category,
            COALESCE(lastSeenLocation, 'Unknown'),
            COALESCE(imageUrl, ''),
            COALESCE(reportedByName, 'Unknown User'),
            CASE
                WHEN reportedDate ~ '^\d+$' THEN reportedDate::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END,
            CASE
                WHEN dateLost ~ '^\d+$' THEN dateLost::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END
        FROM lost_items_backup
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE 'Restored lost_items data';
    END IF;

    -- Restore found_items data with data type conversion
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'found_items_backup' AND table_schema = 'public') THEN
        INSERT INTO public.found_items (
            id, name, description, category, location, imageUrl,
            reportedByEmail, reportedDate, keptAt, claimed, claimedByEmail, dateFound
        )
        SELECT
            COALESCE(id::uuid, gen_random_uuid()),
            name,
            description,
            category,
            location,
            COALESCE(imageUrl, ''),
            COALESCE(reportedByName, 'Unknown User'),
            CASE
                WHEN reportedDate ~ '^\d+$' THEN reportedDate::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END,
            COALESCE(keptAt, 'Unknown Location'),
            COALESCE(claimed, false),
            COALESCE(claimedByName, ''),
            CASE
                WHEN dateFound ~ '^\d+$' THEN dateFound::bigint
                ELSE EXTRACT(epoch FROM NOW()) * 1000
            END
        FROM found_items_backup
        ON CONFLICT (id) DO NOTHING;
        RAISE NOTICE 'Restored found_items data';
    END IF;
END $$;

-- Step 7: Enable Row Level Security (RLS)
ALTER TABLE public.lost_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.found_items ENABLE ROW LEVEL SECURITY;

-- Step 8: Create RLS Policies for lost_items
-- Allow all authenticated users to read all lost items (cross-user visibility)
CREATE POLICY "Allow authenticated users to view all lost items" ON public.lost_items
    FOR SELECT USING (auth.role() = 'authenticated');

-- Allow users to insert their own lost items
CREATE POLICY "Allow users to insert their own lost items" ON public.lost_items
    FOR INSERT WITH CHECK (auth.uid() = reportedBy);

-- Allow users to update their own lost items
CREATE POLICY "Allow users to update their own lost items" ON public.lost_items
    FOR UPDATE USING (auth.uid() = reportedBy);

-- Allow users to delete their own lost items
CREATE POLICY "Allow users to delete their own lost items" ON public.lost_items
    FOR DELETE USING (auth.uid() = reportedBy);

-- Step 9: Create RLS Policies for found_items
-- Allow all authenticated users to read all found items (cross-user visibility)
CREATE POLICY "Allow authenticated users to view all found items" ON public.found_items
    FOR SELECT USING (auth.role() = 'authenticated');

-- Allow users to insert their own found items
CREATE POLICY "Allow users to insert their own found items" ON public.found_items
    FOR INSERT WITH CHECK (auth.uid() = reportedBy);

-- Allow users to update their own found items
CREATE POLICY "Allow users to update their own found items" ON public.found_items
    FOR UPDATE USING (auth.uid() = reportedBy);

-- Allow users to delete their own found items
CREATE POLICY "Allow users to delete their own found items" ON public.found_items
    FOR DELETE USING (auth.uid() = reportedBy);

-- Allow users to claim found items (update claim status)
CREATE POLICY "Allow users to claim found items" ON public.found_items
    FOR UPDATE USING (auth.role() = 'authenticated');

-- Step 10: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_lost_items_reported_by ON public.lost_items(reportedBy);
CREATE INDEX IF NOT EXISTS idx_lost_items_category ON public.lost_items(category);
CREATE INDEX IF NOT EXISTS idx_lost_items_reported_date ON public.lost_items(reportedDate DESC);

CREATE INDEX IF NOT EXISTS idx_found_items_reported_by ON public.found_items(reportedBy);
CREATE INDEX IF NOT EXISTS idx_found_items_category ON public.found_items(category);
CREATE INDEX IF NOT EXISTS idx_found_items_claimed ON public.found_items(claimed);
CREATE INDEX IF NOT EXISTS idx_found_items_reported_date ON public.found_items(reportedDate DESC);

-- Step 11: Storage bucket policies for item-images
-- Create the bucket if it doesn't exist
INSERT INTO storage.buckets (id, name, public)
VALUES ('item-images', 'item-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Drop existing storage policy if it exists, then create new one
DROP POLICY IF EXISTS "Allow public uploads" ON storage.objects;

-- Allow public uploads to item-images bucket
CREATE POLICY "Allow public uploads" ON storage.objects
FOR ALL TO public
USING (bucket_id = 'item-images')
WITH CHECK (bucket_id = 'item-images');

-- Step 12: Enable real-time subscriptions (optional)
DO $$
BEGIN
    -- Add tables to realtime publication if they're not already added
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
        AND tablename = 'lost_items'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.lost_items;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
        AND tablename = 'found_items'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.found_items;
    END IF;
END $$;

-- Step 13: Clean up backup tables (optional - uncomment if you want to remove backups)
-- DROP TABLE IF EXISTS lost_items_backup;
-- DROP TABLE IF EXISTS found_items_backup;

-- Migration completed successfully!
-- Your database now has the new structure with proper UUID support,
-- authentication integration, and cross-user visibility policies.
