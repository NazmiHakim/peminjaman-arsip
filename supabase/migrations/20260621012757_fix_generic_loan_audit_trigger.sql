-- The audit trigger is shared by several tables. Accessing
-- NEW.loan_transaction_id directly fails on tables that do not define that
-- column (for example loan_borrower_agencies). Read the optional field from
-- JSON instead so the trigger remains valid for every audited table.
create or replace function app_private.audit_loan_row_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    row_id uuid;
    transaction_id uuid;
    old_row jsonb;
    new_row jsonb;
begin
    old_row := case
        when tg_op in ('UPDATE', 'DELETE') then to_jsonb(old)
        else null
    end;
    new_row := case
        when tg_op in ('INSERT', 'UPDATE') then to_jsonb(new)
        else null
    end;

    row_id := coalesce(
        nullif(new_row ->> 'id', '')::uuid,
        nullif(old_row ->> 'id', '')::uuid
    );

    transaction_id := case
        when tg_table_name = 'loan_transactions' then row_id
        when tg_table_name in (
            'loan_items',
            'loan_document_locks',
            'loan_extensions'
        ) then coalesce(
            nullif(new_row ->> 'loan_transaction_id', '')::uuid,
            nullif(old_row ->> 'loan_transaction_id', '')::uuid
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
        old_row,
        new_row
    );

    if tg_op = 'DELETE' then
        return old;
    end if;
    return new;
end;
$$;
