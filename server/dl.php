<?php
/**
 * MaxiPTV - dl.php
 * Endpoint simples para download automático (tipo Nidev)
 * 
 * Uso: https://maxiptv-update-1.onrender.com/dl/6789
 * 
 * - Sem Captcha
 * - 100% automático
 * - Valida código no JSONBin (verifica existência, expiração de 6h, validade do usuário)
 * - Redireciona para APK automaticamente
 * 
 * LOGIN AUTOMÁTICO:
 * - Quando o APK é instalado, o downloader passa o código via Intent
 * - O app busca credenciais do index.php?code=CODIGO usando os dados do JSONBin
 * - Login automático acontece baseado nos dados do usuário (username, password, apiUrl, expiryDate)
 * - Validações incluem: código existe, não expirou (6h), usuário ativo
 */

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// URL fixa do APK no GitHub
$apkUrl = "https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk";

// 1️⃣ Pega o código da URL (igual exemplo)
// Aceita: /dl/17531, /17531, ou ?code=17531
$requestUri = $_SERVER['REQUEST_URI'] ?? '';
$path = parse_url($requestUri, PHP_URL_PATH);

// Tentar extrair código do path
if (preg_match('#/dl/([A-Za-z0-9]+)#', $path, $matches)) {
    $code = $matches[1];
} elseif (preg_match('#^/([A-Za-z0-9]+)(?:/|$)#', $path, $matches)) {
    // Aceitar /17531 direto também
    $code = $matches[1];
} else {
    // Fallback: usar basename ou query string
    $code = basename($path);
    if ($code === 'dl.php' || empty($code) || strlen($code) < 3) {
        $code = $_GET['code'] ?? $_GET['codigo'] ?? '';
    }
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

// 3️⃣ Verifica se o código existe e está ativo
if (!isset($codigos[$code]) || !is_array($codigos[$code])) {
    // 5️⃣ Mostra mensagem de erro simples
    http_response_code(404);
    echo "<h3>Codigo invalido ou expirado.</h3>";
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
        http_response_code(404);
        echo "<h3>Codigo invalido ou expirado.</h3>";
        exit;
    }
}

// Verificar se usuário expirou (formato DD/MM/YYYY)
if (isset($user['expiryDate'])) {
    $dataExpiracao = $user['expiryDate'];
    
    if (isExpired($dataExpiracao)) {
        http_response_code(404);
        echo "<h3>Codigo invalido ou expirado.</h3>";
        exit;
    }
}

// 4️⃣ Redireciona pro APK (igual o AFTVNews faz)
header("Location: $apkUrl");
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

