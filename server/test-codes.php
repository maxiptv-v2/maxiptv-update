<?php
// Script para testar e ver códigos no JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$headers = ["X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"];

echo "<h1>🔍 Teste de Códigos JSONBin</h1>\n";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200) {
    $data = json_decode($response, true);
    
    echo "<h2>📊 Estrutura do JSONBin:</h2>\n";
    echo "<pre>" . print_r($data, true) . "</pre>\n";
    
    if (isset($data['record']['simpleCodes'])) {
        $codes = $data['record']['simpleCodes'];
        echo "<h3>Códigos cadastrados: " . count($codes) . "</h3>\n";
        
        if (count($codes) > 0) {
            echo "<table border='1' style='border-collapse: collapse;'>\n";
            echo "<tr><th>Código</th><th>Usuário</th><th>API</th><th>Ativo</th><th>Usado</th><th>Expira</th></tr>\n";
            
            foreach ($codes as $code => $clientData) {
                $status = $clientData['ativo'] ? '✅' : '❌';
                $usado = $clientData['usado'] ? '✅' : '❌';
                echo "<tr>";
                echo "<td>$code</td>";
                echo "<td>{$clientData['usuario']}</td>";
                echo "<td>" . substr($clientData['api'], 0, 30) . "...</td>";
                echo "<td>$status</td>";
                echo "<td>$usado</td>";
                echo "<td>{$clientData['expira_em']}</td>";
                echo "</tr>\n";
            }
            
            echo "</table>\n";
        }
    } else {
        echo "<p>❌ Estrutura simpleCodes não encontrada</p>\n";
    }
} else {
    echo "<p>❌ Erro HTTP $httpCode</p>\n";
}
?>
