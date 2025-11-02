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

// Tentar obter código do path (ex: /6789 → código = 6789)
$code = '';

// Se o path é apenas um número de 4 dígitos, usar como código
if (preg_match('/^(\d{4})$/', $path, $matches)) {
    $code = $matches[1];
} elseif (preg_match('/\/(\d{4})$/', $path, $matches)) {
    // Também aceitar /algo/6789
    $code = $matches[1];
} else {
    // Caso contrário, tentar query string
    $code = $_GET['code'] ?? $_GET['codigo'] ?? '';
}

// Se encontrou código no path, processar download
if ($code && preg_match('/^\d{4}$/', $code)) {
    // Definir código no $_GET para download.php
    $_GET['code'] = $code;
    
    // Processar download diretamente (sem require para evitar duplicação de headers)
    // Mas vamos usar download.php mesmo assim
    $_SERVER['SCRIPT_NAME'] = '/download.php';
    chdir(__DIR__);
    require __DIR__ . '/download.php';
    exit;
}

// Se o path é vazio ou raiz, usar index.php
if (empty($path) || $path === 'index.php') {
    $_SERVER['SCRIPT_NAME'] = '/index.php';
    chdir(__DIR__);
    require __DIR__ . '/index.php';
    exit;
}

// Para outros arquivos PHP, deixar o servidor PHP buscar normalmente
// Retornar false faz o servidor continuar a busca pelo arquivo
return false;
?>

