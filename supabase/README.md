# Supabase setup

## Integration ownership

The Financial Document module owns `archive_classifications`,
`staging_documents`, `storage_locations`, `archive_documents`,
`document_placements`, and `activity_logs`. This loan module must not redefine
their enums, columns, foreign keys, RLS, triggers, or indexes.

Run `verify_financial_document_contract.sql` first. Resolve every `MISSING` or
`TYPE_MISMATCH` result with the Financial Document module owner. The loan module
couples to the shared schema only through
`loan_items.archive_document_id -> archive_documents.id`.

## Current standalone/stub mode

The project currently runs before the real Financial Document module is
integrated. Apply migrations in timestamp order:

1. `20260620091143_stub_financial_document_schema.sql`
   creates a development-only simulation matching
   `SCHEMADOKUMENKEUANGAN.md`.
2. `20260620091200_bpkpad_document_loan_schema.sql`
   creates only loan-module objects and references
   `archive_documents.id`.
3. `20260620113300_harden_loan_input_constraints.sql`
   aligns server-side input limits with the Android domain validation.
4. `20260620150416_harden_loan_extension_notes.sql`
   limits extension and rejection reasons to 500 characters. The base schema
   already enforces one pending extension per transaction and Kasubag approval.
5. `20260620235503_add_mobile_sync_contract.sql`
   adds an idempotent Android `client_reference`, agency field constraints,
   and automatic release of document locks for terminal transactions.
6. `seed.sql` optionally loads seven archive examples.
7. `seed_additional.sql` adds fourteen more archive examples and may be run
   after the initial seed without deleting existing records.

The stub uses minimal placeholder enum labels inferred from the supplied
contract: `SP2D`, `PAPER`, `GOOD`, `AVAILABLE`, and `MANUAL`.

For a clean development reset, run `reset_development.sql` in the SQL Editor.
It removes only the old/current loan module and temporary Financial Document
stub. It does not delete Auth users or directly modify Storage objects.

Supabase blocks direct deletion from `storage.objects`. Delete the
`loan-documents` bucket manually through **Storage → loan-documents → Empty
bucket → Delete bucket**. After resetting, rerun the stub migration, loan
migration, and optional seed in that order.

When the real module is available:

- do not deploy the stub migration to the shared environment;
- run `verify_financial_document_contract.sql` against the real schema;
- deploy only the loan-module migration;
- keep the Android/domain adapter responsible for mapping external enum labels
  into the loan module's Indonesian domain values.

The Android client uses a publishable key only. The key is read from
`local.properties` or the `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`
environment variables. `local.properties` is ignored by Git.

Authentication is Supabase-only when the app is configured. Passwords are
never stored in Room or DataStore. Only a local profile/session projection is
cached, and it is checked against the restored Supabase Auth session when the
app starts.

After one successful online login, the app may perform an offline unlock for
up to 72 hours. It stores only a salted password verifier encrypted by the
Android Keystore. Offline fallback is attempted only for network failures;
invalid credentials, disabled users, and other Auth errors never fall back.
Explicit logout removes the offline verifier.

Loan writes are local-first. New local transactions receive a unique
`sync_key`; Supabase stores it as `client_reference` so retries are
idempotent. Failed network writes remain `pending` in Room and are retried when
the dashboard opens. Built-in Room demo transactions are marked `local_only`
and are never uploaded.

## Apply the database schema

Preferred:

```powershell
npx supabase login
npx supabase link --project-ref qcnicufrljjqihdansly
npx supabase db push
```

If the CLI is unavailable, paste the stub migration first and then the loan
migration into the Supabase SQL Editor.

After all migrations, Auth users, roles, and seeds are configured, run
`verify_deployment.sql`. All expected tables must show `OK`, all `loan_*`
tables must have RLS enabled, the expected policies/triggers must be listed,
and the `loan-documents` bucket must be private.

## Load temporary dummy archive data

Only after the Financial Document owner approves dummy writes, paste `seed.sql`
into the SQL Editor. It uses only fields from the supplied shared contract,
discovers the existing enum labels, and inserts seven representative records.

After applying it:

1. Confirm `public` is exposed under **Integrations → Data API**.
2. Run the Supabase Security Advisor and resolve every warning before production.
3. Disable public user sign-ups in **Authentication → Providers → Email**.
4. Create Auth users from the Dashboard. Never assign roles from Android.
5. New users without trusted `app_metadata.app_role` are created inactive.
   Activate and assign them using the SQL Editor:

```sql
update public.loan_profiles
set
  role = 'arsiparis',
  is_active = true
where username = 'budi';
```

Use `role = 'kasubag'` for a supervisor. Authorization data belongs in
`app_metadata`, never user-editable `user_metadata`.
6. Use an email alias for the existing username login convention, for example
   `budi@bpkpad-balangan.go.id`; do not store passwords in Room for production.

## GitHub Actions

Store these as repository/environment secrets:

- `SUPABASE_ACCESS_TOKEN` for schema deployment only.
- `SUPABASE_DB_PASSWORD` if required by the deployment command.

The Android build needs only the publishable client configuration. It is not a
secret and will exist in the APK, but keeping it out of Git prevents accidental
copying and makes rotation/configuration cleaner. RLS and Auth protect the data.

Never place `sb_secret_...`, `service_role`, database passwords, or personal
access tokens in Android resources, `BuildConfig`, Gradle files, or source code.
