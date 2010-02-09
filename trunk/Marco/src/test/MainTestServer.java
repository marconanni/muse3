package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import dummies.*;

public class MainTestServer {
	
	
	public static void main(String[] args){
		
		int port = 3000;
		int clientport = 2000;
		
		String address = "127.0.0.1";
	
		DummyBuffer buffer = new DummyBuffer ( 500, 500,500, 0, 0);
		
		System.out.println("Riempimento buffer in corso");
		
		for (int k =0; k<buffer.getCapacity(); k++ ){
			
			buffer.put(new Byte("1"));
			
		}
		
		System.out.println("Buffer riempito");
		
		DummySender sender = new DummySender(false, address, port, address, clientport, buffer);
		
	}
	
	
	
	

}
