param(
    [int]$Threads = 5,
    [int]$Iterations = 10
)

$ApiUrl = "http://localhost:8080"
$Results = @()
$StartTime = Get-Date

Write-Host "LOAD TEST START" -ForegroundColor Cyan
Write-Host "Threads: $Threads" -ForegroundColor White
Write-Host "Iterations: $Iterations" -ForegroundColor White
Write-Host "Total: $($Threads * $Iterations) requests" -ForegroundColor White

for ($i = 1; $i -le $Iterations; $i++) {
    Write-Host "Iteration $i / $Iterations" -ForegroundColor Yellow
    
    $Jobs = @()
    
    for ($j = 1; $j -le $Threads; $j++) {
        $Job = Start-Job -ScriptBlock {
            param($Url)
            $ReqStart = Get-Date
            try {
                $Response = Invoke-RestMethod -Uri "$Url/api/v1/invoices/health" -Method GET -TimeoutSec 10 -ErrorAction Stop
                $ReqEnd = Get-Date
                $Duration = ($ReqEnd - $ReqStart).TotalMilliseconds
                
                return @{
                    Success = $true
                    ResponseTime = $Duration
                    Status = "OK"
                }
            } catch {
                return @{
                    Success = $false
                    ResponseTime = 0
                    Status = $_.Exception.Message
                }
            }
        } -ArgumentList $ApiUrl
        
        $Jobs += $Job
    }
    
    $JobResults = $Jobs | Wait-Job | ForEach-Object { Receive-Job $_ }
    
    foreach ($Result in $JobResults) {
        $Results += $Result
    }
    
    $SuccessCount = ($JobResults | Where-Object Success -eq $true).Count
    Write-Host "  Success: $SuccessCount / $Threads" -ForegroundColor Green
}

Write-Host "TEST RESULTS" -ForegroundColor Cyan

$TotalRequests = $Results.Count
$SuccessfulRequests = ($Results | Where-Object Success -eq $true).Count
$FailedRequests = ($Results | Where-Object Success -eq $false).Count
$SuccessRate = ($SuccessfulRequests / $TotalRequests) * 100

Write-Host "Total: $TotalRequests" -ForegroundColor White
Write-Host "Success: $SuccessfulRequests" -ForegroundColor Green
Write-Host "Failed: $FailedRequests" -ForegroundColor Red
Write-Host "Rate: $([math]::Round($SuccessRate, 2))%" -ForegroundColor Green

if ($SuccessfulRequests -gt 0) {
    $ResponseTimes = $Results | Where-Object Success -eq $true | ForEach-Object { $_.ResponseTime }
    $AvgTime = $ResponseTimes | Measure-Object -Average | Select-Object -ExpandProperty Average
    $MaxTime = $ResponseTimes | Measure-Object -Maximum | Select-Object -ExpandProperty Maximum
    $MinTime = $ResponseTimes | Measure-Object -Minimum | Select-Object -ExpandProperty Minimum
    
    Write-Host "Response Times:" -ForegroundColor White
    Write-Host "  Min: $([math]::Round($MinTime, 2)) ms" -ForegroundColor Green
    Write-Host "  Avg: $([math]::Round($AvgTime, 2)) ms" -ForegroundColor Green
    Write-Host "  Max: $([math]::Round($MaxTime, 2)) ms" -ForegroundColor Yellow
}

$EndTime = Get-Date
$Duration = ($EndTime - $StartTime).TotalSeconds
Write-Host "Duration: $([math]::Round($Duration, 2)) seconds" -ForegroundColor White

$Results | Export-Csv -Path "C:\Users\ducho\Desktop\fraudlens-api\jmeter-tests\powershell-results.csv" -NoTypeInformation
Write-Host "Results: powershell-results.csv" -ForegroundColor Yellow
