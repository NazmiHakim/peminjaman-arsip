-- The base schema already enforces one pending extension per transaction,
-- validates the date transition, and limits approval to Kasubag through RLS
-- and the transition trigger. This migration adds bounded free-text fields.

alter table public.loan_extensions
    drop constraint if exists loan_extension_reason_length,
    drop constraint if exists loan_extension_rejection_reason_length;

alter table public.loan_extensions
    add constraint loan_extension_reason_length
        check (char_length(trim(alasan)) between 1 and 500) not valid,
    add constraint loan_extension_rejection_reason_length
        check (
            alasan_penolakan is null
            or char_length(trim(alasan_penolakan)) between 1 and 500
        ) not valid;

alter table public.loan_extensions
    validate constraint loan_extension_reason_length;
alter table public.loan_extensions
    validate constraint loan_extension_rejection_reason_length;
