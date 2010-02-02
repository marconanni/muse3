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
import java.util.Scanner;

public class Client {
		
		public static void main(String args[]){
			int port = 12345;
			InetAddress localAddress=null;
			InetAddress remoteAddress = null;
			try {
				localAddress = InetAddress.getByName("192.168.5.2");
				remoteAddress = InetAddress.getByName("192.168.5.2");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			DatagramSocket server=null;
			try {
				server = new DatagramSocket(port,localAddress);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Read name of file supplied by client (must be a line of text):

//			Scanner in = new Scanner(new DataInputStream(server.getInputStream()));
		//	String filename = in.nextLine();
			//DatagramSocket request = server.accept();


	// Create buffer, then we're ready to go:
	// Puts file into binary form
	// Outputs the binary form
	  //  BufferedOutputStream outbinary = new BufferedOutputStream(request.getOutputStream());

	    int numbytes;
	    int countblocks = 0;
	    int countbytes = 0;
	    byte[] buf = new byte[1024];
	    DatagramPacket packet = new DatagramPacket(buf, buf.length, port);

	    try {
			while (Byte.toString(buf[0]).compareTo("E")==0 && Byte.toString(buf[1]).compareTo("N")==0 && Byte.toString(buf[0]).compareTo("D")==0)
			{
				// receive packet from client, telling it to send the video file
				packet = new DatagramPacket(buf, buf.length, remoteAddress, port);
				server.send(packet);
				
				packet = new DatagramPacket(buf, buf.length, port);
				server.receive(packet);
				numbytes = buf.length;
				
				countblocks++;          // keep statistics on file size
				countbytes += numbytes;
				//outbinary.write(buf,0,numbytes); // write buffer to socket
			}
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	   // outbinary.flush(); // FLUSH THE BUFFER
	    server.close(); // done with the socket
	    System.out.println(countblocks + " were read; " + countbytes + " bytes");
	}
}
