#Requires -Version 5.1
<#
.SYNOPSIS
    UniEvent admin CLI - run admin tools against a local or remote server.

.USAGE
    ./tools.ps1 <command> [options]

.COMMANDS
    setup           Check dependencies and configure the local dev environment
    vault           Initialize and/or unseal Vault (run after docker compose up)
    unseal          Quick unseal Vault (shortcut for restart)
    seed            Seed test data into the database
    clear           Clear all SEED_ prefixed test data
    ingest          (future) Trigger manual Facebook ingestion
    refresh-tokens  (future) Refresh Facebook page tokens

.OPTIONS
    -r, --remote <url>  Target server URL (default: https://localhost)
    -v, --verbose       Show request/response details
    -h, --help          Show this help message

.NOTES
    On Windows (PowerShell): use single-dash flags  (-r, -remote, -v, -h)
    On Linux/Mac (tools.sh): use double-dash flags  (--remote, --verbose, --help)
#>

param(
    [Parameter(Position = 0)]
    [string]$Command,

    [Alias("r")]
    [string]$Remote = "",

    [Alias("v")]
    [switch]$VerboseOutput,

    [Alias("h")]
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# PS5 compatibility: $IsWindows/$IsLinux/$IsMacOS are PS6+ only
if (-not (Get-Variable -Name IsWindows -Scope Global -ErrorAction SilentlyContinue)) {
    $IsWindows = $env:OS -eq "Windows_NT"
    $IsLinux   = $false
    $IsMacOS   = $false
}

# ── Output helpers ────────────────────────────────────────────────────────────

function Write-Info  { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Cyan }
function Write-Ok    { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Green }
function Write-Err   { param([string]$Msg) Write-Host "  ERROR: $Msg" -ForegroundColor Red }
function Write-Warn  { param([string]$Msg) Write-Host "  WARN: $Msg" -ForegroundColor Yellow }
function Write-Sep   { Write-Host ("-" * 50) -ForegroundColor DarkGray }
function Write-Step  { param([string]$Msg) Write-Host "`n  $Msg" -ForegroundColor White }

function Show-Help {
    $cli = if ($IsLinux -or $IsMacOS) { "./tools.sh" } else { "./tools" }

    Write-Host ""
    Write-Host "UniEvent Admin CLI" -ForegroundColor Cyan
    Write-Sep
    Write-Host "Usage: $cli <command> [options]"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  setup                 Check dependencies and configure local dev environment"
    Write-Host "  vault                 Initialize and/or unseal Vault (run after docker compose up)"
    Write-Host "  unseal                Quick unseal Vault (shortcut for restart)"
    Write-Host "  seed                  Seed test data (2 pages, 10 events, 2 places)"
    Write-Host "  clear                 Remove all SEED_ prefixed records"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -r, --remote [<url>]  Target a remote server (default: https://localhost)"
    Write-Host "  -v, --verbose         Show request/response details"
    Write-Host "  -h, --help            Show this help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  $cli setup"
    Write-Host "  $cli seed"
    Write-Host "  $cli clear"
    Write-Host "  $cli seed --remote https://staging.example.com"
    Write-Host ""
}

# ── Load .env ─────────────────────────────────────────────────────────────────

function Load-DotEnv {
    $envFile = Join-Path $PSScriptRoot ".env"
    if (-not (Test-Path $envFile)) {
        Write-Err ".env file not found"
        Write-Warn "Ask the dev team for the .env file and place it in: $PSScriptRoot"
        exit 1
    }

    $vars = @{}
    foreach ($line in Get-Content $envFile) {
        $line = $line.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { continue }
        if ($line -match "^([^=]+)=(.*)$") {
            $vars[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $vars
}

function Update-EnvVar {
    param([string]$Key, [string]$Value)
    $envFile = Join-Path $PSScriptRoot ".env"
    if (-not (Test-Path $envFile)) { return }

    $lines = @(Get-Content $envFile)
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$([regex]::Escape($Key))=") {
            $lines[$i] = "$Key=$Value"
            $found = $true
            break
        }
    }
    if (-not $found) { $lines += "$Key=$Value" }
    $lines | Set-Content $envFile
}

# ── Web request wrapper (handles self-signed certs for localhost) ─────────────

function Invoke-Web {
    param([string]$Method = "GET", [string]$Uri, [hashtable]$Headers = @{}, [int]$TimeoutSec = 30)
    $skipCert = $Uri -match "^https://localhost"
    if ($PSVersionTable.PSVersion.Major -ge 6) {
        return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers `
            -TimeoutSec $TimeoutSec -ErrorAction Stop -SkipCertificateCheck:$skipCert
    }
    # PS 5: use a properly typed delegate - plain scriptblock coercion is unreliable
    try {
        if ($skipCert) {
            [System.Net.ServicePointManager]::ServerCertificateValidationCallback =
                [System.Net.Security.RemoteCertificateValidationCallback]{ param($s, $c, $ch, $e) $true }
        }
        return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -TimeoutSec $TimeoutSec -ErrorAction Stop
    } finally {
        if ($skipCert) { [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $null }
    }
}

# ── Health check ──────────────────────────────────────────────────────────────

function Test-ServerHealth {
    param([string]$BaseUrl)
    if ($VerboseOutput) { Write-Info "Checking server at $BaseUrl" }
    try {
        Invoke-Web -Uri "$BaseUrl/actuator/health" -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        $ex = $_.Exception
        # PS7: HttpRequestException wraps an inner HttpRequestError for non-2xx
        # PS5: WebException with a Response means the server replied (even 401/403)
        if ($ex -is [System.Net.WebException] -and $null -ne $ex.Response) { return $true }
        if ($ex -is [System.Net.WebException] -and $ex.Status -eq [System.Net.WebExceptionStatus]::TrustFailure) { return $true }
        # PS7: HttpResponseException means the server replied with a non-2xx status
        if ($ex.GetType().Name -eq "HttpResponseException") { return $true }
        return $false
    }
}

# ── HTTP request helper ───────────────────────────────────────────────────────

function Invoke-AdminRequest {
    param([string]$Method, [string]$Url)

    $headers = @{ "Content-Type" = "application/json" }

    if ($VerboseOutput) {
        Write-Info "$Method $Url"
    }

    try {
        $resp = Invoke-Web -Uri $Url -Method $Method -Headers $headers -TimeoutSec 30
        return @{ StatusCode = $resp.StatusCode; Body = $resp.Content }
    } catch [System.Net.WebException] {
        $statusCode = [int]$_.Exception.Response.StatusCode
        $body = ""
        if ($null -ne $_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $body = $reader.ReadToEnd()
        }
        return @{ StatusCode = $statusCode; Body = $body }
    } catch {
        Write-Err "Request failed: $($_.Exception.Message)"
        exit 1
    }
}

function Handle-Response {
    param([hashtable]$Response, [string]$SuccessMsg)

    switch ($Response.StatusCode) {
        200 {
            Write-Ok $SuccessMsg
            if ($VerboseOutput -and $Response.Body) {
                Write-Host ""
                Write-Info "Response:"
                try {
                    $json = $Response.Body | ConvertFrom-Json
                    Write-Host ($json | ConvertTo-Json -Depth 5) -ForegroundColor Gray
                } catch {
                    Write-Host $Response.Body -ForegroundColor Gray
                }
            }
        }
        404 {
            Write-Err "Endpoint not found (404)"
            Write-Warn "Is the server running with the 'dev' profile?"
            Write-Warn "  ./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'"
            exit 1
        }
        500 {
            Write-Err "Server error (500)"
            if ($Response.Body) { Write-Warn $Response.Body }
            exit 1
        }
        default {
            Write-Err "Unexpected response: $($Response.StatusCode)"
            if ($Response.Body) { Write-Warn $Response.Body }
            exit 1
        }
    }
}

# ── Setup: executable discovery ───────────────────────────────────────────────

# Known off-PATH install locations per tool (Windows-first, cross-platform fallbacks added at runtime)
$script:KnownPaths = @{
    openssl = @(
        "$env:PROGRAMFILES\Git\usr\bin\openssl.exe"
        "$env:PROGRAMFILES\OpenSSL-Win64\bin\openssl.exe"
        "$env:PROGRAMFILES\OpenSSL\bin\openssl.exe"
        "${env:PROGRAMFILES(x86)}\OpenSSL-Win32\bin\openssl.exe"
        "/usr/bin/openssl"
        "/opt/homebrew/bin/openssl"
    )
    docker  = @(
        "$env:PROGRAMFILES\Docker\Docker\resources\bin\docker.exe"
        "/usr/bin/docker"
        "/usr/local/bin/docker"
        "/opt/homebrew/bin/docker"
    )
    curl    = @(
        "$env:SystemRoot\System32\curl.exe"
        "$env:PROGRAMFILES\Git\usr\bin\curl.exe"
        "/usr/bin/curl"
    )
}

function Find-Executable {
    param([string]$Name, [string[]]$Fallbacks = @())

    # -CommandType Application skips PowerShell aliases (e.g. curl -> Invoke-WebRequest)
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue -CommandType Application | Select-Object -First 1
    if ($cmd) { return $cmd.Source }

    foreach ($path in $Fallbacks) {
        if ($path -and (Test-Path $path -ErrorAction SilentlyContinue)) {
            Write-Warn "'$Name' is not on your PATH but was found at: $path"
            Write-Warn "Consider adding it to your PATH for convenience."
            return $path
        }
    }
    return $null
}

function Find-Java {
    # 1. PATH
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    # 2. JAVA_HOME
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (-not (Test-Path $candidate)) { $candidate = Join-Path $env:JAVA_HOME "bin/java" }
        if (Test-Path $candidate -ErrorAction SilentlyContinue) {
            Write-Warn "Java found via JAVA_HOME but not on PATH: $candidate"
            return $candidate
        }
    }

    # 3. Common Windows install roots
    $roots = @(
        "$env:PROGRAMFILES\Java"
        "$env:PROGRAMFILES\Eclipse Adoptium"
        "$env:PROGRAMFILES\Microsoft"
        "$env:PROGRAMFILES\Amazon Corretto"
    )
    foreach ($root in $roots) {
        if (Test-Path $root -ErrorAction SilentlyContinue) {
            $found = Get-ChildItem "$root\*\bin\java.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($found) {
                Write-Warn "Java found at '$($found.FullName)' but is not on your PATH."
                Write-Warn "Consider setting JAVA_HOME or adding Java to your PATH."
                return $found.FullName
            }
        }
    }

    return $null
}

function Get-JavaMajorVersion {
    param([string]$JavaPath)
    try {
        # java -version writes to stderr; PS5 wraps it in ErrorRecord objects
        $line = (& $JavaPath -version 2>&1) | ForEach-Object {
            if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message }
            else { $_.ToString() }
        } | Where-Object { $_ -match 'version' } | Select-Object -First 1
        if (-not $line) { return $null }
        if ($line -match '"1\.(\d+)')  { return [int]$Matches[1] }  # Java 8: "1.8.x"
        if ($line -match '"(\d+)')     { return [int]$Matches[1] }  # Java 11+: "17.x.x"
    } catch {}
    return $null
}

# ── Vault setup ──────────────────────────────────────────────────────────────

# Runs `docker compose up -d --build [extra args]`, falling back to the legacy
# `docker-compose` CLI if the subcommand variant is unavailable.
# -Quiet suppresses all output (used for background restarts).
function Invoke-ComposeUp {
    param(
        [string]$DockerPath,
        [string[]]$ExtraArgs = @(),
        [switch]$Quiet
    )

    # Temporarily allow native-command stderr without throwing - $LASTEXITCODE
    # is still set correctly, but progress lines written to stderr won't become
    # terminating errors under $ErrorActionPreference = "Stop".
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    $allArgs = @("compose", "up", "-d", "--build") + $ExtraArgs
    if ($Quiet) {
        & $DockerPath @allArgs 2>&1 | Out-Null
    } else {
        & $DockerPath @allArgs
    }
    $exitCode = $LASTEXITCODE

    $ErrorActionPreference = $prev

    if ($exitCode -eq 0) { return $true }

    # Fallback: legacy docker-compose binary
    $legacy = Get-Command "docker-compose" -ErrorAction SilentlyContinue
    if ($legacy) {
        Write-Warn "'docker compose' failed - retrying with legacy 'docker-compose' CLI"
        $legacyArgs = @("up", "-d", "--build") + $ExtraArgs
        $ErrorActionPreference = "Continue"
        if ($Quiet) {
            & docker-compose @legacyArgs 2>&1 | Out-Null
        } else {
            & docker-compose @legacyArgs
        }
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $prev
        return ($exitCode -eq 0)
    }

    return $false
}

function Test-DockerDaemon {
    param([string]$DockerPath)

    try {
        & $DockerPath info 1>$null 2>$null
        if ($LASTEXITCODE -eq 0) { return $true }
    } catch {}

    Write-Err "Docker is installed, but the Docker daemon is not running"
    Write-Warn "Start Docker Desktop (or your Docker service), then re-run this command"
    Write-Warn "Quick check: docker info"
    return $false
}

function Test-VaultContainerRunning {
    param([string]$DockerPath)

    $vaultPsOutput = @(& $DockerPath compose ps -q vault 2>&1)
    if ($LASTEXITCODE -ne 0) {
        $details = ($vaultPsOutput -join " ").Trim()
        Write-Err "Unable to query the Vault service via docker compose"
        if ($details) { Write-Warn $details }
        Write-Warn "Run this command from the project root and ensure docker-compose.yml contains the 'vault' service"
        return $false
    }

    $firstLine = $vaultPsOutput | Select-Object -First 1
    $containerId = if ($null -eq $firstLine) { "" } else { "$firstLine".Trim() }
    if (-not $containerId) {
        Write-Err "Vault container is not running"
        Write-Warn "Start it with: docker compose up -d --build vault"
        Write-Warn "Or start the full stack: docker compose up -d --build"
        return $false
    }

    $statusLines = @(& $DockerPath inspect -f '{{.State.Status}}' $containerId 2>$null)
    $status = ($statusLines -join "").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $status) {
        Write-Err "Could not inspect Vault container state"
        Write-Warn "Check: docker compose ps vault"
        return $false
    }

    if ($status -ne "running") {
        Write-Err "Vault container is '$status' (expected 'running')"
        Write-Warn "Check logs: docker compose logs vault"
        Write-Warn "Try restarting: docker compose up -d --build vault"
        return $false
    }

    return $true
}

function Invoke-VaultSetup {
    $DockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $DockerPath) {
        Write-Err "Docker not found"
        exit 1
    }

    if (-not (Test-DockerDaemon -DockerPath $DockerPath)) {
        exit 1
    }

    if (-not (Test-VaultContainerRunning -DockerPath $DockerPath)) {
        exit 1
    }

    Write-Step "Configuring Vault..."

    # Wait for Vault container to respond to status queries
    Write-Info "Waiting for Vault to be ready..."
    $statusJson = $null
    for ($i = 0; $i -lt 20; $i++) {
        try {
            $lines = @(& $DockerPath compose exec -T vault vault status -format=json 2>$null)
            $exitCode = $LASTEXITCODE
            if ($exitCode -eq 0 -or $exitCode -eq 2) {
                $jsonStr = ($lines -join "`n").Trim()
                $statusJson = $jsonStr | ConvertFrom-Json
                break
            }
        } catch {}
        Start-Sleep -Seconds 3
    }

    if (-not $statusJson) {
        Write-Err "Vault did not respond within 60s"
        Write-Warn "Check logs: docker compose logs vault"
        return
    }

    if (-not $statusJson.initialized) {
        # ── First-time Vault initialization ──────────────────────────────────
        Write-Info "Vault is not initialized - running first-time setup..."

        # Step 1: Initialize
        $initLines = @(& $DockerPath compose exec -T vault vault operator init -key-shares=1 -key-threshold=1 -format=json 2>$null)
        if ($LASTEXITCODE -ne 0) {
            Write-Err "vault operator init failed"
            return
        }
        try {
            $initData = ($initLines -join "`n").Trim() | ConvertFrom-Json
        } catch {
            Write-Err "Failed to parse init output - raw output below (save these values!):"
            Write-Host ($initLines -join "`n") -ForegroundColor Yellow
            return
        }
        $unsealKey = $initData.unseal_keys_b64[0]
        $rootToken = $initData.root_token
        Write-Ok "Vault initialized"

        # Step 2: Unseal
        Write-Info "Unsealing Vault..."
        & $DockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Failed to unseal Vault"
            Write-Warn "Unseal key: $unsealKey"
            return
        }
        Write-Ok "Vault unsealed"

        # Step 3: Enable KV v2 secrets engine
        Write-Info "Enabling KV v2 secrets engine at /secret/..."
        $kvLines = @(& $DockerPath compose exec -T -e "VAULT_TOKEN=$rootToken" vault vault secrets enable -path=secret kv-v2 2>&1)
        if ($LASTEXITCODE -ne 0) {
            $kvOut = $kvLines -join " "
            if ($kvOut -match "already in use") {
                Write-Ok "KV v2 already enabled at /secret/"
            } else {
                Write-Err "Failed to enable KV v2"
                if ($kvOut) { Write-Warn $kvOut }
                return
            }
        } else {
            Write-Ok "KV v2 secrets engine enabled"
        }

        # Step 4: Write application policy
        Write-Info "Writing application policy..."
        & $DockerPath compose exec -T -e "VAULT_TOKEN=$rootToken" vault vault policy write unievent-app /vault/config/policies/unievent-app.hcl 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Failed to write Vault policy"
            return
        }
        Write-Ok "Policy 'unievent-app' written"

        # Step 5: Create application token
        Write-Info "Creating application token (valid 32 days)..."
        $tokenLines = @(& $DockerPath compose exec -T -e "VAULT_TOKEN=$rootToken" vault vault token create -policy=unievent-app -ttl=768h -format=json 2>$null)
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Failed to create application token"
            return
        }
        try {
            $tokenData = ($tokenLines -join "`n").Trim() | ConvertFrom-Json
        } catch {
            Write-Err "Failed to parse token output"
            return
        }
        $appToken = $tokenData.auth.client_token
        Write-Ok "Application token created"

        # Step 6: Save credentials to .env
        Update-EnvVar -Key "VAULT_UNSEAL_TOKEN" -Value $unsealKey
        Update-EnvVar -Key "VAULT_ROOT_TOKEN" -Value $rootToken
        Update-EnvVar -Key "VAULT_TOKEN" -Value $appToken
        Write-Ok "Vault credentials saved to .env"

        # Recreate app container so it picks up the new VAULT_TOKEN
        Write-Info "Restarting app with new Vault token..."
        Invoke-ComposeUp -DockerPath $DockerPath -ExtraArgs @("app") -Quiet | Out-Null
        Write-Ok "App container updated"

        Write-Host ""
        Write-Warn "Back up these Vault credentials - you need them if the volume is lost:"
        Write-Info "  Unseal Key : $unsealKey"
        Write-Info "  Root Token : $rootToken"
        Write-Info "  App Token  : $($appToken.Substring(0, [Math]::Min(20, $appToken.Length)))..."

    } elseif ($statusJson.sealed) {
        # ── Unseal ───────────────────────────────────────────────────────────
        Write-Info "Vault is sealed - unsealing..."

        $envVars = Load-DotEnv
        $unsealKey = if ($envVars.ContainsKey("VAULT_UNSEAL_TOKEN")) { $envVars["VAULT_UNSEAL_TOKEN"] } else { "" }

        if (-not $unsealKey) {
            Write-Err "VAULT_UNSEAL_TOKEN is not set in .env"
            Write-Warn "Add your unseal key to .env or unseal manually:"
            Write-Warn "  docker compose exec vault vault operator unseal <KEY>"
            return
        }

        & $DockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Unseal failed - check VAULT_UNSEAL_TOKEN in .env"
            return
        }
        Write-Ok "Vault unsealed"

        $rootToken = if ($envVars.ContainsKey("VAULT_ROOT_TOKEN")) { $envVars["VAULT_ROOT_TOKEN"] } else { "" }
        $appToken  = if ($envVars.ContainsKey("VAULT_TOKEN"))       { $envVars["VAULT_TOKEN"] }       else { "" }
        Write-Host ""
        Write-Info "Vault credentials (from .env):"
        Write-Info "  Unseal Key : $unsealKey"
        if ($rootToken) { Write-Info "  Root Token : $rootToken" }
        if ($appToken)  { Write-Info "  App Token  : $($appToken.Substring(0, [Math]::Min(20, $appToken.Length)))..." }

    } else {
        Write-Ok "Vault is initialized and unsealed"
    }
}

function Invoke-Unseal {
    $envVars = Load-DotEnv
    $unsealKey = if ($envVars.ContainsKey("VAULT_UNSEAL_TOKEN")) { $envVars["VAULT_UNSEAL_TOKEN"] } else { "" }

    if (-not $unsealKey) {
        Write-Err "VAULT_UNSEAL_TOKEN is not set in .env"
        Write-Warn "Add your Vault unseal key to .env:"
        Write-Warn "  VAULT_UNSEAL_TOKEN=<your-key>"
        exit 1
    }

    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) {
        Write-Err "Docker not found"
        exit 1
    }

    if (-not (Test-DockerDaemon -DockerPath $dockerPath)) {
        exit 1
    }

    if (-not (Test-VaultContainerRunning -DockerPath $dockerPath)) {
        exit 1
    }

    Write-Info "Unsealing Vault..."
    & $dockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Failed to unseal Vault - is the container running?"
        Write-Warn "Start the stack first: docker compose up -d --build"
        exit 1
    }
    Write-Ok "Vault unsealed"
}

# ── Vault status probe (single attempt, no retry) ────────────────────────────

function Get-VaultStatus {
    param([string]$DockerPath)
    try {
        $lines    = @(& $DockerPath compose exec -T vault vault status -format=json 2>$null)
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0 -or $exitCode -eq 2) {
            $json = ($lines -join "`n").Trim() | ConvertFrom-Json
            if (-not $json.initialized) { return "not-initialized" }
            if ($json.sealed)           { return "sealed" }
            return "ready"
        }
    } catch {}
    return "unavailable"
}

# ── Setup: main flow ──────────────────────────────────────────────────────────

function Invoke-Setup {
    $allOk = $true

    Write-Host ""
    Write-Host "  UniEvent Local Dev Setup" -ForegroundColor Cyan
    Write-Sep

    # ── Step 1: Dependency checks ─────────────────────────────────────────────

    Write-Step "Checking dependencies..."
    $failed = @()

    # Java (needs 17+)
    $javaPath = Find-Java
    if (-not $javaPath) {
        Write-Err "Java not found"
        Write-Warn "Install Java 17+ from https://adoptium.net or https://www.microsoft.com/openjdk"
        $failed += "Java"
    } else {
        $ver = Get-JavaMajorVersion -JavaPath $javaPath
        if ($null -eq $ver) {
            Write-Warn "Java found but could not determine version - continuing anyway"
        } elseif ($ver -lt 17) {
            Write-Err "Java $ver found - Java 17 or higher is required"
            $failed += "Java"
        } else {
            Write-Ok "Java $ver"
        }
    }

    # Maven (mvnw in the project handles builds, global mvn is optional)
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue -CommandType Application | Select-Object -First 1
    if ($mvn) {
        Write-Ok "Maven ($($mvn.Source))"
    } else {
        Write-Info "Maven not installed globally (fine - project uses mvnw)"
    }

    # OpenSSL
    $opensslPath = Find-Executable -Name "openssl" -Fallbacks $script:KnownPaths.openssl
    if (-not $opensslPath) {
        Write-Err "OpenSSL not found"
        if ($IsWindows -or $env:OS -eq "Windows_NT") {
            Write-Warn "OpenSSL ships with Git for Windows. Install Git from https://git-scm.com"
            Write-Warn "Or install OpenSSL directly from https://slproweb.com/products/Win32OpenSSL.html"
        } else {
            Write-Warn "Install via: brew install openssl  (Mac) or  sudo apt install openssl  (Linux)"
        }
        $failed += "OpenSSL"
    } else {
        Write-Ok "OpenSSL ($opensslPath)"
    }

    # curl
    $curlPath = Find-Executable -Name "curl" -Fallbacks $script:KnownPaths.curl
    if (-not $curlPath) {
        Write-Err "curl not found"
        if ($IsWindows -or $env:OS -eq "Windows_NT") {
            Write-Warn "curl is built into Windows 10+. Try running from a newer terminal."
        } else {
            Write-Warn "Install via: brew install curl  (Mac) or  sudo apt install curl  (Linux)"
        }
        $failed += "curl"
    } else {
        Write-Ok "curl ($curlPath)"
    }

    # Docker
    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) {
        Write-Err "Docker not found"
        Write-Warn "Install Docker Desktop from https://www.docker.com/products/docker-desktop"
        $failed += "Docker"
    } else {
        Write-Ok "Docker ($dockerPath)"
    }

    if ($failed.Count -gt 0) {
        Write-Host ""
        Write-Err "Missing dependencies: $($failed -join ', ')"
        Write-Warn "Please install the above and re-run: ./tools setup"
        exit 1
    }

    # ── Step 2: .env check ────────────────────────────────────────────────────

    Write-Step "Checking .env..."
    $envFile = Join-Path $PSScriptRoot ".env"
    if (-not (Test-Path $envFile)) {
        Write-Err ".env file not found"
        Write-Warn "Ask the dev team for the .env file and place it at:"
        Write-Warn "  $envFile"
        Write-Warn "A template is available at .env.example"
        exit 1
    }
    Write-Ok ".env found"

    # ── Step 3: docker-compose.override.yml ──────────────────────────────────

    Write-Step "Checking docker-compose.override.yml..."
    $overrideDst = Join-Path $PSScriptRoot "docker-compose.override.yml"
    $overrideSrc = Join-Path $PSScriptRoot "docker-compose.override.yml.example"

    if (Test-Path $overrideDst) {
        Write-Ok "docker-compose.override.yml already exists"
    } elseif (Test-Path $overrideSrc) {
        Copy-Item $overrideSrc $overrideDst
        Write-Ok "Created docker-compose.override.yml from example"
    } else {
        Write-Warn "docker-compose.override.yml.example not found - skipping"
    }

    # ── Step 4: TLS certificate ───────────────────────────────────────────────

    Write-Step "Checking TLS certificate..."
    $certsDir  = Join-Path $PSScriptRoot "certs"
    $certFile  = Join-Path $certsDir "fullchain.pem"
    $keyFile   = Join-Path $certsDir "privkey.pem"

    $certsExist = (Test-Path $certFile) -and (Test-Path $keyFile)

    if ($certsExist) {
        Write-Ok "TLS certificate already exists"
    } else {
        if (-not (Test-Path $certsDir)) {
            New-Item -ItemType Directory -Path $certsDir | Out-Null
            Write-Info "Created certs/ directory"
        } else {
            Write-Info "certs/ directory exists but certificates are missing - generating now"
        }

        Write-Info "Generating self-signed certificate..."
        $subject = "/CN=localhost"
        try {
            & $opensslPath req -x509 -nodes -days 3650 -newkey rsa:2048 `
                -keyout $keyFile -out $certFile `
                -subj $subject 2>&1 | Out-Null
        } catch {
            Write-Err "OpenSSL failed: $($_.Exception.Message)"
            exit 1
        }

        if ((Test-Path $certFile) -and (Test-Path $keyFile)) {
            Write-Ok "Self-signed certificate generated (valid 10 years)"
        } else {
            Write-Err "Certificate generation failed - files not created"
            exit 1
        }
    }

    # ── Step 5: Install CLI on PATH ───────────────────────────────────────────

    Write-Step "Installing CLI..."

    if ($IsLinux -or $IsMacOS) {
        $localBin = Join-Path $HOME ".local/bin"
        $linkPath  = Join-Path $localBin "tools"
        $scriptPath = Join-Path $PSScriptRoot "tools.sh"

        # Ensure ~/.local/bin exists
        if (-not (Test-Path $localBin)) {
            New-Item -ItemType Directory -Path $localBin -Force | Out-Null
        }

        # Remove stale symlink if it points elsewhere
        if (Test-Path $linkPath) {
            $existing = & readlink -f $linkPath 2>/dev/null
            if ($existing -eq $scriptPath) {
                Write-Ok "CLI already installed at $linkPath"
            } else {
                Remove-Item $linkPath -Force
                & ln -s $scriptPath $linkPath
                Write-Ok "CLI symlink updated at $linkPath  →  $scriptPath"
            }
        } else {
            & ln -s $scriptPath $linkPath
            Write-Ok "CLI installed at $linkPath  →  $scriptPath"
        }

        # Check if ~/.local/bin is on PATH
        if ($env:PATH -notmatch [regex]::Escape($localBin)) {
            Write-Host ""
            Write-Warn "~/.local/bin is not on your PATH yet."
            Write-Warn "Add this line to your ~/.bashrc or ~/.zshrc:"
            Write-Host '    export PATH="$HOME/.local/bin:$PATH"' -ForegroundColor White
            Write-Warn "Then restart your terminal (or run: source ~/.bashrc)"
            Write-Warn "After that you can run: tools setup, tools seed, tools clear"
        } else {
            Write-Info "You can now run: tools setup, tools seed, tools clear"
        }
    } else {
        # Windows: drop a stub tools.bat into ~\.local\bin (already on PATH via Claude, etc.)
        $localBin  = Join-Path $env:USERPROFILE ".local\bin"
        $stubPath  = Join-Path $localBin "tools.bat"
        $scriptAbs = Join-Path $PSScriptRoot "tools.ps1"

        if (-not (Test-Path $localBin)) {
            New-Item -ItemType Directory -Path $localBin -Force | Out-Null
        }

        $stubContent = "@echo off`r`npowershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$scriptAbs`" %*`r`n"

        if (Test-Path $stubPath) {
            $existing = Get-Content $stubPath -Raw
            if ($existing -eq $stubContent) {
                Write-Ok "tools already installed at $stubPath"
            } else {
                Set-Content -Path $stubPath -Value $stubContent -NoNewline
                Write-Ok "tools stub updated at $stubPath"
            }
        } else {
            Set-Content -Path $stubPath -Value $stubContent -NoNewline
            Write-Ok "tools installed at $stubPath"
        }

        # Ensure ~\.local\bin is on user PATH
        # Read directly from registry to get current persisted value (avoids stale $env:PATH)
        $regKey   = "HKCU:\Environment"
        $userPath = $null
        try {
            $userPath = (Get-ItemProperty -Path $regKey -Name PATH -ErrorAction Stop).PATH
        } catch {
            # PATH key may not exist in HKCU:\Environment on a clean profile — that's fine
        }

        if ($VerboseOutput) {
            Write-Info "localBin    : $localBin"
            Write-Info "Registry PATH entries:"
            if ($userPath) {
                ($userPath -split ";") | ForEach-Object { Write-Info "  [$_]" }
            } else {
                Write-Info "  (null / not set)"
            }
        }

        $escapedBin = [regex]::Escape($localBin)
        $alreadyIn  = if ($userPath) {
            ($userPath -split ";") | Where-Object { $_ -match "^$escapedBin\\?$" }
        } else { $null }

        if ($VerboseOutput) { Write-Info "Matched entries: $(if ($alreadyIn) { $alreadyIn -join ', ' } else { '(none)' })" }

        if ($alreadyIn) {
            Write-Ok "$localBin is already on PATH (registry)"
        } else {
            $newUserPath = if ($userPath) { "$userPath;$localBin" } else { $localBin }
            # Preserve REG_EXPAND_SZ type so %SystemRoot% etc. in PATH aren't corrupted
            $regType = "String"
            try { $regType = (Get-Item $regKey).GetValueKind("PATH") } catch {}
            if ($regType -eq "ExpandString") {
                Set-ItemProperty -Path $regKey -Name PATH -Value $newUserPath -Type ExpandString
            } else {
                Set-ItemProperty -Path $regKey -Name PATH -Value $newUserPath
            }
            # Refresh current session immediately — no terminal restart needed
            $machinePath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
            $env:PATH = if ($machinePath) { "$newUserPath;$machinePath" } else { $newUserPath }
            Write-Ok "Added $localBin to user PATH (registry)"
        }

        # Also update PowerShell profiles — registry alone is not always picked up
        # (profile runs on every new session regardless of how the terminal was launched)
        $profileLine = "if (`"`$env:USERPROFILE\.local\bin`" -notin (`$env:PATH -split `";`")) { `$env:PATH = `"`$env:USERPROFILE\.local\bin;`$env:PATH`" }"
        $profilePaths = @(
            "$env:USERPROFILE\Documents\WindowsPowerShell\Microsoft.PowerShell_profile.ps1"  # PS5
            "$env:USERPROFILE\Documents\PowerShell\Microsoft.PowerShell_profile.ps1"          # PS7
        )
        foreach ($profilePath in $profilePaths) {
            $profileDir = Split-Path $profilePath -Parent
            if (-not (Test-Path $profileDir)) {
                New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
            }
            $existing = if (Test-Path $profilePath) { Get-Content $profilePath -Raw } else { "" }
            if ($existing -notmatch "\.local\\bin") {
                $addition = "`r`n# Added by UniEvent tools setup`r`n$profileLine`r`n"
                Add-Content -Path $profilePath -Value $addition -Encoding UTF8
                Write-Ok "Updated profile: $(Split-Path $profilePath -Leaf)"
            } else {
                Write-Ok "Profile already configured: $(Split-Path $profilePath -Leaf)"
            }
        }

        Write-Info "You can now run: tools seed, tools setup, tools clear"
    }

    # ── Step 7: Docker ────────────────────────────────────────────────────────

    Write-Host ""
    Write-Sep
    Write-Ok "Setup complete!"
    Write-Host ""

    # Check Docker daemon state
    $dockerRunning = $false
    try {
        $info = & $dockerPath info 2>&1
        $dockerRunning = ($LASTEXITCODE -eq 0)
    } catch {
        $dockerRunning = $false
    }

    if (-not $dockerRunning) {
        Write-Warn "Docker daemon is not running."
        Write-Warn "Start Docker Desktop, then run:"
        Write-Host "    docker compose up -d --build" -ForegroundColor White
        Write-Host ""
        return
    }

    # Check if stack is already up
    $stackUp = $false
    try {
        $ps = & $dockerPath compose ps --quiet 2>&1
        $stackUp = ($LASTEXITCODE -eq 0 -and $ps -and $ps.ToString().Trim() -ne "")
    } catch {
        $stackUp = $false
    }

    $ESC      = [char]27; $BEL = [char]7
    $linkRoot = "${ESC}]8;;http://localhost${BEL}localhost${ESC}]8;;${BEL}"
    $link8080 = "${ESC}]8;;http://localhost:8080${BEL}localhost:8080${ESC}]8;;${BEL}"

    if ($stackUp) {
        Write-Ok "Docker stack is already running. Visit $linkRoot or $link8080."
        $vaultStatus = Get-VaultStatus -DockerPath $dockerPath
        switch ($vaultStatus) {
            "not-initialized" { Write-Warn "Vault is not initialized. Run: tools vault" }
            "sealed"          { Write-Warn "Vault is sealed. Run: tools vault (or tools unseal) to unseal it." }
            "ready"           { Write-Ok   "Vault is initialized and unsealed." }
            "unavailable"     { Write-Warn "Could not reach Vault - it may still be starting. Run: tools vault" }
        }
    } else {
        $answer = Read-Host "  Docker is available but the stack is not running. Start it now? [Y/n]"
        if ($answer -eq "" -or $answer -match "^[Yy]") {
            Write-Info "Starting docker compose..."
            $started = Invoke-ComposeUp -DockerPath $dockerPath
            if ($started) {
                Write-Ok "Stack started. Go to $linkRoot or $link8080 to see frontend. Then initialize/unseal Vault by running 'tools vault'."
            } else {
                Write-Err "docker compose up failed - check the output above."
            }
        } else {
            Write-Info "Skipped. When ready, run: docker compose up -d --build"
        }
    }
    Write-Host ""
}

# ── Main ──────────────────────────────────────────────────────────────────────

if ($Help -or $Command -eq "" -or $Command -eq "-h" -or $Command -eq "--help") {
    Show-Help
    exit 0
}

# Setup runs before any .env or health-check logic
if ($Command.ToLower() -eq "setup") {
    Invoke-Setup
    exit 0
}

if ($Command.ToLower() -eq "vault") {
    Invoke-VaultSetup
    exit 0
}

if ($Command.ToLower() -eq "unseal") {
    Invoke-Unseal
    exit 0
}

# All other commands require a running server
$baseUrl = if ($Remote -ne "") { $Remote.TrimEnd("/") } else { "https://localhost" }

Write-Info "Connecting to $baseUrl ..."
if (-not (Test-ServerHealth -BaseUrl $baseUrl)) {
    Write-Err "Server not running at $baseUrl"
    if ($Remote -eq "") {
        Write-Warn "Start the stack: docker compose up -d --build"
        Write-Warn "Or run locally: ./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'"
    } else {
        Write-Warn "Check that the remote server is reachable: $baseUrl"
    }
    exit 1
}

Write-Sep

switch ($Command.ToLower()) {

    "seed" {
        Write-Info "Seeding test data..."
        $resp = Invoke-AdminRequest -Method "POST" -Url "$baseUrl/admin/tools/seed"
        Handle-Response -Response $resp -SuccessMsg "Test data seeded successfully"
    }

    "clear" {
        Write-Info "Clearing seed data..."
        $resp = Invoke-AdminRequest -Method "DELETE" -Url "$baseUrl/admin/tools/seed"
        Handle-Response -Response $resp -SuccessMsg "Seed data cleared successfully"
    }

    "ingest" {
        Write-Warn "The 'ingest' command is not yet implemented on the server."
        Write-Info "Endpoint: POST $baseUrl/admin/tools/ingest  (coming soon)"
        exit 1
    }

    "refresh-tokens" {
        Write-Warn "The 'refresh-tokens' command is not yet implemented on the server."
        Write-Info "Endpoint: POST $baseUrl/admin/tools/refresh-tokens  (coming soon)"
        exit 1
    }

    default {
        Write-Err "Unknown command: '$Command'"
        Write-Host ""
        Show-Help
        exit 1
    }
}

Write-Sep
