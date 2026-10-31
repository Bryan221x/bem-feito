# Integração frontend/API — Parte 5

O frontend usa dados persistidos pelas APIs Spring. Não há catálogo de demonstração,
conta simulada ou contexto institucional em localStorage/sessionStorage.
Cadastros são feitos por solicitação, aprovação administrativa, convite, ativação
e login. Os testes escrevem exclusivamente em bancos H2 efêmeros.

## Execução local

Prepare o MySQL conforme as Partes 1–4 e forneça DB_PASSWORD no ambiente do processo.
Na pasta `backend`, execute `.\mvnw.cmd spring-boot:run` no PowerShell. Sirva a raiz do frontend na porta 5500, por exemplo
com `python -m http.server 5500`. Use o mesmo hostname no frontend e backend:
`127.0.0.1` ou `localhost`. As origens permitidas são explícitas em
`APP_CORS_ALLOWED_ORIGINS`; a sessão é HttpOnly e as mutações exigem CSRF.

`api-client.js` usa o hostname atual com porta 8080 no desenvolvimento local e
a mesma origem em outros ambientes. Um `<meta name="bemfeito-api-base"
content="https://api.exemplo.org">` pode configurar outro endereço. CORS, cookies
e HTTPS devem ser configurados de acordo com a topologia real; não usar wildcard
com credenciais. O cliente inclui cookies e usa ApiError; FormData mantém o
Content-Type/boundary definido pelo navegador. Não repete mutações automaticamente.

## Serviços e páginas

| Serviço | Responsabilidade |
| --- | --- |
| api-client | Base URL, JSON, multipart, CSRF, cookies, erros, URLs de mídia e ViaCEP |
| auth-service / auth-guard | Login, sessão, logout, memberships e proteção de páginas |
| public-catalog-service | Cidades disponíveis, instituições paginadas, detalhe e pontos do mapa |
| institution-service | Gestão, PATCH parcial, contatos, fotos e memberships |
| unit-service | Unidade, contatos via institution-service, áreas, necessidades e horários |
| admin-service | Resumo agregado e lista administrativa paginada |
| request-service | Solicitações, revisão, convite e ativação já existentes |
| map-service | Leaflet/OSM com coordenadas reais e popups montados com textContent |
| components/api-ui | Paginação, bloqueio durante salvamento, mensagens e contexto nos links |

Home, Sobre e Como funciona mantêm o conteúdo editorial e os CTAs. Não usam
contagens inventadas. Cidades consulta apenas `/api/cities/available`; a busca
filtra os nomes retornados. O link leva `cityId` a Instituições. A listagem também
funciona sem filtro, usa páginas de 20 e DTO leve com categoria, descrição curta
e localização. O detalhe `institution.html?id=...` aceita também o antigo
`institutionId` e distingue ausência de recurso de falha de serviço.

O detalhe público separa contatos gerais e os de cada unidade. Renderiza 0, 1 ou
várias unidades com endereço, áreas, horários e necessidades ACTIVE. Não publica
CNPJ/razão social, contatos privados ou necessidades FULFILLED/INACTIVE. EMAIL,
PHONE e WHATSAPP recebem links validados. Dias sem horário são identificados;
não há horário presumido. Objetivo, descrição e história vazios não geram conteúdo
fictício. O CTA de história só aparece com texto. A galeria ordenada admite zero,
uma ou várias fotos, alt, legenda, miniaturas, contador, teclado, Escape e toque.

A seleção usa `/api/me/institutions` e mantém `institutionId` nos links.
`my-institution.html?institutionId=...` carrega gestão, categoria e vínculos reais;
trocar a instituição navega para novo contexto. Dados gerais e Sobre enviam
somente os campos modificados: omissão preserva e null limpa campos opcionais.
Contatos e fotos têm operações próprias. A UI restringe os controles gerais e
legais conforme permissões; a autorização efetiva permanece no backend.

`unit-form.html?institutionId=...` cria uma unidade; com `unitId`, carrega a unidade
existente e verifica seu proprietário. Endereço usa UF/cityId da base. Coordenadas
são opcionais e editáveis; não são deduzidas do CEP. Contatos, áreas, necessidades
e os sete dias da semana têm operações independentes. Antes de criar uma unidade,
contatos/necessidades são rascunhos; após criar, cada resposta substitui seu
rascunho pelo ID persistido. Uma falha parcial mantém os IDs já salvos para evitar
duplicação na tentativa seguinte. Não há transação distribuída entre esses HTTPs;
operações concluídas permanecem gravadas. A remoção de necessidade respeita o 409
do domínio quando o status ainda não permite exclusão. A interface orienta editar,
selecionar INACTIVE e salvar antes de remover um registro persistido; rascunhos são
removidos somente em memória. Em conflito 409, o item é preservado e o usuário deve
recarregar e conferir o status, sem repetição automática do DELETE.

O formulário consulta a conta e as permissões da instituição antes de habilitar
edição. OWNER/ADMIN podem alterar todos os campos da unidade; MANAGER não altera
documento/status e cria somente ACTIVE sem documento; EDITOR altera apenas a
descrição nos dados da unidade. Contatos, necessidades, horários e áreas mantêm
suas operações próprias. Campos proibidos ficam desabilitados e são excluídos do
PATCH mesmo se seu valor no DOM mudar. Omissão preserva dados e null limpa campos
opcionais. A matriz e a validação do backend permanecem inalteradas.

Na página de cidades, ausência de coordenadas, falha da API de localizações e
indisponibilidade do mapa têm mensagens distintas. Falha do Leaflet, da inicialização
ou dos tiles não impede navegar pela lista de cidades. Só coordenadas válidas
produzem marcadores; o enquadramento do Brasil sem dados não representa instituição.

O dashboard combina o summary das solicitações com contagens do banco, sem baixar
listas para contar. Instituições administrativas têm busca por nome/CNPJ, filtro
de status, paginação, data de criação, contagem de unidades e vínculos ativos.
O detalhe inclui dados legais, vínculos, unidades e links de gestão. Mudanças para
INACTIVE/SUSPENDED pedem confirmação. Somente PLATFORM_ADMIN altera status global.
Atividades recentes mantém um estado vazio explícito: não existe audit log unificado.

## Endpoints

| Endpoint | Uso |
| --- | --- |
| GET /api/cities/available | Cidades com unidades publicáveis |
| GET /api/institutions?cityId=&page=&size= | Catálogo público leve |
| GET /api/institutions/{id} | Detalhe público agregado |
| GET /api/units/map?cityId=&page=&size= | **Novo:** projeção paginada de unidades ACTIVE de instituições ACTIVE com coordenadas |
| GET /api/management/institutions/{id} | Detalhe protegido existente |
| PATCH /api/institutions/{id} | Campos alterados; política por role/campo |
| /api/institutions/{id}/contacts[/{contactId}] | CRUD de contatos |
| /api/institutions/{id}/history-images[/{imageId}] | Metadados, ordem e exclusão |
| POST /api/institutions/{id}/history-images/upload | **Novo:** arquivo multipart e metadados |
| GET /api/history-media/{institutionId}/{key} | **Novo:** mídia controlada |
| GET /api/management/units/{id} | Detalhe protegido da unidade |
| POST /api/institutions/{id}/units | Criação da unidade |
| PATCH /api/units/{id} | Alterações parciais |
| /api/units/{id}/contacts[/{contactId}] | CRUD de contatos da unidade |
| PUT /api/units/{id}/service-areas | Associações por IDs reais |
| PUT /api/units/{id}/opening-hours/{day} | Dia 1 (segunda) até 7 (domingo) |
| /api/units/{id}/needs[/{needId}] | CRUD, FULFILLED e INACTIVE |
| GET /api/admin/summary | **Novo:** instituições, unidades, vínculos ativos e distribuição por cidade |
| GET /api/admin/institutions?q=&status=&page=&size= | **Novo:** lista administrativa e counts |
| /api/institutions/{id}/members[/{membershipId}] | Consulta e PATCH de vínculos existente |

Referências: `/api/states`, `/api/cities?state=UF`, `/api/institution-categories`,
`/api/service-areas`, `/api/need-categories`. Novos DTOs: `UnitMapPoint`,
`AdminOutput.InstitutionRow`, `AdminOutput.CityCount`, `AdminOutput.Summary`.
`InstitutionSummary` ganhou `location`; o upload reutiliza `HistoryImageResponse`
(campos `url`, `alt`, `caption`, `order`). Necessidades recebem `title` na escrita
e devolvem `name` no DTO existente. As listas usam projeções/SQL agregado, sem
carregar coleções completas ou executar uma query por registro.

## Upload e ciclo de vida

`ImageStorage` abstrai gravar, ler e remover. `LocalImageStorage` é a implementação
local. `APP_UPLOAD_DIR` define a raiz; o padrão é `${user.home}/.bem-feito/uploads`,
fora dos assets versionados. `APP_UPLOAD_MAX_BYTES` limita o arquivo (padrão 5 MiB);
`APP_UPLOAD_REQUEST_MAX_BYTES` limita o multipart inteiro (padrão 6 MiB, deve ser
maior que o arquivo para comportar campos e cabeçalhos).

Campos multipart: `file`, `altText` obrigatório, `caption` opcional, `displayOrder`
não negativo. Exige sessão, CSRF e autorização institucional ou de administrador.
ImageIO decodifica o conteúdo JPEG/PNG/WebP (WebP via TwelveMonkeys), limita a
16 milhões de pixels e recodifica para PNG. Extensão/MIME declarados não substituem
a validação. SVG/GIF/executáveis e imagens inválidas são rejeitados. O nome físico
é UUID; nomes de usuário nunca compõem caminhos. A resolução restringe proprietário,
formato de chave, traversal e links simbólicos.

O recurso só é servido se houver registro correspondente da mesma instituição.
Instituições não ativas exigem autorização para leitura da mídia. Retorna PNG,
nosniff e no-store. Rollback do registro remove o arquivo recém-gravado; remoção ou
substituição apaga o arquivo antigo após commit, limitado ao proprietário. URLs
externas existentes não são apagadas. Erros de remoção geram aviso sem expor
caminhos/segredos; podem deixar arquivo órfão sem acesso público.

Filesystem e banco não formam uma transação atômica diante de queda do processo.
Produção precisa volume persistente, backup coordenado, reconciliação de órfãos e
armazenamento compartilhado se houver várias instâncias. A interface permite
substituição por storage persistente remoto sem trocar os controllers.

## ViaCEP, mapa e estados

A consulta opcional usa [ViaCEP](https://viacep.com.br/), sem cookies da aplicação,
com timeout e CEP de oito dígitos. Mapeia UF e nome de município para o cityId da
base local; nunca usa o código IBGE externo como PK. Falhas não bloqueiam entrada
manual. Respostas obsoletas não sobrescrevem outra seleção de UF/CEP. Usuário deve
conferir rua/bairro e complementar número. O serviço externo recebe o CEP consultado.

Mapa usa Leaflet/OpenStreetMap existente. Cada unidade com coordenadas válidas
gera um ponto. A tela de cidades carrega 100 pontos por página com ação para mais;
sem coordenadas não há marcador inventado. Geocodificação automática fica para
uma evolução com provedor e política definidos.

Listas têm loading, empty, error, retry e paginação. Durante salvamento, controles
ficam bloqueados e são liberados em finally. Erros preservam campos digitados.
401 aciona login nas páginas protegidas; 403 informa acesso negado/CSRF expirado;
404 informa recurso ausente; 409 apresenta conflito; 400/422 mostram validação.
Dados de banco são montados com createElement/textContent. Os innerHTML restantes
contêm apenas SVG estático. URLs de sites/mídia aceitam HTTP(S) sem credenciais;
nenhuma gestão recebeu permitAll nem teve CSRF desabilitado.

## Validação e reprodução

Na raiz:

```powershell
node --test tests/frontend/*.test.mjs
python -m unittest discover -s database/scripts -p 'test_*.py' -v
git diff --check
```

Em `backend`: `.\mvnw.cmd clean test`. Com `MYSQL_REFERENCE_TESTS=true` e DB_PASSWORD
no ambiente, inclui Hibernate validate e consultas reais MySQL. A validação compara
estado antes/depois e não cria instituições ou contas de demonstração no MySQL.

Teste de navegador opcional: compile primeiro com `.\mvnw.cmd test-compile
dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt'`. Na raiz,
configure `BROWSER_INTEGRATION_TESTS=true`, `PLAYWRIGHT_MODULE` (módulo Playwright
disponível no ambiente) e `BROWSER_EXECUTABLE` (Chrome/Edge), depois execute
`node --test tests/frontend/browser-flow.test.mjs`. Não instala dependência de
runtime no frontend. `BrowserTestServer` reside apenas em src/test, força H2 em
memória e porta 8081. O teste usa portas 5501/8081, gera senha em memória, cria
contas pelo fluxo real e encerra os processos. Relatório/imagens ficam em
`backend/target/browser-results` (não versionado).

Não há alteração de schema nem nova migração nesta Parte. Recuperação de senha,
OAuth, pagamentos e deploy final continuam fora do escopo. SMTP é a integração
opcional da Parte 4; audit log e geocodificação automática são evoluções futuras.
