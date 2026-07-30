-- Amplía el catálogo de adaptaciones y publica eventos de ejemplo.
--
-- Sin eventos en la tabla `event`, GET /api/events devuelve una lista vacía y el
-- asistente de IA no tiene nada que recomendar. Las inserciones usan claves
-- fijas con INSERT IGNORE para poder ejecutarse tanto desde Flyway como desde
-- los scripts de init-mysql sin duplicar filas.

-- 1. Categorías de discapacidad que faltaban en el catálogo
INSERT IGNORE INTO disability (id, name, description, category) VALUES
(4, 'Discapacidad Intelectual', 'Limitación en funciones intelectuales y de aprendizaje', 'intelectual'),
(5, 'Discapacidad Múltiple', 'Combinación de dos o más discapacidades', 'multiple');

-- 2. Adaptaciones deporte-discapacidad adicionales
INSERT IGNORE INTO sport_disability (sport_id, disability_id, adaptations) VALUES
(1, 4, 'Reglas simplificadas, instrucciones cortas y apoyo visual con pictogramas'),
(2, 3, 'Señales visuales del entrenador, marcador luminoso y comunicación por gestos'),
(2, 5, 'Acompañante de apoyo individual, tiempos de juego reducidos y material adaptado'),
(3, 2, 'Entrada asistida con grúa o rampa, flotadores de apoyo y trabajo del tren superior'),
(3, 3, 'Señales visuales de salida y llegada, luces indicadoras en el borde de la piscina'),
(3, 4, 'Secuencia de pasos fija, demostración previa y acompañamiento dentro del agua');

-- 3. Eventos publicados. Las fechas son relativas al momento de la carga para
--    que siempre queden en el futuro y resulten recomendables.
INSERT IGNORE INTO event
    (id, sport_id, name, description, event_date, event_time, location, max_capacity, available_capacity, status)
VALUES
('a0000001-0000-4000-8000-000000000001', 1,
 'Torneo Inclusivo de Fútbol Sala',
 'Torneo por equipos mixtos con balón sonoro, guías táctiles y señalización luminosa para el arbitraje. Acceso sin barreras y baños adaptados.',
 DATE_ADD(CURDATE(), INTERVAL 21 DAY), '10:00:00', 'Polideportivo Municipal El Salitre', 20, 14, 'active'),

('a0000001-0000-4000-8000-000000000002', 1,
 'Clínica de Iniciación en Fútbol Sala Adaptado',
 'Sesión formativa para nuevos participantes. Instrucciones cortas con apoyo visual y acompañamiento individual durante toda la actividad.',
 DATE_ADD(CURDATE(), INTERVAL 35 DAY), '09:00:00', 'Coliseo Cubierto La Aurora', 16, 16, 'active'),

('a0000001-0000-4000-8000-000000000003', 1,
 'Liga Abierta de Fútbol Sala Inclusivo',
 'Competencia por jornadas durante seis semanas. Arbitraje con señales visuales y balón sonoro disponible en todos los partidos.',
 DATE_ADD(CURDATE(), INTERVAL 70 DAY), '15:30:00', 'Complejo Deportivo Norte', 24, 8, 'active'),

('a0000002-0000-4000-8000-000000000001', 2,
 'Copa Nacional de Baloncesto en Silla',
 'Competencia oficial con cancha adaptada y préstamo de sillas deportivas. Marcador luminoso y comunicación por gestos con el equipo arbitral.',
 DATE_ADD(CURDATE(), INTERVAL 28 DAY), '11:00:00', 'Coliseo El Campín', 24, 6, 'active'),

('a0000002-0000-4000-8000-000000000002', 2,
 'Entrenamiento Abierto de Baloncesto en Silla',
 'Sesión abierta de técnica y desplazamiento. Se dispone de sillas deportivas de préstamo y apoyo individual para quien lo necesite.',
 DATE_ADD(CURDATE(), INTERVAL 14 DAY), '17:00:00', 'Centro Deportivo Sur', 12, 12, 'active'),

('a0000002-0000-4000-8000-000000000003', 2,
 'Torneo Amistoso de Baloncesto en Silla',
 'Encuentro amistoso entre clubes con tiempos de juego reducidos y acompañamiento para participantes con discapacidad múltiple.',
 DATE_ADD(CURDATE(), INTERVAL 56 DAY), '10:30:00', 'Polideportivo Parque Simón Bolívar', 20, 20, 'active'),

('a0000003-0000-4000-8000-000000000001', 3,
 'Encuentro de Natación Adaptada',
 'Pruebas por tramos cortos con cuerdas guía en los carriles, aviso táctil en los bordes y luces indicadoras de salida y llegada.',
 DATE_ADD(CURDATE(), INTERVAL 18 DAY), '08:00:00', 'Piscina Olímpica Distrital', 15, 5, 'active'),

('a0000003-0000-4000-8000-000000000002', 3,
 'Taller de Técnica en Natación Adaptada',
 'Trabajo de respiración y propulsión con acompañamiento dentro del agua. Entrada asistida con grúa y flotadores de apoyo disponibles.',
 DATE_ADD(CURDATE(), INTERVAL 42 DAY), '16:00:00', 'Piscina Cubierta Zona Occidente', 10, 10, 'active'),

('a0000003-0000-4000-8000-000000000003', 3,
 'Festival Acuático Inclusivo',
 'Jornada recreativa abierta a todas las categorías de discapacidad, con estaciones adaptadas y personal de apoyo en cada carril.',
 DATE_ADD(CURDATE(), INTERVAL 90 DAY), '09:30:00', 'Centro Acuático Nacional', 30, 30, 'active');
