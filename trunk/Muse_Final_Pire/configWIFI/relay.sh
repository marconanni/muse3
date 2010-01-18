#!/bin/bash
paramAdHocBigBoss ()
{
	#RETE ADHOC BIGBOSS
	CLUSTERHEAD_INTERFACE=wlan0
	CLUSTERHEAD_ESSID=BIGBOSS
	CLUSTERHEAD_IP=192.168.30.2
}

paramAdHocRelay ()
{
	#RETE ADHOC RELAY1
	CLUSTER_INTERFACE=wlan1
	CLUSTER_ESSID=RELAY1
	CLUSTER_IP=192.168.10.2
}

paramInternetHome ()
{
	#Internet/Server
	INTERNET_INTERFACE=wlan0
	INTERNET_ESSID=lord
	INTERNET_IP=192.168.2.4
}
paramServerAlmawifi ()
{
	INTERNET_INTERFACE=wlan0
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
connect_cluster ()
{
	paramAdHocRelay

	#ADHOC BIGBOSS
	sudo ifconfig $CLUSTER_INTERFACE down
	sudo iwconfig $CLUSTER_INTERFACE mode ad-hoc
	sudo iwconfig $CLUSTER_INTERFACE essid $CLUSTER_ESSID
	sudo iwconfig $CLUSTER_INTERFACE rate 11M
	sudo iwconfig $CLUSTER_INTERFACE key off
	sudo ifconfig $CLUSTER_INTERFACE $CLUSTER_IP netmask 255.255.255.0 up
}

connect_home_internet ()
{
	paramInternetHome
	
	#lord
	sudo ifconfig $INTERNET_INTERFACE down
	sudo iwconfig $INTERNET_INTERFACE mode managed
	sudo iwconfig $INTERNET_INTERFACE essid $INTERNET_ESSID
	sudo iwconfig $INTERNET_INTERFACE channel 9
	sudo iwconfig $INTERNET_INTERFACE key 2627-F685-97
	sudo iwconfig $INTERNET_INTERFACE rate 11M
	sudo ifconfig $INTERNET_INTERFACE $INTERNET_IP netmask 255.255.255.0 up
	sudo dhclient $INTERNET_INTERFACE
}

connect_uni_almawifi ()
{
	paramServerAlmawifi

	sudo ifconfig $INTERNET_INTERFACE down
	sudo ifconfig $INTERNET_INTERFACE up
	sudo wpa_supplicant -Dwext -iwlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf &
	sudo dhclient $INTERNET_INTERFACE
}

connect_uni_ethernet ()
{
	sudo ifconfig $INTERNET_INTERFACE up
	sudo dhclient $INTERNET_INTERFACE 
}

# Declare variable choice and assign value 3
stopService
choice=5
# Print to stdout
 echo "1. normal"
 echo "2. home with internet"
 echo "3. uni with internet Almawifi"
 echo "4. uni with internet ethernet"
 echo -n "Please choose a word [1,2,3 or 4]? "
# Loop while the variable choice is equal 4
# bash while loop
while [ $choice -eq 5 ]; do
	# read user input
	read choice
	# bash nested if/else
	if [ $choice -eq 1 ] ; then
		connect_cluster_head
		connect_cluster
	else                   
	    	if [ $choice -eq 2 ] ; then
	    		connect_cluster_head
			connect_cluster
			connect_home_internet
	   	else
	    		if [ $choice -eq 3 ] ; then
				connect_cluster_head
				connect_cluster
				connect_uni_almawifi
			else
				if [ $choice -eq 4 ] ; then
					connect_cluster_head
					connect_cluster
					connect_uni_ethernet
				else
					echo "1. normal"
	 				echo "2. home with internet"
	 				echo "3. uni with internet ALMAWIFI"
	 				echo "4. uni with internet ethernet"
	 				echo -n "Please choose a word [1,2,3 or 4]? "
					choice=5
			  	fi
			fi
		fi
	fi
done 

