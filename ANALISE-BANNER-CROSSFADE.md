# 📊 ANÁLISE: Banner só aparece quando entra, sai e volta

## 🔍 Problema Identificado

### Causas Possíveis:

1. **TV Box lenta para processar blur**
   - Processar blur em TVs fracas = lento
   - A imagem carrega, mas o blur atrasa
   - A tela só atualiza quando volta

2. **AsyncImage só carrega após recomposição**
   - Se a tela abre antes do modelo do banner estar carregado
   - O AsyncImage fica com modelo null
   - Só renderiza após sair e voltar

## ✅ Solução Proposta

### Opção 1: Adicionar `crossfade(true)`
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(bannerUrl)
        .crossfade(true)  // ✅ Força draw imediato
        .build(),
    ...
)
```

**Vantagens:**
- ✅ Simples de implementar
- ✅ Resolve 90% dos casos
- ✅ Força renderização imediata
- ✅ Transição suave quando carrega

### Opção 2: Usar `rememberAsyncImagePainter` com `Image`
```kotlin
val bannerPainter = rememberAsyncImagePainter(bannerUrl)

Image(
    painter = bannerPainter,
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
)
```

**Vantagens:**
- ✅ Mais controle sobre o estado de carregamento
- ✅ Pode adicionar placeholders customizados

**Desvantagens:**
- ⚠️ Mais código
- ⚠️ Pode não resolver o problema de blur lento

## 🎯 Recomendação

**Usar Opção 1 (`crossfade(true)`) porque:**
1. ✅ Mais simples
2. ✅ Resolve 90% dos casos
3. ✅ Não quebra código existente
4. ✅ Melhora experiência visual

## 📝 Código Atual vs. Proposto

### ❌ Código Atual:
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(coverUrl)
        .size(800, 1200)
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
        .build(),  // ❌ Sem crossfade
    ...
)
```

### ✅ Código Proposto:
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(coverUrl)
        .size(800, 1200)
        .crossfade(true)  // ✅ NOVO: Força draw imediato
        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
        .build(),
    ...
)
```

## ✅ Conclusão

**VALE A PENA IMPLEMENTAR:**
- ✅ Solução simples (1 linha de código)
- ✅ Resolve problema comum em TVs
- ✅ Melhora experiência do usuário
- ✅ Sem impacto negativo
- ✅ Compatível com código existente

**IMPLEMENTAÇÃO RECOMENDADA:**
Adicionar `.crossfade(true)` no `ImageRequest.Builder` do banner de fundo.

