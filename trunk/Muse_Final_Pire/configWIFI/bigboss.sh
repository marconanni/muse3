#!/bin/bash
paramAdHocBigBoss ()
{
	#RETE ADHOC BIGBOSS
	CLUSTERHEAD_INTERFACE=wlan1
	CLUSTERHEAD_ESSID=BIGBOSS
	CLUSTERHEAD_IP=192.168.30.2
}
paramServerHome ()
{
	#Internet/Server
	SERVER_INTERFACE=wlan0
	SERVER_ESSID=lord
	SERVER_IP=192.168.2.4
}
paramServerAlmawifi ()
{
	SERVER_INTERFACE=wlan0
}
paramInternetUniEth ()
{
	INTERNET_INTERFACE=eth0
}
stopService ()
{
	sudo service network-manager start
	sudo service network-manager stop
	sudo service wpa-ifupdown stop
	sudo pkill wpa_supplicant
	sudo pkill dhclient
}
connect_cluster_head ()
{
	paramAdHocBigBoss

	#ADHOC BIGBOSS
	sudo ifconfig $CLUSTERHEAD_INTERFACE down
	sudo iwconfig $CLUSTERHEAD_INTERFACE mode ad-hoc
	sudo iwconfig $CLUSTERHEAD_INTERFACE essid $CLUSTERHEAD_ESSID
	sudo iwconfig $CLUSTERHEAD_INTERFACE rate 11M
	sudo iwconfig $CLUSTERHEAD_INTERFACE key off
	sudo ifconfig $CLUSTERHEAD_INTERFACE $CLUSTERHEAD_IP netmask 255.255.255.0 up
}

connect_home_server ()
{
	paramServerHome
	
	#lord
	sudo ifconfig $SERVER_INTERFACE down
	sudo iwconfig $SERVER_INTERFACE mode managed
	sudo iwconfig $SERVER_INTERFACE essid $SERVER_ESSID
	sudo iwconfig $SERVER_INTERFACE channel 9
	sudo iwconfig $SERVER_INTERFACE key 2627-F685-97
	sudo iwconfig $SERVER_INTERFACE rate 11M
	sudo ifconfig $SERVER_INTERFACE $SERVER_IP netmask 255.255.255.0 up
	sudo dhclient $SERVER_INTERFACE
}

connect_uni_almawifi ()
{
	paramServerAlmawifi

	sudo ifconfig $SERVER_INTERFACE down
	sudo ifconfig $SERVER_INTERFACE up
	sudo wpa_supplicant -Dwext -iwlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf &
	sudo dhclient $SERVER_INTERFACE
}

connect_uni_ethernet ()
{
	sudo ifconfig $INTERNET_INTERFACE up
	sudo dhclient $INTERNET_INTERFACE 
}

# Declare variable choice and assign value 3
stopService
choice=4
# Print to stdout
 echo "1. home"
 echo "2. Uni Almawifi"
 echo "3. Uni Almawifi with ethernet"
 echo -n "Please choose a word [1,2 or 3]? "
# Loop while the variable choice is equal 4
# bash while loop
while [ $choice -eq 4 ]; do
	# read user input
	read choice
	# bash nested if/else
	if [ $choice -eq 1 ] ; then
		connect_cluster_head
		connect_home_server
	else                   
	    	if [ $choice -eq 2 ] ; then
			connect_cluster_head
	    		connect_uni_almawifi
	   	else
	    		if [ $choice -eq 3 ] ; then
				connect_cluster_head
	    			connect_uni_almawifi
				connect_uni_ethernet
			else
				echo "1. home"
				echo "2. Uni Almawifi"
				echo "3. Uni Almawifi with ethernet"
				echo -n "Please choose a word [1,2 or 3]? "
				choice=4
			fi
		fi
	fi
done 

