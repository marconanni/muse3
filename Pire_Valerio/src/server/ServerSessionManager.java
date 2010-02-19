/**
 * 
 */
package server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import javax.media.NoDataSourceException;
import javax.media.rtp.rtcp.SenderReport;

import org.omg.Dynamic.Parameter;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.PortConfiguration;

import debug.DebugConsole;



import server.StreamingServer;

import server.ServerMessageFactory;
import server.connection.ServerCM;
import server.connection.ServerConnectionFactory;




/**
 * @author Leo Di Carlo
 *
 */
public class ServerSessionManager implements Observer{


	private String status;
	private int numberOfSession;
	private Hashtable<String, StreamingServer> ssReferences;
	private DatagramPacket message;
	public static final ServerSessionManager INSTANCE = new ServerSessionManager();
	private String newRelayAddress;
	private ServerCM connManager;
	private DebugConsole consolle;
	
	private File dir;
	private String[] files;
	private int msgIdx = 0;
	private boolean[] grid;
	private ConnectionManager manager;
	private int portaOutUDP;
	
	private InetAddress bigbossAddress;
	
	public ServerSessionManager()
	{
		this.status = "Idle";
		this.numberOfSession = 0;
		ssReferences = new Hashtable();
		connManager = ServerConnectionFactory.getSessionConnectionManager(this);
		connManager.start();
		this.consolle = new DebugConsole("Session Manager");
		this.consolle.setTitle("DEBUG CONSOLLE SERVER SESSION MANAGER");
		
		//manager mi serve per gestire l'invio su UDP
		manager = new ConnectionManager();
		//Inizializzazione porte (mi server per poter inviare, visto che AConnectionReceiver controlla solo la ricezione)
		grid = new boolean[64512];//2^16-1024. 1024 sono le well-kwown port
		//inizializzazione della griglia: tutte le porte sono libere
		for(int i = 0;i < 64512;i++){
			grid[i] = true;
		}
		grid[PortConfiguration.SERVER_SESSION_PORT_IN]=false; //questa porta è già impegnata
		
		//ciclo per individuare la prima porta disponibile
		for(int i = 0;i < 2000;i++){
			if((grid[i]) && (grid[i+1])){
				//per la ricezione sono necessarie due porte libere consecutive
				grid[i] = false;
				grid[i + 1] = false;
				portaOutUDP = i + 1124;
				break;
			}
		}
		manager.openSocket(portaOutUDP);//apro la socket da cui risponderò inviando messaggi UDP
		
		
		// Gestione files
		if(System.getProperty("file.separator").contains("\\")){
			dir=new File(System.getProperty("user.dir")+"\\"+"mp3");
			}
		else
			dir=new File(System.getProperty("user.dir")+"/"+"mp3");
	    FilenameFilter filter = new FilenameFilter() {
	        public boolean accept(File dir, String name) {
	            //return name.endsWith("mp3") || name.endsWith("wav");//l'ho commentato perchè non usiamo i file.mp3 visto che non vano
	        	return name.endsWith("wav");
	        }
	    };
	    System.out.println("directory"+dir.toString());
		files=dir.list(filter);
		if(files.length == 0)
		{
			System.err.println("Non ci sono files WAV o MP3 nella directory "+dir.getName());
			System.exit(0);
		}
		consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Lista file presenti sul server: ");
		for(int i=0;i<files.length;i++){
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,files[i]);
		}
		
		
	}

	public static ServerSessionManager getInstance() {
		return ServerSessionManager.INSTANCE;
	}


	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable receiver, Object arg) {
		System.err.println("UPDATE");
		// TODO Auto-generated method stub
		StreamingServer sS;
		if(arg instanceof DatagramPacket)
		{
			this.message = (DatagramPacket) arg;
			try {
				ServerMessageReader.readContent(this.message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
//				Logger.write("SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"SERVER_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			
			//fare anche il caso forwardrequestfile
			if(ServerMessageReader.getCode()==MessageCodeConfiguration.REQUEST_LIST){//catena: client->bigboss->server
				System.out.println("Ricevuta richiesta lista files, messaggio REQUEST_LIST");
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Ricevuta richiesta lista files, messaggio REQUEST_LIST");
				String list="";
				for(int i=0;i<files.length;i++)
				{
					list+=files[i];
					if(i!=files.length-1)
						list+=",";
				}
				try {
					System.out.println("Indirizzo da cui ho ricevuto la richiesta e su cui rispondo: "+this.message.getAddress()+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Indirizzo da cui ho ricevuto la richiesta e su cui rispondo: "+this.message.getAddress()+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					message=ServerMessageFactory.buildFilesListMessage(msgIdx++, this.message.getAddress(), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN, list);
					manager.sendPacket(message);
					System.out.println("Inviata lista files al bigboss");
					consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata lista files al bigboss");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			if(ServerMessageReader.getCode()==MessageCodeConfiguration.FORWARD_REQ_LIST){//catena: client->relay->bigboss->server
				System.out.println("Ricevuta richiesta lista files, messaggio FORWARD_REQ_LIST");
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Ricevuta richiesta lista files, messaggio FORWARD_REQ_LIST");
				this.bigbossAddress=this.message.getAddress();
				String list="";
				for(int i=0;i<files.length;i++)
				{
					list+=files[i];
					if(i!=files.length-1)
						list+=",";
				}
				try {
					System.out.println("Indirizzo da cui ho ricevuto la richiesta e su cui rispondo: "+this.bigbossAddress+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Indirizzo da cui ho ricevuto la richiesta e su cui rispondo: "+this.bigbossAddress+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					System.out.println("mando a "+this.bigbossAddress+":"+PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					System.out.println("indirizzo client "+ServerMessageReader.getClientAddress());
					message=ServerMessageFactory.buildForwardFilesListMessage(msgIdx++, this.bigbossAddress,PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN, ServerMessageReader.getRelayAddress(),ServerMessageReader.getClientAddress(), list);
					manager.sendPacket(message);
					System.out.println("Inviata lista files al bigboss");
					consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata lista files al bigboss");
					
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
			
			if(ServerMessageReader.getCode()==MessageCodeConfiguration.REQUEST_FILE){
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"SERVER_SESSION_MANAGER: Arrivato un REQUEST_FILE");
				System.out.println("ServerSessionManager si deve QUI procedere con lo streming");				
				String fileRichiesto=ServerMessageReader.getFileName();
				int portaRTPSuCuiInviare=ServerMessageReader.getBigbossStreamingPort();
				DatagramPacket confirm;
			
				System.out.println("porta su cui manderò il flusso rtp "+portaRTPSuCuiInviare);
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"porta su cui manderò il flusso rtp "+portaRTPSuCuiInviare);
				StreamingServer sender = null;
				try {
					System.err.println("INDIRIZZO su cui sparo flusso : "+ServerMessageReader.getPacketAddress().getHostAddress());
					sender = new StreamingServer(fileRichiesto, ServerMessageReader.getPacketAddress().getHostAddress(),ServerMessageReader.getBigbossControlPort(), portaRTPSuCuiInviare, this,consolle);
					//StreamingServer sender = new StreamingServer(fileRichiesto,this.message.getAddress().toString(),ServerMessageReader.getClientPort(), portaRTPSuCuiInviare, this,consolle);
					ssReferences.put(ServerMessageReader.getClientAddress(), sender);
					numberOfSession++;
				} catch (NoDataSourceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				try {
					//confirm = ServerMessageFactory.buildConfirmRequest(msgIdx++,this.message.getAddress(), ServerMessageReader.getClientPort(),portaRTPSuCuiInviare);
					//confirm = ServerMessageFactory.buildConfirmRequest(msgIdx++,this.message.getAddress(), ServerMessageReader.getBigbossPort(), ServerMessageReader.getClientAddress(), ServerMessageReader.getClientPort(), ServerMessageReader.getClientRTPPort());
					//da sistemare, non capisco perchè la risposta la devo mandare sulla porta 9000 e non su quella di bigboss
					//forse perchè il proxy crea una porpia porta, ma non è quella che passo al server col messaggio request file?
//					confirm = ServerMessageFactory.buildConfirmRequest(msgIdx++,this.message.getAddress(), PortConfiguration.PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL, ServerMessageReader.getClientAddress(), ServerMessageReader.getClientPort(), ServerMessageReader.getClientRTPPort(), sender.getTransmissionPort(), sender.getTransmissionControlPort());
					System.err.println("mando la conferma a bigboss "+this.message.getAddress()+":"+ServerMessageReader.getBigbossControlPort());
					confirm = ServerMessageFactory.buildConfirmRequest(msgIdx++,this.message.getAddress(), ServerMessageReader.getBigbossControlPort(),ServerMessageReader.getClientAddress(), ServerMessageReader.getClientRTPPort(), sender.getTransmissionPort(), sender.getTransmissionControlPort());
					manager.sendPacket(confirm);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Inviata conferma dell'avvenuta ricezione della richiesta del file al bigboss "+this.message.getAddress());
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata conferma dell'avvenuta ricezione della richiesta del file al bigboss "+this.message.getAddress());
			}
			
				
			if(ServerMessageReader.getCode() == MessageCodeConfiguration.FORWARD_REQ_FILE){
				DatagramPacket confirm;
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"SERVER_SESSION_MANAGER: Arrivato un FORWARD_REQ_FILE");
				String fileRichiesto=ServerMessageReader.getFileName();
				int portaRTPSuCuiInviare=ServerMessageReader.getBigbossStreamingPort();
				sS=null;
				try {
					//sS = new StreamingServer(fileRichiesto, this.message.getAddress().getHostAddress(), ServerMessageReader.getProxyControlPort(), ServerMessageReader.getProxyReceivingStreamPort(), this, this.consolle);
					sS= new StreamingServer(fileRichiesto, this.message.getAddress().getHostAddress(),ServerMessageReader.getBigbossControlPort(), portaRTPSuCuiInviare, this,consolle);
					ssReferences.put(ServerMessageReader.getClientAddress(), sS);
					numberOfSession++;
				} catch (NoDataSourceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					this.consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"STREAMING_SERVER(COSTRUTTORE): Errore nel DataSource creato...");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
//					confirm = ServerMessageFactory.buildForwardConfirmRequest(msgIdx++, this.message.getAddress(), PortConfiguration.PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL, ServerMessageReader.getRelayAddress(), ServerMessageReader.getRelayControlPort(), ServerMessageReader.getRelayStreamingInPort(),ServerMessageReader.getClientAddress(), ServerMessageReader.getClientPort(),ServerMessageReader.getClientRTPPort(),sS.getTransmissionPort(), sS.getTransmissionControlPort());
					confirm = ServerMessageFactory.buildForwardConfirmRequest(msgIdx++, this.message.getAddress(), PortConfiguration.PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL, ServerMessageReader.getRelayAddress(), ServerMessageReader.getRelayControlPort(), ServerMessageReader.getRelayStreamingInPort(),ServerMessageReader.getClientAddress(), ServerMessageReader.getClientRTPPort(),sS.getTransmissionPort(), sS.getTransmissionControlPort());
					manager.sendPacket(confirm);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Inviata conferma dell'avvenuta ricezione della richiesta del file al bigboss "+this.message.getAddress());
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata conferma dell'avvenuta ricezione della richiesta del file al bigboss "+this.message.getAddress());		
			}
			 	
			
	
			if(ServerMessageReader.getCode() == MessageCodeConfiguration.REDIRECT)
			{
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"SERVER_SESSION_MANAGER: Arrivata una richiesta di REDIRECT");
				this.newRelayAddress = this.message.getAddress().getHostAddress();
				if(!ssReferences.isEmpty())
				{
					Enumeration keys = ssReferences.keys();
					while(keys.hasMoreElements())
					{
						String chiave = keys.nextElement().toString();
						System.out.println(chiave);
						if(!ssReferences.get(chiave).redirect(this.newRelayAddress))
						{
							System.err.println("Errore nella REDIRECT di uno StreamingServer...");
							//Logger.write("Errore nella REDIRECT di uno StreamingServer...");
							consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nella REDIRECT di uno StreamingServer...");
						}
						
					}
				}
			}

		}
	}


	public void updateSessions()
	{
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = "Idle";
		}
	}
	
	
	
}
