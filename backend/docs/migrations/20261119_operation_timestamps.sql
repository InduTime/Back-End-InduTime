-- Execute this migration once before deploying the offset-aware OperationEntity.
-- Existing LocalDateTime values are interpreted as America/Sao_Paulo wall time.
-- Verify that assumption against the production database before running it.
BEGIN;

DO $$
DECLARE
    start_type text;
    end_type text;
BEGIN
    SELECT data_type INTO start_type
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'operacoes'
      AND column_name = 'data_inicio';

    IF start_type = 'timestamp without time zone' THEN
        ALTER TABLE operacoes
            ALTER COLUMN data_inicio TYPE timestamp with time zone
            USING data_inicio AT TIME ZONE 'America/Sao_Paulo';
    END IF;

    SELECT data_type INTO end_type
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'operacoes'
      AND column_name = 'data_fim';

    IF end_type = 'timestamp without time zone' THEN
        ALTER TABLE operacoes
            ALTER COLUMN data_fim TYPE timestamp with time zone
            USING data_fim AT TIME ZONE 'America/Sao_Paulo';
    END IF;
END $$;

ALTER TABLE operacoes
    ADD COLUMN IF NOT EXISTS data_fim_prevista timestamp with time zone;
ALTER TABLE operacoes
    ADD COLUMN IF NOT EXISTS data_inicio_offset_segundos integer;
ALTER TABLE operacoes
    ADD COLUMN IF NOT EXISTS data_fim_offset_segundos integer;
ALTER TABLE operacoes
    ADD COLUMN IF NOT EXISTS data_fim_prevista_offset_segundos integer;

-- PostgreSQL stores the instant in TIMESTAMPTZ but not its original offset.
-- Keep the received offset separately so API responses preserve the sender's local date.
UPDATE operacoes
SET data_inicio_offset_segundos = EXTRACT(EPOCH FROM (
        (data_inicio AT TIME ZONE 'America/Sao_Paulo')
        - (data_inicio AT TIME ZONE 'UTC')
    ))::integer
WHERE data_inicio_offset_segundos IS NULL;

UPDATE operacoes
SET data_fim_offset_segundos = EXTRACT(EPOCH FROM (
        (data_fim AT TIME ZONE 'America/Sao_Paulo')
        - (data_fim AT TIME ZONE 'UTC')
    ))::integer
WHERE data_fim IS NOT NULL
  AND data_fim_offset_segundos IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT id_torno
        FROM operacoes
        WHERE status = 'EM_ANDAMENTO'
        GROUP BY id_torno
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existem máquinas com mais de uma operação EM_ANDAMENTO; corrija os registros antes da migração.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_operacoes_maquina_em_andamento
    ON operacoes (id_torno)
    WHERE status = 'EM_ANDAMENTO';

COMMIT;
