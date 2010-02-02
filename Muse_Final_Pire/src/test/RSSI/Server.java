package test.RSSI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import sun.awt.image.BytePackedRaster;

class Server{
	
	public static void main(String args[]){
		Scanner inFromUser = new Scanner(System.in);
		int port = 12345;
		InetAddress localAddress=null;
		try {
			localAddress = InetAddress.getByName("192.168.5.2");
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

//		Scanner in = new Scanner(new DataInputStream(server.getInputStream()));
	//	String filename = in.nextLine();
		//DatagramSocket request = server.accept();


// Create buffer, then we're ready to go:
// Puts file into binary form
    BufferedInputStream inbinary=null;
	try {
		inbinary = new BufferedInputStream(new FileInputStream("Up.avi"));
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
// Outputs the binary form
  //  BufferedOutputStream outbinary = new BufferedOutputStream(request.getOutputStream());

    int numbytes;
    int countblocks = 0;
    int countbytes = 0;
    byte[] buf = new byte[1024];
    DatagramPacket packet = new DatagramPacket(buf, buf.length, port);

    try {
		while ((numbytes = inbinary.read(buf,0,1024)) >= 0)
		{
			// receive packet from client, telling it to send the video file
			server.receive(packet);
			InetAddress address = packet.getAddress();
			packet = new DatagramPacket(buf, buf.length, address, port);
			server.send(packet);
			countblocks++;          // keep statistics on file size
			countbytes += numbytes;
			//outbinary.write(buf,0,numbytes); // write buffer to socket
		}
		server.receive(packet);
		buf = null;
		buf[0]=Byte.parseByte("E");
		buf[1]=Byte.parseByte("N");
		buf[2]=Byte.parseByte("D");
		
		InetAddress address = packet.getAddress();
		packet = new DatagramPacket(buf, buf.length, address, port);
		server.send(packet);
		
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
   // outbinary.flush(); // FLUSH THE BUFFER
    server.close(); // done with the socket
    System.out.println(countblocks + " were read; " + countbytes + " bytes");
}
}

