# Parte 6.2 — validação local (30/10/2026)

Status: revisão de fechamento da Parte 6.2. HTTPS, autenticação, persistência após
recriação do MySQL, backup e restauração isolada realizados e aprovados conforme
confirmação do operador nesta revisão. Administrador já existente; não repetir bootstrap.
As pendências específicas abaixo permanecem sem comprovação detalhada.
Não constitui validação em produção nem início de uma nova etapa.

## Resultados reais confirmados pelo operador

Estes resultados foram informados pelo operador; não foram reexecutados nesta revisão:

| Verificação | Resultado |
| --- | --- |
| HTTPS real | Aprovado; evidência anterior com Node/CA detalhada abaixo |
| Autenticação real | Aprovada; administrador já existe |
| Persistência após recriação do MySQL | Aprovada |
| Backup e restauração em ambiente isolado | Aprovados |
| Banco restaurado | 21 tabelas, 1 `PLATFORM_ADMIN` ativo e 5.571 cidades |
| Volume de imagens | Restaurado; volume vazio |

A restauração do volume vazio comprova esse cenário, mas não comprova recuperação
de arquivos de imagens existentes. Não foram criados dados fictícios para ampliar o teste.

## Ambiente comprovado

- Projeto Compose: `bemfeito-validacao`; mysql, api e frontend saudáveis.
- Docker: `C:\Users\BRYAN\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe`.
- Env externo: `C:\Users\BRYAN\bemfeito-docker-teste.env` (conteúdo não registrado).
- Override: `C:\Users\BRYAN\bemfeito-docker-local.override.yaml`.
- Apenas frontend publica portas: 127.0.0.1:80 e 127.0.0.1:443.
- Consulta somente leitura no container `bemfeito-validacao-mysql-1`: zero
  administradores antes do bootstrap (registro histórico, não estado atual).
  Nenhum hash ou identificador pessoal exibido.
- Bootstrap existente: AdminBootstrapRunner/Service; BCrypt custo 12, preserva
  conta existente e recusa segundo administrador. Perfil prod proíbe bootstrap.
- Cookies prod: Secure, HttpOnly, SameSite=Lax; CSRF permanece ativo.

## Certificado

Raiz existente: `/data/caddy/pki/authorities/local/root.crt` no frontend.
Identidade: `CN=Caddy Local Authority - 2026 ECC Root`.
Validade: 30/10/2026 00:06:47 a 07/09/2036 00:06:47 (America/Sao_Paulo).

- SHA-256: `670E70B398A6BD0CDF3E13D506DD5F655B10F364335548E604C6FBA8106E8767`.
- Thumbprint SHA-1 para localizar no Windows: `3100252035B90C520DEFB1D2FDE271E28E55420D`.

Após autorização explícita do usuário no chat, a raiz foi obtida do container,
teve SHA-256 conferido e foi adicionada somente a `Cert:\CurrentUser\Root`.
Nenhuma chave privada foi exportada; nenhuma nova CA foi gerada.

Certificado folha: `/data/caddy/certificates/local/localhost/localhost.crt`, SAN
DNS `localhost`, emitido por `Caddy Local Authority - ECC Intermediate`, válido
na inspeção de 30/10/2026 00:06:47 a 12:06:47. Renovação altera a folha.

Antes: curl/Schannel retornava SEC_E_UNTRUSTED_ROOT. Após a instalação passou a
retornar 0x80092012 (checagem de revogação indisponível). Não foram usadas opções
para ignorar certificado ou revogação. Node HTTPS com a CA explicitamente
fornecida validou a cadeia e hostname (`socket.authorized=true`). Isso não prova
o estado de confiança do Chrome nem resolve a limitação de revogação do Schannel.
Chrome estava minimizado; a ferramenta falhou ao capturá-lo, inclusive após uma
tentativa de recuperação. Operador deve restaurar e recarregar para conferir.

Para remover futuramente **somente esta confiança**, por decisão do operador:

```powershell
Remove-Item -LiteralPath 'Cert:\CurrentUser\Root\3100252035B90C520DEFB1D2FDE271E28E55420D'
```

Isso não apaga a CA/volumes do Caddy; certificados emitidos por ela deixarão de
ser confiáveis para esse usuário. Não executar a remoção durante a validação.

## Correções e testes anteriores

Nenhum código da aplicação ou configuração de produção foi alterado.
Os mocks de location em auth.test.mjs e integration.test.mjs não informavam a
porta 5500 do Live Server e causavam duas falhas. Foram corrigidos e acrescentada
regressão para API e imagens na mesma origem HTTPS localhost.

- Frontend: 46 testes, 45 aprovados, zero falhas, 1 browser opt-in ignorado.
- Backend: 161 testes, zero falhas/erros, 2 ignorados; H2 isolado.
- Python: 6 testes aprovados.
- HTTPS com Node e CA explícita: Home, todas as 16 páginas HTML e health: 200;
  `/api/auth/me` e `/api/admin/summary` sem sessão: 401.
- HTML administrativo estático responde 200 por projeto; autorização dos dados
  ocorre na API e guard frontend. Renderização/redirect real ainda pendentes.
- Verificação de referências locais do frontend aprovada na suíte.

A primeira execução Maven foi bloqueada pelo cache C:\.m2; execução com cache
existente do usuário foi concluída. Log em backend/target (ignorado pelo Git).
Certificados públicos de inspeção também estão em backend/target.

## Revisão do bootstrap privado

`deploy/bootstrap-validation.ps1` envolve o bootstrap Java existente. Solicita
credenciais no PowerShell do próprio operador (senha oculta), confere projeto,
serviços, destino mysql:3306/bem_feito e ausência de administrador, exige senha
com 16+ caracteres/até 72 bytes, inicia contêiner temporário sem portas publicadas,
somente na rede database e com driver de logs none. Usa variáveis de ambiente
transitórias, confirma apenas contagem e encerra o contêiner --rm no finally,
usando o ID devolvido pela própria execução, nunca somente o nome reutilizável.
Administradores do Docker podem acessar ambiente de contêiner enquanto ele existe.
Não transcrever o terminal, imprimir ambiente ou inspecionar configuração completa.
O registro anterior informa validação da configuração temporária com config --quiet.
O administrador já existe. A versão corrigida do script não foi executada contra
Docker/MySQL nesta revisão; não repetir o bootstrap para validar essa versão.

Correções desta revisão:

- Shell e SQL enviados por stdin a `sh -s`, com here-string literal, delimitador
  SQL e quebras LF. Isso evita depender de aspas internas em argumentos nativos
  do Windows PowerShell. Senha e usuário são expandidos somente no contêiner.
- Exit codes capturados antes de processar respostas; contagem deve ser uma única
  linha numérica representável como inteiro. Falhas abortam o fluxo.
- JSON do Compose processado com erro sanitizado, sem reproduzir configuração
  interpolada; stderr do Compose suprimido e erros comunicados sem segredos.
- Ausência de administrador conferida novamente após a interação privada.
- Limpeza do ambiente e descarte das SecureStrings no finally; encerramento
  restrito ao ID retornado pela execução, validado como hexadecimal de 64 caracteres.

Não há senha, token ou credencial de acesso fixa no script. Nomes de projeto,
contêiner, banco e caminhos locais são identificadores operacionais. Não há SQL de
alteração/exclusão, remoção de volumes, reset de senha ou substituição de contas.
O propósito do bootstrap continua sendo criar o primeiro administrador quando
inexistente; por isso ele **não deve ser executado no ambiente atual**.
Segredos permanecem transitoriamente na memória e no ambiente do processo/contêiner;
limpar referências não garante apagar memória. O JSON do Compose também existe
transitoriamente em memória, sem ser gravado ou exibido pelo script.

## Verificações desta revisão

- Parser PowerShell: nenhuma falha de sintaxe.
- `git diff --check` aprovado; espaços finais dos dois arquivos ainda não
  rastreados conferidos separadamente, sem ocorrências.
- Função `Get-AdminCount` extraída por AST e testada isoladamente com Docker
  simulado: 7 casos aprovados (zero, um, texto inválido, exit code de erro,
  múltiplas linhas, overflow e ausência de saída). Argumentos, aspas e LF conferidos.
- Frontend: 45 testes aprovados, zero falhas, executando explicitamente os quatro
  arquivos abaixo. Browser opt-in não executado; backend e Python não reexecutados.
- Nenhum comando Docker, bootstrap real, acesso ao banco ou restauração executado
  nesta revisão. A simulação não comprova execução nativa em todas as versões do
  PowerShell/Docker; compatibilidade real da versão corrigida permanece não exercitada.

Reprodução apenas das verificações não destrutivas de frontend e Git:

```powershell
Set-Location 'C:\Users\BRYAN\Desktop\bem-feito'
node --test tests/frontend/auth.test.mjs tests/frontend/integration.test.mjs tests/frontend/requests.test.mjs tests/frontend/references.test.mjs
git diff --check
git status --short
```

Inspeção de serviços sem expandir segredos:

```powershell
$docker = 'C:\Users\BRYAN\AppData\Local\Programs\DockerDesktop\resources\bin\docker.exe'
& $docker compose ls --format json
& $docker ps --filter label=com.docker.compose.project=bemfeito-validacao --format '{{.Names}} {{.Status}} {{.Ports}}'
```

## Pendências para encerramento

HTTPS, autenticação, persistência, backup e restauração não estão mais pendentes.
A confirmação geral de autenticação não discrimina cada cenário: faltam registros
específicos para login pelo formulário, dashboard, `/auth/me` autenticado,
persistência da sessão entre páginas, atributos do cookie real, CSRF positivo e
negativo, credenciais inválidas, logout, 401 posterior e bloqueio após logout.
Esses subitens permanecem pendentes de comprovação individual, sem inferir falha.
Também permanece pendente a conferência visual de páginas públicas/gestão, console,
recursos externos, mapa e confiança no Chrome, além da limitação Schannel registrada.
Nenhum fluxo real deve ser declarado aprovado com base apenas nos testes H2/mocks.

MySQL principal Windows não foi acessado nem modificado. Nenhum segredo foi escrito
no repositório. Não houve deploy, commit, push, limpeza de volumes ou alterações
destrutivas nesta revisão. Persistência/backup/restauração já foram realizados
anteriormente, conforme confirmação do operador. Nenhuma nova etapa foi iniciada.
