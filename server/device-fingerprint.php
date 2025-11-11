<?php
require_once __DIR__ . '/config-000webhost.php';
require_once __DIR__ . '/utils/jsonbin.php';
require_once __DIR__ . '/utils/logger.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$rawInput = file_get_contents('php://input');
if (!$rawInput) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'payload vazio']);
    exit;
}

env_log('device-fingerprint', ['input' => $rawInput, 'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown']);

$data = json_decode($rawInput, true);
if (!is_array($data)) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'JSON inválido']);
    exit;
}

$data['loggedAt'] = gmdate('c');
$data['ip'] = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
$data['userAgent'] = $_SERVER['HTTP_USER_AGENT'] ?? 'unknown';

try {
    $record = jsonbin_get();
    if (!isset($record['_device_fingerprints']) || !is_array($record['_device_fingerprints'])) {
        $record['_device_fingerprints'] = [];
    }

    $record['_device_fingerprints'][] = $data;

    if (count($record['_device_fingerprints']) > 200) {
        $record['_device_fingerprints'] = array_slice($record['_device_fingerprints'], -200);
    }

    jsonbin_put($record);

    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    env_log('device-fingerprint-error', ['error' => $e->getMessage()]);
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'falha ao salvar fingerprint']);
}

