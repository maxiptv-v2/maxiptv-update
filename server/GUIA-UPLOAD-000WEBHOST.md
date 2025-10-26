# 🚀 GUIA COMPLETO - UPLOAD MANUAL PARA 000WEBHOST

## 📋 **PASSO A PASSO PASSO A PASSO:**

### **1️⃣ Criar Conta no 000webhost**
1. Acesse: https://www.000webhost.com/
2. Clique em "Sign Up" 
3. Escolha um nome para seu site (ex: `maxiptv-downloader`)
4. Complete o cadastro com email e senha

### **2️⃣ Fazer Login no Painel**
1. Após criar a conta, faça login
2. Vá em "File Manager" no menu lateral
3. Navegue até a pasta `public_html`

### **3️⃣ Fazer Upload dos Arquivos PHP**
1. Na pasta `server/` do seu projeto, você tem os arquivos:
   - `download.php`
   - `test.php`
   - `config-000webhost.php` (opcional)

2. Abra cada arquivo no editor do 000webhost e cole o conteúdo:
   
   **download.php:** (OBSERVE AS LINHAS 3-4 - CONFIGURE AS CHAVES DO JSONBIN!)
   ```php
   <?php
   // Configurações do JSONBin
   $jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"; // SUA BIN_ID
   $jsonbin_master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"; // SUA MASTER_KEY
   
   // Função para verificar se a data expirou (formato DD/MM/YYYY)
   function isExpired($expiryDate) {
       if (empty($expiryDate)) return true;
       list($day, $month, $year) = array_map('intval', explode('/', $expiryDate));
       $expiryTimestamp = mktime(23, 59, 59, $month, $day, $year);
       return time() > $expiryTimestamp;
   }
   
   // Obter código da URL
   $code = $_GET['code'] ?? null;
   
   if (!$code) {
       http_response_code(400);
       die("❌ Código inválido. Por favor, forneça um código na URL (ex: ?code=1234).");
   }
   
   // Buscar dados do JSONBin
   $ch = curl_init();
   curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
   curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
   curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $jsonbin_master_key"]);
   curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
   curl_setopt($ch, CURLOPT_TIMEOUT, 10);
   $response = curl_exec($ch);
   $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
   curl_close($ch);
   
   if ($response === false || $httpCode != 200) {
       http_response_code(500);
       die("❌ Erro ao conectar com o servidor de códigos. Tente novamente mais tarde.");
   }
   
   $data = json_decode($response, true);
   
   if (!isset($data['record']['simpleCodes'])) {
       http_response_code(500);
       die("❌ Erro na estrutura do servidor de códigos.");
   }
   
   $simpleCodes = $data['record']['simpleCodes'];
   
   // Validar código
   if (!isset($simpleCodes[$code])) {
       http_response_code(404);
       die("❌ Código não encontrado. Verifique se digitou corretamente.");
   }
   
   $clientData = $simpleCodes[$code];
   
   // Verificar se código está ativo
   if (!$clientData['ativo']) {
       http_response_code(403);
       die("❌ Código inativo. Entre em contato com o administrador.");
   }
   
   // Verificar se código já foi usado
   if ($clientData['usado']) {
       http_response_code(403);
       die("❌ Código já foi utilizado. Solicite um novo código ao administrador.");
   }
   
   // Verificar se conta não expirou
   $expiryDate = $clientData['expira_em'];
   if (isExpired($expiryDate)) {
       http_response_code(403);
       die("❌ Sua conta expirou em $expiryDate. Entre em contato com o administrador.");
   }
   
   // Obter URL do APK
   $apkUrl = $clientData['apk'];
   
   if (empty($apkUrl)) {
       http_response_code(500);
       die("❌ URL do APK não configurada. Entre em contato com o administrador.");
   }
   
   // Marcar código como usado
   $clientData['usado'] = true;
   $clientData['usado_em'] = time() * 1000;
   $clientData['usado_device'] = $_SERVER['HTTP_USER_AGENT'] ?? 'Unknown';
   
   // Atualizar no JSONBin
   $data['record']['simpleCodes'][$code] = $clientData;
   
   $updateUrl = $jsonbin_url;
   $updateHeaders = [
       "X-Master-Key: $jsonbin_master_key",
       'Content-Type: application/json'
   ];
   
   $ch = curl_init();
   curl_setopt($ch, CURLOPT_URL, $updateUrl);
   curl_setopt($ch, CURLOPT_HTTPHEADER, $updateHeaders);
   curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
   curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data['record']));
   curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
   curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
   curl_setopt($ch, CURLOPT_TIMEOUT, 10);
   
   $result = curl_exec($ch);
   $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
   curl_close($ch);
   
   // Log da atividade
   $logMessage = date('Y-m-d H:i:s') . " - Código $code usado por " . $clientData['usuario'] . "\n";
   error_log($logMessage);
   
   // Redirecionar para download
   header("Location: " . $apkUrl);
   exit();
   ?>
   ```
   
   **test.php:**
   ```php
   <?php
   echo "✅ Servidor PHP funcionando corretamente!<br>";
   echo "📅 Data/Hora: " . date('d/m/Y H:i:s') . "<br>";
   echo "🌐 Servidor: " . $_SERVER['SERVER_NAME'] . "<br>";
   echo "📁 Pasta: " . __DIR__ . "<br>";
   ?>
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

⚠️ **IMPORTANTE:** Mantenha essas chaves EXATAMENTE como estão! Elas já estão configuradas.

## 📱 **COMO FUNCIONA:**
1. Cliente digita código no downloader
2. Downloader acessa: `https://SEU_SITE.000webhostapp.com/download.php?code=XXXX`
3. PHP valida o código no JSONBin
4. Se válido, redireciona para o APK no GitHub
5. Cliente baixa o app já configurado

## ✅ **CHECKLIST:**
- [ ] Conta criada no 000webhost
- [ ] Arquivos PHP enviados para `public_html`
- [ ] Arquivo `test.php` testado e funcionando
- [ ] Código de teste gerado no painel admin
- [ ] URL completa testada no downloader
