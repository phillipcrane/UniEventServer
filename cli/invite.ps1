# tools invite [-r <url>] [-e <email>] [-v]
# Send organizer invite key and test registration flow
# Default: targets https://localhost with test@example.com
# Key is sent via email, then choose to complete registration in tool or on website

# This CLI tool is for testing the organizer invitation and registration flow. It simulates:
# - generting an organizer key for a given email (which would normally be sent via email)
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

    # Step 1: Get admin token
    Write-Info "Getting admin token..."
    $token = Get-AdminToken -BaseUrl $BaseUrl
    if (-not $token) {
        Write-Err "Failed to get admin token"
        exit 1
    }
    Write-Ok "Admin authenticated"

    # Step 2: Generate organizer key
    Write-Info "Generating organizer key for: $Email"
    $generateBody = @{
        email = $Email
        organizationName = $OrgName
    } | ConvertTo-Json -Compress # there are better ways to do a body tbf

    try {
        $headers = @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $token"
        }
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/organizer-key/generate" -Method "POST" `
            -Headers $headers -Body $generateBody -TimeoutSec 30
        
        $body = $resp.Content | ConvertFrom-Json
        Write-Ok "Organizer key generated successfully"
        
        if ($VerboseOutput) {
            Write-Host ""
            Write-Info "Response:"
            $responseText = $body | ConvertTo-Json -Depth 5
            Write-Host (Redact-SensitiveText -Text $responseText) -ForegroundColor Gray
        }
    } catch {
        Write-Err "Failed to generate organizer key: $($_.Exception.Message)"
        exit 1
    }

    # Step 3: Get key from email
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

    # Step 4: Ask about registration method
    Write-Sep
    Write-Host ""
    Write-Host "How would you like to complete the registration?" -ForegroundColor Cyan
    Write-Host "  [1] Complete registration in this tool"
    Write-Host "  [2] Complete registration on the website (frontend)"
    Write-Host ""
    $regMethod = Read-Host "Choose [1 or 2]"

    if ($regMethod -eq "1") {
        Invoke-RegistrationInTool -BaseUrl $BaseUrl -Email $Email -KeyValue $keyValue -VerboseOutput:$VerboseOutput
    } elseif ($regMethod -eq "2") {
        Invoke-RegistrationOnWebsite -Email $Email -KeyValue $keyValue
    } else {
        Write-Err "Invalid choice '$regMethod'. Expected 1 or 2."
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

    $BaseUrl = Assert-ValidBaseUrl -BaseUrl $BaseUrl
    if (-not (Test-ValidEmail -Email $Email)) {
        Write-Err "Invalid email address: $Email"
        exit 1
    }
    if (-not $KeyValue -or $KeyValue -notmatch '^[A-Za-z0-9]{32}$') {
        Write-Err "Invalid organizer key format"
        exit 1
    }

    Write-Step "[1/3] Verifying organizer key..."
    
    $verifyBody = @{
        key = $KeyValue
    } | ConvertTo-Json -Compress

    try {
        $headers = @{ "Content-Type" = "application/json" }
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/organizer-key/verify" -Method "POST" `
            -Headers $headers -Body $verifyBody -TimeoutSec 30
        
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
        $headers = @{ "Content-Type" = "application/json" }
        $resp = Invoke-Web -Uri "$BaseUrl/api/auth/register-with-key" -Method "POST" `
            -Headers $headers -Body $registerBody -TimeoutSec 30
        
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

function Invoke-RegistrationOnWebsite {
    param(
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
    Write-Host "   http://localhost:3000/signup-organizer" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. On the registration form:"
    Write-Host ""
    
    if ($KeyValue) {
        Write-Host "   Invitation Key: ********************************" -ForegroundColor Yellow
        Write-Host "   (Key already in your clipboard from the email - paste it into the form)"
    } else {
        Write-Host "   Get the key from your email at: $Email" -ForegroundColor Yellow
        Write-Host "   Look for subject: 'You're Invited to Organize Events on UniEvent!'"
        Write-Host "   Copy the 32-character key from the email"
    }
    
    Write-Host ""
    Write-Host "3. Fill in registration details:"
    Write-Host "   - Username (3-50 characters)"
    Write-Host "   - Password (12-100 characters, secure)"
    Write-Host "   - Email: $Email"
    Write-Host ""
    Write-Host "4. Click 'Register' to complete"
    Write-Host ""
    Write-Host "Your organizer account will be created with role: organizer" -ForegroundColor Green
    Write-Host ""
}

