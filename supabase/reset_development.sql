-- DEVELOPMENT RESET ONLY
--
-- Removes:
-- 1. Previous/legacy Document Loan module objects.
-- 2. Current loan_* module objects.
-- 3. The temporary Financial Document stub.
-- 4. Storage policies for the private loan-documents bucket.
--
-- Preserves:
-- - auth.users and every other Supabase Auth object;
-- - all Storage buckets and objects;
-- - Supabase system schemas and extensions.
--
-- DO NOT RUN after integration with the real Financial Document module.

begin;

-- Supabase prevents direct SQL deletion from storage.objects. Delete the
-- loan-documents bucket through Storage Dashboard before or after this reset:
-- Storage -> loan-documents -> Empty bucket -> Delete bucket.
--
-- Only remove policies that were installed by this module.
drop policy if exists loan_documents_read_authenticated
    on storage.objects;
drop policy if exists loan_documents_insert_arsiparis
    on storage.objects;
drop policy if exists loan_documents_update_owner
    on storage.objects;
drop policy if exists loan_documents_delete_owner
    on storage.objects;

-- Remove Auth triggers created by old and current loan migrations.
drop trigger if exists on_auth_user_created on auth.users;
drop trigger if exists on_auth_user_created_for_loan_module on auth.users;

-- Module-owned tables: children before parents.
drop table if exists public.loan_device_tokens cascade;
drop table if exists public.device_tokens cascade;

drop table if exists public.loan_audit_logs cascade;
drop table if exists public.audit_logs cascade;

drop table if exists public.loan_extensions cascade;
drop table if exists public.loan_document_locks cascade;
drop table if exists public.loan_items cascade;
drop table if exists public.loan_transactions cascade;

drop table if exists public.loan_borrower_agencies cascade;
drop table if exists public.borrower_agencies cascade;

drop table if exists public.loan_profiles cascade;
drop table if exists public.profiles cascade;

-- Development-only Financial Document stub: children before parents.
drop table if exists public.document_placements cascade;
drop table if exists public.archive_documents cascade;
drop table if exists public.staging_documents cascade;
drop table if exists public.storage_locations cascade;
drop table if exists public.archive_classifications cascade;
drop table if exists public.activity_logs cascade;

-- Private helper functions from all previous iterations.
drop schema if exists app_private cascade;

-- Current loan-module enums.
drop type if exists public.loan_extension_status cascade;
drop type if exists public.loan_return_condition cascade;
drop type if exists public.loan_approval_method cascade;
drop type if exists public.loan_transaction_status cascade;
drop type if exists public.loan_app_role cascade;

-- Legacy loan-module enums.
drop type if exists public.extension_status cascade;
drop type if exists public.approval_method cascade;
drop type if exists public.loan_status cascade;
drop type if exists public.app_role cascade;

-- Financial Document stub enums.
drop type if exists public.staging_document_source cascade;
drop type if exists public.document_status cascade;
drop type if exists public.document_condition cascade;
drop type if exists public.physical_form cascade;
drop type if exists public.document_type cascade;

commit;

-- Verification: this result set should contain zero rows.
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
      'profiles',
      'borrower_agencies',
      'audit_logs',
      'device_tokens',
      'loan_profiles',
      'loan_borrower_agencies',
      'loan_transactions',
      'loan_items',
      'loan_document_locks',
      'loan_extensions',
      'loan_audit_logs',
      'loan_device_tokens',
      'archive_classifications',
      'staging_documents',
      'storage_locations',
      'archive_documents',
      'document_placements',
      'activity_logs'
  )
order by table_name;

-- Informational only. If this returns a row, remove the bucket through the
-- Supabase Storage Dashboard; do not DELETE from storage tables using SQL.
select
    id,
    name,
    'DELETE_MANUALLY_VIA_STORAGE_DASHBOARD' as required_action
from storage.buckets
where id = 'loan-documents';
