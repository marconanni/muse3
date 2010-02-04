package test;

import java.io.IOException;
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
			try {
				messageReader= new ClientMessageReader();
				messageReader.readContent((DatagramPacket)event);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				consolle.debugMessage(2,"CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			if(messageReader.getCode()== MessageCodeConfiguration.REDIRECT){
				System.out.println( "Arrivato messaggio di Redirect");	
				consolle.debugMessage(2, "Arrivato messaggio di redirect");
			}
			
			else {
				String stringa = event.toString();
				consolle.debugMessage(2, "arrivato messaggio sconosciuto "+stringa);
			}
			
					
		
		
		
		}

	}
}
