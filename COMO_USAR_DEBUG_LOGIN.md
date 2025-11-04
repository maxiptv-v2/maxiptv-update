# 🔍 Sistema de Debug Login Automático - MaxiPTV

## Como Funciona

O sistema agora registra **automaticamente** todas as chamadas do app aos endpoints PHP, permitindo que você veja exatamente o que está acontecendo sem precisar de `adb logcat`.

## 📋 Como Usar

### Opção 1: Via Navegador (Recomendado)

1. **Acesse no navegador:**
   ```
   https://maxiptv-update-1.onrender.com/debug-login.php
   ```

2. **Você verá:**
   - Total de logs registrados
   - Estatísticas (sucessos, erros, avisos)
   - Lista completa de todos os logs com:
     - Data/hora
     - Tipo (success, error, warning, info)
     - Mensagem
     - Dados detalhados (código usado, username, etc.)
     - IP e User-Agent

3. **A página atualiza automaticamente a cada 10 segundos**

### Opção 2: Via Script PowerShell

```powershell
.\verificar-logs-debug.ps1 -Code "4633"
```

Isso mostra os últimos 5 logs no terminal.

## 🔄 Fluxo Esperado

Quando você baixar o APK usando o código `4633`, os seguintes logs devem aparecer:

1. **INFO** - `App chamou get-pending-code.php`
   - Quando o app abre pela primeira vez
   - Dados: IP, User-Agent

2. **SUCCESS** - `Codigo pendente encontrado e retornado`
   - Quando o app encontra o código pendente
   - Dados: código, username

3. **INFO** - `App chamou auto_login.php`
   - Quando o app busca as credenciais
   - Dados: código usado

4. **SUCCESS** - `Credenciais retornadas com sucesso`
   - Quando o login automático funciona
   - Dados: username, api_url, expiryDate

## 🐛 Diagnóstico de Problemas

### Se não aparecer nenhum log:

- ❌ O app **não está chamando** os endpoints PHP
- ✅ **Solução:** Verificar se o app foi compilado com a versão mais recente

### Se aparecer apenas "get-pending-code.php":

- ❌ O app **não está chamando** `auto_login.php`
- ✅ **Solução:** Verificar código do `HomeNav.kt` se está processando o código retornado

### Se aparecer erro em "auto_login.php":

- ❌ **Problema:** Código inválido, expirado ou usuário expirado
- ✅ **Solução:** Verificar o código no JSONBin e validade do usuário

### Se aparecer "SUCCESS" mas o app não faz login:

- ❌ **Problema:** O app está recebendo as credenciais mas não está processando
- ✅ **Solução:** Verificar se o app está processando o formato JSON correto:
  ```json
  {
    "status": "success",
    "autologin": {
      "username": "...",
      "password": "...",
      "api_url": "...",
      "expires_in": 21600,
      "expiryDate": "DD/MM/YYYY"
    }
  }
  ```

## 🧪 Testando Agora

1. **Execute o script de teste:**
   ```powershell
   .\verificar-logs-debug.ps1 -Code "4633"
   ```

2. **Ou acesse diretamente:**
   ```
   https://maxiptv-update-1.onrender.com/debug-login.php
   ```

3. **Depois baixe o APK:**
   ```
   https://maxiptv-update-1.onrender.com/dl/4633
   ```

4. **Instale e abra o app**

5. **Atualize a página de debug** para ver os logs aparecerem em tempo real!

## 📝 Notas Importantes

- Os logs são salvos no JSONBin (estrutura `_login_logs`)
- Mantém apenas os últimos 100 logs para não sobrecarregar
- Cada log contém timestamp, tipo, mensagem, dados detalhados, IP e User-Agent
- Você pode limpar os logs usando o botão "Limpar Logs" na página de debug

## ✅ Próximos Passos

1. Deployar os arquivos atualizados no Render
2. Acessar `debug-login.php` no navegador
3. Baixar o APK usando o código `4633`
4. Instalar e abrir o app
5. Verificar os logs aparecerem em tempo real!

