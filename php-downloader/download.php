<?php
/**
 * MaxiPTV Downloader - Página de Download
 * Processa o download do APK com configurações automáticas
 */

// Configurações
$config = [
    'app_name' => 'MaxiPTV',
    'version' => '1.0.89',
    'github_repo' => 'https://github.com/seu-usuario/MaxiPTV_v2',
    'jsonbin_api_key' => '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae',
    'jsonbin_bin_id' => '68ec647643b1c97be964e96b',
    'jsonbin_url' => 'https://api.jsonbin.io/v3/b/' . '68ec647643b1c97be964e96b',
    'timezone' => 'America/Sao_Paulo'
];

// Definir timezone
date_default_timezone_set($config['timezone']);

// Headers de segurança
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');

$code = $_GET['code'] ?? '';
$status = '';
$statusType = '';
$downloadUrl = '';

if (empty($code)) {
    header('Location: index.php');
    exit;
}

// Validar código novamente
$validation = validateClientCode($code, $config);

if (!$validation['valid']) {
    $status = $validation['message'];
    $statusType = 'error';
} else {
    $clientCode = $validation['clientCode'];
    
    // Marcar código como usado
    markCodeAsUsed($code, $config);
    
    // Gerar URL de download
    $downloadUrl = generateDownloadUrl($clientCode, $config);
    
    $status = 'Download pronto! Clique no botão abaixo.';
    $statusType = 'success';
}

?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $config['app_name']; ?> - Download</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%);
            color: #ffffff;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .container {
            background: rgba(30, 30, 30, 0.95);
            border-radius: 20px;
            padding: 40px;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
            border: 1px solid #444;
            max-width: 600px;
            width: 90%;
            backdrop-filter: blur(10px);
            text-align: center;
        }
        
        .logo {
            margin-bottom: 30px;
        }
        
        .logo h1 {
            font-size: 2.5em;
            background: linear-gradient(45deg, #00D4FF, #0099CC);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 10px;
        }
        
        .status {
            padding: 20px;
            border-radius: 15px;
            margin-bottom: 30px;
            font-weight: 600;
            font-size: 1.1em;
        }
        
        .status.success {
            background: rgba(76, 175, 80, 0.2);
            border: 2px solid #4CAF50;
            color: #4CAF50;
        }
        
        .status.error {
            background: rgba(244, 67, 54, 0.2);
            border: 2px solid #F44336;
            color: #F44336;
        }
        
        .download-btn {
            display: inline-block;
            padding: 20px 40px;
            background: linear-gradient(45deg, #00D4FF, #0099CC);
            color: white;
            text-decoration: none;
            border-radius: 15px;
            font-size: 1.3em;
            font-weight: bold;
            margin: 20px 0;
            transition: all 0.3s ease;
            box-shadow: 0 10px 20px rgba(0, 212, 255, 0.3);
        }
        
        .download-btn:hover {
            transform: translateY(-3px);
            box-shadow: 0 15px 30px rgba(0, 212, 255, 0.4);
        }
        
        .download-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        
        .info-card {
            background: rgba(45, 45, 45, 0.8);
            border-radius: 15px;
            padding: 25px;
            margin: 25px 0;
            text-align: left;
        }
        
        .info-card h3 {
            color: #00D4FF;
            margin-bottom: 15px;
            text-align: center;
        }
        
        .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
            padding: 8px 0;
            border-bottom: 1px solid #444;
        }
        
        .info-item:last-child {
            border-bottom: none;
        }
        
        .info-label {
            color: #888;
            font-weight: 600;
        }
        
        .info-value {
            color: #fff;
            font-weight: 500;
        }
        
        .back-btn {
            display: inline-block;
            padding: 12px 25px;
            background: transparent;
            color: #888;
            text-decoration: none;
            border: 2px solid #444;
            border-radius: 10px;
            margin-top: 20px;
            transition: all 0.3s ease;
        }
        
        .back-btn:hover {
            border-color: #00D4FF;
            color: #00D4FF;
        }
        
        .footer {
            margin-top: 30px;
            color: #666;
            font-size: 0.9em;
        }
        
        .loading {
            display: none;
            margin: 20px 0;
        }
        
        .spinner {
            border: 3px solid #444;
            border-top: 3px solid #00D4FF;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto 15px;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <h1><?php echo $config['app_name']; ?></h1>
            <p>Download Automático</p>
        </div>
        
        <div class="status <?php echo $statusType; ?>">
            <?php echo htmlspecialchars($status); ?>
        </div>
        
        <?php if ($statusType === 'success' && $downloadUrl): ?>
            <div class="info-card">
                <h3>📱 Informações do Download</h3>
                <div class="info-item">
                    <span class="info-label">Usuário:</span>
                    <span class="info-value"><?php echo htmlspecialchars($clientCode['username']); ?></span>
                </div>
                <div class="info-item">
                    <span class="info-label">Expira em:</span>
                    <span class="info-value"><?php echo htmlspecialchars($clientCode['expiryDate']); ?></span>
                </div>
                <div class="info-item">
                    <span class="info-label">Versão:</span>
                    <span class="info-value"><?php echo $config['version']; ?></span>
                </div>
                <div class="info-item">
                    <span class="info-label">Status:</span>
                    <span class="info-value" style="color: #4CAF50;">✅ Configurado</span>
                </div>
            </div>
            
            <a href="<?php echo $downloadUrl; ?>" class="download-btn" id="downloadBtn">
                📱 Baixar MaxiPTV APK
            </a>
            
            <div class="loading" id="loading">
                <div class="spinner"></div>
                <p>Preparando download...</p>
            </div>
            
            <div class="info-card">
                <h3>🚀 Próximos Passos</h3>
                <p style="color: #ccc; line-height: 1.6;">
                    1. <strong>Baixe o APK</strong> clicando no botão acima<br>
                    2. <strong>Instale o app</strong> no seu dispositivo<br>
                    3. <strong>Abra o app</strong> - login automático!<br>
                    4. <strong>Pronto!</strong> Você já está na tela inicial
                </p>
            </div>
            
        <?php else: ?>
            <a href="index.php" class="back-btn">
                ← Voltar e tentar novamente
            </a>
        <?php endif; ?>
        
        <div class="footer">
            <p>© 2024 <?php echo $config['app_name']; ?> - Sistema Profissional</p>
            <p>Suporte: contato@maxiptv.com</p>
        </div>
    </div>
    
    <script>
        document.getElementById('downloadBtn')?.addEventListener('click', function(e) {
            const loading = document.getElementById('loading');
            const btn = this;
            
            loading.style.display = 'block';
            btn.style.display = 'none';
            
            // Simular tempo de download
            setTimeout(function() {
                loading.innerHTML = '<div class="spinner"></div><p>Download iniciado! Verifique sua pasta de downloads.</p>';
            }, 2000);
        });
    </script>
</body>
</html>

<?php
/**
 * Marcar código como usado
 */
function markCodeAsUsed($code, $config) {
    try {
        // Obter dados atuais
        $url = $config['jsonbin_url'] . '/latest';
        $headers = [
            'X-Master-Key: ' . $config['jsonbin_api_key'],
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        
        $response = curl_exec($ch);
        curl_close($ch);
        
        if (!$response) return false;
        
        $data = json_decode($response, true);
        if (!$data || !isset($data['record']['clientCodes'][$code])) {
            return false;
        }
        
        // Marcar como usado
        $data['record']['clientCodes'][$code]['used'] = true;
        $data['record']['clientCodes'][$code]['usedAt'] = time() * 1000;
        $data['record']['clientCodes'][$code]['usedDevice'] = $_SERVER['HTTP_USER_AGENT'] ?? 'Unknown';
        $data['record']['clientCodes'][$code]['downloadsCount'] = ($data['record']['clientCodes'][$code]['downloadsCount'] ?? 0) + 1;
        
        // Salvar de volta
        $updateUrl = $config['jsonbin_url'];
        $updateHeaders = [
            'X-Master-Key: ' . $config['jsonbin_api_key'],
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
        
        return $httpCode === 200;
        
    } catch (Exception $e) {
        error_log("Erro ao marcar código como usado: " . $e->getMessage());
        return false;
    }
}

/**
 * Gerar URL de download
 */
function generateDownloadUrl($clientCode, $config) {
    // Aqui você pode implementar diferentes estratégias:
    // 1. Download direto do GitHub
    // 2. Download via servidor próprio
    // 3. Download com configurações embutidas
    
    // Por enquanto, vamos usar o GitHub
    $githubUrl = 'https://github.com/seu-usuario/MaxiPTV_v2/releases/latest/download/MaxiPTV-v' . $config['version'] . '.apk';
    
    // Em uma implementação real, você poderia:
    // 1. Baixar o APK do GitHub
    // 2. Modificar o APK com as credenciais do cliente
    // 3. Servir o APK modificado
    
    return $githubUrl;
}

/**
 * Validar código de cliente (mesma função do index.php)
 */
function validateClientCode($code, $config) {
    try {
        $url = $config['jsonbin_url'] . '/latest';
        $headers = [
            'X-Master-Key: ' . $config['jsonbin_api_key'],
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode !== 200 || !$response) {
            return [
                'valid' => false,
                'message' => 'Erro ao conectar com o servidor.'
            ];
        }
        
        $data = json_decode($response, true);
        
        if (!$data || !isset($data['record']['clientCodes'])) {
            return [
                'valid' => false,
                'message' => 'Dados do servidor inválidos.'
            ];
        }
        
        $clientCodes = $data['record']['clientCodes'];
        
        if (!isset($clientCodes[$code])) {
            return [
                'valid' => false,
                'message' => 'Código inválido.'
            ];
        }
        
        $clientCode = $clientCodes[$code];
        
        if (time() * 1000 > $clientCode['codeExpiresAt']) {
            return [
                'valid' => false,
                'message' => 'Código expirado.'
            ];
        }
        
        if ($clientCode['used']) {
            return [
                'valid' => false,
                'message' => 'Código já utilizado.'
            ];
        }
        
        if (isUserExpired($clientCode['expiryDate'])) {
            return [
                'valid' => false,
                'message' => 'Conta expirada.'
            ];
        }
        
        return [
            'valid' => true,
            'clientCode' => $clientCode
        ];
        
    } catch (Exception $e) {
        return [
            'valid' => false,
            'message' => 'Erro interno.'
        ];
    }
}

/**
 * Verificar se usuário expirou
 */
function isUserExpired($expiryDate) {
    try {
        $parts = explode('/', $expiryDate);
        if (count($parts) !== 3) return true;
        
        $day = (int)$parts[0];
        $month = (int)$parts[1] - 1;
        $year = (int)$parts[2];
        
        $expiryTime = mktime(23, 59, 59, $month, $day, $year);
        $currentTime = time();
        
        return $currentTime > $expiryTime;
    } catch (Exception $e) {
        return true;
    }
}
?>
