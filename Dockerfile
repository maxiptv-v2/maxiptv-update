FROM php:8.3-cli

WORKDIR /app

# Copiar arquivos PHP
COPY *.php ./

# Instalar extensões necessárias
RUN docker-php-ext-install pdo pdo_mysql curl

# Expor porta
EXPOSE 8080

# Comando para iniciar servidor PHP
CMD ["php", "-S", "0.0.0.0:8080", "-t", "."]

