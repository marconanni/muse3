package muse.client;

import java.io.IOException;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Vector;

import muse.client.connection.ConnectionManager;
import parameters.Parameters;
import muse.client.gui.*;


public class Activator {
	//array per la gestione delle porte libere
	private boolean[] grid;
	//manager per la gestione della connessione con il server
	public ConnectionManager mgr;
	//controller frame
	private IClientView view;
	//indice dei messaggi
	private int idxRec = 0, idxSend = 1;	
	
	//int indexServer=0;
	private int serverPort=0;
	
	public Activator(){
		grid = new boolean[64512];
		//inizializzazione della griglia: tutte le porte sono libere
		for(int i = 0;i < 64512;i++){
			grid[i] = true;
		}
		mgr = new ConnectionManager();
		
		//apertura socket UDP
		int opRes = mgr.openSocket(Parameters.CLIENT_ACTIVATOR_PORT);
		while(opRes != 0)
			opRes = mgr.openSocket(Parameters.CLIENT_ACTIVATOR_PORT);
		//la porta a cui � collegata la socket � occupata
		grid[Parameters.CLIENT_ACTIVATOR_PORT] = false;
		
		
	}
	
	public String[] filesRequest()
	{
		System.out.println("Richiesta lista files");
		DatagramPacket request=null;
		try {
			InetAddress serverAddr = InetAddress.getByName(Parameters.SERVER_ADDRESS);
			request = ClientMessageFactory.buildFilesReqMessage(idxSend++,serverAddr, Parameters.SERVER_REQUEST);
			mgr.sendPacket(request);
			
			int res = 0;
			res = mgr.receivePacket();
			while(res != 0)
				res = mgr.receivePacket();
			idxRec++;
			DatagramPacket response = mgr.getPacket();
			ClientMessageReader.readContent(response);
			int code = ClientMessageReader.getCode();
			if(code == Parameters.FILES_RESPONSE){
				System.out.println("Lista files ricevuta");
				String message = ClientMessageReader.getFirstParam();

				return message.split(",");
			}
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public void init(IClientView frameCtrl){
		
		this.view = frameCtrl;
		String filename = view.getFilename();
		view.debugMessage("ClientActivator: richiesto "+filename);
		System.out.println("ClientActivator: richiesto "+filename);
		
		//ciclo per individuare la prima porta disponibile per il thread di ricezione
		//al termine della trasmissione il client deve liberare la porta
		//la scansione dell porte � un processo troppo lungo!!
		//mgr.scannerPort(grid);
		int offset = 3500;
		int receivingPort = 0;
		for(int i = 0;i < 64512;i++){
			if((grid[i]) && (grid[i+1])){
				//per la ricezione sono necessarie due porte libere consecutive
				grid[i] = false;
				grid[i + 1] = false;
				receivingPort = i + offset;
				break;
			}
		}		
		
		//ciclo per individuare la prima porta disponibile per il thread di controllo
		//al termine della trasmissione il client deve liberare la porta
		int controlPort = 0;
		int controlSendPort= 0;
		for(int i = 0;i < 64512;i++){
			if(grid[i]&&grid[i+1]){
				grid[i] = false;
				grid[i+1] = false;
				controlPort = i + offset;
				controlSendPort =(i + 1) + offset;
				break;
			}
		}
		view.debugMessage("ClientActivator: receiving port : "+receivingPort+" control port: "+controlPort);
		System.out.println("ClientActivator: receiving port : "+receivingPort+" control port: "+controlPort);	

		try{
			InetAddress serverAddr = InetAddress.getByName(Parameters.SERVER_ADDRESS);
			//messaggio di richiesta file al server
			DatagramPacket request = ClientMessageFactory.buildFileRequest(idxSend++, serverAddr, Parameters.SERVER_REQUEST, filename, receivingPort);
			//indexServer++;//incremento il numero del messaggio
			
			mgr.sendPacket(request);
			view.debugMessage("Activator: inviata la richiesta del file al server");
			System.out.println("Activator: inviata la richiesta del file al server");
					
			//attesa del messaggio di conferma da parte del server
			int res = 0;
			res = mgr.receivePacket();
			while(res != 0)
				res = mgr.receivePacket();
			System.out.println("ricevuto un messaggio dal server");
			idxRec++;			
			ClientMessageReader.readContent(mgr.getPacket());
			int code = ClientMessageReader.getCode();
					System.out.println("codice del messaggio ricevuto: "+code);
			if(code == Parameters.CONFIRM_REQUEST){				
				System.out.println("Ricevuta conferma della richiesta da parte del server");
				view.debugMessage("Ricevuta conferma della richiesta da parte del server");
				int serverPort=Integer.parseInt(ClientMessageReader.getFirstParam());
				System.out.println("porta server "+serverPort);
				view.debugMessage("Porta invio flusso server: "+serverPort);
				/*
				view.setDataText(new String[]{InetAddress.getLocalHost().getHostAddress(),
						String.valueOf(Parameters.CLIENT_ACTIVATOR_PORT),
						String.valueOf(controlPort),
						String.valueOf(receivingPort),
						proxyAddr.getHostAddress(),
						String.valueOf(Parameters.PROXY_ACTIVATOR_PORT),
						String.valueOf(proxyCtrlPort),
						String.valueOf(proxySendPort),
						filename});
				*/				
				ClientController ctrl = new ClientController(controlPort, controlSendPort, view, idxRec,idxSend,serverPort,receivingPort,serverAddr);
				ctrl.start();
			}			
		}
		catch(Exception x){
			System.out.println("Eccezione nel ClientActivator: "+x.getMessage());
			x.printStackTrace();
		}
	}

}
