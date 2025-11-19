# ✅ Correção: Travamentos em Canais Live

## 🔍 Problema Identificado

Os canais live estavam travando muito devido a:
1. **Buffers muito pequenos** - não havia buffer suficiente para compensar variações de conexão
2. **Low Latency muito agressivo** - tentando pegar segmentos muito recentes (0s offset)
3. **Timeouts muito curtos** - conexão e leitura com pouco tempo para estabilizar

## ✅ Correções Implementadas

### **1. Buffers Aumentados (PlayerUtils.kt)**

#### **ConnectionQuality.EXCELLENT:**
- `minBufferMs`: **5s → 8s** (+60%)
- `maxBufferMs`: **12s → 18s** (+50%)
- `bufferForPlaybackMs`: **1.5s → 3s** (+100%)
- `bufferForPlaybackAfterRebufferMs`: **3s → 5s** (+67%)
- `backBuffer`: **5s → 8s** (+60%)

#### **ConnectionQuality.GOOD:**
- `minBufferMs`: **5s → 10s** (+100%)
- `maxBufferMs`: **12s → 20s** (+67%)
- `bufferForPlaybackMs`: **1.5s → 4s** (+167%)
- `bufferForPlaybackAfterRebufferMs`: **3s → 6s** (+100%)
- `backBuffer`: **5s → 10s** (+100%)

#### **ConnectionQuality.POOR:**
- `minBufferMs`: **10s → 15s** (+50%)
- `maxBufferMs`: **20s → 30s** (+50%)
- `bufferForPlaybackMs`: **3s → 5s** (+67%)
- `bufferForPlaybackAfterRebufferMs`: **5s → 8s** (+60%)
- `backBuffer`: **10s → 15s** (+50%)

### **2. Timeouts Aumentados (PlayerActivity.kt)**

- `connectTimeout`: **8s → 12s** (+50%) - mais tempo para estabelecer conexão
- `readTimeout`: **10s → 15s** (+50%) - mais tempo para ler dados

### **3. Low Latency Menos Agressivo**

#### **Antes:**
- `targetOffsetMs`: 0s (muito agressivo)
- `minOffsetMs`: 0s (sem margem)
- `maxOffsetMs`: 3s (muito restritivo)
- `minPlaybackSpeed`: 0.98f (muito restritivo)
- `maxPlaybackSpeed`: 1.02f (muito restritivo)

#### **Depois:**
- `targetOffsetMs`: **0s → 2s** (mais estável)
- `minOffsetMs`: **0s → 1s** (margem de segurança)
- `maxOffsetMs`: **3s → 5s** (mais tolerante)
- `minPlaybackSpeed`: **0.98f → 0.95f** (mais tolerante)
- `maxPlaybackSpeed`: **1.02f → 1.05f** (mais tolerante)

### **4. Arquivos Modificados**

- ✅ `app/src/main/java/com/maxiptv/ui/player/PlayerUtils.kt`
  - Buffers aumentados para todas as qualidades de conexão
  
- ✅ `app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt`
  - Timeouts aumentados para canais live
  - Low Latency ajustado em 2 locais (onCreate e retryWithFailover)
  
- ✅ `app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt`
  - Low Latency ajustado em 2 locais (retryStream e LaunchedEffect)

## ✅ Resultado Esperado

- ✅ **Menos travamentos** - buffers maiores compensam variações de conexão
- ✅ **Mais estabilidade** - low latency menos agressivo evita tentar pegar segmentos muito recentes
- ✅ **Melhor tolerância** - velocidades de playback mais flexíveis (0.95x - 1.05x)
- ✅ **Conexões mais robustas** - timeouts maiores permitem conexões mais lentas estabilizarem

## 📊 Comparação de Buffers

| Qualidade | minBuffer | maxBuffer | bufferForPlayback | bufferAfterRebuffer |
|-----------|-----------|-----------|-------------------|---------------------|
| **EXCELLENT** (antes) | 5s | 12s | 1.5s | 3s |
| **EXCELLENT** (depois) | **8s** | **18s** | **3s** | **5s** |
| **GOOD** (antes) | 5s | 12s | 1.5s | 3s |
| **GOOD** (depois) | **10s** | **20s** | **4s** | **6s** |
| **POOR** (antes) | 10s | 20s | 3s | 5s |
| **POOR** (depois) | **15s** | **30s** | **5s** | **8s** |

## 🎯 Impacto

- **Buffer mínimo aumentado em 60-100%** - mais dados pré-carregados
- **Buffer máximo aumentado em 50-67%** - mais capacidade de buffer
- **Buffer para playback aumentado em 100-167%** - menos travamentos ao iniciar
- **Low Latency menos agressivo** - mais estabilidade, menos tentativas de pegar segmentos muito recentes
- **Timeouts aumentados em 50%** - mais tempo para conexões lentas estabilizarem

