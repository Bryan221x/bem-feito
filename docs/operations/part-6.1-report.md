# Parte 6.1 — relatório final

Conferência final em 29/10/2026; execuções de testes em 28/10/2026.
**Preparação local validada; aplicação ainda não aprovada para produção.**
Base: main, ae70b268c177fa859f5efe46a4dbe5df294f1d2b. As Partes 1–5 foram preservadas.

## Análise, alterações e evidências

| Item solicitado | Resultado |
| --- | --- |
| 1. Análise | Documentação das Partes 1–5, modelo/schema/referências/migração, segurança, serviços, frontend e testes revisados. Fluxos existentes reutilizados. |
| 2. Problemas | Faltavam configuração operacional de produção, proteção contra abuso, health dependente do banco e procedimentos de backup/reconciliação. Erro inesperado imprimia exceção completa. |
| 3. Correções | Perfil prod e validação de configuração; limite por IP; health com SELECT 1/503 genérico; logging de erro somente pelo tipo; infraestrutura e documentação. |
| 4. Funcionalidades | Catálogo, gestão, unidades, contatos, necessidades, horários, áreas, fotos, solicitações, aprovação, convite, ativação, login e administração revalidados. |
| 5. Segurança | Sessão, fixation, CSRF, CORS e autorização preservados. Prod exige Secure/HttpOnly/Lax, origem HTTPS, usuário DB não-root, validate e bootstrap desligado. Proxy preparado com limites e headers de segurança; validação efetiva em hospedagem pendente. |
| 6. Deploy recomendado | VPS Linux, Docker Compose, Caddy HTTPS, frontend e /api na mesma origem, MySQL privado e volumes. Nenhum domínio escolhido/serviço contratado. |
| 7. Docker/configuração | Dockerfiles backend/frontend, compose.yaml, .dockerignore, Caddyfile, application-prod.properties e restrição de privilégios da conta runtime. Docker não disponível neste host: build/start de containers não executados. |
| 8. Variáveis | APP_DOMAIN, ACME_EMAIL, DB_PASSWORD, MYSQL_ROOT_PASSWORD e SMTP opcional externos; detalhes em production.md. Nenhum arquivo .env real criado. |
| 9. MySQL persistente | Volume mysql_data, inicialização somente em volume vazio, 21 tabelas e 5.571 cidades. Persistência após restart Docker ainda precisa ser demonstrada. |
| 10. Uploads persistentes | Volume uploads, UID 10001, diretório fora de assets. JPEG/PNG/WebP, 5 MiB/16 milhões de pixels e recodificação PNG preservados. Restart Docker pendente. |
| 11. Backup/restauração | backup.sh para janela de manutenção, dump+tar+checksums; restore isolado, retenção e cópia criptografada documentados. Não executado no banco principal; ensaio completo pendente. |
| 12. Migrações | Sem alteração de schema nem nova migração. Consulta de estado da 004 preparada; migração local já aplicada, não reaplicada. Estado parcial exige análise humana. |
| 13. SMTP | Parte 4 preservada; TLS, hostname, timeouts, falha após commit e reemissão documentados. Nenhum e-mail real enviado; validação do provedor pendente. |
| 14. Java | clean test: **161 testes, zero falhas, erros ou ignorados**, incluindo dois MySQL somente leitura. Oito testes novos de produção/abuso/health. Empacotamento Maven aprovado. |
| 15. Frontend | **44 aprovados**, zero falhas; browser opt-in ignorado na suíte comum e executado separadamente. |
| 16. Python | **6 aprovados**, três anteriores e três de reconciliação sem mutações. |
| 17. Navegador | **1 fluxo E2E ampliado aprovado** no Edge headless com Spring real e H2 efêmero; último resultado 40,6 segundos. Sem erros de página, console ou recursos locais. |
| 18. Responsividade | 13 estados × 1440/1366/1024/768/390/338px, sem overflow; galeria por teclado, Escape, retorno de foco do menu e Home reduced motion. Não certifica contraste/WCAG/leitores de tela. |
| 19. Documentação | README atualizado, situação do IBGE corrigida no modelo; production.md, acceptance.md e este relatório novos. |
| 20. Inconsistências | Relatórios antigos descrevem frontend/segurança/upload ainda futuros, superados pelas Partes seguintes. Recuperação de senha continua ausente. Sem roteiro oficial do professor no repositório; conferir original. |
| 21. Riscos | Docker/TLS/restore não ensaiados; sem scanner completo atualizado de dependências/imagens; sessões em memória; rate limit por instância/IP; SMTP sem fila durável; serviços externos; DB/filesystem não atômicos. |
| 22. Aceite local | Suítes verdes, Hibernate validate H2/MySQL, referências e sintaxe JS/MJS aprovadas, autorização/privacidade/fluxos principais exercitados. |
| 23. Aceite produção | Pendente build/start limpo, volumes/restart, backup/restore, HTTPS/renovação, cookies e login/logout/API online, SMTP autorizado, carga e scanner. |
| 24. Inventário | 19 arquivos novos e 6 alterados, listados abaixo. |
| 25. Diff | git diff --check e checagem adicional dos arquivos novos sem erros; conferir também o diff staged, pois o índice já contém arquivos na conferência final. |
| 26. Credenciais | Varredura exata da senha MySQL usada e padrões de chaves/tokens: zero ocorrências nos arquivos não ignorados examinados. Credenciais apenas no ambiente de processos; nenhum cookie/sessão/token real exportado. |
| 27. MySQL real | Consulta somente leitura: 0 instituições, 0 unidades, 1 PLATFORM_ADMIN ativo, 5.571 cidades. Teste confirmou digest do admin e contagens operacionais iguais antes/depois. Nenhuma migração/bootstrap/escrita de negócio realizada. |
| 28. Git | HEAD preservado, nenhum commit/push/deploy realizado por esta execução. O índice foi observado preenchido na conferência final e foi preservado; não houve comando git add nesta execução. |
| 29. Executar | Comandos exatos de desenvolvimento, testes, preparação VPS e operação estão em production.md e acceptance.md. |
| 30. Próximos passos | Validar containers em ambiente descartável, scanner/digests e backup/restore/restart; definir domínio/VPS/SMTP; conferir roteiro acadêmico; somente então publicar com autorização e validar online. |

## Fluxo de navegador ampliado

Cadastro pelo formulário → aprovação → convite manual → ativação → login →
PATCH geral/história → PNG/WebP → contato privado → segunda unidade → áreas,
horário e necessidade → recarga → catálogo/detalhe/galeria → FULFILLED/INACTIVE →
exclusão de fotos → suspensão. Acrescentados formulário inválido, segunda instituição
via solicitação real, aprovação concorrente (200/409), duas instituições por OWNER,
MANAGER/EDITOR por convite, edição permitida/negada, 403 entre instituições e logout.
Tudo restrito ao H2; nenhum cadastro fictício persistido no MySQL principal.

A primeira execução da extensão recebeu 400 por faltar contato público obrigatório.
Corrigido exclusivamente o payload do teste; contrato de negócio preservado.
Execuções posteriores aprovadas. Não se afirma teste manual com leitor de tela,
contraste completo ou publicação real.

## Arquivos

Alterados (6): README.md; docs/database/database-model.md;
backend/src/main/java/com/bemfeito/api/common/controller/HealthController.java;
backend/src/main/java/com/bemfeito/api/common/error/ApiExceptionHandler.java;
backend/src/main/java/com/bemfeito/api/security/SecurityConfig.java;
tests/frontend/browser-flow.test.mjs.

Novos (19):

- Infraestrutura (8): .dockerignore, compose.yaml, backend/Dockerfile,
  deploy/frontend.Dockerfile, deploy/Caddyfile, deploy/40-restrict-app.sql,
  deploy/migration-status.sql, deploy/upload-references.sql.
- Operação (1): deploy/backup.sh.
- Backend (3): backend/src/main/java/com/bemfeito/api/security/AbuseProtectionFilter.java,
  backend/src/main/java/com/bemfeito/api/security/ProductionConfiguration.java,
  backend/src/main/resources/application-prod.properties.
- Java testes (2): backend/src/test/java/com/bemfeito/api/HealthTests.java e ProductionSecurityTests.java.
- Python (2): database/scripts/audit_uploads.py e test_audit_uploads.py.
- Documentação (3): docs/operations/production.md, acceptance.md e part-6.1-report.md.

Evidências em backend/target: part61-java.log, surefire-reports, part61-browser.log,
browser-results/report.json e institution.png. São ignoradas e reproduzíveis;
clean as remove. Builds, caches e uploads de testes não integram os candidatos.

Não foi identificada vulnerabilidade crítica na revisão realizada; isso não
substitui scanner atualizado ou teste de penetração. Não declarar produção pronta.
