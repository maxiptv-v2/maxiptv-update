<?php
/**
 * API Simples para Downloader Android
 * Retorna JSON direto sem JavaScript
 */

// Forçar output limpo antes de qualquer coisa
ob_clean();

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('X-Content-Type-Options: nosniff');

// Configurações
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$jsonbin_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

// Obter código
$code = $_GET['code'] ?? null;

if (!$code || !preg_match('/^\d{4}$/', $code)) {
    http_response_code(400);
    die(json_encode(['erro' => 'Código inválido']));
}

// Buscar JSONBin
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $jsonbin_key"]);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200 || !$response) {
    http_response_code(500);
    die(json_encode(['erro' => 'Erro ao conectar']));
}

$data = json_decode($response, true);
$simpleCodes = $data['record']['simpleCodes'] ?? [];

// Validar código
if (!isset($simpleCodes[$code])) {
    http_response_code(404);
    die(json_encode(['erro' => 'Código não encontrado']));
}

$client = $simpleCodes[$code];

if (!$client['ativo'] || $client['usado']) {
    http_response_code(403);
    die(json_encode(['erro' => 'Código inativo ou usado']));
}

// Retornar dados
echo json_encode([
    'ok' => true,
    'apk' => $client['apk'] ?? 'https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk',
    'user' => $client['usuario'],
    'pass' => $client['senha'],
    'api' => $client['api']
]);

?>

