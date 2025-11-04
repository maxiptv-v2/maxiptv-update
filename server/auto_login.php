<?php
/**
 * MaxiPTV - auto_login.php
 * Endpoint para login automático no app após download
 * 
 * Uso: https://maxiptv-update-1.onrender.com/auto_login.php?code=6789
 * 
 * Retorna credenciais do usuário baseado no código do painel admin
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Função para adicionar log
function addLog($type, $message, $data = []) {
    global $jsonbin_url, $jsonbin_update, $apiKey;
    
    try {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        
        $response = curl_exec($ch);
        curl_close($ch);
        
        $data_record = json_decode($response, true);
        $record = $data_record['record'] ?? [];
        
        if (!isset($record['_login_logs']) || !is_array($record['_login_logs'])) {
            $record['_login_logs'] = [];
        }
        
        $record['_login_logs'][] = [
            'timestamp' => time(),
            'datetime' => date('Y-m-d H:i:s'),
            'type' => $type,
            'message' => $message,
            'data' => $data,
            'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'unknown'
        ];
        
        if (count($record['_login_logs']) > 100) {
            $record['_login_logs'] = array_slice($record['_login_logs'], -100);
        }
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_update);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($record));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "Content-Type: application/json",
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_exec($ch);
        curl_close($ch);
    } catch (Exception $e) {
        // Silenciar erros de log
    }
}

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

$code = $_GET['code'] ?? '';

// Log de chamada inicial
addLog('info', 'App chamou auto_login.php', [
    'endpoint' => 'auto_login.php',
    'code' => $code,
    'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
    'user_agent' => substr($_SERVER['HTTP_USER_AGENT'] ?? 'unknown', 0, 100)
]);

if (empty($code)) {
    http_response_code(400);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Codigo nao fornecido'
    ]);
    exit;
}

// Validar formato do código (alfanumérico, 3-10 caracteres)
if (!preg_match('/^[A-Za-z0-9]{3,10}$/', $code)) {
    addLog('error', 'Codigo invalido (formato)', [
        'endpoint' => 'auto_login.php',
        'code' => $code
    ]);
    
    http_response_code(400);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Codigo invalido'
    ]);
    exit;
}

// Buscar dados do JSONBin
try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200 || !$response) {
        http_response_code(500);
        echo json_encode([
            'status' => 'erro',
            'mensagem' => 'Erro ao conectar com servidor'
        ]);
        exit;
    }
    
    $data = json_decode($response, true);
    
    if (!isset($data['record'])) {
        http_response_code(500);
        echo json_encode([
            'status' => 'erro',
            'mensagem' => 'Erro ao ler dados do servidor'
        ]);
        exit;
    }
    
    $codigos = $data['record'];
    
    // Verificar se código existe
    if (!isset($codigos[$code]) || !is_array($codigos[$code])) {
        addLog('error', 'Codigo nao encontrado no JSONBin', [
            'endpoint' => 'auto_login.php',
            'code' => $code
        ]);
        
        http_response_code(404);
        echo json_encode([
            'status' => 'erro',
            'mensagem' => 'Codigo invalido ou nao encontrado'
        ]);
        exit;
    }
    
    $user = $codigos[$code];

    // Verificar se código expirou (6 horas após criação)
    if (isset($user['createdAt'])) {
        $createdAt = (int)$user['createdAt'];
        $sixHoursInMs = 6 * 60 * 60 * 1000; // 6 horas em milissegundos
        $validUntil = $createdAt + $sixHoursInMs;
        $currentTime = round(microtime(true) * 1000); // timestamp em milissegundos
        
        if ($currentTime > $validUntil) {
            addLog('error', 'Codigo expirado (mais de 6 horas)', [
                'endpoint' => 'auto_login.php',
                'code' => $code,
                'createdAt' => $createdAt,
                'validUntil' => $validUntil,
                'currentTime' => $currentTime
            ]);
            
            http_response_code(403);
            echo json_encode([
                'status' => 'erro',
                'mensagem' => 'Codigo expirado. O codigo e valido por 6 horas apos a geracao.'
            ]);
            exit;
        }
    }

    // Validar se usuário expirou (formato DD/MM/YYYY) - VALIDAÇÃO CRÍTICA
    // Esta validação deve ser feita ANTES de retornar credenciais para login automático
    $expiryDate = $user['expiryDate'] ?? '';
    if (!empty($expiryDate) && isExpired($expiryDate)) {
        addLog('error', 'Usuario expirado', [
            'endpoint' => 'auto_login.php',
            'code' => $code,
            'username' => $user['username'] ?? '',
            'expiryDate' => $expiryDate
        ]);
        
        http_response_code(403);
        echo json_encode([
            'status' => 'expired',
            'message' => 'Assinatura expirada',
            'expiryDate' => $expiryDate
        ]);
        exit;
    }

    // Retornar dados para login automático (formato esperado pelo app)
    // O app espera: { "status": "success", "autologin": { "username", "password", "api_url", "expires_in", "expiryDate" } }
    // expires_in = tempo de validade do código em segundos (6 horas = 21600 segundos)
    // expiryDate = data de expiração do usuário (formato DD/MM/YYYY)
    $expiresIn = 21600; // 6 horas em segundos
    
    addLog('success', 'Credenciais retornadas com sucesso', [
        'endpoint' => 'auto_login.php',
        'code' => $code,
        'username' => $user['username'] ?? '',
        'api_url' => $user['apiUrl'] ?? '',
        'expiryDate' => $expiryDate
    ]);
    
    echo json_encode([
        'status' => 'success',
        'autologin' => [
            'username' => $user['username'] ?? '',
            'password' => $user['password'] ?? '',
            'api_url' => $user['apiUrl'] ?? '',
            'expires_in' => $expiresIn,
            'expiryDate' => $expiryDate // Data de expiração do usuário
        ]
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Erro interno: ' . $e->getMessage()
    ]);
}

/**
 * Verificar se data expirou (formato DD/MM/YYYY)
 */
function isExpired($expiryDate) {
    try {
        if (empty($expiryDate)) return false;
        
        // Converter data do formato DD/MM/YYYY para timestamp
        $parts = explode('/', $expiryDate);
        if (count($parts) !== 3) return true;
        
        $day = (int)$parts[0];
        $month = (int)$parts[1];
        $year = (int)$parts[2];
        
        $expiryTime = mktime(23, 59, 59, $month, $day, $year);
        $currentTime = time();
        
        return $currentTime > $expiryTime;
    } catch (Exception $e) {
        return true;
    }
}
?>

