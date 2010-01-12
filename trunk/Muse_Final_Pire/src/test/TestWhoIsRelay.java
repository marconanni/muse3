package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;

import debug.DebugConsole;
import relay.connection.WhoIsRelayServer;
import relay.messages.RelayMessageFactory;

class TestWhoIsRelayServer {

	public static void main(String args[]){
		System.out.println("TestWhoIsRelayServer");
		DebugConsole console = new DebugConsole();

		WhoIsRelayServer wirs = new WhoIsRelayServer(console);
		wirs.start();

		InetAddress BCAST;
		DatagramSocket ds;
		DatagramPacket dp;
		try {
			BCAST = InetAddress.getByName(NetConfiguration.CLUSTER_BROADCAST_ADDRESS);

			ds = new DatagramSocket(9999);

		} catch (UnknownHostException e) {e.printStackTrace();} 
		  catch (SocketException e) {e.printStackTrace();}


		for(int i = 0; i<50; i++){
			try {
				
				dp = RelayMessageFactory.buildWhoIsRelay(BCAST, PortConfiguration.WHO_IS_RELAY_PORT_IN);
				ds.send(dp);
				System.out.println("TestWhoIsRelayServer: inviato WHO_IS_RELAY in Broadcast alla porta " + PortConfiguration.WHO_IS_RELAY_PORT_IN);
				
				byte[] buffer = new byte[256];
				DatagramPacket in = new DatagramPacket(buffer,buffer.length);
				
				ds.receive(in);
				cmr = new ClientMessageReader();
				cmr.readContent(in);
				if(cmr.getCode() == MessageCodeConfiguration.IM_RELAY){

					System.err.println("TestWhoIsRelayServer: ricevuto IM_RELAY da: " 
										+ in.getAddress().getHostAddress() 
										+ " dalla porta: " +in.getPort());
					
					System.err.println("TestWhoIsRelayServer: il Relay :" +cmr.getActualRelayAddress()); 

				}
						
				Thread.sleep(4000);
				
			} catch (IOException e) {e.printStackTrace();}
			  catch (InterruptedException e) {e.printStackTrace();}
		}
		
		wirs.close();
	}
}