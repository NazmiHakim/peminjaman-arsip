-- Additional dummy archive records.
-- Run AFTER seed.sql. Existing rows are preserved and document numbers are
-- checked before insert, so this file is safe to run repeatedly.
--
-- Source: C:/Users/USER/Downloads/Rekap Arsip Randy.xlsx
-- Selection: two additional representative records from every 2016-2022
-- worksheet (14 records total).

begin;

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
            'Unable to resolve Financial Document enum labels.';
    end if;

    select id
    into location_id
    from public.storage_locations
    where room = 'Arsip 1'
      and shelf = 'Rak 1 - Tingkat 1'
      and box_number = 'Box 1'
    order by created_at
    limit 1;

    if location_id is null then
        raise exception 'Run seed.sql first: dummy storage location is missing.';
    end if;

    for record_data in
        select *
        from (
            values
                (
                    '04970/SP2D/1.08.01.01/DPPKAD/2016',
                    'Pembayaran Tambahan Penghasilan Uang Makan Harian PNS BLHK Bulan September 2016',
                    2016, false, 'Randy-2016', 2016
                ),
                (
                    '04978/SP2D/1.08.01.01/DPPKAD/2016',
                    'Pembayaran Ganti Uang Persediaan Bulan Oktober 2016',
                    2016, false, 'Randy-2016', 2016
                ),
                (
                    '09164/SP2D/3.03.01.01/BUD/2017',
                    'Pembangunan Jalan Produksi Juuh Kecamatan Tebing Tinggi',
                    2017, false, 'Randy-2017', 2016
                ),
                (
                    '04564/SP2D/4.04.01.02/BUD/2017',
                    'Belanja Hibah Tahap Pertama kepada PAUD Terpadu Kartini Desa Muara Ninian',
                    2017, false, 'Randy-2017', 2016
                ),
                (
                    '08176/SP2D/3.03.01.01/2018',
                    'Jasa Konsultansi Perencanaan Mandiri Benih Tanaman Pangan Kabupaten Balangan',
                    2018, true, 'Randy-2018', 2016
                ),
                (
                    '03471/SP2D/3.03.01.01/2018',
                    'Jasa Konsultansi Perencana Rehab Rumah Dinas Awayan',
                    2018, true, 'Randy-2018', 2016
                ),
                (
                    '04570/SP2D/3.06.01.01/BUD/2019',
                    'Belanja Sewa Stand, Sewa Panggung Hiburan dan Pengisi Acara Balangan Expo',
                    2019, true, 'Randy-2019', 2018
                ),
                (
                    '05665/SP2D/4.01.04.01/BUD/2019',
                    'Pemeliharaan Rehab Gedung A Sekretariat DPRD',
                    2019, true, 'Randy-2019', 2018
                ),
                (
                    '06904/SP2D/4.01.11.01/BUD/2020',
                    'Pengadaan Kendaraan Dinas Operasional Roda Dua',
                    2020, true, 'Randy-2020', 2020
                ),
                (
                    '06911/SP2D/3.06.01.01/BUD/2020',
                    'SPJ Kegiatan Desember IV',
                    2020, true, 'Randy-2020', 2020
                ),
                (
                    '01414/SP2D/1.04.01/BUD/2021',
                    'Honorarium atau Insentif Petugas Taman Hijau Bulan Mei 2021',
                    2021, true, 'Randy-2021', 2020
                ),
                (
                    '01404/SP2D/1.06.01/BUD/2021',
                    'Belanja Bantuan Sosial kepada LKSA Nurul Iman Desa Kalanlang Kecamatan Paringin',
                    2021, true, 'Randy-2021', 2020
                ),
                (
                    '00909/SP2D/BUD/1-03-1-04-0-00-1-0-0/2022',
                    'Angsuran Termyn 95 Persen Normalisasi Sungai Balanti Desa Jimamun',
                    2022, true, 'Randy-2022', 2022
                ),
                (
                    '00922/SP2D/BUD/1-03-1-04-0-00-1.0.0/2022',
                    'Uang Muka 50 Persen Pembangunan Toilet Individual Desa Murung Jambu',
                    2022, true, 'Randy-2022', 2022
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
                    document_type, document_number, classification_code,
                    title, description, year, physical_form, condition,
                    copy_count, is_copy, status, origin_instance,
                    storage_location_id
                ) values (
                    $1::%s, $2, $3, $4, $5, $6, $7::%s, $8::%s,
                    $9, $10, $11::%s, $12, $13
                ) returning id',
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
                    'Dummy tambahan Rekap Arsip Randy.xlsx, sheet %s. Nilai kolom TAHUN sumber: %s.',
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
                'Penempatan dummy tambahan Rekap Arsip Randy.xlsx'
            );

            insert into public.activity_logs (
                action,
                entity_type,
                entity_id,
                metadata
            ) values (
                'ADDITIONAL_DUMMY_ARCHIVE_IMPORTED',
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

-- Should return 14 rows after the first successful run.
select
    document_number,
    title,
    year,
    status
from public.archive_documents
where description like 'Dummy tambahan Rekap Arsip Randy.xlsx%'
order by year, document_number;
