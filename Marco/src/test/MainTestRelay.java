package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observer;
import java.util.Observable;

import parameters.*;
import debug.*;
import relay.*;
import relay.connection.*;



public class MainTestRelay implements Observer {
	
	private RelayMessageReader reader;
	private DebugConsole consolle;
	private RelayCM CM;
	public MainTestRelay() {
		
		consolle = new DebugConsole();
		consolle.debugMessage(0, "relayPartito");
		CM=RelayConnectionFactory.getSessionConnectionManager(this);
		CM.start();
		inviaMsg();
	}
	
	
	public static void main(String [] args){
		MainTestRelay relayTest = new MainTestRelay();
		
		
	}
	
	public  void inviaMsg(){
		try {
			
			DatagramPacket message = RelayMessageFactory.buildRedirect(0, InetAddress.getByName(NetConfiguration.CLIENT_ADDRESS), PortConfiguration.CLIENT_PORT_SESSION_IN);
			consolle.debugMessage(0, "Preparato messaggio");
			CM.sendTo(message);
			consolle.debugMessage(0, "Preparato messaggio");
		} 
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	

}
