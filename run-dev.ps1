# Load .env then run Spring Boot.
# Usage: .\run-dev.ps1

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "Cannot find .env. Copy from .env.example and fill required values."
    exit 1
}

Write-Host "Loading .env ..." -ForegroundColor Cyan

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0) {
        return
    }
    if ($line.StartsWith("#")) {
        return
    }

    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")

        if ($key -match "(?i)(PASSWORD|SECRET|KEY|TOKEN|PRIVATE)") {
            Write-Host "  $key = ***" -ForegroundColor DarkGray
        } else {
            Write-Host "  Loaded: $key" -ForegroundColor DarkGray
        }
    }
}

Write-Host ""
Write-Host "Starting Spring Boot..." -ForegroundColor Green
$mavenFromWrapperCache = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-3.9.14\ed7edd442f634ac1c1ef5ba2b61b6d690b5221091f1a8e1123f5fadcc967520d\bin\mvn.cmd"

if (Test-Path $mavenFromWrapperCache) {
    & $mavenFromWrapperCache spring-boot:run
} else {
    .\mvnw.cmd spring-boot:run
}
