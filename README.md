# sproxy-filter
See: docker.io/digitalforensic/sproxy:latest

```
user@fedora:~/eclipse-workspace/filter$ https_proxy=192.168.50.226:8443 curl -k -v https:/github.com
* Uses proxy env variable https_proxy == '192.168.50.226:8443'
*   Trying 192.168.50.226:8443...
* Connected to 192.168.50.226 (192.168.50.226) port 8443
* CONNECT tunnel: HTTP/1.1 negotiated
* allocate connect buffer
* Establish HTTP proxy tunnel to github.com:443
> CONNECT github.com:443 HTTP/1.1
> Host: github.com:443
> User-Agent: curl/8.6.0
> Proxy-Connection: Keep-Alive
> 
< HTTP/1.1 200 Connection established
< 
* CONNECT phase completed
* CONNECT tunnel established, response 200
* ALPN: curl offers h2,http/1.1
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384 / x25519 / id-ecPublicKey
* ALPN: server did not agree on a protocol. Uses default.
* Server certificate:
*  subject: CN=github.com
*  start date: Mar  7 00:00:00 2024 GMT
*  expire date: Mar  7 23:59:59 2025 GMT
*  issuer: C=EU; O=Security; OU=InternetAccess
*  SSL certificate verify result: self-signed certificate in certificate chain (19), continuing anyway.
*   Certificate level 0: Public key type EC/secp384r1 (384/192 Bits/secBits), signed using ecdsa-with-SHA256
*   Certificate level 1: Public key type EC/secp384r1 (384/192 Bits/secBits), signed using ecdsa-with-SHA384
* using HTTP/1.x
> GET / HTTP/1.1
> Host: github.com
> User-Agent: curl/8.6.0
> Accept: */*
> 
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* old SSL session ID is stale, removing
< HTTP/1.1 200 OK
< Server: GitHub.com
< Date: Tue, 12 Nov 2024 10:07:09 GMT
< Content-Type: text/html; charset=utf-8
< Via: 1.1 user-dev (squid/6.6)
< Connection: keep-alive
< 
<!DOCTYPE html>
<html
...
</html>

* Connection #0 to host 192.168.50.226 left intact


user@fedora:~/eclipse-workspace/filter$ https_proxy=192.168.50.226:8443 curl -k -v https://www.google.com
* Uses proxy env variable https_proxy == '192.168.50.226:8443'
*   Trying 192.168.50.226:8443...
* Connected to 192.168.50.226 (192.168.50.226) port 8443
* CONNECT tunnel: HTTP/1.1 negotiated
* allocate connect buffer
* Establish HTTP proxy tunnel to www.google.com:443
> CONNECT www.google.com:443 HTTP/1.1
> Host: www.google.com:443
> User-Agent: curl/8.6.0
> Proxy-Connection: Keep-Alive
> 
< HTTP/1.1 200 Connection established
< 
* CONNECT phase completed
* CONNECT tunnel established, response 200
* ALPN: curl offers h2,http/1.1
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384 / x25519 / id-ecPublicKey
* ALPN: server accepted h2
* Server certificate:
*  subject: CN=www.google.com
*  start date: Oct  7 08:26:36 2024 GMT
*  expire date: Dec 30 08:26:35 2024 GMT
*  issuer: C=US; O=Google Trust Services; CN=WR2
*  SSL certificate verify result: unable to get local issuer certificate (20), continuing anyway.
*   Certificate level 0: Public key type EC/prime256v1 (256/128 Bits/secBits), signed using sha256WithRSAEncryption
*   Certificate level 1: Public key type RSA (2048/112 Bits/secBits), signed using sha256WithRSAEncryption
*   Certificate level 2: Public key type RSA (4096/152 Bits/secBits), signed using sha256WithRSAEncryption
* using HTTP/2
* [HTTP/2] [1] OPENED stream for https://www.google.com/
* [HTTP/2] [1] [:method: GET]
* [HTTP/2] [1] [:scheme: https]
* [HTTP/2] [1] [:authority: www.google.com]
* [HTTP/2] [1] [:path: /]
* [HTTP/2] [1] [user-agent: curl/8.6.0]
* [HTTP/2] [1] [accept: */*]
> GET / HTTP/2
> Host: www.google.com
> User-Agent: curl/8.6.0
> Accept: */*
> 
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* old SSL session ID is stale, removing
< HTTP/2 200 
< date: Tue, 12 Nov 2024 10:07:32 GMT
< expires: -1
< cache-control: private, max-age=0
< content-type: text/html; charset=ISO-8859-1

<!doctype html><html itemscope="" itemtype="http://schema.org/WebPage" lang="pl"><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"><meta content="/images/branding/googleg/1x/googleg_standard_color_128dp.png" itemprop="image"><title>Google</title>...</html>

user@fedora:~/eclipse-workspace/filter$ https_proxy=192.168.50.226:8443 curl -k -v https://www.amazon.com
* Uses proxy env variable https_proxy == '192.168.50.226:8443'
*   Trying 192.168.50.226:8443...
* Connected to 192.168.50.226 (192.168.50.226) port 8443
* CONNECT tunnel: HTTP/1.1 negotiated
* allocate connect buffer
* Establish HTTP proxy tunnel to www.amazon.com:443
> CONNECT www.amazon.com:443 HTTP/1.1
> Host: www.amazon.com:443
> User-Agent: curl/8.6.0
> Proxy-Connection: Keep-Alive
> 
< HTTP/1.1 302 Found
< Server: squid/6.6
< Date: Tue, 12 Nov 2024 10:07:46 GMT
< Content-Length: 0
< Location: http://example.com
< Cache-Status: user-dev;detail=mismatch
< Via: 1.1 user-dev (squid/6.6)
< Connection: keep-alive
< 
* CONNECT tunnel failed, response 302
* Closing connection
curl: (56) CONNECT tunnel failed, response 302

```

```
less /var/log/squid/cache.log
2024-11-12T11:07:04.194+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.squid.access.filter.FilterApplication  : Started FilterApplication in 4.373 seconds (process running for 7.185)
2024-11-12T11:07:04.195+01:00  INFO 3105977 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-12T11:07:04.204+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.squid.access.filter.FilterApplication  : Starting filter app
2024-11-12T11:07:05.316+01:00  INFO 3105977 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-12T11:07:05.332+01:00  INFO 3105977 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 1121 ms, count=254
2024-11-12T11:07:17.615+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_WITH_SSL_AV(2) :: src=192.168.50.232:47780/-, user=, verb=CONNECT, dst=github.com:443, ct_out=-, ctIn=-
2024/11/12 11:07:17 kid1| ERROR: Unknown TLS option 
    current master transaction: master53
2024-11-12T11:07:17.902+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_WITH_SSL_AV(2) :: src=192.168.50.232:47780/-, user=, verb=GET, dst=https://github.com/, ct_out=-, ctIn=*/*
2024-11-12T11:07:32.429+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_PASSTRU(1) :: src=192.168.50.232:44772/-, user=, verb=CONNECT, dst=www.google.com:443, ct_out=-, ctIn=-
2024-11-12T11:07:46.718+01:00  INFO 3105977 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : DENY :: src=192.168.50.232:45920/-, user=, verb=CONNECT, dst=www.amazon.com:443, ct_out=-, ctIn=- // reason(s): rule by host *.microsoft.com:443 not equal to actual www.amazon.com:443; https://*.microsoft.com/** not equals www.amazon.com:443 for allowed methods [GET] and given CONNECT; rule by host microsoft.com:443 not equal to actual www.amazon.com:443; https://microsoft.com/** not equals www.amazon.com:443 for allowed methods [GET] and given CONNECT; rule by host www.google.com:443 not equal to actual www.amazon.com:443; https://www.google.com/** not equals www.amazon.com:443 for allowed methods [GET] and given CONNECT; rule by host google.com:443 not equal to actual www.amazon.com:443; https://google.com/** not equals www.amazon.com:443 for allowed methods [GET] and given CONNECT; rule by host **.github.com:443 not equal to actual www.amazon.com:443; https://**.github.com/** not equals www.amazon.com:443 for allowed methods [GET, POST, HEAD, OPTIONS] and given CONNECT; rule by host github.com:443 not equal to actual www.amazon.com:443; https://github.com/** not equals www.amazon.com:443 for allowed methods [GET, POST, HEAD, OPTIONS] and given CONNECT; 
2024/11/12 11:07:46 kid1| kick abandoning conn33 local=192.168.50.226:8443 remote=192.168.50.232:45920 FD 12 flags=1
    connection: conn33 local=192.168.50.226:8443 remote=192.168.50.232:45920 FD 12 flags=1

```

