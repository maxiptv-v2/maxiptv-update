# 🔧 Melhorias para v1.0.8 - Reduzir Travamentos Live

## 🎯 **Problema:**
Canais live estão travando muito durante reprodução.

## ✅ **Otimizações Planejadas:**

### 1. **Ajustar Buffer do ExoPlayer**
- ⬇️ Reduzir `minBufferMs` de 15s para **10s** (menos buffer inicial = mais rápido)
- ⬇️ Reduzir `maxBufferMs` de 50s para **30s** (menos memória = mais estável)  
- ⬇️ Reduzir `bufferForPlaybackMs` de 2.5s para **1.5s** (inicia mais rápido)
- ✅ Manter `bufferForPlaybackAfterRebufferMs` em 5s

### 2. **Melhorar Reconexão Automática**
- ✅ Reduzir delay de reconexão de 3s para **2s**
- ✅ Adicionar limite de tentativas (máximo 5 reconexões)
- ✅ Limpar buffer antes de reconectar

### 3. **Timeout mais agressivo**
- ⬇️ Reduzir `connectTimeoutMs` de 10s para **8s**
- ⬇️ Reduzir `readTimeoutMs` de 10s para **8s**

### 4. **Detectar travamento por inatividade**
- ✅ Adicionar listener para detectar quando video para de progredir
- ✅ Auto-reconectar se não houver progresso por 10 segundos

### 5. **Otimizar EmbeddedPlayer (HomeScreen)**
- ✅ Usar configurações ainda mais leves (menos buffer)
- ✅ Desabilitar cache no carrossel (são players temporários)

### 6. **Configuração de Rede**
- ✅ Adicionar retry automático em falhas de rede
- ✅ Melhorar headers HTTP para melhor compatibilidade

---

## 📊 **Resultado Esperado:**
- ⚡ Canais iniciam mais rápido
- 🔄 Reconexão mais ágil
- 📉 Menos travamentos
- 💾 Menos uso de memória

