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

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;

import parameters.PortConfiguration;

import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;

import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutAckSessionInfo;
import relay.timeout.TimeOutSessionInfo;
import relay.timeout.TimeOutSessionRequest;


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
	private Hashtable<String, Proxy> pReferences; // Marco: tabella che contiene per ogni sessione ( identificata dall'indirizzo del client) il proxy che la serve
	private Hashtable<String, int[]> sessionInfo;
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
	private TimeOutSessionRequest toSessionRequest;
	private TimeOutAckSessionInfo toAckSessionInfo;
	private TimeOutSessionInfo toSessionInfo;
	
	
	private RelayCM sessionCM; // Marco: è chi si occupa di spedire i messaggi.
	private RelayMessageReader messageReader;
	
	private boolean isBigBoss;//Valerio: questo flag viene settato in base al metodo che ha Pire in ElectionManager
	private int clientSessionPort;
	private String serverAddress;
	private int serverPortSessionIn;
	private int seqNumSendServer;
	private int seqNumSendClient;
	private int seqNumSendBigBoss;
	private int seqNumSendRelay;
	private String listaFile;
	private String[] files;
	
	private String bigbossAddress;
	private int bigbossPort;
	
	//stati in cui si può trovare il RelayElectionManager
	public enum RelaySessionStatus {  
		IDLE_BIGBOSS,
		ACTIVE_BIGBOSS,
		ATTENDIGACKSESSION,
		IDLE_NORMAL,
		ACTIVE_NORMAL,
		WAITING,
		NOTHING
		}
	
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(RelayElectionManager electionManager) {
		this.electionManager = electionManager;
	}

	public RelaySessionManager()
	{
		this.numberOfSession = 0;
		pReferences = new Hashtable();
		sessionInfo = new Hashtable();
		this.sessionCM = RelayConnectionFactory.getSessionConnectionManager(this);
		this.messageReader = new RelayMessageReader();
		this.sessionCM.start();
		this.consolle.setTitle("RELAY SESSION MANAGER DEBUG CONSOLLE");
		
		if(isImBigBoss()){
			//Valerio: se questo è il bigboss ha l'indirizzo del server, e la porta a cui inviare le richieste
			serverAddress=NetConfiguration.SERVER_ADDRESS;
			serverPortSessionIn=PortConfiguration.SERVER_SESSION_PORT_IN;
		}
		else{
			bigbossAddress=NetConfiguration.BIGBOSS_AD_HOC_ADDRESS;
			bigbossPort=PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN;
		}
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
	public boolean isImRelay() {
		return imRelay;
	}
	
	/**
	 * @return the imBigBoss
	 */
	public boolean isImBigBoss() {
		return imBigBoss;
	}

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

		/**
		 * arrivato messaggio di richiesta da parte del client
		 */
		
		/*Marco: è arrivata una richiesta da parte di un client ed io sono il relay.
		 * creo un nuovo proxy che gestisce lo stream verso il client
		 * aggiungo il proxy alla tabella dei proxy usando l'indirizzo del client come chiave per la sessione
		 * ed aumento il numero di sessioni
		*/
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_LIST && imRelay && imBigBoss){//Valerio: aggiunto da me
			System.out.println("codice request_list e sono il relay bigboss");
			this.clientAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+ this.clientAddress+" devo inviarla al server: "+this.serverAddress+" sulla porta "+this.serverPortSessionIn+" e la risposta andrà inviata al client sulla porta "+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
				this.message=RelayMessageFactory.buildRequestList(seqNumSendServer, InetAddress.getByName(this.serverAddress), this.serverPortSessionIn, this.clientAddress, this.clientSessionPort);
				sessionCM.sendTo(this.message);
				System.out.println("BigBoss: inviato il messaggio request list al server");
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata al server la richiesta lista file");
		}
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_LIST && imRelay && imBigBoss){//Valerio: aggiunto da me	
			//se non è bigboss dovrò creare un messaggio di tipo FORWARD_REQUEST_LIST
			System.out.println("codice request_list e sono un relay normale");
			this.clientAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+ this.clientAddress+" devo inviarla al big boss: "+this.bigbossAddress+" sulla porta "+this.bigbossPort+" e la risposta andrà inviata al client sulla porta "+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
				this.message=RelayMessageFactory.buildForwardRequestList(seqNumSendBigBoss++, InetAddress.getByName(this.bigbossAddress), this.bigbossPort,null, this.clientAddress);
				sessionCM.sendTo(this.message);
				System.out.println("Relay: inviato il messaggio forward request list al bigboss");
				}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Relay: inviato il messaggio forward request list al bigboss");
		}
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.LIST_RESPONSE && imRelay){
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
				this.message=RelayMessageFactory.buildListResponse(seqNumSendClient++,InetAddress.getByName(messageReader.getClientAddress()), messageReader.getClientPort(), listaFile);
				sessionCM.sendTo(this.message);
				System.out.println("inviata la lista dei file al client");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_REQ_LIST&&imRelay){
			//allora questo è bigboss che deve inoltrare tutto al server
			System.out.println("codice forward_request_list quindi sono un relay");
			this.relayAddress=message.getAddress().getHostAddress();
			this.clientAddress=messageReader.getClientAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Arrivata la richiesta della lista file da "+this.relayAddress+" devo inviarla al server: "+serverAddress+" sulla porta "+serverPortSessionIn+" e la risposta andrà inviata al client "+this.clientAddress+":"+PortConfiguration.CLIENT_PORT_SESSION_IN);
			System.out.println("Arrivata la richiesta della lista file da "+this.relayAddress+" devo inviarla al server: "+serverAddress+" sulla porta "+serverPortSessionIn+" e la risposta andrà inviata al client "+this.clientAddress+":"+PortConfiguration.CLIENT_PORT_SESSION_IN);
			try{
				this.message=RelayMessageFactory.buildForwardRequestList(seqNumSendBigBoss++, InetAddress.getByName(serverAddress), serverPortSessionIn,this.relayAddress, this.clientAddress);
				sessionCM.sendTo(this.message);
				System.out.println("Relay: inviato il messaggio request list al BigBoss");
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inviata al BigBoss la richiesta lista file");
		}
		
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_REQ_FILE&&imRelay){
			//allora questo è il bigboss e deve creare un proxy e girare la richiesta al server
			//questo bigboss sta servendo un relay visto che è arrivata il messaggio di forward
			this.status =RelaySessionStatus.ACTIVE_BIGBOSS;
			this.relayAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" dal RELAY: "+ this.relayAddress);
			System.out.println("E' arrivato un REQUEST_FILE dal Relay, ora creo il proxy");
			//Valerio: non so ne nel proxy va cambiato qualcosa o se va bene anche se ad inviare al client invio ad un relay
			//ovviamente gli passo l'indirizzo del relay e non quello del client
			proxy = new Proxy(this, true, messageReader.getFilename(), this.relayAddress, messageReader.getRelayControlPort(),messageReader.getRelayStreamingInputPort(), messageReader.getClientAddress(), messageReader.getClientStreamingInputPort() ,isBigBoss,false);
		
			pReferences.put(this.relayAddress, proxy);
			this.numberOfSession++;
		}
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_LIST_RESPONSE&&imRelay&&imBigBoss){
			//devo differenziare fra bigboss e relay normale
			System.out.println("Sono BigBoss, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Relay");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Sono BigBoss, è arrivato un messaggio di FORWARD_LIST_RESPONSE da inviare al Relay");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"E' arrivata la lista dei file");
			listaFile=messageReader.getListaFile();
			files = listaFile.split(",");
			System.out.println("Lista file:");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Lista File:");
			for (int i = 0; i < files.length; i++) {
				System.out.println(files[i]);
				consolle.debugMessage(DebugConfiguration.DEBUG_INFO,files[i]);
			}
			this.relayAddress=messageReader.getRelayAddress();
			try {
				this.message=RelayMessageFactory.buildForwardListResponse(seqNumSendRelay, InetAddress.getByName(this.relayAddress),PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN, this.messageReader.getClientAddress(), listaFile);
				sessionCM.sendTo(this.message);
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
		
		
		if(this.messageReader.getCode()==MessageCodeConfiguration.FORWARD_LIST_RESPONSE && imRelay && !imBigBoss){
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
			try {
				this.message=RelayMessageFactory.buildListResponse(seqNumSendClient++,InetAddress.getByName(messageReader.getClientAddress()), PortConfiguration.CLIENT_PORT_SESSION_IN, listaFile);
				sessionCM.sendTo(this.message);
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
		}
		
		
		
		
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_FILE && imRelay&&imBigBoss){
			//se il request file arriva al bigboss ho questo comportamento
			this.status = RelaySessionStatus.ACTIVE_BIGBOSS;
			this.clientAddress = message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" da "+ this.clientAddress);
			System.out.println("E' arrivato un REQUEST_FILE, ora creo il proxy");
			proxy = new Proxy(this, true, messageReader.getFilename(), null,-1,-1 ,this.clientAddress, messageReader.getPortStreamingClient(),isBigBoss,true);
			pReferences.put(this.clientAddress, proxy);
			this.numberOfSession++;
			}
		
		if(this.messageReader.getCode() == MessageCodeConfiguration.REQUEST_FILE && imRelay&&!imBigBoss){
			//allora essendo relay devo inoltrare un forwardrequestfile al bigboss
			this.status = RelaySessionStatus.ACTIVE_NORMAL;
//			this.clientAddress = message.getAddress().getHostAddress();
			this.relayAddress=message.getAddress().getHostAddress();
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" da "+ this.clientAddress);
			System.out.println("E' arrivato un REQUEST_FILE, ora creo il proxy");
			proxy = new Proxy(this, true, messageReader.getFilename(),null,-1,-1, this.clientAddress, messageReader.getPortStreamingClient(),isBigBoss,true);
			pReferences.put(this.relayAddress, proxy);
			this.numberOfSession++;
			}

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
		 *  	la porta Z sulla quale il client riceve lo streaming
		 *  	la porta Y sulla quale il vecchio relay riceve lo streaming dal server
		 *  	la porta W dalla quale il relay manda lo stream al client
		 *  	la porta X dalla quale il server manda lo stream al vecchio relay
		 */
//		if(messageReader.getCode() == Parameters.REQUEST_SESSION && this.status.equals("Active") )
//		{
//			if(toSessionRequest!=null) // Marco: viene disattivato il timeout request session: il messaggio è arrivato
//				toSessionRequest.cancelTimeOutSessionRequest();
//			consolle.debugMessage("RELAY_SESSION_MANAGER: ricevuto SESSION_REQUEST dal nuovo RELAY");
//			try {
//				if(this.sessionInfo != null || !this.sessionInfo.isEmpty())
//				{
//					/*
//					 * guida ai parametri con del metodo build session info:
//					 * il primo è un numero di sequenza che viene messo sempre a zero in tutte le parti del codice
//					 * session info dovrebbe contenere per ogni sessione le quattro porte menzionate sopra
//					 * l'indirizzo del nuovo relay (non farti fregare dal nome: non si va a prendere di nuovo il primo della tabella con i pesi derivata dalla ricezione,maxWnextRelay è solo una strnga contennente l'indirizzo del vincitore)
//					 * l'ultimo parametro è la porta sulla il relay resta in ascolto dei messaggi riguardanti la sessione.
//					 */
//					this.message = RelayMessageFactory.buildSessionInfo(0, sessionInfo, InetAddress.getByName(this.maxWnextRelay), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//					
//				}
//				
//				else{this.message = RelayMessageFactory.buildSessionInfo(0, null, InetAddress.getByName(this.maxWnextRelay), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);}
//
//				this.sessionCM.sendTo(this.message); //Marco: invio il messaggio preparato
//				this.status = "AttendingAckSession";
//				this.toAckSessionInfo = RelayTimeoutFactory.getTimeOutAckSessionInfo(this, Parameters.TIMEOUT_ACK_SESSION_INFO);
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		
//		if(messageReader.getCode() == Parameters.ACK_SESSION && this.status.equals("AttendingAckSession"))
//			
//			/*
//			 * il nuovo relay mi ha mandato il messaggio di ACK_SESSION: significa che il nuovo relay che già ottenuto i dati relativi
//			 * alle sessioni: a questo punto del protocollo il vechcio relay dovrebbe mandare il messaggio di redirect al server,
//			 *  ridirigere il flusso sui recovery buffer dei proxy sul nuovo relay
//			 *  Una volta svuotati tutti i buffer di tutti i proxy dovrebbe mandare il messaggio di LEAVE a tutti i client serviti
//			 */
//		{
//			if(this.toAckSessionInfo!=null)
//				toAckSessionInfo.cancelTimeOutAckSessionInfo();
//			consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto ACK_SESSION dal nuovo RELAY");
//			// TODO SETTAGGIO SU TUTTI I PROXY DI TUTTE LE PORTE SU CUI TRASMETTERE I FRAME NEI BUFFER LOCALI
//			// Marco: il metyodo change proxy Session invoca il metodo Start handoff di ogni proxy che, in sostanza dovrebbe gestisce
//			// la redirezione del flusso verso il nuovo relay ( per i dettagli guarda i commenti ai metodi).
//			
//			this.changeProxySession(messageReader.getProxyInfo());
//		}
		
//		if(messageReader.getCode() == Parameters.SESSION_INFO && this.status.equals("RequestingSession"))
//		{
//			
//			/*
//			 * fa parte del nuovo relay: arrivano i dati del messaggio Session Info 
//			 * Marco: composizione della tabella sessionInfo ( ancora non verificato, mi baso sui nomi che vengono assegnati)
//			 * chiave: indirizzo del client
//			 * valore[0]=  porta dalla quale il server eroga lo stream
//			 * valore[1] = porta sulla quale il proxy riceve lo stream
//			 * valore[2] = porta dalla quale il proxy eroga lo steam verso il client
//			 * valore[3] = porta sulla quale il client riceve lo stream dal proxy
//			 * valore[4] = porta di controllo del server
//			 * valore[5] = porta di controllo del relay
//			 * nota hai bisogno di due porte di controllo perchè il proxy sta in mezzo, riceve un flusso dal server e ne eroga uno al client.,
//			 * quindi hai due connessioni rtp e due porte di controllo, visto che c'è una porta di controllo associata ad ogni connessione rtp
//			 */
//			
//			/*
//			 * Marco: successivamente mando il messaggio di ack session al vecchio relay
//			 * sembra che a mandare il messaggio di REDIRECT al server perchè questi dirotti il flusso sul nuovo relay sia 
//			 * ogni nuovo proxy relativamente alla stessa sessione
//			 */
//			 
//			// TODO considera la possibilità di far mandare un unico redirect dal Session Manager anzichè farne mandare tante 
//			 
//			
//			this.toSessionInfo.cancelTimeOutSessionInfo();
//			consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto SESSION_INFO dal vecchio RELAY");
//			this.sessionInfo = messageReader.getSessionInfo();
//			String proxyInfo = this.createProxyFromSession(sessionInfo);
//			try {
//				this.message = RelayMessageFactory.buildAckSession(0, proxyInfo, InetAddress.getByName(this.relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//				this.sessionCM.sendTo(this.message);
//				this.status = "Active";
//			} catch (UnknownHostException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	/**
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * ++++++++++++++++++++++++++++++EVENTI ARRIVATI -- STRINGHE ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 */
	if(arg instanceof String)
	{
		this.event = (String)arg;
		if(this.event.equals("End_Of_Media") && status.equals("ACTIVE_BIGBOSS") && imRelay)
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
			pReferences.remove(proxy.getClientAddress());
			sessionInfo.remove(proxy.getClientAddress());
			this.numberOfSession--;
			if(numberOfSession == 0)
				this.status = RelaySessionStatus.IDLE_BIGBOSS;
		}
		
		if(this.event.equals("End_Of_Media") && status.equals("ACTIVE_NORMAL") && imRelay)
		{
			/*
			 * fine di una canzone 
			 * tolgo le informazioni relative alla sessione terminata dalle relative tabelle 
			 * (sia quella sessione- proxy che quella sessione--sessioninfo)
			 * se non mi restano altre sessioni attive ( le tabelle sono quindi vuote) metto il Relay in stato di Idle
			 */
			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER Relay normale: Evento di END_OF_MEDIA da parte di un proxy");
			proxy = (Proxy)receiver;
			//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
			pReferences.remove(proxy.getClientAddress());
			sessionInfo.remove(proxy.getClientAddress());
			this.numberOfSession--;
			if(numberOfSession == 0)
				this.status = RelaySessionStatus.IDLE_NORMAL;
		}
		
		/**
		 * l'electionmanager mi ha comunica chi è il relay attuale a seguito di un messaggio di WHO_IS_RELAY
		 */
//		if(this.event.contains("RELAY_FOUND"))
//		{
//			StringTokenizer st = new StringTokenizer(this.event, ":");
//			st.nextToken();
//			this.relayAddress = st.nextToken();
//			consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di RELAY_FOUND, il RELAY attuale è: "+this.relayAddress);
//		}
		
		if(this.event.equals("TIMEOUTSESSIONREQUEST") || this.event.equals("TIMEOUTACKSESSIONINFO")) //VECCHIO RELAY: il nuovo relay non ha risposto
		{
			if(this.event.equals("TIMEOUTSESSIONREQUEST") && status.equals("Active"))
				consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_REQUEST");
			if(this.event.equals("TIMEOUTACKSESSIONINFO") && status.equals("AttendingAckSession"))
				consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_ACK_SESSION_INFO");
			//TODO DEVO AVVISARE IL RELAYELECTIONMANAGER
			/*
			 * Marco: in sostanza, se il nuovo relay non risponde, cerco di nominare il secondo
			 * classificato alle elezioni
			 * 
			 */
			if(this.electionManager!=null)
			{
				electionManager.chooseAnotherRelay();
			}

		}
		
		/**
		 * l'electionmanager mi comunica chi è il relay appena eletto
		 */
		/*
		 *se  io sono il relay attualmente attivo salvo il nome di chi ha vinto nella variabile maxWnextRelay
		 *		cambio stato indicando che non sono più io il relay di riferimento della rete 
		 *		e faccio partire il timeout mentre attendo il messaggio di REQUEST SESSION da parte dell'altro relay
		 *
		 *se invece non sono il relay attualmente attivo confronto l'indirizzo del vincitore con il mio: se ho vinto
		 *	cambio il mio stato per indicare che sono il relay attivo, 
		 *	mando il messaggio di Elecion Request al vecchiorelay 
		 *	e mi metto in attesa delle informazioni sulle sessioni
		 *
		 */
//		if(this.event.contains("NEW_RELAY"))
//		{
//			StringTokenizer st = new StringTokenizer(this.event, ":");
//			st.nextToken();
//			String newRelay = st.nextToken();
//			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
//			System.out.println("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
//			if(imRelay) // Marco: imRelay è true se io sono il relay attualmente attivo nella rete
//			{
//				this.maxWnextRelay = newRelay;
//				this.imRelay = false; // Marco: cambio già il flag: non è un po' presto? io lo avrei fatto dopo aver inviato tutti i messaggi di leave
//				this.toSessionRequest = RelayTimeoutFactory.getTimeOutSessionRequest(this, Parameters.TIMEOUT_SESSION_REQUEST);
//			}
//			else
//			{
//				try {
//					if(InetAddress.getByName(Parameters.RELAY_AD_HOC_ADDRESS).getHostAddress().equals(newRelay))
//					{
//						this.imRelay = true; // Marco : cambio già lo stato: non è presto? forse sarebbe meglio aspettare dopo aver mandato il Redirect al server...
//						this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//						this.sessionCM.sendTo(message);
//						this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, Parameters.TIMEOUT_SESSION_INFO);
//						this.status = "RequestingSession";
//					}
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
		
		
		
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
			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO");
			try {
				this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(this.relayAddress), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
				this.sessionCM.sendTo(message);
				this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, Parameters.TIMEOUT_SESSION_INFO);
				this.numberOfRetrasmissions--;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions == 0)
		{
			/*
			 * Marco: sono il nuovo relay: è scattato il timeout sulla ricezione del messaggio SessionInfo.
			 * il parametro numberOf ritrasmissions idica quante volte posso chiedere la ritrasmissione del messaggio Sessioninfo
			 * sfortunatamente non posso ritrasmetterlo: non mi resta che mandare IN BROADCAST
			 *  ai client il messaggio di invalidare le sessioni attive, ipotizzo quindi che il vecchio relay non ci sia più
			 */
			consolle.debugMessage(DebugConfiguration.DEBUG_WARNING,"RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO e numero di ritrasmissioni a 0");
			this.status = "Waiting";
			//Invio il messaggio di invalidazione della sessione ai client che conoscono l'identità del nuovo relay ma non si ha modo di recuperare la sessione
			try {
				this.message = RelayMessageFactory.buildSessionInvalidation(0, InetAddress.getByName(Parameters.BROADCAST_ADDRESS), Parameters.CLIENT_PORT_SESSION_IN);
				this.sessionCM.sendTo(this.message);
				this.status = "Active";
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * il proxy dopo aver creato la sessione ha il compito di avvertire il sessionmanager
	 * ++++++++++++++++++++++++++EVENTO SOLLEVATO DAI PROXY++++++++++++++++++++++++++++++
	 */
	if(arg instanceof int[])
	{
		//CONTROLLARE SE il seguente cast è giusto altrimenti si cambia...
		System.out.println("Il Proxy mi ha notificato i parametri di sessione...");
		
		int[] sessionPorts = (int[])arg;
		System.err.println("RelaySessionManager- Porta di Stream del server: " +sessionPorts[0]);
		System.err.println("RelaySessionManager- Porta di Stream del relay IN: " +sessionPorts[1]);
		System.err.println("RelaySessionManager- Porta di Stream del relay OUT: " +sessionPorts[2]);
		System.err.println("RelaySessionManager- Porta di Stream del Client IN: " +sessionPorts[3]);
		System.err.println("RelaySessionManager- Porta di Stream del Server control: " +sessionPorts[4]);
		System.err.println("RelaySessionManager- Porta di Stream del Proxy control: " +sessionPorts[5]);
		this.sessionInfo.put(((Proxy)receiver).getClientAddress(), sessionPorts);
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
private String createProxyFromSession(Hashtable sessionInfo)
{
	/*
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
	if(!sessionInfo.isEmpty())
	{
		Enumeration keys = sessionInfo.keys();
		while(keys.hasMoreElements())
		{
			chiave = keys.nextElement().toString();
			int[] values =(int[]) sessionInfo.get(chiave);
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
				/**
				 * nel costruttore del proxy è stato inserito il valore di proxy PortStreamOut in 2 punti diversi proprio perchè il vecchio proxy quando avvia
				 * la trasmissione di recovery verso il nuovo riutilizza la medesima porta.
				 * Marco: quindi il numero di porta sulla quale il proxy sul nuovo relay riceve il flusso da inserire nel suo recovery buffer è lo stesso
				 *  numero della porta dalla quale sia il vecchio che il nuovo relay erogano il flusso.
				 * 
				 */
				proxy = new Proxy(this, false, chiave, clientPortStreamIn, proxyPortStreamOut, proxyPortStreamIn, serverPortStreamOut, proxyPortStreamOut ,InetAddress.getByName(this.relayAddress), serverCtrlPort, proxyCtrlPort);
				recStreamInports = recStreamInports+"_"+chiave+"_"+proxy.getRecoveryStreamInPort();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
//	recStreamInports.replaceFirst("_", "");
//	recStreamInports = recStreamInports.substring(arg0, arg1)
	return recStreamInports;
}

private void changeProxySession(Hashtable sessionEndpoint)
{
	/*Marco:
	 * le tuple della tabella session Endpoint contengono come chiave l'indirizzo del client ( come id di sessione)
	 * e come secondo argomento un vettore contenente le quattro porte per lo streaming,
	 * il primo elemento in particolare è la porta sulla quale il nuovo proxy attende il flusso rtp dal vecchio proxy
	 * quindi per ogni sessione si invoca il metodo handoff del relativo roxy che, appunto ridirige il flusso sull'indirizzo e sulla
	 * porta indicata.
	 * inoltre setta il flag ending per ongi proxy
	 * 
	 */
	String chiave;
	if(!sessionEndpoint.isEmpty())
	{
		if(!pReferences.isEmpty())
		{
			Enumeration keys = sessionEndpoint.keys();
			while(keys.hasMoreElements())
			{
				chiave = keys.nextElement().toString();
				int[] values =(int[]) sessionEndpoint.get(chiave);
				try {
					pReferences.get(chiave).startHandoff(values[0], InetAddress.getByName(this.maxWnextRelay));
					pReferences.get(chiave).setEnding(true);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
}
