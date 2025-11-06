<?php
/**
 * MaxiPTV - get-pending-code.php
 * Retorna o código pendente baseado no IP/User-Agent
 * O app chama isso quando abre pela primeira vez (sem login)
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Configurações JSONBin (definir ANTES da função para usar global)
$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

$ip = $_SERVER['REMOTE_ADDR'] ?? '';
$userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';

// Função para adicionar log (preserva TODOS os dados existentes)
function addLog($type, $message, $data = []) {
    global $jsonbin_url, $jsonbin_update, $apiKey;
    
    try {
        // Buscar record COMPLETO para preservar tudo
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10); // Aumentado para 10s
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode !== 200 || !$response) {
            // Se falhar, tentar novamente uma vez
            sleep(1);
            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
            curl_setopt($ch, CURLOPT_HTTPHEADER, [
                "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
            ]);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            curl_setopt($ch, CURLOPT_TIMEOUT, 10);
            $response = curl_exec($ch);
            curl_close($ch);
        }
        
        $data_record = json_decode($response, true);
        $record = $data_record['record'] ?? [];
        
        // IMPORTANTE: Preservar TODOS os dados existentes
        // Não sobrescrever sessions, users, códigos, pending_logins, etc.
        // Apenas adicionar/modificar _login_logs
        
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
        
        // Manter apenas os últimos 500 logs
        if (count($record['_login_logs']) > 500) {
            $record['_login_logs'] = array_slice($record['_login_logs'], -500);
        }
        
        // Salvar record COMPLETO (preservando tudo)
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
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        curl_exec($ch);
        curl_close($ch);
    } catch (Exception $e) {
        // Silenciar erros de log para não quebrar o fluxo principal
    }
}

if (empty($ip)) {
    addLog('error', 'IP nao identificado', ['endpoint' => 'get-pending-code.php']);
    echo json_encode(['status' => 'erro', 'mensagem' => 'IP nao identificado']);
    exit;
}

$method = $_SERVER['REQUEST_METHOD'] ?? 'unknown';
$requestUri = $_SERVER['REQUEST_URI'] ?? '';

// Log de chamada inicial COM DETALHES COMPLETOS
addLog('info', '🔍 App chamou get-pending-code.php - VERIFICANDO SE APP ESTA CONECTANDO', [
    'endpoint' => 'get-pending-code.php',
    'ip' => $ip,
    'user_agent' => substr($userAgent, 0, 200),
    'method' => $method,
    'request_uri' => $requestUri,
    'get_params' => $_GET,
    'post_params' => $_POST,
    'timestamp' => time(),
    'datetime' => date('Y-m-d H:i:s')
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
    $debugInfo = []; // Para debug - definir ANTES do loop
    foreach ($record['_pending_logins'] as $key => $pending) {
        $codeDebug = [
            'key' => $key,
            'code' => $pending['code'] ?? 'N/A',
            'timestamp' => $pending['timestamp'] ?? 0,
            'expiresAt' => $pending['expiresAt'] ?? null,
            'used' => $pending['used'] ?? false,
            'usedAt' => $pending['usedAt'] ?? null,
            'reason' => ''
        ];
        
        // Verificar se expirou
        if (isset($pending['expiresAt']) && $currentTime > $pending['expiresAt']) {
            $codeDebug['reason'] = 'expired';
            $debugInfo[] = $codeDebug;
            continue; // Pular códigos expirados
        }
        
        // Ignorar códigos já usados apenas se foram usados há mais de 5 minutos
        // Isso permite que o mesmo código seja usado novamente se o login falhou
        if (isset($pending['used']) && $pending['used'] === true) {
            // Se foi usado há mais de 5 minutos, ignorar (tentativa antiga)
            if (isset($pending['usedAt']) && ($currentTime - $pending['usedAt']) > 300) {
                $codeDebug['reason'] = 'used_too_long_ago';
                $codeDebug['minutes_ago'] = round(($currentTime - $pending['usedAt']) / 60, 1);
                $debugInfo[] = $codeDebug;
                continue;
            }
            // Se foi usado há menos de 5 minutos, permitir tentar novamente (pode ter falhado)
            $codeDebug['reason'] = 'used_recently_ok';
            $codeDebug['minutes_ago'] = isset($pending['usedAt']) ? round(($currentTime - $pending['usedAt']) / 60, 1) : null;
        } else {
            $codeDebug['reason'] = 'not_used';
        }
        
        // Verificar timestamp para pegar o mais recente
        $timestamp = $pending['timestamp'] ?? 0;
        if ($timestamp > $mostRecentTime) {
            $mostRecent = $key;
            $mostRecentTime = $timestamp;
            $codeDebug['reason'] = $codeDebug['reason'] . ' (selected)';
        }
        
        $debugInfo[] = $codeDebug;
    }
    
    // Se encontrou código válido
    if ($mostRecent !== null && isset($record['_pending_logins'][$mostRecent])) {
        $pending = $record['_pending_logins'][$mostRecent];
        $foundCode = $pending['code'] ?? null;
        $foundUsername = $pending['username'] ?? '';
        // CRÍTICO: Extrair credenciais completas DENTRO deste bloco onde $pending está definido
        $foundPassword = $pending['password'] ?? '';
        $foundApiUrl = $pending['apiUrl'] ?? '';
        $foundExpiryDate = $pending['expiryDate'] ?? '';
        $foundKey = $mostRecent;
        
        // NÃO remover código imediatamente - deixar disponível por mais tempo
        // O código será removido automaticamente quando expirar (15 minutos)
        // Isso permite que o app possa tentar buscar novamente se necessário
        
        // Marcar como "usado" mas manter por mais tempo para debug
        $record['_pending_logins'][$foundKey]['used'] = true;
        $record['_pending_logins'][$foundKey]['usedAt'] = time();
        $record['_pending_logins'][$foundKey]['usedBy'] = $ip;
        
        // Salvar de volta no JSONBin (mas manter o código)
        curl_setopt($ch = curl_init(), CURLOPT_URL, "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7");
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
        // Credenciais já foram extraídas acima dentro do bloco onde $pending estava definido
        
        addLog('success', 'Codigo pendente encontrado e retornado', [
            'endpoint' => 'get-pending-code.php',
            'code' => $foundCode,
            'username' => $foundUsername ?? ''
        ]);
        
        // Log explícito antes de retornar status "ok"
        addLog('info', 'Retornando status OK para app com codigo pendente e credenciais completas', [
            'endpoint' => 'get-pending-code.php',
            'status' => 'ok',
            'code' => $foundCode,
            'username' => $foundUsername ?? '',
            'has_password' => !empty($foundPassword),
            'has_api_url' => !empty($foundApiUrl),
            'response' => [
                'status' => 'ok',
                'code' => $foundCode,
                'username' => $foundUsername ?? '',
                'password' => '***',
                'api_url' => $foundApiUrl,
                'expiryDate' => $foundExpiryDate
            ]
        ]);
        
        echo json_encode([
            'status' => 'ok',
            'code' => $foundCode,
            'username' => $foundUsername ?? '',
            'password' => $foundPassword, // Incluir senha para autologin direto
            'api_url' => $foundApiUrl, // Incluir API URL para autologin direto
            'expiryDate' => $foundExpiryDate, // Incluir data de expiração
            'mensagem' => 'Codigo encontrado - credenciais completas retornadas'
        ]);
    } else {
        // Listar todos os códigos pendentes para debug
        $allPendingCodes = [];
        foreach ($record['_pending_logins'] ?? [] as $key => $pending) {
            $allPendingCodes[] = [
                'key' => $key,
                'code' => $pending['code'] ?? 'N/A',
                'username' => $pending['username'] ?? 'N/A',
                'timestamp' => $pending['timestamp'] ?? 0,
                'expiresAt' => $pending['expiresAt'] ?? 0,
                'expired' => isset($pending['expiresAt']) && $currentTime > $pending['expiresAt'],
                'used' => $pending['used'] ?? false,
                'ip' => $pending['ip'] ?? 'unknown'
            ];
        }
        
        addLog('warning', 'Nenhum codigo pendente encontrado', [
            'endpoint' => 'get-pending-code.php',
            'ip' => $ip,
            'total_pending' => count($record['_pending_logins'] ?? []),
            'has_pending' => isset($record['_pending_logins']),
            'all_pending_codes' => $allPendingCodes,
            'current_time' => $currentTime,
            'debug_info' => $debugInfo ?? []
        ]);
        
        echo json_encode([
            'status' => 'nao_encontrado', 
            'mensagem' => 'Nenhum codigo pendente encontrado',
            'debug' => [
                'ip' => $ip,
                'total_pending' => count($record['_pending_logins'] ?? []),
                'has_pending' => isset($record['_pending_logins']),
                'current_time' => $currentTime,
                'debug_info' => $debugInfo ?? []
            ]
        ]);
    }
    
} catch (Exception $e) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'Erro: ' . $e->getMessage()]);
}
?>

