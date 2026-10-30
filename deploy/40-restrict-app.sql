-- Executed only on a NEW Compose volume, after the schema/reference imports.
-- Migrations and backups use a separate operator account, never this runtime user.
REVOKE ALL PRIVILEGES ON bem_feito.* FROM 'bemfeito'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON bem_feito.* TO 'bemfeito'@'%';
