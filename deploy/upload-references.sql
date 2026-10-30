-- Export while ALL API writers are stopped. Never edit the resulting snapshot.
SELECT JSON_OBJECT('institutionId', institution_id, 'url', image_url)
FROM bem_feito.institution_history_images ORDER BY id;
