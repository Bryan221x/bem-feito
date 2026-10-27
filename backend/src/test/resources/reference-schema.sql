-- DDL das cinco tabelas de referência, extraído de database/schema.sql.
CREATE TABLE states (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,

    CONSTRAINT uq_states_name
        UNIQUE (name),

    CONSTRAINT uq_states_uf
        UNIQUE (uf)
) ENGINE = InnoDB;

CREATE TABLE cities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    state_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(150) NOT NULL,

    CONSTRAINT uq_cities_state_name
        UNIQUE (state_id, name),

    CONSTRAINT fk_cities_state
        FOREIGN KEY (state_id)
        REFERENCES states(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE institution_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_institution_categories_name
        UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE service_areas (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_service_areas_name
        UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE need_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_need_categories_name
        UNIQUE (name)
) ENGINE = InnoDB;