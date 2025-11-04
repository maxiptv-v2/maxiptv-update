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

// URL do APK no GitHub com cache-busting para sempre pegar a versão mais recente
// Adicionar timestamp para forçar atualização (GitHub pode ter cache)
$apkUrl = "https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/maxiptv-release.apk?v=" . time();

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

// Detectar se é um Downloader Android (User-Agent típico)
$isDownloader = isset($_SERVER['HTTP_USER_AGENT']) && 
                (stripos($_SERVER['HTTP_USER_AGENT'], 'downloader') !== false ||
                 stripos($_SERVER['HTTP_USER_AGENT'], 'android') !== false);

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

// Verificar se usuário expirou (formato DD/MM/YYYY) - VALIDAÇÃO CRÍTICA
// Esta validação deve ser feita ANTES de salvar código pendente para login automático
// Se o usuário estiver expirado, não deve salvar código pendente nem permitir download
$expiryDate = $user['expiryDate'] ?? '';
if (!empty($expiryDate) && isExpired($expiryDate)) {
    http_response_code(403);
    echo "<h3>Codigo invalido ou expirado.</h3>";
    echo "<p>Assinatura expirada. Data de validade: $expiryDate</p>";
    echo "<p>Entre em contato para renovar sua assinatura.</p>";
    exit;
}

// 4️⃣ IDENTIFICAR USUÁRIO usando dados do painel (JSONBin)
// O código JÁ está associado ao username no JSONBin quando gerado no painel admin
// Exemplo JSONBin: { "6789": { "username": "casa1", "password": "1234", "apiUrl": "...", "expiryDate": "..." } }

// Obter dados do usuário do código (já validado acima - vem do painel)
$username = $user['username'] ?? '';

// Salvar código temporariamente para login automático após instalação
// Quando o app abrir, ele vai buscar esse código e fazer login automático
try {
    // Buscar JSONBin para salvar código pendente
    $ch2 = curl_init();
    curl_setopt($ch2, CURLOPT_URL, "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest");
    curl_setopt($ch2, CURLOPT_HTTPHEADER, [
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch2, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch2, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch2, CURLOPT_TIMEOUT, 5);
    
    $response2 = curl_exec($ch2);
    $httpCode2 = curl_getinfo($ch2, CURLINFO_HTTP_CODE);
    
    if ($httpCode2 === 200 && $response2) {
        $data2 = json_decode($response2, true);
        $record2 = $data2['record'] ?? [];
        
        $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
        $userAgent = $_SERVER['HTTP_USER_AGENT'] ?? 'unknown';
        $timestamp = time();
        
        // Salvar código pendente (válido por 15 minutos)
        if (!isset($record2['_pending_logins'])) {
            $record2['_pending_logins'] = [];
        }
        
        // Limpar códigos antigos (mais de 15 minutos)
        foreach ($record2['_pending_logins'] as $key => $pending) {
            if (isset($pending['expiresAt']) && time() > $pending['expiresAt']) {
                unset($record2['_pending_logins'][$key]);
            }
        }
        
        // Salvar código + dados do usuário do painel
        // IMPORTANTE: Só salva se usuário NÃO estiver expirado (já validado acima)
        // O código pendente é válido por 15 minutos para o app buscar e fazer login automático
        $record2['_pending_logins'][$ip] = [
            'code' => $code,
            'username' => $username, // Dados do usuário do painel
            'timestamp' => $timestamp,
            'expiresAt' => $timestamp + 900, // 15 minutos
            'expiryDate' => $expiryDate // Salvar também a data de expiração do usuário para referência
        ];
        
        // Salvar de volta no JSONBin
        curl_setopt($ch2, CURLOPT_URL, "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b");
        curl_setopt($ch2, CURLOPT_CUSTOMREQUEST, 'PUT');
        curl_setopt($ch2, CURLOPT_POSTFIELDS, json_encode($record2));
        curl_setopt($ch2, CURLOPT_HTTPHEADER, [
            "Content-Type: application/json",
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_exec($ch2); // Não esperar resposta
    }
    curl_close($ch2);
} catch (Exception $e) {
    // Ignorar erro - não é crítico para o download
}

// Registrar download (opcional - para logs)
try {
    // Você pode adicionar logging aqui se necessário
    error_log("Download registrado - Codigo: $code, Usuario: $username, IP: " . ($_SERVER['REMOTE_ADDR'] ?? 'unknown'));
} catch (Exception $e) {
    // Ignorar erro
}

// Redirect direto para APK
// O código está salvo temporariamente no JSONBin associado ao IP
// Quando o app abrir, ele chama auto_login.php?code=CODIGO para fazer login automático
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

