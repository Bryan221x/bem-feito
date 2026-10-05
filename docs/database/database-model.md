# Modelo de Dados — Bem-Feito

## 1. Visão Geral

O Bem-Feito é uma plataforma destinada a aproximar pessoas interessadas em ajudar de instituições beneficentes.

O modelo de dados foi estruturado considerando:

- normalização até a Terceira Forma Normal (3FN);
- separação entre usuários e instituições;
- possibilidade de uma conta administrar várias instituições;
- possibilidade de uma instituição possuir vários responsáveis;
- múltiplas unidades físicas por instituição;
- contatos próprios para instituição e unidades;
- necessidades específicas por unidade;
- áreas de atuação;
- história institucional e fotografias;
- solicitações de cadastro;
- solicitações de acesso;
- administração global da plataforma.

O banco de dados será implementado utilizando MySQL.

## Dados de referência

O arquivo `database/reference-data.sql` contém os 26 estados brasileiros e o Distrito Federal como dados territoriais de referência, além das taxonomias controladas pelo Bem-Feito: `institution_categories` (classificação das instituições), `service_areas` (públicos atendidos e áreas de atuação das unidades) e `need_categories` (classificação dos itens/necessidades de doação). Esses dados não são mocks nem cadastros de demonstração.

O script deve ser executado após `database/schema.sql` e pode ser repetido: utiliza as constraints UNIQUE existentes e preserva os registros já cadastrados. Novas entradas das taxonomias iniciam com `active = TRUE`.

Municípios serão obtidos separadamente de fonte oficial do IBGE, em uma próxima etapa; não fazem parte deste script.

---

# 2. Convenções

## Identificadores

As entidades principais utilizarão:

BIGINT AUTO_INCREMENT

como chave primária.

Exemplo:

id BIGINT PRIMARY KEY AUTO_INCREMENT

## Datas

As principais tabelas deverão possuir:

created_at
updated_at

para controle de criação e atualização.

## Exclusão

Dados importantes não deverão ser removidos automaticamente apenas porque uma conta ou instituição foi desativada.

Sempre que possível serão utilizados campos de status para preservar o histórico do sistema.

## Senhas

Senhas nunca serão armazenadas em texto puro.

O banco armazenará somente:

password_hash

A geração e validação do hash será responsabilidade do backend.

---

# 3. USERS

Representa uma conta capaz de acessar áreas autenticadas do Bem-Feito.

Uma conta não representa diretamente uma instituição.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| name | VARCHAR(150) | Sim | Nome da pessoa |
| email | VARCHAR(255) | Sim | UNIQUE |
| password_hash | VARCHAR(255) | Não inicialmente | Preenchido após criação da senha |
| platform_role | VARCHAR(30) | Sim | USER ou PLATFORM_ADMIN |
| status | VARCHAR(30) | Sim | Estado da conta |
| email_verified_at | DATETIME | Não | Confirmação futura |
| created_at | DATETIME | Sim | Data de criação |
| updated_at | DATETIME | Sim | Última alteração |

## Regras

O e-mail deve ser único no sistema.

Possíveis papéis globais:

USER
PLATFORM_ADMIN

O vínculo da pessoa com uma instituição NÃO será armazenado nesta tabela.

Esse vínculo será responsabilidade de:

institution_memberships

---

# 4. INSTITUTION_CATEGORIES

Representa a classificação principal da instituição.

Exemplos futuros:

Assistência social
Proteção animal
Educação
Saúde
Acolhimento

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| name | VARCHAR(100) | Sim | UNIQUE |
| description | VARCHAR(255) | Não | Descrição |
| active | BOOLEAN | Sim | Categoria disponível |
| created_at | DATETIME | Sim | Criação |

---

# 5. INSTITUTIONS

Representa a instituição cadastrada e aprovada no Bem-Feito.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| category_id | BIGINT | Sim | FK institution_categories |
| display_name | VARCHAR(150) | Sim | Nome público |
| legal_name | VARCHAR(200) | Sim | Razão social |
| cnpj | CHAR(14) | Sim | UNIQUE, somente números |
| objective | TEXT | Não | Objetivo institucional |
| description | TEXT | Não | Resumo institucional |
| history | TEXT | Não | História completa |
| website | VARCHAR(255) | Não | Site oficial |
| status | VARCHAR(30) | Sim | ACTIVE, INACTIVE ou SUSPENDED |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Regras

display_name é o nome utilizado principalmente na área pública.

legal_name representa a razão social.

O CNPJ será armazenado sem formatação e deverá conter exatamente 14 dígitos.

history é opcional e poderá conter textos longos.

A URL pública NÃO será armazenada porque pode ser construída pela aplicação a partir do identificador da instituição.

---

# 6. INSTITUTION_MEMBERSHIPS

Representa o vínculo entre uma conta e uma instituição.

Resolve o relacionamento muitos-para-muitos entre users e institutions.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| user_id | BIGINT | Sim | FK users |
| institution_id | BIGINT | Sim | FK institutions |
| role | VARCHAR(30) | Sim | Permissão institucional |
| status | VARCHAR(30) | Sim | ACTIVE, PENDING ou REVOKED |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Restrições

A combinação:

user_id + institution_id

deve ser UNIQUE.

## Papéis institucionais previstos

OWNER

MANAGER

EDITOR

Esses papéis são diferentes de platform_role.

Exemplo:

Uma pessoa pode possuir:

platform_role = USER

e simultaneamente:

Lar Esperança → OWNER
Outra Instituição → MANAGER

---

# 7. STATES

Representa os estados brasileiros.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| name | VARCHAR(100) | Sim | UNIQUE |
| uf | CHAR(2) | Sim | UNIQUE |

---

# 8. CITIES

Representa os municípios.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| state_id | BIGINT | Sim | FK states |
| name | VARCHAR(150) | Sim | Nome da cidade |

## Restrição

A combinação:

state_id + name

deve ser UNIQUE.

## Relacionamento

STATE 1:N CITY

Isso evita armazenar repetidamente:

Luz / MG
Luz / Minas Gerais
LUZ / mg

em registros diferentes.

---

# 9. UNITS

Representa uma unidade física de uma instituição.

Uma instituição poderá possuir uma ou várias unidades.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| institution_id | BIGINT | Sim | FK institutions |
| city_id | BIGINT | Sim | FK cities |
| name | VARCHAR(150) | Sim | Nome da unidade |
| document | CHAR(14) | Não | CNPJ próprio da unidade |
| description | TEXT | Não | Descrição |
| postal_code | CHAR(8) | Sim | CEP |
| street | VARCHAR(180) | Sim | Logradouro |
| number | VARCHAR(30) | Sim | Número |
| complement | VARCHAR(120) | Não | Complemento |
| neighborhood | VARCHAR(120) | Sim | Bairro |
| latitude | DECIMAL(10,8) | Não | Latitude |
| longitude | DECIMAL(11,8) | Não | Longitude |
| status | VARCHAR(30) | Sim | ACTIVE ou INACTIVE |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Regras

Uma unidade sempre pertence a uma instituição.

Uma instituição poderá possuir várias unidades.

O CNPJ próprio da unidade é opcional; quando informado, deverá conter exatamente 14 dígitos, sem formatação.

O CEP será armazenado sem formatação e deverá conter exatamente 8 dígitos.

Latitude e longitude somente serão preenchidas quando uma coordenada real estiver disponível e deverão ser informadas juntas.

Na área pública:

1 unidade física = 1 marcador no mapa.

---

# 10. INSTITUTION_CONTACTS

Representa os contatos gerais da instituição.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| institution_id | BIGINT | Sim | FK institutions |
| type | VARCHAR(30) | Sim | EMAIL, PHONE ou WHATSAPP |
| value | VARCHAR(255) | Sim | Valor do contato |
| label | VARCHAR(100) | Não | Ex.: Doações |
| is_primary | BOOLEAN | Sim | Contato principal |
| is_public | BOOLEAN | Sim | Pode aparecer publicamente |
| display_order | INT | Sim | Ordem de apresentação |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Regra

A instituição poderá possuir quantos contatos forem necessários.

A mesma combinação de instituição, tipo e valor de contato não poderá se repetir.

Não existirão campos como:

email1
email2
phone1
phone2

---

# 11. UNIT_CONTACTS

Representa contatos próprios de uma unidade.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| unit_id | BIGINT | Sim | FK units |
| type | VARCHAR(30) | Sim | EMAIL, PHONE ou WHATSAPP |
| value | VARCHAR(255) | Sim | Contato |
| label | VARCHAR(100) | Não | Identificação |
| is_primary | BOOLEAN | Sim | Principal |
| is_public | BOOLEAN | Sim | Público |
| display_order | INT | Sim | Ordem |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Regra

A mesma combinação de unidade, tipo e valor de contato não poderá se repetir.

---

# 12. UNIT_OPENING_HOURS

Representa os horários de funcionamento de cada unidade.

O horário pertence à unidade física e não diretamente à instituição.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| unit_id | BIGINT | Sim | FK units |
| day_of_week | TINYINT | Sim | 1 a 7 |
| opens_at | TIME | Não | Horário de abertura |
| closes_at | TIME | Não | Horário de fechamento |
| closed | BOOLEAN | Sim | Unidade fechada nesse dia |
| note | VARCHAR(150) | Não | Observação |

## Regra

day_of_week seguirá uma convenção única definida pelo backend.

Exemplo:

1 = Segunda-feira
...
7 = Domingo

---

# 13. SERVICE_AREAS

Representa as áreas em que uma unidade atua.

Esse conceito NÃO é igual à categoria de necessidade.

Exemplos:

Crianças
Idosos
Animais
Assistência alimentar
Acolhimento

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| name | VARCHAR(100) | Sim | UNIQUE |
| description | VARCHAR(255) | Não | Descrição |
| active | BOOLEAN | Sim | Disponibilidade |

---

# 14. UNIT_SERVICE_AREAS

Tabela associativa entre unidades e áreas de atuação.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| unit_id | BIGINT | Sim | FK units |
| service_area_id | BIGINT | Sim | FK service_areas |

## Chave

A chave poderá ser composta por:

unit_id + service_area_id

Essa combinação não poderá se repetir.

---

# 15. NEED_CATEGORIES

Representa categorias utilizadas para organizar necessidades.

Exemplos:

Alimentos
Roupas
Higiene
Material escolar
Móveis
Outros

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| name | VARCHAR(100) | Sim | UNIQUE |
| description | VARCHAR(255) | Não | Descrição |
| active | BOOLEAN | Sim | Categoria ativa |

---

# 16. NEEDS

Representa uma necessidade real de uma unidade.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| unit_id | BIGINT | Sim | FK units |
| category_id | BIGINT | Sim | FK need_categories |
| title | VARCHAR(150) | Sim | Nome |
| description | TEXT | Não | Informações adicionais |
| quantity | DECIMAL(10,2) | Não | Quantidade desejada |
| unit_label | VARCHAR(50) | Não | kg, unidades etc. |
| priority | VARCHAR(20) | Sim | LOW, MEDIUM ou HIGH |
| status | VARCHAR(30) | Sim | ACTIVE, FULFILLED ou INACTIVE |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Regra

Necessidades pertencem à unidade.

Não diretamente à instituição.

Exemplo:

Instituição
    Unidade Centro
        Arroz
        Fraldas

    Unidade Norte
        Leite
        Roupas

---

# 17. INSTITUTION_HISTORY_IMAGES

Representa fotografias utilizadas na experiência pública "Nossa história".

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| institution_id | BIGINT | Sim | FK institutions |
| image_url | VARCHAR(500) | Sim | Referência futura da imagem |
| alt_text | VARCHAR(255) | Sim | Acessibilidade |
| caption | VARCHAR(255) | Não | Legenda |
| display_order | INT | Sim | Ordem |
| created_at | DATETIME | Sim | Criação |

## Regras

A instituição poderá possuir:

0 imagens
1 imagem
várias imagens

A ausência de imagens não impede a existência de history.

O frontend deverá funcionar sem fotografias.

---

# 18. REGISTRATION_REQUESTS

Representa uma solicitação para cadastrar uma instituição ainda inexistente no Bem-Feito.

A solicitação NÃO cria imediatamente uma instituição.

Primeiro será analisada pelo administrador da plataforma.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| category_id | BIGINT | Sim | FK institution_categories |
| display_name | VARCHAR(150) | Sim | Nome público |
| legal_name | VARCHAR(200) | Sim | Razão social |
| cnpj | CHAR(14) | Sim | CNPJ solicitado |
| objective | TEXT | Não | Objetivo |
| description | TEXT | Não | Descrição |
| history | TEXT | Não | História |
| website | VARCHAR(255) | Não | Site |
| responsible_name | VARCHAR(150) | Sim | Solicitante |
| responsible_email | VARCHAR(255) | Sim | E-mail |
| responsible_phone | VARCHAR(30) | Sim | Contato |
| responsible_role | VARCHAR(100) | Sim | Função na instituição |
| status | VARCHAR(30) | Sim | Situação |
| reviewed_by_user_id | BIGINT | Não | FK users |
| reviewed_at | DATETIME | Não | Data da análise |
| review_notes | TEXT | Não | Observação do administrador |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Status previstos

PENDING
APPROVED
REJECTED
NEEDS_CHANGES
CANCELLED

O CNPJ da solicitação será armazenado sem formatação e deverá conter exatamente 14 dígitos.

---

# 19. REGISTRATION_REQUEST_CONTACTS

Representa contatos públicos informados durante a solicitação de cadastro.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| registration_request_id | BIGINT | Sim | FK registration_requests |
| type | VARCHAR(30) | Sim | EMAIL, PHONE ou WHATSAPP |
| value | VARCHAR(255) | Sim | Contato |
| label | VARCHAR(100) | Não | Identificação |
| is_primary | BOOLEAN | Sim | Principal |

## Regra

A mesma combinação de solicitação, tipo e valor de contato não poderá se repetir.

---

# 20. REGISTRATION_REQUEST_UNITS

Representa a unidade principal informada durante a solicitação inicial.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| registration_request_id | BIGINT | Sim | FK registration_requests |
| city_id | BIGINT | Sim | FK cities |
| name | VARCHAR(150) | Sim | Nome da unidade |
| document | CHAR(14) | Não | CNPJ da unidade |
| description | TEXT | Não | Descrição |
| postal_code | CHAR(8) | Sim | CEP |
| street | VARCHAR(180) | Sim | Logradouro |
| number | VARCHAR(30) | Sim | Número |
| complement | VARCHAR(120) | Não | Complemento |
| neighborhood | VARCHAR(120) | Sim | Bairro |

## Regra

Embora inicialmente o formulário possua uma unidade principal, a estrutura permite evolução sem misturar endereço com os dados da instituição.

O CNPJ da unidade é opcional; quando informado, deverá conter exatamente 14 dígitos, sem formatação. O CEP deverá conter exatamente 8 dígitos, também sem formatação.

---

# 21. REGISTRATION_REQUEST_UNIT_CONTACTS

Representa contatos específicos da unidade informada na solicitação.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| registration_request_unit_id | BIGINT | Sim | FK registration_request_units |
| type | VARCHAR(30) | Sim | EMAIL, PHONE ou WHATSAPP |
| value | VARCHAR(255) | Sim | Contato |
| label | VARCHAR(100) | Não | Identificação |
| is_primary | BOOLEAN | Sim | Principal |

## Regra

A mesma combinação de unidade da solicitação, tipo e valor de contato não poderá se repetir.

---

# 22. ACCESS_REQUESTS

Representa uma solicitação para administrar uma instituição que já existe no Bem-Feito.

Esse fluxo não cria outra instituição.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| institution_id | BIGINT | Sim | FK institutions |
| requester_user_id | BIGINT | Não | FK users, se já possuir conta |
| requester_name | VARCHAR(150) | Sim | Solicitante |
| requester_email | VARCHAR(255) | Sim | E-mail |
| requester_phone | VARCHAR(30) | Sim | Telefone |
| requester_role | VARCHAR(100) | Sim | Função |
| justification | TEXT | Não | Justificativa |
| status | VARCHAR(30) | Sim | Situação |
| reviewed_by_user_id | BIGINT | Não | Administrador responsável |
| reviewed_at | DATETIME | Não | Análise |
| review_notes | TEXT | Não | Observações |
| created_at | DATETIME | Sim | Criação |
| updated_at | DATETIME | Sim | Alteração |

## Status

PENDING
APPROVED
REJECTED
NEEDS_CHANGES
CANCELLED

## Regra

Ao aprovar a solicitação, o sistema deverá criar ou habilitar o vínculo correspondente em:

institution_memberships

Nenhuma nova instituição será criada.

---

# 23. ACCOUNT_INVITATION_TOKENS

Representa convites seguros enviados após aprovação de um cadastro ou acesso quando for necessário criar ou ativar uma conta.

## Campos

| Campo | Tipo lógico | Obrigatório | Regra |
|---|---|---:|---|
| id | BIGINT | Sim | PK |
| user_id | BIGINT | Sim | FK users |
| token_hash | VARCHAR(255) | Sim | UNIQUE |
| expires_at | DATETIME | Sim | Expiração |
| used_at | DATETIME | Não | Utilização |
| created_at | DATETIME | Sim | Criação |

## Segurança

O token bruto não deverá ser armazenado diretamente.

O banco guarda apenas seu hash.

---

# 24. Principais Relacionamentos

users N:N institutions
através de institution_memberships

institution_categories 1:N institutions

states 1:N cities

cities 1:N units

institutions 1:N units

institutions 1:N institution_contacts

units 1:N unit_contacts

units 1:N unit_opening_hours

units N:N service_areas
através de unit_service_areas

units 1:N needs

need_categories 1:N needs

institutions 1:N institution_history_images

registration_requests 1:N registration_request_contacts

registration_requests 1:N registration_request_units

registration_request_units 1:N registration_request_unit_contacts

institutions 1:N access_requests

users 1:N account_invitation_tokens

---

# 25. Regras de Negócio Fundamentais

Uma conta não representa diretamente uma instituição.

Uma conta pode possuir vínculo com várias instituições.

Uma instituição pode possuir vários responsáveis.

Uma conta somente poderá editar uma instituição se possuir vínculo ativo e permissão suficiente.

O administrador global do Bem-Feito possui permissões da plataforma, não sendo necessário possuir vínculo institucional para realizar tarefas administrativas globais.

Uma instituição poderá possuir várias unidades.

O endereço pertence à unidade.

A localização no mapa pertence à unidade.

Uma unidade física corresponde a um marcador no mapa.

Contatos gerais pertencem à instituição.

Contatos locais pertencem à unidade.

Necessidades pertencem à unidade.

Áreas de atuação pertencem às unidades.

História pertence à instituição.

Fotos da história pertencem à instituição e são opcionais.

Solicitação de cadastro não cria automaticamente uma instituição.

Solicitação de acesso não cria uma nova instituição.

A aprovação de uma solicitação deverá ocorrer por meio de operações transacionais no backend.

Dados públicos deverão ser exibidos somente quando permitidos pelas regras de visibilidade.

---

# 26. Atualizações Parciais

A API deverá permitir atualização parcial de instituições e outras entidades quando aplicável.

Exemplo:

PATCH /api/institutions/{id}

Se apenas history for alterado:

{
  "history": "Novo conteúdo"
}

o backend não deverá sobrescrever:

objective
description
website
contacts
units

ou qualquer outro campo não enviado.

Essa regra corresponde ao comportamento já preparado no frontend.

---

# 27. Normalização

O modelo foi projetado até a Terceira Forma Normal.

## Primeira Forma Normal — 1FN

Os atributos possuem valores atômicos.

Não existem estruturas como:

phone1
phone2
phone3

ou:

email1
email2
email3

Contatos são registros independentes.

## Segunda Forma Normal — 2FN

Os atributos dependem da chave integral de suas tabelas.

Tabelas associativas representam relacionamentos muitos-para-muitos sem armazenar atributos dependentes apenas de parte indevida da chave.

## Terceira Forma Normal — 3FN

Não existem dependências transitivas desnecessárias entre atributos não chave.

Exemplo:

units armazena city_id.

O estado não é repetido em units porque:

unit
→ city
→ state

Da mesma forma, nomes de categorias e áreas não são repetidos nas entidades que os utilizam.

---

# 28. Dados Derivados

Dados que podem ser calculados ou construídos pela aplicação não deverão ser armazenados sem necessidade.

Exemplo:

public_url

pode ser construída pelo backend utilizando o identificador da instituição.

Contagens como:

quantidade de unidades
quantidade de responsáveis
quantidade de necessidades

também poderão ser calculadas por consultas.

---

# 29. Integridade e Segurança

O banco deverá utilizar:

- chaves primárias;
- chaves estrangeiras;
- índices;
- restrições UNIQUE;
- validações NOT NULL;
- regras de exclusão controladas.

O backend será responsável por:

- autenticação;
- autorização;
- validação de permissões;
- validação dos dados;
- hashing de senhas;
- controle de transações;
- sanitização e segurança da API.

---

# 30. Escopo desta versão

A versão atual do Bem-Feito não terá:

- pagamento dentro da plataforma;
- chat entre usuário e instituição;
- logística de retirada ou entrega;
- acompanhamento financeiro de doações;
- métricas fictícias de impacto;
- sistema de comentários ou avaliações públicas.

O foco permanecerá em:

- descoberta de instituições;
- localização;
- necessidades;
- contato;
- informações institucionais;
- administração segura dos dados.
