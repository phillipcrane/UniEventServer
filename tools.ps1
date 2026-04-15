#Requires -Version 5.1
<#
.SYNOPSIS
    UniEvent admin CLI - run admin tools against a local or remote server.

.USAGE
    ./tools.ps1 <command> [options]

.COMMANDS
    setup           Check dependencies and configure the local dev environment
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
    } catch [System.Net.WebException] {
        # HTTP response (even 401/403) = server is up
        if ($null -ne $_.Exception.Response) { return $true }
        # TrustFailure = self-signed cert the delegate didn't catch = server is up
        if ($_.Exception.Status -eq [System.Net.WebExceptionStatus]::TrustFailure) { return $true }
        return $false
    } catch {
        return $false
    }
}

# ── HTTP request helper ───────────────────────────────────────────────────────

function Invoke-AdminRequest {
    param([string]$Method, [string]$Url, [string]$ApiKey)

    $headers = @{ "X-Admin-Key" = $ApiKey; "Content-Type" = "application/json" }

    if ($VerboseOutput) {
        Write-Info "$Method $Url"
        Write-Info "X-Admin-Key: $($ApiKey.Substring(0, [Math]::Min(4, $ApiKey.Length)))***"
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
        401 {
            Write-Err "Unauthorized (401) - UNIEVENT_API_KEY is missing or incorrect"
            Write-Warn "Please request the .env file from the dev team."
            exit 1
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
        Write-Host "    docker compose up -d" -ForegroundColor White
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

    if ($stackUp) {
        Write-Ok "Docker stack is already running."
    } else {
        $answer = Read-Host "  Docker is available but the stack is not running. Start it now? [Y/n]"
        if ($answer -eq "" -or $answer -match "^[Yy]") {
            Write-Info "Starting docker compose..."
            & $dockerPath compose up -d
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "Stack started. Give it ~30s to be healthy, then run 'tools seed'."
            } else {
                Write-Err "docker compose up failed - check the output above."
            }
        } else {
            Write-Info "Skipped. When ready, run: docker compose up -d"
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

# All other commands require .env + a running server
$env = Load-DotEnv
if (-not $env.ContainsKey("UNIEVENT_API_KEY") -or $env["UNIEVENT_API_KEY"] -eq "") {
    Write-Err "UNIEVENT_API_KEY is not set in your .env file"
    Write-Warn "Add: UNIEVENT_API_KEY=your-key-here"
    exit 1
}
$apiKey = $env["UNIEVENT_API_KEY"]

$baseUrl = if ($Remote -ne "") { $Remote.TrimEnd("/") } else { "https://localhost" }

Write-Info "Connecting to $baseUrl ..."
if (-not (Test-ServerHealth -BaseUrl $baseUrl)) {
    Write-Err "Server not running at $baseUrl"
    if ($Remote -eq "") {
        Write-Warn "Start the stack: docker compose up -d"
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
        $resp = Invoke-AdminRequest -Method "POST" -Url "$baseUrl/admin/tools/seed" -ApiKey $apiKey
        Handle-Response -Response $resp -SuccessMsg "Test data seeded successfully"
    }

    "clear" {
        Write-Info "Clearing seed data..."
        $resp = Invoke-AdminRequest -Method "DELETE" -Url "$baseUrl/admin/tools/seed" -ApiKey $apiKey
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
