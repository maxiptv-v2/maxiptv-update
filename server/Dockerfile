FROM php:8.2-cli

WORKDIR /app

COPY *.php ./

EXPOSE 10000

CMD php -S 0.0.0.0:$PORT -t .

