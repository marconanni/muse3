package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import dummies.DummyBuffer;
import dummies.DummyReceiver;


public class MainTestClient   {

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int port = 2000;
		
			String address ="127.0.0.1";
		
		DummyBuffer buffer = new DummyBuffer ( 500, 500,500, 0, 0);
		DummyReceiver Receiver = new DummyReceiver(false, address, port, buffer);
		
		
		
		
		

	}

	

	
	

}
