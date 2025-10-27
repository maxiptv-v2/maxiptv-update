<?php
/**
 * MaxiPTV Downloader - Servidor PHP Simples
 * Valida código e redireciona para download do GitHub
 */

// Configurações
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$headers = ["X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"];

// Obter código da URL
$code = $_GET['code'] ?? null;

if (!$code) {
    http_response_code(400);
    die("❌ Código inválido. Digite um código de 4 dígitos.");
}

// Validar formato do código (4 dígitos)
if (!preg_match('/^\d{4}$/', $code)) {
    http_response_code(400);
    die("❌ Código inválido. Digite um código de 4 dígitos.");
}

// Fazer requisição para JSONBin
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200 || !$response) {
    http_response_code(500);
    die("❌ Erro ao conectar com o servidor. Tente novamente.");
}

// Decodificar resposta
$data = json_decode($response, true);

// Debug: Mostrar estrutura recebida
error_log("JSONBin Response: " . print_r($data, true));

if (!$data || !isset($data['record']['simpleCodes'])) {
    http_response_code(500);
    error_log("Erro: Estrutura simpleCodes não encontrada. Keys disponíveis: " . implode(", ", array_keys($data['record'] ?? [])));
    die("❌ Dados do servidor inválidos. Estrutura simpleCodes não encontrada.");
}

$simpleCodes = $data['record']['simpleCodes'];

// Verificar se código existe
if (!isset($simpleCodes[$code])) {
    http_response_code(404);
    die("❌ Código não encontrado. Verifique se digitou corretamente.");
}

$clientData = $simpleCodes[$code];

// Verificar se código está ativo
if (!$clientData['ativo']) {
    http_response_code(403);
    die("❌ Código inativo. Entre em contato com o administrador.");
}

// Verificar se código já foi usado
if ($clientData['usado']) {
    http_response_code(403);
    die("❌ Código já foi utilizado. Solicite um novo código ao administrador.");
}

// Verificar se conta não expirou
$expiryDate = $clientData['expira_em'];
if (isExpired($expiryDate)) {
    http_response_code(403);
    die("❌ Sua conta expirou em $expiryDate. Entre em contato com o administrador.");
}

// Obter URL do APK
$apkUrl = $clientData['apk'];

if (empty($apkUrl)) {
    http_response_code(500);
    die("❌ URL do APK não configurada. Entre em contato com o administrador.");
}

// Marcar código como usado
$clientData['usado'] = true;
$clientData['usado_em'] = time() * 1000;
$clientData['usado_device'] = $_SERVER['HTTP_USER_AGENT'] ?? 'Unknown';

// Atualizar no JSONBin
$data['record']['simpleCodes'][$code] = $clientData;

$updateUrl = $jsonbin_url;
$updateHeaders = [
    'X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae',
    'Content-Type: application/json'
];

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $updateUrl);
curl_setopt($ch, CURLOPT_HTTPHEADER, $updateHeaders);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data['record']));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$result = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Log da atividade
$logMessage = date('Y-m-d H:i:s') . " - Código $code usado por " . $clientData['usuario'] . " - IP: " . ($_SERVER['REMOTE_ADDR'] ?? 'Unknown') . " - Device: " . $clientData['usado_device'] . "\n";
file_put_contents('downloads.log', $logMessage, FILE_APPEND | LOCK_EX);

// Redirecionar para download
header("Location: " . $apkUrl);
exit();

/**
 * Verificar se data expirou
 */
function isExpired($expiryDate) {
    try {
        // Converter data do formato DD/MM/YYYY para timestamp
        $parts = explode('/', $expiryDate);
        if (count($parts) !== 3) return true;
        
        $day = (int)$parts[0];
        $month = (int)$parts[1];
        $year = (int)$parts[2];
        
        $expiryTime = mktime(23, 59, 59, $month, $day, $year);
        $currentTime = time();
        
        return $currentTime > $expiryTime;
    } catch (Exception $e) {
        return true;
    }
}
?>

