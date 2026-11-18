$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'
$Base = $PSScriptRoot
$Server = Join-Path $Base 'servidor'
$Lock = $null

function Get-VerifiedFile {
    param([string]$Url, [string]$File, [string]$Hash, [string]$Algorithm)
    if (Test-Path -LiteralPath $File) {
        if ((Get-FileHash -LiteralPath $File -Algorithm $Algorithm).Hash -ieq $Hash) { return }
        throw "Arquivo diferente do esperado, preservado: $File. Mova-o para outra pasta e tente novamente."
    }
    $Partial = $File + '.part-' + [Guid]::NewGuid().ToString('N')
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Partial
        if ((Get-FileHash -LiteralPath $Partial -Algorithm $Algorithm).Hash -ine $Hash) {
            throw 'O arquivo recebido falhou na verificacao de integridade. Tente novamente.'
        }
        Move-Item -LiteralPath $Partial -Destination $File
    } finally {
        if (Test-Path -LiteralPath $Partial) { Remove-Item -LiteralPath $Partial -Force }
    }
}

Push-Location $Base
try {
    if (-not [Environment]::Is64BitOperatingSystem) { throw 'Este pacote requer Windows 10 ou 11 de 64 bits.' }
    try {
        $Lock = [IO.File]::Open((Join-Path $Base '.servidor.lock'), [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    } catch { throw 'Nao foi possivel iniciar. Verifique se o servidor ja esta aberto e se voce pode gravar nesta pasta.' }
    $Settings = Get-Content -LiteralPath (Join-Path $Base 'config.json') -Raw | ConvertFrom-Json
    if ([string]$Settings.memoryMB -notmatch '^\d+$') { throw 'memoryMB deve ser um numero inteiro no config.json.' }
    $Ram = [int]$Settings.memoryMB
    if ($Ram -lt 512 -or $Ram -gt 8192) { throw 'Use memoryMB entre 512 e 8192 no config.json. Padrao: 1024.' }
    $Properties = Get-Content -LiteralPath (Join-Path $Server 'server.properties') -Raw
    if ($Properties -notmatch '(?m)^server-ip=127\.0\.0\.1\s*$') { throw 'Este pacote e local. Mantenha server-ip=127.0.0.1 em servidor/server.properties.' }
    if ($Properties -notmatch '(?m)^server-port=25565\s*$') { throw 'Mantenha server-port=25565 em servidor/server.properties.' }
    $Eula = Join-Path $Server 'eula.txt'
    $Accepted = $false
    if (Test-Path -LiteralPath $Eula) { $Accepted = (Get-Content -LiteralPath $Eula -Raw) -match '(?im)^\s*eula\s*=\s*true\s*$' }
    if (-not $Accepted) {
        Write-Host 'Leia a EULA do Minecraft: https://www.minecraft.net/eula' -ForegroundColor Cyan
        Write-Host 'Ao aceitar, voce concorda com os termos para executar o servidor.'
        $Answer = Read-Host 'Se voce leu e concorda, digite SIM. Enter cancela'
        if ($Answer.Trim() -ine 'SIM') { Write-Host 'Cancelado. A EULA nao foi aceita.'; exit 0 }
        Set-Content -LiteralPath $Eula -Value 'eula=true' -Encoding Ascii
    }
    $Cache = Join-Path $Base '.downloads'
    New-Item -ItemType Directory -Path $Cache -Force | Out-Null
    $Jar = Join-Path $Server 'server.jar'
    Write-Host 'Verificando servidor oficial Minecraft 1.8.9...'
    Get-VerifiedFile -Url 'https://launcher.mojang.com/v1/objects/b58b2ceb36e01bcd8dbf49c8fb66c55a9f0676cd/server.jar' -File $Jar -Hash 'b58b2ceb36e01bcd8dbf49c8fb66c55a9f0676cd' -Algorithm 'SHA1'
    $Runtime = Join-Path $Base 'runtime-java8'
    $Java = $null
    if (Test-Path -LiteralPath $Runtime) {
        $Java = Get-ChildItem -LiteralPath $Runtime -Filter 'java.exe' -Recurse -File | Where-Object { $_.Directory.Name -eq 'bin' } | Select-Object -First 1
    }
    if ($null -eq $Java) {
        Write-Host 'Baixando Java 8 portatil oficial Eclipse Temurin. Aguarde...'
        $Zip = Join-Path $Cache 'temurin8u512-win-x64.zip'
        Get-VerifiedFile -Url 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u512-b09/OpenJDK8U-jre_x64_windows_hotspot_8u512b09.zip' -File $Zip -Hash 'ae1ae12f817d7735477c6555107eece73854da0654da69077309ef9bff0b78bf' -Algorithm 'SHA256'
        $Staging = Join-Path $Base ('.java-extraindo-' + [Guid]::NewGuid().ToString('N'))
        Expand-Archive -LiteralPath $Zip -DestinationPath $Staging
        if (-not (Get-ChildItem -LiteralPath $Staging -Filter 'java.exe' -Recurse -File | Where-Object { $_.Directory.Name -eq 'bin' } | Select-Object -First 1)) { throw 'Java nao encontrado no pacote baixado.' }
        if (Test-Path -LiteralPath $Runtime) { Move-Item -LiteralPath $Runtime -Destination ($Runtime + '.incompleto-' + [Guid]::NewGuid().ToString('N')) }
        Move-Item -LiteralPath $Staging -Destination $Runtime
        $Java = Get-ChildItem -LiteralPath $Runtime -Filter 'java.exe' -Recurse -File | Where-Object { $_.Directory.Name -eq 'bin' } | Select-Object -First 1
    }
    Write-Host ''
    Write-Host 'Iniciando Minecraft Java 1.8.9 em localhost:25565' -ForegroundColor Green
    Write-Host 'Espere aparecer Done. Para salvar e encerrar, digite stop e pressione Enter.'
    Write-Host 'No jogo: Multijogador > Conexao direta > localhost'
    Write-Host 'Somente este PC pode entrar. Perfil offline habilitado.'
    Set-Location $Server
    & $Java.FullName '-Xms256M' "-Xmx${Ram}M" '-jar' $Jar 'nogui'
    if ($LASTEXITCODE -ne 0) { throw 'O servidor encerrou com erro. Consulte servidor/logs/latest.log.' }
} catch {
    Write-Host ''
    Write-Host ('Erro: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    if ($null -ne $Lock) { $Lock.Dispose() }
    Pop-Location
}
