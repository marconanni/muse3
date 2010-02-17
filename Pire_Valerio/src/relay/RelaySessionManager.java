/**
 * 
 */
package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

import javax.media.rtp.event.NewParticipantEvent;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.TimeOutConfiguration;

import parameters.PortConfiguration;

import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.connection.RelayPortMapper;

import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutSingleWithMessage;


//import javax.media.NoDataSourceException;


import debug.*;

//import server.StreamingServer;
//import util.Logger;

/**
 * @author Leo Di Carlo
 *
 */
public class RelaySessionManager implements Observer{
	private RelaySessionStatus status;
	private int numberOfSession;
	//private Hashtable<String, Proxy> pReferences; // Marco: tabella che contiene per ogni sessione ( identificata dall'indirizzo del client) il proxy che la serve
	//private Hashtable<String, int[]> sessionInfo;
	private Hashtable<String, Session> sessions; // tablella che contiene le info sulle sessioni: la chiave � l'Ip del cliente finale che ascolta la musica ( anche se mediato da un relay secondario) - per maggiori info guardare la classe Session
	/*
	 * nota: nei commenti si parla di indirizzo inferiore per indicare l'indirizzo che un relay ha sul suo cluster (LocalClusterAddress)
	 * 		e di indirizzo superiore per indicare l'indirizzo che un relay ha nel cluster di chi gli eroga il flusso ( big boss se relay, server se big boss)-> ClusterHeadAddress
	 * 
	 * Marco: composizione della tabella sessionInfo ( ancora non verificato, mi baso sui nomi che vengono assegnati)
	 * chiave: indirizzo del client
	 * valore[0]=  porta dalla quale il server eroga lo stream
	 * valore[1] = porta sulla quale il proxy riceve lo stream
	 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
	 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
	 * valore[4] = porta di controllo del server
	 * valore[5] = porta di controllo del proxy
	 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
	 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
	 */
	private DatagramPacket message;
	public static final RelaySessionManager INSTANCE = new RelaySessionManager();
	private boolean imRelay;
	private boolean imBigBoss;
	private String clientAddress;
	//numero di ritrasmissioni del pacchetto REQUEST_SESSION prima di scatenare un elezione d'emergenza
	private int numberOfRetrasmissions = 1;
	private String maxWnextRelay;
	private String event;
	private DebugConsole consolle;
	private String relayAddress;
	private RelayElectionManager electionManager;

//X Pire: non so come hai chiamato i timeout
//	private TimeOutSingleWithMessage toSessionRequest;
//	private TimeOutSingleWithMessage toAckSessionInfo;
//	private TimeOutSingleWithMessage toSessionInfo;
	
	
	private RelayCM sessionCMClusterHead; // Marco: è chi si occupa di spedire i messaggi.
	private RelayCM sessionCMCluster;
	private RelayMessageReader messageReader;
	
	private boolean isBigBoss;//Valerio: questo flag viene settato in base al metodo che ha Pire in ElectionManager
	private int clientSessionPort;
//	private String serverAddress;
	private int serverPortSessionIn;
	private int seqNumSendServer;
	private int seqNumSendClient;
	private int seqNumSendBigBoss;
	private int seqNumSendRelay;
	private String listaFile;
	private String[] files;
	
//	private String bigbossAddress;
	private int bigbossPort;
	
	private int clientStreamingPort;
	private String fileName;
	
	
	private String localClusterAddress=null;			//indirizzo locale (interfacciato sul CLUSTER)
	private String localClusterHeadAddress = null;			//indirizzo locale (interfacciato sul CLUSTER HEAD)
	private String connectedClusterHeadAddress = null;
	private String oldRelayLocalClusterAddress;
	private String oldRelayClusterHeadAddress;
	
	//stati in cui si può trovare il RelayElectionManager
	public enum RelaySessionStatus {  
		IDLE_BIGBOSS,
		ACTIVE_BIGBOSS,
		ATTENDIGACKSESSION,
		IDLE_NORMAL,
		ACTIVE_NORMAL,
		WAITING,
		NOTHING,
		AttendingRequestSession,
		RequestingSession
		}
	
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(RelayElectionManager electionManager) {
		//mi vado a prendere i tre indirizzi con cui un relay ha a che fare
		//affermazione: quando vado mi servono passo dall'election manager, non uso questi
		this.electionManager = electionManager;
		localClusterAddress=electionManager.getLocalClusterAddress();
		localClusterHeadAddress=electionManager.getLocalClusterHeadAddress();
		connectedClusterHeadAddress=electionManager.getConnectedClusterHeadAddress();
		System.out.println("localClusterAddress = "+localClusterAddress+", localClusterHeadAddress = "+localClusterHeadAddress+", connectedClusterHeadAddress = "+connectedClusterHeadAddress);
		consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"localClusterAddress = "+localClusterAddress+", localClusterHeadAddress = "+localClusterHeadAddress+", connectedClusterHeadAddress = "+connectedClusterHeadAddress);
		}

	public RelaySessionManager()
	{
		this.numberOfSession = 0;
//		pReferences = new Hashtable();
//		sessionInfo = new Hashtable();
		this.sessions= new Hashtable<String, Session>();
		this.sessionCMCluster = RelayConnectionFactory.getClusterSessionConnectionManager(this);
		this.sessionCMClusterHead = RelayConnectionFactory.getClusterHeadSessionConnectionManager(this);
		this.messageReader = new RelayMessageReader();
		this.sessionCMCluster.start();
		this.sessionCMClusterHead.start();
		this.consolle=new DebugConsole("RELAY SESSION MANAGER DEBUG CONSOLLE");
		
		
		
		imBigBoss=electionManager.isBIGBOSS();
		imRelay=electionManager.isRELAY();//significa relay secondario
		
		serverPortSessionIn=PortConfiguration.SERVER_SESSION_PORT_IN;
		bigbossPort=PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN;
		
		seqNumSendServer=0;//numero datagrammi inviati al server
		seqNumSendClient=0;// "" 		""			""	"	client
		seqNumSendBigBoss=0;
		seqNumSendRelay=0;
		
		System.out.println("RelaySessionManager è partito");
	}

	public static RelaySessionManager getInstance() {
		return RelaySessionManager.INSTANCE;
	}


	/**
	 * @return the imRelay
	 */
//	public boolean isImRelay() {
//		return imRelay;
//	}
	
	/**
	 * @return the imBigBoss
	 */
//	public boolean isImBigBoss() { //Questo metodo lo fornisce pire
//		return imBigBoss;
//	}

	/**
	 * @param imBigBoss imRelay the imBigBoss, imRelay to set
	 */
	public void setImRelay(boolean imBigBoss,boolean imRelay) {
		this.imRelay = imRelay;
		this.imBigBoss = imBigBoss;
		if(this.imRelay && this.imBigBoss)
			status = RelaySessionStatus.IDLE_BIGBOSS;
		else if(this.imRelay && !this.imBigBoss)
			status = RelaySessionStatus.IDLE_NORMAL;
		else if(!this.imRelay && !this.imBigBoss)
			status = RelaySessionStatus.WAITING;
		else
			status = RelaySessionStatus.NOTHING;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public synchronized void update(Observable receiver, Object arg) {
	Proxy proxy;
	/**
	 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * +++++++++++++++++++MESSAGGI RICEVUTI ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 */
	System.out.println("Tipo di Classe dell'arg: "+ arg.getClass());
	if(arg instanceof DatagramPacket)
	{
		/**
		 * un messaggio è appena arrivato e richiamo il reader per la lettura dello stesso
		 */
		this.message = (DatagramPacket) arg;
		try {
			messageReader.readContent(this.message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
			consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
			e.printStackTrace();
		}
		
		System.err.println("E' arrivato un messaggio con codice: "+messageReader.getCode()+", imRelay: "+imRelay+", imBigBoss: "+imBigBoss);

		/**
		 * arrivato messaggio di richiesta da parte del client
		 */
		
		/*Marco: è arrivata una richiesta da parte di un client ed io sono il relay.
		 * creo un nuovo proxy che gestisce lo stream verso il client
		 * aggiungo il proxy alla tabella dei proxy usando l'indirizzo del client come chiave per la sessione
		 * ed aumento il numero di sessioni
		*/
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_LIST && imBigBoss){//Valerio: aggiunto da me
			System.out.println("codice request_list e sono il relay bigboss");
			this.clientAddress = message.getAddress().getHostAddress();
			System.out.println("Arrivata la richiesta della lista file da "+ this.clientAddress+" devo inviarla al server: "+electionManager.getConnectedClusterHeadAddress()+" sulla porta "+this.serverPortSessionIn+" e la risposta andrà inviata al client sulla porta "+PortConfiguration.CLIENT_PORT_SESSION_IN);
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+ this.clientAddress+" devo inviarla al server: "+electionManager.getConnectedClusterHeadInetAddress()+" sulla porta "+this.serverPortSessionIn+" e la risposta andrà inviata al client sulla porta "+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
//				this.message=RelayMessageFactory.buildRequestList(seqNumSendServer++, InetAddress.getByName(this.serverAddress), this.serverPortSessionIn, this.clientAddress);
				this.message=RelayMessageFactory.buildRequestList(seqNumSendServer++, InetAddress.getByName(electionManager.getConnectedClusterHeadAddress()), this.serverPortSessionIn, this.clientAddress);//messaggio modificato prendendo l'indirizzo coi metodi di Pire
				sessionCMClusterHead.sendTo(this.message);
				System.out.println("BigBoss: inviato il messaggio request list al server");
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata al server la richiesta lista file");
		}
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_LIST && imRelay ){//Valerio: aggiunto da me	
			//se non è bigboss dovrò creare un messaggio di tipo FORWARD_REQUEST_LIST
			System.out.println("codice request_list e sono un relay normale");
			this.clientAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+ this.clientAddress+" devo inviarla al big boss: "+electionManager.getConnectedClusterHeadInetAddress()+" sulla porta "+this.bigbossPort+" e la risposta andrà inviata al client sulla porta "+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
//				this.message=RelayMessageFactory.buildForwardRequestList(seqNumSendBigBoss++, InetAddress.getByName(this.bigbossAddress), this.bigbossPort,null, this.clientAddress);
				this.message=RelayMessageFactory.buildForwardRequestList(seqNumSendBigBoss++, electionManager.getConnectedClusterHeadInetAddress(), this.bigbossPort,null, this.clientAddress);//messaggio modificato prendendo l'indirizzo coi metodi di Pire
				sessionCMClusterHead.sendTo(this.message);
				System.out.println("Relay: inviato il messaggio forward request list al bigboss");
				}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Relay: inviato il messaggio forward request list al bigboss");
		}
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.LIST_RESPONSE && imBigBoss){
			//un messaggio di tipo List_Response può arrivare solo al bigboss
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"E' arrivata la lista dei file");
			listaFile=messageReader.getListaFile();
			files = listaFile.split(",");
			System.out.println("Lista file:");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Lista File:");
			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i]);
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,files[i]);
			}
			try {
				this.message=RelayMessageFactory.buildListResponse(seqNumSendClient++,InetAddress.getByName(messageReader.getClientAddress()), PortConfiguration.CLIENT_PORT_SESSION_IN, listaFile);
				sessionCMCluster.sendTo(this.message);
				System.out.println("inviata la lista dei file al client");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_REQ_LIST&&imBigBoss){
			//allora questo è bigboss che deve inoltrare tutto al server
			System.out.println("codice forward_request_list quindi sono un relay");
			this.relayAddress=message.getAddress().getHostAddress();
			this.clientAddress=messageReader.getClientAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+this.relayAddress+" devo inviarla al server: "+electionManager.getConnectedClusterHeadInetAddress()+" sulla porta "+serverPortSessionIn+" e la risposta andrà inviata al client "+this.clientAddress+":"+PortConfiguration.CLIENT_PORT_SESSION_IN);
			System.out.println("Arrivata la richiesta della lista file da "+this.relayAddress+" devo inviarla al server: "+electionManager.getConnectedClusterHeadInetAddress()+" sulla porta "+serverPortSessionIn+" e la risposta andrà inviata al client "+this.clientAddress+":"+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
				this.message=RelayMessageFactory.buildForwardRequestList(seqNumSendBigBoss++, electionManager.getConnectedClusterHeadInetAddress(), serverPortSessionIn,this.relayAddress, this.clientAddress);
				sessionCMClusterHead.sendTo(this.message);
				System.out.println("Relay: inviato il messaggio request list al BigBoss");
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata al BigBoss la richiesta lista file");
		}
		
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_REQ_FILE&&imBigBoss){
			//allora questo è il bigboss e deve creare un proxy e girare la richiesta al server
			//questo bigboss sta servendo un relay visto che è arrivata il messaggio di forward
			this.status =RelaySessionStatus.ACTIVE_BIGBOSS;
			this.relayAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" dal RELAY: "+ this.relayAddress);
			System.out.println("E' arrivato un REQUEST_FILE dal Relay, ora creo il proxy");
			//Valerio: non so ne nel proxy va cambiato qualcosa o se va bene anche se ad inviare al client invio ad un relay
			//ovviamente gli passo l'indirizzo del relay e non quello del client
			proxy = new Proxy(this, true, messageReader.getFilename(), this.relayAddress, messageReader.getRelayControlPort(),messageReader.getRelayStreamingInPort(), messageReader.getClientAddress(), messageReader.getClientStreamingPort() ,electionManager.isBIGBOSS(),false,electionManager.getLocalClusterHeadAddress(),electionManager.getConnectedClusterHeadAddress());
			
			Session sessione=new Session(messageReader.getClientAddress(), proxy, this.relayAddress);
			this.sessions.put(messageReader.getClientAddress(), sessione);
//			pReferences.put(this.relayAddress, proxy);
			this.numberOfSession++;
		}
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_LIST_RESPONSE&&imBigBoss){
			//sono bigboss devo quindi mandare il messaggio di forward list response al relay
			System.out.println("Sono BigBoss, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Relay: "+messageReader.getRelayAddress());
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Sono BigBoss, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Relay: "+messageReader.getRelayAddress());
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"E' arrivata la lista dei file");
			listaFile=messageReader.getListaFile();
			files = listaFile.split(",");
			System.out.println("Lista file:");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Lista File:");
			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i]);
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,files[i]);
			}
			try {
				this.message=RelayMessageFactory.buildForwardListResponse(seqNumSendRelay++, InetAddress.getByName(messageReader.getRelayAddress()),PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN, messageReader.getRelayAddress(),this.messageReader.getClientAddress(), listaFile);
				sessionCMCluster.sendTo(this.message);
			}catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Sono BigBoss, ho inviato la lista file al Relay");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Sono BigBoss, ho inviato la lista file al Relay");
		}
		
		
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_LIST_RESPONSE && imRelay){
			System.out.println("Sono un Relay, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Client");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Sono un Relay, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Client");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"E' arrivata la lista dei file");
			listaFile=messageReader.getListaFile();
			files = listaFile.split(",");
			System.out.println("Lista file:");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Lista File:");
			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i]);
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,files[i]);
			}
			this.clientAddress=messageReader.getClientAddress();
			try {
				this.message=RelayMessageFactory.buildListResponse(seqNumSendClient++,InetAddress.getByName(this.clientAddress), PortConfiguration.CLIENT_PORT_SESSION_IN, listaFile);
				sessionCMCluster.sendTo(this.message);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Sono un Relay, ho inviato la lista file al Client");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Sono un Relay, ho inviato la lista file al Client");
		}
					
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_FILE && imBigBoss){
			//se il request file arriva al bigboss ho questo comportamento
			System.out.println("arrivata richiesta file e sono bigboss, vado a creare il proxy");
			this.status = RelaySessionStatus.ACTIVE_BIGBOSS;
			this.clientAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" da "+ this.clientAddress);
			System.out.println("E' arrivato un REQUEST_FILE, ora creo il proxy");
			proxy = new Proxy(this, true, messageReader.getFilename(), null,-1,-1 ,this.clientAddress, messageReader.getClientStreamingPort(),imBigBoss,true,electionManager.getLocalClusterHeadAddress(),electionManager.getConnectedClusterHeadAddress());
			Session sessione=new Session(this.clientAddress, proxy);
			this.sessions.put(this.clientAddress, sessione);
//			pReferences.put(this.clientAddress, proxy);
			this.numberOfSession++;
			}
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_FILE && imRelay){
			//allora essendo relay devo inoltrare un forwardrequestfile al bigboss
			System.out.println("arrivata richiesta file e NON sono bigboss, vado a creare il proxy");
			this.status = RelaySessionStatus.ACTIVE_NORMAL;
//			this.clientAddress = message.getAddress().getHostAddress();
			this.clientAddress=message.getAddress().getHostAddress();
			this.clientStreamingPort=messageReader.getClientStreamingPort();
			this.fileName=messageReader.getFilename();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+this.fileName+" da "+ this.clientAddress);
			System.out.println("E' arrivato un REQUEST_FILE, ora creo il proxy");
			proxy = new Proxy(this, true, this.fileName,null,-1,-1, this.clientAddress, this.clientStreamingPort,imBigBoss,true,electionManager.getLocalClusterHeadAddress(),electionManager.getConnectedClusterHeadAddress());
//			pReferences.put(this.relayAddress, proxy);
			Session sessione=new Session(this.clientAddress, proxy);
			this.sessions.put(this.clientAddress, sessione);
			this.numberOfSession++;
			}

		/**
		 * gestito l'arrivo della richiesta di passaggio della sessione da parte del nuovo relay appena eletto
		 */
		
		// TODO arrivo request session dal nuovo relay

		/**
		 * gestito l'arrivo della richiesta di passaggio della sessione da parte del nuovo relay appena eletto
		 */
		
		/* Marco:
		 * manda al nuovo relay i dati relativi a tutte le sessioni aperte rispondendo con il messaggio sessioninfo che non si vede
		 *  perchè si è incapsulato il tutto in un metodo stratico della classe Relay message factory
		 *  successivamente cambio lo stato in Attendingack session, ossia il successivo messaggio che il vecchio relay
		 *  deve ricevere nel protocollo di scambio sessione.
		 *  
		 *  Il messaggio SessionInfo contiene per ogni sessione: 
		 *  	l'indirizzo del client
		 *  	le porte di streaming ( per maggiori informazioni vedi la classe Session)
		 *  	l'eventuale indirizzo del relay secondario
		 *  
		 */
		
		
		// unico dubbio: serve proprio il controllo sullo stato del SessionManager?
		
		
		if(messageReader.getCode() == MessageCodeConfiguration.REQUEST_SESSION && this.status==RelaySessionStatus.AttendingRequestSession )
		{
			//TIMEOUT COMMENTATI
//			if(toSessionRequest!=null) // Marco: viene disattivato il timeout request session: il messaggio è arrivato
//				toSessionRequest.cancelTimeOutSessionRequest();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: ricevuto SESSION_REQUEST dal nuovo RELAY");
			try {
				if(this.sessions != null || !this.sessions.isEmpty())
				{					
					this.message = RelayMessageFactory.buildSessionInfo(0, sessions, InetAddress.getByName(this.maxWnextRelay), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
					
				}
				// se sessions � vuoto passo come parametro null: build sessioninfo creer� un pacchetto senza le indicazioni delle sessioni, tuttavia non dovrei finirci se non ho sessioni attive...
				else{this.message = RelayMessageFactory.buildSessionInfo(0, null, InetAddress.getByName(this.maxWnextRelay), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);}

				this.sessionCMCluster.sendTo(this.message); //Marco: invio il messaggio preparato
				this.status = RelaySessionStatus.ATTENDIGACKSESSION;
				//TIMEOUT COMMENTATI
//				this.toAckSessionInfo = RelayTimeoutFactory.getTimeOutAckSessionInfo(this, TimeOutConfiguration.TIMEOUT_ACK_SESSION_INFO);
			} catch (UnknownHostException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		
		// TODO arrivo ACK_SESSION dal nuovo relay
		if(messageReader.getCode() == MessageCodeConfiguration.ACK_SESSION && this.status.equals(RelaySessionStatus.ATTENDIGACKSESSION))
			
			/*
			 * il nuovo relay mi ha mandato il messaggio di ACK_SESSION: significa che il nuovo relay che già ottenuto i dati relativi
			 * alle sessioni: 
			 *  ridirigono il flusso sui recovery buffer dei proxy sul nuovo relay
			 *  Una volta svuotati tutti i buffer di tutti i proxy questi  mandano il messaggio di LEAVE a tutti i client serviti
			 */
		{
			//TIMEOUT COMMENTATI
//			if(this.toAckSessionInfo!=null)
//				toAckSessionInfo.cancelTimeOutAckSessionInfo();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Ricevuto ACK_SESSION dal nuovo RELAY");
			// Marco: il metyodo change proxy Session invoca il metodo Start handoff di ogni proxy che, in sostanza dovrebbe gestisce
			// la redirezione del flusso verso il nuovo relay ( per i dettagli guarda i commenti ai metodi).
			// il metodo getProxyInfo restituisce una tabella che ha per ogni chiave ( l'ip del client) la porta sulla
			//quale ridirigere il flusso.
			
			this.changeProxySession(messageReader.getProxyInfo()); 
		}
		
		// TODO arrivo messaggio SESSION_INFO da parte del vecchio relay
		if(messageReader.getCode() == MessageCodeConfiguration.SESSION_INFO && this.status.equals(RelaySessionStatus.RequestingSession))
		{
			
			/*
			 * fa parte del nuovo relay: arrivano i dati del messaggio Session Info 
			 * Marco: composizione della tabella sessionInfo ( ancora non verificato, mi baso sui nomi che vengono assegnati)
			 * chiave: indirizzo del client
			 * valore[0]=  porta dalla quale il server eroga lo stream
			 * valore[1] = porta sulla quale il proxy riceve lo stream
			 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
			 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
			 * valore[4] = porta di controllo del server
			 * valore[5] = porta di controllo del relay
			 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
			 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
			 *  sfruttando il metodo createProxyfromSession creo i nuovi proxy dai dati del messaggio SessionInfo 
			 *  
			 *
			 */
			
			/*
			 * Marco: successivamente mando il messaggio di ack session al vecchio relay
			 * sembra che a mandare il messaggio di REDIRECT al server perchè questi dirotti il flusso sul nuovo relay sia 
			 * ogni nuovo proxy relativamente alla stessa sessione
			 */
			//TIMEOUT COMMENTATI
//			this.toSessionInfo.cancelTimeOutSessionInfo();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Ricevuto SESSION_INFO dal vecchio RELAY");
			 // creo le sessioni ggio dal messa attualmente per� non ci sono i proxy, li metto con il 
			//metodo
			this.sessions = messageReader.getSessions();//X MARCO!!! QUESTO L'HO COMMENTATO PER FARE LE PROVE, PERCHÈ MI DAVA ERRORE!!
			String proxyInfo = this.createProxyFromSession(sessions); // Nota: proxyInfo contiene le porte di recovery dei relay sostituivi sulle quali ridirigere le varie sessioni
			try {
				this.message = RelayMessageFactory.buildAckSession(0, proxyInfo, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
				this.sessionCMCluster.sendTo(this.message);
				if(electionManager.isBIGBOSS())
					this.status = RelaySessionStatus.ACTIVE_BIGBOSS;
				else
					this.status=RelaySessionStatus.ACTIVE_NORMAL;
			} catch (UnknownHostException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
	/**
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * ++++++++++++++++++++++++++++++EVENTI ARRIVATI -- STRINGHE ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 */
	if(arg instanceof String)
	{
		this.event = (String)arg;
		if(this.event.equals("End_Of_Media") && status.equals(RelaySessionStatus.ACTIVE_BIGBOSS))
		{
			/*
			 * fine di una canzone 
			 * tolgo le informazioni relative alla sessione terminata dalle relative tabelle 
			 * (sia quella sessione- proxy che quella sessione--sessioninfo)
			 * se non mi restano altre sessioni attive ( le tabelle sono quindi vuote) metto il Relay in stato di Idle
			 */
			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER BigBoss: Evento di END_OF_MEDIA da parte di un proxy");
			proxy = (Proxy)receiver;
			//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
			//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
			// c'� una sola sessione per ogni proxy. trovo la sesione corrispondente al proxy e l'elimino
			/* a differenza della versione precendenet non posso usare il ClientAddress del proxy: potrebbe
			 * anche essere l'indirizzo del relay secondario al quale eroga lo streaming: 
			 * un proxy difatti non sa se sta erogando il flusso ad un client o ad un relay secondario
			*/
			Enumeration<String> keys = sessions.keys();
			while(keys.hasMoreElements()){
				String chiave = keys.nextElement();
				Session sessione = sessions.get(chiave);
				if ( sessione.getProxy().equals(proxy)){
					sessions.remove(chiave);
					// in teoria qui ci andrebbe un break, non ha senso ciclare per tutti gli altri elementi
					// per stare dalla parte dei bottoni lascio tutto cos�.
				}
			}			
			this.numberOfSession--;
			if(numberOfSession == 0){
				this.status = RelaySessionStatus.IDLE_BIGBOSS;
			}
//			pReferences.remove(proxy.getClientAddress());
//			sessionInfo.remove(proxy.getClientAddress());
		}
		
		if(this.event.equals("End_Of_Media") && status.equals("ACTIVE_NORMAL"))
		{
			/*
			 * fine di una canzone 
			 * tolgo le informazioni relative alla sessione terminata dalle relative tabelle 
			 * (sia quella sessione- proxy che quella sessione--sessioninfo)
			 * se non mi restano altre sessioni attive ( le tabelle sono quindi vuote) metto il Relay in stato di Idle
			 */
			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER Relay: Evento di END_OF_MEDIA da parte dal proxy del bigboss");
			proxy = (Proxy)receiver;
			//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
			//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
			// c'� una sola sessione per ogni proxy. trovo la sesione corrispondente al proxy e l'elimino
			/* a differenza della versione precendenet non posso usare il ClientAddress del proxy: potrebbe
			 * anche essere l'indirizzo del relay secondario al quale eroga lo streaming: 
			 * un proxy difatti non sa se sta erogando il flusso ad un client o ad un relay secondario
			*/
			Enumeration<String> keys = sessions.keys();
			while(keys.hasMoreElements()){
				String chiave = keys.nextElement();
				Session sessione = sessions.get(chiave);
				if ( sessione.getProxy().equals(proxy)){
					sessions.remove(chiave);
					// in teoria qui ci andrebbe un break, non ha senso ciclare per tutti gli altri elementi
					// per stare dalla parte dei bottoni lascio tutto cos�.
				}
			}			
			this.numberOfSession--;
			if(numberOfSession == 0){
				this.status = RelaySessionStatus.IDLE_NORMAL;
			}
//			pReferences.remove(proxy.getClientAddress());
//			sessionInfo.remove(proxy.getClientAddress());
		}
		
		/**
		 * l'electionmanager mi ha comunica chi è il relay attuale a seguito di un messaggio di WHO_IS_RELAY
		 */
		if(this.event.contains("RELAY_FOUND"))
		{
			StringTokenizer st = new StringTokenizer(this.event, ":");
			st.nextToken();
			this.relayAddress = st.nextToken();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Evento di RELAY_FOUND, il RELAY attuale è: "+this.relayAddress);
		}
		
		if(this.event.equals("TIMEOUTSESSIONREQUEST") || this.event.equals("TIMEOUTACKSESSIONINFO")) //VECCHIO RELAY: il nuovo relay non ha risposto
		{
//TIMEOUT COMMENTATI CONROLLARE
//			if(this.event.equals("TIMEOUTSESSIONREQUEST") && status.equals("Active"))
//				consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_REQUEST");
//			if(this.event.equals("TIMEOUTACKSESSIONINFO") && status.equals("AttendingAckSession"))
//				consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_ACK_SESSION_INFO");
//			//TODO DEVO AVVISARE IL RELAYELECTIONMANAGER
//			/*
//			 * Marco: in sostanza, se il nuovo relay non risponde, cerco di nominare il secondo
//			 * classificato alle elezioni
//			 * 
//			 */
//			if(this.electionManager!=null)
//			{
//				electionManager.chooseAnotherRelay();
//			}

		}
		
		/**
		 * l'electionmanager mi comunica chi è il relay appena eletto
		 */
		/* Nella vecchia versione era cos�
		 *se  io sono il relay attualmente attivo salvo il nome di chi ha vinto nella variabile maxWnextRelay
		 *		cambio stato indicando che non sono più io il relay di riferimento della rete 
		 *		e faccio partire il timeout mentre attendo il messaggio di REQUEST SESSION da parte dell'altro relay
		 *
		 *se invece non sono il relay attualmente attivo confronto l'indirizzo del vincitore con il mio: se ho vinto
		 *	cambio il mio stato per indicare che sono il relay attivo, 
		 *	mando il messaggio di Session Request al vecchiorelay 
		 *	e mi metto in attesa delle informazioni sulle sessioni
		 *
		 */
		// ******E' UN GRAN CASINO! RIPENSA TUTTO CON CALMA!*****//
		// TODO "NEW_RELAY " l'elctionManager mi comunica chi ha vinto l'elezione
		if(this.event.contains("NEW_RELAY")) 
			/*
			 * NOTA: indirizzo inferiore = indirizzo all'interno del cluster"inferiore"
			 * 		indirizzo superiore = indirizzo all'interno del cluster "superiore"
			 * 
			 * Marco: l'election manager quando c'� un election done scatena un evento che mi fa ricevere la stringa 
			 * NEW_RELAY:ip del vincitore dell'elezione:ip del vecchio relay sul suo cluster:ip del vecchio relay sul cluster superiore:ip del nodo che eroga ilflusso al vecchio relay( e che lo erogherò anche al nuovo)
			 * 
			 * Cosa fare dipende dal ruolo che il nodo ha in quel momento:
			 * vecchio relay, vicitore dell'elezione, relay secondario che  o candidato che non ha vinto.
			 * 
			 * In ogni modo devo aggiornare le variabili.
			 * 
			 * Se l'idirizzo del vecchio relay � uguale al suo ( � il vecchio relay)
			 * si mette in attesa del request session da parte del nuovo relay
			 * 
			 * registra nella variabile maxWnexRelay l'idirizzo locale del suo successore 
			 * 
			 * Se il nodo � un non � attivo controlla l'indirizzo del vincitore
				Se � pari al suo indirizzo sul cluster locale
				. Manda al vecchio relay il messaggio di 
					Request Session
					
				Se � un relay secondario e l'indirizzo del cluster locale del vincitore � uguale a quello del nodo 
				da cui riceveva il flusso ( � stato rieletto il Big Boss)
					
					Il relay si comporta �da client�: informa tutti I suoi proxy dell'avvenuta rielezione affinch� questi salvino 
					l'indirizzo del nuovo bigBoss, per poi considerarlo come nuova fonte di flusso quando il proxy sul vecchio relay 
					che mandava il flusso invier� il messaggio di leave.
				
				Altrimenti ( sono un relay inattivo e non ho vinto) 
				aggiorno gli indirizzi e basta...
			 * 
			 * 
			 */
		{
			StringTokenizer st = new StringTokenizer(this.event, ":");
			st.nextToken();
			String newRelay = st.nextToken(); // Marco: estraggo l'indirizzo del nuovo relay
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
			System.out.println("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
			// TODO aggiungi le scritte di debug sull'acquisizione dei nuovi campi
			 // estaggo l'indirizzo di chi eroga il flusso al vecchio relay.
			String vecchioRelay = st.nextToken();
			String indsupVecchioRelay = st.nextToken();
			
			String indFornitoreFlussiVecchioRelay=st.nextToken();
			
			
			//io sono un relay seconario ed � stato rieletto il big boss, devo avvertire i Proxy.
			//nota. è imposibile che un relay secondario con delle sessioni attive diventi big boss.
			//possono diventarlo solo nodi senza sessioni attive, 
			if(this.connectedClusterHeadAddress.equals(vecchioRelay)&&electionManager.isRELAY()){
				// posso gi� permettermi di sostituire il riferimento al big boss all'interno di sessionManager 
				// perchè so che ci pu� essere una sola rielezione in corso alla volta, si interagirebbe con il 
				// big boss solo se nel caso di una rielezione di questo proxy secondario, che
				// per� non pu� aver luogo prima che la rielezione del big boss sia finita.
				this.connectedClusterHeadAddress=newRelay;
				
				/*
				 * setto il parametro futureStreamingServer di tutti i proxy, in modo che
				 * quando riceveranno il messaggio di LEAVE dal proxy sul vecchio big boss
				 * che erogava loro il flusso settino come loro nuovo mittente il nuovo 
				 * proxy sul nuovo big boss (che eroga il flusso dalla stessa porta)
				 */
				
				Enumeration<String> chiavi = sessions.keys();
				
				while (chiavi.hasMoreElements()){
					Session sessione = sessions.get(chiavi.nextElement());
//					sessione.getProxy().setFutureStreamingAddress(newRelay);
				}
				
				
				
			}
				
			else{
				// il nodo � il vecchio relay
				
				if (electionManager.getLocalClusterAddress().equals(oldRelayLocalClusterAddress)){
					this.maxWnextRelay = newRelay;
					this.status= RelaySessionStatus.AttendingRequestSession;
					//TIMEOUT DISABILITATI
//					this.toSessionRequest = RelayTimeoutFactory.getTimeOutSessionRequest(this, TimeOutConfiguration.TIMEOUT_SESSION_REQUEST);

				}
				else{
					// il nodo � il vincitore!
					if(electionManager.getLocalClusterAddress().equals(newRelay)){
						//MARCO: ROBA TUA CHE HO DOVUTO COMMENTARE
//						this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(oldRelayLocalClusterAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
						this.sessionCMCluster.sendTo(message);
						//TIMEOUT DISABILITATI
//						this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, TimeOutConfiguration.TIMEOUT_SESSION_INFO);
						this.status = RelaySessionStatus.RequestingSession;
						this.oldRelayLocalClusterAddress=vecchioRelay;
						this.oldRelayClusterHeadAddress=indsupVecchioRelay;
						this.connectedClusterHeadAddress=indFornitoreFlussiVecchioRelay;
					}
					else{
						// in questo caso ci finisco se sono un relay che non ha vinto e se non sono un relay secondario con dei flussi
						// attivi provenienti dal vecchio big boss, mi limito a registrare i dati della rielezione, ma non li user� neanche
						this.relayAddress= newRelay;
						this.oldRelayClusterHeadAddress= vecchioRelay;
						this.oldRelayClusterHeadAddress= indsupVecchioRelay;
						this.connectedClusterHeadAddress=indFornitoreFlussiVecchioRelay;
						
					}
				}
			}
		} // Fine evento new Relay
		
		
		
//		if(this.event.contains("NEW_EM_RELAY"))
//		{
//			consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di NEW_EM_RELAY");
//			StringTokenizer st = new StringTokenizer(this.event, ":");
//			st.nextToken();
//			String newRelay = st.nextToken();
//			try {
//				if(InetAddress.getLocalHost().toString().equals(newRelay))
//				{
//					this.imRelay = true;
//					this.status = "Active";
//				}
//				else
//				{
//					this.relayAddress = newRelay;
//					this.status = "Waiting";
//				}
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions != 0 && this.status.equals("RequestingSession")) // Nuovo Realy: è scattato il timout sull'attesa di SessionInfo
		{
			/*
			 * Marco: sono il nuovo relay: è scattato il timeout sulla ricezione del messaggio SessionInfo.
			 * il parametro numberOf ritrasmissions idica quante volte posso chiedere la ritrasmissione del messaggio Sessioninfo
			 * fortunatamente qui posso ritrasmetterlo diminuendo il numero di ritrasmissioni rimaste
			 */
//			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO");
//			try {
//				this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
//				this.sessionCM.sendTo(message);
//				this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, Parameters.TIMEOUT_SESSION_INFO);
//				this.numberOfRetrasmissions--;
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

		}
		if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions == 0)
		{
//			/*
//			 * Marco: sono il nuovo relay: è scattato il timeout sulla ricezione del messaggio SessionInfo.
//			 * il parametro numberOf ritrasmissions idica quante volte posso chiedere la ritrasmissione del messaggio Sessioninfo
//			 * sfortunatamente non posso ritrasmetterlo: non mi resta che mandare IN BROADCAST
//			 *  ai client il messaggio di invalidare le sessioni attive, ipotizzo quindi che il vecchio relay non ci sia più
//			 */
//			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO e numero di ritrasmissioni a 0");
//			this.status = "Waiting";
//			//Invio il messaggio di invalidazione della sessione ai client che conoscono l'identità del nuovo relay ma non si ha modo di recuperare la sessione
//			try {
//				this.message = RelayMessageFactory.buildSessionInvalidation(0, InetAddress.getByName(Parameters.BROADCAST_ADDRESS), Parameters.CLIENT_PORT_SESSION_IN);
//				this.sessionCM.sendTo(this.message);
//				this.status = "Active";
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		
		// TODO ELECTION_REQUEST_RECEIVED si inizia la fase di rielezione
		if(this.event.equals("ELECTION_REQUEST_RECEIVED")&&electionManager.isRELAY())			{
			/*
			 * Marco:entri in questo blocco quando parte la fase di rielezione del BigBoss e il nodo � un relay secondario
			 * servito dal big boss.
			 * Qui il relay secondario deve ingrandire i buffer (alzando la soglia superiore) dei propri proxy, analogamente
			 * a quello che fanno i clients.
			 * Nota il nodo che lancia l'electionRequest non riceve questo messaggio, sei quindi tutelato nel caso
			 * sia il relay secondario ad indire la rielezione per il suo cluster.
			 * 
			 * Comunico ai proxy di ingrandire i propri buffers.
			 */
			Enumeration <String> chiavi=sessions.keys();
			while(chiavi.hasMoreElements()){
				(sessions.get(chiavi.nextElement()).getProxy()).enlargeNormalBuffer();
			}
			
			
			
			
		}
	}
	/**
	 * il proxy dopo aver creato la sessione ha il compito di avvertire il sessionmanager
	 * ++++++++++++++++++++++++++EVENTO SOLLEVATO DAI PROXY++++++++++++++++++++++++++++++
	 */
	if(arg instanceof int[])
	{
		proxy = (Proxy) receiver; // non ti fare ingannare dal nome. il receiver è colui che ha lanciato l'evento
		/*
		 * Il proxy creato ex-novo, comunca al sessionManager le porte che
		 * usa. Il session Manager inserisce le porte nella tabella session.
		 * trovo il record grazie al fatto che c'� un'unico proxy per sessione;
		 * non posso usare l'indirizzo del client come si poteva fare nella vecchia versione
		 * perch� il proxy pu� erogare anche ad un relay secondario, e quindi 
		 * posso avere pi� proxy con lo stesso client address
		 */
		//CONTROLLARE SE il seguente cast è giusto altrimenti si cambia...
		System.out.println("Il Proxy mi ha notificato i parametri di sessione...");
		
		int[] sessionPorts = (int[])arg;
		System.err.println("RelaySessionManager- Porta di Stream del server: " +sessionPorts[0]);
		System.err.println("RelaySessionManager- Porta di Stream del relay IN: " +sessionPorts[1]);
		System.err.println("RelaySessionManager- Porta di Stream del relay OUT: " +sessionPorts[2]);
		System.err.println("RelaySessionManager- Porta di Stream del Client IN: " +sessionPorts[3]);
		System.err.println("RelaySessionManager- Porta di Stream del Server control: " +sessionPorts[4]);
		System.err.println("RelaySessionManager- Porta di Stream del Proxy control: " +sessionPorts[5]);
		Enumeration<String> keys = sessions.keys();
		while(keys.hasMoreElements()){
			String chiave = keys.nextElement();
			Session sessione = sessions.get(chiave);
		
			if ( sessione.getProxy().equals(proxy)){
				sessione.setSessionInfo(sessionPorts);
				// in teoria qui ci andrebbe un break, non ha senso ciclare per tutti gli altri elementi
				// per stare dalla parte dei bottoni lascio tutto cos�.
				// ps ho controllato effettivamente va bene, l'equals è true solo se si tratta della 
				// stessa istanza del proxy
			}
		}
		
		
		System.out.println("AGGIUNTO IL RIFERIMENTO IN SESSION_INFO");
	}
}


/**
 * @return the maxWnextRelay
 */
public String getMaxWnextRelay() {
	return maxWnextRelay;
}

/**
 * @param maxWnextRelay the maxWnextRelay to set
 */
public void setMaxWnextRelay(String maxWnextRelay) {
	this.maxWnextRelay = maxWnextRelay;
}

public void updateSessions()
{
	if(this.imBigBoss){
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = RelaySessionStatus.IDLE_BIGBOSS;
		}
	}
	else{
		if(this.numberOfSession != 0)
		{
			numberOfSession--;
			if(this.numberOfSession == 0)
				this.status = RelaySessionStatus.IDLE_NORMAL;
		}
	}
}

/**
 * Metdo chiamato dal nuovo proxy quando gli arriva il messaggio SESSION_INFO per creare
 * i proxy
 * Crea i proxy dalla tabella di sessioni ottenuta dal messaggio di Sessioninfo,
 *  inserisce i proxy nelle sessioni e restituisceun'altra tabella che indica
 * per ogni sessione le porte di recovery sulle quali il vecchio relay deve ridirigere il flusso,
 * inoltre riempie i campi proxy delle sessioni presenti nella tabella con i proxy appena creati
 * @param sessions � una tabella che contiene i dati come tabella di Session, la chiave � l'indirizzo del client
 * 
 * @return una stringona che contiene per ogni sessione l'indirizzo ip del client ( la chiave della sessione) e la porta di recovery del 
 * proxy che gestisce quella sessione
 * es (192.168.1.1_5000_162.168.1.2_6000)
 */

private String createProxyFromSession(Hashtable sessionInfo)
{
	/*
	 * 
	 * 
	 * Marco: composizione della tabella sessionInfo (
	 * valore[0]=  porta dalla quale il server eroga lo stream
	 * valore[1] = porta sulla quale il proxy riceve lo stream
	 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
	 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
	 * valore[4] = porta di controllo del server
	 * valore[5] = porta di controllo del relay
	 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
	 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
	 */
	Proxy proxy;
	String chiave;
	String recStreamInports = "";
	int serverPortStreamOut = 0;
	int proxyPortStreamIn = 0;
	int proxyPortStreamOut = 0;
	int clientPortStreamIn = 0;
	int serverCtrlPort = 0;
	int proxyCtrlPort = 0;
	if(!sessions.isEmpty())
	{
		Enumeration keys = sessions.keys();
		while(keys.hasMoreElements())
		{
			chiave = keys.nextElement().toString();
			Session session = (Session)sessions.get(chiave);
			int[] values =session.getSessionInfo();
			if(values.length == 6)
			{
				serverPortStreamOut = values[0];
				proxyPortStreamIn = values[1];
				//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
				RelayPortMapper.getInstance().setRangePortInRTPProxy(proxyPortStreamIn);
				proxyPortStreamOut = values[2];
				//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
				RelayPortMapper.getInstance().setRangePortOutRTPProxy(proxyPortStreamOut);
				clientPortStreamIn = values[3];
				serverCtrlPort = values[4];
				proxyCtrlPort = values[5];
			}
			try {
				/*
				 * nel costruttore del proxy è stato inserito il valore di proxy PortStreamOut in 2 punti diversi proprio perchè il vecchio proxy quando avvia
				 * la trasmissione di recovery verso il nuovo riutilizza la medesima porta.
				 * Marco: quindi il numero di porta sulla quale il proxy sul nuovo relay riceve il flusso da inserire nel suo recovery buffer è lo stesso
				 *  numero della porta dalla quale sia il vecchio che il nuovo relay erogano il flusso.
				 * creo il proxy e  metto l'istanza nella sessione
				 * 
				 * determino il flag servigClient del relay che gli indica se sto servendo direttamente un client o meno nel seguente modo:
				 * 	sto servendo un relay secondario ( flag a false) solo se la sessione è mediata e io sono un big boss
				 * 
				 */
				boolean servingClient;
				if (electionManager.isBIGBOSS()&& session.isMediata())
					servingClient=false;
				else
					servingClient=true;
								
				//proxy = new Proxy(this, false, chiave, clientPortStreamIn, proxyPortStreamOut, proxyPortStreamIn, serverPortStreamOut, proxyPortStreamOut,this.connectedClusterHeadAddress ,InetAddress.getByName(this.oldRelayLocalClusterAddress), serverCtrlPort, proxyCtrlPort, servingClient);
				proxy= new Proxy(this, false, chiave, clientPortStreamIn, proxyPortStreamOut, proxyPortStreamIn, serverPortStreamOut, proxyPortStreamOut, InetAddress.getByName(this.oldRelayLocalClusterAddress), serverCtrlPort, proxyCtrlPort, electionManager.getLocalClusterHeadAddress(), electionManager.getConnectedClusterHeadAddress(), servingClient);
				session.setProxy(proxy);
				recStreamInports = recStreamInports+"_"+chiave+"_"+proxy.getRecoveryStreamInPort();
			} catch (UnknownHostException e) {
				
				e.printStackTrace();
			}
		}// fine while	
	}
//	recStreamInports.replaceFirst("_", "");
//	recStreamInports = recStreamInports.substring(arg0, arg1)
	return recStreamInports;
}

private void changeProxySession(Hashtable sessionEndpoint)
{
	/*Marco:
	 * le tuple della tabella session Endpoint contengono come chiave l'indirizzo del client ( come id di sessione)
	 * e come secondo argomento un vettore di interi con un solo elemento:
	 *  la porta sulla quale il nuovo proxy attende il flusso rtp dal vecchio proxy
	 * quindi per ogni sessione si invoca il metodo handoff del relativo roxy che, appunto ridirige il flusso sull'indirizzo e sulla
	 * porta indicata.
	 * inoltre setta il flag ending per ongi proxy
	 * 
	 */
	String chiave;
	if(!sessionEndpoint.isEmpty())
	{
		if(!sessions.isEmpty())
		{
			Enumeration keys = sessionEndpoint.keys();
			while(keys.hasMoreElements())
			{
				chiave = keys.nextElement().toString();
				int[] values =(int[]) sessionEndpoint.get(chiave);
				try {
					Proxy proxy = sessions.get(chiave).getProxy();
					proxy.startHandoff(values[0], InetAddress.getByName(this.maxWnextRelay));
					proxy.setEnding(true);
				} catch (UnknownHostException e) {
					
					e.printStackTrace();
				}
			}
		}
	}
}




}
