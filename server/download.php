<?php
/**
 * Redirecionamento para valida.php (compatibilidade com chamadas antigas)
 * Este arquivo redireciona para valida.php mantendo os parâmetros
 */

// Redirecionar para valida.php mantendo query string
$queryString = $_SERVER['QUERY_STRING'] ?? '';
if ($queryString) {
    header("Location: /valida.php?$queryString", true, 301);
} else {
    header("Location: /valida.php", true, 301);
}
exit;
?>

