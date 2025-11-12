<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Configurações JSONBin - Fingerprint (mesmo padrão dos outros PHP)
$fingerprint_jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$fingerprint_jsonbin_update = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$fingerprint_apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

function jsonbin_get_fingerprint() {
    global $fingerprint_jsonbin_url, $fingerprint_apiKey;
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $fingerprint_jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: $fingerprint_apiKey"
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
    if (!is_array($json) || !isset($json['record'])) {
        throw new Exception("JSONBin resposta inválida");
    }
    
    return $json['record'];
}

function jsonbin_put_fingerprint($record) {
    global $fingerprint_jsonbin_update, $fingerprint_apiKey;
    
    $body = json_encode($record, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $fingerprint_jsonbin_update);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: $fingerprint_apiKey",
        "Content-Type: application/json"
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
    $record = jsonbin_get_fingerprint();
    if (!isset($record['_device_fingerprints']) || !is_array($record['_device_fingerprints'])) {
        $record['_device_fingerprints'] = [];
    }

    $data['loggedAt'] = gmdate('c');
    $record['_device_fingerprints'][] = $data;

    if (count($record['_device_fingerprints']) > 200) {
        $record['_device_fingerprints'] = array_slice($record['_device_fingerprints'], -200);
    }

    jsonbin_put_fingerprint($record);

    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'falha ao salvar fingerprint: ' . $e->getMessage()]);
}
?>

