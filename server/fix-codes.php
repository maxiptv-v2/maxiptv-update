<?php
/**
 * Script para adicionar campos ausentes aos códigos existentes no JSONBin
 */

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

echo "<h1>🔧 Corrigindo Códigos no JSONBin</h1>\n";
echo "<hr>\n";

// 1. Buscar dados atuais
echo "<h2>1. Buscando códigos existentes...</h2>\n";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $master_key"]);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200) {
    die("❌ Erro HTTP $httpCode ao buscar dados");
}

$data = json_decode($response, true);

if (!isset($data['record']['simpleCodes'])) {
    die("❌ Estrutura simpleCodes não encontrada");
}

$codes = $data['record']['simpleCodes'];
echo "✅ " . count($codes) . " códigos encontrados\n";

// 2. Adicionar campos ausentes
echo "<h2>2. Adicionando campos ausentes...</h2>\n";

$updated = 0;
foreach ($codes as $code => &$clientData) {
    $needsUpdate = false;
    
    if (!isset($clientData['ativo'])) {
        $clientData['ativo'] = true;
        $needsUpdate = true;
    }
    
    if (!isset($clientData['usado'])) {
        $clientData['usado'] = false;
        $needsUpdate = true;
    }
    
    if (!isset($clientData['usado_em'])) {
        $clientData['usado_em'] = null;
        $needsUpdate = true;
    }
    
    if (!isset($clientData['usado_device'])) {
        $clientData['usado_device'] = null;
        $needsUpdate = true;
    }
    
    if ($needsUpdate) {
        echo "✅ Código $code: Adicionando campos ausentes\n";
        $updated++;
    }
}

// 3. Salvar de volta no JSONBin
if ($updated > 0) {
    echo "<h2>3. Salvando correções no JSONBin...</h2>\n";
    
    $updateUrl = $jsonbin_url;
    $updateHeaders = [
        "X-Master-Key: $master_key",
        'Content-Type: application/json'
    ];
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $updateUrl);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $updateHeaders);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data['record']));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200) {
        echo "✅ $updated códigos corrigidos com sucesso!\n";
        echo "<p>Agora você pode <a href='test-jsonbin-serialization.php'>testar novamente</a></p>\n";
    } else {
        echo "❌ Erro HTTP $httpCode ao salvar: " . substr($result, 0, 200) . "\n";
    }
} else {
    echo "✅ Todos os códigos já estão corretos!\n";
}

echo "<hr>\n";
echo "<h2>📊 Resumo</h2>\n";
echo "<p>✅ Correção concluída!</p>\n";
?>

