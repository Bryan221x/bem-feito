
-- Executado apenas na inicialização de um volume novo.
-- Migrações e backups utilizam uma conta operacional separada.

REVOKE ALL PRIVILEGES, GRANT OPTION
FROM 'bemfeito'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE
ON `bem_feito`.*
TO 'bemfeito'@'%';
