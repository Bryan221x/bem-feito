-- Dados territoriais reais e taxonomias do projeto; somente no banco em memória.
INSERT INTO states (name, uf) VALUES ('São Paulo', 'SP'), ('Minas Gerais', 'MG'), ('Acre', 'AC');
INSERT INTO cities (state_id, name) SELECT id, 'Luz' FROM states WHERE uf = 'MG';
INSERT INTO cities (state_id, name) SELECT id, 'Belo Horizonte' FROM states WHERE uf = 'MG';
INSERT INTO cities (state_id, name) SELECT id, 'São Paulo' FROM states WHERE uf = 'SP';
INSERT INTO institution_categories (name, active) VALUES ('Saúde', TRUE), ('Educação', TRUE), ('Outros', FALSE);
INSERT INTO service_areas (name, active) VALUES ('Saúde', TRUE), ('Educação', TRUE), ('Outros', FALSE);
INSERT INTO need_categories (name, active) VALUES ('Roupas e Calçados', TRUE), ('Alimentos', TRUE), ('Outros', FALSE);
