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
import java.util.Timer;
import java.util.TimerTask;

import parameters.BufferConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.Parameters;
import parameters.PortConfiguration;
import parameters.TimeOutConfiguration;

import unibo.core.BufferEmptyEvent;
import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullEvent;
import unibo.core.BufferFullListener;
import util.Logger;

import client.gui.ClientFrameController;
import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutFileRequest;
import client.timeout.TimeOutSearch;
import client.connection.*;

/**
 * @author Leo Di Carlo
 *
 */
public class ClientSessionManager implements Observer, BufferFullListener , BufferEmptyListener{

	private ClientFrameController frameController;
	private String filename;  //Marco: il nome del file della canzone da ascoltare
	private ClientCM sessionCM; //Marco: classe che serve per mandare e ricevere messaggi: in particolare quando riceve messaggi avverte il client session manager chiamando il metodo update ( secondo la modalità tipica del patter observer - Hollywood model)
	private DatagramPacket msg;
	private String eventType;
	private TimeOutSearch timeoutSearch;
	private TimeOutFileRequest timeoutFileRequest;
	private String relayAddress;  //Marco: indirizzo del relay dal quale sta ricevendo lo streaming
	private String status;
	private ClientBufferDataPlaying clientPlaying; // Marco: credo che sia l'entit� che effettivamente si ocupa difare la riproduzione 
	private ClientPortMapper portMapper;
	private int myStreamingPort;	//Marco:la porta dalla quale il client riceve lo streaming
	private int proxyStreamingPort = -1; //Marco: la porta dalla quale il relay eroga lo streaming
	private int proxyCtrlPort = -1; //Marco: la porta di controllo del proxy
	private ClientMessageReader messageReader;
	private ClientElectionManager electionManager;  //Marco: è il manager delle elezioni (quello che manda i beacon)
	private boolean bfSent = true;
	private Timer runner;
	private String newrelay;  //Marco: conterrà l'indirizzo del nuovo relay in caso di rielezione
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(ClientElectionManager electionManager) {
		this.electionManager = electionManager;
	}


	public static final ClientSessionManager INSTANCE = new ClientSessionManager(); //Marco: anche il client session manager è un singleton


	/**
	 * @param frameController the frameController to set
	 */
	public void setFrameController(ClientFrameController frameController) {
		this.frameController = frameController;
	}

	public ClientSessionManager(){
		this.sessionCM = ClientConnectionFactory.getSessionConnectionManager(this,false); //Marco: crea  il manager che manda e riceve messaggi
		this.sessionCM.start(); //Marco: e lo fa partire
		this.status = "Idle";
		this.myStreamingPort = PortConfiguration.CLIENT_PORT_RTP_IN; //Marco: legge dal file di configurazione la prota sulla quale ricevere lo streaming
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
	public synchronized void update(Observable arg, Object event) { //Marco: è il metodo chiamato da clientSessionCM quando arriva un messaggio
		// Auto-generated method stub
		this.messageReader = new ClientMessageReader();
		if(event instanceof String)
		{
			this.eventType = (String)event;

			if(eventType.equals("TIMEOUTSEARCH")){
				System.err.println("SCATTATO TIMEOUTSEARCH...");  //Marco: controlli errore sulla ricezione messaggio
				this.status = "Idle";
				this.frameController.debugMessage("SCATTATO TIMEOUTSEARCH...");
				this.frameController.debugMessage("Relay irraggiungibile...RIPROVARE PIU' TARDI");
			}
			if(eventType.equals("TIMEOUTFILEREQUEST")){			//Marco: qui ho richiesto un file e nessuno mi ha risposto, mi limito a scirverlo a video
				System.err.println("SCATTATO TIMEOUTFILEREQUEST...");
				this.frameController.debugMessage("SCATTATO TIMEOUT_FILE_REQUEST...");

			}

			if(eventType.equals("BUFFER_FULL") && status.equals("WaitingForPlaying")) //Marco: il buffer è pieno e sto aspettando di iniziare a ripreodurre musica
			{
				/* Marco: il comportamento non è chiaro: con il buffer pieno ed in attesa ed il client in attesa di suonare dovrei
				 * iniziare a riprodurre la canzone; il codice commentato ( che ho trovato già commentato) farebbe proprio così.
				 * tuttavia il metodo si limita a cambiare lo stato da WaitingForPlaying a Playing ed a mandare il messaggio
				 * di stopTx al relay che smette di mandare i farmes al client. 
				 * Possibile che la cosa resti in attesa che l'utente pigi il tasto play sull'interfaccia grafica?
				 * Ma allora perchè cambiare lo stato in playing se effettivamente sono sto riproducento?
				
				*/
//				frameController.debugMessage("Buffer Full");
//				try {
//					Thread.sleep(2000);
//				} catch (InterruptedException e1) {
//					// Auto-generated catch block
//					e1.printStackTrace();
//				}
//				if(this.clientPlaying != null)this.clientPlaying.startPlaying();
				status = "Playing";
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStopTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
				sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(eventType.equals("BUFFER_FULL") && status.equals("Playing"))
				/*
				 * Il buffer è pieno e sto suonando: mando al relay il messaggio di stop Tx per dirgli di interrompere  
				 * lo streaming
				 */
			{
//				frameController.debugMessage("Buffer Full");
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStopTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
					sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(eventType.equals("BUFFER_EMPTY") && status.equals("Playing"))
			{
				/*
				 * Sto suonando ed il baffer si è svuotato: mando al relay il messaggio di start TX, che gli fa riprendere
				 * la trasmissione dei frames.
				 */
				
//				frameController.debugMessage("Buffer Empty");
				try {
					if(this.relayAddress!= null || this.proxyCtrlPort != -1)
						this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
					sessionCM.sendTo(msg);
				} catch (UnknownHostException e) {
					// Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(eventType.equals("RECIEVED_ELECTION_REQUEST") && this.status.equals("Playing"))
				/*
				 * Se arriva il messaggio di inizio della fase di rielezione raddoppio il buffer ed alzio la soglia 
				 * sotto la quale chiedere frames al relay, in modo da non restare senza frames durante il successivo handoff
				 * di gestire le azioni che il client deve intraprendere come "elettore" se ne occupa il client election manager
				 * sbirciando nel metodo di clientPlaying sembra che si setti solo la dimensione del buffer e non la soglia,
				 * probabilmente però questa è legata al booleano is normalMode ( l'ultimo parametro inviato) che è settato a false
				 * 
				 */
			{
				this.clientPlaying.setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX + ((BufferConfiguration.CLIENT_BUFFER/2)%2 == 0 ? BufferConfiguration.CLIENT_BUFFER/2:BufferConfiguration.CLIENT_BUFFER/2+1), false);
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
				/*
				 * il dato viene salvato nella variabile newrelay, ma non viene ancora sovreascirtto il dato sull'indirizzo del vecchio relay
				 * nel protocollo studiato all'arrivo dell'ELECTION DONE l'indirizzo del nuovo relay viene solo salvato,
				 * il nuovo relay diventa il relay di riferimento solo quando il client riceve il messaggio LEAVE da parte del
				 * vecchio relay
				 */
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
			}
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
					// Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}
			*/
			if(eventType.equals("RELAY_FOUND") && this.status.equals("Idle"))
			{
				/*
				 * il client è idle e viene trovato un relay
				 * viene salvato l'indirizzo del rely nella variabile realy adress
				 * il realy trovato diventa quindi il relay di riferimento per il client che era "orfano"
				 */
				StringTokenizer st = new StringTokenizer(eventType, ":");
				st.nextToken();
				this.relayAddress = st.nextToken();
				this.frameController.debugMessage("CLIENT SESSION MANAGER: l'indirizzo del relay trovato da CLIENT ELECION MANAGER è "+this.relayAddress);
			}
			if(eventType.equals("END_OF_MEDIA"))
				/*
				 * è finita la canzone: chiudo il player e comunico all'election manager che non sto ricevendo alcuna canzone
				 */
			{
				this.clientPlaying.close();
				this.clientPlaying = null;
				this.electionManager.setImServed(false);
			}
		}
		if(event instanceof DatagramPacket)
		{
			this.msg = (DatagramPacket) event;
			try {
				messageReader.readContent(this.msg);
			} catch (IOException e) {
				// Auto-generated catch block
				System.err.println("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				Logger.write("CLIENT_SESSION_MANAGER: Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			if(messageReader.getCode() == MessageCodeConfiguration.SERVER_UNREACHEABLE && status.equals("WaitingForResponse"))
			{
				/*
				 * scopro che il server dal quale aspetto una risposta non è raggiungibile, annullo il timeout e
				 * scrivo la causa della impossibilità di connessione dulla consolle
				 */
				if(this.timeoutFileRequest != null)this.timeoutFileRequest.cancelTimeOutFileRequest();
				this.frameController.debugMessage("CLIENT SESSION MESSAGE: SERVER IRRAGGIUNGIBILE");
			}
			if(messageReader.getCode() == MessageCodeConfiguration.ACK_CLIENT_REQ && status.equals("WaitingForResponse"))
				/*
				 * arriva la risposta alla richiesta della canzone, 
				 * recupero i dati sulle porte utilizzate dal proxy sul relay
				 * qui creo il player e il controller dell'interfaccia grafica.
				 * In pratica faccio partire il sistema di riproduzione in streaming della canzone
				 */
			{
				if(this.timeoutFileRequest != null)this.timeoutFileRequest.cancelTimeOutFileRequest();
				this.proxyStreamingPort = messageReader.getRelaySendingPort();
				this.proxyCtrlPort = messageReader.getRelayControlPort();
				try {
					this.clientPlaying = new ClientBufferDataPlaying(this.proxyStreamingPort, this.myStreamingPort, InetAddress.getByName(relayAddress), BufferConfiguration.CLIENT_BUFFER, TimeOutConfiguration.TTW, this, this.frameController);
					String[] data = new String[7];
					data[0] = InetAddress.getByName(NetConfiguration.CLIENT_ADDRESS).getHostAddress();
					data[1] = String.valueOf(PortConfiguration.CLIENT_PORT_SESSION_IN);
					data[2] = String.valueOf(this.myStreamingPort);
					data[3] = this.relayAddress;
					data[4] = String.valueOf(this.proxyCtrlPort);
					data[5] = String.valueOf(this.proxyStreamingPort);
					data[6] = this.filename;
					this.frameController.setDataText(data);
			//		this.clientPlaying.preparingSession();
					this.clientPlaying.start();
					
					
			//		runner=new Timer();
			//		runner.schedule(new TimerTask(){public void run(){System.out.println("PARTITO IL PLAYER - MULTIPLEXER...PASSATI "+Parameters.PLAYBACK_DELAY_START);clientPlaying.startPlaying();}}, Parameters.PLAYBACK_DELAY_START);
					
					//this.clientPlaying.startPlaying();
					this.status = "Playing";
					try {
						this.msg = ClientMessageFactory.buildStartTX(0, InetAddress.getByName(relayAddress), this.proxyCtrlPort);
						sessionCM.sendTo(msg);
						status = "WaitingForPlaying";
						if(this.electionManager!=null)
							this.electionManager.setImServed(true);
					} catch (IOException e) {
						// Auto-generated catch block
						e.printStackTrace();
					}
				} catch (UnknownHostException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(messageReader.getCode() == MessageCodeConfiguration.LEAVE)
				/*
				 * cambio indirizzo del realy di riferimeno ( lo diventa il nuovo relay)
				 * imposto le dimensioni e la modalità del buffer su funzionamento normale
				 * e chiamo il metodo di client Playing affinchè consideri il nuovo relay come fonte dello stream
				 */
			{
				this.relayAddress =this.newrelay;
				this.frameController.setNewRelayIP(this.relayAddress);
				this.clientPlaying.setThdOnBuffer(BufferConfiguration.BUFFER_THS_START_TX, true);
				this.clientPlaying.redirectSource(this.relayAddress);
			}

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
				this.msg = ClientMessageFactory.buildRequestFile(0, this.filename, this.myStreamingPort, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
				sessionCM.sendTo(msg);
				this.status = "WaitingForResponse";
				this.timeoutFileRequest = ClientTimeoutFactory.getTimeOutFileRequest(this, TimeOutConfiguration.TIMEOUT_FILE_REQUEST);
			} catch (UnknownHostException e) {
				// Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			this.electionManager.tryToSearchTheRelay();
			this.frameController.debugMessage("RELAY DISCOVERING...\nSE NON SI RICEVONO NOTIFICHE ENTRO BREVE SI CONSIGLIA DI CAMBIARE POSIZIONE E RIPROVARE.");
		}
	}

	public void bufferFullEventOccurred(BufferFullEvent e) {

		
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
	}

	public static void main(String[] args){
		ClientSessionManager.getInstance();
	}

}
