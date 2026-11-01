# Continuação — contexto, contas, mapas, contatos e necessidades

31/10/2026. Validação local concluída, sobre diff anterior preservado. Pronto para revisão; aplicação operacional da migração e SMTP externo continuam pendentes.

Implementação validada localmente: contexto por conta validado na API, assistente calculado,
envio SMTP após commit com fallback manual, recuperação com hash/expiração/uso único,
CSRF, limites e invalidação de sessões, mapa manual com confirmação, limpeza de
coordenadas após mudança de endereço, mapas públicos por unidade, formatação central
de contatos e seção pública de materiais ativos agrupados por unidade.

A causa do mapa inconsistente foi identificada no HTML: um aviso fixo de ausência de
cidades permanecia sobre o mapa. Além disso, cidades disponíveis e pontos consultam
critérios diferentes: endereço/cidade não geram latitude/longitude. O aviso foi removido.

Necessidades já possuíam quantidade e unidade de medida; nenhuma migração de doações
foi criada. A nova migração 005 é exclusiva de recuperação de senha e versão de
credenciais. Não foi aplicada a nenhum banco operacional.

Validação desta continuação:

- Frontend: 77 testes aprovados; navegador executado separadamente.
- Sintaxe: 45 arquivos JS/MJS aprovados; `git diff --check` sem erros.
- Backend: 144 testes selecionados aprovados (RequestApiTests 50, SecurityApiTests
  29, FrontendIntegrationApiTests 12, LocalImageStorageTests 8, InstitutionalApiTests
  30, RequestTransactionTests 10 e PasswordRecoveryTests 5). Mais 1 teste novo de
  ordenação persistida aprovado separadamente: 145 casos distintos aprovados.
- Recuperação com SMTP loopback e H2: novo login, rejeição da senha antiga, token
  expirado/reutilizado recusado, CSRF, limite e sessão anterior inválida aprovados.
- Fluxo completo de navegador aprovado após corrigir largura do mapa e consumo do
  fragmento de recuperação na mesma aba. A abertura remove o token da URL. Foram
  exercitados contexto A/B, permissões MANAGER/EDITOR, imagens/cards, materiais,
  coordenadas persistidas, aprovação, ativação, recuperação e novo login.
- Reexecução final do navegador após o complemento visual/ordenação: 1 fluxo completo
  aprovado, sem erros de página, recursos locais ou console. Responsividade em 338,
  390, 768, 1024, 1366 e 1440 px, incluindo o atalho direto para necessidades.
  Os tiles são substituídos no teste; APIs, autenticação, H2 e SMTP são reais locais.
- Falhas anteriores de MIME, mapa estreito, abertura na mesma aba e seletor ambíguo do teste foram corrigidas.
  As execuções que falharam não contam como aprovação. A restrição de acesso ao cache Maven foi resolvida
  por execução autorizada fora do sandbox; não se alterou o banco operacional.

[Contratos, SMTP, políticas de mapa e limites](../operations/accounts-mail-maps.md).

## Materiais para doação

O modelo Need e os endpoints de cadastro, PATCH parcial, status e remoção por unidade
já existiam, inclusive quantidade e unidade de medida. A apresentação pública ficava
dentro de cada unidade, junto a outras informações, sem uma seção de doações em
destaque ou ligação clara com os contatos responsáveis.

A página pública agora apresenta **O que esta instituição precisa**, antes das
seções detalhadas de unidades. Agrupa materiais por unidade, mostra descrição,
quantidade/unidade de medida e prioridade com texto e cor. Exibe somente ACTIVE;
FULFILLED e INACTIVE deixam de aparecer nos pedidos atuais. Sem itens, apresenta
mensagem vazia sem exemplos. Contatos públicos da unidade têm preferência sobre os
institucionais; contatos privados não são usados. A chamada orienta combinar a doação
diretamente, sem pagamento, reserva ou confirmação automática.

O responsável acessa **Minha Instituição → Unidades → Gerenciar necessidades**, preenche os campos e salva pelo formulário existente. Pode editar
parcialmente e alterar a situação. Exclusão continua exigindo INACTIVE; conflitos 409
preservam a lista e orientam atualizar. As permissões continuam validadas pelo backend.
A consulta pública busca os dados persistidos ao carregar/recarregar a página; não
há atualização em tempo real de outras abas.

## Complemento: cards e ordenação

O repositório NeedRepository já ordenava HIGH → MEDIUM → LOW, título e ID. Os
endpoints retornam a lista completa por unidade, sem paginação de necessidades.
A interface agora usa `need-order.js` para ordenar cópias a cada renderização,
inclusive depois de criar/editar. O desempate visual usa nome em pt-BR e ID; nenhuma
prioridade persistida é alterada e os grupos de unidades permanecem separados.

Cards têm cabeçalho com nome e prioridade textual, metadados agrupados e descrição
opcional. Sem metadados/descrição, não criam linhas vazias. A gestão mostra também
categoria e quantidade/unidade, com quebra de textos longos. O atalho **Gerenciar
necessidades** abre diretamente a etapa correspondente usando instituição/unidade
reais. A exclusão de registros inativos exige confirmação; cancelar não envia DELETE.

Testes novos verificam inclusão HIGH, mudança de prioridade, empate por nome/ID,
campos opcionais, grupos por unidade, preservação do array original e cancelamento
de remoção. O teste H2 confirma ordem após PATCH, campos preservados e retirada de
FULFILLED da consulta pública. Nenhum código de produção do backend ou schema foi
alterado para este complemento.

## Conferência manual recomendada

- Selecionar Instituição e Minha Instituição: troca, recarga e novo login; assistente,
  imagens e acesso direto às necessidades de cada unidade.
- Formulário de unidade: UF antes de cidade, feedback, permissões, posição confirmada
  e alteração de endereço; salvar necessidades e revisar a consulta pública.
- Catálogo, cidades e página pública: fotos, resumo, telefone, materiais agrupados,
  contatos longos, mapa de cada unidade e navegação por teclado em celular/desktop.
- Administração e contas: convite manual, SMTP configurado, ativação e recuperação;
  entrega externa depende do provedor e não é comprovada pelo capturador local.

## Autorizações e configuração ainda necessárias

A migração 005 não foi aplicada ao MySQL. Antes de executar o backend novo contra o
banco existente, são necessários inspeção, backup validado e autorização explícita.
SMTP externo exige configuração do operador. Geocodificação automática não foi
integrada: o mapa permite apontar e confirmar manualmente sem digitar coordenadas.
Nenhuma autorização de migração é necessária para a lista de materiais, que reutiliza
o schema existente. Commit, push e deploy não foram executados.

## Evidências e estado para revisão

Resultados visuais locais: `backend/target/browser-results/donation-needs.png`,
`cards-mobile.png`, `images.png`, `institution.png` e `report.json`. Esses artefatos
usam somente dados temporários do H2 e não comprovam dados operacionais. Capturas de
cards e materiais foram inspecionadas; a paginação com caractere incorreto foi corrigida.
Acessibilidade por leitor de tela e entrega na caixa postal externa ainda exigem
conferência manual/configuração, sem alegar certificação automática.

O refinamento anterior de imagens/cards, feedback e fechamento do modal foi mantido.
A retomada concluiu contexto, assistente, SMTP, recuperação, mapas e contatos, e os
complementos de materiais/ordenação. Não foi iniciado módulo de doação financeira,
gestão de OWNER ou nova arquitetura. Nenhuma alteração foi aplicada ao MySQL,
credenciais operacionais, volumes ou backups; não houve bootstrap, commit, push ou deploy.

## Inventário do diff preservado e desta continuação

Inclui o refinamento anterior de imagens, cards, feedback e aprovação; não representa somente arquivos novos desta retomada.

- `README.md`
- `backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionSummary.java`
- `backend/src/main/java/com/bemfeito/api/institution/history/repository/InstitutionHistoryImageRepository.java`
- `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java`
- `backend/src/main/java/com/bemfeito/api/requests/dto/RequestOutput.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationDeliveryService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationMail.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/InvitationService.java`
- `backend/src/main/java/com/bemfeito/api/requests/service/RequestReviewService.java`
- `backend/src/main/java/com/bemfeito/api/security/AbuseProtectionFilter.java`
- `backend/src/main/java/com/bemfeito/api/security/CurrentAccountFilter.java`
- `backend/src/main/java/com/bemfeito/api/security/SecurityConfig.java`
- `backend/src/main/java/com/bemfeito/api/security/UserPrincipal.java`
- `backend/src/main/java/com/bemfeito/api/security/recovery/PasswordRecoveryController.java`
- `backend/src/main/java/com/bemfeito/api/security/recovery/PasswordRecoveryService.java`
- `backend/src/main/java/com/bemfeito/api/security/recovery/PasswordResetRepository.java`
- `backend/src/main/java/com/bemfeito/api/security/recovery/PasswordResetToken.java`
- `backend/src/main/java/com/bemfeito/api/security/recovery/RecoveryConfiguration.java`
- `backend/src/main/java/com/bemfeito/api/unit/dto/UnitMapPoint.java`
- `backend/src/main/java/com/bemfeito/api/unit/repository/UnitRepository.java`
- `backend/src/main/java/com/bemfeito/api/unit/service/UnitService.java`
- `backend/src/main/java/com/bemfeito/api/user/entity/User.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/com/bemfeito/api/BrowserTestServer.java`
- `backend/src/test/java/com/bemfeito/api/FrontendIntegrationApiTests.java`
- `backend/src/test/java/com/bemfeito/api/InstitutionalApiTests.java`
- `backend/src/test/java/com/bemfeito/api/PasswordRecoveryTests.java`
- `backend/src/test/java/com/bemfeito/api/RequestApiTests.java`
- `backend/src/test/java/com/bemfeito/api/RequestTransactionTests.java`
- `backend/src/test/resources/reference-schema.sql`
- `css/components/institution-card.css`
- `css/global.css`
- `css/pages/institution.css`
- `css/pages/my-institution.css`
- `css/pages/unit-form.css`
- `css/variables.css`
- `database/migrations/005_password_recovery.sql`
- `database/schema.sql`
- `docs/frontend/accounts-maps-report.md`
- `docs/frontend/api-integration.md`
- `docs/frontend/ux-refinement-report.md`
- `docs/operations/acceptance.md`
- `docs/operations/accounts-mail-maps.md`
- `js/components/api-ui.js`
- `js/components/form-feedback.js`
- `js/components/institution-card.js`
- `js/components/institution-details.js`
- `js/components/institution-setup.js`
- `js/components/unit-location-picker.js`
- `js/main.js`
- `js/pages/admin-requests.js`
- `js/pages/cities.js`
- `js/pages/institution-access-request.js`
- `js/pages/institution-selection.js`
- `js/pages/my-institution.js`
- `js/pages/password-recovery.js`
- `js/pages/registration-request.js`
- `js/pages/unit-form.js`
- `js/services/api-client.js`
- `js/services/auth-guard.js`
- `js/services/contact-format.js`
- `js/services/institution-context.js`
- `js/services/map-service.js`
- `js/services/need-order.js`
- `js/services/request-service.js`
- `pages/admin-requests.html`
- `pages/cities.html`
- `pages/institution-access-request.html`
- `pages/institution-login.html`
- `pages/institution.html`
- `pages/my-institution.html`
- `pages/password-recovery.html`
- `pages/registration-request.html`
- `pages/unit-form.html`
- `tests/frontend/auth.test.mjs`
- `tests/frontend/browser-flow.test.mjs`
- `tests/frontend/contact-format.test.mjs`
- `tests/frontend/context.test.mjs`
- `tests/frontend/integration.test.mjs`
- `tests/frontend/requests.test.mjs`
