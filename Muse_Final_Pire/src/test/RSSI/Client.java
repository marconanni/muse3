package test.RSSI;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import test.TestObserver;

import client.connection.ClientCM;
import client.position.ClientPositionController;
import client.wnic.ClientWNICLinuxController;
import client.wnic.exception.WNICException;

public class Client {
	
	private static String name = "DELL";
	private static boolean BIDIREZIONE = true;
	private static int BUFFER_SIZE = 1024;
	private static int TIMEOUT_RECEIVE = 1000;
	private static int PORT_IN_OUT = 12345;
	
	private static boolean RSSI_CONTROLLER = true;
	private static int PORT_RSSI_IN = 3000;
	private static int PORT_RSSI_OUT = 3001;
		
	private static String NETWORK_ESSID = "BIGBOSS";
	private static String WIFI_INTERFACE = "wlan1";
	private static String LOCAL_ADDRESS = "192.168.30.3";
	private static String DESTINATION_ADDRESS = "192.168.30.2";
	private static String BCAST = "192.168.30.255";
		
		public static void main(String args[]){
			TestObserver obs = new TestObserver();

			if(RSSI_CONTROLLER){
				try {
					//crmc = new ClientCM("ClientCM_"+name, InetAddress.getByName(LOCAL_ADDRESS),InetAddress.getByName(BCAST),PORT_RSSI_IN,PORT_RSSI_OUT,obs,true);

					ClientWNICLinuxController cwnic = new ClientWNICLinuxController(WIFI_INTERFACE,NETWORK_ESSID);
					cwnic.init();
					ClientPositionController position = new ClientPositionController(cwnic,null);
					position.start();
				} catch (WNICException e1) {e1.printStackTrace();}
			}


			DatagramSocket server=null;
			try {
				try {
					server = new DatagramSocket(PORT_IN_OUT,InetAddress.getByName(LOCAL_ADDRESS));
				} catch (SocketException e) {e.printStackTrace();}
			} catch (UnknownHostException e6) {e6.printStackTrace();}


			boolean stop = false;
			boolean lose = false;
			long startTime = 0;
			long endTime = 0;
		    int numbytes;
		    int MB = 0;
		    int countblocks = 0;
		    int countbytes = 0;
		    double rate = 0;
		    byte[] buf = new byte[BUFFER_SIZE];
		    DatagramPacket packet =null;
		    if(BIDIREZIONE){
		    	for(int i = 0; i<BUFFER_SIZE; i++) buf[i]=(byte)i;
		    	try {
					server.setSoTimeout(TIMEOUT_RECEIVE);
				} catch (SocketException e) {e.printStackTrace();}
		    }
	
		    try {
				while (true)
				{
					lose = false;
					startTime = System.currentTimeMillis();
					if(BIDIREZIONE){
						//System.out.println("Pacchetto spedito");
						packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(DESTINATION_ADDRESS), PORT_IN_OUT);
						server.send(packet);
					}
					
					packet = new DatagramPacket(buf, buf.length);
					try{
						server.receive(packet);
					}catch(SocketTimeoutException a){
						System.out.println("Pacchetto perso.");
						lose = true;
					}
					endTime = System.currentTimeMillis();
					if(!lose){
						numbytes = buf.length;
						countbytes +=numbytes;
						countblocks++;          // keep statistics on file size
						
						//System.out.println("RATE:"+rate);
						if(countbytes%(BUFFER_SIZE*BUFFER_SIZE)==0){
							//rate = (numbytes*8)/((endTime-startTime)/1000);
							MB++;
							System.out.println("Receive "+countblocks+" blocks = "+MB+" Mbit/s");
						}
						if(packet.getData().length!=BUFFER_SIZE)stop = true;
					}
				}
			} catch (IOException e) {e.printStackTrace();}
			
		   // outbinary.flush(); // FLUSH THE BUFFER
		    server.close(); // done with the socket
		    System.out.println(countblocks + " were read; " + countbytes + " bytes");
		}
}