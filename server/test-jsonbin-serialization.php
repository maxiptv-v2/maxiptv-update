<?php
/**
 * Script para testar serialização do JSONBin
 */

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

echo "<h1>🧪 Teste de Serialização JSONBin</h1>\n";
echo "<hr>\n";

// 1. Buscar dados atuais
echo "<h2>1. Buscando dados do JSONBin...</h2>\n";

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
    echo "⚠️ Estrutura simpleCodes não encontrada\n";
    echo "<pre>" . print_r($data, true) . "</pre>\n";
    exit;
}

$codes = $data['record']['simpleCodes'];

echo "✅ Estrutura simpleCodes encontrada\n";
echo "📊 Total de códigos: " . count($codes) . "\n";

// 2. Testar cada código
echo "<h2>2. Testando cada código...</h2>\n";

echo "<table border='1' style='border-collapse: collapse; width: 100%;'>\n";
echo "<tr><th>Código</th><th>Usuário</th><th>Ativo</th><th>Usado</th><th>Campos Disponíveis</th></tr>\n";

foreach ($codes as $code => $clientData) {
    $ativo = isset($clientData['ativo']) ? ($clientData['ativo'] ? '✅' : '❌') : '⚠️ N/A';
    $usado = isset($clientData['usado']) ? ($clientData['usado'] ? '✅' : '❌') : '⚠️ N/A';
    
    $campos = implode(', ', array_keys($clientData));
    
    echo "<tr>";
    echo "<td><strong>$code</strong></td>";
    echo "<td>{$clientData['usuario']}</td>";
    echo "<td>$ativo</td>";
    echo "<td>$usado</td>";
    echo "<td style='font-size: 10px;'>$campos</td>";
    echo "</tr>\n";
}

echo "</table>\n";

// 3. Testar estrutura esperada
echo "<h2>3. Estrutura Esperada vs. Atual</h2>\n";

$primeiroCode = reset($codes);
$primeiroKey = key($codes);

echo "<h3>Código exemplo ($primeiroKey):</h3>\n";
echo "<pre>" . print_r($primeiroCode, true) . "</pre>\n";

$camposEsperados = ['usuario', 'senha', 'api', 'apk', 'expira_em', 'ativo', 'usado', 'usado_em', 'usado_device'];
$camposAtuais = array_keys($primeiroCode);

echo "<h3>Campos esperados:</h3>\n";
echo "<ul>\n";
foreach ($camposEsperados as $campo) {
    $status = in_array($campo, $camposAtuais) ? '✅' : '❌';
    echo "<li>$status $campo</li>\n";
}
echo "</ul>\n";

echo "<h3>Faltando:</h3>\n";
$faltando = array_diff($camposEsperados, $camposAtuais);
if (empty($faltando)) {
    echo "<p>✅ Nenhum campo faltando!</p>\n";
} else {
    echo "<p>⚠️ Faltam os campos: " . implode(', ', $faltando) . "</p>\n";
}

echo "<hr>\n";
echo "<p><strong>Conclusão:</strong></p>\n";
if (in_array('ativo', $faltando) || in_array('usado', $faltando)) {
    echo "<p style='color: red;'>❌ Os campos 'ativo' e 'usado' não estão sendo salvos pelo Android!</p>\n";
    echo "<p>É preciso adicionar <code>encodeDefaults = true</code> na configuração do JSON no Android.</p>\n";
} else {
    echo "<p style='color: green;'>✅ Todos os campos estão sendo salvos corretamente!</p>\n";
}
?>

