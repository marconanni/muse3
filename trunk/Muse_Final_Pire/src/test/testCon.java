package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.savarese.rocksaw.net.RawSocket;


public class testCon {
	public static void main(String args[]) throws IOException{
		byte[] buffer = {100};
		InetAddress BCAST = null;
		InetAddress local = null;
		

			try {
				BCAST  = InetAddress.getByName("192.168.30.255");
				local = InetAddress.getByName("192.168.30.2");
			} catch (UnknownHostException e) {e.printStackTrace();}

		DatagramPacket pac = new DatagramPacket(buffer,buffer.length,BCAST,5000);
		DatagramSocket inout = new DatagramSocket(5000,local);
		//inout.send(pac);
		DatagramPacket dataIn = new DatagramPacket(buffer, buffer.length);
		while(true){
		try {
			inout.receive(dataIn);
			System.out.println("ricevuto messaggio da : " +dataIn.getAddress().getHostAddress().charAt(10) + " porta: " +dataIn.getPort());
		}catch (IOException e) {
			System.out.println(": " + e.getMessage());
		}
		}
	}

}
