# Guia de Teste - Sinopse dos VODs

## Como usar o script de teste

O script `test-vod-synopsis.ps1` foi criado para testar e verificar onde a sinopse dos VODs está vindo da API Xtream Code.

### Execução básica:

```powershell
.\test-vod-synopsis.ps1
```

O script irá solicitar:
1. URL base da API (ex: `https://seu-servidor.com:porta`)
2. Username
3. Password

### Execução com parâmetros:

```powershell
.\test-vod-synopsis.ps1 -BaseUrl "https://seu-servidor.com:porta" -Username "seu_user" -Password "sua_senha"
```

### Execução com VOD ID específico:

```powershell
.\test-vod-synopsis.ps1 -BaseUrl "https://seu-servidor.com:porta" -Username "seu_user" -Password "sua_senha" -VodId 12345
```

## O que o script faz:

1. ✅ Testa a autenticação na API
2. 📺 Busca a lista de VODs disponíveis
3. 📋 Busca informações detalhadas de um VOD específico
4. 🔍 Analisa onde está o campo de sinopse:
   - `info.plot`
   - `info.Plot` (maiúscula)
   - `movie_data.plot`
   - `movie_data.description`
   - `movie_data.synopsis`
   - E outros campos possíveis
5. 📊 Mostra um resumo completo da estrutura da resposta

## Exemplo de saída:

```
========================================
  TESTE DE SINOPSE DOS VODs
========================================

🔍 Testando conexão com a API...
   ✅ Autenticação OK

📺 Buscando lista de VODs...
   ✅ Encontrados 1500 VODs
   📌 Usando VOD ID: 12345 (Nome do Filme)

📋 Buscando informações detalhadas do VOD...
   ✅ SINOPSE ENCONTRADA em info.plot!
   📖 Conteúdo: Esta é a sinopse do filme...
```

## Interpretação dos resultados:

- ✅ **SINOPSE ENCONTRADA**: O campo foi encontrado e o código atual deve funcionar
- ⚠️ **SINOPSE NÃO ENCONTRADA em info.plot**: A sinopse pode estar em `movie_data` ou com outro nome
- ❌ **SINOPSE NÃO ENCONTRADA**: A API não está retornando sinopse ou está em um campo não esperado

## Próximos passos:

Após executar o script, você saberá:
1. Se a sinopse está vindo da API
2. Onde exatamente ela está (info.plot, movie_data.plot, etc.)
3. Qual é o nome exato do campo
4. Se há algum problema na estrutura da resposta

Com essas informações, podemos ajustar o código do app para buscar a sinopse no lugar correto!

