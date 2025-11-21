# ========================================
# TESTE: Verificar Rating da API
# ========================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE: Rating da API" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Verificar estrutura do modelo
Write-Host "1️⃣ ESTRUTURA DO MODELO:" -ForegroundColor Yellow
$models = Get-Content "app/src/main/java/com/maxiptv/data/Models.kt" -Raw

if ($models -match "data class VodInfoResponse\(val info: VodInfo\?, val movie_data: Map<String,Any>\?\)") {
    Write-Host "   ✅ VodInfoResponse tem movie_data: Map<String,Any>?" -ForegroundColor Green
} else {
    Write-Host "   ❌ VodInfoResponse não encontrado ou estrutura diferente" -ForegroundColor Red
}

if ($models -match "data class VodInfo\(val name: String\?, val plot: String\?, val cover: String\?\)") {
    Write-Host "   ✅ VodInfo tem plot: String?" -ForegroundColor Green
} else {
    Write-Host "   ❌ VodInfo não encontrado ou estrutura diferente" -ForegroundColor Red
}

# 2. Verificar código de busca de rating
Write-Host "`n2️⃣ CÓDIGO DE BUSCA DE RATING:" -ForegroundColor Yellow
$vodDetails = Get-Content "app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt" -Raw

if ($vodDetails -match "val rating = info\?\.movie_data\?\.let") {
    Write-Host "   ✅ Código busca rating em movie_data" -ForegroundColor Green
} else {
    Write-Host "   ❌ Código de busca de rating não encontrado" -ForegroundColor Red
}

# Verificar campos buscados
$campos = @("rating", "imdb_rating", "tmdb_rating", "rate", "score", "vote_average")
$camposEncontrados = 0
foreach ($campo in $campos) {
    if ($vodDetails -match "data\[\`"$campo\`"\]") {
        $camposEncontrados++
    }
}
Write-Host "   📊 Campos buscados: $camposEncontrados/$($campos.Count)" -ForegroundColor White

# 3. Verificar exibição do rating
Write-Host "`n3️⃣ EXIBIÇÃO DO RATING:" -ForegroundColor Yellow
if ($vodDetails -match "if \(rating != null\)") {
    Write-Host "   ✅ Rating é exibido condicionalmente (se não for null)" -ForegroundColor Green
} else {
    Write-Host "   ❌ Código de exibição não encontrado" -ForegroundColor Red
}

# Verificar se rating aparece antes da sinopse
$ratingPos = $vodDetails.IndexOf("if (rating != null)")
$sinopsePos = $vodDetails.IndexOf("text = info?.info?.plot")
if ($ratingPos -lt $sinopsePos -and $ratingPos -ne -1) {
    Write-Host "   ✅ Rating aparece ANTES da sinopse (ordem correta)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Ordem pode estar incorreta ou código não encontrado" -ForegroundColor Yellow
}

# 4. Verificar logs de debug
Write-Host "`n4️⃣ LOGS DE DEBUG:" -ForegroundColor Yellow
if ($vodDetails -match "Campos disponíveis em movie_data") {
    Write-Host "   ✅ Log de campos disponíveis está presente" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Log de debug não encontrado" -ForegroundColor Yellow
}

if ($vodDetails -match "Avaliação encontrada") {
    Write-Host "   ✅ Log de avaliação encontrada está presente" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Log de avaliação não encontrado" -ForegroundColor Yellow
}

# 5. Verificar possíveis problemas
Write-Host "`n5️⃣ POSSÍVEIS PROBLEMAS:" -ForegroundColor Yellow

# Verificar se movie_data pode ser null
if ($vodDetails -match "info\?\.movie_data\?\.let") {
    Write-Host "   ✅ Usa safe call (?.) para movie_data" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Pode não estar usando safe call" -ForegroundColor Yellow
}

# Verificar validação de valores vazios
if ($vodDetails -match "takeIf.*isNotBlank.*!=.*0") {
    Write-Host "   ✅ Valida valores vazios e zeros" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Validação pode estar incompleta" -ForegroundColor Yellow
}

# 6. Verificar se info pode ser null
Write-Host "`n6️⃣ VERIFICAÇÃO DE NULL SAFETY:" -ForegroundColor Yellow
if ($vodDetails -match "info\?\.movie_data") {
    Write-Host "   ✅ Usa safe call para info" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Pode não estar usando safe call para info" -ForegroundColor Yellow
}

# 7. Resumo e recomendações
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESUMO E DIAGNÓSTICO" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "POSSÍVEIS CAUSAS DO PROBLEMA:" -ForegroundColor Yellow
Write-Host "  1. API não retorna movie_data" -ForegroundColor White
Write-Host "  2. Campos de rating têm nomes diferentes" -ForegroundColor White
Write-Host "  3. Valores estão como '0' ou vazios" -ForegroundColor White
Write-Host "  4. movie_data está null" -ForegroundColor White
Write-Host "  5. Tipo de dados diferente (não String nem Number)" -ForegroundColor White

Write-Host "`nPRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "  1. Verificar logs do app quando carrega um filme" -ForegroundColor White
Write-Host "  2. Verificar se 'Campos disponíveis em movie_data' aparece" -ForegroundColor White
Write-Host "  3. Verificar se 'Avaliação encontrada' aparece" -ForegroundColor White
Write-Host "  4. Adicionar mais campos de rating se necessário" -ForegroundColor White
Write-Host "  5. Verificar formato dos dados retornados pela API" -ForegroundColor White

Write-Host "`n========================================`n" -ForegroundColor Cyan

