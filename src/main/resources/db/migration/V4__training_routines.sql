CREATE TABLE IF NOT EXISTS training_routine (
    id CHAR(36) PRIMARY KEY,
    trainer_id CHAR(36) NOT NULL,
    sport_id INT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    disability_focus VARCHAR(100),
    level VARCHAR(20) DEFAULT 'principiante',
    duration_minutes INT DEFAULT 35,
    exercises_json TEXT,
    status VARCHAR(20) DEFAULT 'draft',
    max_capacity INT NOT NULL DEFAULT 20,
    available_capacity INT NOT NULL DEFAULT 20,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_routine_trainer (trainer_id),
    INDEX idx_routine_status (status)
);

CREATE TABLE IF NOT EXISTS routine_registration (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    routine_id CHAR(36) NOT NULL,
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'active',
    UNIQUE KEY uq_user_routine (user_id, routine_id),
    INDEX idx_routine_reg_user (user_id),
    INDEX idx_routine_reg_routine (routine_id)
);
