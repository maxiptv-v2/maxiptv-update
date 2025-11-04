<?php
/**
 * MaxiPTV - router.php
 * Router para aceitar códigos diretamente na URL: /6789
 * Compatível com serviços de encurtamento de URL como AFTVnews
 * 
 * Funciona com:
 * - https://maxiptv-update-1.onrender.com/6789
 * - https://maxiptv-update-1.onrender.com/download.php?code=6789
 * 
 * O servidor PHP built-in chama este arquivo quando não encontra o arquivo solicitado.
 * Exemplo: acessar /6789 → PHP não encontra arquivo → chama router.php
 */

// Obter caminho da requisição
$requestUri = $_SERVER['REQUEST_URI'] ?? '';
$path = parse_url($requestUri, PHP_URL_PATH);
$path = trim($path, '/');

// Prioridade 1: Endpoints específicos (NÃO interceptar auto_login.php, get-pending-code.php, debug-login.php)
if (strpos($path, 'auto_login.php') !== false || 
    strpos($path, 'get-pending-code.php') !== false || 
    strpos($path, 'debug-login.php') !== false) {
    // Deixar PHP built-in server processar arquivos PHP diretamente
    return false;
}

// Prioridade 2: Aceitar /dl/CODIGO (endpoint dedicado tipo Nidev)
if (preg_match('#^dl/([A-Za-z0-9]{3,10})(?:/)?$#', $path, $matches)) {
    $code = $matches[1];
    // Redirecionar para dl.php
    $_GET['code'] = $code;
    $_SERVER['REQUEST_URI'] = '/dl.php?code=' . $code;
    $_SERVER['SCRIPT_NAME'] = '/dl.php';
    chdir(__DIR__);
    require __DIR__ . '/dl.php';
    exit;
}

// Tentar obter código do path (ex: /6789, /A1234 → código = 6789, A1234)
$code = '';

// Prioridade 2: Aceitar códigos alfanuméricos direto (ex: /6789, /A1234)
if (preg_match('/^([A-Za-z0-9]{3,10})$/', $path, $matches)) {
    $code = $matches[1];
} elseif (preg_match('/\/([A-Za-z0-9]{3,10})$/', $path, $matches)) {
    // Também aceitar /algo/A1234
    $code = $matches[1];
} else {
    // Caso contrário, tentar query string
    $code = $_GET['code'] ?? $_GET['codigo'] ?? '';
}

// Se encontrou código no path, processar download
if ($code && preg_match('/^[A-Za-z0-9]{3,10}$/', $code)) {
    // Definir código no $_GET para download.php
    $_GET['code'] = $code;
    
    // Processar download diretamente (sem require para evitar duplicação de headers)
    // Mas vamos usar download.php mesmo assim
    $_SERVER['SCRIPT_NAME'] = '/download.php';
    chdir(__DIR__);
    require __DIR__ . '/download.php';
    exit;
}

// Se o path é vazio ou raiz, servir index.html (página para digitar código)
if (empty($path)) {
    // Verificar se existe index.html e servir
    if (file_exists(__DIR__ . '/index.html')) {
        readfile(__DIR__ . '/index.html');
        exit;
    }
    // Caso contrário, usar index.php
    $_SERVER['SCRIPT_NAME'] = '/index.php';
    chdir(__DIR__);
    require __DIR__ . '/index.php';
    exit;
}

// Se for index.php, processar normalmente
if ($path === 'index.php') {
    $_SERVER['SCRIPT_NAME'] = '/index.php';
    chdir(__DIR__);
    require __DIR__ . '/index.php';
    exit;
}

// Para outros arquivos PHP, deixar o servidor PHP buscar normalmente
// Retornar false faz o servidor continuar a busca pelo arquivo
return false;
?>

