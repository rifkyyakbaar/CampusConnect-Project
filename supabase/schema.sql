create table if not exists public.users (
    uid uuid primary key references auth.users(id) on delete cascade,
    "fullName" text not null default '',
    email text not null default '',
    role text not null default 'Mahasiswa',
    provider text not null default 'email',
    "createdAt" timestamptz not null default now(),
    "updatedAt" timestamptz not null default now()
);

create table if not exists public.events (
    "eventId" uuid primary key,
    "eventName" text not null,
    category text not null default '',
    capacity integer not null default 0,
    description text not null default '',
    "organizerId" uuid not null references auth.users(id) on delete cascade,
    "organizerName" text not null default '',
    "posterUrl" text not null default '',
    status text not null default 'pending',
    registrants integer not null default 0,
    "createdAt" timestamptz not null default now()
);

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

notify pgrst, 'reload schema';
