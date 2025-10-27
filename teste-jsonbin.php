<?php
/**
 * Teste de conexão com JSONBin
 */

echo "<h1>Teste de Conexão JSONBin</h1>";

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$headers = ["X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"];

echo "<p>Testando conexão com JSONBin...</p>";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);

echo "<p>Status HTTP: $httpCode</p>";

if ($httpCode === 200 && $response) {
    echo "<p style='color: green;'>✅ Conexão OK!</p>";
    $data = json_decode($response, true);
    echo "<pre>" . print_r($data, true) . "</pre>";
} else {
    echo "<p style='color: red;'>❌ Erro na conexão!</p>";
    echo "<p>Erro cURL: $error</p>";
    echo "<p>Resposta: " . htmlspecialchars($response) . "</p>";
}

?>

