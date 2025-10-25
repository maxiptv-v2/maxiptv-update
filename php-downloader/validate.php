<?php
/**
 * MaxiPTV Downloader - API de Validação
 * Endpoint para validação de códigos via AJAX
 */

// Configurações
$config = [
    'jsonbin_api_key' => '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae',
    'jsonbin_bin_id' => '68ec647643b1c97be964e96b',
    'jsonbin_url' => 'https://api.jsonbin.io/v3/b/' . '68ec647643b1c97be964e96b',
    'timezone' => 'America/Sao_Paulo'
];

// Definir timezone
date_default_timezone_set($config['timezone']);

// Headers para API
header('Content-Type: application/json');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Permitir apenas POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit;
}

// Obter dados do POST
$input = json_decode(file_get_contents('php://input'), true);
$code = $input['code'] ?? '';

if (empty($code)) {
    http_response_code(400);
    echo json_encode(['error' => 'Code is required']);
    exit;
}

// Validar código
$result = validateClientCode($code, $config);

if ($result['valid']) {
    http_response_code(200);
    echo json_encode([
        'valid' => true,
        'message' => 'Código válido',
        'username' => $result['clientCode']['username'],
        'expiryDate' => $result['clientCode']['expiryDate']
    ]);
} else {
    http_response_code(400);
    echo json_encode([
        'valid' => false,
        'message' => $result['message']
    ]);
}

/**
 * Validar código de cliente
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
                'message' => 'Erro ao conectar com o servidor'
            ];
        }
        
        $data = json_decode($response, true);
        
        if (!$data || !isset($data['record']['clientCodes'])) {
            return [
                'valid' => false,
                'message' => 'Dados do servidor inválidos'
            ];
        }
        
        $clientCodes = $data['record']['clientCodes'];
        
        if (!isset($clientCodes[$code])) {
            return [
                'valid' => false,
                'message' => 'Código inválido'
            ];
        }
        
        $clientCode = $clientCodes[$code];
        
        // Verificar se código expirou
        if (time() * 1000 > $clientCode['codeExpiresAt']) {
            return [
                'valid' => false,
                'message' => 'Código expirado'
            ];
        }
        
        // Verificar se já foi usado
        if ($clientCode['used']) {
            return [
                'valid' => false,
                'message' => 'Código já utilizado'
            ];
        }
        
        // Verificar se conta do usuário não expirou
        if (isUserExpired($clientCode['expiryDate'])) {
            return [
                'valid' => false,
                'message' => 'Conta expirada'
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
            'message' => 'Erro interno do servidor'
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
