#!/bin/bash
sudo service network-manager stop
#INTERNETINTERFACE=wlan0
#INTERNET=lord
#IPINTERNET=192.168.2.7

MASTERINTERFACE=wlan3
RETEMASTER=BIGBOSS
IPRETEMASTER=192.168.30.7

CLUSTERINTERFACE=wlan0
RETECLUSTER=RELAY1
IPRETECLUSTER=192.168.10.7

#sudo ifconfig $INTERNETINTERFACE down
sudo ifconfig $MASTERINTERFACE down
sudo ifconfig $CLUSTERINTERFACE down

#sudo iwconfig $INTERNETINTERFACE mode managed
#sudo iwconfig $INTERNETINTERFACE essid $INTERNET
#sudo iwconfig $INTERNETINTERFACE channel 9
#sudo iwconfig $INTERNETINTERFACE key 2627-F685-97
#sudo iwconfig $INTERNETINTERFACE power all
#sudo iwconfig $INTERNETINTERFACE rate 11M


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

#sudo ifconfig $INTERNETINTERFACE $IPINTERNET netmask 255.255.255.0 up
sudo ifconfig $MASTERINTERFACE $IPRETEMASTER netmask 255.255.255.0 up
sudo ifconfig $CLUSTERINTERFACE $IPRETECLUSTER netmask 255.255.255.0 up

#sudo dhclient wlan0



