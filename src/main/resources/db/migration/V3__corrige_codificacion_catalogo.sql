-- Repara el texto del catálogo que quedó doblemente codificado.
--
-- Los scripts de init-mysql se cargaban sin declarar `SET NAMES utf8mb4`, así
-- que el cliente los interpretaba como latin1 y "Fútbol" terminaba almacenado
-- como "FÃºtbol". La conversión latin1 -> binario -> utf8mb4 deshace ese doble
-- paso. Solo se tocan las filas que presentan el patrón de la corrupción, para
-- no dañar el texto ya correcto.

UPDATE sport
SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
WHERE name COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE sport
SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
WHERE description COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE sport
SET required_materials = CONVERT(BINARY(CONVERT(required_materials USING latin1)) USING utf8mb4)
WHERE required_materials COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE disability
SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
WHERE name COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE disability
SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
WHERE description COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE sport_disability
SET adaptations = CONVERT(BINARY(CONVERT(adaptations USING latin1)) USING utf8mb4)
WHERE adaptations COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE event
SET name = CONVERT(BINARY(CONVERT(name USING latin1)) USING utf8mb4)
WHERE name COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE event
SET description = CONVERT(BINARY(CONVERT(description USING latin1)) USING utf8mb4)
WHERE description COLLATE utf8mb4_bin REGEXP '[ÃÂ]';

UPDATE event
SET location = CONVERT(BINARY(CONVERT(location USING latin1)) USING utf8mb4)
WHERE location COLLATE utf8mb4_bin REGEXP '[ÃÂ]';
