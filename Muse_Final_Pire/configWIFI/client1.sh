#!/bin/bash
sudo service network-manager start
sudo service network-manager stop
ETHERNETINTERFACE=eth0

CLUSTERINTERFACE=wlan0
RETECLUSTER=RELAY1
IPRETECLUSTER=192.168.10.3

sudo ifconfig $ETHERNETINTERFACE down
sudo ifconfig $CLUSTERINTERFACE down

sudo iwconfig $CLUSTERINTERFACE mode ad-hoc
sudo iwconfig $CLUSTERINTERFACE essid $RETECLUSTER
sudo iwconfig $CLUSTERINTERFACE power all
sudo iwconfig $CLUSTERINTERFACE rate 11M
sudo iwconfig $CLUSTERINTERFACE key off

sudo ifconfig $CLUSTERINTERFACE $IPRETECLUSTER netmask 255.255.255.0 up
sudo ifconfig $ETHERNETINTERFACE up

sudo dhclient $ETHERNETINTERFACE
