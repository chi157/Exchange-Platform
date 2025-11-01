Write-Host "=== Exchange Platform Auth API Tests ===" -ForegroundColor Green

Write-Host "`n[Test 1] Register new user..." -ForegroundColor Yellow
$body = @{
    email = "testuser@example.com"
    password = "password123"
    displayName = "Test User"
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $body -ContentType "application/json"
    Write-Host "SUCCESS:" -ForegroundColor Green
    $result | ConvertTo-Json
} catch {
    Write-Host "FAILED: $_" -ForegroundColor Red
}

Write-Host "`n[Test 2] Login..." -ForegroundColor Yellow
$loginBody = @{
    email = "testuser@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $session = $null
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -SessionVariable session
    $loginResult = $response.Content | ConvertFrom-Json
    Write-Host "SUCCESS:" -ForegroundColor Green
    $loginResult | ConvertTo-Json
} catch {
    Write-Host "FAILED: $_" -ForegroundColor Red
}

Write-Host "`n[Test 3] Get current user..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/me" -Method Get -WebSession $session
    $userInfo = $response.Content | ConvertFrom-Json
    Write-Host "SUCCESS:" -ForegroundColor Green
    $userInfo | ConvertTo-Json
} catch {
    Write-Host "FAILED: $_" -ForegroundColor Red
}

Write-Host "`n[Test 4] Logout..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "http://localhost:8080/api/auth/logout" -Method Post -WebSession $session
    Write-Host "SUCCESS: Logged out (204 No Content)" -ForegroundColor Green
} catch {
    Write-Host "FAILED: $_" -ForegroundColor Red
}

Write-Host "`n[Test 5] Access protected resource without login (should fail)..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get
    Write-Host "FAILED: Should have received 401" -ForegroundColor Red
} catch {
    Write-Host "SUCCESS: Received 401 Unauthorized as expected" -ForegroundColor Green
}

Write-Host "`n=== All Tests Complete ===" -ForegroundColor Green
