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
