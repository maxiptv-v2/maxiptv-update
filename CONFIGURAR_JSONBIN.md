# 🔐 Como Configurar o Sistema de Sessões Globais

## ✅ Sistema Implementado!

O sistema de bloqueio de login duplicado está **PRONTO**! Agora você só precisa configurar o JSONBin.io (2 minutos).

---

## 📋 O Que Foi Implementado:

✅ **SessionManager** - Controla logins globalmente  
✅ **Heartbeat automático** - Mantém sessão ativa (30 em 30 segundos)  
✅ **Timeout** - Sessão expira após 2 minutos sem heartbeat  
✅ **Bloqueio de login duplicado** - Impede logar em 2 dispositivos  
✅ **Admin Panel** - Mostra TODAS as sessões ativas globalmente  
✅ **Logout remoto** - Admin pode deslogar qualquer usuário  

---

## 🚀 Como Funciona:

1. **Cliente compra assinatura "joao123"**
2. **Loga na TV Box dele:**
   - Sistema salva no JSONBin.io: "joao123 está ativo na TV Box Samsung"
   
3. **Amigo dele tenta logar no celular com "joao123":**
   - Sistema verifica no JSONBin.io
   - **BLOQUEIA** com mensagem: "Este usuário já está logado em TV Box Samsung"
   
4. **Heartbeat:**
   - A cada 30 segundos, o app envia um "estou vivo" para o JSONBin
   - Se não enviar por 2 minutos, a sessão expira automaticamente
   
5. **Admin:**
   - Pode ver TODOS os usuários logados em QUALQUER dispositivo
   - Pode forçar logout remoto

---

## ⚙️ PASSO A PASSO - Configuração JSONBin.io

### 1. Criar Conta (GRÁTIS)

1. Acesse: https://jsonbin.io/
2. Clique em **"Sign Up"** (cadastro gratuito)
3. Use seu email
4. Confirme o email

### 2. Obter API Key

1. Faça login em https://jsonbin.io/
2. Clique no seu nome (canto superior direito)
3. Clique em **"API Keys"**
4. Clique em **"Create Access Key"**
5. Dê um nome: **"MaxiPTV Sessions"**
6. **COPIE** a chave que aparece (ex: `$2a$10$ABC123XYZ...`)

### 3. Criar o Bin (Banco de Dados)

1. No painel do JSONBin, clique em **"Create Bin"**
2. Cole este JSON inicial:

```json
{
  "sessions": {}
}
```

3. Clique em **"Create"**
4. **COPIE o BIN ID** que aparece na URL (ex: `67abc12345def678`)
   - URL será algo como: `https://jsonbin.io/app/bins/67abc12345def678`
   - O BIN ID é a última parte: `67abc12345def678`

### 4. Configurar no App

Abra o arquivo: `app/src/main/java/com/maxiptv/data/SessionManager.kt`

**LINHA 22-23**, substitua:

```kotlin
private const val JSONBIN_API_KEY = "$2a$10$YOUR_API_KEY_HERE"
private const val JSONBIN_BIN_ID = "YOUR_BIN_ID_HERE"
```

Por (com seus valores reais):

```kotlin
private const val JSONBIN_API_KEY = "$2a$10$ABC123XYZ..."  // Sua API Key
private const val JSONBIN_BIN_ID = "67abc12345def678"      // Seu BIN ID
```

---

## ✅ Pronto!

Agora compile o app e teste! O sistema vai:

- ✅ Bloquear login duplicado automaticamente
- ✅ Mostrar todas as sessões no Admin Panel
- ✅ Permitir logout remoto
- ✅ Expirar sessões inativas

---

## 🧪 Como Testar:

1. **Instale o app em 2 dispositivos** (ou 2 emuladores)
2. **Crie um usuário "teste" no Admin Panel**
3. **Logue com "teste" no Dispositivo 1** ✅
4. **Tente logar com "teste" no Dispositivo 2** ❌
5. **Deve bloquear** com: "Este usuário já está logado em [nome do dispositivo 1]"
6. **No Admin Panel**, você verá a sessão ativa
7. **Clique em "Deslogar"** no Admin Panel
8. **Agora pode logar no Dispositivo 2** ✅

---

## 💡 Dúvidas Frequentes:

**Q: É grátis?**  
R: Sim! O plano gratuito do JSONBin permite:
- 10.000 requisições/mês
- Suficiente para ~300 usuários ativos

**Q: E se eu tiver mais de 300 usuários?**  
R: Plano Pro custa $10/mês para 100.000 requisições

**Q: Precisa de internet?**  
R: Sim, para verificar sessões. Mas o cache de 24h funciona offline.

**Q: E se o JSONBin sair do ar?**  
R: O app funciona normalmente, mas não bloqueia login duplicado.

---

## 🎯 Resumo:

1. Cadastre em jsonbin.io
2. Copie API Key e BIN ID
3. Cole no SessionManager.kt
4. **PRONTO!** 🚀



