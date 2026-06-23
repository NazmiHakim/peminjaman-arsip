-- Stable client-generated key makes retries idempotent when Android writes
-- locally first and reconnects later.
alter table public.loan_transactions
    add column if not exists client_reference text;

create unique index if not exists loan_transactions_client_reference_key
    on public.loan_transactions(client_reference)
    where client_reference is not null;

alter table public.loan_borrower_agencies
    drop constraint if exists loan_agency_code_format,
    drop constraint if exists loan_agency_address_length;

alter table public.loan_borrower_agencies
    add constraint loan_agency_code_format check (
        kode_instansi is null
        or kode_instansi ~ '^[A-Z0-9][A-Z0-9_-]{1,19}$'
    ) not valid,
    add constraint loan_agency_address_length check (
        alamat is null
        or char_length(trim(alamat)) between 1 and 255
    ) not valid;

alter table public.loan_borrower_agencies
    validate constraint loan_agency_code_format;
alter table public.loan_borrower_agencies
    validate constraint loan_agency_address_length;

-- Terminal transactions must release their document locks even when the
-- client disconnects immediately after updating the status.
create or replace function app_private.release_terminal_loan_locks()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.status in ('ditolak', 'dikembalikan', 'dibatalkan')
       and old.status is distinct from new.status then
        update public.loan_document_locks
        set
            released_at = now(),
            release_reason = new.status::text
        where loan_transaction_id = new.id
          and released_at is null;
    end if;
    return new;
end;
$$;

drop trigger if exists release_terminal_loan_locks
    on public.loan_transactions;
create trigger release_terminal_loan_locks
    after update of status on public.loan_transactions
    for each row execute function app_private.release_terminal_loan_locks();
