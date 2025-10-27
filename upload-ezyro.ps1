# Script para fazer upload via FTP no Ezyro

$ftpServer = "ftpupload.net"
$ftpUser = "ezyro_40262171"
$ftpPass = "9ac178b61b"
$remoteFolder = "/htdocs"

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
            Write-Host "URL: http://36466.ezyro.com/$file" -ForegroundColor Cyan
            $response.Close()
        } catch {
            Write-Host "Erro ao enviar $file : $_" -ForegroundColor Red
        }
    } else {
        Write-Host "Arquivo $file nao encontrado!" -ForegroundColor Red
    }
}

Write-Host "Upload concluido!" -ForegroundColor Cyan

