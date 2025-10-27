# Script para fazer upload via FTP
$ftpServer = "ftp.atwebpages.com"
$ftpUser = "4699254_ma"
$ftpPass = "708090ma"
$remoteFolder = "/public_html/maiptv1.atwebpages.com"

# Mudar para diretorio do server
Set-Location -Path "C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2\server"

Write-Host "Upload de arquivos para $ftpServer..." -ForegroundColor Cyan

# Array de arquivos para enviar
$files = @("download.php", "teste-jsonbin.php")

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
    } else {
        Write-Host "Arquivo $file nao encontrado!" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Upload concluido!" -ForegroundColor Cyan

