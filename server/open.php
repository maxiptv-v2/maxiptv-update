<?php
/**
 * MaxiPTV - open.php
 * Endpoint para abrir app com código via deep link
 * Uso: https://maxiptv-update-1.onrender.com/open?code=6789
 * 
 * Identifica cliente usando dados do usuário do painel (JSONBin)
 */

header('Content-Type: text/html; charset=utf-8');

$code = $_GET['code'] ?? '';

if (empty($code)) {
    echo "<h3>Código não fornecido</h3>";
    exit;
}

// Buscar dados do usuário do código no JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

try {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode !== 200 || !$response) {
        echo "<h3>Erro ao conectar com servidor</h3>";
        exit;
    }
    
    $data = json_decode($response, true);
    $codigos = $data['record'] ?? [];
    
    if (!isset($codigos[$code]) || !is_array($codigos[$code])) {
        echo "<h3>Código inválido</h3>";
        exit;
    }
    
    $user = $codigos[$code];
    $username = $user['username'] ?? '';
    
    // Redirecionar para deep link do app com o código
    // O app vai buscar as credenciais usando index.php?code=CODIGO
    ?>
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0;url=maxiptv://login?code=<?php echo urlencode($code); ?>">
        <title>Abrindo MaxiPTV...</title>
    </head>
    <body>
        <h3>Abrindo app...</h3>
        <p>Cliente: <?php echo htmlspecialchars($username); ?></p>
        <p>Código: <?php echo htmlspecialchars($code); ?></p>
        <script>
            // Tentar abrir via deep link
            window.location.href = "maxiptv://login?code=<?php echo urlencode($code); ?>";
            
            // Fallback: abrir app via Intent (Android)
            setTimeout(function() {
                window.location.href = "intent://login?code=<?php echo urlencode($code); ?>#Intent;scheme=maxiptv;package=com.maxiptv;end";
            }, 500);
        </script>
    </body>
    </html>
    <?php
    
} catch (Exception $e) {
    echo "<h3>Erro: " . htmlspecialchars($e->getMessage()) . "</h3>";
}
?>

