-- src/main/resources/db/migration/V1__init_sports_db.sql

CREATE DATABASE IF NOT EXISTS sports_events_ms;
USE sports_events_ms;

-- 1. Tabla: deporte
CREATE TABLE sport (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    difficulty ENUM('bajo', 'medio', 'alto') DEFAULT 'medio',
    required_materials TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla: discapacidad
CREATE TABLE disability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE
);

-- 3. Tabla: relación deporte-discapacidad
CREATE TABLE sport_disability (
    sport_id INT,
    disability_id INT,
    adaptations TEXT NOT NULL,
    PRIMARY KEY (sport_id, disability_id),
    FOREIGN KEY (sport_id) REFERENCES sport(id) ON DELETE CASCADE,
    FOREIGN KEY (disability_id) REFERENCES disability(id) ON DELETE CASCADE
);

-- 4. Tabla: evento
CREATE TABLE event (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    sport_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    location VARCHAR(255),
    max_capacity INT NOT NULL,
    available_capacity INT NOT NULL,
    status ENUM('draft', 'active', 'cancelled', 'finished') DEFAULT 'draft',
    created_by CHAR(36),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sport_id) REFERENCES sport(id),
    INDEX idx_event_date (event_date),
    INDEX idx_event_status (status)
);

-- 5. Tabla: inscripciones
CREATE TABLE event_registration (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    event_id CHAR(36) NOT NULL,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    attended BOOLEAN DEFAULT FALSE,
    waitlist_position INT,
    qr_code TEXT,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    INDEX idx_user_event (user_id, event_id)
);

-- 6. Tabla: asistencia
CREATE TABLE event_attendance (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    registration_id CHAR(36) NOT NULL,
    check_in_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    check_in_method ENUM('qr', 'manual', 'admin') DEFAULT 'qr',
    verified_by CHAR(36),
    FOREIGN KEY (registration_id) REFERENCES event_registration(id)
);

-- 7. Tabla: lista de espera
CREATE TABLE waitlist (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    event_id CHAR(36) NOT NULL,
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    notified_at DATETIME,
    notified BOOLEAN DEFAULT FALSE,
    position INT,
    status ENUM('waiting', 'offered', 'accepted', 'expired') DEFAULT 'waiting',
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    INDEX idx_event_position (event_id, position)
);

-- 8. Vista: calendario de eventos
CREATE VIEW event_calendar AS
SELECT 
    e.id AS event_id,
    e.name AS event_name,
    e.event_date,
    e.event_time,
    e.location,
    s.name AS sport_name,
    e.available_capacity,
    e.max_capacity
FROM event e
JOIN sport s ON e.sport_id = s.id
WHERE e.status = 'active'
ORDER BY e.event_date, e.event_time;

-- 9. Datos iniciales
INSERT INTO sport (name, description, difficulty) VALUES 
('Fútbol Sala', 'Deporte colectivo adaptado para espacios reducidos', 'medio'),
('Baloncesto en Silla', 'Baloncesto adaptado para silla de ruedas', 'alto'),
('Natación Adaptada', 'Natación con adaptaciones según discapacidad', 'medio');

INSERT INTO disability (name, description, category) VALUES 
('Discapacidad Visual', 'Pérdida parcial o total de visión', 'visual'),
('Discapacidad Física', 'Limitación en movilidad de brazos, piernas o tronco', 'fisica'),
('Discapacidad Auditiva', 'Pérdida parcial o total de audición', 'auditiva');

INSERT INTO sport_disability (sport_id, disability_id, adaptations) VALUES 
(1, 1, 'Balón sonoro, guías táctiles, comunicación verbal constante'),
(1, 3, 'Señales visuales del árbitro, sistema de luces'),
(2, 2, 'Silla de ruedas deportiva, cancha adaptada'),
(3, 1, 'Guías táctiles en bordes, cuerdas guía en carriles');