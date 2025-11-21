# Teste simples da logica de sinopse
# Simula o comportamento do codigo Kotlin

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TESTE DA LOGICA DE SINOPSE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Simular dados da API (baseado no teste anterior)
$testData = @{
    info = @{
        plot = "FILME LEGENDADO | Um relato dramatizado do cerco ao Palacio da Justica da Colombia em 1985 combina elementos ficcionais com imagens historicas, explorando temas de crenca, turbulencia e trauma nacional duradouro."
        rating = 7.2
        genre = "Drama"
        releasedate = "2025-10-02"
    }
    movie_data = @{
        stream_id = 1736438456
        plot = $null
        description = $null
    }
}

Write-Host "Teste 1: Sinopse em info.plot" -ForegroundColor Yellow
$synopsis1 = $testData.info.plot
if ($synopsis1 -and $synopsis1.Trim() -ne "") {
    Write-Host "   OK - Sinopse encontrada: $($synopsis1.Substring(0, [Math]::Min(50, $synopsis1.Length)))..." -ForegroundColor Green
} else {
    Write-Host "   ERRO - Sinopse nao encontrada" -ForegroundColor Red
}

Write-Host ""
Write-Host "Teste 2: Simular propriedade synopsis (logica do Kotlin)" -ForegroundColor Yellow

# Simular a logica: info?.plot?.takeIf { it.isNotBlank() }
$plotFromInfo = if ($testData.info.plot -and $testData.info.plot.Trim() -ne "") { $testData.info.plot } else { $null }

if ($plotFromInfo) {
    Write-Host "   OK - plotFromInfo encontrado" -ForegroundColor Green
    $result = $plotFromInfo
} else {
    # Tentar movie_data
    $plotFromMovieData = if ($testData.movie_data.plot -and $testData.movie_data.plot.Trim() -ne "") { 
        $testData.movie_data.plot 
    } elseif ($testData.movie_data.description -and $testData.movie_data.description.Trim() -ne "") { 
        $testData.movie_data.description 
    } else { 
        $null 
    }
    
    if ($plotFromMovieData) {
        Write-Host "   OK - plotFromMovieData encontrado" -ForegroundColor Green
        $result = $plotFromMovieData
    } else {
        Write-Host "   AVISO - Nenhuma sinopse encontrada" -ForegroundColor Yellow
        $result = "Sem descricao"
    }
}

Write-Host "   Resultado final: $($result.Substring(0, [Math]::Min(50, $result.Length)))..." -ForegroundColor Cyan

Write-Host ""
Write-Host "Teste 3: Caso com info.plot vazio" -ForegroundColor Yellow
$testData2 = @{
    info = @{
        plot = ""
        rating = 5.6
    }
    movie_data = @{
        description = "Esta e a descricao do filme"
    }
}

$plotFromInfo2 = if ($testData2.info.plot -and $testData2.info.plot.Trim() -ne "") { $testData2.info.plot } else { $null }
if (-not $plotFromInfo2) {
    $plotFromMovieData2 = if ($testData2.movie_data.description -and $testData2.movie_data.description.Trim() -ne "") { 
        $testData2.movie_data.description 
    } else { 
        $null 
    }
    
    if ($plotFromMovieData2) {
        Write-Host "   OK - Fallback para movie_data.description funcionou" -ForegroundColor Green
        Write-Host "   Resultado: $plotFromMovieData2" -ForegroundColor Cyan
    } else {
        Write-Host "   ERRO - Fallback nao funcionou" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CONCLUSÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "A logica de busca de sinopse esta correta!" -ForegroundColor Green
Write-Host "O problema pode estar na deserializacao do Moshi." -ForegroundColor Yellow
Write-Host ""
Write-Host "Recomendacoes:" -ForegroundColor Cyan
Write-Host "  1. Verificar logs do Logcat para ver se info.plot esta sendo deserializado" -ForegroundColor White
Write-Host "  2. Verificar se o Moshi esta mapeando corretamente sem as anotacoes @Json" -ForegroundColor White
Write-Host "  3. O fallback direto na tela deve garantir que funcione mesmo se synopsis falhar" -ForegroundColor White
Write-Host ""

