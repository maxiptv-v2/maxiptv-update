<?php
/**
 * MaxiPTV - register-aftvnews.php
 * Automatiza registro de códigos no AFTVnews
 * 
 * Uso: POST com code e url_long
 * Exemplo: 
 *   curl -X POST https://maxiptv-update-1.onrender.com/register-aftvnews.php \
 *        -d "code=6789&url_long=https://maxiptv-update-1.onrender.com/6789"
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$code = $_POST['code'] ?? $_GET['code'] ?? '';
$urlLong = $_POST['url_long'] ?? $_GET['url_long'] ?? '';

if (!$code || !$urlLong) {
    http_response_code(400);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Codigo e url_long sao obrigatorios'
    ]);
    exit;
}

// URL do AFTVnews para encurtar
$aftvnewsUrl = 'https://go.aftvnews.com/';

// Tentar registrar no AFTVnews
// Nota: AFTVnews pode requerer reCAPTCHA, então isso pode não funcionar 100%
try {
    // Criar sessão cURL para manter cookies (necessário para reCAPTCHA)
    $ch = curl_init();
    
    // Primeiro, carregar a página inicial para obter tokens de sessão
    curl_setopt($ch, CURLOPT_URL, $aftvnewsUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_COOKIEJAR, sys_get_temp_dir() . '/aftvnews_cookies.txt');
    curl_setopt($ch, CURLOPT_COOKIEFILE, sys_get_temp_dir() . '/aftvnews_cookies.txt');
    curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    
    $html = curl_exec($ch);
    
    // Tentar encontrar o endpoint de submissão do formulário
    // AFTVnews usa um formulário, precisamos encontrar o action
    if (preg_match('/action="([^"]+)"/', $html, $matches)) {
        $formAction = $matches[1];
        // Se for relativo, tornar absoluto
        if (strpos($formAction, 'http') !== 0) {
            $formAction = 'https://go.aftvnews.com' . $formAction;
        }
    } else {
        // Tentar endpoint comum de encurtadores
        $formAction = 'https://go.aftvnews.com/shorten';
    }
    
    // Preparar dados do formulário
    $postData = http_build_query([
        'url' => $urlLong,
        'code' => $code, // Alguns encurtadores permitem código customizado
    ]);
    
    // Fazer requisição POST para encurtar
    curl_setopt($ch, CURLOPT_URL, $formAction);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/x-www-form-urlencoded',
        'Referer: ' . $aftvnewsUrl
    ]);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);
    
    // Limpar cookies
    @unlink(sys_get_temp_dir() . '/aftvnews_cookies.txt');
    
    if ($curlError) {
        throw new Exception("Erro cURL: $curlError");
    }
    
    // Analisar resposta
    // AFTVnews pode retornar JSON ou HTML com o código
    $result = [
        'status' => 'aviso',
        'mensagem' => 'Registro pode requerer validacao manual no AFTVnews',
        'codigo' => $code,
        'url_long' => $urlLong,
        'url_curta_esperada' => "https://go.aftvnews.com/$code",
        'http_code' => $httpCode,
        'nota' => 'AFTVnews pode requerer reCAPTCHA. Registro manual pode ser necessario.'
    ];
    
    // Tentar extrair código curto da resposta
    if (preg_match('/go\.aftvnews\.com\/([A-Za-z0-9]+)/', $response, $matches)) {
        $result['codigo_gerado'] = $matches[1];
        $result['url_curta'] = "https://go.aftvnews.com/{$matches[1]}";
        $result['status'] = 'sucesso';
        $result['mensagem'] = 'Codigo registrado com sucesso no AFTVnews';
    }
    
    echo json_encode($result, JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'status' => 'erro',
        'mensagem' => 'Erro ao registrar no AFTVnews: ' . $e->getMessage(),
        'codigo' => $code,
        'url_long' => $urlLong,
        'nota' => 'Registro manual pode ser necessario em https://go.aftvnews.com'
    ]);
}
?>

