package muse.server;
/**
 * MainServer.java
 * Classe che implementa il thread principale lato server.
 * Per ogni richiesta di servizio ricevuta dal client il thread istanzia un nuovo streaming server
 * che si occupa della trasmissione del file richiesto verso il proxy
 * @author Ambra Montecchia, Pamela Zapparoli
 * @version 1.1
 *  */
import java.io.File;
import java.io.FilenameFilter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import debug.DebugConsolle;
import parameters.Parameters;

public class MainServer extends Thread{
	
	//Manager per la gestione della connessione UDP su cui il server riceve le richieste
	private ConnectionManager manager;
	//console di debug
	private DebugConsolle dc;
	//variabile booleana che indica se il server � in esecuzione
	private boolean running;
	//griglia che tiene traccia delle porte attualmente occupate (true se la porta � libera, false se � occupata)
	private boolean[] grid;
	
	/**
	 * Costruttore
	 * */
	public MainServer(){
		
		manager = new ConnectionManager();
		manager.openSocket(Parameters.SERVER_REQUEST);
		dc = new DebugConsolle();
		running = true;
		grid = new boolean[64512];//2^16-1024. 1024 sono le well-kwown port
		//inizializzazione della griglia: tutte le porte sono libere
		for(int i = 0;i < 64512;i++){
			grid[i] = true;
		}
		grid[Parameters.SERVER_REQUEST] = false;
	}
	
	/**
	 * Metodo run
	 * */
	public void run(){
		
		int res = 0;
		dc.debugMessage("MainServer avviato");
		
		// Gestione files
		
		File dir;
		if(System.getProperty("file.separator").contains("\\")){
			dir=new File(System.getProperty("user.dir")+"\\"+"mp3");
			}
		else
			dir=new File(System.getProperty("user.dir")+"/"+"mp3");
	    FilenameFilter filter = new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            return name.endsWith("mp3") || name.endsWith("wav");
	        }
	    };
	    System.out.println("directory"+dir.toString());
		String[] files=dir.list(filter);
		if(files.length == 0)
		{
			System.err.println("Non ci sono files WAV o MP3 nella directory "+dir.getName());
			System.exit(0);
		}			
		
		// Ciclo attesa richieste
		while(running){
			//il server � in attesa di ricevere richieste da parte del client
			dc.debugMessage("Attesa richieste sulla porta "+manager.getLocalPort());
			res = manager.receivePacket();
			while(res != 0){
				res = manager.receivePacket();
			}
			
			//int proxyPort = 0;
			int clientPort=0;
			int msgIdx=0;
			//InetAddress proxyIP = null;
			InetAddress clientIP = null;
			
			try
			{
				//lettura del messaggio ricevuto
				DatagramPacket packet = manager.getPacket();
				//proxyIP = packet.getAddress();
				clientIP = packet.getAddress();
				System.out.println("prima della lettura del messaggio");
				ServerMessageReader.readContent(manager.getPacket());
				System.out.println("dopo la lettura del messaggio");
				int code = ServerMessageReader.getCode();
				System.out.println("codice del messaggio ricevuto: "+code);
				
				if(code == Parameters.FILE_REQUEST){
					dc.debugMessage("Ricevuta richiesta file da parte del client");
					System.out.println("Ricevuta richiesta file da parte del client");

					clientIP = packet.getAddress();
							System.out.println("FILE_REQUEST----clientIP: "+clientIP);
					String filename = ServerMessageReader.getSecondParam();
							System.out.println("FILE_REQUEST----filename: "+filename);
					//proxyPort = Integer.parseInt(ServerMessageReader.getFirstParam());
					clientPort = Integer.parseInt(ServerMessageReader.getFirstParam());
							System.out.println("FILE_REQUEST----clientPort: "+clientPort);

					
					String filePath = "";
					if(System.getProperty("file.separator").contains("\\"))
						filePath = "file://"+dir+"\\"+filename;
					else
						filePath = "file://"+dir+"/"+filename;

					//inizializzazione e avvio del thread per la gestione della richiesta
					int streamingPort = 0;
					//ciclo per individuare la prima porta disponibile
					//la porta deve essere liberata dal client al termine della trasmissione
					for(int i = 0;i < 64512;i++){
						if(grid[i]){
							grid[i] = false;
							streamingPort = i + 2024;
							break;
						}
					}	
					
										
					
					dc.debugMessage("Client IP: "+clientIP.getHostAddress()+", Client Port: "+clientPort);
					System.out.println("Client IP: "+clientIP.getHostAddress()+", Client Port: "+clientPort);
					StreamingServer sender = new StreamingServer(filePath, streamingPort, clientIP.getHostAddress(), clientPort, dc);
					sender.start();
					// invio della conferma di avvenuta ricezione della richiesta
					DatagramPacket confirm = ServerMessageFactory.buildConfirmRequest(msgIdx++,packet.getAddress(), packet.getPort(),streamingPort);
					manager.sendPacket(confirm);
					System.out.println("Inviata conferma al client");
					dc.debugMessage("Inviata conferma al client");

					
//----------------------------------------------------------
					//aspetto un pacchetto da ClientController
					res = manager.receivePacket();
					ServerMessageReader.readContent(manager.getPacket());
					int codice = ServerMessageReader.getCode();
					System.out.println("-------ARRIVATO MESSAGGIO DA CLIENTCONTROLLER CODICE: "+codice+" ------------");
					//aggiungo queste righe per mandare un messaggio al client in cui dico START_PLAYBACK
					System.out.println("RIPONDO SU "+packet.getAddress()+" "+codice);
					DatagramPacket vai=ServerMessageFactory.vai(msgIdx++, packet.getAddress(), codice);
					manager.sendPacket(vai);					
//------------------------------------------------------------			
					
					
				}
				if(code==Parameters.ACK)
				{
					System.out.println("Ricevuta conferma di ricezione da parte del proxy");
					dc.debugMessage("Ricevuta conferma di ricezione da parte del proxy");
				}
				if(code==Parameters.FILES_REQ)
				{
					dc.debugMessage("Ricevuta richiesta lista files");
					System.out.println("Ricevuta richiesta lista files");
					
					String list="";
					for(int i=0;i<files.length;i++)
					{
						list+=files[i];
						if(i!=files.length-1)
							list+=",";
					}
					
					DatagramPacket resp = ServerMessageFactory.buildFilesListMessage(msgIdx++, packet.getAddress(), packet.getPort(), list);
					manager.sendPacket(resp);
					System.out.println("Inviata lista files al client");
					dc.debugMessage("Inviata lista files al client");
				}
			}
			catch(Exception ex){
				try{
					//invio del messaggio di errore
					DatagramPacket error = ServerMessageFactory.buildErrorMessage(0,clientIP, clientPort);
					manager.sendPacket(error);
					dc.debugMessage("Si � verificato un errore: "+ex.getMessage()+"\nInviato messaggio di notifica al client");
					ex.printStackTrace();
				}
				catch(Exception exc){
					dc.debugMessage("Impossibile inviare messaggio di errore al proxy");
				}
			}
		}
	}
	
	/**
	 * Metodo per la terminazione del server
	 * */
	public void closeServer(){
		running = false;
		dc.debugMessage("Richiesta chiusura MainServer");
	}

}
