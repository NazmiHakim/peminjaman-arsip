-- Keep server-side validation aligned with the Android domain rules.
-- UI filters are only a convenience; these constraints are the final guard.

alter table public.loan_borrower_agencies
    drop constraint if exists loan_agency_name_length,
    drop constraint if exists loan_agency_name_format;

alter table public.loan_borrower_agencies
    add constraint loan_agency_name_length
        check (char_length(trim(nama_instansi)) between 1 and 150) not valid,
    add constraint loan_agency_name_format
        check (nama_instansi ~ '^[[:alpha:][:digit:][:space:].,''()/&+ -]+$') not valid;

alter table public.loan_transactions
    drop constraint if exists loan_transactions_pic_no_hp_check,
    drop constraint if exists loan_applicant_name_length,
    drop constraint if exists loan_applicant_name_format,
    drop constraint if exists loan_phone_format,
    drop constraint if exists loan_letter_number_length,
    drop constraint if exists loan_letter_number_format,
    drop constraint if exists loan_bypass_note_length;

alter table public.loan_transactions
    add constraint loan_applicant_name_length
        check (char_length(trim(pic_nama)) between 1 and 50) not valid,
    add constraint loan_applicant_name_format
        check (pic_nama ~ '^[[:alpha:][:space:].,''() -]+$') not valid,
    add constraint loan_phone_format
        check (pic_no_hp ~ '^[0-9]{10,15}$') not valid,
    add constraint loan_letter_number_length
        check (char_length(trim(nomor_surat_pengantar)) between 1 and 100) not valid,
    add constraint loan_letter_number_format
        check (
            nomor_surat_pengantar
                ~ '^[[:alpha:][:digit:][:space:]/.,_:;()''&+_-]+$'
        ) not valid,
    add constraint loan_bypass_note_length
        check (
            catatan_bypass is null
            or char_length(trim(catatan_bypass)) between 1 and 500
        ) not valid;

alter table public.loan_borrower_agencies
    validate constraint loan_agency_name_length;
alter table public.loan_borrower_agencies
    validate constraint loan_agency_name_format;

alter table public.loan_transactions
    validate constraint loan_applicant_name_length;
alter table public.loan_transactions
    validate constraint loan_applicant_name_format;
alter table public.loan_transactions
    validate constraint loan_phone_format;
alter table public.loan_transactions
    validate constraint loan_letter_number_length;
alter table public.loan_transactions
    validate constraint loan_letter_number_format;
alter table public.loan_transactions
    validate constraint loan_bypass_note_length;
