# tools invite [-r <url>] [-e <email>] [-v]
# Send organizer invite key and test registration flow
# Default: targets https://localhost with test@example.com
# Key is sent via email, then choose to complete registration in tool or on website

function Invoke-TestOrganizerKey {
    param(
        [string]$BaseUrl,
        [string]$Email = "test@example.com",
        [string]$OrgName = "Test Organization",
        [switch]$VerboseOutput
    )

    $BaseUrl = $BaseUrl.TrimEnd("/")
    
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
    } | ConvertTo-Json

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
            Write-Host ($body | ConvertTo-Json) -ForegroundColor Gray
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
    Write-Info "Copy the 32-character invitation key from the email"
    Write-Host ""
    $keyValue = Read-Host "Paste the key here"
    
    if (-not $keyValue -or $keyValue.Length -ne 32) {
        Write-Err "Invalid key format. Key should be 32 characters."
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
    } else {
        Invoke-RegistrationOnWebsite -Email $Email -KeyValue $keyValue
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
    
    $verifyBody = @{
        key = $KeyValue
    } | ConvertTo-Json

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
    
    if ($username.Length -lt 3 -or $username.Length -gt 50) {
        Write-Err "Username must be 3-50 characters"
        exit 1
    }

    $password = Read-Host "Password (12-100 characters, keep secure)" -AsSecureString
    $passwordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [System.Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($password)
    )

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
    } | ConvertTo-Json

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
    Write-Host "   http://localhost:3000/register-organizer" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. On the registration form:"
    Write-Host ""
    
    if ($KeyValue) {
        Write-Host "   Invitation Key: $KeyValue" -ForegroundColor Yellow
        Write-Host "   (Key already available - copy and paste above)"
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

