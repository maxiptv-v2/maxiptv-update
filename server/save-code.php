<?php
/**
 * MaxiPTV - save-code.php
 * Armazena código temporariamente para login automático após instalação
 * 
 * Uso: GET ?code=6789&device=DEVICE_ID
 * Retorna token que pode ser usado para buscar credenciais depois
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$code = $_GET['code'] ?? '';
$deviceId = $_GET['device'] ?? $_SERVER['REMOTE_ADDR'] ?? '';

if (!$code) {
    http_response_code(400);
    echo json_encode(['status' => 'erro', 'mensagem' => 'Codigo nao fornecido']);
    exit;
}

// Gerar token temporário (válido por 1 hora)
$token = bin2hex(random_bytes(16));
$expiresAt = time() + 3600; // 1 hora

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// Buscar dados existentes
try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url . '/latest');
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200) {
        throw new Exception("Erro ao buscar JSONBin");
    }
    
    $data = json_decode($response, true);
    $record = $data['record'] ?? [];
    
    // Criar/selecionar seção de tokens temporários
    if (!isset($record['_temp_codes'])) {
        $record['_temp_codes'] = [];
    }
    
    // Salvar token com código
    $record['_temp_codes'][$token] = [
        'code' => $code,
        'device' => $deviceId,
        'expiresAt' => $expiresAt,
        'createdAt' => time()
    ];
    
    // Atualizar JSONBin
    curl_setopt($ch = curl_init(), CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Content-Type: application/json",
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($record));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    curl_close($ch);
    
    echo json_encode([
        'status' => 'ok',
        'token' => $token,
        'mensagem' => 'Token criado com sucesso. Valido por 1 hora.'
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Erro ao salvar token: ' . $e->getMessage()
    ]);
}
?>

