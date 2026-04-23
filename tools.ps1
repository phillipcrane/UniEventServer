#Requires -Version 5.1
<#
.SYNOPSIS
    UniEvent admin CLI - run admin tools against a local or remote server.

.USAGE
    ./tools.ps1 <command> [flags]

.COMMANDS
    setup                  Check dependencies and configure the local dev environment
    docker                 Start (or rebuild/restart) the Docker stack
    vault                  Initialize and/or unseal Vault
    unseal                 Quick unseal Vault (shortcut for restart)
    seed                   Clear and re-seed test data
    refresh                Refresh Facebook page tokens (all, or one with -p)
    ingest                 Manually ingest from a Facebook page (interactive picker or -p)
    invite                 Send organizer invite key and test registration flow

.FLAGS
    -r, --remote <url>   Target server URL (default: https://localhost)
    -e, --email <email>  invite: Email for organizer invite (default: test@example.com)
    -p, --page <id>      Scope to a single page (refresh, ingest)
    -c, --clear          seed: only clear, skip re-seed
    -d, --down           docker: stop the stack
    -w, --wipe           docker/vault: destroy data volumes (prompts for confirmation)
    -v, --verbose        Show extra output
    -h, --help           Show this help

.NOTES
    Command logic lives in cli/<command>.ps1. Shared helpers live in cli/shared.ps1.
    On Windows (PowerShell): use single-dash flags  (-v, -c, -d, -p, -r, -h)
    On Linux/Mac (tools.sh): use double-dash flags  (--verbose, --clear, --down, --page, --remote, --help)
#>

param(
    [Parameter(Position = 0)]
    [string]$Command,

    [Alias("r")]
    [string]$Remote = "",

    [Alias("e")]
    [string]$Email = "",

    [Alias("p")]
    [string]$Page = "",

    [Alias("c")]
    [switch]$Clear,

    [Alias("d")]
    [switch]$Down,

    [Alias("w")]
    [switch]$Wipe,

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

# ── Load shared helpers ───────────────────────────────────────────────────────

$cliDir = Join-Path $PSScriptRoot "cli"
. (Join-Path $cliDir "shared.ps1")

# ── Help / no-command ─────────────────────────────────────────────────────────

if ($Help -or $Command -eq "" -or $Command -eq "-h" -or $Command -eq "--help") {
    Show-Help
    exit 0
}

# ── Dispatch: commands that don't need a live server first ────────────────────

$cmdLower = $Command.ToLower()

switch ($cmdLower) {
    "setup" {
        . (Join-Path $cliDir "vault.ps1")   # setup uses Get-VaultStatus, Invoke-ComposeUp
        . (Join-Path $cliDir "setup.ps1")
        Invoke-Setup -VerboseOutput:$VerboseOutput
        exit 0
    }
    "vault" {
        . (Join-Path $cliDir "vault.ps1")
        if ($Wipe) { Invoke-VaultWipe } else { Invoke-VaultSetup }
        exit 0
    }
    "unseal" {
        . (Join-Path $cliDir "vault.ps1")
        Invoke-Unseal
        exit 0
    }
    "docker" {
        . (Join-Path $cliDir "vault.ps1")
        . (Join-Path $cliDir "docker.ps1")
        Invoke-Docker -Down:$Down -Wipe:$Wipe -VerboseOutput:$VerboseOutput
        exit 0
    }
}

# ── Server-backed commands: require the server to be reachable ────────────────

$baseUrl = if ($Remote -ne "") { $Remote.TrimEnd("/") } else { "https://localhost" }

Write-Info "Connecting to $baseUrl ..."
if (-not (Test-ServerHealth -BaseUrl $baseUrl -VerboseOutput:$VerboseOutput)) {
    Write-Err "Server not running at $baseUrl"
    if ($Remote -eq "") {
        Write-Warn "Start the stack: tools docker"
        Write-Warn "Or run locally: ./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.profiles.active=dev'"
    } else {
        Write-Warn "Check that the remote server is reachable: $baseUrl"
    }
    exit 1
}

Write-Sep

switch ($cmdLower) {
    "seed" {
        . (Join-Path $cliDir "seed.ps1")
        Invoke-Seed -BaseUrl $baseUrl -Clear:$Clear -VerboseOutput:$VerboseOutput
    }
    "refresh" {
        . (Join-Path $cliDir "refresh.ps1")
        Invoke-Refresh -BaseUrl $baseUrl -Page $Page -VerboseOutput:$VerboseOutput
    }
    "ingest" {
        . (Join-Path $cliDir "ingest.ps1")
        Invoke-Ingest -BaseUrl $baseUrl -Page $Page -VerboseOutput:$VerboseOutput
    }
    "invite" {
        . (Join-Path $cliDir "invite.ps1")
        Invoke-TestOrganizerKey -BaseUrl $baseUrl -Email $Email -VerboseOutput:$VerboseOutput
    }
    default {
        Write-Err "Unknown command: '$Command'"
        Write-Host ""
        Show-Help
        exit 1
    }
}

Write-Sep
