# Shared helpers for the admin CLI: output, paths, env, HTTP, discovery.

# ── Output ────────────────────────────────────────────────────────────────────

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
    Write-Host "  setup                  Check dependencies and configure local dev environment"
    Write-Host "  docker                 Start (or rebuild/restart) the Docker stack"
    Write-Host "  vault                  Initialize and/or unseal Vault"
    Write-Host "  unseal                 Quick unseal Vault (shortcut for restart)"
    Write-Host "  seed                   Clear and re-seed test data"
    Write-Host "  refresh                Refresh Facebook page tokens (all, or one with -p)"
    Write-Host "  ingest                 Manually ingest from a Facebook page (interactive or -p)"
    Write-Host "  invite                 Send organizer invite key and test registration flow"
    Write-Host ""
    Write-Host "Flags:"
    Write-Host "  -r, --remote <url>   Target a remote server (default: https://localhost)"
    Write-Host "  -p, --page <id>      Scope to a single page (refresh, ingest)"
    Write-Host "  -c, --clear          seed: only clear, skip re-seed"
    Write-Host "  -d, --down           docker: stop the stack"
    Write-Host "  -w, --wipe           docker/vault: destroy data volumes (prompts for confirmation)"
    Write-Host "  -v, --verbose        Show extra output"
    Write-Host "  -h, --help           Show this help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  $cli setup"
    Write-Host "  $cli docker                   # start or rebuild/restart"
    Write-Host "  $cli docker -d                # stop"
    Write-Host "  $cli docker -v                # start with full compose output"
    Write-Host "  $cli seed                     # clear + re-seed"
    Write-Host "  $cli seed -c                  # clear only"
    Write-Host "  $cli refresh"
    Write-Host "  $cli refresh -p 1234567890"
    Write-Host "  $cli ingest"
    Write-Host "  $cli ingest -p 1234567890 -v"
    Write-Host "  $cli seed -r https://staging.example.com"
    Write-Host "  $cli invite -e organizer@company.com -v"
    Write-Host ""
}

# ── Paths ─────────────────────────────────────────────────────────────────────
# Resolve the repo root from any script location under cli/.
# Walks up from $PSScriptRoot looking for .git/ or pom.xml.

function Get-RepoRoot {
    param([string]$StartFrom = $PSScriptRoot)

    $dir = Get-Item -LiteralPath $StartFrom
    while ($null -ne $dir) {
        if ((Test-Path (Join-Path $dir.FullName ".git")) -or
            (Test-Path (Join-Path $dir.FullName "pom.xml"))) {
            return $dir.FullName
        }
        if ($null -eq $dir.Parent) { break }
        $dir = $dir.Parent
    }

    throw "Could not find repo root (no .git or pom.xml) walking up from $StartFrom"
}

# ── .env ──────────────────────────────────────────────────────────────────────

function Load-DotEnv {
    $envFile = Join-Path (Get-RepoRoot) ".env"
    if (-not (Test-Path $envFile)) {
        Write-Err ".env file not found"
        Write-Warn "Ask the dev team for the .env file and place it in: $(Get-RepoRoot)"
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
    $envFile = Join-Path (Get-RepoRoot) ".env"
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

# ── HTTP ──────────────────────────────────────────────────────────────────────
# Self-signed-cert handling for https://localhost is the reason this is PowerShell
# rather than bash/curl - PS 7's -SkipCertificateCheck is the load-bearing feature.

function Invoke-Web {
    param([string]$Method = "GET", [string]$Uri, [hashtable]$Headers = @{}, [int]$TimeoutSec = 30, [string]$Body = $null)
    $skipCert = $Uri -match "^https://localhost"
    if ($PSVersionTable.PSVersion.Major -ge 6) {
        if ($null -ne $Body -and $Body -ne "") {
            return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -Body $Body `
                -TimeoutSec $TimeoutSec -ErrorAction Stop -SkipCertificateCheck:$skipCert
        }
        return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers `
            -TimeoutSec $TimeoutSec -ErrorAction Stop -SkipCertificateCheck:$skipCert
    }
    
    # PS 5: Cannot use scriptblock callbacks for cert validation (no runspace in .NET callback)
    # Solution: use compiled C# delegate instead of scriptblock
    if ($skipCert) {
        # Create compiled delegate for cert validation bypass (only once, cached by .NET runtime)
        if (-not ("CertificateBypass" -as [type])) {
            Add-Type -TypeDefinition @"
                using System;
                using System.Net.Security;
                using System.Security.Cryptography.X509Certificates;
                public class CertificateBypass {
                    public static bool IgnoreSSLErrors(object sender, X509Certificate certificate, X509Chain chain, SslPolicyErrors sslPolicyErrors) {
                        return true;
                    }
                }
"@
        }
        $old = [System.Net.ServicePointManager]::ServerCertificateValidationCallback
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = [System.Net.Security.RemoteCertificateValidationCallback]::CreateDelegate([System.Net.Security.RemoteCertificateValidationCallback], [CertificateBypass], 'IgnoreSSLErrors')
    }
    
    try {
        if ($null -ne $Body -and $Body -ne "") {
            return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -Body $Body `
                -TimeoutSec $TimeoutSec -ErrorAction Stop
        }
        return Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers -TimeoutSec $TimeoutSec -ErrorAction Stop
    } finally {
        if ($skipCert) { [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $old }
    }
}

function Test-ServerHealth {
    param([string]$BaseUrl, [switch]$VerboseOutput)
    if ($VerboseOutput) { Write-Info "Checking server at $BaseUrl" }
    try {
        Invoke-Web -Uri "$BaseUrl/actuator/health" -TimeoutSec 5 | Out-Null
        return $true
    } catch {
        $ex = $_.Exception
        
        # DEBUG: Log what exception type we're actually getting
        if ($VerboseOutput) {
            Write-Info "Exception type: $($ex.GetType().FullName)"
            Write-Info "Exception message: $($ex.Message)"
            if ($ex.InnerException) {
                Write-Info "Inner exception: $($ex.InnerException.GetType().FullName): $($ex.InnerException.Message)"
            }
        }
        
        # PS7: HttpRequestException wraps an inner HttpRequestError for non-2xx
        # PS5: WebException with a Response means the server replied (even 401/403)
        if ($ex -is [System.Net.WebException] -and $null -ne $ex.Response) { return $true }
        if ($ex -is [System.Net.WebException] -and $ex.Status -eq [System.Net.WebExceptionStatus]::TrustFailure) { return $true }
        if ($ex.GetType().Name -eq "HttpResponseException") { return $true }
        
        # Fallback: if we got ANY Response object, server is responding
        if ($null -ne $ex.Response) { return $true }
        
        # PS5: also check for ConnectFailure which means server is down
        if ($ex -is [System.Net.WebException] -and $ex.Status -eq [System.Net.WebExceptionStatus]::ConnectFailure) { return $false }
        
        return $false
    }
}

$script:_adminToken = $null

function Get-AdminToken {
    param([string]$BaseUrl)

    if ($script:_adminToken) { return $script:_adminToken }

    $envVars  = Load-DotEnv
    $password = $envVars["ADMIN_PASSWORD"]

    if (-not $password) {
        Write-Err "ADMIN_PASSWORD is not set in .env"
        Write-Warn "Set ADMIN_PASSWORD in your .env - the server provisions the admin account from this value at startup."
        exit 1
    }

    $email = "cli@unievent.internal"

    $loginBody = "{`"email`":`"$email`",`"password`":`"$password`"}"
    try {
        $resp  = Invoke-Web -Uri "$BaseUrl/api/auth/login" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json" } -Body $loginBody -TimeoutSec 15
        $token = ($resp.Content | ConvertFrom-Json).token
    } catch {
        Write-Err "Could not authenticate CLI service account: $($_.Exception.Message)"
        Write-Warn "Ensure the server is running and ADMIN_PASSWORD matches what the server was started with."
        exit 1
    }

    if (-not $token) {
        Write-Err "Login succeeded but response contained no token - check server logs"
        exit 1
    }

    $script:_adminToken = $token
    return $token
}

function Invoke-AdminRequest {
    param([string]$Method, [string]$Url, [switch]$VerboseOutput)

    $uri     = [System.Uri]$Url
    $baseUrl = "$($uri.Scheme)://$($uri.Authority)"
    $token   = Get-AdminToken -BaseUrl $baseUrl

    $headers = @{ "Content-Type" = "application/json"; "Authorization" = "Bearer $token" }

    if ($VerboseOutput) {
        Write-Info "$Method $Url"
    }

    try {
        $resp = Invoke-Web -Uri $Url -Method $Method -Headers $headers -TimeoutSec 120
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
        # PS7 wraps non-2xx in HttpResponseException. The Response content stream is
        # usually already consumed by Invoke-WebRequest, so prefer $_.ErrorDetails.Message
        # which PS captures up-front.
        $ex = $_.Exception
        if ($ex.GetType().Name -eq "HttpResponseException") {
            $statusCode = [int]$ex.Response.StatusCode
            $body = ""
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
                $body = $_.ErrorDetails.Message
            } else {
                try { $body = $ex.Response.Content.ReadAsStringAsync().Result } catch {}
            }
            return @{ StatusCode = $statusCode; Body = $body }
        }
        Write-Err "Request failed: $($_.Exception.Message)"
        exit 1
    }
}

function Handle-Response {
    param([hashtable]$Response, [string]$SuccessMsg, [switch]$VerboseOutput)

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

# ── Executable discovery ──────────────────────────────────────────────────────
# Known off-PATH install locations per tool (Windows-first, cross-platform fallbacks)

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
