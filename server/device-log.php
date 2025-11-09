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

env_log('device-log', ['input' => $rawInput]);

$data = json_decode($rawInput, true);
if (!is_array($data)) {
    http_response_code(400);
    echo json_encode(['status' => 'error', 'message' => 'JSON inválido']);
    exit;
}

try {
    $record = jsonbin_get();
    if (!isset($record['_device_logs']) || !is_array($record['_device_logs'])) {
        $record['_device_logs'] = [];
    }

    $data['loggedAt'] = gmdate('c');
    $record['_device_logs'][] = $data;

    if (count($record['_device_logs']) > 100) {
        $record['_device_logs'] = array_slice($record['_device_logs'], -100);
    }

    jsonbin_put($record);

    echo json_encode(['status' => 'ok']);
} catch (Exception $e) {
    env_log('device-log-error', ['error' => $e->getMessage()]);
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => 'falha ao salvar log']);
}
