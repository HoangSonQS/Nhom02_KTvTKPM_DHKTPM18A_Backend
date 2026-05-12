# run-dev.ps1 — Load .env rồi chạy Spring Boot
# Usage: .\run-dev.ps1

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "Không tìm thấy file .env. Hãy copy từ .env.example rồi điền giá trị."
    exit 1
}

Write-Host "Loading .env ..." -ForegroundColor Cyan

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    # Bỏ qua dòng trống và comment (#)
    if ($line.Length -eq 0) { return }
    if ($line[0] -eq '#') { return }
    # Tách KEY=VALUE
    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        $key   = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
        Write-Host "  $key = $value" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "Starting Spring Boot..." -ForegroundColor Green
.\mvnw spring-boot:run
