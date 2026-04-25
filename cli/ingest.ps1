# tools ingest [-p <id>]
# Without -p: hits /admin/tools/pages, renders numbered picker, ingests selected.
# With    -p: skips the picker, ingests that page directly.

function Get-PageList {
    param([string]$BaseUrl, [switch]$VerboseOutput)

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl
    $resp = Invoke-AdminRequest -Method "GET" -Url "$BaseUrl/admin/tools/pages" -VerboseOutput:$VerboseOutput
    if ($resp.StatusCode -ne 200) {
        Write-Err "Could not list pages (status $($resp.StatusCode))"
        if ($resp.Body) { Write-Warn (Redact-SensitiveText -Text $resp.Body) }
        exit 1
    }
    try {
        return @($resp.Body | ConvertFrom-Json)
    } catch {
        Write-Err "Could not parse pages response"
        if ($resp.Body) { Write-Warn (Redact-SensitiveText -Text $resp.Body) }
        exit 1
    }
}

function Select-PageInteractive {
    param([object[]]$Pages, [switch]$VerboseOutput)

    if ($Pages.Count -eq 0) {
        Write-Err "No pages are tracked on this server."
        Write-Warn "Seed some test pages with './tools seed', or onboard a real Facebook page via the app."
        exit 1
    }

    Write-Host ""
    Write-Host "  Pages:" -ForegroundColor Cyan
    for ($i = 0; $i -lt $Pages.Count; $i++) {
        $p = $Pages[$i]
        $n = $i + 1
        $status = if ($p.tokenStatus) { $p.tokenStatus } else { "unknown" }
        $days = if ($null -ne $p.tokenExpiresInDays) { "expires in $($p.tokenExpiresInDays) days" } else { "no expiry info" }
        $label = "{0,3}. {1,-40} ({2}, {3})" -f $n, $p.name, $status, $days
        Write-Host "  $label" -ForegroundColor Gray
        if ($VerboseOutput) { Write-Host "       id: $($p.id)" -ForegroundColor DarkGray }
    }
    Write-Host ""

    while ($true) {
        $answer = Read-Host "  Select page [1-$($Pages.Count)] (or q to quit)"
        if ($answer -eq "q" -or $answer -eq "Q") {
            Write-Info "Cancelled."
            exit 0
        }
        $n = 0
        if ([int]::TryParse($answer, [ref]$n) -and $n -ge 1 -and $n -le $Pages.Count) {
            return $Pages[$n - 1]
        }
        Write-Warn "Invalid selection. Enter a number between 1 and $($Pages.Count)."
    }
}

function Invoke-Ingest {
    param([string]$BaseUrl, [string]$Page, [switch]$VerboseOutput)

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl

    $pageId = $Page
    $pageName = $Page

    if (-not $pageId) {
        Write-Info "Fetching tracked pages..."
        $pages = @(Get-PageList -BaseUrl $BaseUrl -VerboseOutput:$VerboseOutput)
        $chosen = Select-PageInteractive -Pages $pages -VerboseOutput:$VerboseOutput
        $pageId = $chosen.id
        $pageName = "$($chosen.name) ($pageId)"
    }

    Write-Info "Ingesting events for page: $pageName"
    $encodedPageId = [System.Uri]::EscapeDataString("$pageId")
    $resp = Invoke-AdminRequest -Method "POST" -Url "$BaseUrl/admin/tools/ingest/$encodedPageId" -VerboseOutput:$VerboseOutput

    switch ($resp.StatusCode) {
        200 {
            $body = $null
            try { $body = $resp.Body | ConvertFrom-Json } catch {}
            if ($body) {
                Write-Ok "Ingested $($body.eventCount) event(s) for page $pageId"
                if ($VerboseOutput -and $body.eventTitles -and $body.eventTitles.Count -gt 0) {
                    Write-Host ""
                    Write-Info "Events:"
                    foreach ($t in $body.eventTitles) { Write-Host "    - $t" -ForegroundColor Gray }
                }
            } else {
                Write-Ok "Ingest completed"
                if ($resp.Body) { Write-Host $resp.Body -ForegroundColor Gray }
            }
        }
        404 {
            Write-Err "Page not found: $pageId"
            exit 1
        }
        502 {
            Write-Err "Facebook API error (502)"
            if ($resp.Body) { Write-Warn (Redact-SensitiveText -Text $resp.Body) }
            exit 1
        }
        500 {
            Write-Err "Server error during ingest (500)"
            if ($resp.Body) { Write-Warn (Redact-SensitiveText -Text $resp.Body) }
            exit 1
        }
        default {
            Write-Err "Unexpected response: $($resp.StatusCode)"
            if ($resp.Body) { Write-Warn (Redact-SensitiveText -Text $resp.Body) }
            exit 1
        }
    }
}
