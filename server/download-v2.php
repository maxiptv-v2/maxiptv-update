<?php
/**
 * MaxiPTV Downloader - Versão SEM JavaScript
 * Valida código e retorna JSON ou redireciona para download
 */

// Configurações
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$jsonbin_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

// Obter código da URL
$code = $_GET['code'] ?? null;

if (!$code) {
    header('Content-Type: application/json');
    http_response_code(400);
    die(json_encode(['erro' => 'Código inválido. Digite um código de 4 dígitos.']));
}

// Validar formato do código (4 dígitos)
if (!preg_match('/^\d{4}$/', $code)) {
    header('Content-Type: application/json');
    http_response_code(400);
    die(json_encode(['erro' => 'Código inválido. Digite um código de 4 dígitos.']));
}

// Fazer requisição para JSONBin
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
    header('Content-Type: application/json');
    http_response_code(500);
    die(json_encode(['erro' => 'Erro ao conectar com o servidor. Tente novamente.']));
}

$data = json_decode($response, true);

if (!$data || !isset($data['record']['simpleCodes'])) {
    header('Content-Type: application/json');
    http_response_code(500);
    die(json_encode(['erro' => 'Dados do servidor inválidos.']));
}

$simpleCodes = $data['record']['simpleCodes'];

// Verificar se código existe
if (!isset($simpleCodes[$code])) {
    header('Content-Type: application/json');
    http_response_code(404);
    die(json_encode(['erro' => 'Código não encontrado. Verifique se digitou corretamente.']));
}

$clientData = $simpleCodes[$code];

// Verificar se código está ativo
if (!isset($clientData['ativo']) || !$clientData['ativo']) {
    header('Content-Type: application/json');
    http_response_code(403);
    die(json_encode(['erro' => 'Código inativo. Entre em contato com o administrador.']));
}

// Verificar se código já foi usado
if (isset($clientData['usado']) && $clientData['usado']) {
    header('Content-Type: application/json');
    http_response_code(403);
    die(json_encode(['erro' => 'Código já foi utilizado. Solicite um novo código ao administrador.']));
}

// Retornar dados em JSON (para o Android Downloader processar)
header('Content-Type: application/json');
echo json_encode([
    'status' => 'ok',
    'apk_url' => $clientData['apk'] ?? 'https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk',
    'usuario' => $clientData['usuario'],
    'senha' => $clientData['senha'],
    'api' => $clientData['api'],
    'expira' => $clientData['expira_em']
]);

?>

