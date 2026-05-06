# Shared helpers for the admin CLI: output, paths, env, HTTP, discovery.

function Write-Info  { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Cyan }
function Write-Ok    { param([string]$Msg) Write-Host "  $Msg" -ForegroundColor Green }
function Write-Err   { param([string]$Msg) Write-Host "  ERROR: $Msg" -ForegroundColor Red }
function Write-Warn  { param([string]$Msg) Write-Host "  WARN: $Msg" -ForegroundColor Yellow }
function Write-Sep   { Write-Host ("-" * 50) -ForegroundColor DarkGray }
function Write-Step  { param([string]$Msg) Write-Host "`n  $Msg" -ForegroundColor White }

# scrubs tokens, passwords, and other secrets from a string before printing. Runs a bunch of regex
# patterns over the text and replaces matched values with ***REDACTED***. Safe to call on any output.
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

# validates and normalises the base URL: rejects plain HTTP for non-localhost, strips path/query/fragment.
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

    if ($uri.Scheme -ne "http" -and $uri.Scheme -ne "https") { # only http/https, nothing weird
        throw "Unsupported URL scheme '$($uri.Scheme)'. Use http:// or https://"
    }

    if ($uri.Scheme -eq "http") { # plain HTTP only OK for localhost, not real servers
        $hostname = $uri.Host.ToLowerInvariant()
        $isLocal = ($hostname -eq "localhost" -or $hostname -eq "127.0.0.1" -or $hostname -eq "::1")
        if (-not $isLocal) {
            throw "Refusing insecure HTTP for non-localhost target '$candidate'. Use HTTPS."
        }
    }

    $builder = New-Object System.UriBuilder($uri) # strip path, query, fragment
    $builder.Path = ""
    $builder.Query = ""
    $builder.Fragment = ""

    return $builder.Uri.AbsoluteUri.TrimEnd('/')
}

# same as Resolve-BaseUrl but prints the error and exits instead of throwing.
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
        # let .NET's MailAddress parser do the heavy lifting, then check the normalised address
        # matches what we passed in. This catches things like "foo@" which .NET accepts but normalises.
        $mail = [System.Net.Mail.MailAddress]::new($Email)
        return ($mail.Address -eq $Email)
    } catch {
        return $false
    }
}

function Get-EnvVariable {
    param([string]$Name, [string]$Default)

    $envVar = [Environment]::GetEnvironmentVariable($Name)
    return if ($envVar) { $envVar } else { $Default }
}

function Load-DotEnv {
    param([string]$FilePath)

    if (-not (Test-Path $FilePath)) {
        return
    }

    Get-Content $FilePath | ForEach-Object {
        if ($_ -match '^\s*([^#=]+?)\s*=\s*(.+?)\s*$') {
            $name = $matches[1]
            $value = $matches[2]
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
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
    Write-Host "  status                 Show Docker stack and Vault health"
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
    Write-Host "  --remote <url>       Target server URL (default: https://localhost)"
    Write-Host "  -p, --page <id>      Scope to a single page (refresh, ingest)"
    Write-Host "  -e, --email <email>  invite: recipient email (default: test@example.com)"
    Write-Host "  -n, --orgname <name> invite: organization name (default: Test Organization)"
    Write-Host "  -w, --wipe           seed: only clear, skip re-seed; docker/vault: destroy data volumes"
    Write-Host "  -d, --down           docker: stop the stack"
    Write-Host "  -r, --rebuild        docker/setup: force --no-cache build (slow but clean)"
    Write-Host "  -y, --yes            Non-interactive approval for prompts"
    Write-Host "  -v, --verbose        Show extra output"
    Write-Host "  -h, --help           Show this help"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  $cli setup"
    Write-Host "  $cli docker                   # start or restart"
    Write-Host "  $cli docker -r                # force clean rebuild (--no-cache)"
    Write-Host "  $cli docker -d                # stop"
    Write-Host "  $cli docker -v                # start with full compose output"
    Write-Host "  $cli seed                     # clear + re-seed"
    Write-Host "  $cli seed -w                  # clear only"
    Write-Host "  $cli refresh"
    Write-Host "  $cli refresh -p 1234567890"
    Write-Host "  $cli ingest"
    Write-Host "  $cli ingest -p 1234567890 -v"
    Write-Host "  $cli seed --remote https://staging.example.com"
    Write-Host "  $cli invite -e organizer@company.com -v"
    Write-Host ""
}

$script:_repoRoot = $null

# walks up from $PSScriptRoot looking for .git/ or pom.xml to find the repo root.
# Cached in $_repoRoot after the first call.
function Get-RepoRoot {
    param([string]$StartFrom = $PSScriptRoot)

    if ($script:_repoRoot) { return $script:_repoRoot }

    $dir = Get-Item -LiteralPath $StartFrom
    while ($null -ne $dir) {
        if ((Test-Path (Join-Path $dir.FullName ".git")) -or
            (Test-Path (Join-Path $dir.FullName "pom.xml"))) {
            $script:_repoRoot = $dir.FullName
            return $script:_repoRoot
        }
        if ($null -eq $dir.Parent) { break }
        $dir = $dir.Parent
    }

    throw "Could not find repo root (no .git or pom.xml) walking up from $StartFrom"
}

# reads .env from the repo root and returns key-value pairs as a hashtable. Exits if .env is missing.
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
        if ($line -eq "" -or $line.StartsWith("#")) { continue } # skip blank lines and comments
        if ($line -match "^([^=]+)=(.*)$") {
            $raw = $Matches[2].Trim()
            if (($raw -match '^"(.*)"$') -or ($raw -match "^'(.*)'$")) { $raw = $Matches[1] } # strip surrounding quotes
            $vars[$Matches[1].Trim()] = $raw
        }
    }
    return $vars
}

function Update-EnvVar {
    param([string]$Key, [string]$Value)

    Update-EnvVars -Pairs @{ $Key = $Value }
}

# writes lines as UTF-8 without a BOM, since PowerShell's Set-Content adds one by default
# which breaks .env parsing on Linux. Writes to .tmp first so the target is never half-written.
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

# updates one or more keys in .env in-place. For each key, scans for an existing KEY=... line
# and overwrites it, or appends KEY=value at the end if it's not there yet. Preserves everything else.
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
                $lines[$i] = "$key=$value" # overwrite the existing line
                $found = $true
                break
            }
        }
        if (-not $found) { $lines += "$key=$value" } # key wasn't there, append it
    }

    Write-TextFileUtf8NoBom -Path $envFile -Lines $lines
}

# thin wrapper around Invoke-WebRequest. PS 7 has -SkipCertificateCheck so we can hit
# localhost with a self-signed cert. PS 5 needs the compiled C# workaround below.
function Invoke-Web {
    param(
        [string]$Method = "GET",
        [string]$Uri,
        [hashtable]$Headers = @{},
        [int]$TimeoutSec = 30,
        [string]$Body = $null,
        $WebSession = $null
    )
    $skipCert = $false
    try { # figure out if we need to skip cert validation (only for https localhost)
        $parsedUri = [System.Uri]$Uri
        $hostname = $parsedUri.Host.ToLowerInvariant()
        $isLocalHost = ($hostname -eq "localhost" -or $hostname -eq "127.0.0.1" -or $hostname -eq "::1")
        $skipCert = $parsedUri.Scheme -eq "https" -and $isLocalHost
    } catch {
        $skipCert = $false
    }
    if ($PSVersionTable.PSVersion.Major -ge 6) { # PS 7+ - easy path, just pass the flag
        $splat = @{ Uri=$Uri; Method=$Method; Headers=$Headers; TimeoutSec=$TimeoutSec; ErrorAction="Stop"; SkipCertificateCheck=$skipCert }
        if ($null -ne $Body -and $Body -ne "") { $splat["Body"] = $Body }
        if ($null -ne $WebSession) { $splat["WebSession"] = $WebSession }
        return Invoke-WebRequest @splat
    }

    # PS 5 can't use a scriptblock for cert validation because there's no PowerShell runspace in
    # the .NET callback. So we compile a small C# class on the fly with a static method that just
    # returns true for every cert. .NET caches the compiled type, so this only pays the cost once.
    if ($skipCert) {
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
        $old = [System.Net.ServicePointManager]::ServerCertificateValidationCallback # save so we can restore after
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = [System.Net.Security.RemoteCertificateValidationCallback]::CreateDelegate([System.Net.Security.RemoteCertificateValidationCallback], [CertificateBypass], 'IgnoreSSLErrors')
    }

    try {
        $splat = @{ Uri=$Uri; Method=$Method; Headers=$Headers; TimeoutSec=$TimeoutSec; ErrorAction="Stop" }
        if ($null -ne $Body -and $Body -ne "") { $splat["Body"] = $Body }
        if ($null -ne $WebSession) { $splat["WebSession"] = $WebSession }
        return Invoke-WebRequest @splat
    } finally {
        if ($skipCert) { [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $old }
    }
}

# hits /actuator/health and returns true if the server is UP. Retries a few times so you can call
# it right after starting the container and it'll wait for the app to come up.
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
                # got a 2xx. if the body has a 'status' field, check it's UP. If not (or if
                # it's not JSON at all), treat the 2xx itself as healthy.
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
            $delay = [math]::Min(2 * $attempt, 5) # short backoff, capped at 5s
            Start-Sleep -Seconds $delay
        }
    }

    return $false
}

$script:_adminSessionByBaseUrl = @{}

# logs in as the CLI service account and returns a session (auth cookie + CSRF token).
# Cached per base URL so we only do the login flow once per run.
function Get-AdminSession { 
    param([string]$BaseUrl)

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl

    if ($script:_adminSessionByBaseUrl.ContainsKey($BaseUrl) -and $script:_adminSessionByBaseUrl[$BaseUrl]) {
        return $script:_adminSessionByBaseUrl[$BaseUrl] # already logged in, reuse the session
    }

    $envVars  = Load-DotEnv
    $password = $envVars["ADMIN_PASSWORD"]

    if (-not $password) {
        Write-Err "ADMIN_PASSWORD is not set in .env"
        Write-Warn "Set ADMIN_PASSWORD in your .env - the server provisions the admin account from this value at startup."
        exit 1
    }

    $email    = "cli@unievent.internal" # service account provisioned automatically by the server on startup
    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json -Compress

    # Create a WebSession so auth cookies from the login response are auto-sent on subsequent requests.
    $webSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    try {
        # login requires CSRF protection, so get the token first before sending credentials
        $csrfResp = Invoke-Web -Uri "$BaseUrl/api/auth/csrf-token" -Method "GET" -TimeoutSec 15 -WebSession $webSession
        $csrfToken = ($csrfResp.Content | ConvertFrom-Json).csrfToken
        if (-not $csrfToken) {
            throw "CSRF token endpoint returned no csrfToken."
        }

        $resp      = Invoke-Web -Uri "$BaseUrl/api/auth/login" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json"; "X-CSRF-Token" = $csrfToken } -Body $loginBody -TimeoutSec 15 -WebSession $webSession
        $loginCsrfToken = ($resp.Content | ConvertFrom-Json).csrfToken
    } catch {
        Write-Err "Could not authenticate CLI service account: $($_.Exception.Message)"
        Write-Warn "Make sure the server is running and ADMIN_PASSWORD matches what the server was started with."
        exit 1
    }

    if (-not $loginCsrfToken) {
        Write-Err "Login succeeded but response contained no csrfToken - check server logs"
        exit 1
    }

    $session = @{ WebSession = $webSession; CsrfToken = $loginCsrfToken }
    $script:_adminSessionByBaseUrl[$BaseUrl] = $session
    return $session
}

# sends an authenticated request to an admin endpoint and returns { StatusCode, Body } so the
# caller can decide what counts as success.
function Invoke-AdminRequest {
    param([string]$Method, [string]$Url, [string]$Body = $null, [switch]$VerboseOutput)

    $uri = $null
    if (-not [System.Uri]::TryCreate($Url, [System.UriKind]::Absolute, [ref]$uri)) {
        Write-Err "Invalid request URL: $Url"
        exit 1
    }

    $baseUrl      = "$($uri.Scheme)://$($uri.Authority)" # extract base URL to look up the right session
    $adminSession = Get-AdminSession -BaseUrl $baseUrl

    $headers = @{ "Content-Type" = "application/json" }
    # State-changing methods require the CSRF token alongside the auth cookie.
    if ($Method -in @("POST", "PUT", "PATCH", "DELETE")) {
        $headers["X-CSRF-Token"] = $adminSession.CsrfToken
    }

    if ($VerboseOutput) {
        Write-Info "$Method $Url"
    }

    try {
        $resp = Invoke-Web -Uri $Url -Method $Method -Headers $headers -Body $Body -TimeoutSec 120 -WebSession $adminSession.WebSession
        return @{ StatusCode = $resp.StatusCode; Body = $resp.Content }
    } catch [System.Net.WebException] {
        # PS5 throws WebException for non-2xx. The response body lives on the exception's Response
        # object, not the return value, so we have to pull it out of the stream manually.
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

# prints a success message on 2xx or exits with a friendly error. Covers the common cases:
# validation errors (400/422), auth failures (401/403), missing endpoint (404), server error (500).
function Handle-Response {
    param([hashtable]$Response, [string]$SuccessMsg, [switch]$VerboseOutput)

    switch ($Response.StatusCode) {
        { $_ -ge 200 -and $_ -lt 300 } {
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
        { $_ -eq 400 -or $_ -eq 422 } {
            Write-Err "Validation error ($($Response.StatusCode))"
            if ($Response.Body) { Write-Warn (Redact-SensitiveText -Text $Response.Body) }
            exit 1
        }
        { $_ -eq 401 -or $_ -eq 403 } {
            Write-Err "Unauthorized ($($Response.StatusCode)) - check admin credentials"
            if ($Response.Body) { Write-Warn (Redact-SensitiveText -Text $Response.Body) }
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

# known off-PATH install locations per tool (Windows-first, cross-platform fallbacks)
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

# looks for an executable on PATH first (-CommandType Application skips PS aliases like curl→Invoke-WebRequest),
# then walks $Fallbacks for known off-PATH install locations.
function Find-Executable {
    param([string]$Name, [string[]]$Fallbacks = @())

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
    # 1. check PATH first, the normal case
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    # 2. check JAVA_HOME if PATH didn't have it
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (-not (Test-Path $candidate)) { $candidate = Join-Path $env:JAVA_HOME "bin/java" }
        if (Test-Path $candidate -ErrorAction SilentlyContinue) {
            Write-Warn "Java found via JAVA_HOME but not on PATH: $candidate"
            return $candidate
        }
    }

    # 3. scan common Windows install roots for people who installed Java but never set PATH or JAVA_HOME
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
        # On Linux, resolve symlinks before executing. Invoking a symlink path directly
        # triggers GNOME's gio file-type detection, which prints a harmless but confusing
        # "Failed to find default application for content type 'inode/symlink'" warning.
        if ($IsLinux) {
            $canonical = & readlink -f $JavaPath 2>$null | Select-Object -First 1
            if ($canonical) { $canonical = $canonical.Trim() }
            if ($canonical -and (Test-Path $canonical -ErrorAction SilentlyContinue)) {
                $JavaPath = $canonical
            }
        }
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
