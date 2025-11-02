<?php
/**
 * MaxiPTV - get-pending-code.php
 * Retorna o código pendente baseado no IP/User-Agent
 * O app chama isso quando abre pela primeira vez (sem login)
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$ip = $_SERVER['REMOTE_ADDR'] ?? '';
$userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';

if (empty($ip)) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'IP nao identificado']);
    exit;
}

// Buscar dados do JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

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
    
    // Procurar código pendente para este IP
    // No dl.php, salvamos com a chave sendo o próprio IP: $record2['_pending_logins'][$ip]
    $foundCode = null;
    $currentTime = time();
    
    // Verificar se existe código pendente para este IP
    if (isset($record['_pending_logins'][$ip])) {
        $pending = $record['_pending_logins'][$ip];
        
        // Verificar se expirou
        if (isset($pending['expiresAt']) && $currentTime > $pending['expiresAt']) {
            // Remover código expirado
            unset($record['_pending_logins'][$ip]);
            
            // Salvar de volta
            curl_setopt($ch = curl_init(), CURLOPT_URL, "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b");
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
            curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($record));
            curl_setopt($ch, CURLOPT_HTTPHEADER, [
                "Content-Type: application/json",
                "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
            ]);
            curl_exec($ch);
            curl_close($ch);
        } else {
            // Código válido - retornar
            $foundCode = $pending['code'] ?? null;
            $username = $pending['username'] ?? '';
            
            // Remover código usado (one-time use)
            unset($record['_pending_logins'][$ip]);
            
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
    }
    
    if ($foundCode) {
        echo json_encode([
            'status' => 'ok',
            'code' => $foundCode,
            'username' => $username ?? '',
            'mensagem' => 'Codigo encontrado - dados do usuario do painel'
        ]);
    } else {
        echo json_encode(['status' => 'nao_encontrado', 'mensagem' => 'Nenhum codigo pendente para este dispositivo']);
    }
    
} catch (Exception $e) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'Erro: ' . $e->getMessage()]);
}
?>

