<?php
/**
 * MaxiPTV Downloader - Sistema de Download Automático
 * Interface principal para clientes inserirem códigos PHP
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

?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $config['app_name']; ?> - Download Automático</title>
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
            max-width: 500px;
            width: 90%;
            backdrop-filter: blur(10px);
        }
        
        .logo {
            text-align: center;
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
        
        .logo p {
            color: #888;
            font-size: 1.1em;
        }
        
        .form-group {
            margin-bottom: 25px;
        }
        
        label {
            display: block;
            margin-bottom: 8px;
            color: #00D4FF;
            font-weight: 600;
        }
        
        input[type="text"] {
            width: 100%;
            padding: 15px;
            border: 2px solid #444;
            border-radius: 10px;
            background: #1a1a1a;
            color: #fff;
            font-size: 16px;
            transition: all 0.3s ease;
        }
        
        input[type="text"]:focus {
            outline: none;
            border-color: #00D4FF;
            box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
        }
        
        .btn {
            width: 100%;
            padding: 15px;
            background: linear-gradient(45deg, #00D4FF, #0099CC);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 18px;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s ease;
            margin-bottom: 20px;
        }
        
        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px rgba(0, 212, 255, 0.3);
        }
        
        .btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        
        .status {
            padding: 15px;
            border-radius: 10px;
            margin-bottom: 20px;
            text-align: center;
            font-weight: 600;
        }
        
        .status.success {
            background: rgba(76, 175, 80, 0.2);
            border: 1px solid #4CAF50;
            color: #4CAF50;
        }
        
        .status.error {
            background: rgba(244, 67, 54, 0.2);
            border: 1px solid #F44336;
            color: #F44336;
        }
        
        .status.info {
            background: rgba(33, 150, 243, 0.2);
            border: 1px solid #2196F3;
            color: #2196F3;
        }
        
        .instructions {
            background: rgba(45, 45, 45, 0.8);
            border-radius: 10px;
            padding: 20px;
            margin-top: 20px;
        }
        
        .instructions h3 {
            color: #00D4FF;
            margin-bottom: 15px;
        }
        
        .instructions ol {
            padding-left: 20px;
        }
        
        .instructions li {
            margin-bottom: 8px;
            color: #ccc;
        }
        
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #666;
            font-size: 0.9em;
        }
        
        .loading {
            display: none;
            text-align: center;
            margin: 20px 0;
        }
        
        .spinner {
            border: 3px solid #444;
            border-top: 3px solid #00D4FF;
            border-radius: 50%;
            width: 30px;
            height: 30px;
            animation: spin 1s linear infinite;
            margin: 0 auto 10px;
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
            <p>Download Automático v<?php echo $config['version']; ?></p>
        </div>
        
        <?php
        $status = '';
        $statusType = '';
        
        if ($_POST) {
            $code = trim($_POST['code'] ?? '');
            
            if (empty($code)) {
                $status = 'Por favor, digite o código PHP fornecido.';
                $statusType = 'error';
            } else {
                // Validar código
                $validation = validateClientCode($code, $config);
                
                if ($validation['valid']) {
                    $status = 'Código válido! Iniciando download...';
                    $statusType = 'success';
                    
                    // Redirecionar para download
                    echo '<script>
                        setTimeout(function() {
                            window.location.href = "download.php?code=' . urlencode($code) . '";
                        }, 1500);
                    </script>';
                } else {
                    $status = $validation['message'];
                    $statusType = 'error';
                }
            }
        }
        
        if ($status): ?>
            <div class="status <?php echo $statusType; ?>">
                <?php echo htmlspecialchars($status); ?>
            </div>
        <?php endif; ?>
        
        <form method="POST" id="codeForm">
            <div class="form-group">
                <label for="code">🔑 Código PHP:</label>
                <input 
                    type="text" 
                    id="code" 
                    name="code" 
                    placeholder="Digite seu código aqui..."
                    value="<?php echo htmlspecialchars($_POST['code'] ?? ''); ?>"
                    required
                    autocomplete="off"
                >
            </div>
            
            <button type="submit" class="btn" id="submitBtn">
                📱 Baixar App Automaticamente
            </button>
        </form>
        
        <div class="loading" id="loading">
            <div class="spinner"></div>
            <p>Validando código e preparando download...</p>
        </div>
        
        <div class="instructions">
            <h3>📋 Como funciona:</h3>
            <ol>
                <li>Digite o código PHP fornecido pelo administrador</li>
                <li>O sistema valida automaticamente seu código</li>
                <li>O app é baixado com suas credenciais pré-configuradas</li>
                <li>Login automático - você vai direto para a tela inicial!</li>
            </ol>
        </div>
        
        <div class="footer">
            <p>© 2024 <?php echo $config['app_name']; ?> - Sistema Profissional</p>
            <p>Suporte: contato@maxiptv.com</p>
        </div>
    </div>
    
    <script>
        document.getElementById('codeForm').addEventListener('submit', function(e) {
            const submitBtn = document.getElementById('submitBtn');
            const loading = document.getElementById('loading');
            
            submitBtn.disabled = true;
            submitBtn.textContent = 'Validando...';
            loading.style.display = 'block';
        });
        
        // Auto-focus no campo de código
        document.getElementById('code').focus();
    </script>
</body>
</html>

<?php
/**
 * Validar código de cliente
 */
function validateClientCode($code, $config) {
    try {
        // Fazer requisição para JSONBin
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
                'message' => 'Erro ao conectar com o servidor. Tente novamente.'
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
                'message' => 'Código inválido ou não encontrado.'
            ];
        }
        
        $clientCode = $clientCodes[$code];
        
        // Verificar se código expirou
        if (time() * 1000 > $clientCode['codeExpiresAt']) {
            return [
                'valid' => false,
                'message' => 'Código expirado. Solicite um novo código.'
            ];
        }
        
        // Verificar se já foi usado
        if ($clientCode['used']) {
            return [
                'valid' => false,
                'message' => 'Código já foi utilizado. Solicite um novo código.'
            ];
        }
        
        // Verificar se conta do usuário não expirou
        if (isUserExpired($clientCode['expiryDate'])) {
            return [
                'valid' => false,
                'message' => 'Sua conta expirou. Entre em contato com o administrador.'
            ];
        }
        
        return [
            'valid' => true,
            'clientCode' => $clientCode
        ];
        
    } catch (Exception $e) {
        error_log("Erro na validação: " . $e->getMessage());
        return [
            'valid' => false,
            'message' => 'Erro interno do servidor. Tente novamente.'
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
        $month = (int)$parts[1] - 1; // PHP months are 0-based
        $year = (int)$parts[2];
        
        $expiryTime = mktime(23, 59, 59, $month, $day, $year);
        $currentTime = time();
        
        return $currentTime > $expiryTime;
    } catch (Exception $e) {
        return true;
    }
}
?>
