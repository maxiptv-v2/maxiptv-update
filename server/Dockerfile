FROM php:8.2-cli

WORKDIR /app

# Instalar curl (já vem instalado no php-cli)
RUN apt-get update && apt-get install -y --no-install-recommends libcurl4-openssl-dev && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copiar apenas arquivos PHP essenciais que estão no repositório
COPY valida.php index.php api.php download.php router.php index.html register-aftvnews.php dl.php get-pending-code.php auto_login.php ./

# Expor porta
EXPOSE 10000

# Comando para iniciar servidor PHP com router
# Render passa PORT como variável de ambiente, usar 10000 como fallback
# router.php processa URLs como /6789 e redireciona para download.php?code=6789
CMD sh -c "php -S 0.0.0.0:\${PORT:-10000} -t . router.php"

