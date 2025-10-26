<?php
/**
 * Script para testar geração de código e ver se está sendo salvo no JSONBin
 */

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

echo "<h1>🧪 Teste de Geração de Código</h1>\n";
echo "<hr>\n";

// 1. Buscar códigos existentes
echo "<h2>1. Buscando códigos atuais...</h2>\n";

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
    die("❌ Erro HTTP $httpCode");
}

$data = json_decode($response, true);
$codes = $data['record']['simpleCodes'] ?? [];

echo "✅ Total de códigos antes: " . count($codes) . "\n";

// 2. Mostrar últimos códigos gerados
echo "<h2>2. Últimos códigos gerados:</h2>\n";
echo "<table border='1' style='border-collapse: collapse;'>\n";
echo "<tr><th>Código</th><th>Usuário</th><th>Gerado em</th><th>Status</th></tr>\n";

$sortedCodes = $codes;
uksort($sortedCodes, function($a, $b) {
    return isset($codes[$a]['usado_em']) && isset($codes[$b]['usado_em']) 
        ? ($codes[$a]['usado_em'] <=> $codes[$b]['usado_em']) 
        : -1;
});

$count = 0;
foreach ($sortedCodes as $code => $clientData) {
    $count++;
    if ($count > 10) break; // Mostrar apenas últimos 10
    
    $status = 'Desconhecido';
    if (isset($clientData['usado_em']) && $clientData['usado_em'] !== null) {
        $status = 'Usado';
    } elseif (isset($clientData['ativo']) && $clientData['ativo'] === true) {
        $status = 'Ativo';
    } elseif (isset($clientData['ativo']) && $clientData['ativo'] === false) {
        $status = 'Inativo';
    }
    
    echo "<tr>";
    echo "<td>$code</td>";
    echo "<td>{$clientData['usuario']}</td>";
    echo "<td>" . (isset($clientData['usado_em']) ? date('d/m/Y H:i:s', $clientData['usado_em'] / 1000) : 'N/A') . "</td>";
    echo "<td>$status</td>";
    echo "</tr>\n";
}

echo "</table>\n";

// 3. Verificar se há códigos duplicados
echo "<h2>3. Verificando duplicações...</h2>\n";

$usuarioCodes = [];
foreach ($codes as $code => $clientData) {
    $usuario = $clientData['usuario'];
    if (!isset($usuarioCodes[$usuario])) {
        $usuarioCodes[$usuario] = [];
    }
    $usuarioCodes[$usuario][] = $code;
}

$duplicados = [];
foreach ($usuarioCodes as $usuario => $usuarioCodes2) {
    if (count($usuarioCodes2) > 1) {
        $duplicados[$usuario] = $usuarioCodes2;
    }
}

if (empty($duplicados)) {
    echo "<p>✅ Nenhuma duplicação encontrada</p>\n";
} else {
    echo "<p>⚠️ " . count($duplicados) . " usuário(s) com múltiplos códigos:</p>\n";
    echo "<ul>\n";
    foreach ($duplicados as $usuario => $cCodes) {
        echo "<li><strong>$usuario:</strong> " . implode(', ', $cCodes) . " (" . count($cCodes) . " códigos)</li>\n";
    }
    echo "</ul>\n";
}

echo "<hr>\n";
echo "<h2>📊 Diagnóstico</h2>\n";

if (count($duplicados) > 0) {
    echo "<p style='color: orange;'>⚠️ Múltiplos códigos sendo gerados para o mesmo usuário.</p>\n";
    echo "<p><strong>Possíveis causas:</strong></p>\n";
    echo "<ul>\n";
    echo "<li>Botão sendo clicado múltiplas vezes</li>\n";
    echo "<li>Erro ao salvar no JSONBin (código anterior não é sobrescrito)</li>\n";
    echo "<li>App não está aguardando confirmação de salvamento</li>\n";
    echo "</ul>\n";
} else {
    echo "<p style='color: green;'>✅ Não há códigos duplicados detectados</p>\n";
}

echo "<p><strong>Recomendação:</strong></p>\n";
echo "<p>Verificar se o Android está esperando a confirmação de salvamento antes de mostrar o novo código.</p>\n";
?>

