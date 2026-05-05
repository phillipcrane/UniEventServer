# tools invite [-r <url>] [-e <email>] [-v]
# Send organizer invite key and test registration flow
# Default: targets https://localhost with test@example.com
# Key is sent via email, then choose to complete registration in tool or on website

# This CLI tool tests the organizer invitation and registration flow. It simulates:
# - generating an organizer key for a given email (which would normally be sent via email)
# - verifying the key
# - completing registration with the key (either in the tool or on the website)

function Invoke-TestOrganizerKey {
    param(
        [string]$BaseUrl,
        [string]$Email = "test@example.com",
        [string]$OrgName = "Test Organization",
        [switch]$VerboseOutput # this param is for the -v flag
    )

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl # ensure baseurl is valid in shared.ps1

    # 0. validate email and name before doing anything
    if (-not (Test-ValidEmail -Email $Email)) {
        Write-Err "Invalid email address: $Email"
        exit 1
    }
    Assert-NonEmpty -Name "Organization name" -Value $OrgName

    # intro
    Write-Step "Testing Organizer Key Registration Flow"
    Write-Info "Target: $BaseUrl"
    Write-Info "Test Email: $Email"
    Write-Sep

    # Step 1: Generate organizer key (admin-authenticated endpoint)
    Write-Info "Generating organizer key for: $Email"
    $generateBody = @{ email = $Email; organizationName = $OrgName } | ConvertTo-Json -Compress
    $resp = Invoke-AdminRequest -Method "POST" -Url "$BaseUrl/api/auth/organizer-key/generate" `
        -Body $generateBody -VerboseOutput:$VerboseOutput
    Handle-Response -Response $resp -SuccessMsg "Organizer key generated successfully" -VerboseOutput:$VerboseOutput

    # Step 2: Get key from email
    Write-Sep
    Write-Host ""
    Write-Info "Check your inbox at: $Email"
    Write-Info "Look for subject: 'You're Invited to Organize Events on UniEvent!'"
    Write-Info "Copy the 32-character invitation key from the email (letters and numbers)"
    Write-Host ""
    $keyValue = Read-Host "Paste the key here"

    if (-not $keyValue -or $keyValue.Length -ne 32 -or $keyValue -notmatch '^[A-Za-z0-9]{32}$') {
        Write-Err "Invalid key format. Key must be exactly 32 alphanumeric characters."
        exit 1
    }
    Write-Ok "Key received"

    # Step 3: Ask about registration method
    Write-Sep
    Write-Host ""
    Write-Host "How would you like to complete the registration?" -ForegroundColor Cyan
    Write-Host "  [1] New account - register in this tool"
    Write-Host "  [2] Existing account - upgrade in this tool"
    Write-Host "  [3] Complete on the website (frontend)"
    Write-Host ""
    $regMethod = Read-Host "Choose [1, 2, or 3]"

    if ($regMethod -eq "1") {
        Invoke-RegistrationInTool -BaseUrl $BaseUrl -Email $Email -KeyValue $keyValue -VerboseOutput:$VerboseOutput
    } elseif ($regMethod -eq "2") {
        Invoke-UpgradeInTool -BaseUrl $BaseUrl -Email $Email -KeyValue $keyValue -VerboseOutput:$VerboseOutput
    } elseif ($regMethod -eq "3") {
        Invoke-RegistrationOnWebsite -BaseUrl $BaseUrl -Email $Email -KeyValue $keyValue
    } else {
        Write-Err "Invalid choice '$regMethod'. Expected 1, 2, or 3."
        exit 1
    }

    Write-Sep
}

function Invoke-RegistrationInTool {
    param(
        [string]$BaseUrl,
        [string]$Email,
        [string]$KeyValue,
        [switch]$VerboseOutput
    )

    Write-Step "[1/3] Verifying organizer key..."

    $verifyBody = @{ key = $KeyValue } | ConvertTo-Json -Compress

    try {
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/organizer-key/verify" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json" } -Body $verifyBody -TimeoutSec 30
        $verified = $resp.Content | ConvertFrom-Json
        $confirmationToken = $verified.confirmationToken
        Write-Ok "Key verified successfully"
        if ($VerboseOutput) {
            Write-Info "Confirmation token expires in: $($verified.expiresIn) seconds"
        }
    } catch {
        Write-Err "Key verification failed: $($_.Exception.Message)"
        exit 1
    }

    # Get registration details
    Write-Host ""
    Write-Step "[2/3] Enter registration details"
    $username = Read-Host "Username (3-50 characters)"
    Assert-NonEmpty -Name "Username" -Value $username

    if ($username.Length -lt 3 -or $username.Length -gt 50) {
        Write-Err "Username must be 3-50 characters"
        exit 1
    }

    $password = Read-Host "Password (12-100 characters, keep secure)" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $passwordPlain = $null
    try {
        $passwordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }

    if ($passwordPlain.Length -lt 12 -or $passwordPlain.Length -gt 100) {
        Write-Err "Password must be 12-100 characters"
        exit 1
    }

    # Complete registration
    Write-Step "[3/3] Completing registration..."

    $registerBody = @{
        confirmationToken = $confirmationToken
        username = $username
        email = $Email
        password = $passwordPlain
    } | ConvertTo-Json -Compress

    try {
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/register-with-key" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json" } -Body $registerBody -TimeoutSec 30
        $result = $resp.Content | ConvertFrom-Json
        Write-Ok "Registration successful!"

        if ($VerboseOutput) {
            Write-Host ""
            Write-Info "New organizer account created:"
            Write-Host "  Username: $($result.username)" -ForegroundColor Gray
            Write-Host "  Email: $($result.email)" -ForegroundColor Gray
            Write-Host "  Access token expires in: $($result.accessTokenExpiresInMs)ms" -ForegroundColor Gray
        }
    } catch {
        Write-Err "Registration failed: $($_.Exception.Message)"
        exit 1
    } finally {
        if ($null -ne $passwordPlain) {
            $passwordPlain = ""
        }
    }

    Write-Host ""
    Write-Host "Organizer Account Created Successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Details:" -ForegroundColor Cyan
    Write-Host "  Username: $username"
    Write-Host "  Email: $Email"
    Write-Host "  Role: organizer"
    Write-Host ""
}

function Invoke-UpgradeInTool {
    param(
        [string]$BaseUrl,
        [string]$Email,
        [string]$KeyValue,
        [switch]$VerboseOutput
    )

    Write-Step "[1/3] Verifying organizer key..."

    $verifyBody = @{ key = $KeyValue } | ConvertTo-Json -Compress

    try {
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/organizer-key/verify" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json" } -Body $verifyBody -TimeoutSec 30
        $verified = $resp.Content | ConvertFrom-Json
        $confirmationToken = $verified.confirmationToken
        Write-Ok "Key verified successfully"
        if ($VerboseOutput) {
            Write-Info "Confirmation token expires in: $($verified.expiresIn) seconds"
        }
    } catch {
        Write-Err "Key verification failed: $($_.Exception.Message)"
        exit 1
    }

    # Get user credentials for the existing account
    Write-Host ""
    Write-Step "[2/3] Sign in to the existing account at: $Email"
    $password = Read-Host "Password" -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $passwordPlain = $null
    try {
        $passwordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }

    Write-Step "[3/3] Upgrading account to organizer..."

    $userSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    try {
        # Bootstrap CSRF cookie for the user session (login requires it)
        $csrfResp = Invoke-Web -Uri "$BaseUrl/api/auth/csrf-token" -Method "GET" -TimeoutSec 15 -WebSession $userSession
        $csrfToken = ($csrfResp.Content | ConvertFrom-Json).csrfToken
        if (-not $csrfToken) { throw "CSRF bootstrap response contained no csrfToken." }

        # Log in as the existing user
        $loginBody = @{ email = $Email; password = $passwordPlain } | ConvertTo-Json -Compress
        $loginResp = Invoke-Web -Uri "$BaseUrl/api/auth/login" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json"; "X-CSRF-Token" = $csrfToken } `
            -Body $loginBody -TimeoutSec 30 -WebSession $userSession
        $loginCsrf = ($loginResp.Content | ConvertFrom-Json).csrfToken
        if (-not $loginCsrf) { throw "Login response contained no csrfToken - check server logs." }

        # Upgrade the account
        $upgradeBody = @{ confirmationToken = $confirmationToken } | ConvertTo-Json -Compress
        $upgradeResp = Invoke-Web -Uri "$BaseUrl/api/auth/organizer-key/upgrade" -Method "POST" `
            -Headers @{ "Content-Type" = "application/json"; "X-CSRF-Token" = $loginCsrf } `
            -Body $upgradeBody -TimeoutSec 30 -WebSession $userSession
        $result = $upgradeResp.Content | ConvertFrom-Json
        Write-Ok "Account upgraded to organizer!"

        if ($VerboseOutput) {
            Write-Host ""
            Write-Info "Account details:"
            Write-Host "  Username: $($result.username)" -ForegroundColor Gray
            Write-Host "  Email: $($result.email)" -ForegroundColor Gray
            Write-Host "  Role: $($result.role)" -ForegroundColor Gray
        }
    } catch {
        Write-Err "Upgrade failed: $($_.Exception.Message)"
        exit 1
    } finally {
        if ($null -ne $passwordPlain) { $passwordPlain = "" }
    }

    Write-Host ""
    Write-Host "Account Upgraded Successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Details:" -ForegroundColor Cyan
    Write-Host "  Email: $Email"
    Write-Host "  Role: organizer"
    Write-Host ""
}

function Invoke-RegistrationOnWebsite {
    param(
        [string]$BaseUrl,
        [string]$Email,
        [string]$KeyValue
    )

    Write-Sep
    Write-Host ""
    Write-Host "Complete Registration on Website" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Go to the registration page:"
    Write-Host "   $BaseUrl/signup-organizer" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. On the registration form:"
    Write-Host ""
    Write-Host "   Get the key from your email at: $Email" -ForegroundColor Yellow
    Write-Host "   Look for subject: 'You're Invited to Organize Events on UniEvent!'"
    Write-Host "   Copy the 32-character key from the email and paste it into the form"
    Write-Host ""
    Write-Host "3. If the email address already has an account:"
    Write-Host "   Log in first, then visit the registration page - it will offer"
    Write-Host "   an 'Upgrade to Organizer' option instead of a new-account form."
    Write-Host ""
    Write-Host "   If the email is new, fill in registration details:"
    Write-Host "   - Username (3-50 characters)"
    Write-Host "   - Password (12-100 characters, secure)"
    Write-Host "   - Email: $Email"
    Write-Host ""
    Write-Host "4. Click 'Complete Registration' or 'Upgrade to Organizer' to finish"
    Write-Host ""
    Write-Host "Your organizer account will be created with role: organizer" -ForegroundColor Green
    Write-Host ""
}
