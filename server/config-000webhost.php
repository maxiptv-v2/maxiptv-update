<?php
<?php
/**
 * Configuração central dos serviços (Render / outros provedores)
 *
 * Permite sobrescrever as variáveis via ambiente:
 * - JSONBIN_URL
 * - JSONBIN_MASTER_KEY
 * - SITE_URL
 * - APK_URL
 */

// 🔑 DADOS DO JSONBIN.IO (default: bin principal usada pelo painel Render)
$jsonbin_url = getenv('JSONBIN_URL') ?: 'https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7';
$jsonbin_master_key = getenv('JSONBIN_MASTER_KEY') ?: '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// 🌐 URL DO SITE/APIs (sobrescrevível via ambiente)
$site_url = getenv('SITE_URL') ?: 'https://maxiptv-update-1.onrender.com';

// 📱 URL padrão do APK (pode ser sobrescrito via ambiente)
$apk_url = getenv('APK_URL') ?: 'https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk';

if (php_sapi_name() === 'cli') {
    echo "🔧 CONFIGURAÇÃO DO SERVIDOR PHP\n";
    echo "===============================\n";
    echo "📊 JSONBin URL: " . $jsonbin_url . "\n";
    echo "🔑 Master Key: " . substr($jsonbin_master_key, 0, 20) . "...\n";
    echo "🌐 Site URL: " . $site_url . "\n";
    echo "📱 APK URL: " . $apk_url . "\n";
    echo "===============================\n";
    echo "✅ Configuração carregada com sucesso!\n";
}
?>
