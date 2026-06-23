-- DEVELOPMENT STUB ONLY
--
-- Simulates the Financial Document module contract from
-- SCHEMADOKUMENKEUANGAN.md while the real module is not integrated.
--
-- Production integration rule:
-- Do not apply this migration in an environment where the Financial Document
-- module already owns these objects. Apply only the loan-module migration.

do $$
begin
    create type public.document_type as enum ('SP2D');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.physical_form as enum ('PAPER');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.document_condition as enum ('GOOD');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.document_status as enum ('AVAILABLE');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.staging_document_source as enum ('MANUAL');
exception when duplicate_object then null;
end $$;

create table if not exists public.archive_classifications (
    code text not null,
    name text not null,
    parent_code text,
    level integer not null default 1 check (level > 0),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint archive_classifications_pkey primary key (code),
    constraint archive_classifications_parent_code_fkey
        foreign key (parent_code)
        references public.archive_classifications(code)
);

create table if not exists public.staging_documents (
    id uuid not null default gen_random_uuid(),
    document_type public.document_type not null,
    document_number text,
    classification_code text,
    title text not null,
    description text,
    year integer not null check (year between 1900 and 2100),
    physical_form public.physical_form not null,
    condition public.document_condition not null default 'GOOD',
    copy_count integer not null default 1 check (copy_count > 0),
    is_copy boolean,
    status public.document_status not null default 'AVAILABLE',
    origin_instance text,
    source public.staging_document_source not null default 'MANUAL',
    created_by uuid,
    updated_by uuid,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint staging_documents_pkey primary key (id),
    constraint staging_documents_classification_code_fkey
        foreign key (classification_code)
        references public.archive_classifications(code),
    constraint staging_documents_created_by_fkey
        foreign key (created_by) references auth.users(id),
    constraint staging_documents_updated_by_fkey
        foreign key (updated_by) references auth.users(id)
);

create table if not exists public.storage_locations (
    id uuid not null default gen_random_uuid(),
    room text not null,
    shelf text not null,
    box_number text,
    description text,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint storage_locations_pkey primary key (id)
);

create table if not exists public.archive_documents (
    id uuid not null default gen_random_uuid(),
    document_type public.document_type not null,
    document_number text,
    classification_code text,
    title text not null,
    description text,
    year integer not null check (year between 1900 and 2100),
    physical_form public.physical_form not null,
    condition public.document_condition not null default 'GOOD',
    copy_count integer not null default 1 check (copy_count > 0),
    is_copy boolean,
    status public.document_status not null default 'AVAILABLE',
    origin_instance text,
    storage_location_id uuid,
    source_staging_id uuid,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint archive_documents_pkey primary key (id),
    constraint archive_documents_classification_code_fkey
        foreign key (classification_code)
        references public.archive_classifications(code),
    constraint archive_documents_storage_location_id_fkey
        foreign key (storage_location_id)
        references public.storage_locations(id),
    constraint archive_documents_source_staging_id_fkey
        foreign key (source_staging_id)
        references public.staging_documents(id),
    constraint archive_documents_created_by_fkey
        foreign key (created_by) references auth.users(id),
    constraint archive_documents_updated_by_fkey
        foreign key (updated_by) references auth.users(id)
);

create table if not exists public.document_placements (
    id uuid not null default gen_random_uuid(),
    archive_document_id uuid not null,
    storage_location_id uuid not null,
    placed_at timestamptz not null default now(),
    removed_at timestamptz,
    note text,
    created_by uuid,
    created_at timestamptz not null default now(),
    constraint document_placements_pkey primary key (id),
    constraint document_placements_archive_document_id_fkey
        foreign key (archive_document_id)
        references public.archive_documents(id),
    constraint document_placements_storage_location_id_fkey
        foreign key (storage_location_id)
        references public.storage_locations(id),
    constraint document_placements_created_by_fkey
        foreign key (created_by) references auth.users(id)
);

create table if not exists public.activity_logs (
    id uuid not null default gen_random_uuid(),
    actor_id uuid,
    action text not null,
    entity_type text not null,
    entity_id uuid,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    constraint activity_logs_pkey primary key (id),
    constraint activity_logs_actor_id_fkey
        foreign key (actor_id) references auth.users(id)
);

-- Stub RLS is intentionally read-only for authenticated mobile clients.
-- Seed/setup operations run from SQL Editor as the database owner.
alter table public.archive_classifications enable row level security;
alter table public.staging_documents enable row level security;
alter table public.storage_locations enable row level security;
alter table public.archive_documents enable row level security;
alter table public.document_placements enable row level security;
alter table public.activity_logs enable row level security;

grant usage on schema public to authenticated;
grant select on public.archive_classifications to authenticated;
grant select on public.storage_locations to authenticated;
grant select on public.archive_documents to authenticated;
grant select on public.document_placements to authenticated;

create policy stub_archive_classifications_read
    on public.archive_classifications for select to authenticated
    using (true);
create policy stub_storage_locations_read
    on public.storage_locations for select to authenticated
    using (true);
create policy stub_archive_documents_read
    on public.archive_documents for select to authenticated
    using (deleted_at is null);
create policy stub_document_placements_read
    on public.document_placements for select to authenticated
    using (true);
