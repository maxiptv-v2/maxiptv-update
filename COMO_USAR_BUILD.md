# 🚀 Como Usar o Sistema de Build Automático

## 📋 **Configuração Inicial (Uma Única Vez)**

### 1️⃣ Configurar Token do GitHub
```powershell
$env:GITHUB_TOKEN = "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"
```

---

## 🔧 **Como Compilar**

### 🧪 **Build DEBUG (Apenas para testes)**
```powershell
.\build-release.ps1 debug
```
**O que faz:**
- ✅ Compila APK em modo debug
- ✅ Gera: `app/build/outputs/apk/debug/app-debug.apk`
- ❌ **NÃO** sobe para GitHub
- ❌ **NÃO** atualiza versão
- ❌ **NÃO** cria release

**Quando usar:** Durante testes e correções

---

### 🚀 **Build RELEASE (Versão oficial)**
```powershell
.\build-release.ps1 release
```
**O que faz:**
- ✅ Incrementa versão automaticamente (v1.0.0 → v1.0.1)
- ✅ Compila APK assinado
- ✅ Renomeia para `maxiptv-release.apk`
- ✅ Faz commit no Git
- ✅ Sobe para GitHub
- ✅ Atualiza `update.json` com nova versão

**Quando usar:** Quando tudo estiver testado e pronto para distribuir

---

## 📝 **Fluxo de Trabalho Recomendado**

1. **Fazer alterações no código**
2. **Testar com:**
   ```powershell
   .\build-release.ps1 debug
   ```
3. **Se tudo OK, fazer release:**
   ```powershell
   .\build-release.ps1 release
   ```

---

## 🎯 **Versionamento Automático**

O script incrementa automaticamente a versão:
- `v1.0.0` → `v1.0.1` → `v1.0.2` → ...
- `versionCode`: 1 → 2 → 3 → ...

---

## 📦 **Arquivos Importantes**

- `version.json`: Controla a versão atual
- `update.json`: Publicado no GitHub para auto-update
- `maxiptv-release.apk`: APK final assinado
- `maxiptv-release.keystore`: Chave de assinatura (não compartilhar!)

---

## 🔐 **Segurança**

⚠️ **NUNCA** commite o token do GitHub no código!
- Use sempre variável de ambiente: `$env:GITHUB_TOKEN`
- O token está configurado apenas na sua máquina

---

## ❓ **Ajuda**

Se o build falhar:
1. Verifique se o token está configurado: `$env:GITHUB_TOKEN`
2. Verifique se está no diretório correto
3. Verifique logs de erro no terminal
