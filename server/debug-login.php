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
$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest";
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7";
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
        
        // Adicionar novo log (limitar a 500 logs para manter histórico maior)
        $record['_login_logs'][] = [
            'timestamp' => time(),
            'datetime' => date('Y-m-d H:i:s'),
            'type' => $type,
            'message' => $message,
            'data' => $data,
            'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'unknown'
        ];
        
        // Manter apenas os últimos 500 logs (aumentado de 100 para 500)
        if (count($record['_login_logs']) > 500) {
            $record['_login_logs'] = array_slice($record['_login_logs'], -500);
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
    $deviceLogs = [];
    if (isset($record['_device_logs']) && is_array($record['_device_logs'])) {
        $deviceLogs = $record['_device_logs'];
        usort($deviceLogs, function($a, $b) {
            return strcmp($b['loggedAt'] ?? '', $a['loggedAt'] ?? '');
        });
    }
    $pendingLoginsRaw = $record['_pending_logins'] ?? [];
    $pendingLogins = [];
    
    if (is_array($pendingLoginsRaw)) {
        foreach ($pendingLoginsRaw as $key => $pending) {
            if (!is_array($pending)) {
                continue;
            }
            $pending['pendingKey'] = $key;
            $pendingLogins[] = $pending;
        }
        
        usort($pendingLogins, function($a, $b) {
            return ($b['timestamp'] ?? 0) - ($a['timestamp'] ?? 0);
        });
    }
    
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
        
        .pending-section {
            padding: 0 20px 20px 20px;
        }
        .device-section {
            padding: 0 20px 20px 20px;
        }
        .device-card {
            background: #ffffff;
            border-left: 4px solid #4caf50;
            padding: 15px;
            margin-bottom: 12px;
            border-radius: 5px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            font-size: 13px;
        }
        .device-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            font-weight: bold;
            color: #4caf50;
        }
        .device-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 8px 20px;
        }
        .device-grid div {
            background: #fafafa;
            padding: 8px;
            border-radius: 4px;
            font-family: 'Courier New', monospace;
            color: #444;
            word-break: break-all;
        }
        
        .pending-card {
            background: #fff;
            border-left: 4px solid #ff9800;
            padding: 15px;
            margin-bottom: 12px;
            border-radius: 5px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
            font-size: 13px;
        }
        
        .pending-card h4 {
            margin-bottom: 8px;
            color: #ff9800;
        }
        
        .pending-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 8px 20px;
        }
        
        .pending-grid div {
            background: #fafafa;
            padding: 8px;
            border-radius: 4px;
            font-family: 'Courier New', monospace;
            color: #444;
            word-break: break-all;
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
                <button id="refreshToggle" onclick="toggleAutoRefresh()">⏸️ Pausar Auto-Refresh</button>
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
        
        <div class="pending-section">
            <h3 style="margin-bottom: 10px; color: #ff9800;">⌛ Códigos Pendentes (últimos 15 minutos)</h3>
            <?php if (empty($pendingLogins)): ?>
                <div class="no-logs" style="padding: 30px 20px;">
                    <h3>Nenhum código pendente encontrado</h3>
                    <p>Baixe o APK novamente para gerar um código pendente e testar o autologin.</p>
                </div>
            <?php else: ?>
                <?php foreach ($pendingLogins as $pending): ?>
                    <div class="pending-card">
                        <h4>Código <?php echo htmlspecialchars($pending['code'] ?? ''); ?> • Usuário <?php echo htmlspecialchars($pending['username'] ?? ''); ?></h4>
                        <div class="pending-grid">
                            <div><strong>Senha:</strong> <?php echo htmlspecialchars($pending['password'] ?? ''); ?></div>
                            <div><strong>API URL:</strong> <?php echo htmlspecialchars($pending['apiUrl'] ?? ''); ?></div>
                            <div><strong>Expira em:</strong> <?php echo htmlspecialchars($pending['expiryDate'] ?? ''); ?></div>
                            <div><strong>ExpiresAt (Unix):</strong> <?php echo htmlspecialchars((string)($pending['expiresAt'] ?? '')); ?></div>
                            <div><strong>Gerado em:</strong> <?php echo isset($pending['timestamp']) ? date('Y-m-d H:i:s', $pending['timestamp']) : 'N/A'; ?></div>
                            <div><strong>IP:</strong> <?php echo htmlspecialchars($pending['ip'] ?? ''); ?></div>
                            <div><strong>User-Agent:</strong> <?php echo htmlspecialchars($pending['user_agent'] ?? ''); ?></div>
                            <div><strong>Chave:</strong> <?php echo htmlspecialchars($pending['pendingKey'] ?? ''); ?></div>
                        </div>
                    </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
        
        <div class="device-section">
            <h3 style="margin-bottom: 10px; color: #4caf50;">🖥️ Dispositivos Detectados (últimos 100)</h3>
            <?php if (empty($deviceLogs)): ?>
                <div class="no-logs" style="padding: 30px 20px;">
                    <h3>Nenhum dispositivo registrado ainda</h3>
                    <p>Abra o app (Home) para que o dispositivo envie um relatório automático.</p>
                </div>
            <?php else: ?>
                <?php foreach ($deviceLogs as $device): ?>
                    <div class="device-card">
                        <div class="device-header">
                            <span><?php echo htmlspecialchars($device['classification'] ?? 'Desconhecido'); ?></span>
                            <span><?php echo htmlspecialchars($device['loggedAt'] ?? ''); ?></span>
                        </div>
                        <div class="device-grid">
                            <div><strong>Fabricante:</strong> <?php echo htmlspecialchars($device['manufacturer'] ?? ''); ?></div>
                            <div><strong>Modelo:</strong> <?php echo htmlspecialchars($device['model'] ?? ''); ?></div>
                            <div><strong>Marca:</strong> <?php echo htmlspecialchars($device['brand'] ?? ''); ?></div>
                            <div><strong>Produto:</strong> <?php echo htmlspecialchars($device['product'] ?? ''); ?></div>
                            <div><strong>Resolução:</strong> <?php echo htmlspecialchars($device['resolution'] ?? ''); ?></div>
                            <div><strong>DPI:</strong> <?php echo htmlspecialchars((string)($device['densityDpi'] ?? '')); ?></div>
                            <div><strong>Diagonal estimada:</strong> <?php echo htmlspecialchars((string)($device['diagonalInches'] ?? '')); ?>"</div>
                            <div><strong>Overscan:</strong> <?php echo htmlspecialchars($device['overscan'] ?? ''); ?></div>
                            <div><strong>Versão App:</strong> <?php echo htmlspecialchars($device['appVersion'] ?? ''); ?></div>
                            <div><strong>IP:</strong> <?php echo htmlspecialchars($device['ip'] ?? ''); ?></div>
                            <div><strong>User-Agent:</strong> <?php echo htmlspecialchars($device['userAgent'] ?? ''); ?></div>
                        </div>
                    </div>
                <?php endforeach; ?>
            <?php endif; ?>
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
        let autoRefreshEnabled = localStorage.getItem('autoRefresh') !== 'false';
        let refreshInterval = null;
        
        function toggleAutoRefresh() {
            autoRefreshEnabled = !autoRefreshEnabled;
            localStorage.setItem('autoRefresh', autoRefreshEnabled);
            
            if (autoRefreshEnabled) {
                startAutoRefresh();
                document.getElementById('refreshToggle').textContent = '⏸️ Pausar Auto-Refresh';
            } else {
                stopAutoRefresh();
                document.getElementById('refreshToggle').textContent = '▶️ Iniciar Auto-Refresh';
            }
        }
        
        function startAutoRefresh() {
            if (refreshInterval) clearInterval(refreshInterval);
            refreshInterval = setInterval(function() {
                window.location.reload();
            }, 5000); // Aumentado de 10s para 5s para atualizar mais rápido
        }
        
        function stopAutoRefresh() {
            if (refreshInterval) {
                clearInterval(refreshInterval);
                refreshInterval = null;
            }
        }
        
        // Verificar se auto-refresh está habilitado
        if (autoRefreshEnabled) {
            startAutoRefresh();
        }
    </script>
</body>
</html>

