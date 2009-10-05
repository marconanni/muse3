/**
 * 
 */
package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

import com.sun.media.rtsp.Timer;
//import java.util.Timer;
//import java.util.TimerTask;

//import com.sun.media.protocol.BufferListener;

import parameters.Parameters;

import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutFileRequest;
import client.timeout.TimeOutSearch;
import client.connection.ClientConnectionFactory;
import client.connection.ClientSessionCM;
import debug.DebugConsole;
/**
 * @author Leo Di Carlo
 *
 */
public class ClientSessionManager implements Observer {

	//private ClientFrameController frameController;
	private DebugConsole console;
	private String filename;
	private ClientSessionCM sessionCM;
	private DatagramPacket msg;
	private String eventType;
	private TimeOutFileRequest timeoutFileRequest;
	private String relayAddress;
	private String status;
	private ClientMessageReader messageReader;
	private ClientElectionManager electionManager;

	private boolean bfSent = true;
	private Timer runner;
	private String newrelay;
	private TimeOutSearch timeoutSearch;
	private int myStreamingPort;
	private int proxyStreamingPort = -1;
	private int proxyCtrlPort = -1;
	private ClientBufferDataPlaying clientPlaying;
	private ClientPortMapper portMapper;
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(ClientElectionManager electionManager) {
		this.electionManager = electionManager;
	}


	public static final ClientSessionManager INSTANCE = new ClientSessionManager();


	/**
	 * @param frameController the frameController to set
	 */
	//public void setFrameController(ClientFrameController frameController) {
		//this.frameController = frameController;
	//}
	
	public void setDebugConsole(DebugConsole console) {
		this.console = console;
	}

	public ClientSessionManager(){
		this.sessionCM = ClientConnectionFactory.getSessionConnectionManager(this);
		this.sessionCM.start();
		this.status = "Idle";
		//this.myStreamingPort = Parameters.CLIENT_PORT_RTP_IN;
	}

	public static ClientSessionManager getInstance() {
		return ClientSessionManager.INSTANCE;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 * Il metodo può essere richiamato da diverse entità all'interno del sistema: ClientElectionManager, ClientBufferDataPlaying ConnectionManager e dai
	 * relativi TimoOut rintracciabili all'interno della TimeOutFactory.
	 * GLi eventi possono essere o avvisi come nel caso dei timeout oppure pacchetti. 
	 * gli eventi possono essere
	 */
	@Override
	public synchronized void update(Observable arg, Object event) {
		// TODO Auto-generated method stub
		this.messageReader = new ClientMessageReader();
		
		if(event instanceof String)
		{
			this.eventType = (String)event;

			if(eventType.equals("TIMEOUTSEARCH")){
				console.debugMessage(Parameters.DEBUG_ERROR,"SCATTATO TIMEOUTSEARCH...");
				this.status = "Idle";
				System.out.println("Time out, no relay found...");
				//this.frameController.debugMessage("SCATTATO TIMEOUTSEARCH...");
				//this.frameController.debugMessage("Relay irraggiungibile...RIPROVARE PIU' TARDI");
			}
			/*if(eventType.equals("TIMEOUTFILEREQUEST")){
				System.err.println("SCATTATO TIMEOUTFILEREQUEST...");
				this.frameController.debugMessage("SCATTATO TIMEOUT_FILE_REQUEST...");

			}*/

			/*if(eventType.equals("BUFFER_FULL") && status.equals("WaitingForPlaying"))
			{
//				frameController.debugMessage("Buffer Full");
//				try {
//					Thread.sleep(2000);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//				if(this.clientPlaying != null)this.clientPlaying.startPlaying();
				status = "Playing";
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStopTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
				sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(eventType.equals("BUFFER_FULL") && status.equals("Playing"))
			{
//				frameController.debugMessage("Buffer Full");
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStopTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
					sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(eventType.equals("BUFFER_EMPTY") && status.equals("Playing"))
			{
//				frameController.debugMessage("Buffer Empty");
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
					sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(eventType.equals("RECIEVED_ELECTION_REQUEST") && this.status.equals("Playing"))
			{
				this.clientPlaying.setThdOnBuffer(Parameters.BUFFER_THS_START_TX + ((Parameters.CLIENT_BUFFER/2)%2 == 0 ? Parameters.CLIENT_BUFFER/2:Parameters.CLIENT_BUFFER/2+1), false);
			}
			if(eventType.equals("EMERGENCY_ELECTION"))
			{
				this.clientPlaying.close();
				this.clientPlaying = null;
				this.relayAddress = null;
				this.frameController.debugMessage("CLIENT SESSION MANAGER: ELEZIONE D'EMERGENZA, SESSIONE INVALIDATA, RIPROVARE PIÙ TARDI CON UNA NUOVA RICHIESTA");
			}
			if(eventType.contains("NEW_RELAY"))
			{
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.newrelay = st.nextToken();
				
			}

			if(eventType.equals("PLAYER_PAUSED"))
			{
				this.status = "Paused";
			}
			if(eventType.equals("PLAYER_STARTED"))
			{
				this.status = "Playing";
			}*/
			/*
			if(eventType.equals("RELAY_FOUND") && this.status.equals("SearchingRelay"))
			{
				this.frameController.debugMessage("RELAY CONTATTATO, INOLTRO LA RICHIESTA...");
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();

				try {
					this.msg = ClientMessageFactory.buildRequestFile(0, this.filename, this.myStreamingPort, InetAddress.getByName(relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
					sessionCM.sendTo(msg);
					this.status = "WaitingForResponse";
					this.timeoutFileRequest = ClientTimeoutFactory.getTimeOutFileRequest(this, Parameters.TIMEOUT_FILE_REQUEST);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			*/
			if(eventType.equals("RELAY_FOUND") && this.status.equals("Idle"))
			{
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();
				this.console.debugMessage(Parameters.DEBUG_INFO,"CLIENT SESSION MANAGER: l'indirizzo del relay trovato da CLIENT ELECION MANAGER è "+this.relayAddress);
				//this.frameController.debugMessage("CLIENT SESSION MANAGER: l'indirizzo del relay trovato da CLIENT ELECION MANAGER è "+this.relayAddress);
			}
			/*if(eventType.equals("END_OF_MEDIA"))
			{
				this.clientPlaying.close();
				this.clientPlaying = null;
				this.electionManager.setImServed(false);
			}*/
		}
		if(event instanceof DatagramPacket)
		{
			this.msg = (DatagramPacket) event;
			try {
				messageReader.readContent(this.msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.err.println("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				console.debugMessage(Parameters.DEBUG_ERROR, "CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				//Logger.write("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			if(messageReader.getCode() == Parameters.SERVER_UNREACHEABLE && status.equals("WaitingForResponse"))
			{
				if(this.timeoutFileRequest != null)this.timeoutFileRequest.cancelTimeOutFileRequest();
				console.debugMessage(Parameters.DEBUG_ERROR, "CLIENT SESSION MESSAGE: SERVER IRRAGGIUNGIBILE");
				//this.frameController.debugMessage("CLIENT SESSION MESSAGE: SERVER IRRAGGIUNGIBILE");
			}
			/*if(messageReader.getCode() == Parameters.ACK_CLIENT_REQ && status.equals("WaitingForResponse"))
>>>>>>> .r15
			{
				if(this.timeoutFileRequest != null)this.timeoutFileRequest.cancelTimeOutFileRequest();
				
				//QUI MI PREPARE A RICEVERE LO STREAMING DA PARTE DEL PROXY CHE é STATO CREATO SUL RELAY A CUI SONO CONNESSO
				//this.proxyStreamingPort = messageReader.getRelaySendingPort();
				//this.proxyCtrlPort = messageReader.getRelayControlPort();
				//try {
					//this.clientPlaying = new ClientBufferDataPlaying(this.proxyStreamingPort, this.myStreamingPort, InetAddress.getByName(relayAddress), Parameters.CLIENT_BUFFER, Parameters.TTW, this, this.frameController);
					/*String[] data = new String[7];
					data[0] = InetAddress.getByName(Parameters.CLIENT_ADDRESS).getHostAddress();
					data[1] = String.valueOf(Parameters.CLIENT_PORT_SESSION_IN);
					data[2] = String.valueOf(this.myStreamingPort);
					data[3] = this.relayAddress;
					data[4] = String.valueOf(this.proxyCtrlPort);
					data[5] = String.valueOf(this.proxyStreamingPort);
					data[6] = this.filename;
					this.frameController.setDataText(data);
			//		this.clientPlaying.preparingSession();
					this.clientPlaying.start();
					*/
					
			//		runner=new Timer();
			//		runner.schedule(new TimerTask(){public void run(){System.out.println("PARTITO IL PLAYER - MULTIPLEXER...PASSATI "+Parameters.PLAYBACK_DELAY_START);clientPlaying.startPlaying();}}, Parameters.PLAYBACK_DELAY_START);
					
					//this.clientPlaying.startPlaying();
					/*this.status = "Playing";
					try {
						this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
						sessionCM.sendTo(msg);
						status = "WaitingForPlaying";
						if(this.electionManager!=null)
							this.electionManager.setImServed(true);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(messageReader.getCode() == Parameters.LEAVE)
			{
				this.relayAddress =this.newrelay;
				this.frameController.setNewRelayIP(this.relayAddress);
				this.clientPlaying.setThdOnBuffer(Parameters.BUFFER_THS_START_TX, true);
				this.clientPlaying.redirectSource(this.relayAddress);
			}*/

		}
	}

	/**
	 * 
	 * @param filename Nome del file da richiedere - esso è staticamente preso dalla classe PARAMETERS
	 */
	public void requestFile(String filename)
	{
		this.filename = filename;
		//this.status = "SearchingRelay";
		
		this.relayAddress = electionManager.getActualRelayAddress();
		
		if(this.relayAddress!=null)
		{
			
			System.out.println("relayAddress: " + this.relayAddress);
			try {
				this.msg = ClientMessageFactory.buildRequestFile(0, this.filename, Parameters.CLIENT_PORT_RTP_IN, InetAddress.getByName(this.relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
				sessionCM.sendTo(msg);
				this.status = "WaitingForResponse";
				this.timeoutFileRequest = ClientTimeoutFactory.getTimeOutFileRequest(this, Parameters.TIMEOUT_FILE_REQUEST);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			this.electionManager.tryToSearchTheRelay();
			console.debugMessage(Parameters.DEBUG_WARNING, "RELAY DISCOVERING...\nSE NON SI RICEVONO NOTIFICHE ENTRO BREVE SI CONSIGLIA DI CAMBIARE POSIZIONE E RIPROVARE.");
			//this.frameController.debugMessage("RELAY DISCOVERING...\nSE NON SI RICEVONO NOTIFICHE ENTRO BREVE SI CONSIGLIA DI CAMBIARE POSIZIONE E RIPROVARE.");
		}
	}

	/*public void bufferFullEventOccurred(BufferFullEvent e) {

		
		if (bfSent) {
			//se non � stato gi� inviato, viene inviato al proxy un messaggio di buffer full
//			controller.bufferControl();
//			setChanged();
//			this.controller.update(this, "BUFFER_FULL");
			this.update(null, "BUFFER_FULL");
			bfSent = false;
		}		
	}
	
	public void bufferEmptyEventOccurred(BufferEmptyEvent e) {
		//LUCA: tolto il ! qui
		if (!bfSent) {
			//se non � stato gi� inviato, viene inviato al proxy un messaggio di buffer full
			this.update(null, "BUFFER_EMPTY");
			//LUCA: messo false qui
			bfSent = true;
		}		
	}*/

	public static void main(String[] args){
		ClientSessionManager.getInstance();
	}

}
