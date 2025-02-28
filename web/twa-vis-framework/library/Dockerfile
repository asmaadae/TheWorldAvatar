#
# First stage is to build the TWA-VF.
#
FROM ubuntu:23.10 AS build

# Install latest NodeJS and NPM
RUN apt update && apt install -y nodejs npm

# Install Grunt and typescript
RUN npm install -g grunt-cli typescript

# Copy in source files
WORKDIR /app
COPY ./ /app

# Compile and minimise JS and CSS
RUN npm install && tsc && grunt package

#
# Second stage is for execution and will host visualisation.
#
FROM alpine:3.14.0 AS run

# Install utilities, Apache, and PHP
RUN apk update && apk add procps nano wget bash busybox-initscripts dos2unix apache2 php7-apache2

# Copy the final TWA-VF files
RUN mkdir -p /var/www/twa-vf
COPY --from=build /app/output/ /var/www/twa-vf/

# Custom in the HTTPD configuration
COPY ./resources/httpd.conf /etc/apache2/httpd.conf

# Copy in the start-up script
COPY ./resources/start-up.sh /usr/local/start-up.sh
RUN chmod 777 /usr/local/start-up.sh
RUN chmod +x /usr/local/start-up.sh
RUN dos2unix /usr/local/start-up.sh

# Permissions
RUN mkdir -p /var/www/html
RUN chmod -R 775 /var/www/

# Expose port 80
EXPOSE 80

# Run cron daemon and boot script at start
CMD [ "/bin/bash", "-c", "/usr/local/start-up.sh" ]
