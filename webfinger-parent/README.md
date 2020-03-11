# Webfinger

## Integration

According to [webfinger's RFC](https://tools.ietf.org/html/rfc7033#section-4):

> The path component of a WebFinger URI MUST be the well-known path "/.well-known/webfinger".
> A WebFinger URI MUST contain a query component that encodes the query target and optional link relation types as
> specified in [Section 4.1](https://tools.ietf.org/html/rfc7033#section-4.1).

Internally, the URL provided by XWiki is in the form `/xwiki/webfinger`.
Consequently, an http server is required to redirect queries from `/.well/know/webfinger` to `/xwiki/webfinger`.

### nginx

The integration of XWiki behind an nginx http server is documented
[here](https://www.xwiki.org/xwiki/bin/view/Documentation/AdminGuide/Installation/NginX/).

In order to support webfinger, the following rule must be integrated in the configuration, before the `location /`
block.

Three specific parts needs to be adapted:
- XWIKI_DOMAIN must be replaced with the domain name of ip address of the XWiki instance.
- XWIKI_PORT must be replaced with the listening port of the XWiki instance (the default value is `8080`).
- XWIKI_ROOT must be replaced with the root path to the xwiki instance (the default value is `xwiki`).
See [this documentation](https://www.xwiki.org/xwiki/bin/view/Documentation/AdminGuide/ShortURLs/) to customize the root path to an XWiki instance.

```nginx
location ~ ^/.well-known/webfinger {
    proxy_pass              $scheme://{XWIKI_DOMAIN}:{XWIKI_PORT}/{XWIKI_ROOT}webfinger$is_args$args;
    proxy_set_header        X-Real-IP $remote_addr;
    proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header        Host $host;
    proxy_set_header        X-Forwarded-Proto $scheme;

    expires 0m;
}

```