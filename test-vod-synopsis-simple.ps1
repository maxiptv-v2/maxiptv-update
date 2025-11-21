# Script para testar e verificar onde a sinopse dos VODs esta vindo da API Xtream Code
# Versao simplificada sem caracteres especiais

param(
    [string]$BaseUrl = "",
    [string]$Username = "",
    [string]$Password = "",
    [int]$VodId = 0
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TESTE DE SINOPSE DOS VODs" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Solicitar credenciais se nao foram fornecidas
if ([string]::IsNullOrEmpty($BaseUrl)) {
    $BaseUrl = Read-Host "Digite a URL base da API (ex: https://seu-servidor.com:porta)"
}

if ([string]::IsNullOrEmpty($Username)) {
    $Username = Read-Host "Digite o username"
}

if ([string]::IsNullOrEmpty($Password)) {
    $securePassword = Read-Host "Digite a password" -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $Password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
}

# Limpar URL base
$BaseUrl = $BaseUrl -replace "/player_api\.php$", ""
$BaseUrl = $BaseUrl -replace "player_api\.php$", ""
if (-not $BaseUrl.EndsWith("/")) {
    $BaseUrl = "$BaseUrl/"
}

Write-Host ""
Write-Host "Testando conexao com a API..." -ForegroundColor Yellow

# 1. Testar autenticacao
try {
    $authUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password
    Write-Host "   URL: $authUrl" -ForegroundColor Gray
    
    $authResponse = Invoke-RestMethod -Uri $authUrl -Method Get -ErrorAction Stop
    Write-Host "   OK - Autenticacao OK" -ForegroundColor Green
    
    if ($authResponse.user_info.auth -ne 1) {
        Write-Host "   ERRO - Autenticacao falhou!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ERRO ao autenticar: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Buscando lista de VODs..." -ForegroundColor Yellow

# 2. Buscar lista de VODs
try {
    $vodListUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password + "&action=get_vod_streams"
    $vodList = Invoke-RestMethod -Uri $vodListUrl -Method Get -ErrorAction Stop
    
    if ($vodList.Count -eq 0) {
        Write-Host "   ERRO - Nenhum VOD encontrado!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   OK - Encontrados $($vodList.Count) VODs" -ForegroundColor Green
    
    # Se nao foi fornecido um ID, usar o primeiro
    if ($VodId -eq 0) {
        $VodId = $vodList[0].stream_id
        Write-Host "   Usando VOD ID: $VodId ($($vodList[0].name))" -ForegroundColor Cyan
    } else {
        Write-Host "   Usando VOD ID fornecido: $VodId" -ForegroundColor Cyan
    }
} catch {
    Write-Host "   ERRO ao buscar lista de VODs: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Buscando informacoes detalhadas do VOD..." -ForegroundColor Yellow

# 3. Buscar informacoes detalhadas do VOD
try {
    $vodInfoUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password + "&action=get_vod_info&vod_id=" + $VodId
    Write-Host "   URL: $vodInfoUrl" -ForegroundColor Gray
    
    $vodInfo = Invoke-RestMethod -Uri $vodInfoUrl -Method Get -ErrorAction Stop
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESULTADO DA RESPOSTA" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Converter para JSON formatado
    $jsonFormatted = $vodInfo | ConvertTo-Json -Depth 10
    
    Write-Host "JSON BRUTO (primeiros 3000 caracteres):" -ForegroundColor Yellow
    $previewLength = [Math]::Min(3000, $jsonFormatted.Length)
    Write-Host $jsonFormatted.Substring(0, $previewLength) -ForegroundColor White
    if ($jsonFormatted.Length -gt 3000) {
        Write-Host "... (truncado)" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  ANALISE DA SINOPSE" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Analisar estrutura
    Write-Host "Estrutura da resposta:" -ForegroundColor Yellow
    Write-Host "   Tipo: $($vodInfo.GetType().Name)" -ForegroundColor White
    
    $synopsisFound = $false
    $synopsisLocation = ""
    $synopsisContent = ""
    
    if ($vodInfo.PSObject.Properties.Name -contains "info") {
        Write-Host "   OK - Campo 'info' encontrado" -ForegroundColor Green
        
        $info = $vodInfo.info
        if ($info) {
            Write-Host "   Campos em 'info':" -ForegroundColor Cyan
            foreach ($prop in $info.PSObject.Properties.Name) {
                $value = $info.$prop
                if ($prop -eq "plot" -or $prop -eq "Plot") {
                    $strValue = $value.ToString()
                    Write-Host "      OK - $prop = $($strValue.Substring(0, [Math]::Min(100, $strValue.Length)))..." -ForegroundColor Green
                } else {
                    $displayValue = if ($value -is [string] -and $value.Length -gt 50) { "$($value.Substring(0, 50))..." } else { $value }
                    Write-Host "      - $prop = $displayValue" -ForegroundColor White
                }
            }
            
            # Verificar plot em info
            if ($info.plot) {
                Write-Host ""
                Write-Host "   OK - SINOPSE ENCONTRADA em info.plot!" -ForegroundColor Green
                $synopsisFound = $true
                $synopsisLocation = "info.plot"
                $synopsisContent = $info.plot.ToString()
                Write-Host "   Conteudo: $($synopsisContent.Substring(0, [Math]::Min(200, $synopsisContent.Length)))..." -ForegroundColor Cyan
            } elseif ($info.Plot) {
                Write-Host ""
                Write-Host "   OK - SINOPSE ENCONTRADA em info.Plot (maiuscula)!" -ForegroundColor Green
                $synopsisFound = $true
                $synopsisLocation = "info.Plot"
                $synopsisContent = $info.Plot.ToString()
                Write-Host "   Conteudo: $($synopsisContent.Substring(0, [Math]::Min(200, $synopsisContent.Length)))..." -ForegroundColor Cyan
            } else {
                Write-Host ""
                Write-Host "   AVISO - SINOPSE NAO ENCONTRADA em info.plot ou info.Plot" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "   ERRO - Campo 'info' NAO encontrado" -ForegroundColor Red
    }
    
    if ($vodInfo.PSObject.Properties.Name -contains "movie_data") {
        Write-Host ""
        Write-Host "   OK - Campo 'movie_data' encontrado" -ForegroundColor Green
        
        $movieData = $vodInfo.movie_data
        if ($movieData) {
            Write-Host "   Campos em 'movie_data':" -ForegroundColor Cyan
            
            # Verificar campos relacionados a sinopse
            $synopsisFields = @("plot", "Plot", "description", "Description", "synopsis", "Synopsis", "overview", "Overview")
            
            foreach ($field in $synopsisFields) {
                if ($movieData.PSObject.Properties.Name -contains $field) {
                    $value = $movieData.$field
                    if ($value -and $value.ToString().Trim() -ne "") {
                        Write-Host "      OK - $field = $($value.ToString().Substring(0, [Math]::Min(100, $value.ToString().Length)))..." -ForegroundColor Green
                        if (-not $synopsisFound) {
                            Write-Host ""
                            Write-Host "   OK - SINOPSE ENCONTRADA em movie_data.$field!" -ForegroundColor Green
                            $synopsisFound = $true
                            $synopsisLocation = "movie_data.$field"
                            $synopsisContent = $value.ToString()
                            Write-Host "   Conteudo: $($synopsisContent.Substring(0, [Math]::Min(200, $synopsisContent.Length)))..." -ForegroundColor Cyan
                        }
                    }
                }
            }
            
            if (-not $synopsisFound) {
                Write-Host "      AVISO - Nenhum campo de sinopse encontrado em movie_data" -ForegroundColor Yellow
                Write-Host "      Campos disponiveis em movie_data:" -ForegroundColor Gray
                foreach ($prop in $movieData.PSObject.Properties.Name) {
                    Write-Host "         - $prop" -ForegroundColor Gray
                }
            }
        }
    } else {
        Write-Host ""
        Write-Host "   AVISO - Campo 'movie_data' NAO encontrado" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESUMO" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    if ($synopsisFound) {
        Write-Host "OK - SINOPSE ENCONTRADA!" -ForegroundColor Green
        Write-Host "   Localizacao: $synopsisLocation" -ForegroundColor Cyan
        Write-Host "   Tamanho: $($synopsisContent.Length) caracteres" -ForegroundColor Cyan
        Write-Host "   Preview: $($synopsisContent.Substring(0, [Math]::Min(150, $synopsisContent.Length)))..." -ForegroundColor White
    } else {
        Write-Host "ERRO - SINOPSE NAO ENCONTRADA em nenhum lugar!" -ForegroundColor Red
        Write-Host "   Verifique o JSON bruto acima para ver a estrutura completa." -ForegroundColor Yellow
    }
    
    Write-Host ""
    
} catch {
    Write-Host "   ERRO ao buscar informacoes do VOD: $_" -ForegroundColor Red
    Write-Host "   Detalhes: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Teste concluido!" -ForegroundColor Green
Write-Host ""

