import os
import uuid
from pathlib import Path
from supabase import create_client

# Config
AUDIO_DIR = "/run/user/1000/gvfs/mtp:host=SAMSUNG_SAMSUNG_Android_RZCTB1ERS7Y/Internal storage/Music"  # e.g., "./music" or "/mnt/music"
SUPABASE_URL = "https://uppyfwrazoaciylsdynh.supabase.co"
SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVwcHlmd3Jhem9hY2l5bHNkeW5oIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MzA4NTU5NSwiZXhwIjoyMDk4NjYxNTk1fQ.wVTTHv49s98MvGANCK7X4Mqxbvxj4TuaFizmHThi95c"



# Bucket names in Supabase Storage
SONGS_BUCKET = "songs"
COVERS_BUCKET = "covers"

# Supabase client
supabase = create_client(SUPABASE_URL, SUPABASE_KEY)

AUDIO_EXTS = {'.mp3', '.wav', '.flac', '.m4a', '.ogg', '.aac'}
COVER_EXTS = {'.jpg', '.jpeg', '.png', '.webp', '.gif'}


# ─── HELPERS ─────────────────────────────────────────────────────────
def parse_filename(filename):
    """Extract artist and title from 'Artist - Title.mp3'."""
    name = Path(filename).stem
    parts = name.split(" - ", 1)
    if len(parts) == 2:
        return parts[0].strip(), parts[1].strip()
    return "Unknown Artist", name.strip()


def find_cover_for_audio(audio_path):
    """Look for a cover image with the same name next to the audio file."""
    for ext in COVER_EXTS:
        cover_path = audio_path.with_suffix(ext)
        if cover_path.exists():
            return cover_path
    return None


def ensure_bucket_exists(bucket_name, public=True):
    """Check if bucket exists, create if not."""
    try:
        buckets = supabase.storage.list_buckets()
        bucket_names = [b["name"] for b in buckets]
        
        if bucket_name not in bucket_names:
            supabase.storage.create_bucket(
                bucket_name,
                options={"public": public}
            )
            print(f"✅ Created bucket: {bucket_name}")
        else:
            print(f"ℹ️  Bucket exists: {bucket_name}")
    except Exception as e:
        print(f"⚠️  Bucket check/create issue: {e}")


def upload_file_to_bucket(bucket_name, file_path, dest_path=None):
    """
    Upload a file to Supabase Storage.
    Returns the public URL.
    """
    if dest_path is None:
        dest_path = Path(file_path).name
    
    try:
        with open(file_path, "rb") as f:
            supabase.storage.from_(bucket_name).upload(
                path=dest_path,
                file=f,
                file_options={"content-type": "application/octet-stream", "upsert": "true"}
            )
        
        # Get public URL
        public_url = supabase.storage.from_(bucket_name).get_public_url(dest_path)
        print(f"  ✅ Uploaded: {dest_path}")
        return public_url
        
    except Exception as e:
        # If file already exists, just get the URL
        if "already exists" in str(e).lower():
            public_url = supabase.storage.from_(bucket_name).get_public_url(dest_path)
            print(f"  ℹ️  Already exists: {dest_path}")
            return public_url
        
        print(f"  ❌ Upload failed: {e}")
        return None


def get_or_create_artist(artist_name):
    """Get existing artist UUID or create new artist + profile."""
    # Search existing (case-insensitive)
    result = supabase.table("artists").select("id, display_name").ilike("display_name", artist_name).execute()
    
    if result.data:
        return result.data[0]["id"]
    
    # Create new artist
    new_id = str(uuid.uuid4())
    username = artist_name.lower().replace(" ", "_").replace("-", "_")[:50]
    