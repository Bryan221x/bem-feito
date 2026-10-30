FROM caddy:2.11.7-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY index.html /srv/index.html
COPY pages /srv/pages
COPY css /srv/css
COPY js /srv/js
COPY assets /srv/assets
