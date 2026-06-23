-- Read-only audit against SCHEMADOKUMENKEUANGAN.md.
-- No database object is modified.

with expected_tables(table_name) as (
    values
        ('archive_classifications'),
        ('staging_documents'),
        ('storage_locations'),
        ('archive_documents'),
        ('document_placements'),
        ('activity_logs')
)
select
    e.table_name,
    case when t.table_name is null then 'MISSING' else 'OK' end as status
from expected_tables e
left join information_schema.tables t
    on t.table_schema = 'public'
   and t.table_name = e.table_name
order by e.table_name;

with expected_columns(table_name, column_name, data_type) as (
    values
        ('archive_classifications', 'code', 'text'),
        ('archive_classifications', 'name', 'text'),
        ('archive_classifications', 'parent_code', 'text'),
        ('archive_classifications', 'level', 'integer'),
        ('archive_classifications', 'is_active', 'boolean'),
        ('archive_classifications', 'created_at', 'timestamp with time zone'),
        ('archive_classifications', 'updated_at', 'timestamp with time zone'),
        ('staging_documents', 'id', 'uuid'),
        ('staging_documents', 'document_type', 'USER-DEFINED'),
        ('staging_documents', 'document_number', 'text'),
        ('staging_documents', 'classification_code', 'text'),
        ('staging_documents', 'title', 'text'),
        ('staging_documents', 'description', 'text'),
        ('staging_documents', 'year', 'integer'),
        ('staging_documents', 'physical_form', 'USER-DEFINED'),
        ('staging_documents', 'condition', 'USER-DEFINED'),
        ('staging_documents', 'copy_count', 'integer'),
        ('staging_documents', 'is_copy', 'boolean'),
        ('staging_documents', 'status', 'USER-DEFINED'),
        ('staging_documents', 'origin_instance', 'text'),
        ('staging_documents', 'source', 'USER-DEFINED'),
        ('staging_documents', 'created_by', 'uuid'),
        ('staging_documents', 'updated_by', 'uuid'),
        ('staging_documents', 'created_at', 'timestamp with time zone'),
        ('staging_documents', 'updated_at', 'timestamp with time zone'),
        ('storage_locations', 'id', 'uuid'),
        ('storage_locations', 'room', 'text'),
        ('storage_locations', 'shelf', 'text'),
        ('storage_locations', 'box_number', 'text'),
        ('storage_locations', 'description', 'text'),
        ('storage_locations', 'is_active', 'boolean'),
        ('storage_locations', 'created_at', 'timestamp with time zone'),
        ('storage_locations', 'updated_at', 'timestamp with time zone'),
        ('archive_documents', 'id', 'uuid'),
        ('archive_documents', 'document_type', 'USER-DEFINED'),
        ('archive_documents', 'document_number', 'text'),
        ('archive_documents', 'classification_code', 'text'),
        ('archive_documents', 'title', 'text'),
        ('archive_documents', 'description', 'text'),
        ('archive_documents', 'year', 'integer'),
        ('archive_documents', 'physical_form', 'USER-DEFINED'),
        ('archive_documents', 'condition', 'USER-DEFINED'),
        ('archive_documents', 'copy_count', 'integer'),
        ('archive_documents', 'is_copy', 'boolean'),
        ('archive_documents', 'status', 'USER-DEFINED'),
        ('archive_documents', 'origin_instance', 'text'),
        ('archive_documents', 'storage_location_id', 'uuid'),
        ('archive_documents', 'source_staging_id', 'uuid'),
        ('archive_documents', 'created_by', 'uuid'),
        ('archive_documents', 'updated_by', 'uuid'),
        ('archive_documents', 'created_at', 'timestamp with time zone'),
        ('archive_documents', 'updated_at', 'timestamp with time zone'),
        ('archive_documents', 'deleted_at', 'timestamp with time zone'),
        ('document_placements', 'id', 'uuid'),
        ('document_placements', 'archive_document_id', 'uuid'),
        ('document_placements', 'storage_location_id', 'uuid'),
        ('document_placements', 'placed_at', 'timestamp with time zone'),
        ('document_placements', 'removed_at', 'timestamp with time zone'),
        ('document_placements', 'note', 'text'),
        ('document_placements', 'created_by', 'uuid'),
        ('document_placements', 'created_at', 'timestamp with time zone'),
        ('activity_logs', 'id', 'uuid'),
        ('activity_logs', 'actor_id', 'uuid'),
        ('activity_logs', 'action', 'text'),
        ('activity_logs', 'entity_type', 'text'),
        ('activity_logs', 'entity_id', 'uuid'),
        ('activity_logs', 'metadata', 'jsonb'),
        ('activity_logs', 'created_at', 'timestamp with time zone')
)
select
    e.table_name,
    e.column_name,
    e.data_type as expected_type,
    coalesce(c.data_type, 'MISSING') as actual_type,
    case
        when c.column_name is null then 'MISSING'
        when c.data_type <> e.data_type then 'TYPE_MISMATCH'
        else 'OK'
    end as status
from expected_columns e
left join information_schema.columns c
    on c.table_schema = 'public'
   and c.table_name = e.table_name
   and c.column_name = e.column_name
where c.column_name is null
   or c.data_type <> e.data_type
order by e.table_name, e.column_name;

-- Inspect actual enum names and labels. Their values belong to the Financial
-- Document module and must be consumed exactly, not redefined here.
select
    c.table_name,
    c.column_name,
    c.udt_name as enum_type,
    array_agg(e.enumlabel order by e.enumsortorder) as enum_labels
from information_schema.columns c
join pg_type t on t.typname = c.udt_name
join pg_enum e on e.enumtypid = t.oid
where c.table_schema = 'public'
  and c.table_name in ('staging_documents', 'archive_documents')
  and c.data_type = 'USER-DEFINED'
group by c.table_name, c.column_name, c.udt_name
order by c.table_name, c.column_name;
