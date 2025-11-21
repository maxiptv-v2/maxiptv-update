# Script para testar e verificar onde a sinopse dos VODs está vindo da API Xtream Code
# Uso: .\test-vod-synopsis.ps1

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

# Solicitar credenciais se não foram fornecidas
if ([string]::IsNullOrEmpty($BaseUrl)) {
    $BaseUrl = Read-Host "Digite a URL base da API (ex: https://seu-servidor.com:porta)"
}

if ([string]::IsNullOrEmpty($Username)) {
    $Username = Read-Host "Digite o username"
}

if ([string]::IsNullOrEmpty($Password)) {
    $Password = Read-Host "Digite a password" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password))
}

# Limpar URL base (remover player_api.php se existir)
$BaseUrl = $BaseUrl -replace "/player_api\.php$", ""
$BaseUrl = $BaseUrl -replace "player_api\.php$", ""
if (-not $BaseUrl.EndsWith("/")) {
    $BaseUrl = "$BaseUrl/"
}

Write-Host ""
Write-Host "🔍 Testando conexão com a API..." -ForegroundColor Yellow

# 1. Testar autenticação
try {
    $authUrl = "${BaseUrl}player_api.php?username=$Username" + '&password=' + $Password
    Write-Host "   URL: $authUrl" -ForegroundColor Gray
    
    $authResponse = Invoke-RestMethod -Uri $authUrl -Method Get -ErrorAction Stop
    Write-Host "   ✅ Autenticação OK" -ForegroundColor Green
    
    if ($authResponse.user_info.auth -ne 1) {
        Write-Host "   ❌ Autenticação falhou!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ❌ Erro ao autenticar: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📺 Buscando lista de VODs..." -ForegroundColor Yellow

# 2. Buscar lista de VODs para pegar um ID
try {
    $vodListUrl = "${BaseUrl}player_api.php?username=$Username" + '&password=' + $Password + '&action=get_vod_streams'
    $vodList = Invoke-RestMethod -Uri $vodListUrl -Method Get -ErrorAction Stop
    
    if ($vodList.Count -eq 0) {
        Write-Host "   ❌ Nenhum VOD encontrado!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   ✅ Encontrados $($vodList.Count) VODs" -ForegroundColor Green
    
    # Se não foi fornecido um ID, usar o primeiro
    if ($VodId -eq 0) {
        $VodId = $vodList[0].stream_id
        Write-Host "   📌 Usando VOD ID: $VodId (${($vodList[0].name)})" -ForegroundColor Cyan
    } else {
        Write-Host "   📌 Usando VOD ID fornecido: $VodId" -ForegroundColor Cyan
    }
} catch {
    Write-Host "   ❌ Erro ao buscar lista de VODs: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📋 Buscando informações detalhadas do VOD..." -ForegroundColor Yellow

# 3. Buscar informações detalhadas do VOD
try {
    $vodInfoUrl = "${BaseUrl}player_api.php?username=$Username" + '&password=' + $Password + '&action=get_vod_info&vod_id=' + $VodId
    Write-Host "   URL: $vodInfoUrl" -ForegroundColor Gray
    
    $vodInfo = Invoke-RestMethod -Uri $vodInfoUrl -Method Get -ErrorAction Stop
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESULTADO DA RESPOSTA" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Converter para JSON formatado
    $jsonFormatted = $vodInfo | ConvertTo-Json -Depth 10
    
    Write-Host "📄 JSON BRUTO (primeiros 3000 caracteres):" -ForegroundColor Yellow
    Write-Host $jsonFormatted.Substring(0, [Math]::Min(3000, $jsonFormatted.Length)) -ForegroundColor White
    if ($jsonFormatted.Length -gt 3000) {
        Write-Host "... (truncado)" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  ANÁLISE DA SINOPSE" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Analisar estrutura
    Write-Host "🔍 Estrutura da resposta:" -ForegroundColor Yellow
    Write-Host "   Tipo: $($vodInfo.GetType().Name)" -ForegroundColor White
    
    if ($vodInfo.PSObject.Properties.Name -contains "info") {
        Write-Host "   ✅ Campo 'info' encontrado" -ForegroundColor Green
        
        $info = $vodInfo.info
        if ($info) {
            Write-Host "   📝 Campos em 'info':" -ForegroundColor Cyan
            foreach ($prop in $info.PSObject.Properties.Name) {
                $value = $info.$prop
                if ($prop -eq "plot" -or $prop -eq "Plot") {
                    Write-Host "      ✅ $prop = $($value.Substring(0, [Math]::Min(100, $value.Length)))..." -ForegroundColor Green
                } else {
                    $displayValue = if ($value -is [string] -and $value.Length -gt 50) { "$($value.Substring(0, 50))..." } else { $value }
                    Write-Host "      • $prop = $displayValue" -ForegroundColor White
                }
            }
            
            # Verificar plot em info
            if ($info.plot) {
                Write-Host ""
                Write-Host "   ✅ SINOPSE ENCONTRADA em info.plot!" -ForegroundColor Green
                Write-Host "   📖 Conteúdo: $($info.plot.Substring(0, [Math]::Min(200, $info.plot.Length)))..." -ForegroundColor Cyan
            } elseif ($info.Plot) {
                Write-Host ""
                Write-Host "   ✅ SINOPSE ENCONTRADA em info.Plot (maiúscula)!" -ForegroundColor Green
                Write-Host "   📖 Conteúdo: $($info.Plot.Substring(0, [Math]::Min(200, $info.Plot.Length)))..." -ForegroundColor Cyan
            } else {
                Write-Host ""
                Write-Host "   ⚠️ SINOPSE NÃO ENCONTRADA em info.plot ou info.Plot" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "   ❌ Campo 'info' NÃO encontrado" -ForegroundColor Red
    }
    
    if ($vodInfo.PSObject.Properties.Name -contains "movie_data") {
        Write-Host ""
        Write-Host "   ✅ Campo 'movie_data' encontrado" -ForegroundColor Green
        
        $movieData = $vodInfo.movie_data
        if ($movieData) {
            Write-Host "   📝 Campos em 'movie_data':" -ForegroundColor Cyan
            
            # Verificar campos relacionados a sinopse
            $synopsisFields = @("plot", "Plot", "description", "Description", "synopsis", "Synopsis", "overview", "Overview")
            $foundSynopsis = $false
            
            foreach ($field in $synopsisFields) {
                if ($movieData.PSObject.Properties.Name -contains $field) {
                    $value = $movieData.$field
                    if ($value -and $value.ToString().Trim() -ne "") {
                        Write-Host "      ✅ $field = $($value.ToString().Substring(0, [Math]::Min(100, $value.ToString().Length)))..." -ForegroundColor Green
                        if (-not $foundSynopsis) {
                            Write-Host ""
                            Write-Host "   ✅ SINOPSE ENCONTRADA em movie_data.$field!" -ForegroundColor Green
                            Write-Host "   📖 Conteúdo: $($value.ToString().Substring(0, [Math]::Min(200, $value.ToString().Length)))..." -ForegroundColor Cyan
                            $foundSynopsis = $true
                        }
                    }
                }
            }
            
            if (-not $foundSynopsis) {
                Write-Host "      ⚠️ Nenhum campo de sinopse encontrado em movie_data" -ForegroundColor Yellow
                Write-Host "      📋 Campos disponíveis em movie_data:" -ForegroundColor Gray
                foreach ($prop in $movieData.PSObject.Properties.Name) {
                    Write-Host "         • $prop" -ForegroundColor Gray
                }
            }
        }
    } else {
        Write-Host ""
        Write-Host "   ⚠️ Campo 'movie_data' NÃO encontrado" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESUMO" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    $synopsisFound = $false
    $synopsisLocation = ""
    $synopsisContent = ""
    
    # Verificar info.plot
    if ($vodInfo.info -and $vodInfo.info.plot) {
        $synopsisFound = $true
        $synopsisLocation = "info.plot"
        $synopsisContent = $vodInfo.info.plot
    }
    # Verificar info.Plot (maiúscula)
    elseif ($vodInfo.info -and $vodInfo.info.Plot) {
        $synopsisFound = $true
        $synopsisLocation = "info.Plot"
        $synopsisContent = $vodInfo.info.Plot
    }
    # Verificar movie_data
    elseif ($vodInfo.movie_data) {
        foreach ($field in @("plot", "Plot", "description", "Description", "synopsis", "Synopsis", "overview", "Overview")) {
            if ($vodInfo.movie_data.$field -and $vodInfo.movie_data.$field.ToString().Trim() -ne "") {
                $synopsisFound = $true
                $synopsisLocation = "movie_data.$field"
                $synopsisContent = $vodInfo.movie_data.$field.ToString()
                break
            }
        }
    }
    
    if ($synopsisFound) {
        Write-Host "✅ SINOPSE ENCONTRADA!" -ForegroundColor Green
        Write-Host "   📍 Localização: $synopsisLocation" -ForegroundColor Cyan
        Write-Host "   📏 Tamanho: $($synopsisContent.Length) caracteres" -ForegroundColor Cyan
        Write-Host "   📖 Preview: $($synopsisContent.Substring(0, [Math]::Min(150, $synopsisContent.Length)))..." -ForegroundColor White
    } else {
        Write-Host "❌ SINOPSE NÃO ENCONTRADA em nenhum lugar!" -ForegroundColor Red
        Write-Host "   Verifique o JSON bruto acima para ver a estrutura completa." -ForegroundColor Yellow
    }
    
    Write-Host ""
    
} catch {
    Write-Host "   ❌ Erro ao buscar informações do VOD: $_" -ForegroundColor Red
    Write-Host "   Detalhes: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Teste concluido!" -ForegroundColor Green
Write-Host ""

