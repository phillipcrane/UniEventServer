# tools status
# Read-only status summary for local Docker/Vault.

# prints a quick status snapshot: Docker daemon, Vault, and container health for app and frontend.
function Invoke-Status {
    param([switch]$VerboseOutput)

    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) {
        Write-Err "Docker not found"
        exit 1
    }

    if (-not (Test-DockerDaemon -DockerPath $dockerPath)) { # defined in docker.ps1
        exit 1
    }

    Write-Info "Docker daemon is running"

    $stackPs = @(& $dockerPath compose ps 2>&1) # snapshot of all container statuses
    $psExitCode = $LASTEXITCODE
    if ($psExitCode -ne 0) {
        Write-Warn "Could not query compose stack"
        if ($stackPs.Count -gt 0) { Write-Warn ($stackPs -join " | ") }
        return
    }

    if ($VerboseOutput) { # dump the full compose ps output if -v was passed
        Write-Info "docker compose ps:"
        Write-Host ($stackPs -join "`n") -ForegroundColor Gray
    }

    $vaultStatus = Get-VaultStatus -DockerPath $dockerPath # defined in vault.ps1, returns a status string
    Write-Info "Vault: $vaultStatus"

    # check the health status of the app and frontend containers by scanning the compose ps lines
    $appLine = $stackPs | Where-Object { $_ -match 'unievent-app' } | Select-Object -First 1
    if ($appLine) {
        if ($appLine -match '\(healthy\)') {
            Write-Ok "App container is healthy"
        } elseif ($appLine -match '\(unhealthy\)') {
            Write-Warn "App container is unhealthy"
        } else {
            Write-Warn "App container is starting or unknown"
        }
    } else {
        Write-Warn "App container not found in compose output"
    }

    $frontendLine = $stackPs | Where-Object { $_ -match 'unievent-frontend' } | Select-Object -First 1
    if ($frontendLine) {
        if ($frontendLine -match '\(healthy\)') {
            Write-Ok "Frontend container is healthy"
        } elseif ($frontendLine -match '\(unhealthy\)') {
            Write-Warn "Frontend container is unhealthy"
        } else {
            Write-Warn "Frontend container is starting or unknown"
        }
    } else {
        Write-Warn "Frontend container not found in compose output"
    }
}
