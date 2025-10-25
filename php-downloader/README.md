# MaxiPTV Downloader - Sistema de Download Automático

## 📋 Descrição

Sistema PHP completo para download automático do app MaxiPTV com login automático baseado em códigos únicos gerados pelo painel administrativo.

## 🚀 Funcionalidades

- ✅ **Interface Web Profissional** - Interface moderna e responsiva
- ✅ **Validação de Códigos** - Sistema seguro de validação via JSONBin
- ✅ **Download Automático** - Download direto do GitHub com configurações
- ✅ **Login Automático** - App configurado com credenciais do cliente
- ✅ **Controle de Expiração** - Códigos válidos por 6 horas
- ✅ **Uso Único** - Cada código só pode ser usado uma vez
- ✅ **Logs Completos** - Sistema de logging para monitoramento
- ✅ **Segurança** - Headers de segurança e validações

## 📁 Estrutura de Arquivos

```
php-downloader/
├── index.php          # Interface principal
├── download.php       # Página de download
├── validate.php       # API de validação
├── config.php         # Configurações centralizadas
├── README.md          # Este arquivo
├── logs/              # Pasta de logs (criar manualmente)
├── temp/              # Pasta temporária (criar manualmente)
├── cache/             # Pasta de cache (criar manualmente)
└── backups/           # Pasta de backups (criar manualmente)
```

## ⚙️ Configuração

### 1. Configurar Servidor Web

- **Apache/Nginx** com PHP 7.4+
- **Extensões PHP**: cURL, JSON, OpenSSL
- **Permissões**: Pasta com permissão de escrita

### 2. Configurar JSONBin

Edite `config.php` com suas credenciais:

```php
'jsonbin' => [
    'api_key' => 'SUA_API_KEY_AQUI',
    'bin_id' => 'SEU_BIN_ID_AQUI',
    // ...
]
```

### 3. Configurar GitHub

Edite `config.php` com seu repositório:

```php
'github' => [
    'repo' => 'https://github.com/SEU-USUARIO/MaxiPTV_v2',
    'releases_url' => 'https://api.github.com/repos/SEU-USUARIO/MaxiPTV_v2/releases/latest',
    // ...
]
```

### 4. Criar Pastas Necessárias

```bash
mkdir logs temp cache backups
chmod 755 logs temp cache backups
```

## 🔧 Como Funciona

### 1. Geração de Código (Admin Panel)

1. Admin acessa o painel oculto
2. Clica em "Gerar Código" para um cliente
3. Sistema gera código único (ex: `MAXI_ABC123XYZ`)
4. Código é salvo no JSONBin com dados do cliente
5. Código expira em 6 horas

### 2. Download pelo Cliente

1. Cliente acessa `https://seudominio.com/php-downloader/`
2. Digita o código PHP recebido
3. Sistema valida código no JSONBin
4. Se válido, redireciona para download
5. Código é marcado como "usado"
6. Cliente baixa APK configurado

### 3. Login Automático

1. Cliente instala o APK
2. App detecta configurações automáticas
3. Login é feito automaticamente
4. Cliente vai direto para a tela inicial

## 🔐 Segurança

- **Headers de Segurança**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection
- **Validação de Entrada**: Sanitização de todos os inputs
- **Rate Limiting**: Controle de tentativas por IP
- **Códigos Únicos**: Cada código só pode ser usado uma vez
- **Expiração**: Códigos expiram em 6 horas
- **Logs**: Registro de todas as atividades

## 📊 Monitoramento

### Logs Disponíveis

- **downloads.log**: Registro de downloads
- **errors.log**: Registro de erros
- **security.log**: Registro de tentativas de acesso
- **performance.log**: Métricas de performance

### Métricas Importantes

- Downloads por hora/dia
- Códigos gerados vs utilizados
- Taxa de erro
- Tempo de resposta

## 🚨 Troubleshooting

### Problemas Comuns

1. **Erro de Conexão com JSONBin**
   - Verificar API key e Bin ID
   - Verificar conectividade de rede
   - Verificar limites da API

2. **Download Não Funciona**
   - Verificar URL do GitHub
   - Verificar permissões de pasta
   - Verificar logs de erro

3. **Código Inválido**
   - Verificar se código não expirou
   - Verificar se não foi usado
   - Verificar se conta não expirou

### Logs de Debug

Ativar debug em `config.php`:

```php
'development' => [
    'debug' => true,
    'show_errors' => true,
    // ...
]
```

## 🔄 Atualizações

### Atualizar App

1. Fazer push da nova versão para GitHub
2. Atualizar `version` em `config.php`
3. Sistema automaticamente serve nova versão

### Atualizar Downloader

1. Fazer backup da configuração
2. Atualizar arquivos PHP
3. Restaurar configuração
4. Testar funcionamento

## 📞 Suporte

- **Email**: contato@maxiptv.com
- **Documentação**: [Link para docs]
- **Issues**: [Link para GitHub Issues]

## 📄 Licença

© 2024 MaxiPTV - Sistema Profissional
Todos os direitos reservados.

---

**Versão**: 1.0.89  
**Última Atualização**: 25/10/2024  
**Compatibilidade**: PHP 7.4+, Apache/Nginx
