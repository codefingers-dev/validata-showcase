$ApiUrl = "http://localhost:8080"

Write-Host "API VALIDATION" -ForegroundColor Cyan

Write-Host "`n1. Health Check..." -ForegroundColor Yellow
try {
    $Health = Invoke-RestMethod -Uri "$ApiUrl/api/v1/invoices/health" -Method GET
    Write-Host "OK - $Health" -ForegroundColor Green
} catch {
    Write-Host "FAIL - $_" -ForegroundColor Red
}

Write-Host "`n2. Swagger UI..." -ForegroundColor Yellow
try {
    $Swagger = Invoke-WebRequest -Uri "$ApiUrl/swagger-ui.html" -ErrorAction Stop
    if ($Swagger.StatusCode -eq 200) {
        Write-Host "OK" -ForegroundColor Green
    }
} catch {
    Write-Host "FAIL - $_" -ForegroundColor Red
}

Write-Host "`n3. OpenAPI Schema..." -ForegroundColor Yellow
try {
    $OpenAPI = Invoke-RestMethod -Uri "$ApiUrl/v3/api-docs" -Method GET
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "FAIL - $_" -ForegroundColor Red
}

Write-Host "`n4. JSON Analysis..." -ForegroundColor Yellow
try {
    $TestData = @{
        invoiceNumber = "TEST-$(Get-Random)"
        workshopName = "Test"
        grossAmount = 1500.00
        licensePlate = "M-AB 1234"
        netAmount = 1261.00
        vatAmount = 239.00
        taxNumber = "123/456/78901"
    } | ConvertTo-Json
    
    $Response = Invoke-RestMethod -Uri "$ApiUrl/api/v1/invoices/analyze/json" -Method POST -Body $TestData -ContentType "application/json"
    
    Write-Host "OK - riskScore: $($Response.riskScore)" -ForegroundColor Green
} catch {
    Write-Host "FAIL - $_" -ForegroundColor Red
}

Write-Host "`nVALIDATION COMPLETE!" -ForegroundColor Green
