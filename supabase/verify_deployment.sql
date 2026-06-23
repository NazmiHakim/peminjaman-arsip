-- Read-only post-deployment verification.

select
    expected.expected_name as table_name,
    case
        when actual.table_name is null then 'MISSING'
        else 'OK'
    end as status
from (
    values
        ('archive_documents'),
        ('loan_profiles'),
        ('loan_borrower_agencies'),
        ('loan_transactions'),
        ('loan_items'),
        ('loan_document_locks'),
        ('loan_extensions'),
        ('loan_audit_logs'),
        ('loan_device_tokens')
) expected(expected_name)
left join information_schema.tables actual
    on actual.table_schema = 'public'
   and actual.table_name = expected.expected_name
order by expected.expected_name;

select
    c.relname as table_name,
    c.relrowsecurity as rls_enabled
from pg_class c
where c.relnamespace = 'public'::regnamespace
  and c.relname like 'loan_%'
  and c.relkind = 'r'
order by c.relname;

select
    tablename,
    policyname,
    cmd,
    roles
from pg_policies
where schemaname = 'public'
  and tablename like 'loan_%'
order by tablename, policyname;

select
    trigger_name,
    event_object_schema,
    event_object_table
from information_schema.triggers
where trigger_name in (
    'on_auth_user_created_for_loan_module',
    'enforce_loan_transition',
    'enforce_loan_extension_transition',
    'audit_loan_changes',
    'loan_audit_logs_immutable'
)
order by event_object_schema, event_object_table, trigger_name;

select
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
from storage.buckets
where id = 'loan-documents';

select
    u.email,
    p.username,
    p.role,
    p.is_active,
    u.email_confirmed_at,
    u.last_sign_in_at
from auth.users u
left join public.loan_profiles p on p.id = u.id
order by u.created_at desc;
