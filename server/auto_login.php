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

$code = $_GET['code'] ?? '';

if (empty($code)) {
    http_response_code(400);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Codigo nao fornecido'
    ]);
    exit;
}

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// Validar formato do código (alfanumérico, 3-10 caracteres)
if (!preg_match('/^[A-Za-z0-9]{3,10}$/', $code)) {
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
            http_response_code(403);
            echo json_encode([
                'status' => 'erro',
                'mensagem' => 'Codigo expirado. O codigo e valido por 6 horas apos a geracao.'
            ]);
            exit;
        }
    }
    
    // Verificar se usuário expirou (formato DD/MM/YYYY)
    $expiryDate = $user['expiryDate'] ?? '';
    if (!empty($expiryDate) && isExpired($expiryDate)) {
        http_response_code(403);
        echo json_encode([
            'status' => 'erro',
            'mensagem' => 'Usuario expirado ou inativo'
        ]);
        exit;
    }
    
    // Retornar dados para login automático (formato exato esperado pelo app)
    echo json_encode([
        'user' => $user['username'] ?? '',
        'password' => $user['password'] ?? '',
        'api' => $user['apiUrl'] ?? '',
        'expiryDate' => $user['expiryDate'] ?? ''
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

