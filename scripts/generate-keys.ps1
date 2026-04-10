# Script tạo cặp khóa RSA cho RS256 JWT (Local Development Only)
# Version: 1.0 (Phase 9 Hardening)

# WARNING: Cặp khóa được tạo bởi script này chỉ dùng cho môi trường DEVELOPMENT.
# Trong môi trường PRODUCTION, hãy sử dụng Hashicorp Vault, AWS Secrets Manager hoặc Azure Key Vault.

$KeyDir = "./config/keys"
if (!(Test-Path $KeyDir)) {
    New-Item -ItemType Directory -Path $KeyDir | Out-Null
}

$PrivateKeyPath = "$KeyDir/private_key.pem"
$PublicKeyPath = "$KeyDir/public_key.pem"

Write-Host "--- Generating RSA Key Pair (2048-bit) ---" -ForegroundColor Cyan

# Sử dụng openssl (yêu cầu đã cài đặt Git Bash hoặc OpenSSL)
if (Get-Command openssl -ErrorAction SilentlyContinue) {
    # Tạo private key
    openssl genrsa -out $PrivateKeyPath 2048
    # Trích xuất public key
    openssl rsa -in $PrivateKeyPath -pubout -out $PublicKeyPath
    
    Write-Host "[SUCCESS] Keys generated at: $KeyDir" -ForegroundColor Green
    Write-Host "Private Key: $PrivateKeyPath"
    Write-Host "Public Key: $PublicKeyPath"
} else {
    Write-Host "[ERROR] OpenSSL not found. Vui lòng cài đặt Git Bash hoặc OpenSSL để chạy script này." -ForegroundColor Red
}

Write-Host "`n--- Hướng dẫn cấu hình application.properties ---" -ForegroundColor Yellow
Write-Host "jwt.private-key-source=file:$PrivateKeyPath"
Write-Host "jwt.public-key-sources=dev-key-1:file:$PublicKeyPath"
Write-Host "jwt.active-kid=dev-key-1"
