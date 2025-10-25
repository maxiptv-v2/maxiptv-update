# MaxiPTV Downloader - Servidor PHP

## 📋 Descrição

Servidor PHP simples para validar códigos de download e redirecionar para o APK do GitHub.

## 🚀 Como Funciona

1. **Cliente digita código** de 4 dígitos no downloader
2. **Downloader acessa** `https://seudominio.com/server/download.php?code=XXXX`
3. **Servidor valida** código no JSONBin
4. **Se válido**, redireciona para download do GitHub
5. **Cliente baixa** APK com credenciais pré-configuradas

## 📁 Arquivos

- `download.php` - Servidor principal de download
- `test.php` - Teste do servidor
- `README.md` - Este arquivo

## ⚙️ Configuração

### 1. Hospedar em servidor PHP

- **000webhost** (gratuito)
- **InfinityFree** (gratuito)
- **Hostinger** (pago)
- **Qualquer servidor** com PHP

### 2. Configurar JSONBin

Edite `download.php` com suas credenciais:

```php
$jsonbin_url = "https://api.jsonbin.io/v3/b/SEU_BIN_ID";
$headers = ["X-Master-Key: SUA_API_KEY"];
```

### 3. Configurar GitHub

O APK URL é configurado automaticamente no app:
```php
$apkUrl = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/MaxiPTV-v1.0.92.apk";
```

## 🔧 Uso

### Exemplo de Código no JSONBin:

```json
{
  "7788": {
    "usuario": "joao",
    "senha": "1234",
    "api": "https://sualista.com/api",
    "apk": "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/MaxiPTV-v1.0.92.apk",
    "expira_em": "31/12/2024",
    "ativo": true
  }
}
```

### Exemplo de Uso:

1. Cliente digita `7788` no downloader
2. Downloader acessa `https://seudominio.com/server/download.php?code=7788`
3. Servidor valida e redireciona para GitHub
4. Cliente baixa APK configurado

## 🚨 Validações

- ✅ Código existe no JSONBin
- ✅ Código está ativo
- ✅ Conta não expirou
- ✅ Formato do código (4 dígitos)
- ✅ URL do APK configurada

## 📊 Logs

O servidor registra todas as atividades em `downloads.log`:

```
2024-10-25 17:30:15 - Código 7788 usado por joao - IP: 192.168.1.100
```

## 🔐 Segurança

- Validação de entrada
- Verificação de expiração
- Logs de atividade
- Headers de segurança

## 🧪 Teste

Acesse `test.php` para verificar se tudo está funcionando:

- Conectividade JSONBin
- Conectividade GitHub
- Permissões de arquivo
- Códigos disponíveis

## 📞 Suporte

- **Email**: contato@maxiptv.com
- **Documentação**: [Link para docs]

---

**Versão**: 1.0.92  
**Última Atualização**: 25/10/2024  
**Compatibilidade**: PHP 7.4+
