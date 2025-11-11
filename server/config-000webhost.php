<?php
/**
 * Configuração central dos serviços (Render / outros provedores)
 *
 * Permite sobrescrever as variáveis via ambiente:
 * - LOGIN_JSONBIN_URL / LOGIN_JSONBIN_MASTER_KEY
 * - FINGERPRINT_JSONBIN_URL / FINGERPRINT_JSONBIN_MASTER_KEY
 * - SITE_URL / APK_URL
 */

// 🔑 DADOS DO JSONBIN.IO - Autologin / Códigos
$login_jsonbin_url = getenv('LOGIN_JSONBIN_URL') ?: 'https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7';
$login_jsonbin_master_key = getenv('LOGIN_JSONBIN_MASTER_KEY') ?: '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// 🔐 DADOS DO JSONBIN.IO - Perfis de dispositivo / Safe Area
$fingerprint_jsonbin_url = getenv('FINGERPRINT_JSONBIN_URL') ?: 'https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b';
$fingerprint_jsonbin_master_key = getenv('FINGERPRINT_JSONBIN_MASTER_KEY') ?: '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae';

// 🌐 URL DO SERVIÇO (Render)
$site_url = getenv('SITE_URL') ?: 'https://maxiptv-update-1.onrender.com';

// 📱 URL DO APK NO GITHUB (padrão)
$apk_url = getenv('APK_URL') ?: 'https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk';

if (php_sapi_name() === 'cli') {
    echo "🔧 CONFIGURAÇÃO DO SERVIDOR PHP\n";
    echo "===============================\n";
    echo "📊 JSONBin (login) URL: " . $login_jsonbin_url . "\n";
    echo "🔑 Login Master Key: " . substr($login_jsonbin_master_key, 0, 20) . "...\n";
    echo "📊 JSONBin (fingerprint) URL: " . $fingerprint_jsonbin_url . "\n";
    echo "🔑 Fingerprint Master Key: " . substr($fingerprint_jsonbin_master_key, 0, 20) . "...\n";
    echo "🌐 Site URL: " . $site_url . "\n";
    echo "📱 APK URL: " . $apk_url . "\n";
    echo "===============================\n";
    echo "✅ Configuração carregada com sucesso!\n";
}
?>
