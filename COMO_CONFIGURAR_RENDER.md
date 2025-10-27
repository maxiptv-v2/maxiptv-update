# 🚀 Como Configurar o Render.com - Passo a Passo

## 1️⃣ Criar Novo Serviço

1. Clique em **"Create new project"** (ou clique no projeto "My project")
2. Selecione **"Web Service"**
3. Conecte seu repositório GitHub:
   - Procure por: `maxiptv-v2/maxiptv-update`
   - Clique em **"Connect"**

## 2️⃣ Configurações do Serviço

Na página de configuração do serviço, preencha:

### Name:
```
maxiptv-downloader
```

### Environment:
```
Docker
```

### Root Directory:
```
. (deixe vazio ou ponha um ponto)
```

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

