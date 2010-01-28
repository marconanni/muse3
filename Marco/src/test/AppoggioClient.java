package test;

import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;

import client.ClientMessageReader;
import client.connection.*;
import debug.*;
import parameters.*;

public class AppoggioClient implements Observer {
	private ClientCM sessionCM;
	private DebugConsole consolle;
	ClientMessageReader messageReader;
	
	

	public AppoggioClient() {
		this.sessionCM= ClientConnectionFactory.getSessionConnectionManager(this,false);
		this.sessionCM.start();
		System.out.println("In attesa del messaggio");
		consolle = new DebugConsole();
		consolle.debugMessage(0, "Client Partito");
		
	}



	@Override
	public void update(Observable arg, Object event) {

		if (event instanceof DatagramPacket){
			if(messageReader.getCode()== MessageCodeConfiguration.ACK_CLIENT_REQ)
				System.out.println( "Arrivato messaggio di AckClientREq");
			
			
			
			
			
		}
		
		
		
	}

}
