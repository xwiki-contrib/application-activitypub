# Development Environment

This document describes the installation of an development environment to develop on activitypub with different services.

These instructions have been tested on Ubuntu 18.04 lts.

## Requirements

- nginx
- apache2 (httpd)
- Nextcloud 17.0.4

```shell script
# apt install nginx apache2 
```

```shell script
$ wget https://download.nextcloud.com/server/releases/nextcloud-17.0.4.zip
```

## Configuration

Nginx must be running on port 80.
Apache2 (httpd) must running on port 81.

Nginx is already listening on port 80 by default, but so is Apache2. To change the listening port Apache2, edit `/etc/apache2/ports.conf` and replace `Listen 80` by `Listen 81`.

## Domain names configuration

Edit `/etc/hosts` and add the following lines :

```text
127.0.0.1   nextcloud.home
127.0.0.1   xwiki.home
127.0.0.1   ywiki.home
127.0.0.1   mastodon.local
```

## XWiki installation

XWiki listen by default on port 8080. To run multiple instances of XWiki in a single computer, change the listening port of the instanced with the `-p` option when using tomcat (for instance `./start_xwiki.sh -p 8081`).

## Nexcloud installation

Extract the content of nextcloud-17.0.4.zip in `/var/www/html/server/`.

```shell script
$ unzip nextcloud-17.0.4.zip
$ mv ./nextcloud /var/www/html/server/
```

Connect to http://localhost:81 and follow nextcloud installation steps.

Then edit `/var/www/html/server/config/config.php` to add/edit the follow configuration keys:

```php
'trusted_domains' =>
    array(
        1 => 'nextcloud.home',
    ),
'overwrite.cli.url' => 'http://nextcloud.home',
'debug' => true,
"loglevel" => "0",
"overwritehost" => "nextcloud.home",
"cloud_url" => "nextcloud.home"
```

## Mastodon

Clone `git@github.com:tootsuite/mastodon.git`.

Edit `Vagrantfile` and change the host value from 8080 to 8081 in the line `config.vm.network :forwarded_port, guest: 8080, host: 8080`.

Then execute `vagrant up` and follow the instructions.

## XWiki

XWiki can be started in several ways, see https://www.xwiki.org/xwiki/bin/view/Documentation/AdminGuide/Installation/.

## Nginx configuration

Place the following file in `/etc/nginx/conf.d/`

### xwiki.conf

```
server {
    listen 80;
    server_name  xwiki.home;
    
    charset utf-8;

    location ~ ^/.well-known/webfinger {
        proxy_pass              $scheme://127.0.0.1:8080/xwiki/webfinger$is_args$args;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        Host $host;
        proxy_set_header        X-Forwarded-Proto $scheme;

        expires 0m;
    }

    location / {
        proxy_pass              $scheme://127.0.0.1:8080;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        Host $http_host;
        proxy_set_header        X-Forwarded-Proto $scheme;

        expires 0m;
    }

    # redirect server error pages to the static page /50x.html
    #
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```

## nextcloud.conf

```
server {
    listen 80;
    server_name  nextcloud.home;
    
    charset utf-8;

     rewrite ^/.well-known/webfinger /public.php?service=webfinger last;
    
    location / {
        proxy_pass              $scheme://127.0.0.1:81;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        Host $http_host;
        proxy_set_header        X-Forwarded-Proto $scheme;

        expires 0m;
    }

    # redirect server error pages to the static page /50x.html
    #
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```

## mastodon.conf

```
server {
    listen 80;
    server_name  mastodon.local;
    
    charset utf-8;

    location / {
        proxy_pass              $scheme://127.0.0.1:3000;
        proxy_set_header        X-Real-IP $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        Host $http_host;
        proxy_set_header        X-Forwarded-Proto $scheme;

        expires 0m;
    }

    # redirect server error pages to the static page /50x.html
    #
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```


# Conclusion

You should now be able to access the services from their local domain names:
- http://xwiki.home
- http://nexcloud.home
- http://mastodon.local