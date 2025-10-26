# Script para fazer upload via FTP - Versao 2
$ftpServer = "ftpupload.net"
$ftpUser = "if0_40255943"
$ftpPass = "SVprFEyrgWIP"

# Testar diferentes pastas
$folders = @("/htdocs", "/public_html", "/domains/maxiptvdowloader.rf.gd/htdocs")

foreach ($folder in $folders) {
    Write-Host "Testando pasta: $folder"
    $remoteFolder = $folder
    $files = @("download.php", "test.php")
    
    foreach ($file in $files) {
        Write-Host "Fazendo upload de $file para $folder..."
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
            Write-Host "$file enviado para $folder com sucesso!"
            $response.Close()
        } catch {
            Write-Host "Erro ao enviar $file para $folder : $_"
        }
    }
}

Write-Host "Upload concluido!"
