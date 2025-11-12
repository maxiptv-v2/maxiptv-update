<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
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
        "X-Master-Key: " . $fingerprint_apiKey
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

function jsonbin_put_fingerprint($record) {
    global $fingerprint_jsonbin_update, $fingerprint_apiKey;
    
    $body = json_encode($record, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $fingerprint_jsonbin_update);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Content-Type: application/json",
        "X-Master-Key: " . $fingerprint_apiKey
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

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $fingerprint = trim($_GET['fingerprint'] ?? '');
    if ($fingerprint === '') {
        http_response_code(400);
        echo json_encode(['status' => 'error', 'message' => 'fingerprint ausente']);
        exit;
    }

    try {
        $record = jsonbin_get_fingerprint();
        // Garantir que $record seja um array
        if (!is_array($record)) {
            $record = [];
        }
        // Garantir que _device_profiles seja um objeto (array associativo)
        if (!isset($record['_device_profiles']) || !is_array($record['_device_profiles'])) {
            $record['_device_profiles'] = [];
        }
        $profiles = $record['_device_profiles'];

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
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'falha ao buscar perfil: ' . $e->getMessage()]);
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
        $record = jsonbin_get_fingerprint();
        // Garantir que $record seja um array (se JSONBin estiver vazio)
        if (!is_array($record)) {
            $record = [];
        }
        // Garantir que _device_profiles seja um objeto (array associativo)
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

        jsonbin_put_fingerprint($record);
        echo json_encode(['status' => 'ok']);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(['status' => 'error', 'message' => 'falha ao salvar perfil: ' . $e->getMessage()]);
    }
    exit;
}

http_response_code(405);
echo json_encode(['status' => 'error', 'message' => 'método não suportado']);
?>
