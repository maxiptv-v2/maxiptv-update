# 🎯 Fluxo Completo do Sistema MaxiPTV

## 📱 COMO FUNCIONA ATUALMENTE

### 1️⃣ Admin Gera Código (Painel Admin do App MaxiPTV)
```
Painel Admin → Gerar Código → 
  ├─ Usuario: "cliente1"
  ├─ Senha: "senha123"
  ├─ API: "https://api.example.com/"
  ├─ Expiração: "31/12/2025"
  └─ Código: "1234" (4 dígitos aleatórios)
```

### 2️⃣ Código É Salvo no JSONBin
```json
{
  "simpleCodes": {
    "1234": {
      "usuario": "cliente1",
      "senha": "senha123",
      "api": "https://api.example.com/",
      "apk": "https://raw.githubusercontent.com/.../maxiptv-release.apk",
      "expira_em": "31/12/2025",
      "ativo": true,
      "usado": false
    }
  }
}
```

### 3️⃣ Admin Passa Informações ao Cliente
```
Admin entrega ao cliente:
  - Usuario: "cliente1"
  - Senha: "senha123"
  - Código de 4 dígitos: "1234"
```

### 4️⃣ Cliente Usa Downloader Android
```
Cliente abre app Downloader (F-Droid, APKPure, etc.)
  → Digita código: "1234"
  → App Downloader acessa: 
     https://maxiptv-update.onrender.com/download.php?code=1234
```

### 5️⃣ Servidor PHP Valida Código
```php
download.php:
  1. Recebe código: "1234"
  2. Busca no JSONBin
  3. Verifica: existe? ativo? não usado? não expirado?
  4. Marca como USADO (usado=true)
  5. Redireciona para: https://raw.githubusercontent.com/.../maxiptv-release.apk
```

### 6️⃣ Cliente Baixa e Instala APK
```
Downloader faz download do APK
  → Cliente instala o APK
  → Abre o app MaxiPTV
```

### 7️⃣ Cliente Faz Login no App
```
Tela de Login MaxiPTV:
  - Usuario: "cliente1"
  - Senha: "senha123"
  - Clica em ENTRAR
  - App valida no JSONBin
  - Login bem-sucedido!
```

---

## ✅ O QUE ESTÁ PRONTO

- ✅ Servidor PHP funcionando no Render.com
- ✅ Validação de códigos no JSONBin
- ✅ Sistema de login no app
- ✅ Painel Admin gerando códigos
- ✅ Marca códigos como "usado" após download
- ✅ Bloqueio de códigos expirados
- ✅ Bloqueio de códigos já utilizados

---

## 🤔 PERGUNTA

**Quer que o cliente NÃO DIGITE a senha?**

Opção A: Cliente recebe só o código → digite o código no Downloader → baixa APK → login automático?

Opção B (ATUAL): Cliente recebe código + credenciais → baixa APK → digita credenciais → login

Qual você prefere?


