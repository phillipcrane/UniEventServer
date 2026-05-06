# Vault init/unseal and Docker helpers. Called by `tools vault`, `tools unseal`,
# and by setup to check stack state after `docker compose up`

# runs docker compose build then up -d. Quiet mode suppresses output and shows a progress message instead.
# Returns true on success, false if either step fails.
function Invoke-ComposeUp {
    param(
        [string]$DockerPath,
        [string[]]$ExtraArgs = @(),
        [switch]$Quiet,
        [switch]$NoCache
    )

    # temporarily allow native-command stderr without throwing. $LASTEXITCODE is still set
    # correctly, but progress lines written to stderr won't become terminating errors.
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    # by default use Docker's layer cache for fast rebuilds.
    # pass -NoCache (via --rebuild) to force a full clean build from scratch.
    $buildArgs = @("compose", "build") + $(if ($NoCache) { @("--no-cache") } else { @() }) + $ExtraArgs
    $buildOutput = @()
    if ($Quiet) {
        $msg = if ($NoCache) { "Building Docker images (--no-cache, this may take several minutes)..." } else { "Building Docker images (using cache, a minute or two)..." }
        Write-Info $msg
        $buildOutput = @(& $DockerPath @buildArgs 2>&1)
    } else {
        & $DockerPath @buildArgs
    }
    $buildExitCode = $LASTEXITCODE

    if ($buildExitCode -ne 0) {
        $ErrorActionPreference = $prev
        if ($Quiet -and $buildOutput.Count -gt 0) {
            $tail = @($buildOutput | Select-Object -Last 8)
            Write-Warn ("docker compose build output: " + ($tail -join " | "))
        }
        return $false
    }

    $upArgs = @("compose", "up", "-d") + $ExtraArgs # start containers in detached mode
    $upOutput = @()
    if ($Quiet) {
        Write-Info "Starting containers..."
        $upOutput = @(& $DockerPath @upArgs 2>&1)
    } else {
        & $DockerPath @upArgs
    }
    $exitCode = $LASTEXITCODE

    $ErrorActionPreference = $prev

    if ($exitCode -eq 0) { return $true }

    if ($Quiet) {
        $tail = @($upOutput | Select-Object -Last 8)
        if ($tail.Count -gt 0) {
            Write-Warn ("docker compose up output: " + ($tail -join " | "))
        }
    }

    return $false
}

# runs docker info to check if the daemon is up. Returns false and prints an error if it's not.
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

# checks if the Vault container is in 'running' state via docker compose ps + docker inspect.
function Test-VaultContainerRunning {
    param([string]$DockerPath)

    $vaultPsOutput = @(& $DockerPath compose ps -q vault 2>&1) # -q returns just the container ID
    $psExitCode = $LASTEXITCODE
    if ($psExitCode -ne 0) {
        $details = ($vaultPsOutput -join " ").Trim()
        Write-Err "Unable to query the Vault service via docker compose"
        if ($details) { Write-Warn $details }
        Write-Warn "Run this command from the project root and make sure docker-compose.yml contains the 'vault' service"
        return $false
    }

    # filter out Docker warnings (lines starting with time= and level=warning)
    $cleanOutput = @($vaultPsOutput | Where-Object { $_ -notmatch '^\s*time=' })

    $firstLine = $cleanOutput | Select-Object -First 1
    $containerId = if ($null -eq $firstLine) { "" } else { "$firstLine".Trim() }
    if (-not $containerId) {
        return $false # no container ID means Vault isn't in the stack
    }

    # use inspect rather than ps to get the actual container state (ps status strings vary by version)
    $statusLines = @(& $DockerPath inspect -f '{{.State.Status}}' $containerId 2>$null)
    $inspectExitCode = $LASTEXITCODE
    $status = ($statusLines -join "").Trim()
    if ($inspectExitCode -ne 0 -or -not $status) {
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

# returns a status string for Vault: 'ready', 'sealed', 'not-initialized', or 'unavailable'.
function Get-VaultStatus {
    param([string]$DockerPath)
    try {
        $lines    = @(& $DockerPath compose exec -T vault vault status -format=json 2>$null)
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0 -or $exitCode -eq 2) { # vault status exits 2 when sealed, 0 when unsealed
            $json = ($lines -join "`n").Trim() | ConvertFrom-Json
            if (-not $json.initialized) { return "not-initialized" }
            if ($json.sealed)           { return "sealed" }
            return "ready"
        }
    } catch {}
    return "unavailable"
}

# prints the current Vault status and, if -v, dumps the raw JSON output too.
function Write-VaultStatusDetails {
    param(
        [string]$DockerPath,
        [switch]$VerboseOutput
    )

    $status = Get-VaultStatus -DockerPath $DockerPath
    Write-Info "Vault status: $status"

    if (-not $VerboseOutput) { return }

    try {
        $lines = @(& $DockerPath compose exec -T vault vault status -format=json 2>&1)
        if ($lines.Count -gt 0) {
            Write-Info "Vault raw status output:"
            Write-Host ($lines -join "`n") -ForegroundColor DarkGray
        }
    } catch {
        Write-Warn "Could not read detailed Vault status: $($_.Exception.Message)"
    }
}

# initializes and/or unseals Vault. Three paths: 1) not running (starts it); 2) not initialized
# (full init flow: init, unseal, KV engine, policy, app token); 3) sealed (unseal from .env).
function Invoke-VaultSetup {
    param(
        [switch]$VerboseOutput,
        [switch]$Yes,
        [switch]$Rebuild,
        [switch]$RecoveredFromSealMismatch
    )

    $DockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $DockerPath) {
        Write-Err "Docker not found"
        exit 1
    }

    if (-not (Test-DockerDaemon -DockerPath $DockerPath)) {
        exit 1
    }

    if (-not (Test-VaultContainerRunning -DockerPath $DockerPath)) {
        Write-Info "Starting Vault container..."
        $started = Invoke-ComposeUp -DockerPath $DockerPath -ExtraArgs @("vault") -Quiet -NoCache:$Rebuild
        if (-not $started) {
            Write-Err "Failed to start Vault container"
            exit 1
        }
        if (-not (Test-VaultContainerRunning -DockerPath $DockerPath)) { exit 1 }
    }

    Write-VaultStatusDetails -DockerPath $DockerPath -VerboseOutput:$VerboseOutput

    Write-Step "Configuring Vault..."

    # poll until Vault responds to status queries, timeout at 60s (20 x 3s)
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
        # first-time setup: init with a single key share (fine for dev), then unseal and configure
        Write-Info "Vault is not initialized - running first-time setup..."

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
        $unsealKey = $initData.unseal_keys_b64[0] # the one key we need to unseal
        $rootToken = $initData.root_token          # root token for the setup steps below
        Write-Ok "Vault initialized"

        Update-EnvVars -Pairs @{ VAULT_UNSEAL_TOKEN = $unsealKey; VAULT_ROOT_TOKEN = $rootToken }
        Write-Ok "Vault init secrets saved to .env (unseal key + root token)"

        Write-Info "Unsealing Vault..."
        $unsealOutput = @(& $DockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Failed to unseal Vault"
            if ($unsealOutput.Count -gt 0) {
                $tail = @($unsealOutput | Select-Object -Last 8)
                Write-Warn ("vault output: " + ($tail -join " | "))
            }
            Write-Warn "Use your securely stored unseal key to retry: docker compose exec vault vault operator unseal <KEY>"
            return
        }
        Write-Ok "Vault unsealed"

        # enable the key-value secrets engine at /secret/ so the app has somewhere to store secrets
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

        # write the app policy from the HCL file baked into the Vault container image
        Write-Info "Writing application policy..."
        $policyOut = @(& $DockerPath compose exec -T -e "VAULT_TOKEN=$rootToken" vault vault policy write unievent-app /vault/config/policies/unievent-app.hcl 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Failed to write Vault policy"
            if ($policyOut.Count -gt 0) {
                $tail = @($policyOut | Select-Object -Last 8)
                Write-Warn ("vault output: " + ($tail -join " | "))
            }
            return
        }
        Write-Ok "Policy 'unievent-app' written"

        # create a scoped app token tied to the policy - the app uses this, not the root token
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

        Update-EnvVars -Pairs @{ "VAULT_TOKEN" = $appToken } # app reads VAULT_TOKEN from .env on startup
        Write-Ok "Vault application token saved to .env"

        Write-Info "Restarting app with new Vault token..."
        Invoke-ComposeUp -DockerPath $DockerPath -ExtraArgs @("app") -Quiet -NoCache | Out-Null
        Write-Ok "App container updated"

        Write-Host ""
        Write-Warn "The unseal key and root token are saved in .env for convenience, but .env isn't secure long-term storage. Keep a copy somewhere safe."

    } elseif ($statusJson.sealed) {
        # Vault was initialized before but is sealed (normal after a Docker restart)
        Write-Info "Vault is sealed - unsealing..."

        $envVars = Load-DotEnv
        $unsealKey = if ($envVars.ContainsKey("VAULT_UNSEAL_TOKEN")) { $envVars["VAULT_UNSEAL_TOKEN"] } else { "" }

        if (-not $unsealKey) {
            Write-Err "VAULT_UNSEAL_TOKEN is not set in .env"
            Write-Warn "Unseal manually with your stored copy:"
            Write-Warn "  docker compose exec vault vault operator unseal <KEY>"
            return
        }

        $unsealOutput = @(& $DockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1)
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Unseal failed - check VAULT_UNSEAL_TOKEN in .env"
            if ($unsealOutput.Count -gt 0) {
                $tail = @($unsealOutput | Select-Object -Last 8)
                Write-Warn ("vault output: " + ($tail -join " | "))
            }

            # if the key doesn't match Vault's stored seal data, the volume probably has stale state
            # from a previous init (e.g. the volume survived a wipe but .env was reset)
            $unsealOutputText = ($unsealOutput -join " ")
            $canRecover = -not $RecoveredFromSealMismatch -and (
                $unsealOutputText -match "unable to retrieve stored keys" -or
                $unsealOutputText -match "invalid key" -or
                $unsealOutputText -match "message authentication failed" -or
                $unsealOutputText -match "failed to decrypt keys from storage"
            )

            if ($canRecover) {
                if ($Yes) {
                    Write-Warn "Vault seal data does not match VAULT_UNSEAL_TOKEN - wiping and reinitializing Vault"
                    Invoke-VaultWipe -Yes
                    Invoke-VaultSetup -VerboseOutput:$VerboseOutput -Yes:$Yes -Rebuild:$Rebuild -RecoveredFromSealMismatch
                    return
                }

                Write-Warn "Vault seal data appears to be stale. Run 'tools vault -w' to wipe Vault, then rerun 'tools setup -y'."
            }
            return
        }
        Write-Ok "Vault unsealed"

    } else {
        Write-Ok "Vault is initialized and unsealed"
    }
}

function Invoke-VaultWipe {
    # stops Vault, removes the container and its data volume, and clears credentials from .env.
    # prompts for confirmation unless -y is passed.
    param([switch]$VerboseOutput, [switch]$Yes)

    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) { Write-Err "Docker not found"; exit 1 }
    if (-not (Test-DockerDaemon -DockerPath $dockerPath)) { exit 1 }

    Write-Warn "This will delete all Vault data and clear credentials from .env. You will need to re-initialize Vault afterwards."
    if (-not $Yes) {
        $answer = Read-Host "  Are you sure? [y/N]"
        if ($answer -notmatch "^[Yy]") { Write-Info "Cancelled."; return }
    } else {
        Write-Info "Auto-approved with -y/--yes"
    }

    Write-Info "Stopping and removing Vault container..."
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue" # suppress errorscontainer may already be gone
    & $dockerPath compose stop vault 2>&1 | Out-Null
    & $dockerPath compose rm -f vault 2>&1 | Out-Null
    $ErrorActionPreference = $prev

    Write-Info "Removing Vault volume..."
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $volumes = @(& $dockerPath volume ls -q --filter "name=vault-data" 2>$null) # find all matching volumes
    foreach ($vol in $volumes) {
        & $dockerPath volume rm $vol 2>$null | Out-Null
        Write-Info "  Removed volume: $vol"
    }
    $ErrorActionPreference = $prev

    Write-Info "Clearing Vault credentials from .env..."
    Update-EnvVars -Pairs @{ VAULT_UNSEAL_TOKEN = ""; VAULT_ROOT_TOKEN = ""; VAULT_TOKEN = "" } # blank them out rather than deleting the lines

    Write-Ok "Vault wiped"
    Write-Info "Run 'tools docker' to restart Vault, then 'tools vault' to re-initialize."
}

function Invoke-Unseal {
    # unseals Vault using VAULT_UNSEAL_TOKEN from .env. Use this after a Docker restart when Vault is sealed but already initialized.
    param([switch]$VerboseOutput)

    $envVars = Load-DotEnv
    $unsealKey = if ($envVars.ContainsKey("VAULT_UNSEAL_TOKEN")) { $envVars["VAULT_UNSEAL_TOKEN"] } else { "" }

    if (-not $unsealKey) {
        Write-Err "VAULT_UNSEAL_TOKEN is not set in .env"
        Write-Warn "The unseal key is not saved here. Unseal manually with your secure copy:" # key was never saved, or .env was cleared
        Write-Warn "  docker compose exec vault vault operator unseal <KEY>"
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

    Write-VaultStatusDetails -DockerPath $dockerPath -VerboseOutput:$VerboseOutput # print current status before we try

    Write-Info "Unsealing Vault..."
    $unsealOutput = @(& $dockerPath compose exec -T vault vault operator unseal $unsealKey 2>&1)
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Failed to unseal Vault - is the container running?"
        if ($unsealOutput.Count -gt 0) {
            $tail = @($unsealOutput | Select-Object -Last 8)
            Write-Warn ("vault output: " + ($tail -join " | "))
        }
        Write-Warn "Start the stack first: docker compose up -d --build"
        exit 1
    }
    Write-Ok "Vault unsealed"
}
