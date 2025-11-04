<?php
/**
 * MaxiPTV - debug-login.php
 * Sistema de debug para login automático
 * 
 * Acesse: https://maxiptv-update-1.onrender.com/debug-login.php
 * 
 * Mostra os últimos logs de tentativas de login automático
 */

header('Content-Type: text/html; charset=utf-8');

// Configurações JSONBin
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b";
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// Função para adicionar log
function addLog($type, $message, $data = []) {
    global $jsonbin_url, $jsonbin_update, $apiKey;
    
    try {
        // Buscar dados existentes
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        
        $response = curl_exec($ch);
        curl_close($ch);
        
        $data_record = json_decode($response, true);
        $record = $data_record['record'] ?? [];
        
        // Inicializar logs se não existir
        if (!isset($record['_login_logs']) || !is_array($record['_login_logs'])) {
            $record['_login_logs'] = [];
        }
        
        // Adicionar novo log (limitar a 100 logs)
        $record['_login_logs'][] = [
            'timestamp' => time(),
            'datetime' => date('Y-m-d H:i:s'),
            'type' => $type,
            'message' => $message,
            'data' => $data,
            'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'unknown'
        ];
        
        // Manter apenas os últimos 100 logs
        if (count($record['_login_logs']) > 100) {
            $record['_login_logs'] = array_slice($record['_login_logs'], -100);
        }
        
        // Salvar de volta no JSONBin
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_update);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($record));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "Content-Type: application/json",
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_exec($ch);
        curl_close($ch);
        
    } catch (Exception $e) {
        // Silenciar erros de log
    }
}

// Verificar se é uma requisição para limpar logs
if (isset($_GET['clear']) && $_GET['clear'] == '1') {
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
        curl_close($ch);
        
        $data_record = json_decode($response, true);
        $record = $data_record['record'] ?? [];
        
        $record['_login_logs'] = [];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $jsonbin_update);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($record));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "Content-Type: application/json",
            "X-Master-Key: \$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_exec($ch);
        curl_close($ch);
        
        echo "<script>alert('Logs limpos!'); window.location.href='debug-login.php';</script>";
        exit;
    } catch (Exception $e) {
        echo "<script>alert('Erro ao limpar logs');</script>";
    }
}

// Buscar logs
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
    curl_close($ch);
    
    $data_record = json_decode($response, true);
    $record = $data_record['record'] ?? [];
    $logs = $record['_login_logs'] ?? [];
    
    // Ordenar logs por timestamp (mais recente primeiro)
    usort($logs, function($a, $b) {
        return ($b['timestamp'] ?? 0) - ($a['timestamp'] ?? 0);
    });
    
} catch (Exception $e) {
    $logs = [];
}

?>
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Debug Login Automático - MaxiPTV</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        
        .header h1 {
            font-size: 28px;
            margin-bottom: 10px;
        }
        
        .header p {
            opacity: 0.9;
            font-size: 14px;
        }
        
        .controls {
            padding: 20px;
            background: #f5f5f5;
            border-bottom: 1px solid #ddd;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .controls button {
            background: #667eea;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.3s;
        }
        
        .controls button:hover {
            background: #5568d3;
        }
        
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            padding: 20px;
            background: #f9f9f9;
        }
        
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            text-align: center;
        }
        
        .stat-card .number {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            margin-bottom: 5px;
        }
        
        .stat-card .label {
            color: #666;
            font-size: 14px;
        }
        
        .logs {
            padding: 20px;
        }
        
        .log-item {
            background: #f9f9f9;
            border-left: 4px solid #667eea;
            padding: 15px;
            margin-bottom: 15px;
            border-radius: 5px;
            transition: transform 0.2s;
        }
        
        .log-item:hover {
            transform: translateX(5px);
        }
        
        .log-item.success {
            border-left-color: #28a745;
        }
        
        .log-item.error {
            border-left-color: #dc3545;
        }
        
        .log-item.warning {
            border-left-color: #ffc107;
        }
        
        .log-item.info {
            border-left-color: #17a2b8;
        }
        
        .log-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }
        
        .log-type {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }
        
        .log-type.success { background: #28a745; color: white; }
        .log-type.error { background: #dc3545; color: white; }
        .log-type.warning { background: #ffc107; color: black; }
        .log-type.info { background: #17a2b8; color: white; }
        
        .log-time {
            color: #666;
            font-size: 12px;
        }
        
        .log-message {
            font-size: 14px;
            margin-bottom: 8px;
            color: #333;
        }
        
        .log-data {
            background: #fff;
            padding: 10px;
            border-radius: 4px;
            margin-top: 8px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            color: #555;
            white-space: pre-wrap;
            max-height: 200px;
            overflow-y: auto;
        }
        
        .no-logs {
            text-align: center;
            padding: 60px 20px;
            color: #999;
        }
        
        .no-logs svg {
            width: 80px;
            height: 80px;
            margin-bottom: 20px;
            opacity: 0.5;
        }
        
        .refresh-info {
            text-align: center;
            padding: 10px;
            background: #e3f2fd;
            color: #1976d2;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔍 Debug Login Automático</h1>
            <p>Sistema de rastreamento de tentativas de login automático</p>
        </div>
        
        <div class="controls">
            <div>
                <strong>Total de logs:</strong> <?php echo count($logs); ?>
            </div>
            <div>
                <button onclick="window.location.reload()">🔄 Atualizar</button>
                <button onclick="if(confirm('Limpar todos os logs?')) window.location.href='?clear=1'">🗑️ Limpar Logs</button>
            </div>
        </div>
        
        <div class="stats">
            <?php
            $successCount = 0;
            $errorCount = 0;
            $warningCount = 0;
            $infoCount = 0;
            
            foreach ($logs as $log) {
                switch ($log['type'] ?? '') {
                    case 'success': $successCount++; break;
                    case 'error': $errorCount++; break;
                    case 'warning': $warningCount++; break;
                    case 'info': $infoCount++; break;
                }
            }
            ?>
            <div class="stat-card">
                <div class="number"><?php echo $successCount; ?></div>
                <div class="label">✅ Sucessos</div>
            </div>
            <div class="stat-card">
                <div class="number"><?php echo $errorCount; ?></div>
                <div class="label">❌ Erros</div>
            </div>
            <div class="stat-card">
                <div class="number"><?php echo $warningCount; ?></div>
                <div class="label">⚠️ Avisos</div>
            </div>
            <div class="stat-card">
                <div class="number"><?php echo $infoCount; ?></div>
                <div class="label">ℹ️ Informações</div>
            </div>
        </div>
        
        <div class="logs">
            <?php if (empty($logs)): ?>
                <div class="no-logs">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <h3>Nenhum log encontrado</h3>
                    <p>Os logs aparecerão aqui quando o app tentar fazer login automático</p>
                </div>
            <?php else: ?>
                <?php foreach ($logs as $log): ?>
                    <div class="log-item <?php echo $log['type'] ?? 'info'; ?>">
                        <div class="log-header">
                            <span class="log-type <?php echo $log['type'] ?? 'info'; ?>">
                                <?php echo strtoupper($log['type'] ?? 'INFO'); ?>
                            </span>
                            <span class="log-time"><?php echo $log['datetime'] ?? date('Y-m-d H:i:s'); ?></span>
                        </div>
                        <div class="log-message">
                            <?php echo htmlspecialchars($log['message'] ?? ''); ?>
                        </div>
                        <?php if (!empty($log['data'])): ?>
                            <div class="log-data">
                                <?php echo htmlspecialchars(json_encode($log['data'], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE)); ?>
                            </div>
                        <?php endif; ?>
                        <div style="margin-top: 8px; font-size: 11px; color: #999;">
                            IP: <?php echo htmlspecialchars($log['ip'] ?? 'unknown'); ?> | 
                            User-Agent: <?php echo htmlspecialchars(substr($log['user_agent'] ?? 'unknown', 0, 50)); ?>
                        </div>
                    </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
        
        <div class="refresh-info">
            💡 Dica: Atualize esta página após instalar o app para ver os logs em tempo real
        </div>
    </div>
    
    <script>
        // Auto-refresh a cada 10 segundos
        setTimeout(function() {
            window.location.reload();
        }, 10000);
    </script>
</body>
</html>

