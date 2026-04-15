DO $$
DECLARE
    sequence_record RECORD;
    max_id BIGINT;
    sequence_name TEXT;
BEGIN
    FOR sequence_record IN
        SELECT
            c.table_schema,
            c.table_name,
            c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'retail_management_service'
          AND (c.is_identity = 'YES' OR c.column_default LIKE 'nextval(%')
    LOOP
        sequence_name := pg_get_serial_sequence(
            format('%I.%I', sequence_record.table_schema, sequence_record.table_name),
            sequence_record.column_name
        );

        IF sequence_name IS NULL THEN
            CONTINUE;
        END IF;

        EXECUTE format(
            'SELECT COALESCE(MAX(%I), 0) FROM %I.%I',
            sequence_record.column_name,
            sequence_record.table_schema,
            sequence_record.table_name
        ) INTO max_id;

        IF max_id > 0 THEN
            EXECUTE format('SELECT setval(%L, %s, true)', sequence_name, max_id);
        ELSE
            EXECUTE format('SELECT setval(%L, 1, false)', sequence_name);
        END IF;
    END LOOP;
END
$$;
