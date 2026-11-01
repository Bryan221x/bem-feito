-- Migração aditiva. Preparada para aplicação posterior pelo operador; não executada no banco operacional.
ALTER TABLE users ADD COLUMN credentials_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE password_reset_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX ix_password_reset_user_created (user_id, created_at)
) ENGINE=InnoDB;
