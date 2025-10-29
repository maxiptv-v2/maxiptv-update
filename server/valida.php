<?php
/**
 * MaxiPTV - valida.php
 * Apenas testa se o servidor está online
 * Retorna {"status": "ok"} para confirmar que está funcionando
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

echo json_encode([
    "status" => "ok",
    "mensagem" => "Servidor online"
]);
exit;
?>
