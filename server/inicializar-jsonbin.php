<?php
/**
 * Script para inicializar a estrutura simpleCodes no JSONBin
 */

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$jsonbin_master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

echo "<h1>🔧 Inicialização da Estrutura JSONBin</h1>\n";

// 1. Buscar dados atuais
echo "<h2>1. Buscando dados atuais...</h2>\n";
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $jsonbin_master_key"]);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200) {
    $data = json_decode($response, true);
    
    // Verificar se simpleCodes existe
    if (isset($data['record']['simpleCodes'])) {
        echo "✅ Estrutura simpleCodes já existe!<br>\n";
        $codes = is_array($data['record']['simpleCodes']) ? $data['record']['simpleCodes'] : [];
        echo "Códigos cadastrados: " . count($codes) . "<br>\n";
    } else {
        echo "⚠️ Estrutura simpleCodes não encontrada, criando...<br>\n";
        
        // Criar estrutura
        if (!isset($data['record'])) {
            $data['record'] = [];
        }
        
        if (!isset($data['record']['simpleCodes'])) {
            $data['record']['simpleCodes'] = [];
        }
        
        // Atualizar no JSONBin
        $updateUrl = $jsonbin_url;
        $updateHeaders = [
            "X-Master-Key: $jsonbin_master_key",
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
            echo "✅ Estrutura simpleCodes criada com sucesso!<br>\n";
        } else {
            echo "❌ Erro ao criar estrutura: HTTP $httpCode<br>\n";
        }
    }
} else {
    echo "❌ Erro ao buscar dados: HTTP $httpCode<br>\n";
}

echo "<hr>\n";
echo "<h2>📊 Resumo</h2>\n";
echo "<p>✅ Estrutura JSONBin inicializada!</p>\n";
echo "<p>Agora você pode gerar códigos no painel admin do app.</p>\n";
?>
