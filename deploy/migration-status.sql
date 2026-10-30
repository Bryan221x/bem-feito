-- READ ONLY. Inspect definitions as well as presence before deciding to migrate.
SELECT table_name, column_name, generation_expression, extra
FROM information_schema.columns
WHERE table_schema = 'bem_feito' AND
 ((table_name = 'registration_requests' AND column_name = 'open_cnpj') OR
  (table_name = 'access_requests' AND column_name = 'pending_email'));
SELECT table_name, index_name, non_unique, seq_in_index, column_name
FROM information_schema.statistics
WHERE table_schema = 'bem_feito' AND index_name IN
 ('uq_registration_requests_open_cnpj', 'uq_access_requests_pending')
ORDER BY table_name, index_name, seq_in_index;
SELECT cnpj, COUNT(*) FROM bem_feito.registration_requests
WHERE status IN ('PENDING','APPROVED') GROUP BY cnpj HAVING COUNT(*) > 1;
SELECT institution_id, requester_email, COUNT(*) FROM bem_feito.access_requests
WHERE status = 'PENDING' GROUP BY institution_id, requester_email HAVING COUNT(*) > 1;
