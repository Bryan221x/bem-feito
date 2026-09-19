-- =========================================================
-- BEM-FEITO
-- Banco de dados inicial
-- MySQL 8+
-- =========================================================

CREATE DATABASE IF NOT EXISTS bem_feito
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE bem_feito;

-- =========================================================
-- USERS
-- Contas de acesso ao sistema.
-- Uma conta não representa diretamente uma instituição.
-- =========================================================

CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,

    platform_role VARCHAR(30) NOT NULL DEFAULT 'USER',
    status VARCHAR(30) NOT NULL DEFAULT 'INVITED',

    email_verified_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email
        UNIQUE (email),

    CONSTRAINT chk_users_platform_role
        CHECK (platform_role IN ('USER', 'PLATFORM_ADMIN')),

    CONSTRAINT chk_users_status
        CHECK (status IN (
            'INVITED',
            'ACTIVE',
            'INACTIVE',
            'SUSPENDED'
        ))
) ENGINE = InnoDB;


-- =========================================================
-- INSTITUTION CATEGORIES
-- Classificação principal das instituições.
-- =========================================================

CREATE TABLE institution_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_institution_categories_name
        UNIQUE (name)
) ENGINE = InnoDB;


-- =========================================================
-- STATES
-- Estados brasileiros.
-- =========================================================

CREATE TABLE states (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,

    CONSTRAINT uq_states_name
        UNIQUE (name),

    CONSTRAINT uq_states_uf
        UNIQUE (uf)
) ENGINE = InnoDB;


-- =========================================================
-- CITIES
-- Municípios vinculados aos estados.
-- =========================================================

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


-- =========================================================
-- INSTITUTIONS
-- Instituições cadastradas e aprovadas.
-- =========================================================

CREATE TABLE institutions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    category_id BIGINT UNSIGNED NOT NULL,

    display_name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    cnpj CHAR(14) NOT NULL,

    objective TEXT NULL,
    description TEXT NULL,
    history TEXT NULL,

    website VARCHAR(255) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_institutions_cnpj
        UNIQUE (cnpj),

    CONSTRAINT chk_institutions_cnpj_format
        CHECK (cnpj REGEXP '^[0-9]{14}$'),

    CONSTRAINT chk_institutions_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE',
            'SUSPENDED'
        )),

    CONSTRAINT fk_institutions_category
        FOREIGN KEY (category_id)
        REFERENCES institution_categories(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- INSTITUTION MEMBERSHIPS
-- Relação N:N entre contas e instituições.
-- =========================================================

CREATE TABLE institution_memberships (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL,
    institution_id BIGINT UNSIGNED NOT NULL,

    role VARCHAR(30) NOT NULL DEFAULT 'EDITOR',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_membership_user_institution
        UNIQUE (user_id, institution_id),

    CONSTRAINT chk_membership_role
        CHECK (role IN (
            'OWNER',
            'MANAGER',
            'EDITOR'
        )),

    CONSTRAINT chk_membership_status
        CHECK (status IN (
            'ACTIVE',
            'PENDING',
            'REVOKED'
        )),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_membership_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- UNITS
-- Unidades físicas das instituições.
-- =========================================================

CREATE TABLE units (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    institution_id BIGINT UNSIGNED NOT NULL,
    city_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(150) NOT NULL,
    document CHAR(14) NULL,
    description TEXT NULL,

    postal_code CHAR(8) NOT NULL,
    street VARCHAR(180) NOT NULL,
    number VARCHAR(30) NOT NULL,
    complement VARCHAR(120) NULL,
    neighborhood VARCHAR(120) NOT NULL,

    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_units_document_format
        CHECK (
            document IS NULL
            OR document REGEXP '^[0-9]{14}$'
        ),

    CONSTRAINT chk_units_postal_code_format
        CHECK (
            postal_code REGEXP '^[0-9]{8}$'
        ),

    CONSTRAINT chk_units_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT chk_units_latitude
        CHECK (
            latitude IS NULL
            OR latitude BETWEEN -90 AND 90
        ),

    CONSTRAINT chk_units_longitude
        CHECK (
            longitude IS NULL
            OR longitude BETWEEN -180 AND 180
        ),

    CONSTRAINT chk_units_coordinates_pair
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR
            (latitude IS NOT NULL AND longitude IS NOT NULL)
        ),

    CONSTRAINT fk_units_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_units_city
        FOREIGN KEY (city_id)
        REFERENCES cities(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- INSTITUTION CONTACTS
-- Contatos gerais da instituição.
-- =========================================================

CREATE TABLE institution_contacts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    institution_id BIGINT UNSIGNED NOT NULL,

    type VARCHAR(30) NOT NULL,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(100) NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,

    display_order INT UNSIGNED NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_institution_contacts_value
        UNIQUE (institution_id, type, value),

    CONSTRAINT chk_institution_contacts_type
        CHECK (type IN (
            'EMAIL',
            'PHONE',
            'WHATSAPP'
        )),

    CONSTRAINT fk_institution_contacts_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- UNIT CONTACTS
-- Contatos específicos das unidades.
-- =========================================================

CREATE TABLE unit_contacts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    unit_id BIGINT UNSIGNED NOT NULL,

    type VARCHAR(30) NOT NULL,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(100) NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,

    display_order INT UNSIGNED NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_unit_contacts_value
        UNIQUE (unit_id, type, value),

    CONSTRAINT chk_unit_contacts_type
        CHECK (type IN (
            'EMAIL',
            'PHONE',
            'WHATSAPP'
        )),

    CONSTRAINT fk_unit_contacts_unit
        FOREIGN KEY (unit_id)
        REFERENCES units(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- UNIT OPENING HOURS
-- Horários de funcionamento de cada unidade.
-- =========================================================

CREATE TABLE unit_opening_hours (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    unit_id BIGINT UNSIGNED NOT NULL,

    day_of_week TINYINT UNSIGNED NOT NULL,

    opens_at TIME NULL,
    closes_at TIME NULL,

    closed BOOLEAN NOT NULL DEFAULT FALSE,

    note VARCHAR(150) NULL,

    CONSTRAINT uq_unit_opening_hours_day
        UNIQUE (unit_id, day_of_week),

    CONSTRAINT chk_opening_hours_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT chk_opening_hours_values
        CHECK (
            (
                closed = TRUE
                AND opens_at IS NULL
                AND closes_at IS NULL
            )
            OR
            (
                closed = FALSE
                AND opens_at IS NOT NULL
                AND closes_at IS NOT NULL
                AND opens_at < closes_at
            )
        ),

    CONSTRAINT fk_opening_hours_unit
        FOREIGN KEY (unit_id)
        REFERENCES units(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- SERVICE AREAS
-- Áreas de atuação.
-- =========================================================

CREATE TABLE service_areas (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_service_areas_name
        UNIQUE (name)
) ENGINE = InnoDB;


-- =========================================================
-- UNIT SERVICE AREAS
-- Relação N:N entre unidades e áreas de atuação.
-- =========================================================

CREATE TABLE unit_service_areas (
    unit_id BIGINT UNSIGNED NOT NULL,
    service_area_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (unit_id, service_area_id),

    CONSTRAINT fk_unit_service_area_unit
        FOREIGN KEY (unit_id)
        REFERENCES units(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_unit_service_area_service_area
        FOREIGN KEY (service_area_id)
        REFERENCES service_areas(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- NEED CATEGORIES
-- Categorias de necessidades.
-- =========================================================

CREATE TABLE need_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_need_categories_name
        UNIQUE (name)
) ENGINE = InnoDB;


-- =========================================================
-- NEEDS
-- Necessidades reais das unidades.
-- =========================================================

CREATE TABLE needs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    unit_id BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,

    title VARCHAR(150) NOT NULL,
    description TEXT NULL,

    quantity DECIMAL(10, 2) NULL,
    unit_label VARCHAR(50) NULL,

    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_needs_quantity
        CHECK (quantity IS NULL OR quantity >= 0),

    CONSTRAINT chk_needs_priority
        CHECK (priority IN (
            'LOW',
            'MEDIUM',
            'HIGH'
        )),

    CONSTRAINT chk_needs_status
        CHECK (status IN (
            'ACTIVE',
            'FULFILLED',
            'INACTIVE'
        )),

    CONSTRAINT fk_needs_unit
        FOREIGN KEY (unit_id)
        REFERENCES units(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_needs_category
        FOREIGN KEY (category_id)
        REFERENCES need_categories(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- INSTITUTION HISTORY IMAGES
-- Fotografias usadas na experiência "Nossa história".
-- =========================================================

CREATE TABLE institution_history_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    institution_id BIGINT UNSIGNED NOT NULL,

    image_url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255) NOT NULL,
    caption VARCHAR(255) NULL,

    display_order INT UNSIGNED NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_history_images_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- REGISTRATION REQUESTS
-- Solicitações de cadastro de novas instituições.
-- =========================================================

CREATE TABLE registration_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    category_id BIGINT UNSIGNED NOT NULL,

    display_name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200) NOT NULL,
    cnpj CHAR(14) NOT NULL,

    objective TEXT NULL,
    description TEXT NULL,
    history TEXT NULL,

    website VARCHAR(255) NULL,

    responsible_name VARCHAR(150) NOT NULL,
    responsible_email VARCHAR(255) NOT NULL,
    responsible_phone VARCHAR(30) NOT NULL,
    responsible_role VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    reviewed_by_user_id BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    review_notes TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_registration_requests_cnpj_format
        CHECK (cnpj REGEXP '^[0-9]{14}$'),

    CONSTRAINT chk_registration_requests_status
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'NEEDS_CHANGES',
            'CANCELLED'
        )),

    CONSTRAINT fk_registration_requests_category
        FOREIGN KEY (category_id)
        REFERENCES institution_categories(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_registration_requests_reviewer
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- REGISTRATION REQUEST CONTACTS
-- Contatos gerais apresentados durante a solicitação.
-- =========================================================

CREATE TABLE registration_request_contacts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    registration_request_id BIGINT UNSIGNED NOT NULL,

    type VARCHAR(30) NOT NULL,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(100) NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_registration_request_contacts_value
        UNIQUE (registration_request_id, type, value),

    CONSTRAINT chk_registration_request_contacts_type
        CHECK (type IN (
            'EMAIL',
            'PHONE',
            'WHATSAPP'
        )),

    CONSTRAINT fk_registration_request_contacts_request
        FOREIGN KEY (registration_request_id)
        REFERENCES registration_requests(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- REGISTRATION REQUEST UNITS
-- Unidade informada durante o cadastro inicial.
-- =========================================================

CREATE TABLE registration_request_units (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    registration_request_id BIGINT UNSIGNED NOT NULL,
    city_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(150) NOT NULL,

    document CHAR(14) NULL,
    description TEXT NULL,

    postal_code CHAR(8) NOT NULL,
    street VARCHAR(180) NOT NULL,
    number VARCHAR(30) NOT NULL,
    complement VARCHAR(120) NULL,
    neighborhood VARCHAR(120) NOT NULL,

    CONSTRAINT chk_registration_request_units_document_format
        CHECK (
            document IS NULL
            OR document REGEXP '^[0-9]{14}$'
        ),

    CONSTRAINT chk_registration_request_units_postal_code_format
        CHECK (postal_code REGEXP '^[0-9]{8}$'),

    CONSTRAINT fk_registration_request_units_request
        FOREIGN KEY (registration_request_id)
        REFERENCES registration_requests(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_registration_request_units_city
        FOREIGN KEY (city_id)
        REFERENCES cities(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- REGISTRATION REQUEST UNIT CONTACTS
-- Contatos da unidade informada na solicitação.
-- =========================================================

CREATE TABLE registration_request_unit_contacts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    registration_request_unit_id BIGINT UNSIGNED NOT NULL,

    type VARCHAR(30) NOT NULL,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(100) NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_registration_request_unit_contacts_value
        UNIQUE (registration_request_unit_id, type, value),

    CONSTRAINT chk_registration_request_unit_contacts_type
        CHECK (type IN (
            'EMAIL',
            'PHONE',
            'WHATSAPP'
        )),

    CONSTRAINT fk_registration_request_unit_contacts_unit
        FOREIGN KEY (registration_request_unit_id)
        REFERENCES registration_request_units(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- ACCESS REQUESTS
-- Solicitações para administrar uma instituição existente.
-- =========================================================

CREATE TABLE access_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    institution_id BIGINT UNSIGNED NOT NULL,
    requester_user_id BIGINT UNSIGNED NULL,

    requester_name VARCHAR(150) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    requester_phone VARCHAR(30) NOT NULL,
    requester_role VARCHAR(100) NOT NULL,

    justification TEXT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    reviewed_by_user_id BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    review_notes TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_access_requests_status
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'NEEDS_CHANGES',
            'CANCELLED'
        )),

    CONSTRAINT fk_access_requests_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_access_requests_requester_user
        FOREIGN KEY (requester_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_access_requests_reviewer
        FOREIGN KEY (reviewed_by_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- ACCOUNT INVITATION TOKENS
-- Convites para criação/ativação de conta.
-- O token bruto nunca deve ser armazenado.
-- =========================================================

CREATE TABLE account_invitation_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_account_invitation_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_account_invitation_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- ÍNDICES
-- Índices voltados às consultas mais comuns da aplicação.
-- =========================================================

CREATE INDEX idx_cities_state
    ON cities(state_id);

CREATE INDEX idx_institutions_category
    ON institutions(category_id);

CREATE INDEX idx_institutions_status
    ON institutions(status);

CREATE INDEX idx_institutions_display_name
    ON institutions(display_name);

CREATE INDEX idx_units_institution
    ON units(institution_id);

CREATE INDEX idx_units_city
    ON units(city_id);

CREATE INDEX idx_units_status
    ON units(status);

CREATE INDEX idx_institution_memberships_user
    ON institution_memberships(user_id);

CREATE INDEX idx_institution_memberships_institution
    ON institution_memberships(institution_id);

CREATE INDEX idx_institution_contacts_institution
    ON institution_contacts(institution_id);

CREATE INDEX idx_unit_contacts_unit
    ON unit_contacts(unit_id);

CREATE INDEX idx_unit_service_areas_service_area
    ON unit_service_areas(service_area_id);

CREATE INDEX idx_needs_unit
    ON needs(unit_id);

CREATE INDEX idx_needs_category
    ON needs(category_id);

CREATE INDEX idx_needs_status
    ON needs(status);

CREATE INDEX idx_history_images_institution
    ON institution_history_images(institution_id);

CREATE INDEX idx_registration_requests_status
    ON registration_requests(status);

CREATE INDEX idx_registration_requests_cnpj
    ON registration_requests(cnpj);

CREATE INDEX idx_access_requests_institution
    ON access_requests(institution_id);

CREATE INDEX idx_access_requests_status
    ON access_requests(status);
