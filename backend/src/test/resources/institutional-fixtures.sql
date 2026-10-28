-- Dados operacionais controlados EXCLUSIVAMENTE no H2 de testes, com rollback.
INSERT INTO institutions (category_id,display_name,legal_name,cnpj,objective,description,history,website,status)
SELECT id,'Alpha Teste','Razão Alpha Teste','11222333000181','Objetivo preservado','Descrição preservada','História sem fotos','https://example.org','ACTIVE'
FROM institution_categories WHERE name='Educação';
INSERT INTO institutions (category_id,display_name,legal_name,cnpj,status)
SELECT id,'Beta Teste','Razão Beta Teste','11444777000161','INACTIVE' FROM institution_categories WHERE name='Educação';
INSERT INTO institutions (category_id,display_name,legal_name,cnpj,status)
SELECT id,'Gamma Teste','Razão Gamma Teste','19131243000197','ACTIVE' FROM institution_categories WHERE name='Educação';
INSERT INTO units (institution_id,city_id,name,document,postal_code,street,number,neighborhood,status)
SELECT i.id,c.id,'Unidade Luz',NULL,'35595000','Rua de Teste','10','Centro','ACTIVE' FROM institutions i CROSS JOIN cities c WHERE i.display_name='Alpha Teste' AND c.name='Luz';
INSERT INTO units (institution_id,city_id,name,postal_code,street,number,neighborhood,status)
SELECT i.id,c.id,'Unidade São Paulo','01001000','Rua de Teste','20','Centro','ACTIVE' FROM institutions i CROSS JOIN cities c WHERE i.display_name='Alpha Teste' AND c.name='São Paulo';
INSERT INTO units (institution_id,city_id,name,postal_code,street,number,neighborhood,status)
SELECT i.id,c.id,'Unidade Oculta','35595000','Rua de Teste','30','Centro','ACTIVE' FROM institutions i CROSS JOIN cities c WHERE i.display_name='Beta Teste' AND c.name='Luz';
INSERT INTO units (institution_id,city_id,name,postal_code,street,number,neighborhood,status)
SELECT i.id,c.id,'Unidade Inativa','35595000','Rua de Teste','40','Centro','INACTIVE' FROM institutions i CROSS JOIN cities c WHERE i.display_name='Gamma Teste' AND c.name='Belo Horizonte';
INSERT INTO institution_contacts (institution_id,type,value,is_primary,is_public,display_order)
SELECT id,'EMAIL','public@example.org',TRUE,TRUE,2 FROM institutions WHERE display_name='Alpha Teste';
INSERT INTO institution_contacts (institution_id,type,value,is_primary,is_public,display_order)
SELECT id,'PHONE','3133334444',FALSE,TRUE,1 FROM institutions WHERE display_name='Alpha Teste';
INSERT INTO institution_contacts (institution_id,type,value,is_primary,is_public,display_order)
SELECT id,'EMAIL','private@example.org',FALSE,FALSE,0 FROM institutions WHERE display_name='Alpha Teste';
INSERT INTO unit_contacts (unit_id,type,value,is_primary,is_public,display_order)
SELECT id,'EMAIL','unit-public@example.org',TRUE,TRUE,0 FROM units WHERE name='Unidade Luz';
INSERT INTO unit_contacts (unit_id,type,value,is_primary,is_public,display_order)
SELECT id,'EMAIL','unit-private@example.org',FALSE,FALSE,1 FROM units WHERE name='Unidade Luz';
INSERT INTO needs (unit_id,category_id,title,priority,status)
SELECT u.id,c.id,'Alimentos prioridade alta','HIGH','ACTIVE' FROM units u CROSS JOIN need_categories c WHERE u.name='Unidade Luz' AND c.name='Alimentos';
INSERT INTO needs (unit_id,category_id,title,priority,status)
SELECT u.id,c.id,'Alimentos prioridade baixa','LOW','ACTIVE' FROM units u CROSS JOIN need_categories c WHERE u.name='Unidade Luz' AND c.name='Alimentos';
INSERT INTO needs (unit_id,category_id,title,priority,status)
SELECT u.id,c.id,'Necessidade atendida','MEDIUM','FULFILLED' FROM units u CROSS JOIN need_categories c WHERE u.name='Unidade Luz' AND c.name='Alimentos';
INSERT INTO needs (unit_id,category_id,title,priority,status)
SELECT u.id,c.id,'Necessidade inativa','MEDIUM','INACTIVE' FROM units u CROSS JOIN need_categories c WHERE u.name='Unidade Luz' AND c.name='Alimentos';
