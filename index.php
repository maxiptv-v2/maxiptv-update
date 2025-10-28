<?php
/**
 * MaxiPTV Downloader - Render.com
 * Roteador principal que redireciona para download.php
 */

header('Content-Type: text/plain');
header('Access-Control-Allow-Origin: *');

$code = $_GET['code'] ?? null;

if (!$code || !preg_match('/^\d{4}$/', $code)) {
    http_response_code(400);
    die("❌ Código inválido! Digite um código de 4 dígitos.");
}

// Redirecionar para valida.php
header("Location: valida.php?code=$code");
exit();

?>

