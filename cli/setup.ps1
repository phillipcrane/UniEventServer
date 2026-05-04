# tools setup
# checks dependencies, creates TLS certs, installs CLI to ~/.local/bin, optional docker stack bringup.
# CLI install to ~/.local/bin, optional docker stack bringup.

function Invoke-Setup {
    param([switch]$VerboseOutput, [switch]$Yes, [switch]$Rebuild)

    $repoRoot = Get-RepoRoot

    Write-Host ""
    Write-Host "  UniEvent Local Dev Setup" -ForegroundColor Cyan
    Write-Sep

    # ── Step 1: Dependency checks ─────────────────────────────────────────────

    Write-Step "Checking dependencies..."
    $failed = @()

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

    $mvn = Get-Command mvn -ErrorAction SilentlyContinue -CommandType Application | Select-Object -First 1
    if ($mvn) {
        Write-Ok "Maven ($($mvn.Source))"
    } else {
        Write-Info "Maven not installed globally (fine - project uses mvnw)"
    }

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

    $dockerPath = Find-Executable -Name "docker" -Fallbacks $script:KnownPaths.docker
    if (-not $dockerPath) {
        Write-Err "Docker not found"
        Write-Warn "Install Docker Desktop from https://www.docker.com/products/docker-desktop"
        $failed += "Docker"
    } else {
        Write-Ok "Docker ($dockerPath)"
    }

    $npmCmd = Get-Command npm -ErrorAction SilentlyContinue -CommandType Application | Select-Object -First 1
    if ($npmCmd) {
        Write-Ok "npm ($($npmCmd.Source))"
    } else {
        Write-Err "Node.js / npm not found"
        Write-Warn "Install from https://nodejs.org"
        $failed += "npm"
    }

    if ($failed.Count -gt 0) {
        Write-Host ""
        Write-Err "Missing dependencies: $($failed -join ', ')"
        Write-Warn "Please install the above and re-run: ./tools setup"
        exit 1
    }

    # ── Step 2: .env check ────────────────────────────────────────────────────

    Write-Step "Checking .env..."
    $envFile = Join-Path $repoRoot ".env"
    if (-not (Test-Path $envFile)) {
        Write-Err ".env file not found"
        Write-Warn "Ask the dev team for the .env file and place it at:"
        Write-Warn "  $envFile"
        Write-Warn "A template is available at .env.example"
        exit 1
    }
    Write-Ok ".env found"

    # ── Step 3: Frontend dependencies ────────────────────────────────────────

    Write-Step "Installing frontend dependencies (web/)..."
    $webDir = Join-Path $repoRoot "web"
    $nodeModules = Join-Path $webDir "node_modules"
    if (Test-Path $nodeModules) {
        Write-Ok "web/node_modules already exists"
    } else {
        $prev = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & npm install --prefix $webDir
        $npmExit = $LASTEXITCODE
        $ErrorActionPreference = $prev
        if ($npmExit -eq 0) {
            Write-Ok "Frontend dependencies installed"
        } else {
            Write-Err "npm install failed (exit $npmExit)"
            exit 1
        }
    }

    # ── Step 5: docker-compose.override.yml ──────────────────────────────────

    Write-Step "Checking docker-compose.override.yml..."
    $overrideDst = Join-Path $repoRoot "docker-compose.override.yml"
    $overrideSrc = Join-Path $repoRoot "docker-compose.override.yml.example"

    if (Test-Path $overrideDst) {
        Write-Ok "docker-compose.override.yml already exists"
    } elseif (Test-Path $overrideSrc) {
        Copy-Item $overrideSrc $overrideDst
        Write-Ok "Created docker-compose.override.yml from example"
    } else {
        Write-Warn "docker-compose.override.yml.example not found - skipping"
    }

    # ── Step 6: TLS certificate ───────────────────────────────────────────────

    Write-Step "Checking TLS certificate..."
    $certsDir  = Join-Path $repoRoot "certs"
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

        Write-Info "Generating self-signed certificate (CN=localhost, SAN=DNS:localhost,IP:127.0.0.1)..."

        # Write a temporary OpenSSL config so SANs work across all OpenSSL versions.
        # Without SAN, Chrome 66+ rejects the cert even for self-signed certs.
        $nginxSanConf = @"
[req]
distinguished_name = req_distinguished_name
x509_extensions    = v3_req
prompt             = no

[req_distinguished_name]
CN = localhost

[v3_req]
subjectAltName = DNS:localhost,IP:127.0.0.1
"@
        $nginxTempConf = [System.IO.Path]::GetTempFileName() + ".cnf"
        $nginxSanConf | Set-Content -Path $nginxTempConf -Encoding ASCII

        $ErrorActionPreference = "Continue"
        $certOutput = @(& $opensslPath req -x509 -nodes -days 3650 -newkey rsa:2048 `
            -keyout $keyFile -out $certFile `
            -config $nginxTempConf 2>&1)
        $certExitCode = $LASTEXITCODE
        $ErrorActionPreference = "Stop"
        Remove-Item $nginxTempConf -ErrorAction SilentlyContinue

        if ((Test-Path $certFile) -and (Test-Path $keyFile)) {
            Write-Ok "Self-signed certificate generated (valid 10 years)"
        } else {
            Write-Err "Certificate generation failed - files not created"
            if ($certExitCode -ne 0 -and $certOutput.Count -gt 0) {
                $tail = @($certOutput | Select-Object -Last 8)
                Write-Warn ("OpenSSL output: " + ($tail -join " | "))
            }
            Write-Warn "Check that OpenSSL is installed and runnable: openssl version"
            exit 1
        }
    }

    # ── Step 7: Vault TLS certificate ────────────────────────────────────────

    Write-Step "Checking Vault TLS certificate..."
    $vaultCertFile = Join-Path $certsDir "vault.crt"
    $vaultKeyFile  = Join-Path $certsDir "vault.key"

    if ((Test-Path $vaultCertFile) -and (Test-Path $vaultKeyFile)) {
        Write-Ok "Vault TLS certificate already exists"
    } else {
        Write-Info "Generating Vault TLS certificate (CN=vault, SAN=DNS:vault,DNS:localhost,IP:127.0.0.1)..."

        # Write a temporary OpenSSL config so SANs work across all OpenSSL versions
        $sanConf = @"
[req]
distinguished_name = req_distinguished_name
x509_extensions    = v3_req
prompt             = no

[req_distinguished_name]
CN = vault

[v3_req]
subjectAltName = DNS:vault,DNS:localhost,IP:127.0.0.1
"@
        $tempConf = [System.IO.Path]::GetTempFileName() + ".cnf"
        $sanConf | Set-Content -Path $tempConf -Encoding ASCII

        $ErrorActionPreference = "Continue"
        $vaultCertOutput = @(& $opensslPath req -x509 -nodes -days 3650 -newkey rsa:2048 `
            -keyout $vaultKeyFile -out $vaultCertFile `
            -config $tempConf 2>&1)
        $vaultCertExitCode = $LASTEXITCODE
        $ErrorActionPreference = "Stop"
        Remove-Item $tempConf -ErrorAction SilentlyContinue

        if ((Test-Path $vaultCertFile) -and (Test-Path $vaultKeyFile)) {
            Write-Ok "Vault TLS certificate generated (valid 10 years)"
        } else {
            Write-Err "Vault TLS certificate generation failed - files not created"
            if ($vaultCertExitCode -ne 0 -and $vaultCertOutput.Count -gt 0) {
                $tail = @($vaultCertOutput | Select-Object -Last 8)
                Write-Warn ("OpenSSL output: " + ($tail -join " | "))
            }
            Write-Warn "Check that OpenSSL is installed and runnable: openssl version"
            exit 1
        }
    }

    # ── Step 8: Install CLI on PATH ───────────────────────────────────────────

    Write-Step "Installing CLI..."

    if ($IsLinux -or $IsMacOS) {
        $localBin = Join-Path $HOME ".local/bin"
        $linkPath  = Join-Path $localBin "tools"
        $scriptPath = Join-Path $repoRoot "tools.sh"

        if (-not (Test-Path $localBin)) {
            New-Item -ItemType Directory -Path $localBin -Force | Out-Null
        }

        if (Test-Path $linkPath) {
            $resolvedScriptPath = (Resolve-Path -LiteralPath $scriptPath).Path
            $existing = ""
            try {
                $linkItem = Get-Item -LiteralPath $linkPath -Force -ErrorAction Stop
                $target = $linkItem.Target
                if ($target -is [array]) { $target = $target[0] }

                if ($target) {
                    $candidate = $target
                    if (-not ([System.IO.Path]::IsPathRooted($candidate))) {
                        $candidate = Join-Path (Split-Path $linkPath -Parent) $candidate
                    }
                    $existing = (Resolve-Path -LiteralPath $candidate -ErrorAction Stop).Path
                }
            } catch {
                $existing = ""
            }

            if ($existing -eq $resolvedScriptPath) {
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

        if ($env:PATH -notmatch [regex]::Escape($localBin)) {
            Write-Host ""
            Write-Warn "~/.local/bin is not on your PATH yet."
            Write-Warn "Add this line to your ~/.bashrc or ~/.zshrc:"
            Write-Host '    export PATH="$HOME/.local/bin:$PATH"' -ForegroundColor White
            Write-Warn "Then restart your terminal (or run: source ~/.bashrc)"
            Write-Warn "After that you can run: tools setup, tools seed, tools refresh"
        } else {
            Write-Info "You can now run: tools setup, tools seed, tools refresh"
        }
    } else {
        # Windows: drop a stub tools.bat into ~\.local\bin (already on PATH via Claude, etc.)
        $localBin  = Join-Path $env:USERPROFILE ".local\bin"
        $stubPath  = Join-Path $localBin "tools.bat"
        $scriptAbs = Join-Path $repoRoot "tools.ps1"

        if (-not (Test-Path $localBin)) {
            New-Item -ItemType Directory -Path $localBin -Force | Out-Null
        }

        $stubContent = "@echo off`r`nwhere /q pwsh.exe`r`nif %errorlevel% equ 0 (`r`n  pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$scriptAbs`" %*`r`n) else (`r`n  powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File `"$scriptAbs`" %*`r`n)`r`n"

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

        # Ensure ~\.local\bin is on user PATH (read directly from registry to avoid stale $env:PATH)
        $regKey   = "HKCU:\Environment"
        $userPath = $null
        try {
            $userPath = (Get-ItemProperty -Path $regKey -Name PATH -ErrorAction Stop).PATH
        } catch {
            # PATH key may not exist in HKCU:\Environment on a clean profile - that's fine
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
            $machinePath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
            $env:PATH = if ($machinePath) { "$newUserPath;$machinePath" } else { $newUserPath }
            Write-Ok "Added $localBin to user PATH (registry)"
        }

        # Also update PowerShell profiles
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

        Write-Info "You can now run: tools seed, tools setup, tools refresh"
    }

    # ── Step 9: Done ──────────────────────────────────────────────────────────

    Write-Host ""
    Write-Sep
    Write-Ok "Setup complete!"
    Write-Host ""

    $answer = if ($Yes) { "Y" } else { Read-Host "  Start the Docker stack now? [Y/n]" }
    if ($answer -eq "" -or $answer -match "^[Yy]") {
        . (Join-Path $PSScriptRoot "vault.ps1")
        . (Join-Path $PSScriptRoot "docker.ps1")
        Invoke-Docker -VerboseOutput:$VerboseOutput -SkipVaultSetup -Yes:$Yes -Rebuild:$Rebuild
        Write-Host ""
        $answer2 = if ($Yes) { "Y" } else { Read-Host "  Initialize / unseal Vault now? [Y/n]" }
        if ($answer2 -eq "" -or $answer2 -match "^[Yy]") {
            Invoke-VaultSetup
        }
    }
    Write-Host ""
}
