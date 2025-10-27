FROM php:8.2-cli

WORKDIR /app

# Instalar curl (já vem instalado no php-cli)
RUN apt-get update && apt-get install -y --no-install-recommends libcurl4-openssl-dev && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copiar apenas arquivos PHP da raiz
COPY download.php index.php api.php ./

# Expor porta
EXPOSE 10000

# Comando para iniciar servidor PHP
CMD php -S 0.0.0.0:$PORT -t .

