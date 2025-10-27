# MaxiPTV Downloader - Render.com

Servidor PHP para validação de códigos e download de APK.

## Configuração no Render

1. **Environment**: PHP
2. **Build Command**: (deixar vazio)
3. **Start Command**: `php -S 0.0.0.0:$PORT -t .`
4. **Root Directory**: `server`

## Arquivos

- `download.php` - Valida código e redireciona para APK do GitHub
- `api.php` - Retorna JSON com dados do código
- `index.php` - Roteador principal

## Uso

Cliente digita código de 4 dígitos no Downloader Android.
Servidor valida no JSONBin e redireciona para GitHub.
