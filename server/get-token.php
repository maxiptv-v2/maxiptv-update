<?php
/**
 * MaxiPTV - get-token.php
 * Retorna o código associado a um token
 * O app chama isso quando instala para identificar qual usuário é
 * 
 * Uso: get-token.php?token=ABC123...
 * Retorna: { "status": "ok", "code": "6789", "username": "casa1" }
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$token = $_GET['token'] ?? '';

if (empty($token)) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'Token nao fornecido']);
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
    
    if (!isset($record['_tokens']) || !is_array($record['_tokens'])) {
        echo json_encode(['status' => 'erro', 'mensagem' => 'Token nao encontrado']);
        exit;
    }
    
    // Procurar token
    if (!isset($record['_tokens'][$token])) {
        echo json_encode(['status' => 'erro', 'mensagem' => 'Token invalido ou expirado']);
        exit;
    }
    
    $tokenData = $record['_tokens'][$token];
    
    // Verificar se expirou
    if (isset($tokenData['expiresAt']) && time() > $tokenData['expiresAt']) {
        // Remover token expirado
        unset($record['_tokens'][$token]);
        
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
        
        echo json_encode(['status' => 'erro', 'mensagem' => 'Token expirado']);
        exit;
    }
    
    // Token válido - retornar código e username
    $code = $tokenData['code'] ?? '';
    $username = $tokenData['username'] ?? '';
    
    // Remover token usado (one-time use)
    unset($record['_tokens'][$token]);
    
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
    
    echo json_encode([
        'status' => 'ok',
        'code' => $code,
        'username' => $username,
        'mensagem' => 'Token validado - dados do usuario do painel'
    ]);
    
} catch (Exception $e) {
    echo json_encode(['status' => 'erro', 'mensagem' => 'Erro: ' . $e->getMessage()]);
}
?>

