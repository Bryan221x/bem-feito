# Parte 1 — Dados de referência

## Estado inicial e escopo

Analisado `main`, commit `69c650b`, sem alterações locais. Já existiam schema,
seeds das taxonomias/27 UFs e o domínio State com GET /api/states.
Foram preservados. Não há implementação de Institution, Unit, Need, autenticação,
administração ou integração do frontend nesta entrega.

## Municípios e reprodução da carga

Fonte: [API oficial de localidades do IBGE](https://servicodados.ibge.gov.br/api/docs/localidades),
[consulta utilizada](https://servicodados.ibge.gov.br/api/v1/localidades/municipios?orderBy=id).
O snapshot obtido em 27/10/2026 tem **5.571 entradas**, cobrindo as 27 UFs.
É a lista integral retornada pelo endpoint, incluindo Brasília e Fernando de
Noronha como entradas territoriais do IBGE; não se deve interpretar cada entrada
como município com o mesmo regime administrativo. Inclui Boa Esperança do Norte/MT.

Arquivos versionáveis:

- `database/ibge/municipalities.csv`: código IBGE, UF e nome, em UTF-8.
- `database/ibge/manifest.json`: fonte, horário UTC da coleta, contagens por UF e SHA-256.
- `database/scripts/prepare_cities.py`: Python 3, apenas biblioteca padrão.
- `database/cities.sql`: carga MySQL 8+ separada do seed pequeno.

A UF vem de `regiao-imediata.regiao-intermediaria.UF`. A microrregião antiga
está ausente para Boa Esperança do Norte e não é usada. O gerador verifica
compatibilidade do prefixo do código IBGE com a UF, códigos únicos, cobertura
nacional, nomes, checksum e contagens do manifesto antes de gerar SQL.

Na raiz do projeto, sem rede:

```powershell
python database/scripts/prepare_cities.py
```

Para uma atualização explícita pela internet:

```powershell
python database/scripts/prepare_cities.py --refresh
```

Revisar o diff do CSV, manifesto e SQL antes de importar uma atualização.
A atualização é aditiva: nomes novos são inseridos, mas renomeações e extinções
exigem conciliação humana porque o schema atual não armazena o código IBGE.
O código é preservado no CSV para essa auditoria. Não se apagam cidades nem
se alteram IDs potencialmente referenciados por unidades.

### MySQL

Em um banco novo, executar `database/schema.sql`, depois `database/reference-data.sql`.
No banco já preparado, executar apenas `database/cities.sql`. O backend não
executa esses arquivos na inicialização. No PowerShell, abrir o cliente:

```powershell
& 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe' --host=127.0.0.1 --user=root --password --default-character-set=utf8mb4
```

Dentro do cliente MySQL, ajustar o caminho absoluto conforme o checkout:

```sql
source C:/Users/BRYAN/Desktop/bem-feito/database/cities.sql
SELECT COUNT(*) FROM bem_feito.cities;
```

Não usar `mysql --force`: erros devem interromper a execução. A carga usa
transação e tabelas temporárias InnoDB; exige todas as UFs antes de inserir em
`cities`, habilita modo estrito e restaura o modo da sessão após sucesso.
Em caso de erro no cliente interativo, executar `ROLLBACK` e encerrar a conexão;
no cliente em lote, a conexão encerrada desfaz a transação pendente.
São necessários privilégios de tabelas temporárias, SELECT e INSERT/UPDATE
nas tabelas envolvidas. O gerador nunca recebe senha nem conecta ao banco.
Os literais SQL usam hexadecimal UTF-8 para preservar acentos e apóstrofos
independentemente de `NO_BACKSLASH_ESCAPES`; o CSV permanece legível para revisão.

O vínculo é resolvido por `states.uf`, nunca pelo ID do IBGE ou por uma posição
fixa do seed. `cities.id` continua sendo a identidade interna AUTO_INCREMENT.
A restrição UNIQUE(state_id, name) permite repetir a carga sem duplicar cidades
nem trocar IDs existentes. Reexecuções podem consumir valores da sequência
AUTO_INCREMENT; lacunas são normais e IDs não devem ser usados como contagem.
O backend permanece independente da API externa depois da importação.

## API pública

| Método e rota | Resultado |
| --- | --- |
| GET /api/states | Estados ordenados por nome; contrato existente |
| GET /api/cities | Todas as cidades por nome; desempate por UF |
| GET /api/cities?state=MG | Somente cidades de MG, por nome |
| GET /api/cities?stateId=13 | Filtro pelo ID interno do estado |
| GET /api/institution-categories | Categorias ativas, por nome |
| GET /api/service-areas | Áreas ativas, por nome |
| GET /api/need-categories | Categorias de necessidades ativas, por nome |

Todas as listagens retornam arrays JSON; sem resultados, `200 []`.
`state` aceita espaços externos e letras minúsculas. Uma UF de duas letras sem
correspondência retorna vazio. UF vazia/malformada, ID não numérico/não positivo
ou os dois filtros simultâneos retornam 400. Não há escrita pública; POST retorna 405.
A ordenação usa a collation do MySQL (`utf8mb4_0900_ai_ci`, insensível a acentos).
Os formulários devem buscar por UF/ID: o filtro acontece no SQL e não baixa o
Brasil inteiro. A listagem nacional permanece simples, sem paginação nesta etapa.

Contratos dos itens (tipos; IDs abaixo são ilustrativos e variam por instalação):

```json
{"id": 123, "name": "Luz", "state": {"id": 13, "name": "Minas Gerais", "uf": "MG"}}
```

Estados: `{"id": number, "name": string, "uf": string}`.
As três taxonomias usam `{"id": number, "name": string, "description": string|null}`.
`active` é filtro de consulta, não um campo necessário do DTO público.

## Arquitetura

Controller → Service → Repository → MySQL; entidades nunca são respostas HTTP.
Injeção por construtor, records para DTOs e `@Transactional(readOnly = true)`
nos services. City tem ManyToOne LAZY obrigatório com State. EntityGraph carrega
o estado nas consultas para evitar N+1 e a conversão acontece dentro da transação.
Não há cascatas de escrita, Lombok, chamadas ao IBGE no Spring ou mudança do schema.
`ddl-auto=validate` e `open-in-view=false` foram mantidos.

Ajustes pontuais no código existente: `State.uf` recebeu `@JdbcTypeCode(CHAR)`
para refletir o CHAR(2) já definido no SQL e passar a validação de tipos;
o Maven Wrapper do Windows passou a verificar Target nulo antes de indexá-lo,
pois uma pasta .m2 comum não tem destino de link. Nenhuma dessas partes foi refeita.
H2 foi adicionado exclusivamente no escopo de testes.

## Testes e validação

Na pasta `backend`, Java 25:

```powershell
.\mvnw.cmd test
```

No Linux/macOS: `./mvnw test`. A suíte padrão cria bancos H2 em memória em modo
MySQL, inicializa via SQL explícito e mantém o Hibernate em `validate`.
As fixtures são apenas localidades reais e taxonomias já existentes no projeto.
Alterações de ativação e exclusões para testar vazios ficam nos bancos efêmeros,
com rollback; nunca são aplicadas ao MySQL de desenvolvimento.
MockMvc simula o transporte HTTP, mas controllers, services, repositories e JPA
são reais: não há beans mockados. O DDL de teste contém exatamente as cinco
tabelas de referência extraídas do schema MySQL; há teste Python contra divergência.
H2 não substitui a verificação do dialeto MySQL.

Verificação adicional opt-in, somente leitura, contra o MySQL já preparado:

```powershell
$env:MYSQL_REFERENCE_TESTS = 'true'
$env:DB_USER = 'root'
# Definir DB_PASSWORD no ambiente de forma local; não salvar no repositório.
.\mvnw.cmd test
Remove-Item Env:MYSQL_REFERENCE_TESTS
Remove-Item Env:DB_PASSWORD
```

`DB_URL` também é aceito. O perfil mysql impede inicialização dos SQLs de teste,
valida o schema real e consulta todas as UFs/endpoints. Não ativar globalmente o
perfil mysql na suíte: apenas MySqlReferenceDataTests o usa, com opt-in por variável.
O teste MySQL é ignorado na execução padrão quando essa variável não existe.

Na raiz:

```powershell
python -m unittest discover -s database/scripts -p 'test_*.py' -v
git diff --check
```

Cobertura: contexto Spring, regressão de estados, filtros UF/ID, normalização,
ordenação, DTO com estado, categorias ativas/inativas, vazios, parâmetros
inválidos, restrição de escrita e integridade da FK. Python verifica geração
offline idêntica, rejeição de snapshot corrompido e fidelidade do DDL de teste.
Resultados executados nesta entrega constam no relatório final abaixo.

## Compatibilidade futura com o frontend

- `pages/cities.html` e `js/pages/cities.js` filtram cards presentes no DOM e
  representam cidades **com instituições cadastradas**. `/api/cities` é um catálogo
  territorial e não informa instituições, contagens nem coordenadas. Esse recorte
  público e os marcadores do mapa dependem do próximo domínio, sem inventar dados.
- `pages/unit-form.html`/`js/pages/unit-form.js` e
  `pages/registration-request.html`/`js/pages/registration-request.js` usam cidade
  e UF como texto. A integração deverá selecionar estado, buscar cidades da UF e
  guardar `city.id`, limpando a cidade ao trocar de estado; não enviar nome como FK.
- O cadastro de instituição usa `category` textual: deverá selecionar a taxonomia
  de `/api/institution-categories` e enviar o ID no futuro contrato de escrita.
- O formulário de unidade envia códigos fixos FOOD/CLOTHING/HYGIENE/CHILDREN/
  ELDERLY/ANIMALS/OTHER em `serviceAreas`. Eles não são IDs do banco e misturam
  doações e públicos atendidos. As opções deverão vir de `/api/service-areas`;
  itens de doação usam `/api/need-categories`.
- O editor de necessidades ainda não tem seletor de categoria: a integração
  futura precisará preservar o ID da categoria, além do título e descrição.
- Não foi adicionado CORS irrestrito: usar mesma origem/proxy, ou definir a origem
  autorizada quando houver integração e servidor de frontend definidos.

Nenhum arquivo de frontend foi alterado. Não foram criados dados operacionais,
instituições, usuários ou unidades fictícias, nem listas de municípios no Java/JS.

## Arquivos da entrega

Criados: cinco arquivos (entity, repository, dto, service, controller) em cada um
 dos pacotes `location/city`, `institution/category`, `servicearea` e `need/category`
 sob `backend/src/main/java/com/bemfeito/api/` (20 arquivos Java); os testes
 `ReferenceDataApiTests.java` e `MySqlReferenceDataTests.java`; quatro recursos em
 `backend/src/test/resources/` (`application.properties`, `application-mysql.properties`,
 `reference-schema.sql`, `reference-fixtures.sql`); `database/cities.sql`;
 `database/ibge/municipalities.csv` e `manifest.json`; `database/scripts/prepare_cities.py`,
 `test_prepare_cities.py` e `.gitignore`; este documento.

Modificados: `backend/pom.xml`, `backend/mvnw.cmd`,
`backend/src/main/java/com/bemfeito/api/location/state/entity/State.java`,
`database/reference-data.sql` (apenas comentário de orientação) e `README.md`.

Nenhum commit foi realizado.

## Resultado da validação em 27/10/2026

- Maven Wrapper: `test` concluído com BUILD SUCCESS; 11 testes, zero falhas,
  zero erros e zero ignorados na execução com MYSQL_REFERENCE_TESTS=true.
- Contextos H2 e MySQL iniciados; Hibernate validou os cinco mapeamentos e
  Spring Data registrou os cinco repositories sem queries inválidas.
- Três testes Python aprovados, inclusive reprodução byte a byte do SQL offline.
- MySQL: 5.571 cidades, 27 UFs, zero órfãos. MG: 853; MT: 142; SP: 645.
- Carga reexecutada com sucesso; quantidade e IDs existentes preservados
  (soma dos IDs = 15.520.806 antes e depois).
- `git diff --check`: aprovado, sem erros de whitespace.
- Nenhum dado fictício foi criado. Fixtures efêmeras usam localidades reais e
  taxonomias do projeto. Nenhum commit foi realizado.
