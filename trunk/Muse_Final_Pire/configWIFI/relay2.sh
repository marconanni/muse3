#!/bin/bash
sudo service network-manager start
sudo service network-manager stop
ETHERNETINTERFACE=eth0

MASTERINTERFACE=wlan0
RETEMASTER=BIGBOSS
IPRETEMASTER=192.168.30.3

CLUSTERINTERFACE=wlan1
RETECLUSTER=RELAY2
IPRETECLUSTER=192.168.5.3

sudo ifconfig $ETHERNETINTERFACE down
sudo ifconfig $MASTERINTERFACE down
sudo ifconfig $CLUSTERINTERFACE down

sudo iwconfig $MASTERINTERFACE mode ad-hoc
sudo iwconfig $MASTERINTERFACE essid $RETEMASTER
sudo iwconfig $MASTERINTERFACE power all
sudo iwconfig $MASTERINTERFACE rate 11M
sudo iwconfig $MASTERINTERFACE key off

sudo iwconfig $CLUSTERINTERFACE mode ad-hoc
sudo iwconfig $CLUSTERINTERFACE essid $RETECLUSTER
sudo iwconfig $MASTERINTERFACE power all
sudo iwconfig $MASTERINTERFACE rate 11M
sudo iwconfig $CLUSTERINTERFACE key off

sudo ifconfig $MASTERINTERFACE $IPRETEMASTER netmask 255.255.255.0 up
sudo ifconfig $CLUSTERINTERFACE $IPRETECLUSTER netmask 255.255.255.0 up
sudo ifconfig $ETHERNETINTERFACE up

sudo dhclient $ETHERNETINTERFACE
