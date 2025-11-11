<?php
require_once __DIR__ . '/../config-000webhost.php';

function jsonbin_scope_config(string $scope = 'login'): array {
    global $login_jsonbin_url, $login_jsonbin_master_key,
           $fingerprint_jsonbin_url, $fingerprint_jsonbin_master_key;

    if ($scope === 'fingerprint') {
        return [$fingerprint_jsonbin_url, $fingerprint_jsonbin_master_key];
    }

    return [$login_jsonbin_url, $login_jsonbin_master_key];
}

function jsonbin_get(string $scope = 'login'): array {
    list($baseUrl, $masterKey) = jsonbin_scope_config($scope);

    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => rtrim($baseUrl, '/') . '/latest',
        CURLOPT_HTTPHEADER => [
            'X-Master-Key: ' . $masterKey,
            'Content-Type: application/json'
        ],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_TIMEOUT => 15,
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($httpCode !== 200 || $response === false) {
        throw new Exception("JSONBin GET falhou (scope=$scope HTTP=$httpCode error=$err)");
    }

    $json = json_decode($response, true);
    if (!is_array($json) || !isset($json['record'])) {
        throw new Exception("JSONBin resposta inválida (scope=$scope)");
    }

    return $json['record'];
}

function jsonbin_put(array $record, string $scope = 'login'): void {
    list($baseUrl, $masterKey) = jsonbin_scope_config($scope);

    $body = json_encode($record, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    if ($body === false) {
        throw new Exception('Falha ao codificar JSON para salvar no JSONBin');
    }

    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => rtrim($baseUrl, '/'),
        CURLOPT_CUSTOMREQUEST => 'PUT',
        CURLOPT_HTTPHEADER => [
            'X-Master-Key: ' . $masterKey,
            'Content-Type: application/json'
        ],
        CURLOPT_POSTFIELDS => $body,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_TIMEOUT => 15,
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($httpCode < 200 || $httpCode >= 300) {
        throw new Exception("JSONBin PUT falhou (scope=$scope HTTP=$httpCode error=$err resp=$response)");
    }
}
?>
