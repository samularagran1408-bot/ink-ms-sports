-- Marca el momento de cancelación para retirar el evento 2 horas después.
ALTER TABLE event
    ADD COLUMN cancelled_at DATETIME NULL;

UPDATE event
SET cancelled_at = NOW()
WHERE status = 'cancelled' AND cancelled_at IS NULL;
