<?php
// CONFIGURAÇÃO PARA 000WEBHOST
// Substitua os valores abaixo pelos seus dados reais

// 🔑 DADOS DO JSONBIN.IO
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"; // SEU BIN_ID
$jsonbin_master_key = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"; // SUA MASTER_KEY

// 🌐 URL DO SEU SITE NO 000WEBHOST
$site_url = "https://maxiptv-downloader.000webhostapp.com"; // SUBSTITUA pelo seu domínio

// 📱 URL DO APK NO GITHUB
$apk_url = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/MaxiPTV-v1.0.95.apk";

// ✅ TESTE DE CONFIGURAÇÃO
echo "🔧 CONFIGURAÇÃO DO SERVIDOR PHP\n";
echo "===============================\n";
echo "📊 JSONBin URL: " . $jsonbin_url . "\n";
echo "🔑 Master Key: " . substr($jsonbin_master_key, 0, 20) . "...\n";
echo "🌐 Site URL: " . $site_url . "\n";
echo "📱 APK URL: " . $apk_url . "\n";
echo "===============================\n";
echo "✅ Configuração carregada com sucesso!\n";
?>
