-- Parte 4. Executar uma vez no banco existente; não altera usuários.
-- Falha com duplicatas preexistentes: revisar esses pedidos antes de aplicar.
ALTER TABLE registration_requests
    ADD COLUMN open_cnpj CHAR(14) GENERATED ALWAYS AS
        (CASE WHEN status IN ('PENDING','APPROVED') THEN cnpj ELSE NULL END) STORED,
    ADD CONSTRAINT uq_registration_requests_open_cnpj UNIQUE (open_cnpj);
ALTER TABLE access_requests
    ADD COLUMN pending_email VARCHAR(255) GENERATED ALWAYS AS
        (CASE WHEN status = 'PENDING' THEN requester_email ELSE NULL END) STORED,
    ADD CONSTRAINT uq_access_requests_pending UNIQUE (institution_id,pending_email);
