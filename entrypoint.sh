#!/bin/sh
echo "***[ Starting SPROXY container built at: ]****************"
cat /buildinfo
echo "***[ Starting AV daemon ]********************************"
sudo -u clamscan /usr/sbin/clamd -c /etc/clamd.d/scan.conf
echo "***[ Staring AV updater daemon ]*************************"
sudo -u clamupdate /usr/bin/freshclam -d
echo "***[ Starting ICAP server ]******************************"
sudo -u c-icap /usr/sbin/c-icap -f /etc/c-icap/c-icap.conf
echo "***[ Starting Security Internet Gateway ]****************"
/usr/sbin/squid -f /etc/squid/sproxy/sproxy.conf -C
sleep 3
tail -f /var/log/squid/cache.log




