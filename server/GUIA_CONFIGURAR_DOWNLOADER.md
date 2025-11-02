# 📱 Guia: Como Configurar URL no Downloader

## ⚠️ Importante

O **Downloader by AFTVnews** **NÃO possui configuração de URL base** no aplicativo. Ele funciona de duas formas:

1. **Digitar URL completa** (sempre funciona)
2. **Usar códigos do AFTVnews** (aftv.news/CODIGO)

## ✅ OPÇÃO 1: Usar URL Completa (Recomendado)

Esta é a forma mais simples e funciona 100% das vezes:

### Passos:

1. Abra o **Downloader by AFTVnews** no dispositivo
2. No campo de URL, digite:
   ```
   https://maxiptv-update-1.onrender.com/dl/6789
   ```
   (Substitua `6789` pelo código fornecido pelo admin)
3. Clique em **"Go"** ou pressione **Enter**
4. O download começará automaticamente

### Vantagens:
- ✅ Funciona sempre
- ✅ Não precisa configurar nada
- ✅ Simples e direto

---

## 🔄 OPÇÃO 2: Criar Encurtador Próprio (Para Funcionar Apenas com Código)

Para fazer funcionar **apenas digitando o código** (como Nidev), você precisa criar um domínio/encurtador próprio:

### Como Fazer:

1. **Registrar domínio curto** (opcional):
   - Exemplo: `dl.meuapp.com`
   - Configurar DNS para apontar para `maxiptv-update-1.onrender.com`

2. **Ou usar subdomínio do Render** (se disponível):
   - Render permite domínios customizados
   - Criar `dl.seudominio.com` apontando para o serviço

3. **Configurar cliente para usar domínio curto:**
   - Cliente digita: `dl.meuapp.com/6789`
   - Funciona igual ao Nidev

### Vantagens:
- ✅ URL mais curta e fácil
- ✅ Mais profissional
- ✅ Cliente lembra mais fácil

---

## 📋 OPÇÃO 3: Instruir Cliente a Usar URL Completa

Criar uma página simples com instruções:

### Página de Instruções:

1. Cliente acessa: `https://maxiptv-update-1.onrender.com`
2. Vê página bonita com campo para digitar código
3. Digita código e clica "Baixar"
4. Redireciona automaticamente

**✅ Esta página já está criada!** (`index.html`)

---

## 🎯 Recomendação Final

**Para máxima compatibilidade, use:**
- **URL completa** quando possível
- **Página HTML** (`maxiptv-update-1.onrender.com`) para facilitar
- **Domínio curto** se quiser parecer mais profissional (opcional)

---

## 📝 Exemplo de Instruções para Cliente

Envie isso para seus clientes:

```
📱 COMO BAIXAR O APK:

Opção 1 (Mais Fácil):
1. Abra o Downloader
2. Digite na URL:
   https://maxiptv-update-1.onrender.com/dl/6789
   (substitua 6789 pelo seu código)
3. Clique em "Go"
4. O download começará automaticamente

Opção 2 (Página Simples):
1. Abra o Downloader
2. Digite na URL:
   https://maxiptv-update-1.onrender.com
3. Digite seu código no campo
4. Clique em "Baixar APK"
```

---

**Nota:** O Downloader não possui configuração de "URL base" como um navegador normal. Por isso, sempre será necessário usar URL completa ou a página HTML que criamos.

