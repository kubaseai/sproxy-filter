FROM redhat/ubi9

MAINTAINER kuba@sys.one.pl

RUN /bin/sh -c "dnf -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-9.noarch.rpm && \
dnf update -y && dnf install sudo java-21-openjdk squid squidclamav c-icap clamav clamd clamav-freshclam -y && \
echo 'Service av squidclamav.so' >> /etc/c-icap/c-icap.conf && \
echo 'LocalSocket /var/run/clamav/clamd.ctl' >> /etc/clamd.d/scan.conf && \
chown c-icap:c-icap /etc/c-icap/c-icap.conf && \
chown clamupdate:clamupdate /etc/freshclam.conf && \
chown clamscan:clamscan /etc/clamd.d/scan.conf && \
mkdir -p /var/run/clamav && chown clamscan:clamscan /var/run/clamav && \
touch /run/squid.pid && chown squid:squid /run/squid.pid && \
/usr/lib64/squid/security_file_certgen -c -s /var/spool/squid/ssl_db -M 128MB && \
chown -R squid:squid /var/spool/squid/ssl_db && \
sudo -u clamupdate /usr/bin/freshclam"
RUN date > /buildinfo

ADD squidclamav.conf /etc/c-icap/squidclamav.conf
ADD src/main/resources/sproxy.conf /etc/squid/sproxy/sproxy.conf
ADD src/main/resources/config.yaml /etc/squid/sproxy/configs/config.yaml
ADD src/main/resources/cakey.pem /etc/squid/sproxy/cakey.pem
ADD src/main/resources/ca.pem /etc/squid/sproxy/ca.pem
ADD target/filter-0.0.1-SNAPSHOT.jar /etc/squid/sproxy-filter.jar
ADD entrypoint.sh /

RUN chown -R squid:squid /etc/squid

ENTRYPOINT [ "/entrypoint.sh" ]

HEALTHCHECK --start-period=200s CMD http_proxy=127.0.0.1:8443 curl -f http://www.msftncsi.com/ncsi.txt || exit 1
  
EXPOSE 8443
