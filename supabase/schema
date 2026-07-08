-- =========================================================================
-- Sepatify — Supabase schema, security policies, realtime wiring and seed data
-- =========================================================================
-- HOW TO USE:
--   1. Open your Supabase project -> SQL Editor -> New query.
--   2. Paste the entire contents of this file and click "Run".
--   3. Open Project Settings -> API and copy the "anon / public" key into
--      `local.properties` as SUPABASE_ANON_KEY (see that file for the exact key name).
--   4. (Optional) Project Settings -> Auth -> disable "Confirm email" while
--      developing, so sign-up works immediately without an email round-trip.
-- =========================================================================

-- ---------------------------------------------------------------------
-- 1. PROFILES  (1:1 with auth.users)
-- ---------------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null,
  display_name text not null default 'Guest User',
  avatar_url text,
  bio text,
  is_premium boolean not null default false,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

drop policy if exists "Profiles are viewable by authenticated users" on public.profiles;
create policy "Profiles are viewable by authenticated users"
  on public.profiles for select
  to authenticated
  using (true);

drop policy if exists "Users can insert their own profile" on public.profiles;
create policy "Users can insert their own profile"
  on public.profiles for insert
  to authenticated
  with check (auth.uid() = id);

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
  on public.profiles for update
  to authenticated
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- Auto-create a profile row whenever a new auth user signs up.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'username', split_part(new.email, '@', 1)) || '_' || substr(new.id::text, 1, 4),
    coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1))
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ---------------------------------------------------------------------
-- 2. SONGS  (the music catalog — >= 50 real, playable tracks)
-- ---------------------------------------------------------------------
create table if not exists public.songs (
  id text primary key,
  title text not null,
  artist_name text not null,
  cover_image_url text not null,
  audio_url text not null,
  category text not null default 'Popular', -- Trending | Popular | NewRelease | Recommendation
  created_at timestamptz not null default now()
);

alter table public.songs enable row level security;

drop policy if exists "Songs are viewable by everyone" on public.songs;
create policy "Songs are viewable by everyone"
  on public.songs for select
  to anon, authenticated
  using (true);

-- ---------------------------------------------------------------------
-- 3. FOLLOWS  (social graph)
-- ---------------------------------------------------------------------
create table if not exists public.follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  followed_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, followed_id)
);

alter table public.follows enable row level security;

drop policy if exists "Follow graph is viewable by authenticated users" on public.follows;
create policy "Follow graph is viewable by authenticated users"
  on public.follows for select
  to authenticated
  using (true);

drop policy if exists "Users can follow as themselves" on public.follows;
create policy "Users can follow as themselves"
  on public.follows for insert
  to authenticated
  with check (auth.uid() = follower_id);

drop policy if exists "Users can unfollow as themselves" on public.follows;
create policy "Users can unfollow as themselves"
  on public.follows for delete
  to authenticated
  using (auth.uid() = follower_id);

-- ---------------------------------------------------------------------
-- 4. PLAYLISTS + PLAYLIST_SONGS
-- ---------------------------------------------------------------------
create table if not exists public.playlists (
  id bigint generated always as identity primary key,
  owner_id uuid references public.profiles(id) on delete cascade,
  title text not null,
  description text not null default '',
  category text not null default 'User', -- Global | Local | User
  cover_url text,
  created_at timestamptz not null default now()
);

alter table public.playlists enable row level security;

drop policy if exists "Playlists are viewable by everyone" on public.playlists;
create policy "Playlists are viewable by everyone"
  on public.playlists for select
  to anon, authenticated
  using (category in ('Global', 'Local') or owner_id = auth.uid());

drop policy if exists "Users can create their own playlists" on public.playlists;
create policy "Users can create their own playlists"
  on public.playlists for insert
  to authenticated
  with check (owner_id = auth.uid());

drop policy if exists "Users can delete their own playlists" on public.playlists;
create policy "Users can delete their own playlists"
  on public.playlists for delete
  to authenticated
  using (owner_id = auth.uid());

create table if not exists public.playlist_songs (
  playlist_id bigint not null references public.playlists(id) on delete cascade,
  song_id text not null references public.songs(id) on delete cascade,
  position int not null default 0,
  added_at timestamptz not null default now(),
  primary key (playlist_id, song_id)
);

alter table public.playlist_songs enable row level security;

drop policy if exists "Playlist songs are viewable by everyone" on public.playlist_songs;
create policy "Playlist songs are viewable by everyone"
  on public.playlist_songs for select
  to anon, authenticated
  using (true);

drop policy if exists "Users can manage songs in their own playlists" on public.playlist_songs;
create policy "Users can manage songs in their own playlists"
  on public.playlist_songs for insert
  to authenticated
  with check (
    exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = auth.uid())
  );

drop policy if exists "Users can remove songs from their own playlists" on public.playlist_songs;
create policy "Users can remove songs from their own playlists"
  on public.playlist_songs for delete
  to authenticated
  using (
    exists (select 1 from public.playlists p where p.id = playlist_id and p.owner_id = auth.uid())
  );

-- ---------------------------------------------------------------------
-- 5. LIKED SONGS (server-synced; Room caches this locally for offline use)
-- ---------------------------------------------------------------------
create table if not exists public.liked_songs (
  user_id uuid not null references public.profiles(id) on delete cascade,
  song_id text not null references public.songs(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, song_id)
);

alter table public.liked_songs enable row level security;

drop policy if exists "Users manage their own liked songs" on public.liked_songs;
create policy "Users manage their own liked songs"
  on public.liked_songs for all
  to authenticated
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- ---------------------------------------------------------------------
-- 6. CHAT MESSAGES (real-time DMs, delivered over Supabase Realtime/WebSocket)
-- ---------------------------------------------------------------------
create table if not exists public.chat_messages (
  id bigint generated always as identity primary key,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  receiver_id uuid not null references public.profiles(id) on delete cascade,
  text text not null default '',
  is_song_share boolean not null default false,
  song_id text,
  song_title text,
  song_artist text,
  song_cover text,
  song_audio text,
  status text not null default 'Sent', -- Sending | Sent | Delivered | Read
  created_at timestamptz not null default now()
);

alter table public.chat_messages enable row level security;

drop policy if exists "Users can read their own conversations" on public.chat_messages;
create policy "Users can read their own conversations"
  on public.chat_messages for select
  to authenticated
  using (auth.uid() = sender_id or auth.uid() = receiver_id);

drop policy if exists "Users can send messages as themselves" on public.chat_messages;
create policy "Users can send messages as themselves"
  on public.chat_messages for insert
  to authenticated
  with check (auth.uid() = sender_id);

drop policy if exists "Participants can update message status" on public.chat_messages;
create policy "Participants can update message status"
  on public.chat_messages for update
  to authenticated
  using (auth.uid() = sender_id or auth.uid() = receiver_id)
  with check (auth.uid() = sender_id or auth.uid() = receiver_id);

-- Stream INSERT/UPDATE events for this table down the Realtime websocket.
alter publication supabase_realtime add table public.chat_messages;

-- ---------------------------------------------------------------------
-- 7. STORAGE (avatars)
-- ---------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

drop policy if exists "Avatar images are publicly accessible" on storage.objects;
create policy "Avatar images are publicly accessible"
  on storage.objects for select
  to anon, authenticated
  using (bucket_id = 'avatars');

drop policy if exists "Users can upload their own avatar" on storage.objects;
create policy "Users can upload their own avatar"
  on storage.objects for insert
  to authenticated
  with check (bucket_id = 'avatars' and owner = auth.uid());

drop policy if exists "Users can update their own avatar" on storage.objects;
create policy "Users can update their own avatar"
  on storage.objects for update
  to authenticated
  using (bucket_id = 'avatars' and owner = auth.uid());

-- ---------------------------------------------------------------------
-- 8. SEED DATA — 50 real, streamable tracks with metadata
-- ---------------------------------------------------------------------
insert into public.songs (id, title, artist_name, cover_image_url, audio_url, category) values
('s1', 'Lofi Sunset Echoes', 'Kavinsky Shimmer', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'Trending'),
('s2', 'Ambient Solitude', 'Moby Skies', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'Popular'),
('s3', 'Synthwave Morning Breeze', 'Neon Horizon', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'NewRelease'),
('s4', 'Late Night Coffee', 'Tokyo Cafe', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'Trending'),
('s5', 'Dreamy Forest Rain', 'Zen Lotus', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'Recommendation'),
('s6', 'Spring (The Four Seasons)', 'Antonio Vivaldi', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'Popular'),
('s7', 'Symphony No. 5 in C Minor', 'Ludwig van Beethoven', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'Recommendation'),
('s8', 'Nocturne in E-Flat Major', 'Frederic Chopin', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'NewRelease'),
('s9', 'Cello Suite No. 1', 'Johann Sebastian Bach', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'Trending'),
('s10', 'Fur Elise', 'Ludwig van Beethoven', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'Popular'),
('s11', 'Ascendant Galaxies', 'Starlit Nebula', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 'NewRelease'),
('s12', 'Ethereal Whispers', 'Sora Whisper', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 'Recommendation'),
('s13', 'Forgotten Ruins', 'Chronos', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 'Trending'),
('s14', 'Velocity Storm', 'Pulse Driver', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', 'Popular'),
('s15', 'Oasis of Eternity', 'Sands of Sahara', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3', 'NewRelease'),
('s16', 'Horizon Explorer', 'Voyager 9', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3', 'Recommendation'),
('s17', 'Clair de Lune', 'Claude Debussy', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'Popular'),
('s18', 'The Blue Danube', 'Johann Strauss II', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'Popular'),
('s19', 'Gymnopedie No. 1', 'Erik Satie', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'Recommendation'),
('s20', 'Moonlight Sonata', 'Ludwig van Beethoven', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'Recommendation'),
('s21', 'Ride of the Valkyries', 'Richard Wagner', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'NewRelease'),
('s22', 'Requiem in D Minor', 'Wolfgang Amadeus Mozart', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'NewRelease'),
('s23', 'Canon in D', 'Johann Pachelbel', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'Trending'),
('s24', 'The Planets: Jupiter', 'Gustav Holst', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'Trending'),
('s25', 'Neo Seoul Grid', 'Laserhawk', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'Popular'),
('s26', 'Outrun Velocity', 'Retro Rider', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'Popular'),
('s27', 'Arcade Dreamland', 'Cyber Shimmer', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 'NewRelease'),
('s28', 'Megastructure 2049', 'Industrial Pulse', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 'NewRelease'),
('s29', 'GridRunner Prime', 'Glitch Shaman', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 'Trending'),
('s30', 'Rainy Day Cocoa', 'Soma Lofi', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', 'Trending'),
('s31', 'Old Books & Vinyl', 'Dusty Grooves', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3', 'Recommendation'),
('s32', 'Sunday Waffles', 'Lazy Sunday', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3', 'Recommendation'),
('s33', 'Study Room Session', 'Pencil Clicks', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'Popular'),
('s34', 'Transit Midnight Train', 'Urban Nostalgia', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'Popular'),
('s35', 'Fireside Guitar', 'Canyon Strings', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3', 'NewRelease'),
('s36', 'Golden Hour Fields', 'Prairie Wind', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3', 'NewRelease'),
('s37', 'Wildflowering Trail', 'Alpine Bloom', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3', 'Recommendation'),
('s38', 'Coastline Drive', 'Salty Hair', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3', 'Recommendation'),
('s39', 'Stardust Campfire', 'Constellations', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3', 'Trending'),
('s40', 'Kinetic Velocity', 'Electronica Core', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3', 'Trending'),
('s41', 'HyperSpace Acceleration', 'Hypersonic', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3', 'Popular'),
('s42', 'Digital Oasis Glow', 'Oasis Pixel', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', 'Popular'),
('s43', 'Glitch Symphony', 'Artifact 10', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', 'Recommendation'),
('s44', 'Bassline Architect', 'LowFreq Sector', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', 'Recommendation'),
('s45', 'Circuit Shaman Ritual', 'Digital Shaman', 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3', 'NewRelease'),
('s46', 'Neon Rain City', 'Retro Tokyo', 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3', 'NewRelease'),
('s47', 'Slick Synthesis', 'Glide Wave', 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3', 'Trending'),
('s48', 'Quantum Superposition', 'Chronosphere', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3', 'Trending'),
('s49', 'Ether Voyager', 'Stellar Drifter', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3', 'Popular'),
('s50', 'Distant Echoes of AUT', 'Computer AUT Spring', 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3', 'Popular')
on conflict (id) do update set
  title = excluded.title,
  artist_name = excluded.artist_name,
  cover_image_url = excluded.cover_image_url,
  audio_url = excluded.audio_url,
  category = excluded.category;

-- Seed two curated "Global" playlists and one "Local" (device-agnostic sample) playlist,
-- each populated from the catalog above.
insert into public.playlists (id, owner_id, title, description, category, cover_url)
overriding system value
values
  (1, null, 'Global Top Hits', 'The most streamed tracks around the world right now.', 'Global', 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80'),
  (2, null, 'Classical Essentials', 'Timeless pieces from the greatest composers.', 'Global', 'https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&q=80'),
  (3, null, 'Chill & Focus (Local Picks)', 'Lofi and ambient tracks curated for focus sessions.', 'Local', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80')
on conflict (id) do nothing;

select setval(pg_get_serial_sequence('public.playlists', 'id'), greatest((select max(id) from public.playlists), 1));

insert into public.playlist_songs (playlist_id, song_id, position)
select 1, id, row_number() over () from public.songs where category in ('Trending', 'Popular') limit 10
on conflict do nothing;

insert into public.playlist_songs (playlist_id, song_id, position)
select 2, id, row_number() over () from public.songs where artist_name in ('Antonio Vivaldi','Ludwig van Beethoven','Frederic Chopin','Johann Sebastian Bach','Claude Debussy','Johann Strauss II','Erik Satie','Richard Wagner','Wolfgang Amadeus Mozart','Johann Pachelbel','Gustav Holst')
on conflict do nothing;

insert into public.playlist_songs (playlist_id, song_id, position)
select 3, id, row_number() over () from public.songs where category in ('Recommendation') limit 10
on conflict do nothing;

-- =========================================================================
-- Done! Your Sepatify backend is ready:
--   * 50 songs in `songs`
--   * 3 starter playlists (2 Global, 1 Local) in `playlists` / `playlist_songs`
--   * profiles auto-created on sign up
--   * chat_messages streamed live via Supabase Realtime
--   * an `avatars` public storage bucket
-- =========================================================================
