# Parte 2 — Núcleo institucional

## Segurança temporária e limite de uso

**Os endpoints de escrita e as consultas de gestão desta entrega NÃO possuem
proteção de autenticação/autorização. São destinados apenas a desenvolvimento e
testes. Não realizar deploy público antes da implementação do Spring Security
na Parte 3.** A palavra `management` na URL não é uma barreira de segurança.

Não há usuário fixo, login falso, header secreto, responsável simulado, token ou
bypass de autorização. A API pública filtra a visibilidade dos dados, mas isso
não protege as rotas de gestão nem as respostas de escrita.

Na Parte 3, proteger todas as operações POST/PUT/PATCH/DELETE listadas abaixo e
**todos os GET /api/management/**, incluindo contatos privados, razão social,
CNPJ, estados internos e necessidades não públicas. A criação de instituições e
alterações de status institucional devem receber política de plataforma definida
na próxima Parte; as demais ações deverão verificar vínculo institucional ativo
e permissões reais. Os services recebem IDs de instituição/unidade e validam a
vinculação dos recursos filhos ao pai indicado. Isso é integridade de domínio,
não autorização do solicitante. A autorização poderá ser aplicada na entrada dos
services, antes da execução transacional, sem reescrever validações e persistência.
Não foi criada uma política permissiva fictícia para substituí-la.

## Estado analisado e modelagem

Base: `main`, commit `7bf72a5` (Parte 1), inicialmente sem alterações locais.
Foram lidos `database/schema.sql`, `docs/database/database-model.md`, os cinco
domínios existentes e os fluxos públicos/institucionais do frontend.

Foram mapeadas exatamente as tabelas existentes:

| Entidade | Tabela | Relação principal |
| --- | --- | --- |
| Institution | institutions | N:1 InstitutionCategory |
| InstitutionContact | institution_contacts | N:1 Institution |
| InstitutionHistoryImage | institution_history_images | N:1 Institution |
| Unit | units | N:1 Institution e City |
| UnitContact | unit_contacts | N:1 Unit |
| UnitOpeningHours | unit_opening_hours | N:1 Unit |
| UnitServiceArea | unit_service_areas | chave composta Unit + ServiceArea |
| Need | needs | N:1 Unit e NeedCategory |

Não houve alteração de `database/schema.sql` nem criação de migration.
`ddl-auto=validate` e `open-in-view=false` foram preservados. Os domínios da
Parte 1 não foram refeitos. As relações são LAZY, sem cascatas de escrita ou
remoção inesperadas. DTOs e entidades permanecem separados; sem Lombok.
Identidade e timestamps comuns usam mapped superclasses. Campos CHAR mantêm
mapeamento JDBC CHAR explícito; valores TEXT, DECIMAL, TINYINT e comprimentos
seguem o schema. DATETIME é representado por LocalDateTime em ISO 8601, sem offset.

## Consultas públicas

| Método | Rota | Resposta |
| --- | --- | --- |
| GET | /api/institutions?cityId=&page=0&size=20 | página de resumos públicos |
| GET | /api/institutions/{id} | detalhes públicos agregados |
| GET | /api/institutions/{id}/units | unidades públicas agregadas |
| GET | /api/units/{id} | unidade pública agregada |
| GET | /api/cities/available | cidades com unidade ativa de instituição ativa |

`GET /api/cities` continua sendo o catálogo territorial da Parte 1.
A consulta `available` não inventa coordenadas, contagens ou instituições.
Cada cidade aparece uma vez, ordenada por nome e UF. Sem registros disponíveis,
retorna `200 []`; isso é esperado enquanto não houver instituições reais cadastradas.

Instituição pública significa `status=ACTIVE`. Unidades públicas exigem também
`Unit.status=ACTIVE` e instituição ativa. Instituições inativas/suspensas e suas
unidades retornam 404 na consulta pública direta. A listagem sem cidade pode
incluir uma instituição ativa ainda sem unidades; o filtro por cidade e o catálogo
`available` exigem uma unidade ativa na cidade. Categoria desativada não oculta
automaticamente dados institucionais já aprovados.

A listagem usa `page` base zero, tamanho padrão 20 e máximo 100. Valores fora dos
limites retornam 400. `cityId` deve ser positivo; uma cidade sem correspondência
retorna página vazia. `EXISTS` evita duplicar instituições com várias unidades
na mesma cidade. A ordem é nome de exibição e ID; descrições do resumo são
limitadas a 500 caracteres e a história completa não é carregada na listagem.

Formato da página:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Resumo: `id`, `displayName`, `categoryId`, `category` (nome), `description`.
Detalhe público: `id`, `displayName`, `categoryId`, `category`, `objective`,
`description`, `history`, `website`, `contacts`, `historyImages`, `units`.
Não inclui razão social, CNPJ, status interno ou timestamps de gestão.

Cada unidade pública contém `id`, `institutionId`, `name`, `description`,
`address`, `latitude`, `longitude`, `contacts`, `openingHours`, `serviceAreas`, `needs`.
`address` contém `cityId`, `city` (nome), `state` (UF), `postalCode`, `street`,
`number`, `complement` e `neighborhood`. Não inclui o documento da unidade.

Contatos: `id`, `type`, `value`, `label`, `isPrimary`, `isPublic`, `displayOrder`.
Consultas públicas selecionam apenas `isPublic=true` no banco e ordenam por
`displayOrder`, depois ID. Não existe imposição de um único contato principal no
schema; a flag não altera essa ordenação.

Necessidades: `id`, `categoryId`, `category` (nome), `name`, `description`,
`quantity`, `unitLabel`, `priority`, `status`. O campo de entrada `title` é
representado como `name` na resposta para compatibilidade com os renderizadores
existentes. `needs` público contém somente ACTIVE, ordenado por prioridade
HIGH/MEDIUM/LOW, título e ID. Gestão inclui todos os status na mesma ordenação.

História não depende de imagens: texto pode existir com `historyImages: []`.
Imagens retornam `id`, `url`, `alt`, `caption`, `order`, `createdAt`, compatíveis
com o visualizador atual; são ordenadas por `displayOrder` e ID. Não há download,
armazenamento binário, upload, geocodificação ou requisição ao servidor da imagem.

## Gestão e operações de escrita

**Todas as rotas desta seção precisarão de autorização na Parte 3.**

| Métodos | Rota | Operação |
| --- | --- | --- |
| GET | /api/management/institutions?page=0&size=20 | seleção paginada, incluindo status internos |
| GET | /api/management/institutions/{id} | agregado completo, com legalName/cnpj/status/datas |
| POST | /api/institutions | criar instituição |
| PUT, PATCH | /api/institutions/{id} | editar dados escalares |
| GET | /api/management/units/{id} | unidade completa, incluindo document/status/datas |
| POST | /api/institutions/{institutionId}/units | criar unidade vinculada |
| PUT, PATCH | /api/units/{id} | editar unidade sem transferir instituição |
| POST | /api/institutions/{institutionId}/contacts | adicionar contato geral |
| PUT, PATCH, DELETE | /api/institutions/{institutionId}/contacts/{id} | substituir, editar parcialmente, remover |
| GET | /api/management/institutions/{institutionId}/contacts[/{id}] | consultar contatos gerais, inclusive privados |
| POST | /api/units/{unitId}/contacts | adicionar contato local |
| PUT, PATCH, DELETE | /api/units/{unitId}/contacts/{id} | substituir, editar parcialmente, remover |
| GET | /api/management/units/{unitId}/contacts[/{id}] | consultar contatos locais, inclusive privados |
| POST | /api/units/{unitId}/needs | criar necessidade |
| PUT, PATCH, DELETE | /api/units/{unitId}/needs/{id} | substituir, editar/status, remover se INACTIVE |
| GET | /api/management/units/{unitId}/needs[/{id}] | consultar necessidades de qualquer status |
| PUT | /api/units/{unitId}/opening-hours/{day} | cadastrar/atualizar um dia (upsert) |
| GET | /api/management/units/{unitId}/opening-hours | consultar horários |
| PUT | /api/units/{unitId}/service-areas | substituir associações de áreas |
| GET | /api/management/units/{unitId}/service-areas | consultar associações |
| POST | /api/institutions/{institutionId}/history-images | incluir referência de imagem |
| PUT, PATCH, DELETE | /api/institutions/{institutionId}/history-images/{id} | metadados, URL, ordem e remoção da referência |
| GET | /api/management/institutions/{institutionId}/history-images[/{id}] | consultar referências |

A notação `[/{id}]` representa duas rotas GET: listagem e consulta individual.
POST retorna 201 e Location de gestão; PUT/PATCH e upserts retornam 200; DELETE,
204. Consultas individuais não encontradas retornam 404. Instituições e unidades
são desativadas por status; não se oferece exclusão física desses agregados.
Necessidades ACTIVE/FULFILLED não podem ser excluídas fisicamente (409); usar
PATCH de status para preservar o histórico, ou inativar antes de remover.
Remover imagem exclui apenas a referência; não apaga o arquivo no futuro provedor.

Gestão institucional agrega contatos privados e unidades de qualquer status.
Unidade de gestão inclui todas as necessidades e contatos, além de documento e
datas. A API ainda não determina quais instituições pertencem à conta, pois
Users e memberships estão fora desta Parte. Não consumir essa listagem como
se já fosse a seleção de instituições autorizadas do usuário.

## Contratos de escrita

IDs abaixo são ilustrativos, devem ser obtidos das APIs de referência.
POST e PUT exigem todos os campos obrigatórios. Status/prioridade/flags não
recebem defaults silenciosos: são explícitos no contrato.

Instituição:

```json
{
  "categoryId": 1,
  "displayName": "Nome de exibição",
  "legalName": "Razão social",
  "cnpj": "12345678000195",
  "objective": null,
  "description": null,
  "history": null,
  "website": null,
  "status": "ACTIVE"
}
```

Unidade (instituição é definida na URL, cidade por FK real):

```json
{
  "cityId": 1,
  "name": "Nome da unidade",
  "document": null,
  "description": null,
  "postalCode": "35595000",
  "street": "Logradouro",
  "number": "10",
  "complement": null,
  "neighborhood": "Bairro",
  "latitude": null,
  "longitude": null,
  "status": "ACTIVE"
}
```

Contato:

```json
{"type":"EMAIL","value":"contato@example.org","label":null,"isPrimary":true,"isPublic":true,"displayOrder":0}
```

Necessidade:

```json
{"categoryId":1,"title":"Item necessário","description":null,"quantity":2.50,"unitLabel":"kg","priority":"HIGH","status":"ACTIVE"}
```

Horário (PUT em `/opening-hours/1`, segunda-feira):

```json
{"closed":false,"opensAt":"08:00:00","closesAt":"18:00:00","note":null}
```

Dia fechado: `{"closed":true,"opensAt":null,"closesAt":null,"note":null}`.
Dias são ISO: 1=segunda, 7=domingo. Dia ausente significa horário não informado,
não fechamento presumido. Um dia tem um intervalo conforme o schema: não há
expediente atravessando meia-noite nem múltiplos intervalos no mesmo dia.
O upsert preserva o ID e a unicidade (unit_id, day_of_week).

Áreas: `{"serviceAreaIds":[1,2]}` substitui o conjunto. `[]` limpa as associações;
`null` não é aceito. Todas as referências são validadas antes da alteração e as
associações mantidas não são apagadas/reinseridas.

Imagem de história:

```json
{"imageUrl":"https://example.org/photo.jpg","altText":"Descrição acessível","caption":null,"displayOrder":0}
```

## PATCH e validações

DTOs de patch têm campos tipados e rastreiam presença via JsonSetter, separada
do valor. Exemplo `PATCH /api/institutions/{id}`:

```json
{"history":"Texto atualizado"}
```

Somente history é alterado. Campos não enviados permanecem intactos, inclusive
relacionamentos/coleções. `{"history":null}` remove explicitamente a história;
`{"displayName":null}` retorna 400 porque o campo é obrigatório. `{}` é um no-op.
Coleções têm endpoints próprios e não são aceitas no PATCH escalar. A estratégia
se aplica a instituição, unidade, contatos, necessidades e imagens. PUT substitui
os campos escalares do recurso: opcionais omitidos são limpos; filhos permanecem
independentes. A versão mesclada é integralmente validada antes de persistir.
Campos desconhecidos, JSON inválido, tipos/enums incompatíveis e enums numéricos
são rejeitados com 400, evitando ignorar silenciosamente alterações de contrato.

Validações:

- nomes obrigatórios não vazios e limites do schema;
- CNPJ numérico de 14 dígitos e verificadores válidos; documento de unidade opcional;
- CEP com oito dígitos, sem máscara;
- URLs absolutas HTTP(S), com host, sem credenciais; texto vazio não substitui null;
- EMAIL validado como e-mail; PHONE/WHATSAPP canônicos com 8–15 dígitos e `+` opcional;
- latitude -90..90 e longitude -180..180, sempre juntas, até oito casas decimais;
- quantidade >=0, no máximo oito dígitos inteiros e duas casas decimais;
- status/prioridade/tipo como enums explícitos do schema;
- horários coerentes com closed, abertura anterior ao fechamento, sem frações de segundo;
- ordem de exibição não negativa e unicidade de contatos conforme constraints;
- referências existentes, com taxonomias ativas para novas associações;
- textos TEXT limitados a 65.535 bytes em UTF-8, respeitando exatamente a capacidade
  do MySQL, sem reduzir artificialmente a quantidade permitida de caracteres ASCII.

Taxonomias desativadas já associadas podem permanecer ao editar outros campos.
Isso evita impedir correções em registros históricos; não permite novas
associações a categorias/áreas desativadas.

## Tratamento de erros

RestControllerAdvice retorna contrato uniforme:

```json
{"timestamp":"2026-10-27T23:00:00Z","status":400,"message":"Dados inválidos","fields":{"displayName":"não deve estar em branco"}}
```

400: validação/JSON/parâmetros. 404: recurso ausente ou invisível publicamente.
409: duplicidade/integridade ou exclusão incompatível com status.
405: método não suportado. Outros erros de protocolo preservam seu status (por
exemplo, 415 para conteúdo não suportado). 500: erro inesperado, com mensagem genérica e registro
interno. Nenhum erro devolve stack trace, SQL, nomes de constraints ou entidades.

## Desempenho e transações

Controller → Service → Repository → banco. Injeção por construtor.
Escritas são transacionais; leituras usam `@Transactional(readOnly=true)`.
DTOs são materializados dentro das transações, sem serialização de proxies JPA.

A listagem usa projeção JPQL com paginação. Categorias/cidade/estado são
carregados com fetch graph ou join; não se usa join fetch de várias coleções.
O agregado usa consultas separadas em lote: instituição, contatos gerais,
imagens, unidades (cidade/estado), contatos das unidades, horários, áreas com
taxonomia e necessidades com categoria. O número de consultas permanece
constante com mais unidades, sem produto cartesiano de coleções. Quando não há
unidades, as quatro consultas de filhos são dispensadas. Detalhes carregam um
agregado por vez; a listagem não retorna coleções nem história integral.

## Adaptações exatas na futura integração do frontend

Nenhum HTML/CSS/JS foi alterado nesta Parte.

1. `pages/cities.html` / `js/pages/cities.js`: obter cards de `/api/cities/available`,
   mantendo `/api/cities?state=UF` para formulários. A busca atual filtra o DOM;
   renderizar os cards reais e manter estados vazio/loading/erro. Não usar o
   catálogo territorial como indicação de instituições disponíveis. Marcadores
   devem usar coordenadas reais das unidades, não coordenadas inventadas da cidade.
2. `pages/institutions.html` / `js/pages/institutions.js`: enviar `cityId`, consumir
   `response.content` e implementar navegação por página. `createInstitutionCard`
   já recebe displayName/category/description compatíveis. Resumos não contêm units.
3. `pages/institution.html`, `js/pages/institution.js` e
   `js/components/institution-details.js`: conectar GET de detalhe aos renderizadores;
   diferenciar 404, falha e vazio. `category`, `address.city/state`, `needs[].name`
   e `historyImages[].url/alt/order` já correspondem aos renderizadores atuais.
   O visualizador atual só abre a galeria se houver texto de história, embora o
   schema/API permitam referências sem texto; considerar essa condição na integração.
   Áreas são objetos `{id,name,description}`: renderizar `area.name`, sem converter
   o objeto em string ou usar o dicionário de códigos fixos. Adicionar exibição
   de openingHours, que o backend já fornece e a tela atual ainda não renderiza.
4. `js/components/institution-details.js` e `js/services/map-service.js`: antes
   de converter coordenadas com Number(), testar explicitamente null/ausência.
   Hoje `Number(null)` vira zero e poderia produzir marcador incorreto. Criar um
   marcador por unidade somente quando as duas coordenadas forem válidas.
5. `pages/my-institution.html` / `js/pages/my-institution.js`: aguardar autorização
   real na Parte 3 para usar gestão; não tratar GET management como instituições
   do usuário. Mapear o atual `document` institucional para `cnpj`; selecionar
   `categoryId` por taxonomia, mantendo category textual só na exibição. Tratar
   também o status SUSPENDED, que o dicionário atual não rotula.
6. Eventos `bemfeito:institution-partial-update-requested`: enviar somente campos
   escalares alterados por PATCH; transformar remoção de opcionais em null. Os
   atuais historyImages/contactChanges devem ser enviados aos endpoints filhos,
   não como coleções no PATCH institucional. Fotos locais/blob/File continuam
   sendo previews; o upload futuro precisa fornecer imageUrl HTTP(S). Mapear
   url→imageUrl, alt→altText e order→displayOrder nas escritas; leituras já usam aliases.
7. `pages/unit-form.html` / `js/pages/unit-form.js`: usar `cityId` vindo de seletor
   UF/cidade real, sem enviar os nomes de cidade/estado como FK; limpar seleção ao
   trocar UF. A entrada da API é plana (postalCode/street/etc.), portanto desmembrar
   o atual changes.address. O endereço de leitura já é agregado e compatível.
8. O formulário de unidade deve exigir os campos obrigatórios do backend na criação,
   remover máscaras de CNPJ/CEP, enviar status explícito e coordenadas em par.
   Na edição, enviar apenas dirtyFields; não enviar institutionId para transferir unidade.
9. Substituir FOOD/CLOTHING/HYGIENE/CHILDREN/ELDERLY/ANIMALS/OTHER por IDs reais em
   `serviceAreaIds`; a lista de leitura contém objetos e a edição deve comparar IDs.
   `serviceAreasObservations` não existe no schema e não será persistido nem
   mapeado artificialmente em outro campo. Decidir esse requisito em etapa própria.
10. Editor de necessidades: selecionar needCategoryId real (entrada categoryId),
    mapear o atual name→title ao escrever, incluir quantity/unitLabel quando
    aplicável e traduzir contactChanges/needChanges em chamadas de recursos filhos.
    Nunca enviar IDs draft-*; usar o ID devolvido após POST. Remoção de necessidade
    ativa deve usar status INACTIVE antes do DELETE ou apenas manter a inativação.
11. Contatos: preservar flags isPublic/isPrimary e displayOrder, normalizar telefone
    conforme contrato e exibir mensagens de validação do objeto fields.
12. Incluir edição dos sete horários, com dias fechados e horários ausentes distintos.
    O backend não presume funcionamento por falta de informação.
13. Para múltiplas requisições de uma tela, tratar cada sucesso/erro e atualizar
    IDs/dados retornados; não prometer atomicidade de várias chamadas HTTP.
    Definir origem/proxy e CORS autorizado na etapa de integração, sem CORS irrestrito.

## Testes e reprodução

Na pasta backend, Java 25:

```powershell
.\mvnw.cmd test
```

A suíte padrão usa bancos H2 efêmeros, DDL SQL explícito e Hibernate validate.
InstitutionalApiTests fixa URL e credenciais H2 nas propriedades da própria classe,
para impedir uso acidental do MySQL real por um perfil global.
`institutional-fixtures.sql` é aplicado apenas em InstitutionalApiTests com
rollback por teste. Não há fixtures operacionais nos recursos de produção,
no seed ou no MySQL. MockMvc simula somente o transporte: serviços e JPA são reais.
Os testes da Parte 1 foram preservados.

Para verificar também o schema MySQL existente, somente com leituras:

```powershell
$env:MYSQL_REFERENCE_TESTS = 'true'
$env:DB_USER = 'root'
# Configurar DB_PASSWORD no ambiente local, sem salvar no repositório.
.\mvnw.cmd test
Remove-Item Env:MYSQL_REFERENCE_TESTS
Remove-Item Env:DB_PASSWORD
```

Não ativar o perfil mysql globalmente: somente MySqlReferenceDataTests o usa.
Os testes operacionais continuam isolados no H2. O teste MySQL é opt-in e não
inicializa scripts ou popula tabelas. A credencial usada localmente não foi
adicionada a nenhum arquivo versionável.

Na raiz:

```powershell
python -m unittest discover -s database/scripts -p 'test_*.py' -v
git diff --check
```

A verificação Python do DDL foi ampliada para as 13 tabelas mapeadas. Ela também
mantém os testes de integridade e reprodução offline da carga IBGE.

Cobertura institucional: listagem/paginação/filtro, cidades disponíveis, vazios,
404 e visibilidade, DTOs públicos/gestão, contatos privados/públicos, necessidades
por status/prioridade, criação/edição e PATCH/null/ausência, validações, conflitos,
FKs, sete dias/upsert, áreas atômicas, texto de história sem fotos, imagens com
ordenação/metadados/remoção e contagem de consultas contra N+1.

## Resultado e inventário

O resultado final executado e o inventário exato de arquivos seguem abaixo.
Não foi feito deploy público, commit ou cadastro de instituição no MySQL real.

### Validação final — 27/10/2026

- `mvnw.cmd ... test`, com MYSQL_REFERENCE_TESTS=true: **BUILD SUCCESS**.
- **41 testes Java**, zero falhas, zero erros e zero ignorados: 29 institucionais,
  nove de regressão das APIs de referência, dois somente leitura no MySQL e um
  de contexto Spring. Execução final concluída às 20:55 (America/Sao_Paulo).
- Contextos Spring inicializados no H2 e MySQL; Hibernate validou as 13 tabelas
  mapeadas; os 13 repositories inicializaram sem queries inválidas.
- Validada a capacidade exata de TEXT: 65.535 bytes ASCII aceitos; excesso em
  bytes ASCII ou UTF-8 multibyte rejeitado com 400.
- **Três testes Python aprovados**, incluindo fidelidade das 13 tabelas de teste
  ao schema MySQL versionado e integridade/reprodução offline da carga IBGE.
- O teste de desempenho confirmou o mesmo número de consultas para duas e oito
  unidades, limitado a nove consultas no agregado público.
- `git diff --check`: sem erros. Arquivos novos também verificados separadamente
  quanto a whitespace, UTF-8 e ausência de caracteres de substituição.
- As oito tabelas operacionais do MySQL permaneceram com zero registros, antes
  e depois da suíte. Nenhuma instituição fictícia foi cadastrada no banco real.
- Nenhuma alteração do schema de produção, integração visual, deploy público,
  autenticação simulada ou commit. HEAD permaneceu em `7bf72a5`.

### Auditoria e limpeza pré-commit — 27/10/2026

Nenhuma arquitetura, regra de negócio, endpoint, DTO ou teste foi alterado nesta
limpeza. `generate_writes.py` era um gerador descartável, não era necessário para
compilar, executar ou reproduzir o projeto e não foi movido para scripts definitivos.
Foram removidos de backend/target: generate_core.py, generate_queries.py,
generate_reads.py, generate_writes.py, generate_controllers.py, write_core_docs.py e
institution-generation.pkl. Nenhuma referência a esses artefatos foi encontrada
nos fontes, nos scripts definitivos ou na documentação de execução.

O Maven `clean test` apagou a saída antiga e reconstruiu o projeto sem esses
arquivos: BUILD SUCCESS, **41 testes Java**, zero falhas, zero erros e zero
ignorados, incluindo os dois testes somente leitura no MySQL real. Execução
concluída às 21:04 (America/Sao_Paulo). **Três testes Python** também passaram.
Hibernate continua validando as 13 tabelas mapeadas, com ddl-auto=validate.

O backend/.gitignore já ignora target/ e permanece inalterado. O .gitignore da
raiz foi reforçado para target/, *.class, __pycache__/, *.pyc, *.pyo, *.tmp,
*.temp, backups com ~ e relatórios locais de falhas JVM. git check-ignore confirmou
as regras para o gerador removido, bytecode, caches e temporários; git ls-files
não encontrou saídas compiladas/build rastreadas. Após os testes, target contém
apenas saídas Maven ignoradas; a afirmação de ausência de artefatos refere-se ao
conjunto candidato ao commit, não à ausência física de build após executar testes.

`git status` executado: main, somente alterações da Parte 2/limpeza, nada staged.
`git diff --check` passou. A inspeção adicional dos arquivos novos não encontrou
whitespace incorreto, UTF-8 inválido, geradores descartáveis ou artefatos de build.
Não há alterações no frontend ou no schema de produção. A varredura de credenciais
não identificou senhas reais, tokens, chaves privadas ou secrets nos candidatos;
configurações mantêm variáveis de ambiente e senha vazia para H2, sem credenciais
reais embutidas. Dados institucionais de teste continuam restritos ao H2 efêmero.
As oito tabelas operacionais do MySQL foram consultadas novamente após a suíte:
todas continuam com zero registros. Nenhum commit foi realizado; HEAD permanece
7bf72a5. Não houve deploy nem implementação de segurança falsa.

### Todos os arquivos candidatos ao commit, agrupados

Total: **84 arquivos** — 77 novos e 7 alterados.

#### common/error/validation (19)

- [backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactPatch.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactPatch.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/contact/dto/ContactResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/domain/ContactType.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/domain/ContactType.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/domain/InstitutionStatus.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/domain/InstitutionStatus.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/domain/NeedPriority.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/domain/NeedPriority.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/domain/NeedStatus.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/domain/NeedStatus.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/domain/UnitStatus.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/domain/UnitStatus.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/dto/PageResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/dto/PageResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/dto/PatchRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/dto/PatchRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/entity/IdentifiedEntity.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/entity/IdentifiedEntity.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/entity/TimestampedEntity.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/entity/TimestampedEntity.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/error/ApiError.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/error/ApiError.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/error/ApiExceptionHandler.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/error/ApiExceptionHandler.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/validation/HttpUrl.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/validation/HttpUrl.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/validation/HttpUrlValidator.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/validation/HttpUrlValidator.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/validation/RequestValidation.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/validation/RequestValidation.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/validation/Utf8Text.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/validation/Utf8Text.java) — novo.
- [backend/src/main/java/com/bemfeito/api/common/validation/Utf8TextValidator.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/common/validation/Utf8TextValidator.java) — novo.

#### institution (22)

- [backend/src/main/java/com/bemfeito/api/institution/contact/controller/InstitutionContactController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/contact/controller/InstitutionContactController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/contact/entity/InstitutionContact.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/contact/entity/InstitutionContact.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/contact/repository/InstitutionContactRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/contact/repository/InstitutionContactRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/contact/service/InstitutionContactService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/contact/service/InstitutionContactService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/controller/InstitutionController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/controller/InstitutionController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionManagementResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionManagementResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionManagementSummary.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionManagementSummary.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionPatch.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionPatch.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionPublicResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionPublicResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionSummary.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionSummary.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/entity/Institution.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/entity/Institution.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/controller/InstitutionHistoryImageController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/controller/InstitutionHistoryImageController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImagePatch.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImagePatch.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImageRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImageRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImageResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/dto/HistoryImageResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/entity/InstitutionHistoryImage.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/entity/InstitutionHistoryImage.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/repository/InstitutionHistoryImageRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/repository/InstitutionHistoryImageRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/history/service/InstitutionHistoryImageService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/history/service/InstitutionHistoryImageService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/repository/InstitutionRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/repository/InstitutionRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/institution/service/InstitutionService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/institution/service/InstitutionService.java) — novo.

#### unit (26)

- [backend/src/main/java/com/bemfeito/api/unit/contact/controller/UnitContactController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/contact/controller/UnitContactController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/contact/entity/UnitContact.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/contact/entity/UnitContact.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/contact/repository/UnitContactRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/contact/repository/UnitContactRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/contact/service/UnitContactService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/contact/service/UnitContactService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/controller/UnitController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/controller/UnitController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/dto/AddressResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/dto/AddressResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/dto/UnitManagementResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/dto/UnitManagementResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/dto/UnitPatch.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/dto/UnitPatch.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/dto/UnitPublicResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/dto/UnitPublicResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/dto/UnitRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/dto/UnitRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/entity/Unit.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/entity/Unit.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/controller/UnitOpeningHoursController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/controller/UnitOpeningHoursController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/dto/OpeningHoursRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/dto/OpeningHoursRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/dto/OpeningHoursResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/dto/OpeningHoursResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/entity/UnitOpeningHours.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/entity/UnitOpeningHours.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/repository/UnitOpeningHoursRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/repository/UnitOpeningHoursRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/hours/service/UnitOpeningHoursService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/hours/service/UnitOpeningHoursService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/repository/UnitRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/repository/UnitRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/service/UnitQueryService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/service/UnitQueryService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/service/UnitService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/service/UnitService.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/controller/UnitServiceAreaController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/controller/UnitServiceAreaController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/dto/ServiceAreasRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/dto/ServiceAreasRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/entity/UnitServiceArea.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/entity/UnitServiceArea.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/entity/UnitServiceAreaId.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/entity/UnitServiceAreaId.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/repository/UnitServiceAreaRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/repository/UnitServiceAreaRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/unit/servicearea/service/UnitServiceAreaService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/unit/servicearea/service/UnitServiceAreaService.java) — novo.

#### need (7)

- [backend/src/main/java/com/bemfeito/api/need/controller/NeedController.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/controller/NeedController.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/dto/NeedPatch.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/dto/NeedPatch.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/dto/NeedRequest.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/dto/NeedRequest.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/dto/NeedResponse.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/dto/NeedResponse.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/entity/Need.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/entity/Need.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/repository/NeedRepository.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/repository/NeedRepository.java) — novo.
- [backend/src/main/java/com/bemfeito/api/need/service/NeedService.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/java/com/bemfeito/api/need/service/NeedService.java) — novo.

#### location/city (0)

Nenhum arquivo neste grupo. O domínio location/city da Parte 1 permanece inalterado.

#### tests (6)

- [backend/src/test/java/com/bemfeito/api/InstitutionalApiTests.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/test/java/com/bemfeito/api/InstitutionalApiTests.java) — novo.
- [backend/src/test/java/com/bemfeito/api/MySqlReferenceDataTests.java](C:/Users/BRYAN/Desktop/bem-feito/backend/src/test/java/com/bemfeito/api/MySqlReferenceDataTests.java) — alterado.
- [backend/src/test/resources/application.properties](C:/Users/BRYAN/Desktop/bem-feito/backend/src/test/resources/application.properties) — alterado.
- [backend/src/test/resources/institutional-fixtures.sql](C:/Users/BRYAN/Desktop/bem-feito/backend/src/test/resources/institutional-fixtures.sql) — novo.
- [backend/src/test/resources/reference-schema.sql](C:/Users/BRYAN/Desktop/bem-feito/backend/src/test/resources/reference-schema.sql) — alterado.
- [database/scripts/test_prepare_cities.py](C:/Users/BRYAN/Desktop/bem-feito/database/scripts/test_prepare_cities.py) — alterado.

#### documentação/configuração (4)

- [.gitignore](C:/Users/BRYAN/Desktop/bem-feito/.gitignore) — alterado.
- [README.md](C:/Users/BRYAN/Desktop/bem-feito/README.md) — alterado.
- [backend/src/main/resources/application.properties](C:/Users/BRYAN/Desktop/bem-feito/backend/src/main/resources/application.properties) — alterado.
- [docs/backend/institutional-core.md](C:/Users/BRYAN/Desktop/bem-feito/docs/backend/institutional-core.md) — novo.

