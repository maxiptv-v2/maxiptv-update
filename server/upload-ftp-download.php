$ftpServer = "ftpupload.net"
$ftpUser = "if0_40255943"
$ftpPass = "SVprFEyrgWIP"
$remoteFolder = "/htdocs"
$files = @("download.php")

foreach ($file in $files) {
    Write-Host "Fazendo upload de $file..."
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
        Write-Host "$file enviado com sucesso!"
        $response.Close()
    } catch {
        Write-Host "Erro ao enviar $file : $_"
    }
}
Write-Host "Upload concluido!"
