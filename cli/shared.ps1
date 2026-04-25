# Shared helpers for the admin CLI: output, paths, env, HTTP, discovery.

# ── Output ────────────────────────────────────────────────────────────────────

function Write-Info  { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Cyan }
function Write-Ok    { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Green }
function Write-Err   { param([string]$Msg) Write-Host "  ERROR: $Msg" -ForegroundColor Red }
function Write-Warn  { param([string]$Msg) Write-Host "  WARN: $Msg" -ForegroundColor Yellow }
function Write-Sep   { Write-Host ("-" * 50) -ForegroundColor DarkGray }
function Write-Step  { param([string]$Msg) Write-Host "`n  $Msg" -ForegroundColor White }

function Redact-SensitiveText {
    param([string]$Text)

    if (-not $Text) { return $Text }

    $redacted = $Text
    $patterns = @(
        '(?im)((?:token|password|secret|authorization|client_secret|access_token|refresh_token)\s*[:=]\s*)([^\s,;]+)',
        '(?im)("(?:token|password|secret|authorization|client_secret|access_token|refresh_token)"\s*:\s*")([^"]+)(")',
        '(?im)(Bearer\s+)([A-Za-z0-9\-\._~\+/=]+)',
        '(?im)((?:VAULT_TOKEN|VAULT_ROOT_TOKEN|VAULT_UNSEAL_TOKEN|confirmationToken|inviteKey|invitationKey)\s*[:=]\s*)([^\s,;]+)',
        '(?im)("(?:confirmationToken|inviteKey|invitationKey)"\s*:\s*")([^"]+)(")'
    )

    foreach ($pattern in $patterns) {
        $redacted = [regex]::Replace($redacted, $pattern, '$1***REDACTED***')
    }

    return $redacted
}

function Resolve-BaseUrl {
    param([string]$RawBaseUrl)

    $candidate = if ($RawBaseUrl) { $RawBaseUrl.Trim() } else { "" }
    if (-not $candidate) {
        throw "Base URL cannot be empty"
    }

    $uri = $null
    if (-not [System.Uri]::TryCreate($candidate, [System.UriKind]::Absolute, [ref]$uri)) {
        throw "Invalid URL format: '$candidate'"
    }

    if ($uri.Scheme -ne "http" -and $uri.Scheme -ne "https") {
        throw "Unsupported URL scheme '$($uri.Scheme)'. Use http:// or https://"
    }

    if ($uri.Scheme -eq "http") {
        $host = $uri.Host.ToLowerInvariant()
        $isLocal = ($host -eq "localhost" -or $host -eq "127.0.0.1" -or $host -eq "::1")
        if (-not $isLocal) {
            throw "Refusing insecure HTTP for non-localhost target '$candidate'. Use HTTPS."
        }
    }

    $builder = New-Object System.UriBuilder($uri)
    $builder.Path = ""
    $builder.Query = ""
    $builder.Fragment = ""

    return $builder.Uri.AbsoluteUri.TrimEnd('/')
}

function Assert-ValidBaseUrl {
    param([string]$BaseUrl)

    try {
        return Resolve-BaseUrl -RawBaseUrl $BaseUrl
    } catch {
        Write-Err "Invalid base URL: $($_.Exception.Message)"
        exit 1
    }
}

function Assert-NonEmpty {
    param([string]$Name, [string]$Value)

    if (-not $Value -or -not $Value.Trim()) {
        Write-Err "$Name must not be empty"
        exit 1
    }
}

function Test-ValidEmail {
    param([string]$Email)

    if (-not $Email) { return $false }
    try {
        $mail = [System.Net.Mail.MailAddress]::new($Email)
        return ($mail.Address -eq $Email)
    } catch {
        return $false
    }
}

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
    Write-Host "  -p, --page <id>      Scope to a single page (refresh, ingest)"
    Write-Host "  -w, --wipe           seed: only clear, skip re-seed; docker/vault: destroy data volumes"
    Write-Host "  -d, --down           docker: stop the stack"
    Write-Host "  -y, --yes            Non-interactive approval for prompts"
    Write-Host "  -v, --verbose        Show extra output"
    Write-Host "  -h, --help           Show this help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  $cli setup"
    Write-Host "  $cli docker                   # start or rebuild/restart"
    Write-Host "  $cli docker -d                # stop"
    Write-Host "  $cli docker -v                # start with full compose output"
    Write-Host "  $cli seed                     # clear + re-seed"
    Write-Host "  $cli seed -w                  # clear only"
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

    Update-EnvVars -Pairs @{ $Key = $Value }
}

function Write-TextFileUtf8NoBom {
    param(
        [string]$Path,
        [string[]]$Lines
    )

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $tmpPath = "$Path.tmp"
    [System.IO.File]::WriteAllLines($tmpPath, $Lines, $utf8NoBom)
    Move-Item -Path $tmpPath -Destination $Path -Force
}

function Update-EnvVars {
    param([hashtable]$Pairs)

    if (-not $Pairs -or $Pairs.Count -eq 0) { return }

    $envFile = Join-Path (Get-RepoRoot) ".env"
    if (-not (Test-Path $envFile)) { return }

    $lines = @(Get-Content $envFile)

    foreach ($key in $Pairs.Keys) {
        $value = "$($Pairs[$key])"
        $found = $false
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match "^$([regex]::Escape($key))=") {
                $lines[$i] = "$key=$value"
                $found = $true
                break
            }
        }
        if (-not $found) { $lines += "$key=$value" }
    }

    Write-TextFileUtf8NoBom -Path $envFile -Lines $lines
}

# ── HTTP ──────────────────────────────────────────────────────────────────────
# Self-signed-cert handling for https://localhost is the reason this is PowerShell
# rather than bash/curl - PS 7's -SkipCertificateCheck is the load-bearing feature.

function Invoke-Web {
    param([string]$Method = "GET", [string]$Uri, [hashtable]$Headers = @{}, [int]$TimeoutSec = 30, [string]$Body = $null)
    $skipCert = $false
    try {
        $parsedUri = [System.Uri]$Uri
        $host = $parsedUri.Host.ToLowerInvariant()
        $isLocalHost = ($host -eq "localhost" -or $host -eq "127.0.0.1" -or $host -eq "::1")
        $skipCert = $parsedUri.Scheme -eq "https" -and $isLocalHost
    } catch {
        $skipCert = $false
    }
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
    param([string]$BaseUrl, [switch]$VerboseOutput, [int]$Retries = 3)

    if ($Retries -lt 1) { $Retries = 1 }
    if ($VerboseOutput) { Write-Info "Checking server health at $BaseUrl" }

    for ($attempt = 1; $attempt -le $Retries; $attempt++) {
        try {
            $resp = Invoke-Web -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
            $statusCode = [int]$resp.StatusCode
            if ($statusCode -lt 200 -or $statusCode -ge 300) {
                if ($VerboseOutput) { Write-Warn "Health endpoint returned HTTP $statusCode" }
            } else {
                $healthIsUp = $true
                if ($resp.Content) {
                    try {
                        $payload = $resp.Content | ConvertFrom-Json
                        if ($payload.PSObject.Properties.Name -contains 'status') {
                            $healthIsUp = ("$($payload.status)".ToUpperInvariant() -eq 'UP')
                        }
                    } catch {
                        if ($VerboseOutput) {
                            Write-Warn "Health response is not valid JSON; accepting HTTP success"
                        }
                    }
                }

                if ($healthIsUp) {
                    return $true
                }

                if ($VerboseOutput) {
                    Write-Warn "Health endpoint reachable but status is not UP"
                }
            }
        } catch {
            if ($VerboseOutput) {
                $ex = $_.Exception
                Write-Info "Health check attempt $attempt/$Retries failed: $($ex.GetType().Name): $($ex.Message)"
            }
        }

        if ($attempt -lt $Retries) {
            $delay = [math]::Min(2 * $attempt, 5)
            Start-Sleep -Seconds $delay
        }
    }

    return $false
}

$script:_adminTokenByBaseUrl = @{}

function Get-AdminToken {
    param([string]$BaseUrl)

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl

    if ($script:_adminTokenByBaseUrl.ContainsKey($BaseUrl) -and $script:_adminTokenByBaseUrl[$BaseUrl]) {
        return $script:_adminTokenByBaseUrl[$BaseUrl]
    }

    $envVars  = Load-DotEnv
    $password = $envVars["ADMIN_PASSWORD"]

    if (-not $password) {
        Write-Err "ADMIN_PASSWORD is not set in .env"
        Write-Warn "Set ADMIN_PASSWORD in your .env - the server provisions the admin account from this value at startup."
        exit 1
    }

    $email = "cli@unievent.internal"

    $loginBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json -Compress
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

    $script:_adminTokenByBaseUrl[$BaseUrl] = $token
    return $token
}

function Invoke-AdminRequest {
    param([string]$Method, [string]$Url, [switch]$VerboseOutput)

    $uri = $null
    if (-not [System.Uri]::TryCreate($Url, [System.UriKind]::Absolute, [ref]$uri)) {
        Write-Err "Invalid request URL: $Url"
        exit 1
    }

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
        $response = $_.Exception.Response
        if ($null -eq $response) {
            Write-Err "Request failed: $($_.Exception.Message)"
            exit 1
        }

        $statusCode = [int]$response.StatusCode
        $body = ""
        $stream = $null
        $reader = $null
        try {
            $stream = $response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $body = $reader.ReadToEnd()
            }
        } finally {
            if ($null -ne $reader) { $reader.Dispose() }
            if ($null -ne $stream) { $stream.Dispose() }
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
                    $output = $json | ConvertTo-Json -Depth 5
                    Write-Host (Redact-SensitiveText -Text $output) -ForegroundColor Gray
                } catch {
                    Write-Host (Redact-SensitiveText -Text $Response.Body) -ForegroundColor Gray
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
            if ($Response.Body) { Write-Warn (Redact-SensitiveText -Text $Response.Body) }
            exit 1
        }
        default {
            Write-Err "Unexpected response: $($Response.StatusCode)"
            if ($Response.Body) { Write-Warn (Redact-SensitiveText -Text $Response.Body) }
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
