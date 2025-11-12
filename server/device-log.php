<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Configurações JSONBin - Login (mesmo padrão dos outros PHP)
$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

function jsonbin_get_device_log() {
    global $jsonbin_url, $apiKey;
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: " . $apiKey
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200 || !$response) {
        throw new Exception("JSONBin GET falhou (HTTP=$httpCode)");
    }
    
    $json = json_decode($response, true);
    if (!is_array($json)) {
        throw new Exception("JSONBin resposta inválida");
    }
    
    // Se não tiver 'record' ou estiver vazio, retornar objeto vazio
    if (!isset($json['record'])) {
        return [];
    }
    
    // Garantir que record seja um array
    $record = $json['record'];
    if (!is_array($record)) {
        return [];
    }
    
    return $record;
}

function jsonbin_put_device_log($record) {
    global $jsonbin_update, $apiKey;
    
    $body = json_encode($record, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_update);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Content-Type: application/json",
        "X-Master-Key: " . $apiKey
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode < 200 || $httpCode >= 300) {
        throw new Exception("JSONBin PUT falhou (HTTP=$httpCode resp=$response)");
    }
}

$rawInput = file_get_contents('php://input');
if (!$rawInput) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'payload vazio']);
    exit;
}

$data = json_decode($rawInput, true);
if (!is_array($data)) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'JSON inválido']);
    exit;
}

try {
    $record = jsonbin_get_device_log();
    if (!isset($record['_device_logs']) || !is_array($record['_device_logs'])) {
        $record['_device_logs'] = [];
    }

    $data['loggedAt'] = gmdate('c');
    $record['_device_logs'][] = $data;

    if (count($record['_device_logs']) > 100) {
        $record['_device_logs'] = array_slice($record['_device_logs'], -100);
    }

    jsonbin_put_device_log($record);

    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'falha ao salvar log: ' . $e->getMessage()]);
}
