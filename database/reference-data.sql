-- Bem-Feito — dados de referência, MySQL 8+.
-- Executar após database/schema.sql, com o arquivo em UTF-8.
-- Somente dados territoriais reais e taxonomias controladas pelo Bem-Feito.
-- Não contém mocks nem dados operacionais ou de demonstração.
-- As constraints UNIQUE impedem duplicatas. Em caso de conflito, a
-- atribuição do próprio id preserva todos os campos do registro existente.
-- Novas entradas das taxonomias iniciam com active = TRUE; entradas
-- existentes preservam seu estado de ativação e suas descrições.

SET NAMES utf8mb4;
USE bem_feito;

START TRANSACTION;

-- Estados brasileiros: 26 estados e Distrito Federal.
-- UNIQUE (name) e UNIQUE (uf), conforme o schema existente.
INSERT INTO states (name, uf) VALUES
    ('Acre', 'AC'),
    ('Alagoas', 'AL'),
    ('Amapá', 'AP'),
    ('Amazonas', 'AM'),
    ('Bahia', 'BA'),
    ('Ceará', 'CE'),
    ('Distrito Federal', 'DF'),
    ('Espírito Santo', 'ES'),
    ('Goiás', 'GO'),
    ('Maranhão', 'MA'),
    ('Mato Grosso', 'MT'),
    ('Mato Grosso do Sul', 'MS'),
    ('Minas Gerais', 'MG'),
    ('Pará', 'PA'),
    ('Paraíba', 'PB'),
    ('Paraná', 'PR'),
    ('Pernambuco', 'PE'),
    ('Piauí', 'PI'),
    ('Rio de Janeiro', 'RJ'),
    ('Rio Grande do Norte', 'RN'),
    ('Rio Grande do Sul', 'RS'),
    ('Rondônia', 'RO'),
    ('Roraima', 'RR'),
    ('Santa Catarina', 'SC'),
    ('São Paulo', 'SP'),
    ('Sergipe', 'SE'),
    ('Tocantins', 'TO')
ON DUPLICATE KEY UPDATE id = states.id;

-- Classificação principal das instituições. UNIQUE (name).
INSERT INTO institution_categories (name, active) VALUES
    ('Assistência Social', TRUE),
    ('Proteção Animal', TRUE),
    ('Educação', TRUE),
    ('Saúde', TRUE),
    ('Acolhimento', TRUE),
    ('Meio Ambiente', TRUE),
    ('Outros', TRUE)
ON DUPLICATE KEY UPDATE id = institution_categories.id;

-- Quem uma unidade atende ou em qual área atua. UNIQUE (name).
-- Estas áreas não classificam itens de doação.
INSERT INTO service_areas (name, active) VALUES
    ('Crianças e Adolescentes', TRUE),
    ('Pessoas Idosas', TRUE),
    ('Pessoas com Deficiência', TRUE),
    ('Pessoas em Situação de Vulnerabilidade Social', TRUE),
    ('Animais', TRUE),
    ('Educação', TRUE),
    ('Saúde', TRUE),
    ('Alimentação', TRUE),
    ('Acolhimento', TRUE),
    ('Meio Ambiente', TRUE),
    ('Outros', TRUE)
ON DUPLICATE KEY UPDATE id = service_areas.id;

-- Classificação dos itens/necessidades de doação. UNIQUE (name).
-- Estas categorias não representam áreas de atuação das unidades.
INSERT INTO need_categories (name, active) VALUES
    ('Alimentos', TRUE),
    ('Roupas e Calçados', TRUE),
    ('Higiene e Limpeza', TRUE),
    ('Material Escolar', TRUE),
    ('Móveis e Utensílios', TRUE),
    ('Medicamentos e Saúde', TRUE),
    ('Itens para Animais', TRUE),
    ('Outros', TRUE)
ON DUPLICATE KEY UPDATE id = need_categories.id;

-- Municípios serão carregados separadamente a partir de fonte oficial do IBGE.
-- A estratégia de importação será implementada na próxima etapa.

COMMIT;
