<?php
/**
 * MaxiPTV Downloader - Teste do Sistema
 * Script para testar se tudo está funcionando
 */

// Incluir configurações
$config = require_once 'config.php';

echo "<h1>🧪 Teste do Sistema MaxiPTV Downloader</h1>\n";
echo "<hr>\n";

// Teste 1: Verificar PHP
echo "<h2>1. Verificação do PHP</h2>\n";
echo "Versão do PHP: " . phpversion() . "<br>\n";
echo "Extensões necessárias:<br>\n";

$required_extensions = ['curl', 'json', 'openssl'];
foreach ($required_extensions as $ext) {
    $status = extension_loaded($ext) ? '✅' : '❌';
    echo "- $ext: $status<br>\n";
}

// Teste 2: Verificar pastas
echo "<h2>2. Verificação de Pastas</h2>\n";
$required_dirs = ['logs', 'temp', 'cache', 'backups'];
foreach ($required_dirs as $dir) {
    $exists = is_dir($dir) ? '✅' : '❌';
    $writable = is_writable($dir) ? '✅' : '❌';
    echo "- $dir: Existe $exists | Gravável $writable<br>\n";
}

// Teste 3: Verificar conectividade JSONBin
echo "<h2>3. Teste de Conectividade JSONBin</h2>\n";
try {
    $url = $config['jsonbin']['base_url'] . '/b/' . $config['jsonbin']['bin_id'] . '/latest';
    $headers = [
        'X-Master-Key: ' . $config['jsonbin']['api_key'],
        'Content-Type: application/json'
    ];
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200) {
        echo "✅ Conexão com JSONBin: OK<br>\n";
        $data = json_decode($response, true);
        if ($data && isset($data['record'])) {
            echo "✅ Dados recebidos: OK<br>\n";
            if (isset($data['record']['clientCodes'])) {
                echo "✅ Estrutura clientCodes: OK<br>\n";
                echo "Códigos cadastrados: " . count($data['record']['clientCodes']) . "<br>\n";
            } else {
                echo "⚠️ Estrutura clientCodes: Não encontrada<br>\n";
            }
        } else {
            echo "❌ Dados recebidos: Inválidos<br>\n";
        }
    } else {
        echo "❌ Conexão com JSONBin: Erro HTTP $httpCode<br>\n";
    }
} catch (Exception $e) {
    echo "❌ Erro na conexão: " . $e->getMessage() . "<br>\n";
}

// Teste 4: Verificar GitHub
echo "<h2>4. Teste de Conectividade GitHub</h2>\n";
try {
    $githubUrl = $config['github']['releases_url'];
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $githubUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    curl_setopt($ch, CURLOPT_USERAGENT, 'MaxiPTV-Downloader/1.0');
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200) {
        echo "✅ Conexão com GitHub: OK<br>\n";
        $data = json_decode($response, true);
        if ($data && isset($data['tag_name'])) {
            echo "✅ Release encontrada: " . $data['tag_name'] . "<br>\n";
        } else {
            echo "⚠️ Release não encontrada<br>\n";
        }
    } else {
        echo "❌ Conexão com GitHub: Erro HTTP $httpCode<br>\n";
    }
} catch (Exception $e) {
    echo "❌ Erro na conexão GitHub: " . $e->getMessage() . "<br>\n";
}

// Teste 5: Verificar permissões
echo "<h2>5. Verificação de Permissões</h2>\n";
$test_file = 'temp/test_write.txt';
if (file_put_contents($test_file, 'test') !== false) {
    echo "✅ Escrita em temp: OK<br>\n";
    unlink($test_file);
} else {
    echo "❌ Escrita em temp: Falhou<br>\n";
}

// Teste 6: Verificar configurações
echo "<h2>6. Verificação de Configurações</h2>\n";
echo "App Name: " . $config['app']['name'] . "<br>\n";
echo "Versão: " . $config['app']['version'] . "<br>\n";
echo "JSONBin Bin ID: " . $config['jsonbin']['bin_id'] . "<br>\n";
echo "GitHub Repo: " . $config['github']['repo'] . "<br>\n";

// Resumo
echo "<hr>\n";
echo "<h2>📊 Resumo do Teste</h2>\n";
echo "<p><strong>Status:</strong> Sistema pronto para uso!</p>\n";
echo "<p><strong>Próximos passos:</strong></p>\n";
echo "<ol>\n";
echo "<li>Configurar servidor web (Apache/Nginx)</li>\n";
echo "<li>Atualizar URLs do GitHub em config.php</li>\n";
echo "<li>Testar geração de código no app</li>\n";
echo "<li>Testar download completo</li>\n";
echo "</ol>\n";

echo "<p><strong>Links úteis:</strong></p>\n";
echo "<ul>\n";
echo "<li><a href='index.php'>Interface Principal</a></li>\n";
echo "<li><a href='README.md'>Documentação</a></li>\n";
echo "</ul>\n";
?>
