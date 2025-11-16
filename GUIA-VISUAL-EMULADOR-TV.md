# 📺 GUIA VISUAL: Criar Emulador Android TV (Fire Stick HD)

## ⚠️ IMPORTANTE: Escolher CATEGORIA "TV" (NÃO Phone!)

---

## 📋 PASSO A PASSO COM IMAGENS

### PASSO 1: Abrir Device Manager
1. Abra o **Android Studio**
2. No menu superior, clique em **"Tools"**
3. Clique em **"Device Manager"**

### PASSO 2: Criar Novo Dispositivo
1. Na janela do Device Manager, clique no botão **"Create Device"** (ou botão **"+"** no canto superior esquerdo)

### PASSO 3: ⚠️ ESCOLHER CATEGORIA "TV" (CRÍTICO!)
Na tela de seleção de dispositivo:

**❌ NÃO ESCOLHA:**
- ❌ Phone
- ❌ Tablet
- ❌ Wear OS
- ❌ Automotive

**✅ ESCOLHA:**
- ✅ **TV** ← ESTE É O CORRETO!

Depois de escolher **TV**, você verá opções como:
- TV (1080p) ← **RECOMENDADO**
- TV (720p)
- TV (4K)

**Selecione: "TV (1080p)"** ou **"TV (720p)"**

### PASSO 4: Escolher System Image
1. Clique em **"Next"**
2. Na tela de System Image:
   - Escolha **"API 33"** ou superior (recomendado: API 33)
   - **IMPORTANTE:** Escolha **"Google APIs"** (não "AOSP")
   - Se não tiver, clique em **"Download"** ao lado e aguarde

### PASSO 5: Configurar AVD
1. Clique em **"Next"**
2. **AVD Name:** Digite: `FireStick_HD_Test`
3. (Opcional) Clique em **"Show Advanced Settings"**:
   - **RAM:** 2048 MB
   - **VM heap:** 256 MB
   - **Internal Storage:** 2048 MB

### PASSO 6: Finalizar
1. Clique em **"Finish"**
2. Aguarde a criação do AVD (pode demorar alguns segundos)

---

## ✅ VERIFICAR SE FOI CRIADO CORRETAMENTE

Após criar, você deve ver na lista do Device Manager:
- ✅ **FireStick_HD_Test**
- ✅ Categoria: **TV**
- ✅ Resolução: **1920x1080** (ou 1280x720)

---

## 🚀 INICIAR EMULADOR

### Opção 1: Via Android Studio
1. No Device Manager, encontre **"FireStick_HD_Test"**
2. Clique no botão **▶️ Play** ao lado

### Opção 2: Via Scripts Batch
```batch
# Iniciar em 1080p (Full HD)
.\start-emulator-1080p.bat

# OU iniciar em 720p (HD)
.\start-emulator-720p.bat
```

---

## 📱 INSTALAR APP NO EMULADOR

### Método 1: Arrastar e Soltar (Mais Fácil)
1. Inicie o emulador
2. Arraste o arquivo `maxiptv-release.apk` para a tela do emulador
3. Aguarde a instalação

### Método 2: Via ADB
```powershell
# Verificar se emulador está rodando
adb devices

# Instalar APK
adb install maxiptv-release.apk
```

---

## 🧪 TESTAR LAYOUTS

Após instalar o app, teste:

### ✅ Checklist de Testes:
- [ ] **Home Screen** - Cards de categoria não estouram
- [ ] **Live Screen** - Nomes de canais não estouram
- [ ] **VOD Screen** - Cards de filmes não estouram
- [ ] **Series Screen** - Cards de séries não estouram
- [ ] **VOD Details** - Botões não estouram
- [ ] **Series Details** - Lista de episódios não estoura
- [ ] **Category Chips** - Scroll horizontal funciona
- [ ] **Textos longos** - Têm ellipsis quando necessário

### Testar em Diferentes Resoluções:
1. **720p (1280x720)** - Simula Fire Stick básico
2. **1080p (1920x1080)** - Simula Fire Stick 4K

---

## 🔍 VERIFICAR SE É TV (NÃO Smartphone)

### Sinais de que está correto (TV):
- ✅ Interface mostra controles de TV (D-PAD)
- ✅ Não tem teclado virtual na tela
- ✅ Resolução é 1920x1080 ou 1280x720
- ✅ Orientação é landscape (paisagem)

### Sinais de que está ERRADO (Smartphone):
- ❌ Interface mostra teclado virtual
- ❌ Resolução é pequena (ex: 1080x1920)
- ❌ Orientação é portrait (retrato)
- ❌ Tem botões de navegação na parte inferior

---

## 🐛 PROBLEMAS COMUNS

### Problema: Não vejo opção "TV" na lista
**Solução:** 
- Verifique se tem Android TV System Image instalado
- Vá em: Tools > SDK Manager > SDK Platforms
- Marque "Android 13" ou superior
- Na aba "SDK Tools", marque "Android TV Intel x86 Atom System Image"

### Problema: Emulador não inicia
**Solução:**
- Verifique se HAXM está instalado (Intel) ou Hyper-V está desabilitado
- Aumente RAM alocada para o emulador
- Verifique espaço em disco

### Problema: App não instala
**Solução:**
- Verifique se emulador está rodando: `adb devices`
- Tente reinstalar: `adb install -r maxiptv-release.apk`

---

## 📝 NOTAS

- O emulador pode ser mais lento que dispositivo real
- Alguns comportamentos podem diferir do Fire Stick real
- Sempre teste em dispositivo real antes de release final
- Use o emulador principalmente para verificar layouts visuais

---

## ✅ PRONTO!

Após seguir estes passos, você terá um emulador Android TV configurado para testar layouts do app antes de instalar em dispositivos reais!

