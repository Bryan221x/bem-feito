# Bem-Feito — refinamento final de UX/UI

Data: 31/10/2026. Base: `0cff1d9`. Alterações preservadas e complementadas sem commit.

## Revisão do trabalho interrompido

O diff já continha a seção visível de imagens, cards públicos enriquecidos,
organização dos detalhes, mudança do modal administrativo e parte da validação.
Foram concluídos o teste antigo do modal, a ordem UF/Cidade na unidade, o feedback
compartilhado, as regressões adicionais e a documentação. A execução real revelou
que campos de editores internos ocultos bloqueavam o envio da unidade; o filtro foi
corrigido sem dispensar campos obrigatórios das etapas. Também foi corrigida a perda
de fotos em rascunho ao salvar textos e a sobreposição de cores por estilos antigos.

## Resultado por requisito

| Área | Resultado |
| --- | --- |
| Formulários | Erros junto ao campo, aria-invalid/describedby, foco inicial e limpeza após correção. Falhas da API preservam entradas; login mantém a limpeza de senha já existente. Campos desabilitados e editores independentes são respeitados. |
| Cores | Verde para sucesso, vermelho para erros/remoção/rejeição, âmbar para pendência e neutro para inativo/desabilitado. Sucesso do cadastro e remover foto conferidos pelo estilo computado no navegador. |
| UF/Cidade | UF precede Cidade no DOM do cadastro e da unidade; opções continuam vindo da API. |
| Administração | Sucesso fecha modal após a resposta, anuncia resultado e recarrega lista com filtros. Processamento bloqueia duplicidade. Erro mantém modal/notas; 409 oferece atualização explícita. Convite manual permanece na página e não é anunciado como e-mail enviado. |
| Imagens | Seção visível em Minha Instituição, atalho no menu, seleção JPEG/PNG/WebP até 5 MiB, prévia, texto alternativo, legenda, ordem, remoção e feedback. Rascunhos sobrevivem ao salvamento dos textos. |
| Cards | Primeira foto ordenada real, fallback neutro, nome, categoria, cidade/UF, PHONE/WHATSAPP público, descrição com três linhas e Conhecer instituição. Conteúdo renderizado como texto, sem alterar registros. |
| Detalhes | Destaque fotográfico, resumo, descrição/objetivo expansíveis, contatos seguros, unidades, necessidades e galeria/história preservados. Galeria também disponível quando há fotos sem história. |
| Dados | Nenhum registro existente foi alterado. Preparação da banca documentada em ambiente separado usando os fluxos do sistema. |

O resumo público recebeu apenas os campos opcionais image/publicContact em contrato
existente, consultados em lote. Não foram criados endpoints, schema, migrations,
novos privilégios nem armazenamento simulado. O backend segue validando os uploads.
A primeira imagem é um destaque derivado da ordem da galeria: **não há modelo de
logotipo, capa independente ou fotos por unidade**; essas funções não foram criadas.

## Verificações executadas

- `node --test tests/frontend/*.test.mjs`: **66 aprovados, zero falhas**; um teste
  opt-in de navegador ignorado nessa execução e executado separadamente abaixo.
- `FrontendIntegrationApiTests`: **12 aprovados**; `InstitutionalApiTests`:
  **29 aprovados**; `LocalImageStorageTests`: **8 aprovados**. Total **49**, sem
  falhas/erros/ignorados. Comando em backend:
  `.\mvnw.cmd '-Dmaven.repo.local=C:\Users\BRYAN\.m2\repository' '-Dtest=FrontendIntegrationApiTests,InstitutionalApiTests,LocalImageStorageTests' test`.
- Edge headless + Spring real + H2 em memória: **1 fluxo completo aprovado** na
  execução final, em aproximadamente 44 segundos. Sem interceptar respostas reais.
  Inclui cadastro, aprovação, convite manual, ativação, login, edição, upload PNG e
  WebP, tipo inválido, alt obrigatório, preservação de rascunho, remoção pela interface,
  unidade/necessidades/horários, contato privado não publicado, cards com/sem foto e
  contato, descrição longa, galeria por teclado, papéis, logout e suspensão.
- Regressões rápidas: aprovação fecha modal; rejeição/ajustes/cancelamento aguardam
  resposta e preservam filtro; duplicidade bloqueada; 409 exige atualização; 503
  preserva modal/notas. Esses cenários de erro administrativos usam respostas de
  teste; não foram provocados no ambiente principal.
- Responsividade: **13 telas/estados × 6 larguras** (1440, 1366, 1024, 768, 390 e
  338 px), sem overflow horizontal. Zero pageErrors, consoleErrors ou erros de
  recursos locais na execução final. Capturas de cards desktop/celular, seção de
  imagens e detalhes inspecionadas visualmente.
- `node --check`: **37 arquivos JS/MJS aprovados**.
- `git diff --check`: aprovado.

As tentativas iniciais detectaram o teste obsoleto do modal e o bloqueio da unidade;
ambos foram corrigidos e reexecutados. Dois testes de upload falharam na execução
restrita; a execução autorizada fora do sandbox passou os 49 testes acima. Não se
considera a tentativa inicial como aprovação.

Artefatos locais, não versionados: `backend/target/browser-results/report.json`,
`cards.png`, `cards-mobile.png`, `images.png` e `institution.png`; relatórios Java
em `backend/target/surefire-reports`. A imagem com texto “Imagem do teste isolado”
é gerada somente pelo teste e enviada à API H2; não existe como dado da aplicação.

## Limites e conferência manual

- Nenhuma evidência de certificação completa de acessibilidade: leitor de tela,
  zoom 200%, dispositivos físicos e contraste de todos os estados exigem ensaio
  manual. Não se declara cobertura visual de toda a aplicação.
- Ordem de fotos e fallback de falha de imagem foram revisados no código; não houve
  cenário específico de falha de rede de imagem ou reordenação no navegador.
- Não foram adicionadas gestão de OWNER, logotipo/capa independente, fotos por
  unidade, nem outras funções que exigiriam ampliação do modelo.
- Produção, SMTP real e recuperação de backups com imagens presentes seguem
  pendentes conforme a Parte 6.2. Upload validado em H2 não comprova restauração.
- Operações da galeria usam requisições separadas; não há transação única entre
  todos os uploads/remoções. Em falha parcial, conferir o resultado antes de repetir.
- Conteúdo final da banca deve ser preparado posteriormente em ambiente isolado,
  conforme [aceitação](../operations/acceptance.md). Os registros existentes não
  foram apagados, encurtados ou substituídos.

Para conferir no servidor local habitual, abrir `pages/registration-request.html`,
`pages/admin-requests.html` como administrador e `pages/my-institution.html` pelo
seletor autenticado (seção Imagens da instituição). Abrir `pages/unit-form.html`
pelo botão da instituição; navegar por `pages/cities.html` → catálogo → detalhes,
preservando os identificadores reais nas URLs. Não apontar o teste H2 ao MySQL.

As alterações estão prontas para revisão do diff. Não houve bootstrap operacional,
alteração do MySQL principal, senhas/contas existentes, volumes ou backups, commit,
push ou deploy. Os testes usam dados temporários H2 e arquivos em target.

## Arquivos alterados

- `README.md`
- `backend/src/main/java/com/bemfeito/api/institution/dto/InstitutionSummary.java`
- `backend/src/main/java/com/bemfeito/api/institution/history/repository/InstitutionHistoryImageRepository.java`
- `backend/src/main/java/com/bemfeito/api/institution/service/InstitutionQueryService.java`
- `backend/src/test/java/com/bemfeito/api/BrowserTestServer.java`
- `backend/src/test/java/com/bemfeito/api/FrontendIntegrationApiTests.java`
- `css/components/institution-card.css`
- `css/global.css`
- `css/pages/institution.css`
- `css/pages/my-institution.css`
- `css/variables.css`
- `docs/frontend/api-integration.md`
- `docs/frontend/ux-refinement-report.md`
- `docs/operations/acceptance.md`
- `js/components/api-ui.js`
- `js/components/form-feedback.js`
- `js/components/institution-card.js`
- `js/components/institution-details.js`
- `js/main.js`
- `js/pages/admin-requests.js`
- `js/pages/institution-access-request.js`
- `js/pages/my-institution.js`
- `js/pages/registration-request.js`
- `js/pages/unit-form.js`
- `js/services/api-client.js`
- `js/services/request-service.js`
- `pages/admin-requests.html`
- `pages/institution.html`
- `pages/my-institution.html`
- `pages/registration-request.html`
- `pages/unit-form.html`
- `tests/frontend/browser-flow.test.mjs`
- `tests/frontend/requests.test.mjs`
