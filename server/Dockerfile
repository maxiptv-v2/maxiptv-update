FROM php:8.2-cli

WORKDIR /app

# Instalar curl (já vem instalado no php-cli)
RUN apt-get update && apt-get install -y --no-install-recommends libcurl4-openssl-dev && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copiar apenas arquivos PHP da raiz
COPY valida.php index.php api.php download.php ./

# Expor porta
EXPOSE 10000

# Comando para iniciar servidor PHP
# Render passa PORT como variável de ambiente, usar 10000 como fallback
# Usar -t . para servir da raiz do WORKDIR
CMD php -S 0.0.0.0:${PORT:-10000} -t .

