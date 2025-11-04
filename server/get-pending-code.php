<?php
/**
 * MaxiPTV - get-pending-code.php
 * Retorna o código pendente baseado no IP/User-Agent
 * O app chama isso quando abre pela primeira vez (sem login)
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Configurações JSONBin (definir ANTES da função para usar global)
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

$ip = $_SERVER['REMOTE_ADDR'] ?? '';
$userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';

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

if (empty($ip)) {
    addLog('error', 'IP nao identificado', ['endpoint' => 'get-pending-code.php']);
    echo json_encode(['status' => 'erro', 'mensagem' => 'IP nao identificado']);
    exit;
}

// Log de chamada inicial
addLog('info', 'App chamou get-pending-code.php', [
    'endpoint' => 'get-pending-code.php',
    'ip' => $ip,
    'user_agent' => substr($userAgent, 0, 100)
]);

// Buscar dados do JSONBin (já definido acima)

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
        echo json_encode(['status' => 'erro', 'mensagem' => 'Erro ao conectar']);
        exit;
    }
    
    $data = json_decode($response, true);
    $record = $data['record'] ?? [];
    
    if (!isset($record['_pending_logins']) || !is_array($record['_pending_logins'])) {
        echo json_encode(['status' => 'nao_encontrado', 'mensagem' => 'Nenhum codigo pendente']);
        exit;
    }
    
    // Procurar código pendente - buscar o mais recente válido
    // Pode ser do mesmo IP ou qualquer código válido recente (últimos 15 minutos)
    $foundCode = null;
    $foundUsername = null;
    $foundKey = null;
    $currentTime = time();
    $mostRecent = null;
    $mostRecentTime = 0;
    
    // Buscar o código pendente mais recente ainda válido
    foreach ($record['_pending_logins'] as $key => $pending) {
        // Verificar se expirou
        if (isset($pending['expiresAt']) && $currentTime > $pending['expiresAt']) {
            continue; // Pular códigos expirados
        }
        
        // Verificar timestamp para pegar o mais recente
        $timestamp = $pending['timestamp'] ?? 0;
        if ($timestamp > $mostRecentTime) {
            $mostRecent = $key;
            $mostRecentTime = $timestamp;
        }
    }
    
    // Se encontrou código válido
    if ($mostRecent !== null && isset($record['_pending_logins'][$mostRecent])) {
        $pending = $record['_pending_logins'][$mostRecent];
        $foundCode = $pending['code'] ?? null;
        $foundUsername = $pending['username'] ?? '';
        $foundKey = $mostRecent;
        
        // Remover código usado (one-time use)
        unset($record['_pending_logins'][$foundKey]);
        
        // Salvar de volta no JSONBin
        curl_setopt($ch = curl_init(), CURLOPT_URL, "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b");
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
    }
    
    if ($foundCode) {
        addLog('success', 'Codigo pendente encontrado e retornado', [
            'endpoint' => 'get-pending-code.php',
            'code' => $foundCode,
            'username' => $foundUsername ?? ''
        ]);
        
        echo json_encode([
            'status' => 'ok',
            'code' => $foundCode,
            'username' => $foundUsername ?? '',
            'mensagem' => 'Codigo encontrado - dados do usuario do painel'
        ]);
    } else {
        addLog('warning', 'Nenhum codigo pendente encontrado', [
            'endpoint' => 'get-pending-code.php',
            'ip' => $ip,
            'total_pending' => count($record['_pending_logins'] ?? []),
            'has_pending' => isset($record['_pending_logins'])
        ]);
        
        echo json_encode([
            'status' => 'nao_encontrado', 
            'mensagem' => 'Nenhum codigo pendente encontrado',
            'debug' => [
                'ip' => $ip,
                'total_pending' => count($record['_pending_logins'] ?? []),
                'has_pending' => isset($record['_pending_logins'])
            ]
        ]);
    }
    
} catch (Exception $e) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'Erro: ' . $e->getMessage()]);
}
?>

