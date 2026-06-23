-- BPKPAD Balangan - Document Loan Management module
--
-- INTEGRATION BOUNDARY:
-- The following objects are owned by the Financial Document module and MUST
-- already exist. This migration never creates, alters, indexes, triggers, or
-- changes RLS on them:
--   public.archive_classifications
--   public.staging_documents
--   public.storage_locations
--   public.archive_documents
--   public.document_placements
--   public.activity_logs
--
-- Source contract:
-- C:/Users/USER/Downloads/SCHEMADOKUMENKEUANGAN.md

create schema if not exists app_private;
revoke all on schema app_private from public, anon, authenticated;

do $contract$
declare
    missing_objects text[];
begin
    select array_agg(expected_name order by expected_name)
    into missing_objects
    from (
        values
            ('public.archive_classifications'),
            ('public.staging_documents'),
            ('public.storage_locations'),
            ('public.archive_documents'),
            ('public.document_placements'),
            ('public.activity_logs')
    ) expected(expected_name)
    where to_regclass(expected_name) is null;

    if missing_objects is not null then
        raise exception
            'Financial Document schema is incomplete. Missing: %',
            array_to_string(missing_objects, ', ');
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'archive_documents'
          and column_name = 'id'
          and data_type = 'uuid'
    ) then
        raise exception
            'Contract mismatch: public.archive_documents.id must be uuid';
    end if;

    if not exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'archive_documents'
          and column_name = 'status'
          and data_type = 'USER-DEFINED'
    ) then
        raise exception
            'Contract mismatch: public.archive_documents.status must retain its external enum type';
    end if;
end
$contract$;

do $$
begin
    create type public.loan_app_role as enum ('arsiparis', 'kasubag');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.loan_transaction_status as enum (
        'menunggu_persetujuan',
        'disetujui',
        'ditolak',
        'dipinjam',
        'dikembalikan',
        'dibatalkan'
    );
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.loan_approval_method as enum ('online', 'bypass');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.loan_return_condition as enum ('baik', 'rusak', 'hilang');
exception when duplicate_object then null;
end $$;

do $$
begin
    create type public.loan_extension_status as enum ('pending', 'approved', 'rejected');
exception when duplicate_object then null;
end $$;

create table if not exists public.loan_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    legacy_id bigint generated always as identity unique,
    username text not null unique,
    nama_lengkap text not null,
    nip text unique,
    role public.loan_app_role not null default 'arsiparis',
    no_hp text,
    is_active boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint loan_profiles_username_format
        check (username ~ '^[A-Za-z0-9._-]{3,50}$'),
    constraint loan_profiles_phone_format
        check (no_hp is null or no_hp ~ '^[0-9]{10,20}$')
);

create table if not exists public.loan_borrower_agencies (
    id uuid primary key default gen_random_uuid(),
    legacy_id bigint generated always as identity unique,
    nama_instansi text not null unique,
    alamat text,
    kode_instansi text unique,
    created_by uuid references auth.users(id),
    updated_by uuid references auth.users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.loan_transactions (
    id uuid primary key default gen_random_uuid(),
    legacy_id bigint generated always as identity unique,
    borrower_agency_id uuid not null
        references public.loan_borrower_agencies(id),
    pic_nama text not null,
    pic_no_hp text not null check (pic_no_hp ~ '^[0-9]{10,20}$'),
    nomor_surat_pengantar text not null,
    foto_surat_pengantar_path text not null,
    qr_code_token uuid unique,
    tanggal_pinjam date not null,
    tanggal_kembali_rencana date not null,
    tanggal_kembali_aktual date,
    status public.loan_transaction_status not null
        default 'menunggu_persetujuan',
    metode_persetujuan public.loan_approval_method,
    bukti_bypass_path text,
    catatan_bypass text,
    is_bypass_acknowledged boolean not null default false,
    alasan_penolakan text,
    created_by uuid not null references auth.users(id),
    approved_by uuid references auth.users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint loan_return_plan_valid
        check (tanggal_kembali_rencana > tanggal_pinjam),
    constraint loan_actual_return_valid check (
        tanggal_kembali_aktual is null
        or tanggal_kembali_aktual >= tanggal_pinjam
    ),
    constraint loan_rejection_requires_reason check (
        status <> 'ditolak'
        or nullif(trim(alasan_penolakan), '') is not null
    ),
    constraint loan_bypass_requires_proof check (
        metode_persetujuan <> 'bypass'
        or (
            nullif(trim(bukti_bypass_path), '') is not null
            and nullif(trim(catatan_bypass), '') is not null
        )
    ),
    constraint returned_loan_requires_date check (
        status <> 'dikembalikan'
        or tanggal_kembali_aktual is not null
    )
);

create table if not exists public.loan_items (
    id uuid primary key default gen_random_uuid(),
    loan_transaction_id uuid not null
        references public.loan_transactions(id) on delete cascade,
    -- Only this UUID is coupled to the external Financial Document module.
    archive_document_id uuid not null
        references public.archive_documents(id),
    document_number_snapshot text,
    title_snapshot text not null,
    year_snapshot integer,
    location_snapshot text,
    return_condition public.loan_return_condition,
    condition_note text,
    created_at timestamptz not null default now(),
    unique (loan_transaction_id, archive_document_id),
    constraint loan_item_condition_note check (
        return_condition not in ('rusak', 'hilang')
        or nullif(trim(condition_note), '') is not null
    )
);

-- Module-owned lock. It prevents two active loans without changing the
-- external archive_documents.status enum or taking ownership of that table.
create table if not exists public.loan_document_locks (
    id uuid primary key default gen_random_uuid(),
    archive_document_id uuid not null
        references public.archive_documents(id),
    loan_transaction_id uuid not null
        references public.loan_transactions(id) on delete cascade,
    locked_by uuid not null references auth.users(id),
    locked_at timestamptz not null default now(),
    released_at timestamptz,
    release_reason text,
    constraint loan_document_lock_release_order check (
        released_at is null or released_at >= locked_at
    )
);

create unique index if not exists one_active_loan_lock_per_document
    on public.loan_document_locks(archive_document_id)
    where released_at is null;

create table if not exists public.loan_extensions (
    id uuid primary key default gen_random_uuid(),
    legacy_id bigint generated always as identity unique,
    loan_transaction_id uuid not null
        references public.loan_transactions(id) on delete cascade,
    tanggal_kembali_lama date not null,
    tanggal_kembali_baru date not null,
    foto_surat_perpanjangan_path text not null,
    alasan text not null,
    status public.loan_extension_status not null default 'pending',
    alasan_penolakan text,
    created_by uuid not null references auth.users(id),
    approved_by uuid references auth.users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint loan_extension_date_valid
        check (tanggal_kembali_baru > tanggal_kembali_lama),
    constraint loan_extension_rejection_requires_reason check (
        status <> 'rejected'
        or nullif(trim(alasan_penolakan), '') is not null
    )
);

create unique index if not exists one_pending_extension_per_loan
    on public.loan_extensions(loan_transaction_id)
    where status = 'pending';

create table if not exists public.loan_audit_logs (
    id uuid primary key default gen_random_uuid(),
    loan_transaction_id uuid
        references public.loan_transactions(id) on delete set null,
    actor_id uuid references auth.users(id) on delete set null,
    action text not null,
    entity_type text not null,
    entity_id uuid,
    old_data jsonb,
    new_data jsonb,
    detail text,
    note text,
    created_at timestamptz not null default now()
);

create table if not exists public.loan_device_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    token text not null unique,
    platform text not null default 'android',
    is_active boolean not null default true,
    last_seen_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index if not exists loan_transactions_status_idx
    on public.loan_transactions(status);
create index if not exists loan_transactions_due_date_idx
    on public.loan_transactions(tanggal_kembali_rencana)
    where status = 'dipinjam';
create index if not exists loan_items_document_idx
    on public.loan_items(archive_document_id);
create index if not exists loan_audit_logs_transaction_idx
    on public.loan_audit_logs(loan_transaction_id, created_at desc);

create or replace function app_private.current_loan_role()
returns public.loan_app_role
language sql
stable
security definer
set search_path = ''
as $$
    select p.role
    from public.loan_profiles p
    where p.id = (select auth.uid())
      and p.is_active = true
$$;

create or replace function app_private.is_loan_role(
    required_role public.loan_app_role
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select coalesce(
        app_private.current_loan_role() = required_role,
        false
    )
$$;

create or replace function app_private.set_loan_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create or replace function app_private.audit_loan_row_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    row_id uuid;
    transaction_id uuid;
begin
    row_id := coalesce(new.id, old.id);
    transaction_id := case
        when tg_table_name = 'loan_transactions' then row_id
        when tg_table_name in (
            'loan_items',
            'loan_document_locks',
            'loan_extensions'
        ) then coalesce(
            new.loan_transaction_id,
            old.loan_transaction_id
        )
        else null
    end;

    insert into public.loan_audit_logs (
        loan_transaction_id,
        actor_id,
        action,
        entity_type,
        entity_id,
        old_data,
        new_data
    ) values (
        transaction_id,
        (select auth.uid()),
        upper(tg_op),
        tg_table_name,
        row_id,
        case when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old) end,
        case when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new) end
    );
    return coalesce(new, old);
end;
$$;

create or replace function app_private.protect_loan_audit_log()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    raise exception 'Loan audit logs are immutable';
end;
$$;

create or replace function app_private.enforce_loan_transition()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    actor_role public.loan_app_role;
begin
    actor_role := app_private.current_loan_role();
    if actor_role is null then
        raise exception 'Akun tidak aktif atau tidak memiliki role peminjaman';
    end if;

    if new.created_by <> old.created_by
       or new.borrower_agency_id <> old.borrower_agency_id then
        raise exception 'Pemilik dan instansi transaksi tidak boleh diubah';
    end if;

    if old.status in ('ditolak', 'dikembalikan', 'dibatalkan')
       and new.status <> old.status then
        raise exception 'Status terminal tidak dapat diubah';
    end if;

    if actor_role = 'arsiparis' then
        if not (
            (old.status = 'menunggu_persetujuan'
                and new.status = 'menunggu_persetujuan')
            or (old.status = 'menunggu_persetujuan'
                and new.status = 'disetujui'
                and new.metode_persetujuan = 'bypass')
            or (old.status in ('menunggu_persetujuan', 'disetujui')
                and new.status = 'dibatalkan')
            or (old.status = 'disetujui' and new.status = 'dipinjam')
            or (old.status = 'dipinjam' and new.status = 'dikembalikan')
        ) then
            raise exception 'Transisi tidak diizinkan untuk Arsiparis';
        end if;

        if new.status = 'disetujui'
           and (
               new.metode_persetujuan <> 'bypass'
               or new.approved_by <> (select auth.uid())
           ) then
            raise exception 'Bypass harus dilakukan oleh Arsiparis aktif';
        end if;
    elsif actor_role = 'kasubag' then
        if not (
            (old.status = 'menunggu_persetujuan'
                and new.status in ('disetujui', 'ditolak'))
            or (old.status in ('menunggu_persetujuan', 'disetujui')
                and new.status = 'dibatalkan')
            or (
                old.status = 'disetujui'
                and new.status = 'disetujui'
                and old.metode_persetujuan = 'bypass'
                and old.is_bypass_acknowledged = false
                and new.is_bypass_acknowledged = true
            )
            or (
                old.status = 'dipinjam'
                and new.status = 'dipinjam'
                and new.tanggal_kembali_rencana
                    > old.tanggal_kembali_rencana
                and (
                    to_jsonb(new)
                        - 'tanggal_kembali_rencana'
                        - 'updated_at'
                ) = (
                    to_jsonb(old)
                        - 'tanggal_kembali_rencana'
                        - 'updated_at'
                )
            )
        ) then
            raise exception 'Transisi tidak diizinkan untuk Kasubag';
        end if;

        if old.status = 'menunggu_persetujuan'
           and new.status = 'disetujui' then
            new.metode_persetujuan := 'online';
            new.approved_by := (select auth.uid());
        elsif old.status = 'menunggu_persetujuan'
           and new.status = 'ditolak' then
            new.approved_by := (select auth.uid());
        end if;
    end if;

    if new.status = 'dikembalikan' then
        new.qr_code_token := null;
        new.tanggal_kembali_aktual := coalesce(
            new.tanggal_kembali_aktual,
            current_date
        );
    end if;

    return new;
end;
$$;

create or replace function app_private.enforce_loan_extension_transition()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not app_private.is_loan_role('kasubag') then
        raise exception 'Hanya Kasubag dapat memproses perpanjangan';
    end if;

    if old.status <> 'pending'
       or new.status not in ('approved', 'rejected') then
        raise exception 'Transisi perpanjangan tidak valid';
    end if;

    new.approved_by := (select auth.uid());

    if new.status = 'approved' then
        update public.loan_transactions
        set tanggal_kembali_rencana = new.tanggal_kembali_baru
        where id = new.loan_transaction_id
          and status = 'dipinjam';

        if not found then
            raise exception
                'Transaksi tidak ditemukan atau tidak sedang dipinjam';
        end if;
    end if;

    return new;
end;
$$;

create or replace function app_private.handle_new_loan_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    requested_role public.loan_app_role;
begin
    requested_role := case new.raw_app_meta_data ->> 'app_role'
        when 'arsiparis' then 'arsiparis'::public.loan_app_role
        when 'kasubag' then 'kasubag'::public.loan_app_role
        else null
    end;

    insert into public.loan_profiles (
        id,
        username,
        nama_lengkap,
        role,
        is_active
    ) values (
        new.id,
        coalesce(
            nullif(new.raw_app_meta_data ->> 'username', ''),
            split_part(coalesce(new.email, new.id::text), '@', 1)
        ),
        coalesce(
            nullif(new.raw_app_meta_data ->> 'nama_lengkap', ''),
            split_part(coalesce(new.email, 'Pengguna'), '@', 1)
        ),
        coalesce(requested_role, 'arsiparis'::public.loan_app_role),
        requested_role is not null
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created_for_loan_module on auth.users;
create trigger on_auth_user_created_for_loan_module
    after insert on auth.users
    for each row execute function app_private.handle_new_loan_user();

-- Backfill Auth users that existed before this module migration. They remain
-- inactive until an administrator explicitly assigns a loan-module role.
insert into public.loan_profiles (
    id,
    username,
    nama_lengkap,
    role,
    is_active
)
select
    u.id,
    coalesce(
        nullif(u.raw_app_meta_data ->> 'username', ''),
        split_part(coalesce(u.email, u.id::text), '@', 1)
    ),
    coalesce(
        nullif(u.raw_app_meta_data ->> 'nama_lengkap', ''),
        split_part(coalesce(u.email, 'Pengguna'), '@', 1)
    ),
    case u.raw_app_meta_data ->> 'app_role'
        when 'kasubag' then 'kasubag'::public.loan_app_role
        else 'arsiparis'::public.loan_app_role
    end,
    (u.raw_app_meta_data ->> 'app_role') in ('arsiparis', 'kasubag')
from auth.users u
on conflict (id) do nothing;

do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'loan_profiles',
        'loan_borrower_agencies',
        'loan_transactions',
        'loan_extensions'
    ]
    loop
        execute format(
            'drop trigger if exists set_loan_updated_at on public.%I',
            table_name
        );
        execute format(
            'create trigger set_loan_updated_at before update on public.%I ' ||
            'for each row execute function app_private.set_loan_updated_at()',
            table_name
        );
    end loop;
end $$;

do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'loan_borrower_agencies',
        'loan_transactions',
        'loan_items',
        'loan_document_locks',
        'loan_extensions',
        'loan_device_tokens'
    ]
    loop
        execute format(
            'drop trigger if exists audit_loan_changes on public.%I',
            table_name
        );
        execute format(
            'create trigger audit_loan_changes ' ||
            'after insert or update or delete on public.%I ' ||
            'for each row execute function app_private.audit_loan_row_change()',
            table_name
        );
    end loop;
end $$;

drop trigger if exists loan_audit_logs_immutable
    on public.loan_audit_logs;
create trigger loan_audit_logs_immutable
    before update or delete on public.loan_audit_logs
    for each row execute function app_private.protect_loan_audit_log();

drop trigger if exists enforce_loan_transition
    on public.loan_transactions;
create trigger enforce_loan_transition
    before update on public.loan_transactions
    for each row execute function app_private.enforce_loan_transition();

drop trigger if exists enforce_loan_extension_transition
    on public.loan_extensions;
create trigger enforce_loan_extension_transition
    before update of status on public.loan_extensions
    for each row
    when (old.status is distinct from new.status)
    execute function app_private.enforce_loan_extension_transition();

alter table public.loan_profiles enable row level security;
alter table public.loan_borrower_agencies enable row level security;
alter table public.loan_transactions enable row level security;
alter table public.loan_items enable row level security;
alter table public.loan_document_locks enable row level security;
alter table public.loan_extensions enable row level security;
alter table public.loan_audit_logs enable row level security;
alter table public.loan_device_tokens enable row level security;

grant usage on schema public to authenticated;
grant usage on schema app_private to authenticated;
grant execute on function app_private.current_loan_role()
    to authenticated;
grant execute on function app_private.is_loan_role(public.loan_app_role)
    to authenticated;

grant select on public.loan_profiles to authenticated;
grant select, insert, update, delete
    on public.loan_borrower_agencies to authenticated;
grant select, insert, update
    on public.loan_transactions to authenticated;
grant select, insert, update
    on public.loan_items to authenticated;
grant select, insert, update
    on public.loan_document_locks to authenticated;
grant select, insert, update
    on public.loan_extensions to authenticated;
grant select on public.loan_audit_logs to authenticated;
grant select, insert, update, delete
    on public.loan_device_tokens to authenticated;

do $$
declare
    sequence_name text;
begin
    foreach sequence_name in array array[
        pg_get_serial_sequence('public.loan_profiles', 'legacy_id'),
        pg_get_serial_sequence('public.loan_borrower_agencies', 'legacy_id'),
        pg_get_serial_sequence('public.loan_transactions', 'legacy_id'),
        pg_get_serial_sequence('public.loan_extensions', 'legacy_id')
    ]
    loop
        if sequence_name is not null then
            execute format(
                'grant usage, select on sequence %s to authenticated',
                sequence_name
            );
        end if;
    end loop;
end $$;

create policy loan_profiles_read_authenticated
    on public.loan_profiles for select to authenticated
    using ((select auth.uid()) is not null);

create policy loan_agencies_read_authenticated
    on public.loan_borrower_agencies for select to authenticated
    using (true);
create policy loan_agencies_write_arsiparis
    on public.loan_borrower_agencies for all to authenticated
    using (app_private.is_loan_role('arsiparis'))
    with check (app_private.is_loan_role('arsiparis'));

create policy loan_transactions_read_authenticated
    on public.loan_transactions for select to authenticated
    using (true);
create policy loan_transactions_insert_arsiparis
    on public.loan_transactions for insert to authenticated
    with check (
        app_private.is_loan_role('arsiparis')
        and created_by = (select auth.uid())
        and status = 'menunggu_persetujuan'
    );
create policy loan_transactions_update_roles
    on public.loan_transactions for update to authenticated
    using (
        app_private.is_loan_role('arsiparis')
        or app_private.is_loan_role('kasubag')
    )
    with check (
        app_private.is_loan_role('arsiparis')
        or app_private.is_loan_role('kasubag')
    );

create policy loan_items_read_authenticated
    on public.loan_items for select to authenticated
    using (true);
create policy loan_items_write_arsiparis
    on public.loan_items for all to authenticated
    using (app_private.is_loan_role('arsiparis'))
    with check (app_private.is_loan_role('arsiparis'));

create policy loan_locks_read_authenticated
    on public.loan_document_locks for select to authenticated
    using (true);
create policy loan_locks_write_arsiparis
    on public.loan_document_locks for all to authenticated
    using (app_private.is_loan_role('arsiparis'))
    with check (
        app_private.is_loan_role('arsiparis')
        and locked_by = (select auth.uid())
    );

create policy loan_extensions_read_authenticated
    on public.loan_extensions for select to authenticated
    using (true);
create policy loan_extensions_insert_arsiparis
    on public.loan_extensions for insert to authenticated
    with check (
        app_private.is_loan_role('arsiparis')
        and created_by = (select auth.uid())
        and status = 'pending'
    );
create policy loan_extensions_update_kasubag
    on public.loan_extensions for update to authenticated
    using (app_private.is_loan_role('kasubag'))
    with check (app_private.is_loan_role('kasubag'));

create policy loan_audit_read_authenticated
    on public.loan_audit_logs for select to authenticated
    using (true);

create policy loan_device_tokens_own_rows
    on public.loan_device_tokens for all to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
) values (
    'loan-documents',
    'loan-documents',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists loan_documents_read_authenticated
    on storage.objects;
create policy loan_documents_read_authenticated
    on storage.objects for select to authenticated
    using (bucket_id = 'loan-documents');

drop policy if exists loan_documents_insert_arsiparis
    on storage.objects;
create policy loan_documents_insert_arsiparis
    on storage.objects for insert to authenticated
    with check (
        bucket_id = 'loan-documents'
        and app_private.is_loan_role('arsiparis')
        and owner_id = (select auth.uid()::text)
    );

drop policy if exists loan_documents_update_owner
    on storage.objects;
create policy loan_documents_update_owner
    on storage.objects for update to authenticated
    using (
        bucket_id = 'loan-documents'
        and app_private.is_loan_role('arsiparis')
        and owner_id = (select auth.uid()::text)
    )
    with check (
        bucket_id = 'loan-documents'
        and app_private.is_loan_role('arsiparis')
        and owner_id = (select auth.uid()::text)
    );

drop policy if exists loan_documents_delete_owner
    on storage.objects;
create policy loan_documents_delete_owner
    on storage.objects for delete to authenticated
    using (
        bucket_id = 'loan-documents'
        and app_private.is_loan_role('arsiparis')
        and owner_id = (select auth.uid()::text)
    );
