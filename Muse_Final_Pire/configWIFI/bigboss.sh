#!/bin/bash
sudo service network-manager stop
MASTERINTERFACE=wlan0
RETEMASTER=lord
IPRETEMASTER=192.168.2.4

CLUSTERINTERFACE=wlan1
RETECLUSTER=BIGBOSS
IPRETECLUSTER=192.168.30.2

sudo ifconfig $MASTERINTERFACE down
sudo ifconfig $CLUSTERINTERFACE down

sudo iwconfig $MASTERINTERFACE mode managed
sudo iwconfig $MASTERINTERFACE essid $RETEMASTER
sudo iwconfig $MASTERINTERFACE channel 9
sudo iwconfig $MASTERINTERFACE key 2627-F685-97
sudo iwconfig $MASTERINTERFACE rate 11M
sudo iwconfig $MASTERINTERFACE power all

sudo iwconfig $CLUSTERINTERFACE mode ad-hoc
sudo iwconfig $CLUSTERINTERFACE essid $RETECLUSTER
sudo iwconfig $CLUSTERINTERFACE key off
sudo iwconfig $CLUSTERINTERFACE power all

sudo ifconfig $MASTERINTERFACE $IPRETEMASTER netmask 255.255.255.0 up
sudo ifconfig $CLUSTERINTERFACE $IPRETECLUSTER netmask 255.255.255.0 up

sudo dhclient wlan0
