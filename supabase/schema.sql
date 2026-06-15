create table if not exists public.users (
    uid uuid primary key references auth.users(id) on delete cascade,
    "fullName" text not null default '',
    email text not null default '',
    role text not null default 'Mahasiswa',
    provider text not null default 'email',
    "createdAt" timestamptz not null default now(),
    "updatedAt" timestamptz not null default now(),
    "deletedAt" timestamptz
);

alter table public.users
add column if not exists "deletedAt" timestamptz;

create table if not exists public.events (
    "eventId" uuid primary key,
    "eventName" text not null,
    category text not null default '',
    location text not null default '',
    capacity integer not null default 0,
    description text not null default '',
    "organizerId" uuid not null references auth.users(id) on delete cascade,
    "organizerName" text not null default '',
    "posterUrl" text not null default '',
    status text not null default 'pending',
    registrants integer not null default 0,
    "eventDate" text not null default '',
    "createdAt" timestamptz not null default now()
);

alter table public.events
add column if not exists location text not null default '';

alter table public.events
add column if not exists "eventDate" text not null default '';

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'event-posters',
    'event-posters',
    true,
    5242880,
    array['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

alter table public.users enable row level security;
alter table public.events enable row level security;

drop policy if exists "Users can read own profile" on public.users;
create policy "Users can read own profile"
on public.users for select
to authenticated
using (auth.uid() = uid);

drop policy if exists "Users can insert own profile" on public.users;
create policy "Users can insert own profile"
on public.users for insert
to authenticated
with check (auth.uid() = uid);

drop policy if exists "Users can update own profile" on public.users;
create policy "Users can update own profile"
on public.users for update
to authenticated
using (auth.uid() = uid)
with check (auth.uid() = uid);

drop policy if exists "Users can delete own profile" on public.users;
create policy "Users can delete own profile"
on public.users for delete
to authenticated
using (auth.uid() = uid);

drop policy if exists "Anyone can read events" on public.events;
create policy "Anyone can read events"
on public.events for select
to anon, authenticated
using (true);

drop policy if exists "Organizers can create events" on public.events;
create policy "Organizers can create events"
on public.events for insert
to authenticated
with check (auth.uid() = "organizerId");

drop policy if exists "Organizers can update own events" on public.events;
create policy "Organizers can update own events"
on public.events for update
to authenticated
using (auth.uid() = "organizerId")
with check (auth.uid() = "organizerId");

drop policy if exists "Admins can update event approval status" on public.events;
create policy "Admins can update event approval status"
on public.events for update
to anon, authenticated
using (true)
with check (status in ('pending', 'approved', 'rejected'));

drop policy if exists "Anyone can read event posters" on storage.objects;
create policy "Anyone can read event posters"
on storage.objects for select
to anon, authenticated
using (bucket_id = 'event-posters');

drop policy if exists "Authenticated users can upload event posters" on storage.objects;
create policy "Authenticated users can upload event posters"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'event-posters'
    and auth.uid()::text = (storage.foldername(name))[1]
);

notify pgrst, 'reload schema';
