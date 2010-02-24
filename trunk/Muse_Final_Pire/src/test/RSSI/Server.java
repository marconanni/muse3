package test.RSSI;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;



import debug.DebugConsole;

import relay.connection.RelayCM;
import relay.position.RelayPositionMonitor;
import test.TestObserver;

class Server{
	
	private static String name = "Gericom";
	private static boolean BIDIREZIONE = false;
	private static int BUFFER_SIZE = 1024;
	private static boolean RSSI_CONTROLLER = true;
	private static int PORT_RSSI_IN = 3000;
	private static int PORT_RSSI_OUT = 3001;
	private static int PORT_IN_OUT = 12346;
	
	private static String LOCAL_ADDRESS = "192.168.30.2";
	private static String DESTINATION_ADDRESS = "192.168.30.3";
	
	
	public static void main(String args[]){
		TestObserver obs = new TestObserver();
		DebugConsole console = new DebugConsole(name);
		
		if(RSSI_CONTROLLER){
			//RelayCM rrmc = new RelayCM("RelayCM_"+name, InetAddress.getByName(LOCAL_ADDRESS),null,PORT_RSSI_IN,PORT_RSSI_OUT,obs,false);
			RelayPositionMonitor monitor = new RelayPositionMonitor(10,2000,obs,console,null);
			monitor.start();

			
		}
		
//		DatagramSocket server=null;
//		try {
//			try {
//				server = new DatagramSocket(PORT_IN_OUT,InetAddress.getByName(LOCAL_ADDRESS));
//			} catch (UnknownHostException e) {e.printStackTrace();}
//		} catch (SocketException e) {e.printStackTrace();}
//
//		BufferedInputStream inbinary=null;
//		try {
//			inbinary = new BufferedInputStream(new FileInputStream("Up.avi"));
//		} catch (FileNotFoundException e) {e.printStackTrace();}
//	
//		long startTime = 0;
//		long endTime = 0;
//		int numbytes;
//	    int countblocks = 0;
//	    int countbytes = 0;
//	    int MB = 0;
//	    double rate = 0;
//	    byte[] buf = new byte[BUFFER_SIZE];
//	    DatagramPacket packet = new DatagramPacket(buf, buf.length);
//
//	    try {
//	    	while ((numbytes = inbinary.read(buf,0,BUFFER_SIZE)) >= 0)
//	    	{
//	    	//	startTime = System.currentTimeMillis();
//	    		if(BIDIREZIONE)server.receive(packet);
//
//
//				packet = new DatagramPacket(buf, buf.length,InetAddress.getByName(DESTINATION_ADDRESS),PORT_IN_OUT);
//				server.send(packet);
//			//	endTime = System.currentTimeMillis()+1000;
//				
//				countbytes+= numbytes;
//				countblocks++;         
//				//rate = (numbytes*8)/((endTime-startTime)/1000);
//				if(countbytes%(BUFFER_SIZE*BUFFER_SIZE)==0){
//					MB++;
//					System.out.println("SEND "+countblocks+" blocks = "+MB+" Mb");
//				}
//			}
//		} catch (IOException e) {e.printStackTrace();}
//	
//	    server.close(); // done with the socket
//	    System.out.println(countblocks + " were read; " + countbytes + " bytes");
	}
}

