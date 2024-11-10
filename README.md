# sproxy-filter

```
user@fedora:~/eclipse-workspace/filter$ https_proxy=192.168.50.226:8443 curl -k -v https://www.github.com
* Uses proxy env variable https_proxy == '192.168.50.226:8443'
*   Trying 192.168.50.226:8443...
* Connected to 192.168.50.226 (192.168.50.226) port 8443
* CONNECT tunnel: HTTP/1.1 negotiated
* allocate connect buffer
* Establish HTTP proxy tunnel to www.github.com:443
> CONNECT www.github.com:443 HTTP/1.1
> Host: www.github.com:443
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
> Host: www.github.com
> User-Agent: curl/8.6.0
> Accept: */*
> 
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* old SSL session ID is stale, removing
< HTTP/1.1 301 Moved Permanently
< Content-Length: 0
< Location: https://github.com/
< Date: Sun, 10 Nov 2024 16:07:15 GMT
< Cache-Status: user-dev;detail=mismatch
< Via: 1.1 user-dev (squid/6.6)
< Connection: keep-alive
< 
* Connection #0 to host 192.168.50.226 left intact
user@fedora:~/eclipse-workspace/filter$ 
```

```
less /var/log/squid/cache.log
2024-11-10T17:07:15.544+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_WITH_SSL_AV(2) :: src=192.168.50.232:53832, user=, verb=CONNECT, dst=www.github.com:443, ct_out=-, ctIn=mt
2024-11-10T17:07:15.856+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-10T17:07:15.873+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-10T17:07:15.874+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 18 ms, count=254
2024-11-10T17:07:15.874+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-10T17:07:15.883+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-10T17:07:15.883+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 9 ms, count=254
2024-11-10T17:07:15.884+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_WITH_SSL_AV(2) :: src=192.168.50.232:53832, user=, verb=GET, dst=https://www.github.com/, ct_out=-, ctIn=mt
2024-11-10T17:07:54.126+01:00  INFO 990228 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-10T17:07:54.132+01:00  INFO 990228 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-10T17:07:54.132+01:00  INFO 990228 --- [sproxy-filter] [   scheduling-1] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 6 ms, count=254
2024-11-10T17:08:04.724+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-10T17:08:04.733+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-10T17:08:04.733+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 9 ms, count=254
2024-11-10T17:08:04.733+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request started
2024-11-10T17:08:04.743+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Reload config request finished
2024-11-10T17:08:04.744+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.ConfigReader  : Configuration reloaded in 11 ms, count=254
2024-11-10T17:08:04.745+01:00  INFO 990228 --- [sproxy-filter] [           main] n.s.access.filter.services.AccessLog     : ACCEPT_PASSTRU(1) :: src=192.168.50.232:43980, user=, verb=CONNECT, dst=www.google.com:443, ct_out=-, ctIn=mt
```

