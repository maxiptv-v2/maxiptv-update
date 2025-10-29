# 🚀 Como Configurar o Render.com - Passo a Passo

## 0️⃣ Se você já tem um serviço antigo

**RECOMENDADO:** Deletar o serviço antigo e criar um novo para evitar problemas de configuração!

1. Vá no serviço antigo no Render
2. Clique em **Settings** → role até o final
3. Clique em **"Delete Service"**
4. Confirme a deleção
5. Continue para criar um novo serviço abaixo

## 1️⃣ Criar Novo Serviço

1. Clique em **"New +"** → **"Web Service"**
2. Conecte seu repositório GitHub:
   - Procure por: `maxiptv-v2/maxiptv-update`
   - Clique em **"Connect"**

## 2️⃣ Configurações do Serviço

Na página de configuração do serviço, preencha:

### Name:
```
maxiptv-update
```

### Environment:
```
Docker
```

### Root Directory:
```
server
```
**IMPORTANTE:** 
- Deve ser exatamente `server` (sem barra no final)
- Isso indica que os arquivos estão na pasta `server/` do repositório

### Docker Build Context Directory:
```
server
```
**IMPORTANTE:** 
- Deve ser exatamente `server` (sem barra no final)
- Isso faz o Docker construir a partir da pasta `server/`

### Dockerfile Path:
```
(deixe VAZIO - em branco)
```
**IMPORTANTE:** 
- **DEIXE VAZIO!** Não preencha nada aqui
- O Render vai procurar automaticamente por `Dockerfile` na pasta `server/`
- Se você colocar algo aqui, pode dar erro

### Build Command:
```
(deixe VAZIO - não precisa colocar nada aqui!)
```

### Start Command:
```
(deixe VAZIO também!)
```

### Environment Variables (adicionar):
Clique em **"Add Environment Variable"** e adicione:
- **Key:** `PORT`
- **Value:** `10000`

## 3️⃣ Deploy

1. Clique em **"Create Web Service"**
2. Aguarde o deploy (pode levar 2-5 minutos)
3. Quando aparecer "Live", copie a URL (exemplo: `https://maxiptv-downloader-xxxx.onrender.com`)

## 4️⃣ Atualizar o App Android

Depois que pegar a URL do Render.com, me envie aqui para eu atualizar o código do app.

