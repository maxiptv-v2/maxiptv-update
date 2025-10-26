# 🚀 GUIA COMPLETO PARA CONFIGURAR NO 000WEBHOST

## 📋 **PASSO A PASSO:**

### **1️⃣ Criar Conta no 000webhost**
1. Acesse: https://www.000webhost.com/
2. Clique em "Sign Up"
3. Escolha um nome para seu site (ex: `maxiptv-downloader`)
4. Complete o cadastro

### **2️⃣ Fazer Upload dos Arquivos**
1. Faça login no painel do 000webhost
2. Vá em "File Manager"
3. Navegue até a pasta `public_html`
4. Faça upload dos arquivos:
   - `download.php`
   - `test.php`
   - `config-000webhost.php`

### **3️⃣ Configurar o download.php**
1. Abra o arquivo `download.php` no editor do 000webhost
2. Substitua as linhas 3-4 pelos seus dados:
   ```php
   $jsonbin_url = "https://api.jsonbin.io/v3/b/SEU_BIN_ID_AQUI";
   $jsonbin_master_key = "SUA_MASTER_KEY_AQUI";
   ```

### **4️⃣ Testar o Sistema**
1. Acesse: `https://SEU_SITE.000webhostapp.com/test.php`
2. Deve aparecer: "✅ Servidor PHP funcionando!"
3. Teste um código: `https://SEU_SITE.000webhostapp.com/download.php?code=1234`

### **5️⃣ URLs Finais**
- **Teste:** `https://maxiptv-downloader.000webhostapp.com/test.php`
- **Download:** `https://maxiptv-downloader.000webhostapp.com/download.php?code=XXXX`

## 🔧 **CONFIGURAÇÕES IMPORTANTES:**

### **No download.php, linha 3-4:**
```php
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$jsonbin_master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";
```

### **URLs que você vai usar:**
- **Para clientes:** `https://maxiptv-downloader.000webhostapp.com/download.php?code=XXXX`
- **Para testar:** `https://maxiptv-downloader.000webhostapp.com/test.php`

## ⚠️ **IMPORTANTE:**
- Substitua `maxiptv-downloader` pelo nome do seu site
- Mantenha as chaves do JSONBin exatamente como estão
- Teste sempre antes de enviar para clientes

## 🎯 **COMO FUNCIONA:**
1. Cliente digita código no downloader
2. Downloader acessa: `https://SEU_SITE.000webhostapp.com/download.php?code=XXXX`
3. PHP valida o código no JSONBin
4. Se válido, redireciona para o APK no GitHub
5. Cliente baixa o app já configurado
