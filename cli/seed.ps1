# tools seed [--wipe]
# Default: clear existing seed data, then re-seed.
# --wipe (-w): only clear, do not re-seed.

# clears existing seed data then re-seeds. With --wipe, just clears and stops.
function Invoke-Seed {
    param([string]$BaseUrl, [switch]$Wipe, [switch]$VerboseOutput)

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl

    # if --wipe, just clear and bail without re-seeding
    if ($Wipe) {
        Write-Info "Clearing seed data..."
        $resp = Invoke-AdminRequest -Method "DELETE" -Url "$BaseUrl/admin/tools/seed" -VerboseOutput:$VerboseOutput
        Handle-Response -Response $resp -SuccessMsg "Seed data cleared" -VerboseOutput:$VerboseOutput
        return
    }

    Write-Info "Clearing existing seed data..."
    $resp = Invoke-AdminRequest -Method "DELETE" -Url "$BaseUrl/admin/tools/seed" -VerboseOutput:$VerboseOutput
    Handle-Response -Response $resp -SuccessMsg "Cleared" -VerboseOutput:$VerboseOutput

    Write-Info "Seeding test data..."
    $resp = Invoke-AdminRequest -Method "POST" -Url "$BaseUrl/admin/tools/seed" -VerboseOutput:$VerboseOutput
    Handle-Response -Response $resp -SuccessMsg "Test data seeded successfully" -VerboseOutput:$VerboseOutput
}
