-- Optional temporary dummy data for the external Financial Document module.
-- Source: C:/Users/USER/Downloads/Rekap Arsip Randy.xlsx
--
-- This file respects SCHEMADOKUMENKEUANGAN.md:
-- - no extra columns are assumed;
-- - enum types are discovered from the existing table contract;
-- - enum labels are validated before inserting;
-- - records are inserted only when document_number is not already present.

begin;

insert into public.archive_classifications (
    code,
    name,
    level,
    is_active
)
select
    '900.1.3.1',
    'Keuangan - Surat Perintah Pencairan Dana (SP2D)',
    4,
    true
where not exists (
    select 1
    from public.archive_classifications
    where code = '900.1.3.1'
);

insert into public.storage_locations (
    room,
    shelf,
    box_number,
    description,
    is_active
)
select
    'Arsip 1',
    'Rak 1 - Tingkat 1',
    'Box 1',
    'Lokasi dummy dari Rekap Arsip Randy.xlsx',
    true
where not exists (
    select 1
    from public.storage_locations
    where room = 'Arsip 1'
      and shelf = 'Rak 1 - Tingkat 1'
      and box_number = 'Box 1'
);

do $seed$
declare
    document_type_type regtype;
    physical_form_type regtype;
    document_condition_type regtype;
    document_status_type regtype;
    document_type_label text;
    physical_form_label text;
    good_condition_label text;
    available_status_label text;
    location_id uuid;
    record_data record;
    inserted_document_id uuid;
begin
    select a.atttypid::regtype
    into document_type_type
    from pg_attribute a
    where a.attrelid = 'public.archive_documents'::regclass
      and a.attname = 'document_type'
      and not a.attisdropped;

    select a.atttypid::regtype
    into physical_form_type
    from pg_attribute a
    where a.attrelid = 'public.archive_documents'::regclass
      and a.attname = 'physical_form'
      and not a.attisdropped;

    select a.atttypid::regtype
    into document_condition_type
    from pg_attribute a
    where a.attrelid = 'public.archive_documents'::regclass
      and a.attname = 'condition'
      and not a.attisdropped;

    select a.atttypid::regtype
    into document_status_type
    from pg_attribute a
    where a.attrelid = 'public.archive_documents'::regclass
      and a.attname = 'status'
      and not a.attisdropped;

    select e.enumlabel
    into document_type_label
    from pg_enum e
    where e.enumtypid = document_type_type
    order by
        case
            when upper(e.enumlabel) = 'SP2D' then 0
            when upper(e.enumlabel) like '%SP2D%' then 1
            else 2
        end,
        e.enumsortorder
    limit 1;

    select e.enumlabel
    into physical_form_label
    from pg_enum e
    where e.enumtypid = physical_form_type
    order by
        case
            when upper(e.enumlabel) in ('PAPER', 'KERTAS') then 0
            when upper(e.enumlabel) in ('PHYSICAL', 'FISIK') then 1
            else 2
        end,
        e.enumsortorder
    limit 1;

    select e.enumlabel
    into good_condition_label
    from pg_enum e
    where e.enumtypid = document_condition_type
    order by
        case
            when upper(e.enumlabel) = 'GOOD' then 0
            when upper(e.enumlabel) = 'BAIK' then 1
            else 2
        end,
        e.enumsortorder
    limit 1;

    select e.enumlabel
    into available_status_label
    from pg_enum e
    where e.enumtypid = document_status_type
    order by
        case
            when upper(e.enumlabel) = 'AVAILABLE' then 0
            when upper(e.enumlabel) = 'TERSEDIA' then 1
            else 2
        end,
        e.enumsortorder
    limit 1;

    if document_type_label is null
       or physical_form_label is null
       or good_condition_label is null
       or available_status_label is null then
        raise exception
            'Unable to resolve Financial Document enum labels. Confirm the shared schema before seeding.';
    end if;

    select id
    into location_id
    from public.storage_locations
    where room = 'Arsip 1'
      and shelf = 'Rak 1 - Tingkat 1'
      and box_number = 'Box 1'
    order by created_at
    limit 1;

    for record_data in
        select *
        from (
            values
                (
                    '04969/SP2D/1.08.01.01/DPPKAD/2016',
                    'Pembayaran Tambahan Penghasilan Tunjangan Daerah PNS BLHK Bulan September',
                    2016,
                    false,
                    'Randy-2016',
                    2016
                ),
                (
                    '09770/SP20/1.03.01.01/BUD/2017',
                    'Angsuran Pemeliharaan 5% Pekerjaan Normalisasi Sungai Desa Simpang Nadung',
                    2017,
                    false,
                    'Randy-2017',
                    2016
                ),
                (
                    '07310/SP2D/3.03.01.01/2018',
                    'Pembayaran Belanja Jasa Konsultansi Perencanaan Jalan Usaha Tani Sebanyak 1 Paket',
                    2018,
                    true,
                    'Randy-2018',
                    2016
                ),
                (
                    '04794/SP2D/1.03.01.01/BUD/2019',
                    'Angsuran Pemeliharaan 5% Pekerjaan Normalisasi Sungai Desa Banua Hanyar Kecamatan Batumandi',
                    2019,
                    true,
                    'Randy-2019',
                    2018
                ),
                (
                    '06807/SP2D/2.10.01.01/BUD/2020',
                    'Publikasi Kegiatan Pemerintah Kabupaten Balangan di Media Online LKBN Antara Kalsel Edisi Desember 2020',
                    2020,
                    true,
                    'Randy-2020',
                    2020
                ),
                (
                    '01402/SP2D/4.11.06/BUD/2021',
                    'Belanja Tunjangan Hari Raya Kecamatan Paringin Selatan',
                    2021,
                    true,
                    'Randy-2021',
                    2020
                ),
                (
                    '01257/SP2D/BUD/1-03-1-04-0-00-1.0.0/2022',
                    'Angsuran Pemeliharaan 5% Pembangunan/Peningkatan Jalan Lingkungan Kelurahan Batu Piring RT 11 Kecamatan Paringin Selatan',
                    2022,
                    true,
                    'Randy-2022',
                    2022
                )
        ) as samples(
            document_number,
            title,
            document_year,
            is_copy,
            source_sheet,
            source_excel_year
        )
    loop
        if not exists (
            select 1
            from public.archive_documents d
            where d.document_number = record_data.document_number
              and d.deleted_at is null
        ) then
            execute format(
                'insert into public.archive_documents (
                    document_type,
                    document_number,
                    classification_code,
                    title,
                    description,
                    year,
                    physical_form,
                    condition,
                    copy_count,
                    is_copy,
                    status,
                    origin_instance,
                    storage_location_id
                ) values (
                    $1::%s, $2, $3, $4, $5, $6, $7::%s,
                    $8::%s, $9, $10, $11::%s, $12, $13
                )
                returning id',
                document_type_type,
                physical_form_type,
                document_condition_type,
                document_status_type
            )
            into inserted_document_id
            using
                document_type_label,
                record_data.document_number,
                '900.1.3.1',
                record_data.title,
                format(
                    'Dummy import Rekap Arsip Randy.xlsx, sheet %s. Nilai kolom TAHUN sumber: %s.',
                    record_data.source_sheet,
                    record_data.source_excel_year
                ),
                record_data.document_year,
                physical_form_label,
                good_condition_label,
                1,
                record_data.is_copy,
                available_status_label,
                'BPKPAD Kabupaten Balangan',
                location_id;

            insert into public.document_placements (
                archive_document_id,
                storage_location_id,
                note
            ) values (
                inserted_document_id,
                location_id,
                'Penempatan awal dummy import Rekap Arsip Randy.xlsx'
            );

            insert into public.activity_logs (
                action,
                entity_type,
                entity_id,
                metadata
            ) values (
                'DUMMY_ARCHIVE_IMPORTED',
                'archive_documents',
                inserted_document_id,
                jsonb_build_object(
                    'source_file', 'Rekap Arsip Randy.xlsx',
                    'source_sheet', record_data.source_sheet
                )
            );
        end if;
    end loop;
end
$seed$;

commit;

select
    document_number,
    title,
    year,
    physical_form,
    condition,
    status
from public.archive_documents
where description like 'Dummy import Rekap Arsip Randy.xlsx%'
order by year, document_number;
