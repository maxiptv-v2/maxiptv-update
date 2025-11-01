<?php
/**
 * MaxiPTV - index.php
 * Recebe código via GET, busca no JSONBin e retorna JSON com dados
 * Uso: ?code=2011
 * Se não tiver código, retorna página de boas-vindas ou valida.php
 */

// Se não tiver código, incluir valida.php (teste simples)
$code = $_GET['code'] ?? $_GET['codigo'] ?? '';

if (!$code) {
    // Sem código: incluir valida.php que retorna {"status":"ok"}
    require_once __DIR__ . '/valida.php';
    exit;
}

// Com código: processar normalmente
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// URL fixa do APK no GitHub
$link_apk = "https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk";

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// Validar formato do código (4 dígitos)
if (!preg_match('/^\d{4}$/', $code)) {
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Codigo invalido. Digite um codigo de 4 digitos."
    ]);
    exit;
}

// Buscar dados do JSONBin
try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($httpCode !== 200 || !$response) {
        error_log("JSONBin Error: HTTP $httpCode - $curlError");
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Erro ao conectar com o servidor. HTTP: $httpCode",
            "curl_error" => $curlError
        ]);
        exit;
    }

    // Decodificar resposta
    $data = json_decode($response, true);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        error_log("JSON Decode Error: " . json_last_error_msg());
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Erro ao decodificar resposta do servidor."
        ]);
        exit;
    }

    if (!isset($data['record'])) {
        error_log("JSONBin Response missing 'record': " . print_r($data, true));
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Erro ao ler dados do servidor - record nao encontrado."
        ]);
        exit;
    }

    // Buscar código no objeto direto
    $codigos = $data['record'];
} catch (Exception $e) {
    error_log("Exception em index.php: " . $e->getMessage());
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Erro interno: " . $e->getMessage()
    ]);
    exit;
}

// Verificar se código existe
if (!isset($codigos[$code])) {
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Codigo invalido. Codigo nao encontrado."
    ]);
    exit;
}

if (!is_array($codigos[$code])) {
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Codigo invalido - dados corrompidos."
    ]);
    exit;
}

$user = $codigos[$code];

// Verificar se código expirou (6 horas após criação)
if (isset($user['createdAt'])) {
    $createdAt = (int)$user['createdAt'];
    $sixHoursInMs = 6 * 60 * 60 * 1000; // 6 horas em milissegundos
    $validUntil = $createdAt + $sixHoursInMs;
    $currentTime = round(microtime(true) * 1000); // timestamp em milissegundos
    
    if ($currentTime > $validUntil) {
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Codigo expirado. O codigo e valido por 6 horas apos a geracao."
        ]);
        exit;
    }
}

// Verificar se usuário expirou (formato DD/MM/YYYY)
if (isset($user['expiryDate'])) {
    $dataExpiracao = $user['expiryDate'];
    
    if (isExpired($dataExpiracao)) {
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Usuario expirado ou inativo"
        ]);
        exit;
    }
}

// Usar apkUrl do código ou link fixo como fallback
$apkUrl = $user['apkUrl'] ?? $link_apk;

// Retornar dados em JSON (para login automático no app)
echo json_encode([
    "status" => "ok",
    "usuario" => $user['username'] ?? '',
    "senha" => $user['password'] ?? '',
    "api" => $user['apiUrl'] ?? '',
    "expira_em" => $user['expiryDate'] ?? '',
    "apk" => $apkUrl
]);
exit;

/**
 * Verificar se data expirou (formato DD/MM/YYYY)
 */
function isExpired($expiryDate) {
    try {
        if (empty($expiryDate)) return false;
        
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
