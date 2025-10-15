# 🔄 COMO CONTINUAR O TRABALHO DEPOIS

Este documento te ajuda a retomar o desenvolvimento do **MaxiPTV v2** em qualquer momento.

---

## 📍 ONDE ESTAMOS

**Status Atual:** ✅ **FUNCIONANDO**  
**Última Build:** 11 de Outubro de 2025  
**APK:** `app/build/outputs/apk/debug/app-debug.apk`  
**Projeto:** `C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2`  
**Backup:** `C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2_BACKUP_2025-10-10_223227`

### ✅ O que JÁ funciona:
- Cache inteligente (24h)
- Detecção de idiomas (Legendado/Dublado)
- Categorias aparecendo corretamente
- Player com fullscreen (2 cliques)
- Banners bonitos em grid
- Séries com seletor de temporadas
- Logos coloridas em Live
- Deduplicação de filmes/séries

---

## 🚀 PARA RETOMAR O DESENVOLVIMENTO

### **1. Abrir o Projeto**
```bash
# Pasta:
C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2

# Abrir no Android Studio:
- File > Open
- Selecionar pasta MaxiPTV_v2
- Esperar indexação
```

### **2. Verificar que está tudo OK**
```bash
# No PowerShell:
cd C:\Users\maxca\OneDrive\Desktop\MaxiPTV_v2
.\gradlew clean build
```

### **3. Testar no Emulador**
```bash
# Abrir emulador:
emulator -avd <NOME_DO_EMULADOR>

# Instalar APK:
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk

# Ver logs:
& $adb logcat | Select-String "XRepo|MaxiPTV"
```

---

## 📋 O QUE PRECISA TESTAR NA TV REAL

### **Testes Prioritários:**
1. ✅ Cache funciona?
2. ✅ Categorias aparecem?
3. ✅ Idiomas detectados corretamente?
4. ✅ Player funciona?
5. ✅ Fullscreen com 2 cliques?
6. ✅ Controle remoto reconhecido?
7. ✅ Banners carregam rápido?
8. ✅ Series mostram todas temporadas?

### **Como Testar:**
```
1. Copiar app-debug.apk para pen drive
2. Inserir na TV Box
3. Instalar com gerenciador de arquivos
4. Abrir app
5. Testar cada funcionalidade acima
```

---

## 🐛 SE ENCONTRAR PROBLEMAS

### **Problema 1: App não abre**
```bash
# Ver erro:
& $adb logcat | Select-String "FATAL|AndroidRuntime"

# Limpar cache do app:
& $adb shell pm clear com.maxiptv
```

### **Problema 2: Categorias não aparecem**
```bash
# Ver logs de cache:
& $adb logcat | Select-String "XRepo|Cache|categorias"

# Força buscar da API (limpar cache):
& $adb shell pm clear com.maxiptv
```

### **Problema 3: Player não reproduz**
```bash
# Ver logs do ExoPlayer:
& $adb logcat | Select-String "ExoPlayer|PlayerActivity"

# Verificar URL:
& $adb logcat | Select-String "REPRODUZINDO|URL"
```

### **Problema 4: Idiomas errados**
```bash
# Ver detecção de idiomas:
& $adb logcat | Select-String "availableLanguages|VodDetails"
```

---

## 🔧 ARQUIVOS IMPORTANTES

### **Backend/Lógica:**
- `app/src/main/java/com/maxiptv/data/Repo.kt` - Busca API e cache
- `app/src/main/java/com/maxiptv/data/CacheManager.kt` - Gerencia cache
- `app/src/main/java/com/maxiptv/data/Models.kt` - Modelos de dados

### **Telas/UI:**
- `app/src/main/java/com/maxiptv/ui/screens/VodScreen.kt` - Lista de filmes
- `app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt` - Detalhes do filme
- `app/src/main/java/com/maxiptv/ui/screens/SeriesScreen.kt` - Lista de séries
- `app/src/main/java/com/maxiptv/ui/screens/SeriesDetailsScreen.kt` - Detalhes da série
- `app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt` - Canais LIVE

### **Player:**
- `app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt` - Reprodutor

### **Configuração:**
- `app/build.gradle.kts` - Dependências
- `app/src/main/AndroidManifest.xml` - Permissões

---

## 💡 PRÓXIMAS FUNCIONALIDADES A IMPLEMENTAR

### **Fácil (1-2 horas cada):**
1. **Favoritos**
   - Adicionar botão ❤️ nos detalhes
   - Salvar IDs no DataStore
   - Criar tela "Meus Favoritos"

2. **Histórico**
   - Salvar filme/série assistido
   - Mostrar "Continuar assistindo"

3. **Busca melhorada**
   - Adicionar filtro por ano, gênero
   - Ordenação (A-Z, recentes, avaliação)

### **Médio (3-5 horas cada):**
1. **Download offline**
   - Baixar filme para assistir sem internet
   - Progress bar de download

2. **Perfis de usuário**
   - Criar múltiplos perfis
   - Cada um com histórico próprio

3. **Recomendações**
   - "Filmes similares"
   - "Baseado no que você assistiu"

### **Avançado (1-2 dias cada):**
1. **Chromecast**
   - Enviar vídeo para TV
   - Controle remoto via celular

2. **Picture-in-Picture**
   - Assistir em janela flutuante
   - Continuar navegando no app

3. **Sync entre dispositivos**
   - Começar no celular, continuar na TV
   - Firebase Realtime Database

---

## 📚 DOCUMENTAÇÃO ÚTIL

### **APIs:**
- Xtream Codes: Já implementado
- TMDB (para metadados extras): https://www.themoviedb.org/documentation/api

### **Bibliotecas:**
- ExoPlayer: https://exoplayer.dev/
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Retrofit: https://square.github.io/retrofit/
- Coil: https://coil-kt.github.io/coil/

### **Android TV:**
- Leanback Library: https://developer.android.com/training/tv
- TV Input Framework: https://developer.android.com/training/tv/tif

---

## 🎯 CHECKLIST ANTES DE PUBLICAR

### **Quando estiver pronto para lançar:**

- [ ] Testar em pelo menos 3 dispositivos diferentes
- [ ] Testar em TV Box, Fire Stick, Smartphone
- [ ] Verificar todas as categorias (LIVE, VOD, Series)
- [ ] Testar reprodução de pelo menos 10 filmes/séries
- [ ] Verificar se legendas funcionam
- [ ] Testar fullscreen com controle remoto
- [ ] Verificar performance (sem lag)
- [ ] Criar ícone bonito (512x512 PNG)
- [ ] Mudar `versionCode` e `versionName` em `build.gradle.kts`
- [ ] Gerar APK release (assinado):
  ```bash
  .\gradlew assembleRelease
  ```
- [ ] Testar APK release antes de distribuir

---

## 🔐 CREDENCIAIS E CONFIGURAÇÃO

**API Xtream padrão:**
```kotlin
// Em: app/build.gradle.kts
buildConfigField("String", "DEFAULT_PLAYER_API", "\"https://canais.is/\"")
buildConfigField("String", "DEFAULT_USERNAME", "\"max\"")
buildConfigField("String", "DEFAULT_PASSWORD", "\"1h2yd90\"")
```

**Para trocar:**
1. Editar `app/build.gradle.kts`
2. Recompilar: `.\gradlew clean build`

---

## 📧 SUPORTE

**Se precisar de ajuda:**
1. Ler `RESUMO_ALTERACOES.md` (tem tudo detalhado)
2. Ver logs no Android Studio
3. Pesquisar erro no Google/Stack Overflow
4. Documentação oficial do Android

---

## 💾 BACKUP

**Fazer backup antes de grandes mudanças:**
```powershell
# Executar:
.\FAZER_BACKUP.ps1

# Salvar ZIP em:
- Pen drive
- HD externo
- Google Drive
- OneDrive (já tem backup automático)
```

---

## 🎉 BOA SORTE!

O projeto está **100% funcional** e pronto para testar na TV real.  
Todos os arquivos estão salvos no OneDrive (backup automático).  
APK está em `app/build/outputs/apk/debug/app-debug.apk`.

**Qualquer dúvida, leia os documentos:**
- `RESUMO_ALTERACOES.md` - O que foi feito
- `COMO_CONTINUAR.md` - Este arquivo
- `FAZER_BACKUP.ps1` - Script de backup

**Sucesso! 🚀**

