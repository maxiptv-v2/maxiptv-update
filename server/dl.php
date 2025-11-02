<?php
/**
 * MaxiPTV - dl.php
 * Endpoint simples para download automático (tipo Nidev)
 * 
 * Uso: https://maxiptv-update-1.onrender.com/dl/6789
 * 
 * - Sem Captcha
 * - 100% automático
 * - Valida código e redireciona para APK
 */

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// URL fixa do APK no GitHub
$apkUrl = "https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk";

// Obter código da URL
// Aceita: /dl/6789 ou /dl.php?code=6789
$code = '';

// Tentar obter do path (/dl/6789)
$requestUri = $_SERVER['REQUEST_URI'] ?? '';
$path = parse_url($requestUri, PHP_URL_PATH);

// Extrair código do path
if (preg_match('#/dl/([A-Za-z0-9]{3,10})(?:/)?$#', $path, $matches)) {
    $code = $matches[1];
} elseif (preg_match('#^/([A-Za-z0-9]{3,10})(?:/)?$#', $path, $matches)) {
    // Aceitar também /6789 direto
    $code = $matches[1];
} else {
    // Tentar query string
    $code = $_GET['code'] ?? $_GET['codigo'] ?? '';
}

// Validar código
if (!$code || !preg_match('/^[A-Za-z0-9]{3,10}$/', $code)) {
    http_response_code(400);
    die("Codigo invalido. Digite um codigo valido (3-10 caracteres alfanumericos).");
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
    curl_close($ch);

    if ($httpCode !== 200 || !$response) {
        http_response_code(500);
        die("Erro ao conectar com o servidor.");
    }

    $data = json_decode($response, true);
    
    if (!isset($data['record'])) {
        http_response_code(500);
        die("Erro ao ler dados do servidor.");
    }

    $codigos = $data['record'];
} catch (Exception $e) {
    http_response_code(500);
    die("Erro interno: " . $e->getMessage());
}

// Verificar se código existe
if (!isset($codigos[$code])) {
    http_response_code(404);
    die("Codigo invalido ou expirado.");
}

if (!is_array($codigos[$code])) {
    http_response_code(400);
    die("Codigo invalido - dados corrompidos.");
}

$user = $codigos[$code];

// Verificar se código expirou (6 horas após criação)
if (isset($user['createdAt'])) {
    $createdAt = (int)$user['createdAt'];
    $sixHoursInMs = 6 * 60 * 60 * 1000; // 6 horas em milissegundos
    $validUntil = $createdAt + $sixHoursInMs;
    $currentTime = round(microtime(true) * 1000); // timestamp em milissegundos
    
    if ($currentTime > $validUntil) {
        http_response_code(404);
        die("Codigo invalido ou expirado.");
    }
}

// Verificar se usuário expirou (formato DD/MM/YYYY)
if (isset($user['expiryDate'])) {
    $dataExpiracao = $user['expiryDate'];
    
    if (isExpired($dataExpiracao)) {
        http_response_code(404);
        die("Codigo invalido ou expirado.");
    }
}

// Código válido! Redirecionar para APK
header("HTTP/1.1 302 Found");
header("Location: $apkUrl");
header("Content-Type: text/plain");
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

