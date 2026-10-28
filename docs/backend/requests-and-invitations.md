# Parte 4 — solicitações, aprovações e ativação

## Base e implementação

Base: `main`, `a393a31` (Parte 3), inicialmente sem alterações locais. Foram
analisados schema/modelo de dados, documentação das Partes 2/3, entidades,
repositories, autorização, login/seleção e os formulários/páginas administrativas.
Java 25, Spring Boot 4.1.1, Hibernate, MySQL, sessão HTTP, BCrypt, CSRF, CORS e
arquitetura Controller → Service transacional → Repository foram preservados.
A dependência nova é o starter oficial `spring-boot-starter-mail`, gerenciado
pelo parent existente. Não há framework frontend novo.

Esta Parte implementa os dois fluxos completos: cadastro e aprovação de uma
instituição; solicitação e aprovação de acesso a instituição existente. Ambos
resolvem a conta real do responsável, criam o vínculo e, quando necessário,
emitem convite para definição inicial de senha. Nenhum cadastro demonstrativo
é inserido no MySQL; a demonstração deve usar exatamente os formulários reais.

## Modelagem e contratos

Entidades em `com.bemfeito.api.requests.entity`:

- RegistrationRequest → registration_requests;
- RegistrationRequestContact → registration_request_contacts;
- RegistrationRequestUnit → registration_request_units;
- RegistrationRequestUnitContact → registration_request_unit_contacts;
- AccessRequest → access_requests;
- AccountInvitationToken → account_invitation_tokens.

As associações são LAZY, IDs/timestamps respeitam as tabelas existentes e as
entidades nunca são respostas HTTP. RequestStatus contém os cinco estados do
schema. A API recebe uma mainUnit, mas a aprovação copia **todas** as unidades e
seus contatos que estejam associados ao pedido, sem descartar dados existentes.
Não são inventadas coordenadas: latitude/longitude começam null.

`RequestInput` agrupa Registration, Responsible, Contact, MainUnit, Access,
Decision, Inspect e Accept. `RequestOutput` agrupa Submitted, Summary, Detail,
UnitDetail, Location, SearchResult, Invitation, Reviewed, Inspection e Dashboard.
Accept/Inspect/Invitation e o evento de e-mail possuem toString redigido para
não revelar senha, token ou URL de ativação.

### Cadastro público

POST `/api/registration-requests`, JSON:

```json
{
  "categoryId": 1,
  "displayName": "NOME_DE_EXIBICAO",
  "legalName": "RAZAO_SOCIAL",
  "cnpj": "CNPJ_REAL",
  "objective": "OBJETIVO",
  "description": "DESCRICAO",
  "history": null,
  "website": null,
  "responsible": {
    "name": "NOME_RESPONSAVEL",
    "email": "EMAIL_RESPONSAVEL",
    "phone": "TELEFONE_RESPONSAVEL",
    "role": "CARGO_RESPONSAVEL"
  },
  "publicContacts": [{"type": "EMAIL", "value": "EMAIL_PUBLICO", "label": null, "isPrimary": true}],
  "mainUnit": {
    "name": "NOME_UNIDADE", "document": null, "description": null,
    "cityId": 1, "postalCode": "CEP_REAL", "street": "LOGRADOURO",
    "number": "NUMERO", "complement": null, "neighborhood": "BAIRRO", "contacts": []
  }
}
```

IDs acima ilustram a estrutura; escolher os IDs retornados pelas taxonomias,
sem presumir que o ID 1 corresponde a uma categoria/cidade específica. O corpo
não contém senha, IDs de usuário/revisor, status ou permissões administrativas.
Campos desconhecidos e enums numéricos são rejeitados pela política da Parte 3.

Normalização ocorre antes da validação: trim em textos; e-mails em lowercase;
CNPJ/documento/CEP em dígitos; telefone em 8–15 dígitos com `+` internacional
opcional. CNPJ usa o validador existente, CEP exige oito dígitos, website usa
HttpUrl e textos longos usam Utf8Text. Categoria deve existir/estar ativa e
cidade deve existir. Função/cargo é obrigatório. Há limites de tamanho e de
30 contatos por lista, no máximo um principal e proibição de contatos duplicados
após normalização. Contatos de e-mail/telefone são validados por tipo.

A resposta 201 tem somente `id`, `status: PENDING` e `createdAt`. CNPJ de uma
instituição existente retorna 409 orientando Solicitar acesso; pedido PENDING
ou APPROVED do mesmo CNPJ retorna 409. Estados REJECTED, NEEDS_CHANGES ou
CANCELLED permitem nova solicitação corrigida. Não há consulta pública de dados
privados dos pedidos.

### Acesso público ou autenticado

GET `/api/institutions/access-search?q=...`: início do nome ou CNPJ completo,
somente instituições ACTIVE, mínimo dois e máximo 150 caracteres, máximo 15
resultados e cinco localidades distintas por instituição. Caracteres `%`, `_`
e `!` são escapados. DTO: id, displayName, category e locations(id,city,state).
Não inclui razão social, contatos ou informações de responsáveis.

POST `/api/access-requests` recebe institutionId, requesterName,
requesterEmail, requesterPhone, requesterRole e justification opcional.
O institutionId precisa vir da seleção real; institutionQuery não integra o
contrato. Instituição inexistente/inativa é recusada. Mesmo e-mail+instituição
com pedido PENDING ou vínculo ACTIVE resulta em 409, inclusive para anônimos
cujo e-mail já corresponda a uma conta com vínculo.

Anônimo tem requesterUserId null. Em sessão, o ID vem de UserPrincipal,
e-mail precisa corresponder à conta e nome/e-mail persistidos vêm do servidor.
Não se aceita requesterUserId do navegador. O cargo declarado continua texto de
negócio e nunca é convertido automaticamente em MembershipRole.

## Estados, revisão e transações

PENDING pode ir para APPROVED, REJECTED, NEEDS_CHANGES ou CANCELLED. A ação é
terminal para aquele pedido; repetir decisão retorna 409. Rejeição, ajustes e
cancelamento exigem notas. NEEDS_CHANGES permite nova submissão corrigida,
pois o solicitante ainda não possui autenticação para editar um pedido pendente.
Não há edição anônima, reabertura nem aprovação de estados terminais.

Só PLATFORM_ADMIN pode listar, detalhar, revisar ou reemitir convites. As decisões
exigem CSRF e uma sessão real. reviewedByUser vem da sessão; reviewedAt é gerado
no servidor; reviewNotes é validado. Não existe reviewerId no contrato de entrada.

A aprovação de cadastro executa uma única transação: lock PESSIMISTIC_WRITE do
pedido, validação do estado/CNPJ/categoria/cidades, Institution ACTIVE, contatos
públicos, Unit ACTIVE e seus contatos, resolução do User, Membership OWNER ACTIVE,
convite se necessário e decisão/revisor. Uma falha de banco ou conflito tardio
faz rollback de todo o agregado e mantém o pedido PENDING.

Acesso usa lock do pedido e da instituição, revalida ACTIVE, resolve a conta,
bloqueia vínculo já ACTIVE, cria ou reativa vínculo PENDING/REVOKED com a role
**explicitamente escolhida pelo administrador**, gera convite quando aplicável
e registra a decisão na mesma transação. UI usa EDITOR como opção inicial e pede
confirmação com a permissão selecionada antes da concessão.

### Contas existentes

| Estado da conta | Resultado |
| --- | --- |
| Ausente | USER, INVITED, passwordHash null; vínculo ACTIVE e convite |
| ACTIVE com BCrypt utilizável | Reutiliza conta e senha; novo vínculo sem convite |
| INVITED | Reutiliza conta; vínculo aprovado e convite novo, invalidando anteriores |
| INACTIVE / SUSPENDED | Conflito; não reativa nem altera a senha |
| ACTIVE sem hash utilizável | Conflito para resolução administrativa |

Vínculo ACTIVE com conta INVITED representa concessão já aprovada. Login continua
impossível até a ativação da conta pelo convite. Uma conta pode ter vínculos com
várias instituições; não há instituição fixa no usuário.

### Concorrência e schema

O lock do pedido impede duas aprovações do mesmo pedido. O lock da instituição
coordena vínculo e solicitação de acesso com a política de memberships existente.
A resolução de conta existente e todas as mutações de convites usam lock do User.
Accept adquire User e depois token; reemissão também começa pelo User. Leituras
sob lock são atualizadas no contexto JPA com PESSIMISTIC_WRITE antes de validar
estado, garantindo leitura atual também sob REPEATABLE_READ do MySQL. Só um convite
não utilizado permanece por conta. Unique de e-mail e de membership continua
como defesa final: uma corrida ao criar uma conta ainda ausente faz uma transação
falhar/voltar integralmente; a revisão pode ser repetida para reutilizar a conta.

Faltavam duas garantias de unicidade condicional. A correção mínima está em
`database/migrations/004_request_uniqueness.sql`, também incluída ao final de
`database/schema.sql` para bancos novos:

- registration_requests.open_cnpj é gerada para PENDING/APPROVED, null nos demais
  estados, com unique uq_registration_requests_open_cnpj;
- access_requests.pending_email é gerada para PENDING, null nos demais, com
  unique (institution_id,pending_email).

As duas colunas são geradas pelo banco e não recebem valores de JPA/payloads.
MySQL permite várias entradas null nos índices; assim a restrição não impede
nova submissão corrigida. No banco existente, executar o arquivo de migração
**uma vez** antes de iniciar esta versão. Não repetir no banco criado pelo schema
completo. Duplicatas antigas fazem a migração falhar e precisam de revisão
administrativa prévia, sem remoção silenciosa. Esta entrega aplicou essa migração
no MySQL local vazio de pedidos e verificou os índices e integridade do admin.
O DDL funcional usa H2 equivalente (generated sem STORED, ADDs separados); o teste
Python compara literalmente os CREATE TABLE das 21 tabelas com o schema MySQL.

## Convite e ativação

SecureRandom gera 32 bytes (256 bits) e Base64 URL-safe sem padding (43 caracteres).
Só SHA-256 hexadecimal (64 caracteres) vai para token_hash. O schema original
mantém VARCHAR(255), sem necessidade de alterá-lo. Token bruto não aparece em
colunas, logs ou endpoints de consulta. Ao emitir, tokens ainda não usados do
mesmo usuário recebem usedAt; cada reemissão cria segredo novo.

APP_INVITATION_TTL padrão 48h, positivo e até 30 dias. A URL usa
`APP_FRONTEND_BASE_URL/pages/account-activation.html#token=...`. O fragmento não é
query da API nem enviado automaticamente ao servidor HTTP. A página o extrai,
remove via history.replaceState e conserva apenas em memória. Inspeção/ativação
usam corpo JSON e CSRF. Não há localStorage, sessionStorage ou cookie de token.

Inspect retorna READY, INVALID, EXPIRED ou ALREADY_USED; detalhes de identidade
(nome/e-mail mascarado/validade) só são retornados para um token válido e utilizável.
Inválidos não indicam existência de uma conta. Inspect não altera negócio.

Accept recebe `token`, `password`, `confirmPassword` (nome usado consistentemente
no backend/frontend). Sob lock, valida usuário INVITED, expiração e uso; valida
mínimo 12 caracteres, máximo 72 bytes UTF-8 e confirmação; gera BCrypt custo 12,
ativa User, preenche emailVerifiedAt, usa o token e invalida os outros. Não há
login automático. A página limpa os campos e oferece Entrar na conta. Convites
não redefinem a senha de contas ACTIVE, INACTIVE ou SUSPENDED.

## E-mail e modo manual

APP_MAIL_ENABLED=false é o padrão. Somente a **resposta da ação administrativa
que acabou de emitir** tem invitation.activationUrl; reemissão retorna esse
mesmo contrato diretamente. Listas/detalhes nunca recuperam o segredo. O modal
exibe temporariamente link e botão de cópia; fecha/navega/recarrega e o valor
é descartado. Para obter outro, reemitir (invalida anteriores). Não há envio
simulado ou autenticação alternativa.

APP_MAIL_ENABLED=true publica InvitationMail em memória na transação. Um
TransactionalEventListener AFTER_COMMIT usa JavaMailSender oficial. Não executa
SMTP dentro da transação de banco e não envia se ela sofrer rollback. Mensagem
inclui instituição quando disponível, validade e orientação para ignorar caso
não reconheça; nunca inclui senha. URL não é retornada ao frontend nesse modo.
O envio é síncrono após commit, com timeouts finitos; não há fila durável ou
retry automático. Falha não desfaz a aprovação: log fixo apenas com userId, sem
exception/message/cause que possam carregar segredo; ADMIN pode reemitir pelo
modal de detalhes de pedidos aprovados cuja conta continue INVITED.

Não se afirma entrega quando a API apenas solicitou envio. Ausência de host,
remetente ou indisponibilidade SMTP gera aviso técnico e permite reemissão após
corrigir configuração. Não foram enviados e-mails reais nesta entrega; testes
mockam JavaMailSender e cobrem envio, erro e evento após commit. Comunicação por
e-mail de rejeição/ajustes é opcional no requisito e não foi adicionada.

| Variável/propriedade | Uso |
| --- | --- |
| DB_URL / DB_USER / DB_PASSWORD | MySQL existente; somente ambiente |
| APP_INVITATION_TTL | 48h padrão; duração positiva, máximo 30d |
| APP_FRONTEND_BASE_URL | http://127.0.0.1:5500 padrão; URL HTTP(S), sem segredo/query/fragmento |
| APP_MAIL_ENABLED | false padrão; true habilita entrega SMTP |
| APP_MAIL_FROM | Remetente real do convite (necessário em modo SMTP) |
| SPRING_MAIL_HOST / SPRING_MAIL_PORT | Host/porta do servidor SMTP |
| SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD | Credenciais SMTP somente ambiente |
| spring.mail.properties.mail.smtp.auth | Habilitar conforme servidor |
| spring.mail.properties.mail.smtp.starttls.enable | TLS conforme servidor |
| spring.mail.properties.mail.smtp.starttls.required | Exigir TLS quando apropriado |
| spring.mail.properties.mail.smtp.connectiontimeout | 5000 ms |
| spring.mail.properties.mail.smtp.timeout | 5000 ms |
| spring.mail.properties.mail.smtp.writetimeout | 5000 ms |
| spring.mail.properties.mail.debug | false; não habilitar logs de protocolo/corpo |

Spring Mail segue as propriedades externas nativas do Boot. Não há .env nem
credencial de SMTP no repositório. Em produção, usar HTTPS, cookie Secure,
CORS explícito e configuração SMTP/TLS do provedor. As limitações operacionais
de sessão e infraestrutura da Parte 3 continuam aplicáveis.

## Endpoints

Todos os POSTs exigem CSRF, inclusive públicos. API usa ApiError existente:
400 validação/token inválido, 401 sem autenticação, 403 sem role/CSRF, 404 recurso
inexistente e 409 conflito/estado/duplicidade. Nenhum endpoint expõe Entities.

| Método | Caminho | Autorização / resposta |
| --- | --- | --- |
| POST | /api/registration-requests | Pública; 201 Submitted |
| GET | /api/institutions/access-search?q= | Pública; lista leve limitada |
| POST | /api/access-requests | Pública ou sessão; 201 Submitted |
| POST | /api/account-invitations/inspect | Pública; Inspection; token no body |
| POST | /api/account-invitations/accept | Pública; state SUCCESS; sem sessão autenticada nova |
| GET | /api/admin/registration-requests | ADMIN; Page Summary |
| GET | /api/admin/registration-requests/{id} | ADMIN; Detail |
| GET | /api/admin/access-requests | ADMIN; Page Summary |
| GET | /api/admin/access-requests/{id} | ADMIN; Detail |
| POST | /api/admin/registration-requests/{id}/approve | ADMIN; Reviewed |
| POST | /api/admin/registration-requests/{id}/reject | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/registration-requests/{id}/needs-changes | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/registration-requests/{id}/cancel | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/access-requests/{id}/approve | ADMIN; Reviewed; membershipRole obrigatória |
| POST | /api/admin/access-requests/{id}/reject | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/access-requests/{id}/needs-changes | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/access-requests/{id}/cancel | ADMIN; Reviewed; notes obrigatória |
| POST | /api/admin/users/{id}/invitation | ADMIN; Invitation; somente INVITED |
| GET | /api/admin/requests/summary | ADMIN; contagem pendente + cinco pedidos recentes |

Listas aceitam status opcional, page ≥ 0 e size 1–100 (padrão 20), ordenadas por
createdAt DESC, id DESC. Tipo é explícito no endpoint e no DTO. Decisão recebe
`{"notes":"...","membershipRole":"EDITOR"}`; role só é exigida/aplicada na
aprovação de acesso, nunca derivada do cargo declarado.

Services: RequestSubmissionService valida/persiste pedidos; RequestQueryService
materializa consultas/DTOs; RequestReviewService orquestra aprovação/revisão;
InvitationService emite/inspeciona/ativa; InvitationDeliveryService entrega após
commit. PublicRequestController e AdminRequestController expõem os contratos.
SecurityConfig libera apenas as operações públicas necessárias; /api/admin/**
continua com role na cadeia e method security nos services administrativos.

## Frontend e teste manual local

Cadastro preserva layout/editor de contatos, adiciona seletores reais e estados
loading/error/empty, erro por campo, confirmação com número do pedido e limpeza
após sucesso. Cargo é obrigatório. Município usa cityId, categoria categoryId;
contatos da unidade são enviados como lista vazia enquanto não há editor próprio.

Acesso tem debounce de 350 ms, descarta respostas atrasadas, estados de busca,
seleção visual única e CTA de cadastro. Sessão válida preenche nome/e-mail como
readonly; indisponibilidade de sessão bloqueia envio até recarregar, 401 permite
fluxo anônimo. O backend continua autoridade da identidade.

Admin tem tipo/status, paginação, modal nativo acessível com detalhes completos,
notas, permissões, confirmação e ações reais. Bloqueia controles enquanto envia;
recarrega lista e dados do modal após decisão. Reemissão fica disponível para
conta INVITED em pedido aprovado. Dashboard integra contagem pendente e cinco
pedidos recentes usando o mesmo serviço de consulta. Demais indicadores ficam
indisponíveis, sem números inventados. Não foi alterado login nem seleção de
instituição da Parte 3.

Ativação usa componentes/cores existentes e estados loading, invalid, expired,
already-used, ready, submitting, success e error. A senha não fica em storage;
é limpa após envio. Inspeção indisponível orienta reabrir o link. Logout e sessões
continuam nos mecanismos nativos da Parte 3.

No CMD, após preparar MySQL/schema e o administrador real:

```bat
cd /d C:\Users\BRYAN\Desktop\bem-feito\backend
set "DB_USER=SEU_USUARIO"
set "DB_PASSWORD=SUA_SENHA"
set "APP_BOOTSTRAP_ADMIN_ENABLED=false"
set "APP_MAIL_ENABLED=false"
set "APP_FRONTEND_BASE_URL=http://127.0.0.1:5500"
mvnw.cmd spring-boot:run
```

Abrir Live Server na raiz e usar o mesmo hostname 127.0.0.1 na porta 5500.
Cadastro: `/pages/registration-request.html`; login do administrador:
`/pages/institution-login.html`; análise: `/pages/admin-requests.html`.
Após aprovação, copiar manualmente o link por canal privado; abrir como destinatário
em janela privada/outro navegador (ou encerrar a sessão administrativa), ativar e efetuar login
normal e selecionar o vínculo real. Para conceder outro acesso, usar
`/pages/institution-access-request.html` e escolher permissão na revisão.
Usar localhost nos dois serviços também funciona, mas nesse caso alterar a base
de convite para localhost. Não misturar os dois hosts em sessão/ativação.

## Verificação e limites

Testes funcionais são exclusivamente H2. RequestApiTests cobre normalização,
validação, duplicidade, privacidade, CSRF, ADMIN, identidade real de sessão,
estados de revisão, cópia de contatos/unidade, contas existentes, todas as
MembershipRoles, reativação de vínculos, convites/BCrypt/expiração/reuso e login
após ativação. RequestTransactionTests usa transações realmente commitadas e
duas conexões concorrentes: falha de CHECK no banco com rollback integral,
conflito tardio de conta, SMTP após commit, erro de envio, rollback sem e-mail,
dupla aprovação, duplicidade simultânea, dupla ativação e reemissão simultânea.
Fixtures são de teste, em memória; cleanup existe apenas para o H2 exclusivo.

Testes Node nativos exercitam os módulos reais com DOM/fetch isolados: taxonomias,
formulário de cadastro, busca/seleção de acesso, contratos/CSRF, erros, bloqueio de
duplo envio, decisão e atualização do modal, link manual descartado, fragmento e
ativação. Não foi instalado framework pesado. Testes Python existentes verificam
snapshot oficial/carga reproduzível e correspondência do DDL de 21 tabelas.
MySqlReferenceDataTests valida Hibernate e consultas somente leitura, incluindo
novas listas/resumo, índices e digest do administrador antes/depois.

Comandos: `mvnw.cmd clean test`, `python -m unittest discover -s database/scripts
-p "test_*.py"`, `node --test tests/frontend/*.test.mjs`, `node --check` de cada JS
alterado/criado e `git diff --check`. MYSQL_REFERENCE_TESTS=true e DB_PASSWORD no
ambiente habilitam somente os dois testes MySQL de leitura; demais contextos usam
H2 explícito. Bootstrap fica desabilitado nos testes.

Resultados finais estão registrados no relatório de entrega abaixo. Não houve
implantação pública, git add, commit ou push. Permanecem para Parte 5: indicadores
administrativos restantes, integração completa das demais páginas públicas e
my-institution/unit-form, além do escopo que for definido pelo projeto.
Recuperação de senha, OAuth/social, pagamentos, comentários, doações financeiras
e deploy final não foram antecipados.

Referências técnicas: [Spring Boot Mail](https://docs.spring.io/spring-boot/reference/io/email.html)
e [TransactionalEventListener](https://github.com/spring-projects/spring-framework/blob/main/spring-tx/src/main/java/org/springframework/transaction/event/TransactionalEventListener.java).

## Relatório final de entrega — 27/10/2026

1. **Criados:** 33 arquivos; inventário completo abaixo, agrupado por área.
2. **Alterados:** 24 arquivos; inventário completo abaixo.
3. **Entidades:** seis tabelas de solicitações/convites mapeadas e RequestStatus.
4. **DTOs:** RequestInput/RequestOutput, com contratos públicos mínimos e detalhes
   administrativos separados, sem Entity, passwordHash ou tokenHash nas respostas.
5. **Services:** Submission, Query, Review, Invitation e Delivery; evento efêmero
   InvitationMail conecta emissão à entrega após commit.
6. **Controllers:** PublicRequestController e AdminRequestController; todos os
   caminhos/métodos e autorizações constam na tabela de endpoints acima.
7. **Públicos:** cadastro, busca limitada, pedido de acesso, inspect e accept;
   todos os POSTs continuam com CSRF.
8. **Administrativos:** listas/detalhes dos dois tipos, oito decisões, reemissão
   e resumo do dashboard; exclusivamente PLATFORM_ADMIN.
9. **Cadastro:** formulário com IDs reais → pedido PENDING e recibo mínimo.
10. **Aprovação:** transação única cria instituição/unidade/contatos, resolve conta,
    concede OWNER e registra revisão; nenhuma publicação fictícia antecipada.
11. **Acesso:** seleção real, identidade validada, revisão com permissão explícita,
    vínculo criado/reativado e convite somente se INVITED.
12. **Convite:** emissão/reemissão segura, um utilizável por conta, manual local ou
    SMTP configurado; não há recuperação posterior do token bruto.
13. **Ativação:** inspeção, definição/validação inicial de senha, BCrypt 12, ACTIVE,
    emailVerifiedAt/usedAt; login normal posterior.
14. **Existing users:** ACTIVE funcional é reutilizado com senha preservada;
    INVITED recebe convite novo; INACTIVE/SUSPENDED/ACTIVE sem hash geram conflito.
15. **Locking:** pedido, instituição quando necessário, usuário e token; corridas
    testadas com duas threads/conexões e constraints como defesa final.
16. **Transações:** rollback integral inclusive em falha real de constraint e em
    conflito tardio; SMTP só depois de commit e nunca provoca rollback posterior.
17. **Token:** SecureRandom 256 bits + Base64 URL-safe; somente SHA-256 persistido.
18. **TTL:** APP_INVITATION_TTL=48h por padrão; positivo e até 30d; validade
    reavaliada na inspeção e, sob lock, na aceitação.
19. **SMTP:** starter oficial/JavaMailSender, remetente e conexão externos,
    timeouts de 5000 ms; entrega/falha testadas com mock, sem e-mail real.
20. **Manual:** APP_MAIL_ENABLED=false; URL somente na resposta administrativa de
    emissão, exibida temporariamente e removida ao fechar/navegar.
21. **Frontend cadastro:** categoria/estado/cidade reais, cargo obrigatório,
    contatos normalizados, submit real, erros/recibo e bloqueio de duplo envio.
22. **Frontend acesso:** debounce, loading/error/empty, seleção única obrigatória,
    sessão preenchida/readonly e envio com institutionId.
23. **Admin requests:** filtros/paginação, detalhes completos, quatro ações por
    tipo, notas, permissão de acesso, confirmação, reemissão e cópia manual.
24. **Página de ativação:** HTML/CSS/JS novos, identidade visual existente,
    fragmento retirado, estados explícitos e ausência de autenticação automática.
25. **Testes criados:** RequestApiTests (50 casos), RequestTransactionTests
    (9 casos), requests.test.mjs (14 casos); MySQL/Python existentes ampliados.
26. **Java:** `mvnw.cmd clean test` com opt-in MySQL: **133 testes, zero falhas,
    zero erros, zero ignorados**. Inclui os 74 casos anteriores e 59 novos.
27. **Frontend:** **26 testes Node aprovados** (12 anteriores + 14 novos).
28. **Python:** **3 testes aprovados**, inclusive snapshot oficial e 21 CREATE TABLEs.
29. **Hibernate/MySQL:** validação aprovada contra MySQL 8.4.11; dois testes de
    leitura aprovados; ddl-auto continua validate e open-in-view false.
30. **node --check:** sete arquivos JS/MJS alterados/criados aprovados; HTML com
    IDs únicos e labels associados nas quatro páginas dos fluxos.
31. **git diff --check:** aprovado; arquivos novos também verificados quanto a
    whitespace. Textos em LF conforme .gitattributes.
32. **Schema:** somente duas colunas geradas e seus índices únicos, migração
    versionada/reproduzível e já aplicada ao MySQL local. Nenhuma alteração de usuário.
33. **Incompatibilidades corrigidas:** cargo opcional versus NOT NULL;
    categoria/cidade livres versus IDs; placeholders de formulários/admin versus
    fluxos reais; lacuna de unicidade concorrente. Sem incompatibilidade pendente
    nos testes. SMTP exige configuração real antes de habilitar envio.
34. **Parte 5:** indicadores e integrações restantes indicados na seção de limites;
    nenhuma dessas funcionalidades foi antecipada nesta entrega.
35. **Credenciais:** revisão do inventário/diff não encontrou senha real, chave
    privada, credencial SMTP ou .env. Exemplos usam placeholders; testes geram
    credenciais isoladas em execução, sem usuários operacionais persistentes.
36. **Segredos de convite:** nenhum token bruto persistido; não há logs de token
    ou senha nos services. Banco só armazena hash; browser só memória temporária.
37. **MySQL real:** 27 estados, 5.571 cidades, um usuário real; instituições,
    unidades, memberships, registration_requests, access_requests e convites
    continuam com contagem zero. Nenhum dado fictício funcional foi persistido.
38. **Administrador existente:** contagem de um PLATFORM_ADMIN e digest de todos
    os seus campos iguais antes/depois (incluindo hash e timestamps); não foi
    apagado, reativado, promovido, renomeado nem teve senha alterada.
39. **Git:** HEAD permanece a393a317876e42a2013d87d52f71842ee2596b46; staging
    vazio. Não houve git add, commit, push ou deploy. target, classes compiladas,
    caches e temporários permanecem ignorados; generate_writes.py não existe.

### Inventário completo

Os caminhos abaixo são relativos à raiz do repositório. Os únicos arquivos novos
SQL de dados operacionais são fixtures/cleanup em src/test, usados exclusivamente
no H2. Nenhum gerador descartável ou fonte dentro de target integra esta entrega.

#### Criados (33)

**Backend — produção**

- `backend/src/main/java/com/bemfeito/api/requests/controller/AdminRequestController.java`
- `backend/src/main/java/com/bemfeito/api/requests/controller/PublicRequestController.java`
- `backend/src/main/java/com/bemfeito/api/requests/dto/RequestInput.java`
- `backend/src/main/java/com/bemfeito/api/requests/dto/RequestOutput.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/AccessRequest.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/AccountInvitationToken.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/RegistrationRequest.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/RegistrationRequestContact.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/RegistrationRequestUnit.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/RegistrationRequestUnitContact.java`
- `backend/src/main/java/com/bemfeito/api/requests/entity/RequestStatus.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/AccessRequestRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/AccountInvitationTokenRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/RegistrationRequestContactRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/RegistrationRequestRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/RegistrationRequestUnitContactRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/repository/RegistrationRequestUnitRepository.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationDeliveryService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationMail.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/RequestQueryService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/RequestReviewService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/RequestSubmissionService.java`

**Testes**

- `backend/src/test/java/com/bemfeito/api/RequestApiTests.java`
- `backend/src/test/java/com/bemfeito/api/RequestTransactionTests.java`
- `backend/src/test/resources/request-cleanup.sql`
- `tests/frontend/requests.test.mjs`

**Frontend**

- `css/pages/account-activation.css`
- `js/pages/account-activation.js`
- `js/services/request-service.js`
- `pages/account-activation.html`

**Documentação, banco e configuração**

- `database/migrations/004_request_uniqueness.sql`
- `docs/backend/requests-and-invitations.md`


#### Alterados (24)

**Backend — produção**

- `backend/src/main/java/com/bemfeito/api/institution/membership/repository/InstitutionMembershipRepository.java`
- `backend/src/main/java/com/bemfeito/api/institution/repository/InstitutionRepository.java`
- `backend/src/main/java/com/bemfeito/api/security/SecurityConfig.java`
- `backend/src/main/java/com/bemfeito/api/unit/repository/UnitRepository.java`
- `backend/src/main/java/com/bemfeito/api/user/repository/UserRepository.java`

**Testes**

- `backend/src/test/java/com/bemfeito/api/MySqlReferenceDataTests.java`
- `backend/src/test/resources/application.properties`
- `backend/src/test/resources/reference-schema.sql`
- `database/scripts/test_prepare_cities.py`

**Frontend**

- `css/pages/admin-requests.css`
- `css/pages/registration-request.css`
- `js/pages/admin-dashboard.js`
- `js/pages/admin-requests.js`
- `js/pages/institution-access-request.js`
- `js/pages/registration-request.js`
- `pages/admin-requests.html`
- `pages/institution-access-request.html`
- `pages/registration-request.html`

**Documentação, banco e configuração**

- `README.md`
- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `database/schema.sql`
- `docs/backend/security.md`
- `docs/database/database-model.md`

