<?php
/**
 * MaxiPTV Downloader - Configurações
 * Arquivo de configuração central
 */

return [
    // Informações do App
    'app' => [
        'name' => 'MaxiPTV',
        'version' => '1.0.89',
        'description' => 'Sistema IPTV Profissional',
        'author' => 'MaxiPTV Team',
        'website' => 'https://maxiptv.com',
        'support_email' => 'contato@maxiptv.com'
    ],
    
    // Configurações do GitHub
    'github' => [
        'repo' => 'https://github.com/seu-usuario/MaxiPTV_v2',
        'releases_url' => 'https://api.github.com/repos/seu-usuario/MaxiPTV_v2/releases/latest',
        'download_url' => 'https://github.com/seu-usuario/MaxiPTV_v2/releases/latest/download/MaxiPTV-v{version}.apk'
    ],
    
    // Configurações do JSONBin
    'jsonbin' => [
        'api_key' => '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae',
        'bin_id' => '68ec647643b1c97be964e96b',
        'base_url' => 'https://api.jsonbin.io/v3',
        'timeout' => 10
    ],
    
    // Configurações de Segurança
    'security' => [
        'code_length' => 12,
        'code_expiry_hours' => 6,
        'max_attempts' => 3,
        'rate_limit_minutes' => 5,
        'allowed_ips' => [], // Deixe vazio para permitir todos
        'blocked_ips' => []
    ],
    
    // Configurações de Download
    'download' => [
        'max_file_size' => '50MB',
        'allowed_extensions' => ['apk'],
        'temp_dir' => './temp/',
        'cleanup_after_hours' => 24
    ],
    
    // Configurações de Log
    'logging' => [
        'enabled' => true,
        'log_file' => './logs/downloader.log',
        'max_log_size' => '10MB',
        'log_level' => 'INFO' // DEBUG, INFO, WARNING, ERROR
    ],
    
    // Configurações de Cache
    'cache' => [
        'enabled' => true,
        'ttl_seconds' => 300, // 5 minutos
        'cache_dir' => './cache/'
    ],
    
    // Configurações de Email (opcional)
    'email' => [
        'enabled' => false,
        'smtp_host' => 'smtp.gmail.com',
        'smtp_port' => 587,
        'smtp_username' => '',
        'smtp_password' => '',
        'from_email' => 'noreply@maxiptv.com',
        'from_name' => 'MaxiPTV System'
    ],
    
    // Configurações de Notificação (opcional)
    'notifications' => [
        'telegram' => [
            'enabled' => false,
            'bot_token' => '',
            'chat_id' => ''
        ],
        'discord' => [
            'enabled' => false,
            'webhook_url' => ''
        ]
    ],
    
    // Configurações de Backup
    'backup' => [
        'enabled' => true,
        'backup_dir' => './backups/',
        'keep_days' => 30,
        'auto_backup_hours' => 24
    ],
    
    // Configurações de Monitoramento
    'monitoring' => [
        'enabled' => true,
        'check_interval_minutes' => 5,
        'alert_on_errors' => true,
        'alert_on_high_usage' => true,
        'max_downloads_per_hour' => 100
    ],
    
    // Configurações de Localização
    'locale' => [
        'default' => 'pt_BR',
        'timezone' => 'America/Sao_Paulo',
        'date_format' => 'd/m/Y',
        'time_format' => 'H:i:s'
    ],
    
    // Configurações de Desenvolvimento
    'development' => [
        'debug' => false,
        'show_errors' => false,
        'log_queries' => false,
        'test_mode' => false
    ]
];
?>
