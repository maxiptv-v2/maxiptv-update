# Script para fazer upload via FTP no 000webhost
# https://000webhost.com

Write-Host "Para usar este script:" -ForegroundColor Cyan
Write-Host "1. Crie uma conta em https://000webhost.com" -ForegroundColor Yellow
Write-Host "2. Crie um site (pode usar qualquer nome)" -ForegroundColor Yellow
Write-Host "3. Pegue as credenciais FTP:" -ForegroundColor Yellow
Write-Host "   - FTP Host: ftpupload.net" -ForegroundColor White
Write-Host "   - FTP User: (seu usuario)" -ForegroundColor White
Write-Host "   - FTP Pass: (sua senha)" -ForegroundColor White
Write-Host "4. Edite este script e coloque as credenciais" -ForegroundColor Yellow
Write-Host "5. Execute: .\upload-000webhost.ps1" -ForegroundColor Yellow
Write-Host ""

# CONFIGURAR AQUI quando tiver as credenciais:
$ftpServer = "ftpupload.net"
$ftpUser = "SEU_USUARIO_AQUI"
$ftpPass = "SUA_SENHA_AQUI"
$remoteFolder = "/public_html"

# Mudar para diretorio do server
Set-Location -Path "C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2\server"

Write-Host "Upload de arquivos para $ftpServer..." -ForegroundColor Cyan

$files = @("download.php")

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Enviando $file..." -ForegroundColor Yellow
        
        try {
            $ftp = "ftp://$ftpServer$remoteFolder/$file"
            $ftpWebRequest = [System.Net.FtpWebRequest]::Create($ftp)
            $ftpWebRequest.Credentials = New-Object System.Net.NetworkCredential($ftpUser, $ftpPass)
            $ftpWebRequest.Method = [System.Net.WebRequestMethods+Ftp]::UploadFile
            $ftpWebRequest.UseBinary = $true
            $ftpWebRequest.UsePassive = $true
            
            $content = [System.IO.File]::ReadAllBytes($file)
            $ftpWebRequest.ContentLength = $content.Length
            
            $requestStream = $ftpWebRequest.GetRequestStream()
            $requestStream.Write($content, 0, $content.Length)
            $requestStream.Close()
            
            $response = $ftpWebRequest.GetResponse()
            Write-Host "$file enviado com sucesso!" -ForegroundColor Green
            $response.Close()
        } catch {
            Write-Host "Erro ao enviar $file : $_" -ForegroundColor Red
        }
    }
}

Write-Host "Upload concluido!" -ForegroundColor Cyan

