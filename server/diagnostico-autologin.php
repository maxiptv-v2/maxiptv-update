<?php
/**
 * Script de Diagnóstico - Autologin e Download
 * Verifica todo o fluxo desde o download até o autologin
 */

header('Content-Type: text/html; charset=utf-8');

// Configurações
$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

echo "<!DOCTYPE html>";
echo "<html><head><meta charset='utf-8'><title>Diagnóstico Autologin</title>";
echo "<style>
body { font-family: monospace; background: #1a1a1a; color: #00ff00; padding: 20px; }
h1 { color: #00ff00; }
h2 { color: #ffff00; margin-top: 30px; }
.success { color: #00ff00; }
.error { color: #ff0000; }
.warning { color: #ffaa00; }
.info { color: #00aaff; }
pre { background: #000; padding: 10px; border: 1px solid #333; overflow-x: auto; }
.section { margin: 20px 0; padding: 15px; border: 1px solid #333; }
</style></head><body>";

echo "<h1>🔍 DIAGNÓSTICO COMPLETO - AUTOLOGIN E DOWNLOAD</h1>";

// Obter código de teste
$testCode = $_GET['code'] ?? '1078';
echo "<div class='section'>";
echo "<h2>📝 Código de Teste: <span class='info'>$testCode</span></h2>";
echo "</div>";

// ==================== ETAPA 1: Verificar se código existe no JSONBin ====================
echo "<div class='section'>";
echo "<h2>1️⃣ VERIFICANDO CÓDIGO NO JSONBIN</h2>";

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $apiKey"]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200 && $response) {
        $data = json_decode($response, true);
        $record = $data['record'] ?? [];
        
        echo "<p class='success'>✅ JSONBin acessado com sucesso (HTTP $httpCode)</p>";
        echo "<p class='info'>📦 Total de chaves no record: " . count($record) . "</p>";
        
        // Listar todas as chaves
        $keys = array_keys($record);
        echo "<p class='info'>Chaves encontradas: " . implode(", ", array_slice($keys, 0, 20)) . "...</p>";
        
        // Verificar se código existe
        if (isset($record[$testCode])) {
            $codeData = $record[$testCode];
            echo "<p class='success'>✅ Código '$testCode' encontrado no JSONBin!</p>";
            echo "<pre>";
            echo "Dados do código:\n";
            echo "  Username: " . ($codeData['username'] ?? 'N/A') . "\n";
            echo "  Password: " . (isset($codeData['password']) ? '***' : 'N/A') . "\n";
            echo "  API URL: " . ($codeData['apiUrl'] ?? 'N/A') . "\n";
            echo "  Expiry Date: " . ($codeData['expiryDate'] ?? 'N/A') . "\n";
            echo "  Created At: " . ($codeData['createdAt'] ?? 'N/A') . "\n";
            echo "</pre>";
        } else {
            echo "<p class='error'>❌ Código '$testCode' NÃO encontrado no JSONBin!</p>";
            echo "<p class='warning'>Códigos disponíveis (primeiros 10):</p>";
            $codes = array_filter($keys, function($k) { return preg_match('/^[A-Za-z0-9]{3,10}$/', $k); });
            if (count($codes) > 0) {
                echo "<pre>" . implode(", ", array_slice($codes, 0, 10)) . "</pre>";
            } else {
                echo "<p class='error'>Nenhum código encontrado!</p>";
            }
        }
        
        // Verificar _pending_logins
        if (isset($record['_pending_logins'])) {
            $pendingLogins = $record['_pending_logins'];
            echo "<p class='info'>📋 Códigos pendentes (_pending_logins): " . count($pendingLogins) . "</p>";
            if (count($pendingLogins) > 0) {
                echo "<pre>";
                foreach (array_slice($pendingLogins, -5, 5, true) as $key => $pending) {
                    echo "Chave: $key\n";
                    echo "  Code: " . ($pending['code'] ?? 'N/A') . "\n";
                    echo "  Username: " . ($pending['username'] ?? 'N/A') . "\n";
                    echo "  Timestamp: " . ($pending['timestamp'] ?? 'N/A') . "\n";
                    echo "  Expires At: " . ($pending['expiresAt'] ?? 'N/A') . "\n";
                    echo "  IP: " . ($pending['ip'] ?? 'N/A') . "\n";
                    echo "---\n";
                }
                echo "</pre>";
            }
        } else {
            echo "<p class='warning'>⚠️ _pending_logins não encontrado ou vazio</p>";
        }
        
        // Verificar _login_logs
        if (isset($record['_login_logs'])) {
            $logs = $record['_login_logs'];
            echo "<p class='info'>📋 Logs de login (_login_logs): " . count($logs) . "</p>";
            if (count($logs) > 0) {
                echo "<p class='info'>Últimos 5 logs:</p>";
                echo "<pre>";
                foreach (array_slice($logs, -5) as $log) {
                    echo "[" . ($log['datetime'] ?? 'N/A') . "] ";
                    echo ($log['type'] ?? 'N/A') . ": ";
                    echo ($log['message'] ?? 'N/A') . "\n";
                    if (isset($log['data']['code'])) {
                        echo "  Code: " . $log['data']['code'] . "\n";
                    }
                    echo "---\n";
                }
                echo "</pre>";
            }
        } else {
            echo "<p class='warning'>⚠️ _login_logs não encontrado ou vazio</p>";
        }
        
    } else {
        echo "<p class='error'>❌ Erro ao acessar JSONBin (HTTP $httpCode)</p>";
        echo "<pre>$response</pre>";
    }
} catch (Exception $e) {
    echo "<p class='error'>❌ Erro: " . $e->getMessage() . "</p>";
}

echo "</div>";

// ==================== ETAPA 2: Simular dl.php ====================
echo "<div class='section'>";
echo "<h2>2️⃣ SIMULANDO dl.php (Download)</h2>";

$renderUrl = "https://maxiptv-update-1.onrender.com/dl.php?code=$testCode";
echo "<p class='info'>🔗 URL: <a href='$renderUrl' target='_blank'>$renderUrl</a></p>";

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $renderUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, false); // Não seguir redirect
    curl_setopt($ch, CURLOPT_HEADER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $headers = substr($response, 0, $headerSize);
    $body = substr($response, $headerSize);
    curl_close($ch);
    
    echo "<p class='info'>📡 Resposta HTTP: <span class='success'>$httpCode</span></p>";
    echo "<pre>Headers:\n$headers</pre>";
    
    if ($httpCode === 302 || $httpCode === 301) {
        preg_match('/Location:\s*(.+)/i', $headers, $matches);
        $redirectUrl = $matches[1] ?? 'N/A';
        echo "<p class='success'>✅ Redirect para: <a href='$redirectUrl' target='_blank'>$redirectUrl</a></p>";
    } else {
        echo "<pre>Body:\n$body</pre>";
    }
    
    // Aguardar 2 segundos para o código pendente ser salvo
    echo "<p class='info'>⏳ Aguardando 2 segundos para código pendente ser salvo...</p>";
    sleep(2);
    
} catch (Exception $e) {
    echo "<p class='error'>❌ Erro: " . $e->getMessage() . "</p>";
}

echo "</div>";

// ==================== ETAPA 3: Verificar se código pendente foi salvo ====================
echo "<div class='section'>";
echo "<h2>3️⃣ VERIFICANDO CÓDIGO PENDENTE APÓS dl.php</h2>";

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, ["X-Master-Key: $apiKey"]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200 && $response) {
        $data = json_decode($response, true);
        $record = $data['record'] ?? [];
        
        if (isset($record['_pending_logins'])) {
            $pendingLogins = $record['_pending_logins'];
            echo "<p class='info'>📋 Total de códigos pendentes: " . count($pendingLogins) . "</p>";
            
            // Buscar código pendente mais recente
            $mostRecent = null;
            $mostRecentKey = null;
            $mostRecentTime = 0;
            
            foreach ($pendingLogins as $key => $pending) {
                $timestamp = $pending['timestamp'] ?? 0;
                if ($timestamp > $mostRecentTime) {
                    $mostRecentTime = $timestamp;
                    $mostRecent = $pending;
                    $mostRecentKey = $key;
                }
            }
            
            if ($mostRecent) {
                echo "<p class='success'>✅ Código pendente encontrado!</p>";
                echo "<pre>";
                echo "Chave: $mostRecentKey\n";
                echo "Code: " . ($mostRecent['code'] ?? 'N/A') . "\n";
                echo "Username: " . ($mostRecent['username'] ?? 'N/A') . "\n";
                echo "Timestamp: " . ($mostRecent['timestamp'] ?? 'N/A') . " (" . date('Y-m-d H:i:s', $mostRecent['timestamp'] ?? 0) . ")\n";
                echo "Expires At: " . ($mostRecent['expiresAt'] ?? 'N/A') . " (" . date('Y-m-d H:i:s', $mostRecent['expiresAt'] ?? 0) . ")\n";
                echo "IP: " . ($mostRecent['ip'] ?? 'N/A') . "\n";
                echo "</pre>";
                
                if (($mostRecent['code'] ?? '') === $testCode) {
                    echo "<p class='success'>✅ Código pendente corresponde ao código de teste!</p>";
                } else {
                    echo "<p class='warning'>⚠️ Código pendente ($mostRecent[code]) não corresponde ao código de teste ($testCode)</p>";
                }
            } else {
                echo "<p class='error'>❌ Nenhum código pendente encontrado!</p>";
            }
        } else {
            echo "<p class='error'>❌ _pending_logins não existe ou está vazio!</p>";
        }
        
        // Verificar logs mais recentes
        if (isset($record['_login_logs'])) {
            $logs = $record['_login_logs'];
            echo "<p class='info'>📋 Logs mais recentes (últimos 10):</p>";
            echo "<pre>";
            foreach (array_slice($logs, -10) as $log) {
                echo "[" . ($log['datetime'] ?? 'N/A') . "] ";
                echo ($log['type'] ?? 'N/A') . ": ";
                echo ($log['message'] ?? 'N/A') . "\n";
                if (isset($log['data'])) {
                    echo "  Data: " . json_encode($log['data'], JSON_PRETTY_PRINT) . "\n";
                }
                echo "---\n";
            }
            echo "</pre>";
        }
        
    } else {
        echo "<p class='error'>❌ Erro ao verificar código pendente (HTTP $httpCode)</p>";
    }
} catch (Exception $e) {
    echo "<p class='error'>❌ Erro: " . $e->getMessage() . "</p>";
}

echo "</div>";

// ==================== ETAPA 4: Testar get-pending-code.php ====================
echo "<div class='section'>";
echo "<h2>4️⃣ TESTANDO get-pending-code.php</h2>";

$pendingUrl = "https://maxiptv-update-1.onrender.com/get-pending-code.php";
echo "<p class='info'>🔗 URL: <a href='$pendingUrl' target='_blank'>$pendingUrl</a></p>";

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $pendingUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    echo "<p class='info'>📡 Resposta HTTP: <span class='success'>$httpCode</span></p>";
    echo "<pre>$response</pre>";
    
    $json = json_decode($response, true);
    if ($json) {
        if (isset($json['status']) && $json['status'] === 'ok') {
            echo "<p class='success'>✅ Status: OK</p>";
            if (isset($json['code'])) {
                echo "<p class='success'>✅ Código pendente: <span class='info'>" . $json['code'] . "</span></p>";
                if ($json['code'] === $testCode) {
                    echo "<p class='success'>✅ Código corresponde ao código de teste!</p>";
                } else {
                    echo "<p class='warning'>⚠️ Código ($json[code]) não corresponde ao código de teste ($testCode)</p>";
                }
            } else {
                echo "<p class='error'>❌ Código não encontrado na resposta</p>";
            }
        } else {
            echo "<p class='error'>❌ Status: " . ($json['status'] ?? 'N/A') . "</p>";
            if (isset($json['mensagem'])) {
                echo "<p class='error'>Mensagem: " . $json['mensagem'] . "</p>";
            }
        }
    }
    
} catch (Exception $e) {
    echo "<p class='error'>❌ Erro: " . $e->getMessage() . "</p>";
}

echo "</div>";

// ==================== ETAPA 5: Testar auto_login.php ====================
echo "<div class='section'>";
echo "<h2>5️⃣ TESTANDO auto_login.php</h2>";

$autoLoginUrl = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$testCode";
echo "<p class='info'>🔗 URL: <a href='$autoLoginUrl' target='_blank'>$autoLoginUrl</a></p>";

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $autoLoginUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    echo "<p class='info'>📡 Resposta HTTP: <span class='success'>$httpCode</span></p>";
    echo "<pre>$response</pre>";
    
    $json = json_decode($response, true);
    if ($json) {
        if (isset($json['status']) && $json['status'] === 'success') {
            echo "<p class='success'>✅ Status: SUCCESS</p>";
            if (isset($json['autologin'])) {
                $autologin = $json['autologin'];
                echo "<pre>";
                echo "Dados de autologin:\n";
                echo "  Username: " . ($autologin['username'] ?? 'N/A') . "\n";
                echo "  Password: " . (isset($autologin['password']) ? '***' : 'N/A') . "\n";
                echo "  API URL: " . ($autologin['api_url'] ?? 'N/A') . "\n";
                echo "  Expiry Date: " . ($autologin['expiryDate'] ?? 'N/A') . "\n";
                echo "  Expires In: " . ($autologin['expires_in'] ?? 'N/A') . " segundos\n";
                echo "</pre>";
            } else {
                echo "<p class='error'>❌ Objeto 'autologin' não encontrado na resposta</p>";
            }
        } else {
            echo "<p class='error'>❌ Status: " . ($json['status'] ?? 'N/A') . "</p>";
            if (isset($json['mensagem'])) {
                echo "<p class='error'>Mensagem: " . $json['mensagem'] . "</p>";
            }
        }
    }
    
} catch (Exception $e) {
    echo "<p class='error'>❌ Erro: " . $e->getMessage() . "</p>";
}

echo "</div>";

// ==================== RESUMO FINAL ====================
echo "<div class='section'>";
echo "<h2>📊 RESUMO FINAL</h2>";
echo "<p class='info'>Para testar com outro código, adicione <code>?code=SEU_CODIGO</code> na URL</p>";
echo "<p class='info'>Exemplo: <a href='?code=1078'>?code=1078</a></p>";
echo "</div>";

echo "</body></html>";
?>

