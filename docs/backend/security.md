# Parte 3 — Usuários, autenticação e autorização

## Estado e escopo

Base analisada: `main`, commit `c21fad1`, sem alterações locais. Spring Boot
4.1.1 e Java 25 preservados. Foram lidos o schema, modelo de dados, documentação
e services/controllers da Parte 2, login/seleção, páginas institucionais e
administrativas, renderizadores, navegação principal e sidebars.

Esta Parte implementa User, InstitutionMembership, login e logout reais, sessão
HTTP, CSRF, CORS, autorização dos endpoints institucionais, bootstrap opcional e
integração do contexto autenticado no frontend. Não cria solicitações, convites,
aprovações, recuperação de senha, envio de e-mail ou dashboard administrativo real.
Não foi feito commit, deploy nem cadastro de usuários no MySQL real.

## Dependências e arquitetura

Adicionados `spring-boot-starter-security` e `spring-boot-starter-security-test`
(escopo test). As versões são gerenciadas pelo parent 4.1.1; o segundo starter
inclui spring-security-test e a integração de testes própria do Boot 4. Não há
JWT, OAuth externo, HTTP Basic, remember-me ou biblioteca extra de autenticação.

Controller → Service transacional → Repository → MySQL. AuthorizationService é
a política central, aplicada com method security nos services da Parte 2 e nos
novos services de membership. Classes de serviços institucionais usam denyAll por
padrão, com permissão explícita por método. As leituras públicas materializam
somente os DTOs públicos existentes. Os controllers não recebem roles do browser.

User e InstitutionMembership mapeiam exatamente users e institution_memberships,
incluindo timestamps e enums do schema. As associações são N:N por uma entidade
própria, sem user.institutionId. E-mails são normalizados com trim/lowercase antes
da consulta e da persistência; UNIQUE do MySQL continua sendo a autoridade final.
Nenhuma alteração em database/schema.sql. ddl-auto=validate foi mantido.
User Entity e UserPrincipal não são respostas de endpoints; UserResponse contém
somente id, name, email, platformRole e status. passwordHash nunca aparece em DTO.

## Login e sessão

POST /api/auth/login usa o filtro e AuthenticationManager nativos do Spring
Security. Entrada é application/x-www-form-urlencoded com email e password.
O UserDetailsService consulta o banco, valida a existência de hash BCrypt e
habilita somente status ACTIVE. INVITED, INACTIVE e SUSPENDED não autenticam.
BCryptPasswordEncoder usa custo 12. Hash é retirado do principal após autenticar,
e não fica armazenado na sessão. Senhas acima de 72 bytes UTF-8 são rejeitadas
para evitar ambiguidade de truncamento do BCrypt.

Falhas de login sempre retornam 401 com mensagem genérica:

```json
{"timestamp":"...","status":401,"message":"E-mail ou senha inválidos.","fields":{}}
```

Não são revelados existência, suspensão ou erro específico da credencial.
Sucesso retorna 200:

```json
{"authenticated":true,"user":{"id":1,"name":"Nome","email":"conta@example.invalid","platformRole":"USER","status":"ACTIVE"}}
```

Os dados acima representam apenas o contrato, não uma conta persistida.
GET /api/auth/me retorna o objeto user diretamente; anônimo recebe JSON 401.
Não há formulário HTML ou redirect de login do Spring nas chamadas API.

A autenticação é armazenada no servidor em HttpSession; o navegador mantém
JSESSIONID em cookie HttpOnly, sem senha, hash ou token de autenticação acessível
a JavaScript. Native changeSessionId protege contra session fixation. A sessão
expira após 30 minutos sem atividade. A cada requisição de uma sessão real,
CurrentAccountFilter reconsulta o usuário, revoga a sessão de uma conta não ativa
e atualiza a role da plataforma. Memberships são consultados novamente nas decisões;
revogação de vínculo ou perda de privilégio não exige novo login para valer.

POST /api/auth/logout exige CSRF válido, invalida sessão, limpa contexto e remove
o cookie JSESSIONID; responde 200 `{"authenticated":false}` sem redirect HTML.
O logout nativo é idempotente: também pode encerrar um contexto já anônimo com
CSRF válido, sem revelar dados. A sessão antiga não autentica mais requisições.
Não existe POST /api/auth/register, criação pública de contas ou endpoints para
alterar platformRole de usuários comuns.

## CSRF e CORS

CSRF permanece habilitado para todos os métodos mutáveis, inclusive login e logout.
Usa o repositório de sessão e o tratamento XOR/BREACH nativos do Spring Security.
GET /api/auth/csrf é público e retorna token, headerName e parameterName.
O frontend envia o token recebido no header indicado (X-CSRF-TOKEN). O token fica
somente em memória e não é credencial de autenticação. Após login, o cliente
obtém novo token; após logout, descarta o anterior, e o próximo fluxo o requisita.
Token ausente/inválido/antigo retorna JSON 403. GET/HEAD não alteram dados de negócio;
a criação técnica de sessão/token ocorre pelo mecanismo nativo de CSRF.

CORS usa CorsFilter nativo registrado explicitamente uma única vez na cadeia
Spring Security, antes de CSRF e da autenticação. O registro servlet separado
é desabilitado para não duplicar filtros. O processor mantém as verificações
nativas e formata rejeições como ApiError JSON 403. Preflight OPTIONS permitido
não exige sessão. Credentials=true, métodos e headers explícitos. Não há wildcard
com credenciais nem default que permita qualquer origem. Origens configuradas
com wildcard, credenciais, path, query ou fragment são rejeitadas na inicialização.

Variável APP_CORS_ALLOWED_ORIGINS aceita origens exatas separadas por vírgula.
Defaults de desenvolvimento: http://127.0.0.1:5500 e http://localhost:5500.
Não se deve misturar localhost e 127.0.0.1 entre frontend/backend: embora as duas
origens possam estar autorizadas por CORS, os cookies são avaliados por site/host.
Use localhost nos dois serviços ou 127.0.0.1 nos dois.

## Matriz de permissões

| Operação | PLATFORM_ADMIN | OWNER ativo | MANAGER ativo | EDITOR ativo |
| --- | --- | --- | --- | --- |
| Consultas públicas | Sim | Sim | Sim | Sim |
| Visão de gestão de instituição vinculada e dados privados | Qualquer instituição | Sim | Sim | Sim |
| Listar todas as instituições na gestão / api/admin/** | Sim | Não | Não | Não |
| Criar instituição diretamente | Sim | Não | Não | Não |
| Alterar institution.status | Sim | Não | Não | Não |
| Alterar displayName/categoryId | Sim | Sim | Sim | Não |
| Alterar razão social/CNPJ da instituição | Sim | Sim | Não | Não |
| Objetivo, descrição, história, website | Sim | Sim | Sim | Sim |
| Contatos, referências de imagens, horários, áreas e necessidades | Sim | Sim | Sim | Sim |
| Criar unidade ativa | Sim | Sim | Sim, sem documento jurídico inicial | Não |
| Editar nome/endereço/coordenadas de unidade | Sim | Sim | Sim | Não |
| Alterar documento ou status de unidade | Sim | Sim | Não | Não |
| Editar descrição da unidade | Sim | Sim | Sim | Sim |
| Consultar/administrar memberships | Sim | Sim | Não | Não |

Todas as permissões institucionais exigem vínculo ACTIVE com a instituição real.
PENDING e REVOKED não concedem acesso. PLATFORM_ADMIN opera sem precisar de vínculo.
EDITOR não cria/desativa unidade, e MANAGER não altera identificação jurídica nem
status da unidade; esta é a decisão conservadora adotada para distinguir editar de
desativar. institution.status é sempre controlado pela plataforma.

PATCH analisa campos presentes, preservando a semântica de null da Parte 2.
Um PATCH institucional comum contendo status é negado, mesmo que tente reenviar
o mesmo valor. PUT permite conservar valores restritos já existentes, mas nega
qualquer alteração neles. EDITOR só pode usar PUT de unidade se todos os campos
restritos estiverem preservados; somente a descrição pode diferir. Não se altera
a vinculação de unidade à instituição via payload.

## Recursos filhos e memberships

InstitutionAuthorizationService resolve Unit → Institution pelo banco. Contatos,
necessidades e imagens exigem também que o registro filho pertença ao pai indicado
na URL. Informar o ID de uma instituição própria junto de um filho estrangeiro não
concede acesso. A camada de domínio mantém suas verificações de integridade.
Ausência de permissão retorna 403, independentemente de IDs fornecidos pelo cliente.

GET /api/me/institutions retorna exclusivamente os memberships ACTIVE da conta
atual, com institutionId, displayName, status institucional, membershipRole,
membershipStatus e permissions calculadas no servidor. ADMIN tem gestão global
separada, e não recebe memberships inventados nessa lista.

GET /api/institutions/{institutionId}/members é restrito a OWNER/ADMIN.
PATCH /api/institutions/{institutionId}/members/{id} altera somente role e/ou status.
Campos ausentes permanecem; null explícito é inválido. Não cria usuário, convite
ou membership novo. Ativar vínculo exige conta ACTIVE. Mudanças bloqueiam a linha
da instituição (PESSIMISTIC_WRITE), revalidam autorização após adquirir o lock e
contam os OWNERs com vínculo ACTIVE, conta ACTIVE e hash utilizável pelo login. OWNER não pode rebaixar/revogar
o último OWNER nessas condições (409). Vínculos de contas INVITED, INACTIVE ou
SUSPENDED, ou com hash ausente/inválido, não contam como substitutos para essa proteção.
Pode promover outro responsável e então deixar o vínculo. ADMIN pode resolver
exceções administrativas. A proteção usa lock de banco, não contador do frontend.

## Bootstrap do primeiro administrador

Bootstrap desabilitado por padrão. Não foi habilitado nem executado contra o MySQL
nesta entrega. Só é executado no startup com APP_BOOTSTRAP_ADMIN_ENABLED=true.
Exige as três variáveis de identidade/credencial, sem defaults pessoais:

- APP_BOOTSTRAP_ADMIN_NAME;
- APP_BOOTSTRAP_ADMIN_EMAIL;
- APP_BOOTSTRAP_ADMIN_PASSWORD.

Nome/e-mail são validados e e-mail normalizado. Senha exige mínimo de 12 caracteres
e máximo de 72 bytes UTF-8. Apenas seu BCrypt é persistido. A conta criada é ACTIVE,
PLATFORM_ADMIN, com emailVerifiedAt preenchido. Se o administrador correspondente
já existe, nada é alterado, inclusive senha; uma conta USER existente nunca é
promovida. Se outro administrador já existe, o bootstrap não cria um segundo.
Executar a inicialização em uma instância controlada e desabilitar o bootstrap
e remover as variáveis de senha após o primeiro uso. Não colocar credenciais em
SQL, arquivos versionados ou exemplos de comandos com senhas reais.

## Configuração local e produção

| Variável | Default/efeito |
| --- | --- |
| DB_URL / DB_USER / DB_PASSWORD | Configuração MySQL existente; senha só no ambiente |
| APP_CORS_ALLOWED_ORIGINS | As duas origens Live Server locais, separadas por vírgula |
| APP_SESSION_COOKIE_SECURE | false em HTTP local; definir true em produção HTTPS |
| APP_SESSION_COOKIE_SAME_SITE | lax; none somente quando necessário, sempre com HTTPS/Secure |
| APP_BOOTSTRAP_ADMIN_ENABLED | false |
| APP_BOOTSTRAP_ADMIN_NAME/EMAIL/PASSWORD | Ausentes; exigidos somente para bootstrap explícito |
| server.servlet.session.timeout | 30m (propriedade Spring externalizável) |
| MYSQL_REFERENCE_TESTS | Opt-in dos testes somente leitura no banco real |

Produção deve usar HTTPS, cookie Secure, origens exatas do frontend e acesso
restrito ao MySQL. SameSite=Lax é apropriado ao desenvolvimento entre portas do
mesmo host. Se frontend/API forem sites distintos, configurar SameSite=None e
Secure=true conscientemente; navegadores ainda podem restringir cookies terceiros.
As sessões são server-side na instância da aplicação; não há persistência de
sessão entre reinícios nem infraestrutura distribuída nesta Parte. Se houver
várias instâncias, planejar afinidade/armazenamento compartilhado antes de escalar.

Frontend lê opcionalmente `<meta name="bemfeito-api-base" content="...">`.
Sem meta, em localhost/127.0.0.1 usa porta 8080 com o mesmo hostname/protocolo;
em outros hosts usa mesma origem. Meta contém URL pública, nunca segredo.
Live Server padrão usa 5500 e deve ser iniciado no mesmo hostname que a API.

## Integração do frontend

api-client centraliza fetch, credentials:include, JSON/form, CSRF em memória e
erros 401/403. Não repete mutações automaticamente. auth-service expõe login,
logout, usuário atual, CSRF e instituições da conta. A senha é retirada do campo
após envio e do objeto de formulário ao concluir; não há storage de autenticação.

Login preserva layout, toggle de senha, validações e mensagens. Bloqueia o botão
durante envio, autentica realmente e consulta me. ADMIN segue para
admin-dashboard.html; USER para institution-selection.html. Quem já está logado
não precisa preencher formulário novamente. Falhas de login usam mensagem genérica.

Seleção carrega somente /api/me/institutions e reutiliza o renderer existente,
com id adaptado de institutionId. Sem vínculos, mantém o empty state e CTAs de
cadastro/acesso sem criar cards. Solicitações continuam sem backend nesta Parte.

Guardas são reutilizadas por institution-selection, my-institution e unit-form;
as três páginas admin exigem PLATFORM_ADMIN. O HTML inicia oculto até a checagem,
sem mostrar conteúdo administrativo antes da decisão. Páginas restauradas pelo
cache de navegação do browser revalidam a sessão antes de revelar conteúdo. Falha de rede mantém uma
mensagem de erro em vez do conteúdo protegido. Há checagem de contexto institucional
e permissão de criar unidade para UX; nenhuma query string é autoridade no backend.

Todos os sidebars receberam Sair, usando estilos existentes com ajuste mínimo
para o botão. O logout só redireciona após invalidar a sessão no servidor. Em falha,
mostra erro e permite tentar de novo. Não foram integrados CRUDs completos das
outras telas nem dashboards; elas continuam preparadas para as próximas partes.

## Testes e auditoria

Testes existentes usam principal de teste ADMIN e CSRF do spring-security-test
para preservar as verificações de domínio, sem desligar a segurança. SecurityApiTests
realiza login real e acompanha HttpSession, senha BCrypt, fixation, rotação CSRF,
logout, status de contas, revogação e autorização transitiva. Dados de conta são
criados somente em H2 com propriedades explícitas por classe e rollback; as senhas
são geradas em memória. AdminBootstrapTests testa bootstrap somente no H2.
A rota /api/admin/security-probe existe exclusivamente no contexto de teste.

Comandos, na pasta backend (Java 25):

```powershell
.\mvnw.cmd clean test
```

Opcionalmente definir MYSQL_REFERENCE_TESTS=true e DB_PASSWORD no ambiente local
para validar o schema real. Não ativar o perfil mysql globalmente: os testes
operacionais permanecem no H2. MySqlReferenceDataTests só consulta tabelas e APIs,
incluindo contagens de users e institution_memberships antes/depois. Nunca habilitar
bootstrap de produção durante testes. O DDL de teste usa as 15 tabelas mapeadas
extraídas do schema original; a verificação Python foi ampliada para elas.

Na raiz:

```powershell
python -m unittest discover -s database/scripts -p 'test_*.py' -v
node --test tests/frontend/auth.test.mjs
# node --check para cada módulo JS alterado/novo
git diff --check
```

Testes frontend usam apenas node:test e mocks de transporte/DOM, sem framework
instalado. Não substituem a autoridade do backend nem simulam autenticação em
produção. Verificam credentials, CSRF, login, erros, guardas e logout aguardando o
servidor. Não há dados ou credenciais de demonstração no MySQL real.

Referências oficiais: [form login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html),
[CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html) e
[módulos de testes do Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).

## Classificação completa dos endpoints

| Método | Rota | Classificação |
| --- | --- | --- |
| QUALQUER | /api/admin/** | PLATFORM_ADMIN_REQUIRED, inclusive rotas futuras |
| GET | /api/auth/csrf | PUBLIC |
| POST | /api/auth/login | PUBLIC, credenciais e CSRF obrigatórios |
| POST | /api/auth/logout | PUBLIC (logout idempotente; CSRF obrigatório) |
| GET | /api/auth/me | AUTHENTICATED |
| GET | /api/cities | PUBLIC |
| GET | /api/cities/available | PUBLIC |
| GET | /api/health | PUBLIC |
| GET | /api/institution-categories | PUBLIC |
| GET | /api/institutions | PUBLIC |
| POST | /api/institutions | PLATFORM_ADMIN_REQUIRED |
| GET | /api/institutions/{id} | PUBLIC |
| PATCH | /api/institutions/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/institutions/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/institutions/{id}/units | PUBLIC |
| POST | /api/institutions/{institutionId}/contacts | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| DELETE | /api/institutions/{institutionId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PATCH | /api/institutions/{institutionId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/institutions/{institutionId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| POST | /api/institutions/{institutionId}/history-images | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| DELETE | /api/institutions/{institutionId}/history-images/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PATCH | /api/institutions/{institutionId}/history-images/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/institutions/{institutionId}/history-images/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/institutions/{institutionId}/members | OWNER_REQUIRED (ou PLATFORM_ADMIN) |
| PATCH | /api/institutions/{institutionId}/members/{id} | OWNER_REQUIRED (ou PLATFORM_ADMIN) |
| POST | /api/institutions/{institutionId}/units | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/institutions | PLATFORM_ADMIN_REQUIRED |
| GET | /api/management/institutions/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/institutions/{institutionId}/contacts | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/institutions/{institutionId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/institutions/{institutionId}/history-images | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/institutions/{institutionId}/history-images/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/contacts | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/needs | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/needs/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/opening-hours | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/management/units/{unitId}/service-areas | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| GET | /api/me/institutions | AUTHENTICATED |
| GET | /api/need-categories | PUBLIC |
| GET | /api/service-areas | PUBLIC |
| GET | /api/states | PUBLIC |
| GET | /api/units/{id} | PUBLIC |
| PATCH | /api/units/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/units/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| POST | /api/units/{unitId}/contacts | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| DELETE | /api/units/{unitId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PATCH | /api/units/{unitId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/units/{unitId}/contacts/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| POST | /api/units/{unitId}/needs | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| DELETE | /api/units/{unitId}/needs/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PATCH | /api/units/{unitId}/needs/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/units/{unitId}/needs/{id} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/units/{unitId}/opening-hours/{day} | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |
| PUT | /api/units/{unitId}/service-areas | MEMBERSHIP_REQUIRED (matriz por campo/operação; ADMIN dispensa vínculo) |

Toda rota de escrita institucional está na cadeia autenticada e tem proteção
adicional no service. O único POST anônimo de autenticação é o login (CSRF e
credenciais); logout é idempotente e protegido contra CSRF. Não há permitAll de
POST/PUT/PATCH/DELETE institucional. Desconhecidos não recebem acesso de USER.

## Resultado final e inventário

Validação em 27/10/2026 (America/Sao_Paulo), HEAD c21fad1:

- `mvnw.cmd clean test`, com MYSQL_REFERENCE_TESTS=true: **BUILD SUCCESS** às 22:16.
- **74 testes Java**, zero falhas, zero erros e zero ignorados: os 41 anteriores,
  28 testes de segurança/autorização e cinco de bootstrap.
- Hibernate validou **15 tabelas** no MySQL real e no H2. Repositories inicializados.
- **Três testes Python aprovados**, incluindo verificação do DDL das 15 tabelas.
- **12 testes frontend aprovados** com node:test, sem instalação de framework.
- **node --check: sete módulos aprovados**, incluindo o arquivo de testes mjs.
- `git diff --check`: aprovado; arquivos novos também verificados em UTF-8/whitespace.
- Auditoria: nenhum JWT/storage de autenticação, CSRF desabilitado, wildcard CORS
  com credenciais, endpoint mutável institucional público, hash em DTO, credencial
  real, token de autenticação hardcoded ou logging de senhas foi encontrado.
- Os permitAll são somente consultas públicas da Parte 1/2 e o endpoint CSRF;
  login usa o processamento nativo de formulário protegido por CSRF.
- Consulta final do MySQL: **users=0 e institution_memberships=0**; as oito tabelas
  institucionais também continuam vazias. Bootstrap real não foi habilitado.
- Nenhum schema de produção alterado, senha real gravada nos arquivos candidatos,
  usuário fictício persistido no MySQL, deploy ou commit realizado.

Parte 4: solicitações de cadastro/acesso, aprovação/rejeição, criação/ativação
por convite, tokens de convite, envio de e-mail e recuperação de senha. A integração
completa dos CRUDs institucionais e dados reais de dashboards permanece fora desta
Parte. Nenhum mecanismo fictício foi criado para antecipar esses fluxos.


### Arquivos criados (31)

- `backend/src/main/java/com/bemfeito/api/institution/membership/controller/InstitutionMembershipController.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MembershipPatch.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MembershipResponse.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MyInstitutionResponse.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/entity/InstitutionMembership.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/entity/MembershipRole.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/entity/MembershipStatus.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/repository/InstitutionMembershipRepository.java`
- `backend/src/main/java/com/bemfeito/api/institution/membership/service/InstitutionMembershipService.java`
- `backend/src/main/java/com/bemfeito/api/security/AuthController.java`
- `backend/src/main/java/com/bemfeito/api/security/CurrentAccountFilter.java`
- `backend/src/main/java/com/bemfeito/api/security/InstitutionAuthorizationService.java`
- `backend/src/main/java/com/bemfeito/api/security/SecurityConfig.java`
- `backend/src/main/java/com/bemfeito/api/security/SecurityJson.java`
- `backend/src/main/java/com/bemfeito/api/security/UserPrincipal.java`
- `backend/src/main/java/com/bemfeito/api/security/bootstrap/AdminBootstrapRunner.java`
- `backend/src/main/java/com/bemfeito/api/security/bootstrap/AdminBootstrapService.java`
- `backend/src/main/java/com/bemfeito/api/user/dto/UserResponse.java`
- `backend/src/main/java/com/bemfeito/api/user/entity/PlatformRole.java`
- `backend/src/main/java/com/bemfeito/api/user/entity/User.java`
- `backend/src/main/java/com/bemfeito/api/user/entity/UserStatus.java`
- `backend/src/main/java/com/bemfeito/api/user/repository/UserRepository.java`
- `backend/src/main/java/com/bemfeito/api/user/service/CurrentUserService.java`
- `backend/src/main/java/com/bemfeito/api/user/service/UserService.java`
- `backend/src/test/java/com/bemfeito/api/AdminBootstrapTests.java`
- `backend/src/test/java/com/bemfeito/api/SecurityApiTests.java`
- `docs/backend/security.md`
- `js/services/api-client.js`
- `js/services/auth-guard.js`
- `js/services/auth-service.js`
- `tests/frontend/auth.test.mjs`

### Arquivos alterados (32)

- `README.md`
- `backend/pom.xml`
- `backend/src/main/java/com/bemfeito/api/common/error/ApiExceptionHandler.java`
- `backend/src/main/java/com/bemfeito/api/institution/contact/service/InstitutionContactService.java`
- `backend/src/main/java/com/bemfeito/api/institution/history/service/InstitutionHistoryImageService.java`
- `backend/src/main/java/com/bemfeito/api/institution/repository/InstitutionRepository.java`
- `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java`
- `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionService.java`
- `backend/src/main/java/com/bemfeito/api/need/service/NeedService.java`
- `backend/src/main/java/com/bemfeito/api/unit/contact/service/UnitContactService.java`
- `backend/src/main/java/com/bemfeito/api/unit/hours/service/UnitOpeningHoursService.java`
- `backend/src/main/java/com/bemfeito/api/unit/service/UnitQueryService.java`
- `backend/src/main/java/com/bemfeito/api/unit/service/UnitService.java`
- `backend/src/main/java/com/bemfeito/api/unit/servicearea/service/UnitServiceAreaService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/com/bemfeito/api/InstitutionalApiTests.java`
- `backend/src/test/java/com/bemfeito/api/MySqlReferenceDataTests.java`
- `backend/src/test/java/com/bemfeito/api/ReferenceDataApiTests.java`
- `backend/src/test/resources/reference-schema.sql`
- `css/components/admin-layout.css`
- `database/scripts/test_prepare_cities.py`
- `docs/backend/institutional-core.md`
- `js/main.js`
- `js/pages/institution-login.js`
- `js/pages/institution-selection.js`
- `pages/admin-dashboard.html`
- `pages/admin-institutions.html`
- `pages/admin-requests.html`
- `pages/institution-login.html`
- `pages/institution-selection.html`
- `pages/my-institution.html`
- `pages/unit-form.html`

## Auditoria final pré-commit da Parte 3

### Problemas encontrados e correções pontuais

1. **Negociação do erro 403 com Accept: text/html.** O handler MVC não fixava o
   Content-Type da resposta. O filtro de segurança ainda devolvia JSON ao final,
   mas somente após uma falha do handler e um stack trace desnecessário no log.
   ApiExceptionHandler agora declara application/json explicitamente. O teste
   verifica a mensagem do próprio handler, além de status e Content-Type, para
   não aceitar silenciosamente o fallback como prova de funcionamento.
2. **Contagem do último OWNER.** O contador anterior incluía vínculos ACTIVE de
   contas INVITED, INACTIVE ou SUSPENDED. A contagem usada na proteção agora exige
   também User.status=ACTIVE e o mesmo critério de hash utilizável empregado no login.
   Contas ACTIVE sem hash ou com hash malformado também não contam como substitutos.
   O lock da instituição, a revalidação sob lock e a
   exceção controlada de PLATFORM_ADMIN foram preservados. Um teste cobre os três
   estados de conta, hash ausente/inválido e tanto revogação quanto rebaixamento
   do último OWNER. A consulta usa fetch do usuário em lote, sem N+1 por responsável.

Não foram criados endpoints, permissões, dependências, fluxos ou funcionalidades
novas nesta auditoria. A arquitetura e a matriz de papéis foram mantidas.
Acrescentados somente testes de regressão de PUT, erros JSON/Basic, hostname e
último OWNER; a implementação de produção teve apenas as duas correções acima.

### Todos os permitAll existentes e justificativas

Há **oito ocorrências explícitas** no código de produção: dois blocos HTTP e seis
anotações em métodos de consulta. Não há permitAll em escrita institucional,
consultas de gestão, memberships ou /api/admin/**.

| GET público no SecurityConfig | Justificativa |
| --- | --- |
| /api/health | Disponibilidade da API; não expõe credenciais ou dados internos |
| /api/states | Catálogo territorial usado pelo visitante/formulários |
| /api/cities | Catálogo territorial oficial, separado de instituições disponíveis |
| /api/cities/available | Descoberta pública de cidades com unidade/instituição ativa |
| /api/institution-categories | Taxonomia ativa de formulários públicos |
| /api/service-areas | Taxonomia ativa de formulários públicos |
| /api/need-categories | Taxonomia ativa de necessidades |
| /api/auth/csrf | Inicialização anônima da proteção CSRF, inclusive antes do login |
| /api/institutions | Catálogo público, DTO leve e filtro de visibilidade |
| /api/institutions/{id} | Detalhes apenas de instituição pública/ativa |
| /api/institutions/{id}/units | Unidades públicas de instituição ativa |
| /api/units/{id} | Detalhe de unidade e instituição ativas, sem documento privado |

Essas 12 rotas estão nos dois blocos limitados a HttpMethod.GET.
As seis anotações @PreAuthorize("permitAll()") são:

- InstitutionQueryService.list: projeção de listagem pública com status ACTIVE.
- InstitutionQueryService.publicDetail: aplica visible(id), DTO público e filhos filtrados.
- InstitutionQueryService.publicUnits: valida instituição visível e carrega apenas unidades públicas.
- UnitQueryService.publicDetail: exige unidade e instituição ACTIVE, DTO público.
- UnitQueryService.publicForInstitution: consulta com publicOnly=true e status dos pais.
- UnitQueryService.availableCities: somente cidades com unidade/instituição ACTIVE.

Login e logout são processados pelos filtros nativos antes da decisão de rota,
com CSRF obrigatório. Login exige credenciais; logout é idempotente, sem leitura
privada ou mutação institucional. Isso não é um permitAll de CRUD. Preflight é
tratado pelo CorsFilter apenas para a origem/método/headers configurados.

### Matriz confirmada diretamente nos métodos de autorização

| Operação | OWNER ACTIVE | MANAGER ACTIVE | EDITOR ACTIVE |
| --- | --- | --- | --- |
| Consultar gestão da instituição vinculada, inclusive contatos privados | Permite | Permite | Permite |
| Alterar displayName/categoryId | Permite | Permite | Nega |
| Alterar objective/description/history/website | Permite | Permite | Permite |
| Alterar legalName/cnpj da instituição | Permite | Nega | Nega |
| Alterar institution.status | Nega | Nega | Nega |
| Criar unidade | Permite, inclusive inativa | Somente ACTIVE e sem documento jurídico inicial | Nega |
| Editar nome, cidade/endereço e coordenadas da unidade | Permite | Permite | Nega |
| Alterar documento/status da unidade | Permite | Nega | Nega |
| Editar descrição da unidade | Permite | Permite | Permite |
| Criar/editar/remover contatos e referências de imagens | Permite | Permite | Permite |
| Cadastrar/atualizar os sete horários | Permite | Permite | Permite |
| Substituir/limpar associações de áreas | Permite | Permite | Permite |
| Criar/editar/status de necessidades | Permite | Permite | Permite |
| Remover necessidade | Apenas após INACTIVE | Apenas após INACTIVE | Apenas após INACTIVE |
| Consultar/alterar role e status de memberships | Permite, respeitando último OWNER | Nega | Nega |
| Criar instituição diretamente | Nega | Nega | Nega |
| Listagem global de gestão e /api/admin/** | Nega | Nega | Nega |

ADMIN tem o bypass administrativo explícito, originado da role carregada pelo
Spring a partir do banco e revalidada por requisição; não vem de header, payload,
query string ou DOM. Contas USER não o recebem. Todos os acessos institucionais
comuns exigem membership ACTIVE da conta autenticada; PENDING e REVOKED são negados.

PATCH rejeita a presença de campos restritos ao papel; PUT só admite manter seus
valores atuais. O teste adicional confirma que PUT não permite contornar as
restrições jurídicas, de status ou de edição de unidade do EDITOR. Não foi
identificado privilégio excessivo para MANAGER ou EDITOR nos demais campos.

### Autorização transitiva de cada recurso

| Recurso | Relação conferida no banco |
| --- | --- |
| Unit | findById(unitId) → unit.institution.id → membership da conta |
| UnitContact | Unit real → Institution; findByIdAndUnitId confirma que o contato pertence à Unit indicada |
| UnitOpeningHours | Unit real → Institution; upsert por unitId+dayOfWeek, sem ID de horário livre |
| UnitServiceArea | Unit real → Institution; associações são consultadas/substituídas para essa Unit |
| Need | Unit real → Institution; findByIdAndUnitId impede combinar necessidade estrangeira com Unit própria |
| InstitutionContact | membership na Institution e findByIdAndInstitutionId do contato persistido |
| InstitutionHistoryImage | membership na Institution e findByIdAndInstitutionId da imagem persistida |

Na criação de um filho ainda não há relacionamento persistido a consultar: o pai
é resolvido no banco, a permissão é conferida e o vínculo é definido pelo backend.
Nenhum payload pode definir outro pai. Para registros existentes, a combinação
pai/filho é conferida antes de acessar/alterar. ADMIN dispensa membership, mas a
regra de integridade dos services continua impedindo atualizar filho sob pai errado.

### Sessão, CSRF, frontend, hosts e bootstrap conferidos

- Sessão HTTP server-side; JSESSIONID HttpOnly, timeout 30m, changeSessionId no login.
  Secure=false no HTTP local, externalizável para true no HTTPS; SameSite=Lax default.
- Logout POST invalida sessão, limpa contexto e expira JSESSIONID; cookie/sessão
  antiga não autentica. GET /api/auth/logout não encerra a sessão.
- CSRF ativo, fluxo GET csrf → header X-CSRF-TOKEN → validação nativa; token
  anterior ao login é rejeitado após autenticar. Cliente renova após login e
  descarta após logout, obtendo outro no próximo fluxo. Não há storage de token.
- GET não altera dados de negócio. GET csrf pode criar sessão/token e o filtro
  pode invalidar uma sessão de conta suspensa: são operações técnicas de segurança.
- CORS com origens exatas, credentials=true e preflight antes de CSRF/autenticação.
  Wildcard com credentials não é permitido. 401/403 permanecem JSON, sem redirects,
  inclusive quando o cliente pede HTML. Authorization: Basic não autentica a API.
- API client deriva o hostname atual: 127.0.0.1→127.0.0.1:8080 e
  localhost→localhost:8080. O teste adicional executa os dois casos com
  credentials:include. A configuração opcional de base URL deve conservar o host
  local se houver cookies SameSite=Lax; CORS sozinho não corrige mistura de sites.
- /api/auth/me é a origem do usuário/role e /api/me/institutions a dos vínculos.
  Nenhuma role é inventada no frontend; guardas são apenas UX e o backend autoriza.
  ADMIN vai para dashboard e USER para seleção. Sair chama o servidor antes de navegar.
- Nenhuma senha é guardada em storage, cookie ou estado persistente do frontend.
  O campo de senha é limpo após disparar envio; URLSearchParams remove password no finally.
- BCrypt custo 12, comparação pelo mecanismo nativo de autenticação. Regex do User
  verifica apenas formato do hash; não compara senha. DTOs não contêm hash e User/
  UserPrincipal têm JsonIgnore como proteção adicional. Não há logs de senhas/hashes.
- Bootstrap disabled por padrão; exige flag explícita e identidade/senha no ambiente,
  normaliza e-mail, gera BCrypt, não promove USER existente, não redefine senha e é
  idempotente. Foi testado somente no H2 e não foi habilitado no MySQL real.

### Artefatos, credenciais e escopo da limpeza

A revisão examinou os arquivos de texto versionáveis do projeto inteiro, além do
diff: fontes, configurações, documentação e testes. Referências a password nos
inputs/configurações e ao password_hash do schema são nomes de campos, não senhas
embutidas. As credenciais de teste são geradas em memória; não são contas reais.

Não foram encontrados credenciais reais, chaves privadas, tokens de autenticação
hardcoded, serialização de hash em DTO, logging de credenciais, autenticação fake,
HTTP Basic configurado como fluxo, CSRF desabilitado ou CORS wildcard com credentials.
As referências a localStorage/sessionStorage nos testes são proxies que falham se
forem acessados; não são persistência de autenticação. Não há uso em código frontend.

.gitignore da raiz e backend mantêm target, bytecode, caches Python, .env e
temporários fora do conjunto candidato. git ls-files não encontrou .env, logs,
classes, jars ou saídas target rastreadas. Cookie/sessão do servidor não são gravados
em arquivos versionáveis. Após os testes, target contém builds ignorados, não fontes
necessários ou geradores descartáveis. Nada foi colocado no staging ou commitado.

### Resultado final da auditoria — 27/10/2026, 22:16 (America/Sao_Paulo)

| Validação | Resultado final |
| --- | --- |
| mvnw.cmd clean test, MYSQL_REFERENCE_TESTS=true | BUILD SUCCESS: 74 testes, zero falhas/erros/ignorados |
| Testes de segurança | 28, incluindo quatro novos casos nesta auditoria |
| Bootstrap H2 | Cinco testes; bootstrap de produção desabilitado |
| Regressões anteriores | 41 testes preservados, incluindo dois somente leitura no MySQL |
| Hibernate/MySQL | 15 tabelas validadas; nenhuma alteração de schema |
| Python | Três testes aprovados |
| Frontend node:test | 12 aprovados; novo caso cobre localhost e 127.0.0.1 |
| node --check | Sete módulos aprovados |
| git diff --check | Aprovado, incluindo revisão separada dos arquivos novos |
| Logs | Sem Failure in @ExceptionHandler, senhas ou valores de hash |
| MySQL real | users=0, institution_memberships=0, oito tabelas institucionais=0 |
| Git | HEAD c21fad1; nenhum commit e nada colocado em staging |

Alterações realizadas **nesta auditoria**, além da entrega anterior da Parte 3:

- common/error/ApiExceptionHandler.java: Content-Type application/json explícito.
- institution/membership/repository/InstitutionMembershipRepository.java: consulta
  em lote de OWNERs com conta e vínculo ACTIVE, carregando o usuário no mesmo SELECT.
- institution/membership/service/InstitutionMembershipService.java: proteção do último
  OWNER considera somente contas aptas ao login pelo critério de status/hash;
  mantém possível revogar outro OWNER cuja conta não possa autenticar.
- SecurityApiTests.java: regressões de PUT por papel, JSON para Accept HTML, Basic
  não autenticando, GET logout sem efeito e OWNER substituto não utilizável.
- tests/frontend/auth.test.mjs: regressão dos dois hostnames Live Server.
- Este documento: achados, correções, evidências e inventário atualizado.

As regras OWNER/MANAGER/EDITOR, endpoints, arquitetura, dependências e schema não
foram ampliados. As únicas correções de produção foram as duas falhas descritas.
Não há credenciais reais ou artefatos temporários no conjunto candidato ao commit.
Nenhum usuário/instituição fictício foi persistido no MySQL. Nenhum commit foi feito.

### Inventário final do conjunto candidato ao commit

**63 arquivos: 31 novos e 32 alterados.** Nada staged.

| Arquivo | Estado |
| --- | --- |
| `README.md` | Alterado |
| `backend/pom.xml` | Alterado |
| `backend/src/main/java/com/bemfeito/api/common/error/ApiExceptionHandler.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/institution/contact/service/InstitutionContactService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/institution/history/service/InstitutionHistoryImageService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/institution/membership/controller/InstitutionMembershipController.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MembershipPatch.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MembershipResponse.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/dto/MyInstitutionResponse.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/entity/InstitutionMembership.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/entity/MembershipRole.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/entity/MembershipStatus.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/repository/InstitutionMembershipRepository.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/membership/service/InstitutionMembershipService.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/institution/repository/InstitutionRepository.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/need/service/NeedService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/security/AuthController.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/CurrentAccountFilter.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/InstitutionAuthorizationService.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/SecurityConfig.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/SecurityJson.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/UserPrincipal.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/bootstrap/AdminBootstrapRunner.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/security/bootstrap/AdminBootstrapService.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/unit/contact/service/UnitContactService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/unit/hours/service/UnitOpeningHoursService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/unit/service/UnitQueryService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/unit/service/UnitService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/unit/servicearea/service/UnitServiceAreaService.java` | Alterado |
| `backend/src/main/java/com/bemfeito/api/user/dto/UserResponse.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/entity/PlatformRole.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/entity/User.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/entity/UserStatus.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/repository/UserRepository.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/service/CurrentUserService.java` | Novo |
| `backend/src/main/java/com/bemfeito/api/user/service/UserService.java` | Novo |
| `backend/src/main/resources/application.properties` | Alterado |
| `backend/src/test/java/com/bemfeito/api/AdminBootstrapTests.java` | Novo |
| `backend/src/test/java/com/bemfeito/api/InstitutionalApiTests.java` | Alterado |
| `backend/src/test/java/com/bemfeito/api/MySqlReferenceDataTests.java` | Alterado |
| `backend/src/test/java/com/bemfeito/api/ReferenceDataApiTests.java` | Alterado |
| `backend/src/test/java/com/bemfeito/api/SecurityApiTests.java` | Novo |
| `backend/src/test/resources/reference-schema.sql` | Alterado |
| `css/components/admin-layout.css` | Alterado |
| `database/scripts/test_prepare_cities.py` | Alterado |
| `docs/backend/institutional-core.md` | Alterado |
| `docs/backend/security.md` | Novo |
| `js/main.js` | Alterado |
| `js/pages/institution-login.js` | Alterado |
| `js/pages/institution-selection.js` | Alterado |
| `js/services/api-client.js` | Novo |
| `js/services/auth-guard.js` | Novo |
| `js/services/auth-service.js` | Novo |
| `pages/admin-dashboard.html` | Alterado |
| `pages/admin-institutions.html` | Alterado |
| `pages/admin-requests.html` | Alterado |
| `pages/institution-login.html` | Alterado |
| `pages/institution-selection.html` | Alterado |
| `pages/my-institution.html` | Alterado |
| `pages/unit-form.html` | Alterado |
| `tests/frontend/auth.test.mjs` | Novo |

## Continuidade na Parte 4

As referências acima a solicitações/convites sem integração descrevem o escopo
histórico da Parte 3. A Parte 4 acrescenta somente os POSTs públicos necessários
(cadastro, acesso, inspect/accept) e a busca pública limitada; CSRF permanece
ativo e `/api/admin/**` continua exclusivo de PLATFORM_ADMIN. Não altera login,
bootstrap, sessões nem as permissões institucionais existentes. Consulte
[contratos, locks, convites e configuração](requests-and-invitations.md).
