# Execute interativamente no seu PowerShell. Nunca cole credenciais no chat.
# Wrapper do bootstrap Java existente; exclusivo do projeto de validacao.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
# Tratar explicitamente exit codes, inclusive com a preferencia nativa do PS 7 ativa.
$PSNativeCommandUseErrorActionPreference = $false
$docker = Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe'
$repo = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $env:USERPROFILE 'bemfeito-docker-teste.env'
$override = Join-Path $env:USERPROFILE 'bemfeito-docker-local.override.yaml'
$project = 'bemfeito-validacao'
$temporary = 'bemfeito-validacao-bootstrap-once'
$variables = @('APP_BOOTSTRAP_ADMIN_NAME','APP_BOOTSTRAP_ADMIN_EMAIL','APP_BOOTSTRAP_ADMIN_PASSWORD')
foreach ($path in @($docker,$envFile,$override)) {
    if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Arquivo ausente: $path" }
}
foreach ($variable in $variables) {
    if (Test-Path "Env:$variable") { throw 'Remova variaveis de bootstrap preexistentes antes de continuar.' }
}
function Assert-Service($name, $service) {
    $details = & $docker inspect --format '{{json .Config.Labels}}' $name
    if ($LASTEXITCODE) { throw 'Container esperado ausente.' }
    $labels = $details | ConvertFrom-Json
    if ($labels.'com.docker.compose.project' -ne $project -or $labels.'com.docker.compose.service' -ne $service) {
        throw 'Container fora do projeto de validacao.'
    }
}

function Get-AdminCount {
    # Shell e SQL via stdin: evita a perda de aspas no argv do Windows PowerShell 5.1.
    # Here-string literal: somente o shell do container expande MYSQL_PASSWORD/USER.
    $query = @'
export MYSQL_PWD="$MYSQL_PASSWORD"
exec mysql --user="$MYSQL_USER" --database=bem_feito --batch --skip-column-names <<'SQL'
SELECT COUNT(*) FROM users WHERE platform_role = 'PLATFORM_ADMIN';
SQL
'@
    $result = ($query.Replace("`r`n", "`n") + "`n") | & $docker exec -i bemfeito-validacao-mysql-1 sh -s 2>$null
    $queryExit = $LASTEXITCODE
    $count = 0
    if ($queryExit -ne 0 -or @($result).Count -ne 1 -or
        "$result" -notmatch '^\d+$' -or ![int]::TryParse("$result", [ref]$count)) {
        throw 'Consulta de administradores falhou.'
    }

    return $count
}

Assert-Service 'bemfeito-validacao-mysql-1' 'mysql'
Assert-Service 'bemfeito-validacao-api-1' 'api'
if ((Get-AdminCount) -ne 0) { throw 'Ja existe administrador. Conta preservada; investigue antes de continuar.' }
$compose = @('compose','--project-name',$project,'--env-file',$envFile,'-f',(Join-Path $repo 'compose.yaml'),'-f',$override)
& $docker @compose config --quiet 2>$null
if ($LASTEXITCODE) { throw 'Configuracao Compose invalida.' }
$configurationJson = & $docker @compose config --format json 2>$null
$configExit = $LASTEXITCODE
try {
    if ($configExit) { throw 'Falha Compose.' }
    $configuration = $configurationJson | ConvertFrom-Json -ErrorAction Stop
} catch {
    # Nao propagar erros de parsing: podem incluir o JSON com segredos interpolados.
    throw 'Nao foi possivel validar a configuracao Compose.'
} finally {
    $configurationJson = $null
}
if ($configuration.services.api.environment.DB_URL -notmatch '^jdbc:mysql://mysql:3306/bem_feito\?') {
    throw 'Destino do banco diferente do MySQL Docker esperado.'
}
$configuration = $null
$bootstrapOverride = "services:`n  api:`n    networks: !override [database]`n    logging:`n      driver: none`n      options: !reset {}`n"
$containerId = $null
$password = $null
$confirmation = $null
$secure = $null
$secureConfirmation = $null
try {
    $env:APP_BOOTSTRAP_ADMIN_NAME = Read-Host 'Nome do administrador real'
    $env:APP_BOOTSTRAP_ADMIN_EMAIL = Read-Host 'Email do administrador real'
    $secure = Read-Host 'Senha nova forte (16+ caracteres, ate 72 bytes UTF-8)' -AsSecureString
    $secureConfirmation = Read-Host 'Repita a senha' -AsSecureString
    $password = [System.Net.NetworkCredential]::new('', $secure).Password
    $confirmation = [System.Net.NetworkCredential]::new('', $secureConfirmation).Password
    if ($password -cne $confirmation -or $password.Length -lt 16 -or [Text.Encoding]::UTF8.GetByteCount($password) -gt 72 -or
        $password -cnotmatch '[a-z]' -or $password -cnotmatch '[A-Z]' -or $password -notmatch '[0-9]' -or $password -notmatch '[^a-zA-Z0-9]') {
        throw 'Senhas devem coincidir e ter 16+ caracteres, maiuscula, minuscula, numero e simbolo; limite 72 bytes.'
    }
    $env:APP_BOOTSTRAP_ADMIN_PASSWORD = $password
    # Revalidar apos a interacao; nunca alterar conta ja existente.
    if ((Get-AdminCount) -ne 0) { throw 'Ja existe administrador. Bootstrap cancelado.' }
    # Nenhum segredo como argumento. Sem portas publicadas ou logs persistentes.
    $runOutput = $bootstrapOverride | & $docker @compose -f - run --detach --rm --no-deps --name $temporary `
        -e SPRING_PROFILES_ACTIVE=bootstrap -e APP_BOOTSTRAP_ADMIN_ENABLED=true `
        -e APP_SESSION_COOKIE_SECURE=true -e LOGGING_LEVEL_ROOT=OFF `
        -e APP_BOOTSTRAP_ADMIN_NAME -e APP_BOOTSTRAP_ADMIN_EMAIL -e APP_BOOTSTRAP_ADMIN_PASSWORD api 2>$null
    $runExit = $LASTEXITCODE
    if ($runExit -or @($runOutput).Count -ne 1 -or "$runOutput" -notmatch '^[a-f0-9]{64}$') {
        throw 'Inicializacao nao confirmada. Inspecione apenas nome/status do temporario antes de repetir.'
    }
    $containerId = "$runOutput"
    foreach ($variable in $variables) { Remove-Item "Env:$variable" -ErrorAction SilentlyContinue }
    $password = $null
    $confirmation = $null
    $created = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ((Get-AdminCount) -eq 1) { $created = $true; break }
        Start-Sleep -Seconds 2
    }
    if (!$created) { throw 'Criacao nao confirmada. Nao repita antes de investigar.' }
    Write-Host 'Administrador confirmado exclusivamente no MySQL Docker de validacao.'
} finally {
    foreach ($variable in $variables) { Remove-Item "Env:$variable" -ErrorAction SilentlyContinue }
    $password = $null
    $confirmation = $null
    if ($secure) { $secure.Dispose() }
    if ($secureConfirmation) { $secureConfirmation.Dispose() }
    if ($containerId) {
        # ID retornado por esta execucao; nunca parar outro container pelo nome reutilizavel.
        & $docker stop --time 15 $containerId *> $null
        if ($LASTEXITCODE) { Write-Warning 'Confira o encerramento do container temporario; nao execute docker logs/inspect completo.' }
    }
}
