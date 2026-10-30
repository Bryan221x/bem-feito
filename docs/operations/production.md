# Parte 6.1 — instalação e operação

Esta preparação não é um deploy nem uma certificação de produção. Consulte o
[relatório e critérios pendentes](part-6.1-report.md). Nunca executar limpeza do
MySQL local para testar estes procedimentos.

## Topologia e limites

Uma VPS Linux com Docker Engine e Compose v2, domínio e portas 80/443: Caddy serve
somente index/pages/css/js/assets e encaminha `/api` ao Spring. MySQL fica numa
rede interna, sem porta publicada. Somente Caddy publica portas. API executa como
UID 10001, filesystem somente leitura, `/tmp` efêmero e volume de uploads gravável.
Uma única instância mantém sessões em memória; reiniciar exige novo login. Isso
não apaga contas, dados ou fotos. Não escalar para múltiplas APIs sem planejar
sessões compartilhadas e storage compartilhado. SMTP exige saída de rede da API.

Caddy foi escolhido por automatizar emissão/renovação TLS e proxy sem dependência
de plug-ins. `APP_DOMAIN` deve ser um hostname controlado pelo operador, sem
protocolo/path; nenhum domínio foi escolhido ou comprado. DNS A/AAAA, firewall,
certificados, limites de tráfego e custos devem ser conferidos na hospedagem.
Dimensionamento inicial para ensaio: 2 vCPU, 4 GiB RAM, SSD; medir CPU, heap e disco
antes de aceitar carga real. Não é garantia de capacidade nem orçamento comercial.

Imagens oficiais consultadas em 28/10/2026: Temurin
`25.0.4.1_1-jdk-noble`/`25.0.4.1_1-jre-noble`, MySQL `8.4.11` e Caddy `2.11.7-alpine`.
Fontes: [Temurin](https://hub.docker.com/_/eclipse-temurin),
[MySQL](https://hub.docker.com/_/mysql), [Caddy](https://hub.docker.com/_/caddy).
Versões explícitas reduzem variação; tags e pacotes apt ainda podem mudar. Antes
de publicar, registrar RepoDigests, executar scanner de vulnerabilidades atualizado
e fixar os digests aprovados. Não foi possível executar pull/build sem Docker neste
host. O Maven Wrapper fixa Maven 3.9.16 e o parent fixa Spring Boot 4.1.1; nenhum
segredo é argumento de build, layer ou arquivo copiado para a imagem.

## Variáveis externas

Fornecer por ambiente ou arquivo de secrets fora do checkout com permissão 0600.
Compose recebe `--env-file /caminho/externo/production.env` ou ambiente exportado.
Não executar `docker compose config` sem `--quiet` em logs compartilhados: pode
expandir senhas. Administradores do Docker podem ler variáveis dos containers.

| Variável | Configuração |
| --- | --- |
| APP_DOMAIN | Hostname real, sem `https://` ou barra |
| ACME_EMAIL | E-mail operacional para ACME |
| DB_PASSWORD | Segredo novo da conta restrita `bemfeito` do container |
| MYSQL_ROOT_PASSWORD | Segredo distinto para operação inicial/backup, nunca usado pela API |
| APP_MAIL_ENABLED | false até SMTP autorizado/configurado; modo manual explícito |
| APP_MAIL_FROM | Remetente autorizado pelo provedor |
| SPRING_MAIL_HOST/PORT | SMTP; porta padrão 587 |
| SPRING_MAIL_USERNAME/PASSWORD | Credenciais SMTP externas |
| APP_SMTP_STARTTLS | true por padrão; obrigatório com SMTP sem TLS implícito |
| APP_SMTP_SSL | false; true para TLS implícito, normalmente porta 465 |

Compose define DB_URL, DB_USER, APP_UPLOAD_DIR, APP_FRONTEND_BASE_URL,
APP_CORS_ALLOWED_ORIGINS e SPRING_PROFILES_ACTIVE=prod. Não copiar credenciais do
MySQL pessoal. TLS do MySQL está desabilitado apenas no segmento Docker privado da
mesma máquina; banco externo requer URL com verificação TLS/CA e rede protegida.
O usuário de runtime tem SELECT/INSERT/UPDATE/DELETE, sem DDL. Operador usa conta
separada para migrações. Habilitar backups antes de cadastrar dados reais.

## Banco novo e primeira inicialização (VPS futura)

Na raiz do checkout, com variáveis exportadas de forma segura:

```sh
docker compose config --quiet
docker compose build --pull
docker compose run --rm --no-deps frontend caddy validate --config /etc/caddy/Caddyfile
docker compose up -d mysql
docker compose ps
```

Somente um volume MySQL vazio executa schema.sql (21 tabelas), reference-data.sql,
cities.sql (5.571 entradas) e a restrição de privilégios. O schema completo já
contém a 004. O healthcheck exige a carga de cidades. Se inicialização falhar,
investigar logs e estado parcial antes de agir; não reiniciar scripts cegamente,
não usar `--force` nem apagar volumes de instalações com dados.

Criar primeiro administrador em container isolado, sem portas publicadas, sem
frontend ativo. Exportar APP_BOOTSTRAP_ADMIN_NAME, APP_BOOTSTRAP_ADMIN_EMAIL e
APP_BOOTSTRAP_ADMIN_PASSWORD apenas para esta execução. O perfil de bootstrap
usa defaults existentes e sobrescreve explicitamente cookie/flag; a API normal
continua com bootstrap proibido no perfil prod:

```sh
docker compose run --rm --no-deps -e SPRING_PROFILES_ACTIVE=bootstrap \
  -e APP_BOOTSTRAP_ADMIN_ENABLED=true -e APP_SESSION_COOKIE_SECURE=true \
  -e APP_BOOTSTRAP_ADMIN_NAME -e APP_BOOTSTRAP_ADMIN_EMAIL \
  -e APP_BOOTSTRAP_ADMIN_PASSWORD api
```

Aguardar startup sem erro e confirmar a existência de exatamente um administrador
pelo cliente MySQL local ao container (não imprimir hash). Encerrar com Ctrl+C;
o processo não tem porta publicada. Remover as três variáveis e a flag do ambiente.
Bootstrap preserva administrador já existente e não promove USER. Nunca manter
esse comando em supervisor, cron ou restart policy.

```sh
unset APP_BOOTSTRAP_ADMIN_NAME APP_BOOTSTRAP_ADMIN_EMAIL APP_BOOTSTRAP_ADMIN_PASSWORD APP_BOOTSTRAP_ADMIN_ENABLED
docker compose up -d api frontend
docker compose ps
curl --fail "https://$APP_DOMAIN/api/health"
```

Validar login/CSRF/cookie Secure/HttpOnly/Lax, logout, cadastro, imagens e páginas
online. Caddy obtém certificado após DNS/portas corretos. Não executar os comandos
de publicação neste trabalho. Não montar a raiz do repositório como site público.

## Banco existente e migração 004

Nunca importar schema.sql em banco existente. Fazer backup coordenado, ensaiar em
cópia isolada e executar `deploy/migration-status.sql` apenas para leitura. Conferir
as expressões geradas open_cnpj/pending_email e os dois índices UNIQUE com suas
colunas/ordem. Se ambos completos e equivalentes: 004 já aplicada, não reaplicar.
Se ambos ausentes e sem duplicatas: operador autorizado aplica 004 uma única vez
com cliente que interrompe no primeiro erro. Estado parcial/divergente ou duplicatas:
parar e preparar reparo específico; DDL MySQL não é transação reversível integral.
Registrar versão/hash do script, data, operador e resultado fora de segredos.
O MySQL local já tem 004; esta Parte não executa migração nem modifica schema.

## Atualização, falha e observabilidade

Antes de atualizar: testes, scanner, backup e ensaio de restauração. Anotar imagem
anterior/digest e versões do schema. Executar `docker compose build --pull`, conferir
migrações pendentes e `docker compose up -d --no-deps api frontend`. Há breve
indisponibilidade e sessões expiram; avisar para salvar formulários antes. Não
reverter código incompatível com schema sem plano de recuperação ensaiado.

`docker compose ps`, `docker compose logs --since 15m --tail 200 api mysql frontend`
e `/api/health` (200 com SELECT 1, 503 sem detalhes se banco indisponível) dão sinais
básicos. Logs Docker rotacionam 3×10 MiB por serviço. Não habilitar debug JDBC,
binds SQL, Spring Security, mail, dumps de body/cookies/headers. Caddy não habilita
access log nesta configuração; assim URLs/fragmentos não viram logs operacionais.
Monitorar externamente HTTPS/health, disco, idade dos backups, 5xx, memória e
certificado. Healthcheck unhealthy não provoca restart sozinho: alertar/investigar.
Política unless-stopped reinicia processos encerrados, não repara dados.

Login 10/min/IP; formulários públicos compartilham 10/10min/IP; convites 20/min/IP;
CSRF 60/min/IP, resposta 429 + Retry-After. Limites em memória, no máximo 10.000
chaves, reiniciam com a API; NAT compartilha orçamento. Não substituem proteção
DDoS de borda nem controle distribuído. Proxy é o único acesso à API; não publicar
8080. Tomcat processa forwarded headers de proxies privados; não aceitar proxy
arbitrário na rede. Cookies e autorização permanecem independentes desses limites.

Erros de rede não repetem escrita automaticamente. Em resultado incerto, recarregar
o recurso antes de reenviar. Operações de recursos distintos não são atômicas.
SMTP após commit pode falhar: aprovação permanece; corrigir SMTP e reemitir convite
para conta INVITED. Timeouts 5s, TLS com verificação de hostname, sem e-mail real
enviado nesta etapa. Convites expiram e reemissão invalida anteriores. Sem fila
durável: monitorar avisos de entrega e orientar operação manual quando desabilitado.

## Backup e restauração

Executar `BACKUP_DIR=/diretorio/externo sh deploy/backup.sh` com ambiente Compose.
O script para frontend/API, deixa MySQL ativo, faz dump consistente e tar de uploads,
grava SHA256SUMS e retoma apenas serviços anteriormente ativos, inclusive em falha.
Não imprime secrets. Falha produz conjunto incompleto: não tratá-lo como backup.
Validar checksums e SQL/tar; guardar também versão das imagens/configuração e
manifesto de migrações. Secrets e certificados precisam cópia protegida separada.

Recomendação inicial: diário e antes de atualização; sete diários, quatro semanais,
três mensais, ajustados à política de dados. Criptografar antes da cópia fora da VPS,
restringir acesso, guardar chave de recuperação separadamente. Sem retenção
automática destrutiva. RPO inicial até 24h; RTO só pode ser afirmado após ensaio.

Ensaio obrigatório em OUTRA VPS/VM ou projeto Compose isolado, sem volumes externos
da produção: definir COMPOSE_PROJECT_NAME=bemfeito-restore e segredos novos, iniciar
somente mysql. Conferir `docker compose ls` e volumes do projeto. Antes do restore,
validar `sha256sum -c SHA256SUMS`; nunca usar `down -v` no projeto real.

```sh
# Somente no projeto novo e descartável já identificado pelo operador:
docker compose up -d mysql
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root bem_feito' < /backup/database.sql
docker compose run --rm --no-deps -T --entrypoint tar api -C /var/lib/bemfeito/uploads -xf - < /backup/uploads.tar
```

O dump contém DROP/CREATE das tabelas: esses comandos são DESTRUTIVOS no destino e
só se aplicam ao ambiente descartável explicitamente conferido. Usar arquivo tar
do próprio backup com checksum válido, não arquivo arbitrário. Conferir ownership
UID 10001 e autorização dos arquivos, contagens de 21 tabelas, FKs, 5.571 cidades,
admin, referências de imagem, Hibernate validate, login e imagens após reinício.
Não iniciar frontend com domínio de produção no ensaio; usar hostname isolado e
certificado apropriado. Não enviar SMTP real. Registrar duração/resultado. O ensaio
de Docker/restore não foi executado aqui por ausência de Docker.

## Reconciliação dos uploads

APP_UPLOAD_DIR é /var/lib/bemfeito/uploads no volume `uploads`, nunca assets. JPEG,
PNG e WebP são decodificados, limitados a 5 MiB/16 milhões de pixels e recodificados
para PNG. Multipart limitado a 6 MiB. Mídia exige referência no banco; arquivo órfão
não é público. Exclusão após commit/rollback funciona em operação normal, mas
queda ou erro de filesystem pode deixar órfãos. Nunca usar limpeza por idade apenas.

Fazer backup e parar TODOS os escritores da API. Exportar `deploy/upload-references.sql`
em modo batch/raw/skip-column-names; copiar o volume para área de auditoria protegida,
mantendo a API parada até finalizar a decisão. Executar:

```sh
python database/scripts/audit_uploads.py --root /copia/uploads --references /copia/references.jsonl > /copia/reconciliation.json
```

O programa só lê; informa missing, orphanCandidates com SHA-256, unknown e
referencedFiles. Rejeita referência gerenciada inválida, não segue symlinks nem
apaga nada. Arquivo desconhecido exige investigação. Para órfãos confirmados,
conferir exportação completa e atual, relação owner/chave, hash e backup; operador
pode mover individualmente para quarentena externa com API parada. Não há script
de remoção automática. Manter quarentena até prazo/validação acordados. Ausentes
devem ser restaurados do backup correspondente, sem inventar URLs/IDs.
