package test.RSSI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import relay.wnic.RelayAPWNICLinuxController;

public class ClientStrength {
	
	private static String name = "DELL";
	private static boolean BIDIREZIONE = true;
	private static int BUFFER_SIZE = 1024;
	private static int TIMEOUT_RECEIVE = 100;
	private static int PORT_IN_OUT = 12345;
		
	private static String NETWORK_ESSID = "lord";
	private static String WIFI_INTERFACE = "wlan0";
	private static String LOCAL_ADDRESS = "192.168.2.4";
	private static String DESTINATION_ADDRESS = "192.168.2.7";
	
	private static WritableWorkbook workbook = null;
	private static WritableSheet sheet = null;
	private BufferedWriter bw = null;
	
	private static RelayAPWNICLinuxController wnic = null;
		
		public static void main(String args[]){

			try {
				wnic = new RelayAPWNICLinuxController(5,WIFI_INTERFACE,NETWORK_ESSID);
			} catch (relay.wnic.exception.WNICException e1) {e1.printStackTrace();}

			DatagramSocket server=null;
			try {
				try {
					server = new DatagramSocket(PORT_IN_OUT,InetAddress.getByName(LOCAL_ADDRESS));
				} catch (SocketException e) {e.printStackTrace();}
			} catch (UnknownHostException e6) {e6.printStackTrace();}



		    byte[] buf = new byte[BUFFER_SIZE];
		    DatagramPacket packet =null;
		    if(BIDIREZIONE){
		    	for(int i = 0; i<BUFFER_SIZE; i++) buf[i]=(byte)i;
		    	try {
					server.setSoTimeout(TIMEOUT_RECEIVE);
				} catch (SocketException e) {e.printStackTrace();}
		    }
	
		    boolean lose = false;
		    int countcell = 0;
		    
		    try {
				workbook = Workbook.createWorkbook(new File("signalStrength.xls"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sheet = workbook.createSheet("First Sheett",0);
			Label l1 = new Label(0, countcell, "RSSI");
			Label l2 = new Label(1,countcell, "Lose");
			Label l3 = new Label(2,countcell,"Ok");
			Label l4 = new Label(3,countcell,"Time send - receive (ms)");
			
			
			try {
				sheet.addCell(l1);
				sheet.addCell(l2);
				sheet.addCell(l3);
				sheet.addCell(l4);
			}
			 catch (RowsExceededException e1) {e1.printStackTrace();} 
			 catch (WriteException e1) {e1.printStackTrace();}
			
			
			int countLose = 0;
			long start = 0;
			long stop = 0;
		    
		    try {
				while (true)
				{
					countcell++;
					lose = false;

					if(BIDIREZIONE){
						//System.out.println("Pacchetto spedito");
						packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(DESTINATION_ADDRESS), PORT_IN_OUT);
						server.send(packet);
						start = System.currentTimeMillis();
					}
					
					packet = new DatagramPacket(buf, buf.length);
					try{
						server.receive(packet);
						stop = System.currentTimeMillis();
						try {
							Number n1 = new Number(0,countcell,wnic.updateSignalStrenghtValue());
							Number n2 = new Number(2,countcell,packet.getLength());
							Number n3 = new Number(3,countcell,stop-start);
							sheet.addCell(n1);
							sheet.addCell(n2);
							sheet.addCell(n3);
						} catch (relay.wnic.exception.WNICException e) {e.printStackTrace();}
						  catch (RowsExceededException e) {	} catch (WriteException e) {e.printStackTrace();}
					}catch(SocketTimeoutException a){
						stop = System.currentTimeMillis();
						System.out.println("Pacchetto perso."+countLose);
						countLose++;
						try {
							Number n1 = new Number(0,countcell,wnic.updateSignalStrenghtValue());
							Number n2 = new Number(1,countcell,1);
							Number n3 = new Number(3,countcell,stop-start);
							
							try {
								sheet.addCell(n1);
								sheet.addCell(n2);
								sheet.addCell(n3);
							} catch (RowsExceededException e) {	} catch (WriteException e) {e.printStackTrace();}
						} catch (relay.wnic.exception.WNICException e) {e.printStackTrace();}
						if(countLose ==10){
							workbook.write();
							try {
								workbook.close();
							} catch (WriteException e) {e.printStackTrace();}
							System.exit(1);
						}
					}
					if(countcell%10000==0){
						workbook.write();
					}
				}
			} catch (IOException e) {e.printStackTrace();}
			
		   // outbinary.flush(); // FLUSH THE BUFFER
		    server.close(); // done with the socket
		}
}