<?php
/**
 * MaxiPTV - Validação de Código e Retorno de Credenciais
 * Busca código no JSONBin e retorna JSON com dados para login automático
 */

// Habilitar exibição de erros (apenas para debug)
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// URL fixa do APK no GitHub
$link_apk = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk";

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// Obter código da URL
$code = $_GET['code'] ?? $_GET['codigo'] ?? '';

if (!$code) {
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Codigo nao fornecido"
    ]);
    exit;
}

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

    // Buscar código no objeto direto (não array, não simpleCodes)
    $codigos = $data['record'];
} catch (Exception $e) {
    error_log("Exception em valida.php: " . $e->getMessage());
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Erro interno: " . $e->getMessage()
    ]);
    exit;
}

// Verificar se código existe
if (!isset($codigos[$code])) {
    // Log para debug (verificar chaves disponíveis)
    $availableKeys = array_keys($codigos);
    $codeKeys = array_values(array_filter($availableKeys, function($key) {
        return preg_match('/^\d{4}$/', $key);
    }));
    
    echo json_encode([
        "status" => "erro",
        "mensagem" => "Codigo invalido. Codigos disponiveis: " . implode(", ", array_slice($codeKeys, 0, 10))
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

// Verificar se expirou (formato DD/MM/YYYY)
if (isset($user['expiryDate'])) {
    $dataExpiracao = $user['expiryDate'];
    
    if (isExpired($dataExpiracao)) {
        echo json_encode([
            "status" => "erro",
            "mensagem" => "Codigo expirado ou inativo"
        ]);
        exit;
    }
}

// Usar apkUrl do código ou link fixo como fallback
$apkUrl = $user['apkUrl'] ?? $link_apk;

// Retornar dados para login automático
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

