<?php
require_once __DIR__ . '/config-000webhost.php';
require_once __DIR__ . '/utils/jsonbin.php';
require_once __DIR__ . '/utils/logger.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $fingerprint = trim($_GET['fingerprint'] ?? '');
    if ($fingerprint === '') {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'fingerprint ausente']);
        exit;
    }

    try {
        $record = jsonbin_get('fingerprint');
        $profiles = $record['_device_profiles'] ?? [];

        if (isset($profiles[$fingerprint])) {
            echo json_encode([
                'status' => 'ok',
                'profile' => $profiles[$fingerprint]
            ]);
        } else {
            http_response_code(404);
            echo json_encode(['status' => 'not_found']);
        }
    } catch (Exception $e) {
        env_log('device-profile-error', ['error' => $e->getMessage()]);
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'falha ao buscar perfil']);
    }
    exit;
}

if ($method === 'POST') {
    $rawInput = file_get_contents('php://input');
    if (!$rawInput) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'payload vazio']);
        exit;
    }

    env_log('device-profile', ['input' => $rawInput, 'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown']);

    $payload = json_decode($rawInput, true);
    if (!is_array($payload)) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'JSON inválido']);
        exit;
    }

    $fingerprint = trim($payload['fingerprint'] ?? '');
    $profile = $payload['profile'] ?? null;
    if ($fingerprint === '' || !is_array($profile)) {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'dados insuficientes']);
        exit;
    }

    $device = $payload['device'] ?? [];
    $screen = $payload['screen'] ?? [];
    $scaleFactor = $payload['scaleFactor'] ?? null;
    $overscanAdjusted = $payload['overscanAdjusted'] ?? false;
    $source = $payload['source'] ?? 'app';
    $usedCode = $payload['code'] ?? null;

    try {
        $record = jsonbin_get('fingerprint');
        if (!isset($record['_device_profiles']) || !is_array($record['_device_profiles'])) {
            $record['_device_profiles'] = [];
        }

        $entry = [
            'fingerprint' => $fingerprint,
            'device' => $device,
            'screen' => $screen,
            'safeArea' => $profile,
            'scaleFactor' => $scaleFactor,
            'overscanAdjusted' => $overscanAdjusted,
            'source' => $source,
            'updatedAt' => gmdate('c')
        ];

        $record['_device_profiles'][$fingerprint] = $entry;

        // Limitar a 400 perfis recentes
        if (count($record['_device_profiles']) > 400) {
            uasort($record['_device_profiles'], function ($a, $b) {
                return strtotime($a['updatedAt'] ?? '1970-01-01') <=> strtotime($b['updatedAt'] ?? '1970-01-01');
            });
            $record['_device_profiles'] = array_slice($record['_device_profiles'], -400, null, true);
        }

        // Opcional: anexar dados ao código usado, se informado
        if (!empty($usedCode)) {
            $codeKey = (string)$usedCode;
            if (!isset($record[$codeKey]) || !is_array($record[$codeKey])) {
                $record[$codeKey] = [];
            }
            $record[$codeKey]['deviceModel'] = $device['model'] ?? ($device['manufacturer'] ?? 'unknown');
            $record[$codeKey]['manufacturer'] = $device['manufacturer'] ?? 'unknown';
            $record[$codeKey]['usedScreenWidth'] = $screen['widthPx'] ?? null;
            $record[$codeKey]['usedScreenHeight'] = $screen['heightPx'] ?? null;
            $record[$codeKey]['usedDensityDpi'] = $screen['densityDpi'] ?? null;
            $record[$codeKey]['overscanAdjusted'] = $overscanAdjusted;
            $record[$codeKey]['lastCalibrationAt'] = gmdate('c');
        }

        jsonbin_put($record, 'fingerprint');
        echo json_encode(['status' => 'ok']);
    } catch (Exception $e) {
        env_log('device-profile-error', ['error' => $e->getMessage()]);
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'falha ao salvar perfil']);
    }
    exit;
}

http_response_code(405);
echo json_encode(['status' => 'error', 'message' => 'método não suportado']);
?>
