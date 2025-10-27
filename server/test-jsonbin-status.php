<?php
/**
 * MaxiPTV - Teste de Status do JSONBin
 * Verifica conexão e estrutura completa
 */

// Configurações do JSONBin (mesmas do app Android)
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$jsonbin_master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae";

echo "<h1>🔍 Teste de Status JSONBin</h1>\n";
echo "<hr>\n";

echo "<h2>1. Buscando dados do JSONBin...</h2>\n";

// Buscar dados do JSONBin
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $jsonbin_master_key"]);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode !== 200 || !$response) {
    echo "❌ Erro ao conectar com o JSONBin. HTTP $httpCode<br>\n";
    if ($response) {
        echo "Resposta: <pre>" . htmlspecialchars($response) . "</pre>\n";
    }
    die();
}

$data = json_decode($response, true);

echo "✅ Conectado com sucesso!<br>\n";

echo "<h2>2. Estrutura completa do JSONBin:</h2>\n";
echo "<pre>" . print_r($data, true) . "</pre>\n";

echo "<h2>3. Verificando estrutura de 'users':</h2>\n";
if (isset($data['record']['users'])) {
    $users = is_array($data['record']['users']) ? $data['record']['users'] : [];
    echo "✅ Campo 'users' encontrado!<br>\n";
    echo "📊 Total de usuários: " . count($users) . "<br>\n";
    
    if (count($users) > 0) {
        echo "<table border='1' cellpadding='5' cellspacing='0'><tr><th>ID</th><th>Usuário</th><th>API URL</th><th>Expira</th></tr>\n";
        foreach ($users as $user) {
            echo "<tr>\n";
            echo "<td>{$user['id']}</td>\n";
            echo "<td>{$user['username']}</td>\n";
            echo "<td>{$user['apiUrl']}</td>\n";
            echo "<td>{$user['expiryDate']}</td>\n";
            echo "</tr>\n";
        }
        echo "</table>\n";
    } else {
        echo "⚠️ Nenhum usuário cadastrado.<br>\n";
    }
} else {
    echo "❌ Campo 'users' não encontrado no JSONBin.<br>\n";
    echo "Chaves disponíveis: " . implode(", ", array_keys($data['record'] ?? [])) . "<br>\n";
}

echo "<h2>4. Verificando estrutura de 'simpleCodes':</h2>\n";
if (isset($data['record']['simpleCodes'])) {
    $simpleCodes = is_array($data['record']['simpleCodes']) ? $data['record']['simpleCodes'] : [];
    echo "✅ Campo 'simpleCodes' encontrado!<br>\n";
    echo "📊 Total de códigos: " . count($simpleCodes) . "<br>\n";
    
    if (count($simpleCodes) > 0) {
        echo "<table border='1' cellpadding='5' cellspacing='0'><tr><th>Código</th><th>Usuário</th><th>Ativo</th><th>Usado</th></tr>\n";
        foreach ($simpleCodes as $code => $codeData) {
            $ativo = isset($codeData['ativo']) && $codeData['ativo'] ? '✅ Sim' : '❌ Não';
            $usado = isset($codeData['usado']) && $codeData['usado'] ? '✅ Sim' : '❌ Não';
            echo "<tr>\n";
            echo "<td>$code</td>\n";
            echo "<td>{$codeData['usuario']}</td>\n";
            echo "<td>$ativo</td>\n";
            echo "<td>$usado</td>\n";
            echo "</tr>\n";
        }
        echo "</table>\n";
    } else {
        echo "⚠️ Nenhum código cadastrado.<br>\n";
    }
} else {
    echo "❌ Campo 'simpleCodes' não encontrado no JSONBin.<br>\n";
}

echo "<h2>5. Verificando estrutura de 'sessions':</h2>\n";
if (isset($data['record']['sessions'])) {
    $sessions = is_array($data['record']['sessions']) ? $data['record']['sessions'] : [];
    echo "✅ Campo 'sessions' encontrado!<br>\n";
    echo "📊 Total de sessões: " . count($sessions) . "<br>\n";
} else {
    echo "⚠️ Campo 'sessions' não encontrado (normal se não houver sessões ativas).<br>\n";
}

echo "<hr>\n";
echo "<h2>📊 Resumo</h2>\n";
$usersCount = isset($data['record']['users']) ? count($data['record']['users']) : 0;
$codesCount = isset($data['record']['simpleCodes']) ? count($data['record']['simpleCodes']) : 0;
echo "Usuários: $usersCount<br>\n";
echo "Códigos: $codesCount<br>\n";
?>

