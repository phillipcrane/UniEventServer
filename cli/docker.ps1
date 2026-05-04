# tools docker [--down] [--wipe] [--rebuild] [-v]
# Default: start if down, rebuild + restart if already up (using Docker layer cache).
# --down:    stop the stack.
# --wipe:    docker compose down -v (removes containers AND volumes). Prompts for confirmation.
# --rebuild: force --no-cache build (slower but guarantees a clean image).

# this cli tool is indeed a simple wrapper around docker compose. It is run during tools setup to start the stack
# as step 2. Also checks if docker is running/installed
function Invoke-Docker { # Invoke prefix = powershell convention for "run this command"
    # we save the flags as params:
    # -d, --down      => $Down
    # -w, --wipe      => $Wipe
    # --rebuild       => $Rebuild
    # -v, --verbose   => $VerboseOutput
    # -y, --yes       => $Yes
    param([switch]$Down, [switch]$Wipe, [switch]$Rebuild, [switch]$VerboseOutput, [switch]$SkipVaultSetup, [switch]$Yes)

    # 1. first, check if docker is available and the daemon is running before doing anything else
    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) {
        Write-Err "Docker not found"
        Write-Warn "Install Docker Desktop: https://www.docker.com/products/docker-desktop"
        exit 1
    }

    # 2. then check if docker daemon is running
    if (-not (Test-DockerDaemon -DockerPath $dockerPath)) { exit 1 }

    # 3. if the did wipe flag and docker is running plus available, then wipe!
    if ($Wipe) {
        Write-Warn "This will remove all containers AND volumes (database, Vault data, media). All data will be lost."
        if (-not $Yes) {
            $answer = Read-Host "  Are you sure? [y/N]" # default to No if they just press enter
            if ($answer -notmatch "^[Yy]") { Write-Info "Cancelled."; return }
        } else {
            Write-Info "Auto-approved with -y/--yes"
        }
        Write-Info "Wiping stack..."
        $prev = $ErrorActionPreference 
        # NB: ErrorActionPreference is a nice built-in variable that controls how PowerShell responds to errors. 
        $ErrorActionPreference = "Continue" # We set it to "Continue" to allow the script to
        # keep running even if the docker command fails, and then we check the exit code manually.
        & $dockerPath compose down -v # here we run the actual command...
        $exitCode = $LASTEXITCODE # inbuilt var, 0 = success, anything else = failure
        $ErrorActionPreference = $prev
        if ($exitCode -eq 0) { Write-Ok "Stack and all volumes removed" }
        else { Write-Err "docker compose down -v failed (exit $exitCode)"; exit 1 }
        return
    }

    # 4. if just the down flag, then stop but don't wipe
    if ($Down) {
        Write-Info "Stopping stack..."
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $dockerPath compose down # # here we run the actual command...
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $prev
        if ($exitCode -eq 0) { Write-Ok "Stack stopped" }
        else { Write-Err "docker compose down failed (exit $exitCode)"; exit 1 }
        return
    }

    # 5. Finally, if no flags, then we want to start the stack. 
    $stackUp = $false
    # 6. tho check first if the stack is running...
    try {
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $psLines = @(& $dockerPath compose ps 2>$null) # check status via docker compose ps.
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $prev
        $stackUp = ($exitCode -eq 0) -and [bool]($psLines | Where-Object { $_ -match '\brunning\b|\bUp\b' })
    } catch { $stackUp = $false }

    # 7. if its running we wanna restart
    if ($stackUp) {
        Write-Info "Stack is running - rebuilding and restarting..."
    } else {
        Write-Info "Starting stack..."
    }

    # 8. we run docker compose up -d (with --build to force rebuild) and check if it succeeded.
    $started = Invoke-ComposeUp -DockerPath $dockerPath -Quiet:(!$VerboseOutput) -NoCache:$Rebuild
    if (-not $started) {
        Write-Err "docker compose up failed"
        if (-not $VerboseOutput) { Write-Warn "Re-run with -v for full output" }
        exit 1
    }

    # 9. if it started, we wait for services to be healty via timeout loop and checking with docker compose ps.
    Write-Info "Waiting for infrastructure services to be healthy..."
    $elapsed = 0
    $infraReady = $false
    while ($elapsed -lt 150) { # time out after 150s to escape while loop
        # 9a for every 9 seconds...
        Start-Sleep -Seconds 5
        $elapsed += 5
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $psLines = @(& $dockerPath compose ps 2>$null) # ...check status via docker compose ps, ignore errs
        $ErrorActionPreference = $prev

        # 9b look for lines with (starting) or (health: starting) 
        $stillStarting = $psLines | Where-Object { $_ -match '\(starting\)|\(health: starting\)' } # look for status
        $unhealthy     = $psLines | Where-Object { $_ -match '\(unhealthy\)' } # look for unhealthy status
        
        # 9c: if unhealthy...
        if ($unhealthy) {
            Write-Warn "  Service(s) unhealthy at $($elapsed)s - check with: docker compose ps"
            foreach ($line in $unhealthy) { Write-Host "    $line" -ForegroundColor Yellow }
        }
        # 9d: if still starting but not unhealthy, just print waiting message
        if (-not $stillStarting) {
            if ($unhealthy) {
                Write-Warn "Services stopped starting but some are unhealthy ($($elapsed)s)"
            } else {
                Write-Ok "Infrastructure ready ($($elapsed)s)"
            }
            $infraReady = $true
            break
        }
        Write-Info "  Still starting... ($($elapsed)s)"
    }

    # 9e: if timeout, continue anyway but warn
    if (-not $infraReady) {
        Write-Warn "Timed out waiting for services to be healthy (150s) - continuing anyway"
        Write-Warn "Check status with: docker compose ps"
    }

    # 10 start vault and run the vault script in a nother /cli/ file
    if (-not $SkipVaultSetup) {
        Write-Host ""
        Write-Step "Configuring Vault..."
        Invoke-VaultSetup
    }

    # 11. after vault is started, we wait for the app to be healthy again with another timeout loop.
    Write-Host ""
    Write-Info "Waiting for app to be ready..."
    $appReady = $false
    $appLines  = @()
    for ($i = 0; $i -lt 24; $i++) { # same again, timeout after 120s in 5s increments
        Start-Sleep -Seconds 5
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $psLines = @(& $dockerPath compose ps 2>$null) # ...check status via docker compose ps, ignore errs
        $ErrorActionPreference = $prev
        $appLines = @($psLines | Where-Object { $_ -match 'unievent-app' }) 
        $appStatus = $appLines -join " "
        # check for all pines:
        if ($appStatus -match '\(healthy\)')   { Write-Ok "App is ready ($(($i + 1) * 5)s)"; $appReady = $true; break }
        if ($appStatus -match '\(unhealthy\)') { Write-Warn "App is unhealthy - check with: docker compose logs app"; break }
        # if neither healthy nor unhealthy, keep waiting and print status every 15s
        Write-Info "  App starting... ($(($i + 1) * 5)s)"
    }
    # 11e: if we timed out and app is not healthy, warn but continue anyway
    if (-not $appReady -and (($appLines -join " ") -notmatch '\(unhealthy\)')) {
        Write-Warn "App did not report healthy within 120s - it may still be starting"
        Write-Warn "Check status: docker compose ps"
        Write-Warn "Check logs:   docker compose logs app"
    }
}
