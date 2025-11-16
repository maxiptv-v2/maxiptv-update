# 📺 GUIA: Configurar Emulador Android TV (Fire Stick HD)

## 🎯 OBJETIVO
Criar um emulador Android TV com perfil similar ao Fire Stick para testar layouts do app antes de instalar em dispositivos reais.

---

## 📋 PRÉ-REQUISITOS

1. **Android Studio instalado**
   - Download: https://developer.android.com/studio
   - Instale com Android SDK e Android Emulator

2. **Verificar instalação:**
   ```powershell
   # Verificar se Android SDK está instalado
   Test-Path "$env:LOCALAPPDATA\Android\Sdk"
   ```

---

## 🚀 MÉTODO 1: Via Android Studio (RECOMENDADO)

### Passo 1: Abrir Device Manager
1. Abra o **Android Studio**
2. Vá em: **Tools > Device Manager**
3. Clique em **"Create Device"** (ou botão **"+"** no canto superior)

### Passo 2: Escolher Categoria TV
1. Categoria: **TV**
2. Selecione: **"TV (1080p)"** ou **"TV (720p)"**
3. Clique **"Next"**

### Passo 3: Escolher System Image
1. Selecione: **API 33 (Android 13)** ou superior
2. Escolha: **Google APIs** (não AOSP)
3. Se não tiver, clique **"Download"** e aguarde
4. Clique **"Next"**

### Passo 4: Configurar AVD
1. **AVD Name:** `FireStick_HD_Test`
2. **Startup orientation:** Landscape
3. Clique **"Show Advanced Settings"**

### Passo 5: Configurações Avançadas (Opcional)
```
RAM: 2048 MB
VM heap: 256 MB
Internal Storage: 2048 MB
SD Card: 512 MB (opcional)
```

### Passo 6: Finalizar
1. Clique **"Finish"**
2. Aguarde criação do AVD

---

## 🚀 MÉTODO 2: Via Script PowerShell

### Executar Script:
```powershell
powershell -ExecutionPolicy Bypass -File .\criar-emulador-firestick-simples.ps1
```

### Opções do Script:
1. **Listar AVDs existentes** - Ver quais emuladores já estão criados
2. **Criar novo AVD** - Mostra instruções passo a passo
3. **Iniciar emulador existente** - Inicia o emulador padrão
4. **Iniciar em 720p** - Inicia com resolução 1280x720
5. **Iniciar em 1080p** - Inicia com resolução 1920x1080

---

## 🎮 INICIAR EMULADOR

### Opção 1: Via Android Studio
1. Abra **Device Manager**
2. Clique no botão **▶️ Play** ao lado do AVD `FireStick_HD_Test`

### Opção 2: Via Scripts Batch
```batch
# Iniciar em 720p
.\start-emulator-720p.bat

# Iniciar em 1080p
.\start-emulator-1080p.bat
```

### Opção 3: Via Linha de Comando
```powershell
# 720p
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd FireStick_HD_Test -skin 1280x720 -dpi-device 213

# 1080p
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd FireStick_HD_Test -skin 1920x1080 -dpi-device 320
```

---

## 📱 INSTALAR APP NO EMULADOR

### Método 1: Arrastar e Soltar
1. Inicie o emulador
2. Arraste o arquivo `maxiptv-release.apk` para a tela do emulador
3. Aguarde instalação

### Método 2: Via ADB
```powershell
# Conectar ao emulador
adb devices

# Instalar APK
adb install maxiptv-release.apk

# Ou instalar do diretório do projeto
adb install app\build\outputs\apk\release\app-release.apk
```

### Método 3: Via Android Studio
1. Abra o projeto no Android Studio
2. Clique em **Run** (▶️) ou pressione **Shift+F10**
3. Selecione o emulador `FireStick_HD_Test`
4. Aguarde instalação e execução

---

## 🧪 TESTAR LAYOUTS

### Checklist de Testes:
- [ ] **Home Screen** - Cards de categoria não estouram
- [ ] **Live Screen** - Nomes de canais não estouram
- [ ] **VOD Screen** - Cards de filmes não estouram
- [ ] **Series Screen** - Cards de séries não estouram
- [ ] **VOD Details** - Botões não estouram
- [ ] **Series Details** - Lista de episódios não estoura
- [ ] **Category Chips** - Scroll horizontal funciona
- [ ] **Textos longos** - Têm ellipsis quando necessário

### Resoluções para Testar:
1. **720p (1280x720)** - Simula Fire Stick básico
2. **1080p (1920x1080)** - Simula Fire Stick 4K

---

## 🔧 CONFIGURAÇÕES RECOMENDADAS DO AVD

### Para Simular Fire Stick:
```
Nome: FireStick_HD_Test
Categoria: TV
Resolução: 1920x1080 (ou 1280x720)
DPI: 320 (ou 213 para 720p)
RAM: 2048 MB
VM Heap: 256 MB
Android: 13 (API 33) ou superior
Google APIs: Sim
```

### Configurações Avançadas:
```
- Hardware Back/Home: Simulado
- D-Pad: Habilitado
- Keyboard: Habilitado
- Accelerometer: Simulado
- GPS: Simulado
```

---

## 📊 COMPARAR COM FIRE STICK REAL

### Fire Stick Real:
- Resolução: 720p ou 1080p
- DPI: ~213 (720p) ou ~320 (1080p)
- RAM: 1-2 GB
- Android: Fire OS (baseado em Android)

### Emulador:
- Resolução: Configurável (720p/1080p)
- DPI: Configurável (213/320)
- RAM: Configurável (recomendado 2GB)
- Android: Android TV (similar ao Fire OS)

---

## ✅ VERIFICAÇÃO

Após criar e iniciar o emulador:

1. **Verificar se está rodando:**
   ```powershell
   adb devices
   ```
   Deve mostrar: `emulator-5554 device`

2. **Verificar resolução:**
   ```powershell
   adb shell wm size
   ```
   Deve mostrar: `Physical size: 1920x1080` (ou 1280x720)

3. **Verificar DPI:**
   ```powershell
   adb shell wm density
   ```
   Deve mostrar: `Physical density: 320` (ou 213)

---

## 🐛 SOLUÇÃO DE PROBLEMAS

### Emulador não inicia:
- Verifique se HAXM está instalado (Intel) ou Hyper-V está desabilitado
- Aumente RAM alocada para o emulador
- Verifique se há espaço em disco suficiente

### App não instala:
- Verifique se o emulador está rodando (`adb devices`)
- Tente reinstalar: `adb install -r maxiptv-release.apk`
- Verifique se o APK está assinado corretamente

### Layouts ainda estouram:
- Verifique resolução do emulador (`adb shell wm size`)
- Teste em diferentes resoluções (720p e 1080p)
- Compare com dispositivo real

---

## 📝 NOTAS

- O emulador pode ser mais lento que dispositivo real
- Alguns comportamentos podem diferir do Fire Stick real
- Sempre teste em dispositivo real antes de release final
- Use o emulador principalmente para verificar layouts visuais

---

## 🎯 PRÓXIMOS PASSOS

Após configurar o emulador:
1. ✅ Instalar o app
2. ✅ Testar todas as telas
3. ✅ Verificar layouts em 720p e 1080p
4. ✅ Documentar problemas encontrados
5. ✅ Corrigir problemas encontrados
6. ✅ Testar novamente

