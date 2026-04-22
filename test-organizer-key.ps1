# Test script for Organizer Key Registration endpoints
# This script tests the complete flow: generate -> verify -> register

Write-Host "========================================"
Write-Host "Organizer Key Registration Test Suite"
Write-Host "========================================`n"

$baseUrl = "http://localhost:8080"

# Step 1: Login as admin to get token
Write-Host "[1/5] Logging in as admin..."

$loginBody = @{
    email = "cli@unievent.internal"
    password = "ukhjgfdsrgtyhukiyghftdrhjytyr5trtr65rewqq09090900o90o909090kkkkk"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
      -Method Post `
      -Headers @{"Content-Type" = "application/json"} `
      -Body $loginBody
    
    $adminToken = $loginResponse.token
    Write-Host "[OK] Admin token obtained"
    Write-Host "Token: $($adminToken.Substring(0, 50))...`n"
}
catch {
    Write-Host "[ERROR] Login failed: $_"
    exit 1
}

# Step 2: Generate organizer key (admin only)
Write-Host "[2/5] Generating organizer invitation key..."

$generateBody = @{
    email = "testorganizer@example.com"
    organizationName = "Test Event Company"
} | ConvertTo-Json

try {
    $generateResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/organizer-key/generate" `
      -Method Post `
      -Headers @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $adminToken"
      } `
      -Body $generateBody
    
    Write-Host "[OK] Key generated successfully"
    Write-Host "Message: $($generateResponse.message)"
    Write-Host "Expires in: $($generateResponse.expiresIn) seconds (24 hours)`n"
}
catch {
    Write-Host "[ERROR] Key generation failed: $_"
    exit 1
}

# Step 3: Get the key from database (in real scenario, organizer gets it via email)
Write-Host "[3/5] Retrieving key from database..."
Write-Host "(In production, this would be sent via email)"
Write-Host ""
Write-Host "Run this SQL query to get the key:"
Write-Host "SELECT keyValue FROM organizer_keys WHERE email = 'testorganizer@example.com' ORDER BY createdAt DESC LIMIT 1;"
Write-Host ""

$organizerKey = Read-Host "Paste the key value here"

if ([string]::IsNullOrWhiteSpace($organizerKey)) {
    Write-Host "[ERROR] No key provided"
    exit 1
}

Write-Host "[OK] Key retrieved: $($organizerKey.Substring(0, 10))...`n"

# Step 4: Verify the key (public endpoint)
Write-Host "[4/5] Verifying organizer key..."

$verifyBody = @{
    key = $organizerKey
} | ConvertTo-Json

try {
    $verifyResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/organizer-key/verify" `
      -Method Post `
      -Headers @{"Content-Type" = "application/json"} `
      -Body $verifyBody
    
    $confirmationToken = $verifyResponse.confirmationToken
    $verifyEmail = $verifyResponse.email
    
    Write-Host "[OK] Key verified successfully"
    Write-Host "Email: $verifyEmail"
    Write-Host "Confirmation token expires in: $($verifyResponse.expiresIn) seconds (10 minutes)`n"
}
catch {
    Write-Host "[ERROR] Key verification failed: $_"
    exit 1
}

# Step 5: Register organizer with key (public endpoint)
Write-Host "[5/5] Registering organizer account..."

$registerBody = @{
    confirmationToken = $confirmationToken
    username = "testorganizer"
    password = "SecurePassword123!@#"
    email = $verifyEmail
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register-with-key" `
      -Method Post `
      -Headers @{"Content-Type" = "application/json"} `
      -Body $registerBody
    
    Write-Host "[OK] Organizer registered successfully"
    Write-Host "Username: $($registerResponse.username)"
    Write-Host "Email: $($registerResponse.email)"
    Write-Host "Role: organizer"
    Write-Host "Access token expires in: $($registerResponse.accessTokenExpiresInMs / 1000) seconds"
    Write-Host "Refresh token expires in: $($registerResponse.refreshTokenExpiresInMs / 1000) seconds`n"
    
    Write-Host "========================================"
    Write-Host "[SUCCESS] ALL TESTS PASSED!"
    Write-Host "========================================`n"
    
    Write-Host "New organizer tokens:"
    Write-Host "Access Token: $($registerResponse.token.Substring(0, 50))..."
    Write-Host "Refresh Token: $($registerResponse.refreshToken.Substring(0, 50))..."
    
}
catch {
    Write-Host "[ERROR] Organizer registration failed: $_"
    exit 1
}

# Test error cases
Write-Host ""
Write-Host "========================================"
Write-Host "Testing Error Cases"
Write-Host "========================================`n"

# Test 1: Invalid key
Write-Host "Test: Verify invalid key..."
try {
    $invalidVerifyBody = @{ key = "invalid_key_12345678901234567890" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/organizer-key/verify" `
      -Method Post `
      -Headers @{"Content-Type" = "application/json"} `
      -Body $invalidVerifyBody
    Write-Host "[FAIL] Should have returned 404 but did not"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "[OK] Correctly returned 404 for invalid key"
    }
    else {
        Write-Host "[FAIL] Unexpected status code: $statusCode"
    }
}

# Test 2: Generate key without admin role
Write-Host "Test: Generate key without admin authorization..."
try {
    $noAuthBody = @{
        email = "noauth@example.com"
        organizationName = "Test"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/organizer-key/generate" `
      -Method Post `
      -Headers @{"Content-Type" = "application/json"} `
      -Body $noAuthBody
    Write-Host "[FAIL] Should have returned 401 but did not"
}
catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "[OK] Correctly returned 401 for unauthorized request"
    }
    else {
        Write-Host "[FAIL] Unexpected status code: $statusCode"
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "Test suite completed!"
Write-Host "========================================`n"
