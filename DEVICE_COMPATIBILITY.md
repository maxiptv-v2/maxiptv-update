# 📱 Compatibilidade Universal - MaxiPTV

## ✅ Dispositivos Suportados

### 📺 **TV Box / Android TV**
- ✅ TV Box genéricas (qualquer fabricante)
- ✅ Xiaomi Mi Box
- ✅ Mecool, X96, T95, H96
- ✅ Sony Bravia (Android TV)
- ✅ Philips Android TV
- ✅ TCL Android TV
- ✅ NVIDIA Shield

### 🔥 **Amazon Fire TV**
- ✅ Fire TV Stick (todas gerações)
- ✅ Fire TV Stick 4K
- ✅ Fire TV Cube

### 📺 **Chromecast**
- ✅ Chromecast com Google TV (HD)
- ✅ Chromecast com Google TV (4K)

### 📱 **Smartphones**
- ✅ Qualquer Android 7.0+ (API 24+)
- ✅ Telas pequenas (até 600dp)

### 📱 **Tablets**
- ✅ Tablets Android (600dp ou maior)
- ✅ Samsung Tab, Lenovo Tab, etc.

---

## 🔧 Recursos Implementados

### 1️⃣ **AndroidManifest.xml**
```xml
✅ android:banner (launcher TV)
✅ LEANBACK_LAUNCHER (intent-filter TV)
✅ android.software.leanback (required=false)
✅ android.hardware.touchscreen (required=false)
✅ android.hardware.faketouch (required=false)
✅ android.hardware.gamepad (required=false)
✅ android.hardware.type.television (required=false)
✅ android.max_aspect (2.4 para telas 21:9)
✅ hardwareAccelerated (true)
✅ largeHeap (true para cache)
```

### 2️⃣ **Recursos por Dispositivo**

#### 📱 **Smartphone/Tablet** (`values/`)
```
tv_player_height: 220dp
tv_carousel_height: 280dp
text_title: 18sp
text_body: 14sp
padding_screen: 16dp
```

#### 📺 **TV Box/Fire Stick/Chromecast** (`values-television/`)
```
tv_player_height: 380dp (73% maior)
tv_carousel_height: 480dp (71% maior)
text_title: 28sp (56% maior)
text_body: 20sp (43% maior)
padding_screen: 32dp (100% maior - overscan)
```

### 3️⃣ **DensityNormalizer.kt** (Compose)
- ✅ Detecção automática de dispositivo
- ✅ Ajuste de densidade (density * 0.85 para TV)
- ✅ Ajuste de escala de fonte (fontScale * 1.15 para TV)
- ✅ Suporte a 10-foot UI (visualização a distância)
- ✅ Logs detalhados para debug

### 4️⃣ **MaxiApp.kt** (Detecção Global)
- ✅ Detecta `UI_MODE_TYPE_TELEVISION`
- ✅ Detecta Fire Stick (manufacturer/model/product)
- ✅ Detecta Chromecast (model/product)
- ✅ Detecta Android TV (brand/manufacturer)
- ✅ Logs completos no Logcat

---

## 🧪 Como Verificar

### **No Logcat:**
```
═══════════════════════════════════════
📱 DETECÇÃO DE DISPOSITIVO
Fabricante: amazon
Modelo: fire tv stick 4k
Marca: amazon
Produto: mantis
UI Mode: TELEVISION
Largura: 960dp
Altura: 540dp
───────────────────────────────────────
✅ Tipo detectado: Fire Stick
═══════════════════════════════════════
```

### **Teste Rápido:**
1. Instale o APK no dispositivo
2. Abra o Logcat (`adb logcat | grep MaxiApp`)
3. Verifique o tipo detectado
4. Confira se a UI está com dimensões adequadas

---

## 📦 Arquivos Criados/Modificados

### **Novos:**
- ✅ `app/src/main/res/values/dimens.xml`
- ✅ `app/src/main/res/values-television/dimens.xml`
- ✅ `app/src/main/java/com/maxiptv/ui/tv/DensityNormalizer.kt`

### **Modificados:**
- ✅ `app/src/main/AndroidManifest.xml`
- ✅ `app/src/main/java/com/maxiptv/MaxiApp.kt`
- ✅ `app/src/main/java/com/maxiptv/MainActivity.kt`

---

## 🎯 Próximos Passos

1. ✅ Testar no Fire Stick
2. ✅ Testar em TV Box genérica
3. ✅ Testar no Smartphone
4. ✅ Verificar logs de detecção
5. ✅ Ajustar dimensões se necessário

---

**Última Atualização:** Outubro 2025  
**Versão Mínima:** Android 7.0 (API 24)  
**Versão Alvo:** Android 14 (API 34)

