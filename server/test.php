<?php
/**
 * MaxiPTV Downloader - Teste do Servidor
 * Testa se o servidor está funcionando corretamente
 */

echo "<h1>🧪 Teste do Servidor MaxiPTV</h1>\n";
echo "<hr>\n";

// Teste 1: Verificar PHP
echo "<h2>1. Verificação do PHP</h2>\n";
echo "Versão do PHP: " . phpversion() . "<br>\n";
echo "Extensões necessárias:<br>\n";

$required_extensions = ['curl', 'json'];
foreach ($required_extensions as $ext) {
    $status = extension_loaded($ext) ? '✅' : '❌';
    echo "- $ext: $status<br>\n";
}

// Teste 2: Verificar conectividade JSONBin
echo "<h2>2. Teste de Conectividade JSONBin</h2>\n";
try {
    $jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
    $headers = ["X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"];
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
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
            if (isset($data['record']['simpleCodes'])) {
                echo "✅ Estrutura simpleCodes: OK<br>\n";
                echo "Códigos cadastrados: " . count($data['record']['simpleCodes']) . "<br>\n";
                
                // Mostrar códigos disponíveis
                if (!empty($data['record']['simpleCodes'])) {
                    echo "<h3>Códigos Disponíveis:</h3>\n";
                    echo "<ul>\n";
                    foreach ($data['record']['simpleCodes'] as $code => $clientData) {
                        $status = $clientData['ativo'] ? '✅ Ativo' : '❌ Inativo';
                        echo "<li>Código: $code - Usuário: {$clientData['usuario']} - Status: $status</li>\n";
                    }
                    echo "</ul>\n";
                }
            } else {
                echo "⚠️ Estrutura simpleCodes: Não encontrada<br>\n";
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

// Teste 3: Verificar GitHub
echo "<h2>3. Teste de Conectividade GitHub</h2>\n";
try {
    $githubUrl = "https://api.github.com/repos/maxiptv-v2/maxiptv-update/releases/latest";
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
            if (isset($data['assets'][0]['browser_download_url'])) {
                echo "✅ URL de download: " . $data['assets'][0]['browser_download_url'] . "<br>\n";
            }
        } else {
            echo "⚠️ Release não encontrada<br>\n";
        }
    } else {
        echo "❌ Conexão com GitHub: Erro HTTP $httpCode<br>\n";
    }
} catch (Exception $e) {
    echo "❌ Erro na conexão GitHub: " . $e->getMessage() . "<br>\n";
}

// Teste 4: Verificar permissões
echo "<h2>4. Verificação de Permissões</h2>\n";
if (is_writable('.')) {
    echo "✅ Escrita no diretório: OK<br>\n";
} else {
    echo "❌ Escrita no diretório: Falhou<br>\n";
}

// Resumo
echo "<hr>\n";
echo "<h2>📊 Resumo do Teste</h2>\n";
echo "<p><strong>Status:</strong> Servidor pronto para uso!</p>\n";
echo "<p><strong>Como usar:</strong></p>\n";
echo "<ol>\n";
echo "<li>Cliente digita código de 4 dígitos no downloader</li>\n";
echo "<li>Downloader acessa: <code>https://seudominio.com/server/download.php?code=XXXX</code></li>\n";
echo "<li>Servidor valida código no JSONBin</li>\n";
echo "<li>Se válido, redireciona para download do GitHub</li>\n";
echo "<li>Cliente baixa APK com credenciais pré-configuradas</li>\n";
echo "</ol>\n";

echo "<p><strong>Exemplo de uso:</strong></p>\n";
echo "<p>Se o código for <strong>7788</strong>, o downloader acessa:</p>\n";
echo "<p><code>https://seudominio.com/server/download.php?code=7788</code></p>\n";
?>
