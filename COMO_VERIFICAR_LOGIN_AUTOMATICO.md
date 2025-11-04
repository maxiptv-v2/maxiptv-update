# Como Verificar se o Login Automático Está Funcionando

## 📋 Checklist de Verificação

### 1. ✅ Servidor (PHP) - JÁ VERIFICADO E FUNCIONANDO

Execute o script para verificar:
```powershell
.\verificar-app-autologin.ps1 -Code "SEU_CODIGO"
```

O script verifica:
- ✅ Código existe no JSONBin
- ✅ `get-pending-code.php` retorna o código corretamente
- ✅ `auto_login.php` retorna formato JSON correto:
  ```json
  {
    "user": "max",
    "password": "1234",
    "api": "https://aztv.cx",
    "expiryDate": "12/05/2026"
  }
  ```

### 2. 🔍 Verificar se o App está Chamando as APIs

#### Opção A: Ver logs do app em tempo real
```bash
adb logcat | grep -E "HomeNav|LoginScreen|auto_login|get-pending"
```

#### Opção B: Ver logs completos do app
```bash
adb logcat > logs.txt
# Depois abra logs.txt e procure por:
# - "HomeNav"
# - "get-pending-code"
# - "auto_login"
# - "Login automático"
```

### 3. 📱 O que o App DEVE Fazer (Passo a Passo)

1. **App abre** → `HomeNav` é carregado
2. **Verifica usuário logado** → Se não logado, continua
3. **Chama get-pending-code.php** → Deve ver no log:
   ```
   🔍 Buscando código pendente: https://maxiptv-update-1.onrender.com/get-pending-code.php
   ```
4. **Se código encontrado** → Deve ver no log:
   ```
   ✅ Código pendente encontrado: 1078
   ```
5. **Chama auto_login.php** → Deve ver no log:
   ```
   📡 Chamando auto_login.php?code=1078
   ```
6. **Recebe credenciais** → Deve ver no log:
   ```
   ✅ Login automático iniciado para: max
   ```
7. **Faz login** → Deve ver no log:
   ```
   ✅ Login automático bem-sucedido: max
   ```
8. **Navega para home** → Deve ver no log:
   ```
   🏠 Login automático completo! Navegando para HOME
   🚀 Executando navegação para home após login automático
   ✅ Navegação para 'home' executada com sucesso!
   ```

### 4. ❌ Se Algo Falhar - Onde Está o Erro?

#### Erro: "get-pending-code.php retornou HTTP != 200"
- **Causa:** Servidor Render não está respondendo
- **Solução:** Verifique se o serviço Render está "Live"
- **Verificar:** Acesse `https://maxiptv-update-1.onrender.com/get-pending-code.php` no navegador

#### Erro: "Código pendente não encontrado"
- **Causa:** `dl.php` não salvou o código em `_pending_logins`
- **Solução:** Verifique se `dl.php` está salvando corretamente
- **Verificar:** Execute `testar-fluxo-login-automatico.ps1 -Code "SEU_CODIGO"`

#### Erro: "auto_login.php retornou campos incompletos"
- **Causa:** `auto_login.php` não está retornando todos os campos
- **Solução:** Verifique o formato do JSON retornado
- **Verificar:** Execute `verificar-app-autologin.ps1 -Code "SEU_CODIGO"`

#### Erro: "Erro no login automático"
- **Causa:** `UserManager.login()` falhou
- **Solução:** Verifique se as credenciais estão corretas
- **Verificar:** Veja os logs do app para o erro específico do `UserManager`

#### Erro: "ERRO ao navegar para home"
- **Causa:** `NavHost` não está pronto quando tenta navegar
- **Solução:** Já corrigido no código - adicionado delay e retry
- **Verificar:** Veja os logs para ver se o retry funcionou

### 5. 🔧 Como Corrigir Problemas Comuns

#### Problema: App não chama get-pending-code.php
**Verificar em HomeNav.kt:**
```kotlin
// Deve ter esta linha:
val pendingUrl = "https://maxiptv-update-1.onrender.com/get-pending-code.php"
```

#### Problema: App não processa resposta do auto_login.php
**Verificar em HomeNav.kt:**
```kotlin
// Deve ler os campos assim:
val user = json.optString("user", "")
val pass = json.optString("password", "")
val api = json.optString("api", "")
val expiryDate = json.optString("expiryDate", "")
```

#### Problema: App não navega para home
**Verificar em HomeNav.kt:**
```kotlin
// Deve ter:
shouldNavigateToHome = true
// E um LaunchedEffect que observa shouldNavigateToHome
```

### 6. 📊 Teste Completo do Fluxo

1. **Gerar código no painel admin**
2. **Acessar dl.php no navegador:**
   ```
   https://maxiptv-update-1.onrender.com/dl/SEU_CODIGO
   ```
3. **Verificar se código foi salvo:**
   ```powershell
   .\testar-fluxo-login-automatico.ps1 -Code "SEU_CODIGO"
   ```
4. **Instalar app no dispositivo**
5. **Abrir app e verificar logs:**
   ```bash
   adb logcat | grep -E "HomeNav|auto_login"
   ```
6. **Verificar se navegou para home automaticamente**

### 7. ✅ Checklist Final

- [ ] Código existe no JSONBin
- [ ] `dl.php` salva código em `_pending_logins`
- [ ] `get-pending-code.php` retorna o código
- [ ] `auto_login.php` retorna JSON correto
- [ ] App chama `get-pending-code.php` ao abrir
- [ ] App chama `auto_login.php` com o código
- [ ] App processa resposta JSON corretamente
- [ ] App chama `UserManager.login()` com credenciais
- [ ] App navega para `home` após login

### 8. 🆘 Se Nada Funcionar

1. **Limpar dados do app:**
   ```bash
   adb uninstall com.maxiptv
   ```
2. **Reinstalar app:**
   ```bash
   adb install app/build/outputs/apk/release/maxiptv-release.apk
   ```
3. **Verificar logs novamente**
4. **Executar scripts de diagnóstico:**
   ```powershell
   .\testar-fluxo-login-automatico.ps1 -Code "SEU_CODIGO"
   .\verificar-app-autologin.ps1 -Code "SEU_CODIGO"
   ```

