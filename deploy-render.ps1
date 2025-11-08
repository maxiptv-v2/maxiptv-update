# Deploy para Render.com
# Faz commit e push dos arquivos PHP atualizados

Write-Host "=== DEPLOY PARA RENDER.COM ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se está no diretório correto
if (-not (Test-Path "server/Dockerfile")) {
    Write-Host "❌ Erro: Execute este script na raiz do projeto" -ForegroundColor Red
    exit 1
}

Write-Host "📋 Arquivos PHP para deploy:" -ForegroundColor Yellow
Write-Host "  - server/valida.php" -ForegroundColor White
Write-Host "  - server/index.php" -ForegroundColor White
Write-Host "  - server/api.php" -ForegroundColor White
Write-Host "  - server/download.php" -ForegroundColor White
Write-Host "  - server/router.php" -ForegroundColor White
Write-Host "  - server/index.html" -ForegroundColor White
Write-Host "  - server/register-aftvnews.php" -ForegroundColor White
Write-Host "  - server/dl.php" -ForegroundColor White
Write-Host "  - server/open.php" -ForegroundColor White
Write-Host "  - server/get-pending-code.php" -ForegroundColor White
Write-Host "  - server/get-token.php" -ForegroundColor White
Write-Host "  - server/auto_login.php ⭐ NOVO" -ForegroundColor Green
Write-Host "  - server/Dockerfile" -ForegroundColor White
Write-Host ""

# Verificar status do git
Write-Host "🔍 Verificando status do Git..." -ForegroundColor Yellow
$status = git status --porcelain 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao verificar status do Git" -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Host "✅ Nenhuma alteração para commitar" -ForegroundColor Green
    Write-Host ""
    Write-Host "💡 Para forçar redeploy no Render:" -ForegroundColor Yellow
    Write-Host "   1. Acesse: https://dashboard.render.com" -ForegroundColor White
    Write-Host "   2. Vá em seu serviço maxiptv-update" -ForegroundColor White
    Write-Host "   3. Clique em 'Manual Deploy' > 'Deploy latest commit'" -ForegroundColor White
    exit 0
}

Write-Host "📝 Alterações encontradas:" -ForegroundColor Yellow
git status --short
Write-Host ""

# Adicionar arquivos
Write-Host "➕ Adicionando arquivos ao Git..." -ForegroundColor Yellow
git add server/valida.php server/index.php server/api.php server/download.php server/router.php server/index.html server/register-aftvnews.php server/dl.php server/open.php server/get-pending-code.php server/get-token.php server/auto_login.php server/Dockerfile

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao adicionar arquivos" -ForegroundColor Red
    exit 1
}

# Commit
Write-Host ""
Write-Host "💾 Fazendo commit..." -ForegroundColor Yellow
$commitMessage = "feat: Adicionar auto_login.php e melhorar login automático

- Criar endpoint auto_login.php para login automático
- Atualizar dl.php para salvar código pendente
- Melhorar get-pending-code.php
- Atualizar HomeNav e LoginScreen para usar auto_login.php
- Login automático completo após download do APK"

git commit -m $commitMessage

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao fazer commit" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Commit realizado com sucesso!" -ForegroundColor Green
Write-Host ""

# Push
Write-Host "🚀 Fazendo push para GitHub..." -ForegroundColor Yellow
git push origin main

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️ Tentando push para master..." -ForegroundColor Yellow
    git push origin master
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao fazer push" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Você pode fazer push manualmente:" -ForegroundColor Yellow
    Write-Host "   git push origin main" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "✅ Push realizado com sucesso!" -ForegroundColor Green
Write-Host ""
Write-Host "📦 Deploy automático no Render:" -ForegroundColor Cyan
Write-Host "   O Render detecta automaticamente o push e inicia o deploy" -ForegroundColor White
Write-Host "   Aguarde alguns minutos para o deploy concluir" -ForegroundColor White
Write-Host ""
Write-Host "Verificar deploy:" -ForegroundColor Yellow
Write-Host '   https://dashboard.render.com' -ForegroundColor White
Write-Host ""
Write-Host "Testar apos deploy:" -ForegroundColor Yellow
Write-Host '   https://maxiptv-update-1.onrender.com/valida.php' -ForegroundColor White
Write-Host '   https://maxiptv-update-1.onrender.com/auto_login.php?code=6789' -ForegroundColor White

