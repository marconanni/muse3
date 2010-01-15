package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;


import debug.DebugConsole;

import parameters.Parameters;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.connection.WhoIsRelayServer;
import relay.positioning.RelayPositionAPMonitor;
import relay.positioning.RelayPositionMonitor;
import relay.battery.RelayBatteryMonitor;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutElectionBeacon;
import relay.timeout.TimeOutFailToElect;
import relay.timeout.TimeOutSearch;
import relay.timeout.TimeOutToElect;
import relay.wnic.RelayWNICController;
import relay.wnic.WNICFinder;
import relay.wnic.exception.WNICException;

/**Classe che gestisce la parte legata al protocollo di elezione del nuovo Relay
 * @author Luca Campeti
 */
public class RelayElectionManager extends Observable implements Observer {
	
	//Debug consolle del RelayElectionManager
	private DebugConsole console = null;

	//stati in cui si può trovare il RelayElectionManager
	public enum RelayStatus {
		OFF,
		WAITING_WHO_IS_RELAY,
		IDLE,
		MONITORING,
		WAITING_BEACON,
		ACTIVE_NORMAL_ELECTION,
		WAITING_END_NORMAL_ELECTION,
		ACTIVE_EMERGENCY_ELECTION,
		WAITING_END_EMERGENCY_ELECTION,
		WAITING_RESPONSE
	}

	//il manager per le comunicazioni
	private RelayCM comManager = null;

	//indirizzo broadcast in forma InetAddress
	private static InetAddress BCAST = null;
	static { 
		try {
			BCAST  = InetAddress.getByName(Parameters.BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}

	//stato attuale del RelayElectionManager
	private RelayStatus actualStatus = null;

	//indirizzo del Relay attuale in forma String
	private String localRelayAddress = null;
	
	//indirizzo attuale del Relay (big boss) a cui è connesso -> solo relay normale
	private String connectedRelayAddress = null;
	
	//indirizzo del Relay in forma InetAddress
	private InetAddress localRelayInetAddress = null;
	
	//indirizzo del Relay in forma InetAddress
	private InetAddress connectedRelayInetAddress = null;

	//Vector da riempire con le Couple relative agli ELECTION_RESPONSE ricevuti dal Relay uscente
	private Vector<Couple> possibleRelay = null; 
	
	//peso del nodo 
	private double W = -1;
	
	//massimo peso rilevato
	private double maxW = -1;

	//boolean che indica se si è in stato di elezione o meno
	private boolean electing = false;
	
	//boolean che indica se è stato già ricevuto un EM_ELECTION
	//private boolean firstEM_ELECTIONarrived = false;
	
	//indici dei messaggi inviati
	private int indexELECTION_RELAY_BEACON=0;
	
	//boolean che indica se è stato già inviato un ELECTION_DONE
	private boolean firstELECTION_DONEsent = false;

	//boolean che indica se si è il BigBoss attivo (connesso al nodo server) o meno
	private boolean imBigBoss = false;
	
	//boolean che indica se si è il Relay e attivo o meno
	private boolean imRelay = false;
	
	//boolean che indica se può sostituire il nodo BigBoss o relay attivo ( cioè risulta essere bigboss passivo)
	private boolean imPossibleBigBoss = false;
	private boolean imPossibleRelay =false;
	
	//numero di clients rilevati al momento a 1 hop da questo nodo
	private int counter_clients = 0;
	private int counter_relays = 0;
	private int active_relays = 0;

	//il RelayMessageReader per leggere il contenuto dei messaggi ricevuti
	private RelayMessageReader relayMessageReader = null;

	//il RelayAPWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete MANAGED
	private RelayWNICController relayAPWNICController = null;
	
	//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc
	private RelayWNICController relayAHWNICController = null;
	
	//il RelayPositionAPMonitor per conoscere la propria situazione nei confronti dell'AP
	private RelayPositionAPMonitor relayPositionAPMonitor = null;
	
	//private RelayPositionController relayPositionController = null;
	
	//il RelayPositionClientsMonitor per conoscere la propria situazione nei confronti dei clients attualmente serviti
	//compreso BIG BOSS in caso di relay secondario
	private RelayPositionMonitor relayPositionMonitor = null;
	
	//il RelayBatteryMonitor per conoscere la situazione della propria batteria
	private RelayBatteryMonitor relayBatteryMonitor = null;
	
	//il WhoIsRelayServer per rispondere ai WHO_IS_RELAY nel caso questo nodo sia il Relay attuale
	//o per rispondere alle richieste WHO_IS_BIG_BOSS_RELAY
	private WhoIsRelayServer whoIsRelayServer = null;

	//vari Timeout necessari al RelayElectionManager
	private TimeOutSearch timeoutSearch = null;
	private TimeOutToElect timeoutToElect = null;
	private TimeOutFailToElect timeoutFailToElect = null;
	private TimeOutElectionBeacon timeoutElectionBeacon = null;
	//private TimeOutClientDetection timeoutClientDetection = null;
	//private TimeOutEmElection timeoutEmElection = null;
	

	//indici dei messaggi inviati 
	/*private int indexELECTION_RESPONSE = 0;
	private int indexREQUEST_SESSION = 0;
	private int indexEM_EL_DET_RELAY = 0;
	private int indexEM_ELECTION = 0;
	private int indexELECTION_DONE = 0;*/

	//DA TOGLIERE DOPO I TEST
//	private boolean electionResponseAutoEnable = false;
//	private boolean electionDoneAutoEnable = false;
//	private boolean emElectionAutoEnable = false;
//	private boolean emElDetRelayAutoEnable = false;
//	private boolean electionRequestAutoEnable = false;	
	//FINE DA TOGLIERE DOPO I TEST


	//istanza singleton del RelayElectionManager
	private static RelayElectionManager INSTANCE = null;

	
	/**Costruttore per ottenere un RelayElectionManager. Fa partire il Connection Manager e, 
	 * a seconda che imRelay sia a true o meno, si prepara a svolgere il ruolo di Relay oppure
	 * si mette alla ricerca del Relay attuale.
	 * @param imRelay un boolean che a true indica che il nodo è stato creato per essere Relay
	 * @throws Exception
	 */
	private RelayElectionManager(boolean imBigBoss, boolean imRelay,boolean imPossibleBigBoss,boolean imPossibleRelay, RelaySessionManager sessionManager) throws Exception{

		this.actualStatus = RelayStatus.OFF;
		this.imBigBoss = imBigBoss;
		this.imRelay = imRelay;
		this.imPossibleBigBoss = imPossibleBigBoss;
		this.imPossibleRelay = imPossibleRelay;
		this.addObserver(sessionManager);
		this.console = new DebugConsole();
		this.console.setTitle("RELAY ELECTION MANAGER DEBUG CONSOLE");

		try {
		//Solo il bigBoss o un suo sostituto deve avere due interfaccie WIFI, una per il nodo server e una per la rete ad hoc
			if((imBigBoss)||(!imBigBoss && !imRelay && imPossibleBigBoss)){
				relayAPWNICController = WNICFinder.getCurrentWNIC(
						Parameters.NAME_OF_MANAGED_RELAY_INTERFACE,
						Parameters.NAME_OF_MANAGED_NETWORK,
						Parameters.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
			}
			relayAHWNICController = WNICFinder.getCurrentWNIC(
					Parameters.NAME_OF_AD_HOC_RELAY_INTERFACE,
					Parameters.NAME_OF_AD_HOC_NETWORK,
					Parameters.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
		} catch (WNICException e) {System.err.println("ERRORE:"+e.getMessage());System.exit(1);}
		comManager = RelayConnectionFactory.getElectionConnectionManager(this);
		comManager.start();

		//Se parto come Relay BIG BOSS
		if(imBigBoss){ 
			try {
				if(relayAPWNICController.isConnected()&&relayAHWNICController.isConnected()) 
					becomeBigBossRelay();
				else{
					console.debugMessage(Parameters.DEBUG_ERROR,"Questo nodo non può essere il BigBoss dato che non vede l'AP o non è collegato alla rete Ad Hoc");
					throw new Exception("RelayElectionManager: ERRORE: questo nodo non può essere il BigBoss dato che non vede l'AP o non è collegato alla rete Ad Hoc");
				}
			} catch (WNICException e) {
				console.debugMessage(Parameters.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager: ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}

		//Parto da relay secondario
		else if(imRelay){
			searchingBigBossRelay();
		}
		
		//parto come cliente normale  (relay o bigboss passivo)
		else{
			searchingRelay();
		}
	}


	/**Metodo per ottenere l'instanza della classe singleton RelayElectionManager
	 * @param imRelay un boolean che indica se il nodo è il Relay attuale o meno.
	 * @return un riferimento al singleton RelayElectionManager
	 */
	public static RelayElectionManager getInstance(boolean imBigBoss, boolean imRelay, boolean imPossibleBigBoss, boolean imPossibleRelay, RelaySessionManager sessionManager){
		if(INSTANCE == null)
			try {
				INSTANCE = new RelayElectionManager(imBigBoss, imRelay, imPossibleBigBoss, imPossibleRelay, sessionManager);
			} catch (Exception e) {e.printStackTrace();}
			return INSTANCE;
	}
	
	/**Metodo che consente di far si che questo nodo diventi il Relay attuale,
	 * memorizzando l'indirizzo locale come indirizzo del Relay, creando e facendo
	 * partire il RelayPositionAPMonitor, il RelayPositionClientsMonitor,
	 * il RelayBatteryMonitor e il WhoIsRelayServer. Poi passa allo stato di MONITORING.
	 */
	private void becomeBigBossRelay(){

		imBigBoss = true;
		imRelay = false;
		imPossibleBigBoss = false;
		imPossibleRelay = false;

		localRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
		memorizeLocalRelayAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
		if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
		if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();

		try {
			//Monitoraggio RSSI nei confronti del server
			relayPositionAPMonitor = new RelayPositionAPMonitor(
											relayAPWNICController,	
											Parameters.POSITION_AP_MONITOR_PERIOD,
											this);

			//Monitoraggio RSSI client e relay attivi
			//viene fatto partire nel momento in cui si collega o un relay o un cliente
			//in teoria si dovrebbe farlo partire nel momento in cui inizia una sessione ovvero richiesta file da parte del client
			relayPositionMonitor = new RelayPositionMonitor(imBigBoss,
											relayAHWNICController,
											Parameters.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
											Parameters.POSITION_CLIENTS_MONITOR_PERIOD,
											this);
			
			relayPositionMonitor.setLocalRelayAddress(localRelayAddress);
			
			//Monitoraggio della batteria
			relayBatteryMonitor = new RelayBatteryMonitor(Parameters.BATTERY_MONITOR_PERIOD,this);
			//Risponde ai messaggi WHO_IS_RELAY  (in caso di big boss anche ai messaggi WHO_IS_BIG_BOSS
			whoIsRelayServer = new WhoIsRelayServer(imBigBoss,console);
			
			relayPositionAPMonitor.start();
			whoIsRelayServer.start();
			//relayBatteryMonitor.start();
		
			actualStatus = RelayStatus.IDLE;
				
			console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): X -> STATO ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
			
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	//si fa partire nel momento in cui ci sta flusso
	//per fare le provo lo faccio partire nel momento in cui si collega un relay attivo o un client
	public void startRelayPositionMonitoring(){
		relayPositionMonitor.start();
		relayPositionMonitor.startRSSIMonitor();
		actualStatus = RelayStatus.MONITORING;
		console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager Stato MONITORING");
	}
	
	/**Metodo che consente di far si che questo nodo diventi il Relay attuale,
	 * memorizzando l'indirizzo locale come indirizzo del Relay, creando e facendo
	 * partire il RelayPositionAPMonitor, il RelayPositionClientsMonitor,
	 * il RelayBatteryMonitor e il WhoIsRelayServer. Poi passa allo stato di MONITORING.
	 */
	
	private void searchingBigBossRelay(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsBigBoss(BCAST,Parameters.WHO_IS_RELAY_PORT_IN);
			comManager.sendTo(dpOut);
		} catch (IOException e) {console.debugMessage(Parameters.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this,	Parameters.TIMEOUT_SEARCH);
		actualStatus=RelayStatus.WAITING_WHO_IS_RELAY;
		console.debugMessage(Parameters.DEBUG_WARNING,"RelayElectionManager: stato OFF, inviato WHO_IS_BIG_BOSS_RELAY e start del TIMEOUT_SEARCH");
	}

	private void becomRelay(){

		imBigBoss = false;
		imRelay = true;

		localRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
		memorizeLocalRelayAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
		if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
		if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();
		/*Fine Vedere se sta parte serve*/

		//Compresi client e realy secondari
		relayPositionMonitor = new RelayPositionMonitor(imBigBoss,
				relayAHWNICController,
				Parameters.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
				Parameters.POSITION_CLIENTS_MONITOR_PERIOD,
				this);
		
		relayPositionMonitor.setLocalRelayAddress(localRelayAddress);
		relayPositionMonitor.setConnectedRelayAddress(connectedRelayAddress);
		
		//Client -> faccio partire nel momento in cui si collega qualche client...
		relayBatteryMonitor = new RelayBatteryMonitor(Parameters.BATTERY_MONITOR_PERIOD,this);

		whoIsRelayServer = new WhoIsRelayServer(imBigBoss,console);
		
		whoIsRelayServer.start();
		//relayBatteryMonitor.start();
	
		actualStatus = RelayStatus.IDLE;
		
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildAckConnection(connectedRelayInetAddress, Parameters.RELAY_ELECTION_PORT_IN, Parameters.TYPERELAY);
			comManager.sendTo(dpOut);
		} catch (IOException e) {console.debugMessage(Parameters.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");e.getStackTrace();}
	
		console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager.becomeRelay(): X -> STATO IDLE: WhoIsRelayServer partito, ACK_CONNECTION spedito");
		startRelayPositionMonitoring();
	}



	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public synchronized void update(Observable arg0, Object arg1) {

		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){
			DatagramPacket dpIn = (DatagramPacket)arg1;
			relayMessageReader = new RelayMessageReader();
			try {
				relayMessageReader.readContent(dpIn);
			} catch (IOException e) {console.debugMessage(Parameters.DEBUG_ERROR,"RElayElectionManager : errore durante la lettura del pacchetto election");e.printStackTrace();}
			
			
/********************** STATO: WAITING_WHO_IS_RELAY **********************/ 
	
			/*
			 * Relay secondario attivo in attesa di risposta da parte del BigBoss
			 */
			if((relayMessageReader.getCode() == Parameters.IM_BIGBOSS) && 
			   (actualStatus == RelayStatus.WAITING_WHO_IS_RELAY) && 
			   (imRelay)){
					
				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}
				connectedRelayAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedRelayAddress();
				setChanged();
				notifyObservers("BIGBOSS_FOUND:"+relayMessageReader.getActualConnectedRelayAddress());
				console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager: STATO ["+actualStatus.toString()+"], IM_BIG_BOSS arrivato: connectRelayAddress: ["+connectedRelayAddress+"]");
				becomRelay(); //stato idle
			}
			
			/*
			 * Client o relay passivo in attesa di risposta da parte di un relay o BigBoss attivi
			 */
			if((relayMessageReader.getCode() == Parameters.IM_RELAY) && 
			   (actualStatus == RelayStatus.WAITING_WHO_IS_RELAY)){
				
				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: STATO ["+actualStatus.toString()+"]: IM_RELAY arrivato");
				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}
				actualStatus = RelayStatus.IDLE;
				connectedRelayAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedRelayAddress();
				setChanged();
				notifyObservers("RELAY_FOUND:"+connectedRelayAddress);
				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: STATO ["+actualStatus.toString()+"]: connectedRelayAddress: ["+ connectedRelayAddress+"]");
			}
				
/********************** STATO: IDLE,MONITORING *********************/
			
			/*
			 * Conferma da parte di un nodo della connessione al Big Boss
			 */
			if((relayMessageReader.getCode()==Parameters.ACK_CONNECTION) && 
			   (imBigBoss)){
				
				if(relayMessageReader.getTypeNode()==Parameters.TYPERELAY){
					active_relays++;
					setChanged();
					notifyObservers("NEW_CONNECTED_RELAY:"+relayMessageReader.getPacketAddess().toString());
					console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: nuovo relay secondario connesso, IP ["+relayMessageReader.getPacketAddess().toString()+"]");
				}else
					console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: nuovo client o relay,bigboss passivo si è connesso IP ["+relayMessageReader.getPacketAddess().toString()+"]");
				startRelayPositionMonitoring();
			}
			
			/* richiesta di elezione da parte del BigBoss (se il nodo è un possibile sostituto)
			 * 1. Prende atto dell'inizio dell'elezione
   			 * 2. Setta un TIMEOUT_FAIL_TO_ELECT (vedi 2.3.1)
   			 * 3. Setta un TIMEOUT_ELECTION_BEACON, scaduto il quale assume di aver ottenuto un messaggio di ELECTION_BEACON e ELECTION_RELAY_BEACON da parti di tutti i nodi coinvolti nell'elezione e presenti nel prorprio raggio di copertura e completare il calcolo del proprio peso da inviare in UNICAST al BIG BOSS da sostituire.
   			 * 4. Si mette in attesa di messaggi ELECTION_BEACON da parte dei client attivi coinvolti nelle elezione
   			 * 5. Si mette in attesa di messaggi ELECTION_RELAY_BEACON da parte dei relay attivi.
			 */
			else if((relayMessageReader.getCode() == Parameters.ELECTION_BIGBOSS_REQUEST) && 
					(actualStatus == RelayStatus.IDLE) &&
					(imPossibleBigBoss)){
				try {
					console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager possible BigBoss: STATO ["+actualStatus.toString()+"], ELECTION_REQUEST arrivato, AP Visibility ["+ 
							relayAPWNICController.isConnected()+"]");
				} catch (WNICException e1) {e1.printStackTrace();}

				electing = true;
				counter_clients = 0;
				counter_relays = 0;
				active_relays = relayMessageReader.getActiveRelays();
				W = -1;

				try {
					//ap Visibility == true
					if(relayAPWNICController.isConnected()){
						timeoutElectionBeacon = RelayTimeoutFactory.getTimeOutElectionBeacon(this, Parameters.TIMEOUT_ELECTION_BEACON);
						timeoutFailToElect = RelayTimeoutFactory.getTimeOutFailToElect(this, Parameters.TIMEOUT_FAIL_TO_ELECT);
						actualStatus  = RelayStatus.ACTIVE_NORMAL_ELECTION;
						console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: STATO ["+actualStatus.toString()+"], TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti \nAttesa di messaggi ELECTION_BACON e ELECTION_RELAY_BACON");
					} 			
				} catch (WNICException e) {e.printStackTrace();}
				setChanged();
				notifyObservers("ELECTION_BIGBOSS_REQUEST_RECEIVED");
			}
			
			
			/* richiesta di elezione da parte del BigBoss (relay attivo)
			 * 1. Prende atto dell'inizio dell'elezione
			 * 2. Effettua un BROADCAST del messaggio ELECTION_RELAY_BEACON destinati ai nodi possibili sostituti (Big Boss passivi) con visibilità dell'AP
			 * 3. Setta un TIMEOUT_FAIL_TO_ELECT (vedi 2.3.1)
			 * 4. in attesa del messaggio di ELECTION_BIGBOSS_DONE da parte del nuovo BIG BOSS appena eletto..
			 */
			else if((relayMessageReader.getCode() == Parameters.ELECTION_BIGBOSS_REQUEST) && 
					(actualStatus == RelayStatus.MONITORING) &&
					(imRelay)){
				electing = true;
				
				DatagramPacket dpOut = null;
				try {
					dpOut = RelayMessageFactory.buildElectioRelayBeacon(indexELECTION_RELAY_BEACON,BCAST,Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					indexELECTION_RELAY_BEACON++;
					actualStatus  = RelayStatus.ACTIVE_NORMAL_ELECTION;
				} catch (IOException e) {console.debugMessage(Parameters.DEBUG_ERROR,"Errore nel spedire il messaggio di ELECTION_RELAY_BEACON");e.getStackTrace();}
					
				timeoutFailToElect = RelayTimeoutFactory.getTimeOutFailToElect(this, Parameters.TIMEOUT_FAIL_TO_ELECT);
				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: ELECTION_BIGBOSS_REQUEST arrivato STATO MONITORING -> ACTIVE_NORMAL_ELECTION\n" +
															"ELECTION_RELAY_BEACON spedito in broadcast, TIMEOUT_FAIL_TO_ELECT partiti");
				setChanged();
				notifyObservers("ELECTION_BIGBOSS_REQUEST_RECEIVED");
			}
			
			/** STATO: ACTIVE_NORMAL_ELECTION **/
			/* Arrivo del messaggio di ELECTION_BEACON (nodi possibile sostituti sia BigBoss sia relay Attivi)
			 * 1. Se questo è il primo messaggio ricevuto resetta ed incrementa il counter_clients
			 * 2. Se non è il primo messaggio ricevuto incrementa il counter_clients
			 */
			else if((relayMessageReader.getCode() == Parameters.ELECTION_BEACON) && 
					(actualStatus == RelayStatus.ACTIVE_NORMAL_ELECTION) &&
					(imPossibleRelay || imPossibleBigBoss)){

				counter_clients++;
				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectioneManager "+((imPossibleBigBoss)?"ImPossibleBigBoss":"ImPossibleRelay")+" ELECTION_BEACON arrivato, counterClient = ["+counter_clients+"]");
			}
			
			/* Arrivo del messaggio di ELECTION_RELAY_BEACON (solo nodi Possibile BigBoss con visibilità dell'AP):
			 * 1. Se questo è il primo messaggio ricevuto resetta ed incrementa il counter_relays
			 * 2. Se non è il primo messaggio ricevuto incrementa il counter_relays
			 */
			else if ((relayMessageReader.getCode()==Parameters.ELECTION_RELAY_BEACON)&&
					(actualStatus == RelayStatus.ACTIVE_NORMAL_ELECTION)&&
					(imPossibleBigBoss)){
				counter_relays++;
				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectioneManager ImPossibleBigBoss ELECTION_RELAY_BEACON arrivato, counterRelayst = ["+counter_relays+"]");
			}
			
		}
		
		
		
		
		if(arg1 instanceof String){

			String event = (String) arg1;
			
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.WAITING_WHO_IS_RELAY){
				console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				if(imRelay)
					searchingBigBossRelay();
				else
					searchingRelay();
			}
			/*[DISCONNECTION_WARNING sollevato]
			 * imRelay = true
			 * status = monitoring
			 */
			else if(event.equals("DISCONNECTION_WARNING") && actualStatus == RelayStatus.MONITORING){
				console.debugMessage(Parameters.DEBUG_WARNING, "RelayElectionManager: STATO MONITORING: DISCONNECTION_WARNING sollevato da uno dei Monitor");
				
				imBigBoss = false;
				imRelay = false;

				connectedRelayAddress = null;
				connectedRelayInetAddress = null;
				
				W = -1;
				maxW = -1;

				whoIsRelayServer.close();
				relayPositionAPMonitor.close();
				relayPositionMonitor.close();
				relayBatteryMonitor.close();

				console.debugMessage(Parameters.DEBUG_WARNING,"RelayElectionManager: STATO MONITORING: chiusura dei Monitor e del whoIsRelayServer");

				possibleRelay = new Vector<Couple>();

				DatagramPacket dpOut = null;

				try {
					//invio ai Relay
					dpOut = RelayMessageFactory.buildElectionBigBossRequest(BCAST, Parameters.RELAY_ELECTION_PORT_IN, active_relays);
					comManager.sendTo(dpOut);
					dpOut = RelayMessageFactory.buildElectionBigBossRequest(BCAST, Parameters.CLIENT_PORT_ELECTION_IN,active_relays);
					comManager.sendTo(dpOut);
				} catch (IOException e) {
					e.printStackTrace();
				}

				timeoutToElect = RelayTimeoutFactory.getTimeOutToElect(this, Parameters.TIMEOUT_TO_ELECT);

				electing = true;

				actualStatus = RelayStatus.WAITING_RESPONSE;

				console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: STATO MONITORING -> WAITING_RESPONSE: " +
						"DISCONNECTION_WARNING sollevato da uno dei Monitor, ELECTION_REQUEST inviato, " +
				"TIMEOUT_TO_ELECT partito ");
			}
			
		}
		
	}


			/*[ELECTION_DONE ricevuto] / 
			 *relayAddress = <IP del NEW RELAY>
			 *reset TIMEOUT_SEARCH
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_DONE && 
					actualStatus == RelayStatus.IDLE 
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || electionDoneAutoEnable)
			){

				if(timeoutSearch != null){
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				debugPrint("RelayElectionManager: STATO IDLE: ELECTION_DONE arrivato");

				electing = false;

				if(actualRelayAddress == null || 
						!actualRelayAddress.equals(relayMessageReader.getNewRelayAddress())){

					if(!relayMessageReader.getNewRelayAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS)){
						actualRelayAddress = relayMessageReader.getNewRelayAddress();
						memorizeRelayAddress();
					}
					else becomeRelay();

					setChanged();
					notifyObservers("NEW_RELAY:"+actualRelayAddress);

					//RIPROPAGAZIONE UNA TANTUM DELL'ELECTION_DONE
					comManager.sendTo(prepareRepropagation(dpIn));

					debugPrint("RelayElectionManager: STATO IDLE: ELECTION_DONE ripropagato: indirizzo: " + 
							dpIn.getAddress().getHostAddress() + " porta: " +dpIn.getPort());
				}
				else debugPrint("RelayElectionManager: STATO IDLE: ELECTION_DONE non ripropagato");

				debugPrint("RelayElectionManager: STATO IDLE: ELECTION_DONE ricevuto: actualRelayAddress: "+ 
						actualRelayAddress);
			}


			

			/*CASO CHE SERVE PER FAR SI CHE IL RELAY SENZA VISIBILITA' DELL'AP SAPPIA CMQ CHI E' IL NUOVO RELAY
			PERCHE' POI, POTENDO RIENTRARE IN VISIBILITA' DELL'AP, QUANDO SCATTA UNA ULTERIORE ELEZIONE, PUO'
			PARTECIVARVI (COME E' SUO DIRITTO)VISTO CHE CONOSCE L'INDIRIZZO DELL'ORMAI OLD
			RELAY ELETTO TRAMITE ELEZIONE DI EMERGENZA (a chi manderebbe sennò la sua ELECTION_RESPONSE ? )*/
			/*else if(relayMessageReader.getCode() == Parameters.EM_ELECTION && 
					actualStatus == RelayStatus.IDLE 
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || emElectionAutoEnable)
			){

				debugPrint("RelayElectionManager: STATO IDLE: EM_ELECTION arrivato");

				actualRelayAddress = dpIn.getAddress().getHostAddress();
				maxW = relayMessageReader.getW();
				memorizeRelayAddress();

				W =-1;
				counter_clients = 0;

				firstEM_ELECTIONarrived = true;

				timeoutEmElection = RelayTimeoutFactory.getTimeOutEmElection(this, Parameters.TIMEOUT_EM_ELECTION);
				
				actualStatus = RelayStatus.WAITING_END_EMERGENCY_ELECTION;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");

				debugPrint("RelayElectionManager: STATO IDLE -> WAITING_END_EMERGENCY_ELECTION : " +
						"EM_ELECTION arrivato e actualRelayAddress: " + actualRelayAddress +" maxW: "+ maxW );
				debugPrint("RelayElectionManager: STATO IDLE -> WAITING_END_EMERGENCY_ELECTION : " +
				"e TIMEOUT_EM_ELECTION partito ");		
			}
			/**FINE STATO IDLE**/	



			/**INIZIO STATO ACTIVE_NORMAL_ELECTION**/	


			/**FINE STATO ACTIVE_NORMAL_ELECTION**/	



			/**INIZIO STATO WAITING_END_NORMAL_ELECTION**/

			/*[ELECTION_DONE arrivato && <IP in ELECTION_DONE> != localhost] /
			 * reset TIMEOUT_FAIL_TO_ELECT
			 * relayAddress = <IP del NEW RELAY>
			 * invio ELECTION_DONE
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_DONE && 
					actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION &&
					!relayMessageReader.getNewRelayAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) 
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || electionDoneAutoEnable)		
			){

				if(timeoutFailToElect != null){
					timeoutFailToElect.cancelTimeOutFailToElect();
					timeoutFailToElect = null;
				}

				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION: ELECTION_DONE arrivato: nuovo Relay: " +
						relayMessageReader.getNewRelayAddress()+ " != " + Parameters.RELAY_AD_HOC_ADDRESS );

				actualRelayAddress = relayMessageReader.getNewRelayAddress();
				memorizeRelayAddress();
				
				imRelay = false;
				electing = false;
				W = -1;
				counter_clients = 0;
				
				actualStatus = RelayStatus.IDLE;
				
				setChanged();
				notifyObservers("NEW_RELAY:"+actualRelayAddress);
				
				//RIPROPAGAZIONE UNA TANTUM DELL'ELECTION_DONE
				comManager.sendTo(prepareRepropagation(dpIn));
				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION: ELECTION_DONE ripropagato verso indirizzo: " +
						dpIn.getAddress().getHostAddress() + " porta: " +dpIn.getPort());

				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> IDLE: " +
						"ELECTION_DONE arrivato && " + relayMessageReader.getNewRelayAddress()+
						" != " + Parameters.RELAY_AD_HOC_ADDRESS);
			}

			/*[ELECTION_DONE arrivato && <IP in ELECTION_DONE> == localhost] /
			 * reset TIMEOUT_FAIL_TO_ELECT
			 * status = Monitoring
			 * relayAddress = localhost
			 * imRelay = true 
			 * invia REQUEST_SESSION 
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_DONE && 
					actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION &&
					relayMessageReader.getNewRelayAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS)
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || electionDoneAutoEnable)	
			){

				if(timeoutFailToElect != null){
					timeoutFailToElect.cancelTimeOutFailToElect();
					timeoutFailToElect = null;
				}

				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION: ELECTION_DONE arrivato && " + 
						relayMessageReader.getNewRelayAddress()+
						" == " + Parameters.RELAY_AD_HOC_ADDRESS);

				electing = false;
				W = -1;
				counter_clients = 0;

				becomeRelay();
				
				setChanged();
				notifyObservers("NEW_RELAY:"+ Parameters.RELAY_AD_HOC_ADDRESS);
				
				//RIPROPAGAZIONE UNA TANTUM DELL'ELECTION_DONE
				comManager.sendTo(prepareRepropagation(dpIn));
				debugPrint("RelayElectionManager: STATO IDLE: ELECTION_DONE ripropagato verso indirizzo: " +
						dpIn.getAddress().getHostAddress() + " porta: " +dpIn.getPort());
				
				//NON LO MANDO IO IL REQUEST_SESSION MA CI PENSA IL SESSION_MANAGER
				/*
				DatagramPacket dpOut = null;

				try {
					dpOut = RelayMessageFactory.buildRequestSession(indexREQUEST_SESSION, relayInetAddress, Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
					comManager.sendTo(dpOut);
					indexREQUEST_SESSION++;
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/

				//debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> IDLE: " +
				//"ELECTION_DONE arrivato && <IP in ELECTION_DONE> == localhost");
				
				//NON LO MANDO IO IL REQUEST_SESSION MA CI PENSA IL SESSION_MANAGER
				/*debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> IDLE: " +
						"REQUEST_SESSION inviato al nuovo Relay: " + actualRelayAddress);*/
			//}


			/*[EM_EL_DET_RELAY arrivato || EM_EL_DET_CLIENT arrivato]
			 * reset TIMEOUT_FAIL_TO_ELECT
			 * relayAddress = null
			 * counter_clients = 0
			 * if(EM_EL_DET_CLIENT arrivato)counter_clients++
			 * start TIMEOUT_CLIENT_DETECTION
			 * status = ActiveEmergencyElection
			 */
			/*else if((relayMessageReader.getCode() == Parameters.EM_EL_DET_RELAY ||
					relayMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT) &&
					actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || emElDetRelayAutoEnable)		
			){

				actualRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
				memorizeRelayAddress();

				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION:  EM_EL_DET_"+
						(relayMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT?"CLIENT":"RELAY")+
				" arrivato");

				counter_clients = 0;
				W = -1;

				

				if(relayMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT) counter_clients++;

				timeoutClientDetection = RelayTimeoutFactory.getTimeOutClientDetection(this, Parameters.TIMEOUT_CLIENT_DETECTION);

				actualStatus = RelayStatus.ACTIVE_EMERGENCY_ELECTION;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");
				
				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> " +
						"ACTIVE_EMERGENCY_ELECTION: EM_EL_DET_"+
						(relayMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT?"CLIENT":"RELAY")+
						" arrivato, TIMEOUT_CLIENT_DETECTION paritito  (counter_clients: " 
						+ counter_clients+")");
			}


			/*CASO IN CUI POTREBBE ARRIVARE UN EM_ELECTION DURANTE IL WAITING_END_NORMAL_ELECTION
			 NELLA REALTA' NON DOVREBEB MAI CAPITARE */
			/*else if(relayMessageReader.getCode() == Parameters.EM_ELECTION &&
					actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || emElectionAutoEnable)		
			){

				actualRelayAddress = dpIn.getAddress().getHostAddress();
				maxW = relayMessageReader.getW();
				memorizeRelayAddress();

				firstEM_ELECTIONarrived = true;

				counter_clients = 0;
				W = -1;

				timeoutClientDetection = RelayTimeoutFactory.getTimeOutClientDetection(this, Parameters.TIMEOUT_CLIENT_DETECTION);

				actualStatus = RelayStatus.ACTIVE_EMERGENCY_ELECTION;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");
				
				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> " +
						"ACTIVE_EMERGENCY_ELECTION: EM_ELECTION arrivato, actualRelayAddress: " + 
						actualRelayAddress +" maxW: "+ maxW );
				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> " +
				"ACTIVE_EMERGENCY_ELECTION: e TIMEOUT_CLIENT_DETECTION partito");
			}
			/**FINE STATO WAITING_END_NORMAL_ELECTION**/



			/**INIZIO STATO ACTIVE_EMERGENCY_ELECTION**/

			/*[EM_EL_DET_CLIENT arrivato]
			 * counter_clients++
			 */
			/*else if(relayMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT &&
					actualStatus == RelayStatus.ACTIVE_EMERGENCY_ELECTION){

				counter_clients++;

				debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: EM_EL_DET_CLIENT arrivato " +
						"(counter_clients: " + counter_clients+")");
			}


			/*[EM_ELECTION arrivato]
			 * if(<W arrivato> > maxW){
			 * maxW = W (arrivato da fuori)
			 * relayAddress = <IP arrivato>;
			 * }
			 *if(<W arrivato> == maxW){
			 *if(<IP arrivato> < relayAddress) relayAddress = <IP arrivato>
			 *}
			 */
			/*else if(relayMessageReader.getCode() == Parameters.EM_ELECTION &&
					actualStatus == RelayStatus.ACTIVE_EMERGENCY_ELECTION
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || emElectionAutoEnable)	
			){

				firstEM_ELECTIONarrived = true;
				compareWithLocalData(relayMessageReader.getW(), dpIn.getAddress().getHostAddress());	

				debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: EM_ELECTION arrivato," +
						" actualRelayAddress: " + actualRelayAddress +" maxW: "+ maxW );
			}
			/**FINE STATO ACTIVE_EMERGENCY_ELECTION**/



			/**INIZIO STATO WAITING_END_EMERGENCY_ELECTION**/

			/*[EM_ELECTION arrivato]
			 * if(<W arrivato> > maxW){
			 * maxW = W (arrivato da fuori)
			 * relayAddress = <IP arrivato>;
			 * }
			 *if(<W arrivato> == maxW){
			 *if(<IP arrivato> < relayAddress) relayAddress = <IP arrivato>
			 *}
			 */
			/*else if(relayMessageReader.getCode() == Parameters.EM_ELECTION &&
					actualStatus == RelayStatus.WAITING_END_EMERGENCY_ELECTION
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS)	|| emElectionAutoEnable)	
			){

				compareWithLocalData(relayMessageReader.getW(), dpIn.getAddress().getHostAddress());

				debugPrint("RelayElectionManager: STATO WAITING_END_EMERGENCY_ELECTION: EM_ELECTION arrivato," +
						" actualRelayAddress: " + actualRelayAddress +" maxW: "+ maxW );
			}

			/**FINE STATO WAITING_END_EMERGENCY_ELECTION**/



			/**INIZIO STATO WAITING_RESPONSE**/

			/*[ELECTION_RESPONSE arrivato]
			 * if(!responses)responses = true
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_RESPONSE &&
					actualStatus == RelayStatus.WAITING_RESPONSE
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || electionResponseAutoEnable) 		
			){

				possibleRelay.add(new Couple(dpIn.getAddress().getHostAddress(),relayMessageReader.getW()));

				Collections.sort(possibleRelay);

				maxW = possibleRelay.get(0).getWeight();
				actualRelayAddress = possibleRelay.get(0).getAddress();
				memorizeRelayAddress();

				debugPrint("RelayElectionManager: STATO WAITING_RESPONSE: ELECTION_RESPONSE arrivato: " +
						"miglior nodo giunto " + actualRelayAddress + " con peso: " + maxW);
			}
			/**FINE STATO WAITING_RESPONSE **/


		//}


		//PARTE PER LA GESTIONE DELLE NOTIFICHE NON PROVENIENTI DALLA RETE
	



			/**INIZIO STATO ACTIVE_NORMAL_ELECTION**/	

			/*[TIMEOUT_ELECTION_BEACON scattato] --> WeightCalculation
			 */
			/*else if(event.equals("TIMEOUTELECTIONBEACON") &&
					actualStatus == RelayStatus.ACTIVE_NORMAL_ELECTION){

				debugPrint("RelayElectionManager: STATO ACTIVE_NORMAL_ELECTION: TIMEOUT_ELECTION_BEACON scattato" +
				", procedo al calcolo del W");

				//Stato instabile WeightCalculation
				weightCalculation();

				debugPrint("RelayElectionManager: STATO ACTIVE_NORMAL_ELECTION: W calcolato: " + W);

				/*[W calcolato]
				 * invia ELECTION_RESPONSE
				 * start TIMEOUT_FAIL_TO_ELECT  <-- (MA S'ERA FATTO PARTIRE PRIMAAAA !!!!)
				 * status = WaitingEndNormalElection
				 */
				/*DatagramPacket dpOut = null;

				try {
					dpOut = RelayMessageFactory.buildElectionResponse(indexELECTION_RESPONSE, W, relayInetAddress, Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					indexELECTION_RESPONSE++;
				} catch (IOException e) {
					e.printStackTrace();
				}

				actualStatus = RelayStatus.WAITING_END_NORMAL_ELECTION;

				debugPrint("RelayElectionManager: STATO ACTIVE_NORMAL_ELECTION -> " +
						"WAITING_END_NORMAL_ELECTION: TIMEOUT_ELECTION_BEACON scattato, W calcolato, " +
				"ELECTION_RESPONSE inviato");
			}
			/**FINE STATO ACTIVE_NORMAL_ELECTION**/	



			/**INIZIO STATO WAITING_END_NORMAL_ELECTION**/

			/*[TIMEOUT_FAIL_TO_ELECT scattato]
			 * relayAddress = null
			 * counter_clients = 0
			 * invia EM_EL_DET_RELAY
			 * start TIMEOUT_CLIENT_DETECTION
			 * status = ActiveEmergencyElection
			 */
			/*else if(event.equals("TIMEOUTFAILTOELECT") &&
					actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION){

				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION: " +
				"TIMEOUT_FAIL_TO_ELECT scattato");

				actualRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
				memorizeRelayAddress();

				counter_clients = 0;
				W = -1;
				maxW = -1;

				DatagramPacket dpOut = null;

				try {
					dpOut = RelayMessageFactory.buildEmElDetRelay(indexEM_EL_DET_RELAY, BCAST, Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					indexEM_EL_DET_RELAY++;
				} catch (IOException e) {
					e.printStackTrace();
				}

				timeoutClientDetection = RelayTimeoutFactory.getTimeOutClientDetection(this, Parameters.TIMEOUT_CLIENT_DETECTION);

				actualStatus = RelayStatus.ACTIVE_EMERGENCY_ELECTION;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");
				
				debugPrint("RelayElectionManager: STATO WAITING_END_NORMAL_ELECTION -> " +
						"ACTIVE_EMERGENCY_ELECTION: TIMEOUT_FAIL_TO_ELECT scattato, EM_EL_DET_RELAY inviato " +
				"e TIMEOUT_CLIENT_DETECTION partito");
			}
			/**FINE STATO WAITING_END_NORMAL_ELECTION**/



			/**INIZIO STATO ACTIVE_EMERGENCY_ELECTION**/

			/*[TIMEOUT_CLIENT_DETECTION scattato] --> WeightCalculation
			 */
			/*else if(event.equals("TIMEOUTCLIENTDETECTION") &&
					actualStatus == RelayStatus.ACTIVE_EMERGENCY_ELECTION){

				debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: TIMEOUT_CLIENT_DETECTION," +
				" procedo al calcolo del W");

				//Stato instabile WeightCalculation
				weightCalculation();

				debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: W calcolato: "+ W);

				/*[W calcolato]
				 * if(!firstEM_ELECTIONarrived){
				 * relayAddress = localhost
				 * maxW = W
				 * status = WaitingEndEmergencyElection
				 * invia EM_ELECTION
				 * } else {
				 * if(W>maW){
				 *  invia EM_ELECTION
				 * } 
				 * }
				 */

			/*	try {
					if(relayWNICController.isConnected() && (!firstEM_ELECTIONarrived || compareAfterWeightCalculation())){

						actualRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
						memorizeRelayAddress();
						maxW = W;

						DatagramPacket dpOut = null;

						try {
							//invio ai Relay
							dpOut = RelayMessageFactory.buildEmElection(indexEM_ELECTION, W, BCAST, Parameters.RELAY_ELECTION_PORT_IN);
							comManager.sendTo(dpOut);
							dpOut = RelayMessageFactory.buildEmElection(indexEM_ELECTION, W, BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
							comManager.sendTo(dpOut);
							indexEM_ELECTION++;
						} catch (IOException e) {
							e.printStackTrace();
						}

						debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: " +
						"TIMEOUT_CLIENT_DETECTION scattato, W calcolato e EM_ELECTION inviato");		
					}
					else debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: " +
					"TIMEOUT_CLIENT_DETECTION scattato, W calcolato e EM_ELECTION non inviato");

					debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: " +
							"actualRelayAddress = " +actualRelayAddress + " con W: " + maxW);

				} catch (WNICException e) {
					debugPrint("RalayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION: ERRORE: " + e.getMessage());
				}

				firstEM_ELECTIONarrived = true;

				timeoutEmElection = RelayTimeoutFactory.getTimeOutEmElection(this, Parameters.TIMEOUT_EM_ELECTION);

				actualStatus = RelayStatus.WAITING_END_EMERGENCY_ELECTION;

				debugPrint("RelayElectionManager: STATO ACTIVE_EMERGENCY_ELECTION -> " +
						"WAITING_END_EMERGENCY_ELECTION:  TIMEOUT_CLIENT_DETECTION scattato e " +
				"TIMEOUT_EM_ELECTION partito ");
			}
			/**FINE STATO ACTIVE_EMERGENCY_ELECTION**/



			/**INIZIO STATO WAITING_END_EMERGENCY_ELECTION**/

			/*[TIMEOUT_EM_ELECTION scattato && relayAddress != localhost]
			 * imRelay = false
			 * status = Idle
			 */

			/*[TIMEOUT_EM_ELECTION scattato && relayAddress == localhost]
			 * imRelay = true
			 * status = monitoring
			 */
			/*else if(event.equals("TIMEOUTEMELECTION") &&
					actualStatus == RelayStatus.WAITING_END_EMERGENCY_ELECTION){

				electing = false;

				firstEM_ELECTIONarrived = false;

				debugPrint("RelayElectionManager: STATO WAITING_END_EMERGENCY_ELECTION: " +
						"TIMEOUT_EM_ELECTION scattato e actualRelayAddress: " 
						+ actualRelayAddress + " con W: " +maxW);

				counter_clients = 0;
				maxW = -1;
				W = -1;

				if(!actualRelayAddress.equals(Parameters.RELAY_AD_HOC_ADDRESS)){

					imRelay = false;
					actualStatus = RelayStatus.IDLE;

					debugPrint("RelayElectionManager: STATO WAITING_END_EMERGENCY_ELECTION -> IDLE: " +
					"TIMEOUT_EM_ELECTION scattato e imRelay == false");
				}

				else {

					debugPrint("RelayElectionManager: STATO WAITING_END_EMERGENCY_ELECTION -> MONITORING: " +
					"TIMEOUT_EM_ELECTION scattato e imRelay == true");

					becomeRelay();
				}		
				
				//avverto il SessionManager dell'identità del nuovo Relay
				setChanged();
				notifyObservers("NEW_EM_RELAY:"+actualRelayAddress);
				
			}
			/**FINE STATO WAITING_END_EMERGENCY_ELECTION**/






			/**INIZIO STATO WAITING_RESPONSE**/

			/*[TIMEOUT_TO_ELECT scattato]/
			 * if(responces){
			 * invio ELECTION_DONE
			 * }
			 * status = IDLE
			 */
			/*else if(event.equals("TIMEOUTTOELECT") &&
					actualStatus == RelayStatus.WAITING_RESPONSE){

				electing = false;
				firstEM_ELECTIONarrived = false;

				debugPrint("RelayElectionManager: STATO WAITING_RESPONSE: TIMEOUT_TO_ELECT scattato ");

				if (!possibleRelay.isEmpty()) {

					DatagramPacket dpOut = null;

					try {
						//invio ai Relay
						dpOut = RelayMessageFactory.buildElectionDone(
								indexELECTION_DONE, actualRelayAddress,
								BCAST, Parameters.RELAY_ELECTION_PORT_IN);
						comManager.sendTo(dpOut);
						dpOut = RelayMessageFactory.buildElectionDone(
								indexELECTION_DONE, actualRelayAddress,
								BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
						comManager.sendTo(dpOut);
						indexELECTION_DONE++;
						firstELECTION_DONEsent = true;
					} catch (IOException e) {
						e.printStackTrace();
					}

					//faccio sapere al SESSIONMANAGER chi ho appena eletto
					setChanged();
					notifyObservers("NEW_RELAY:" + actualRelayAddress);
					
					System.err
					.println("RelayElectionManager: STATO WAITING_RESPONSE -> IDLE: TIMEOUT_TO_ELECT scattato e " +
					"ELECTION_DONE inviato");
					System.err
					.println("RelayElectionManager: STATO WAITING_RESPONSE -> IDLE: nodo selezionato: " + 
							actualRelayAddress + " con peso: " + maxW);
				}

				maxW = -1;				

				actualStatus = RelayStatus.IDLE;
			}
			/**FINE STATO WAITING_RESPONSE**/
		//}
	//}




	//METODI PRIVATI
	/**Metodo che consente di mettersi alla ricerca di un Relay (se non si è in elezione)
	 * inviando un WHO_IS_RELAY, facendo partire subito dopo 
	 * il TIMEOUT_SEARCH  e impostando lo stato in IDLE
	 */
	private void searchingRelay(){
		localRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
		memorizeLocalRelayAddress();

		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCAST, Parameters.WHO_IS_RELAY_PORT_IN);
			comManager.sendTo(dpOut);
		} catch (IOException e) {e.printStackTrace();}

		timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this, Parameters.TIMEOUT_SEARCH);
		actualStatus = RelayStatus.WAITING_WHO_IS_RELAY;

		console.debugMessage(Parameters.DEBUG_INFO,"RelayElectionManager: STATO WAITING_WHO_IS_RELAY: " +
			"WHO_IS_RELAY inviato e TIMEOUT_SEARCH partito");
	}

	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeLocalRelayAddress(){
		try {
			localRelayInetAddress = InetAddress.getByName(localRelayAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeConnectedRelayAddress(){
		try {
			connectedRelayInetAddress = InetAddress.getByName(connectedRelayAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}


	/**Metodo per calcolare il W tramite il WeightCalculation a cui passo il RelayWNICController 
	 * e il numero di clients rilevati 
	 */
//	private void weightCalculation(){
//
//		W = WeightCalculator.calculateWeight(relayWNICController, counter_clients);
//		System.out.println("RelayElectionManager.weightCalculation(): STATO INSTABILE WEIGHT_CALCULATION: " +
//				"calcolato W = " + W);
//	}


	/**Metodo per confrontare il W proveniente dal messaggio 
	 * @param externalW un double che rappresenta il W arrivato con il messaggio EM_ELECTION
	 * @param externalAddress una String che rappresenta l'indirizzo del mittente del messaggio EM_ELECTION
	 */
//	private void compareWithLocalData(double externalW, String externalAddress){
//
//		//DEBUG
//		//debugPrint("externalAddress: " + externalAddress);
//
//		if(externalW > maxW){
//
//			maxW = externalW;
//			System.out.println("RelayElectionManager.compareWithLocalData(): (externalW > maxW)");
//			System.out.println("RelayElectionManager.compareWithLocalData(): memorizzo "+externalAddress+
//			" perchè ha W più alto");
//			actualRelayAddress = externalAddress;
//			memorizeRelayAddress();
//		}
//		else if(externalW == maxW){
//
//			if((actualRelayAddress.compareTo(externalAddress))>0){
//
//				System.out.println("RelayElectionManager.compareWithLocalData(): (externalW == maxW)");
//				System.out.println("RelayElectionManager.compareWithLocalData(): memorizzo "+externalAddress+
//				" perchè ha IP più basso");
//				actualRelayAddress = externalAddress;
//				memorizeRelayAddress();				
//			}
//		}	
//	}


	/**Metodo da lanciare appena finito di calcolare il proprio W
	 * @return true se il W appena calcolato è maggiore del massimo W ottenuto dai messaggi 
	 * EM_ELECTION, false altrimenti
	 */
//	private boolean compareAfterWeightCalculation(){
//
//		debugPrint("compareAfterWeightCalculation");
//
//		if(W < 0)return false;
//
//		if(W > maxW){
//			System.out.println("RelayElectionManager.compareAfterWeightCalculation(): (W > maxW)");
//			System.out.println("RelayElectionManager.compareAfterWeightCalculation(): " +
//			"Memorizzo il mio indirizzo come quello del NEW RELAY perchè ha W più alto");
//			return true;
//		}
//		else if(W == maxW){
//
//			if((actualRelayAddress.compareTo(actualRelayAddress))<0){
//				System.out.println("RelayElectionManager.compareAfterWeightCalculation(): (W == maxW)");
//				System.out.println("RelayElectionManager.compareAfterWeightCalculation(): " +
//				"Memorizzo il mio indirizzo come quello del NEW RELAY perchè ha IP più basso");
//				return true;
//			}
//		}
//		return false;
//	}


	/**Metodo per costruire il pacchetto di ELECTION_DONE da ripropagare
	 * @param il DatagramPacket contenente il messaggio ELECTION_DONE da ripropagare
	 * @return il DatagramPacket pronto per essere inviato per ripropagare il messaggio ELECTION_DONE
	 */
	private DatagramPacket prepareRepropagation(DatagramPacket dpIn){

		dpIn.setAddress(BCAST);
		dpIn.setPort(Parameters.RELAY_ELECTION_PORT_IN);
		return dpIn;
	}


	/**Metodo da chiamare in caso il Relay appena eletto non risponda all'ELECTION_DONE con un REQUEST_SESSION
	 * affinchè il Relay uscente selezioni un altro tra i possibili Relay registrati tramite gli ELECTION_RESPONSE
	 * come nuovo Relay
	 */
//	public synchronized void chooseAnotherRelay(){
//
//		if (firstELECTION_DONEsent) {
//
//			if (possibleRelay.size() > 1) {
//
//				possibleRelay.remove(0);
//
//				DatagramPacket dpOut = null;
//				
//				try {
//					dpOut = RelayMessageFactory.buildElectionDone(
//							indexELECTION_DONE, possibleRelay.get(0)
//							.getAddress(), BCAST,
//							Parameters.RELAY_ELECTION_PORT_IN);
//
//					comManager.sendTo(dpOut);
//
//					dpOut = RelayMessageFactory.buildElectionDone(
//							indexELECTION_DONE, possibleRelay.get(0)
//							.getAddress(), BCAST,
//							Parameters.CLIENT_PORT_ELECTION_IN);
//
//					comManager.sendTo(dpOut);
//
//					indexELECTION_DONE++;
//
//					System.out.println("RelayElectionManager.chooseAnotherRelay(): NUOVO ELECTION_DONE SPEDITO");
//
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			} else
//				System.out
//				.println("RelayElectionManager.chooseAnotherRelay(): Non ci sono altri possibili Relay rilevati");
//		} else
//			System.out
//			.println("RelayElectionManager.chooseAnotherRelay(): impossibile inviare un nuovo ELECTION_DONE " +
//			"perchè non ne è stato inviato uno in precendenza");
//	}
	
	
	/**Metodo per avere stampate di debug
	 * @param message una String che rappresenta il messaggio da presentare
	 */
//	private void debugPrint(String message){
//		System.err.println(message);
//		if(debugConsolle != null)this.debugConsolle.debugMessage(message);	
//
//	}


	public RelayCM getComManager() {return comManager;}
	public void setComManager(RelayCM comManager) {this.comManager = comManager;}

	public RelayStatus getActualStatus() {return actualStatus;}
	public void setActualStatus(RelayStatus actualStatus) {this.actualStatus = actualStatus;}
	public String getLocalRelayAddress() {return localRelayAddress;}
	public String getConnectedRelayAddress() {return connectedRelayAddress;}
	public void setLocalRelayAddress(String localRelayAddress) {this.localRelayAddress = localRelayAddress;}
	public void setConnectedRelayAddress(String connectedRelayAddress) {this.connectedRelayAddress = connectedRelayAddress;}
	public InetAddress getLocalRelayInetAddress() {return localRelayInetAddress;}
	public InetAddress getConnectedRelayInetAddress() {return connectedRelayInetAddress;}
	public void setLocalRelayInetAddress(InetAddress localRelayInetAddress) {this.localRelayInetAddress = localRelayInetAddress;}
	public void setConnectedRelayInetAddress(InetAddress connectedRelayInetAddress) {this.connectedRelayInetAddress = connectedRelayInetAddress;}

	public boolean isImRelay() {return imRelay;}
	public boolean isImBigBoss(){return imBigBoss;}
	public boolean isImPossibleBigBoss(){return imPossibleBigBoss;}
	public boolean isImPossibleRelay(){return imPossibleRelay;}
	public void setImRelay(boolean imRelay) {this.imRelay = imRelay;}
	public void setImBigBoss(boolean imBigBoss) {this.imBigBoss = imBigBoss;}
	public void setImPossibleBigBoss(boolean imPossibleBigBoss) {this.imPossibleBigBoss = imPossibleBigBoss;}
	public void setImPossibleRelay(boolean imPossibleRelay) {this.imPossibleRelay = imPossibleRelay;}
	
	public RelayMessageReader getRelayMessageReader() {return relayMessageReader;}
	public void setRelayMessageReader(RelayMessageReader relayMessageReader) {this.relayMessageReader = relayMessageReader;}

	public RelayWNICController getRelayAHWNICController() {return relayAHWNICController;}
	public RelayWNICController getRelayAPWNICController() {return relayAPWNICController;}
	public void setRelayAHWNICController(RelayWNICController relayAHWNICController) {this.relayAHWNICController = relayAHWNICController;}
	public void setRelayAPWNICController(RelayWNICController relayAPWNICController) {this.relayAPWNICController = relayAPWNICController;}

	public RelayPositionAPMonitor getRelayPositionAPMonitor() {return relayPositionAPMonitor;}
	public void setRelayPositionAPMonitor(RelayPositionAPMonitor relayPositionAPMonitor) {this.relayPositionAPMonitor = relayPositionAPMonitor;}

	public RelayPositionMonitor getRelayPositionMonitor() {return relayPositionMonitor;}
	public void setRelayPositionMonitor(RelayPositionMonitor relayPositionMonitor) {this.relayPositionMonitor = relayPositionMonitor;}

	public WhoIsRelayServer getWhoIsRelayServer() {return whoIsRelayServer;}
	public void setWhoIsRelayServer(WhoIsRelayServer whoIsRelayServer) {this.whoIsRelayServer = whoIsRelayServer;}

	//Timeout per la ricerca del relay big boss o relay secondario
	public TimeOutSearch getTimeoutSearch() {return timeoutSearch;}
	public void setTimeoutSearch(TimeOutSearch timeoutSearch) {this.timeoutSearch = timeoutSearch;}
	
	//Timeout per rieleggere un nuovo big boss o un relay secondario
	public TimeOutToElect getTimeoutToElect() {return timeoutToElect;}
	public void setTimeoutToElect(TimeOutToElect timeoutToElect) {this.timeoutToElect = timeoutToElect;}
	
	public int getIndexELECTION_RELAY_BEACON() {return indexELECTION_RELAY_BEACON;}
	public void setIndexELECTION_RELAY_BEACON(int indexELECTION_RELAY_BEACON) {this.indexELECTION_RELAY_BEACON = indexELECTION_RELAY_BEACON;}
		
	public Vector<Couple> getPossibleRelay() {return possibleRelay;}
	public void setPossibleRelay(Vector<Couple> possibleRelay) {this.possibleRelay = possibleRelay;}
	
	public double getW() {return W;}
	public void setW(double w) {W = w;}
	public double getMaxW() {return maxW;}
	public void setMaxW(double maxW) {this.maxW = maxW;}
	
	public int getCounter_clients() {return counter_clients;}
	public void setCounter_clients(int counter_clients) {this.counter_clients = counter_clients;}
	
	public int getCounter_relays() {return counter_relays;}
	public void setCounter_relays(int counter_relays) {this.counter_relays = counter_relays;}
	
	public int getActive_relays() {return active_relays;}
	public void setACtive_relays(int active_relays) {this.active_relays = active_relays;}
	
	public static InetAddress getBCAST() {return BCAST;}
	public static void setBCAST(InetAddress bcast) {BCAST = bcast;}
	
	public RelayBatteryMonitor getRelayBatteryMonitor() {return relayBatteryMonitor;}
	public void setRelayBatteryMonitor(RelayBatteryMonitor relayBatteryMonitor) {this.relayBatteryMonitor = relayBatteryMonitor;}

	//Elezione in corso o meno
	public boolean isElecting() {return electing;}
	public void setElecting(boolean electing) {this.electing = electing;}
//
//
//	public boolean isFirstEM_ELECTIONarrived() {
//		return firstEM_ELECTIONarrived;
//	}
//
//
//	public void setFirstEM_ELECTIONarrived(boolean firstEM_ELECTIONarrived) {
//		this.firstEM_ELECTIONarrived = firstEM_ELECTIONarrived;
//	}

//	public TimeOutFailToElect getTimeoutFailToElect() {
//		return timeoutFailToElect;
//	}
//
//
//	public void setTimeoutFailToElect(TimeOutFailToElect timeoutFailToElect) {
//		this.timeoutFailToElect = timeoutFailToElect;
//	}
//
//
//	public TimeOutElectionBeacon getTimeoutElectionBeacon() {
//		return timeoutElectionBeacon;
//	}
//
//
//	public void setTimeoutElectionBeacon(TimeOutElectionBeacon timeoutElectionBeacon) {
//		this.timeoutElectionBeacon = timeoutElectionBeacon;
//	}
//
//
//	public TimeOutClientDetection getTimeoutClientDetection() {
//		return timeoutClientDetection;
//	}
//
//
//	public void setTimeoutClientDetection(
//			TimeOutClientDetection timeoutClientDetection) {
//		this.timeoutClientDetection = timeoutClientDetection;
//	}
//
//
//	public TimeOutEmElection getTimeoutEmElection() {
//		return timeoutEmElection;
//	}
//
//
//	public void setTimeOutEmElection(TimeOutEmElection timeOutEmElection) {
//		this.timeoutEmElection = timeOutEmElection;
//	}


//	public int getIndexELECTION_RESPONSE() {
//		return indexELECTION_RESPONSE;
//	}
//
//
//	public void setIndexELECTION_RESPONSE(int indexELECTION_RESPONSE) {
//		this.indexELECTION_RESPONSE = indexELECTION_RESPONSE;
//	}
//
//
//	public int getIndexREQUEST_SESSION() {
//		return indexREQUEST_SESSION;
//	}
//
//
//	public void setIndexREQUEST_SESSION(int indexREQUEST_SESSION) {
//		this.indexREQUEST_SESSION = indexREQUEST_SESSION;
//	}
//
//
//	public int getIndexEM_EL_DET_RELAY() {
//		return indexEM_EL_DET_RELAY;
//	}
//
//
//	public void setIndexEM_EL_DET_RELAY(int indexEM_EL_DET_RELAY) {
//		this.indexEM_EL_DET_RELAY = indexEM_EL_DET_RELAY;
//	}
//
//
//	public int getIndexEM_ELECTION() {
//		return indexEM_ELECTION;
//	}
//
//
//	public void setIndexEM_ELECTION(int indexEM_ELECTION) {
//		this.indexEM_ELECTION = indexEM_ELECTION;
//	}
//
//
//	public int getIndexELECTION_DONE() {
//		return indexELECTION_DONE;
//	}
//
//
//	public void setIndexELECTION_DONE(int indexELECTION_DONE) {
//		this.indexELECTION_DONE = indexELECTION_DONE;
//	}




//	public boolean isElectionResponseAutoEnable() {
//		return electionResponseAutoEnable;
//	}
//
//
//	public void setElectionResponseAutoEnable(boolean electionResponseAutoEnable) {
//		this.electionResponseAutoEnable = electionResponseAutoEnable;
//	}
//
//
//	public boolean isElectionDoneAutoEnable() {
//		return electionDoneAutoEnable;
//	}
//
//
//	public void setElectionDoneAutoEnable(boolean electionDoneAutoEnable) {
//		this.electionDoneAutoEnable = electionDoneAutoEnable;
//	}
//
//
//	public boolean isEmElectionAutoEnable() {
//		return emElectionAutoEnable;
//	}
//
//
//	public void setEmElectionAutoEnable(boolean emElectionAutoEnable) {
//		this.emElectionAutoEnable = emElectionAutoEnable;
//	}
//
//
//	public boolean isEmElDetRelayAutoEnable() {
//		return emElDetRelayAutoEnable;
//	}
//
//
//	public void setEmElDetRelayAutoEnable(boolean emElDetRelayAutoEnable) {
//		this.emElDetRelayAutoEnable = emElDetRelayAutoEnable;
//	}
//
//
//	public boolean isElectionRequestAutoEnable() {
//		return electionRequestAutoEnable;
//	}
//
//
//	public void setElectionRequestAutoEnable(boolean electionRequestAutoEnable) {
//		this.electionRequestAutoEnable = electionRequestAutoEnable;
//	}
//
//



//	public boolean isFirstELECTION_DONEsent() {
//		return firstELECTION_DONEsent;
//	}
//
//
//	public void setFirstELECTION_DONEsent(boolean firstELECTION_DONEsent) {
//		this.firstELECTION_DONEsent = firstELECTION_DONEsent;
//	}





//	public void setTimeoutEmElection(TimeOutEmElection timeoutEmElection) {
//		this.timeoutEmElection = timeoutEmElection;
//	}

}



class Couple implements Comparable<Couple> {

	private double weight = -1;
	private String address = null;

	public Couple(String address, double weight){
		this.address = address;
		this.weight = weight;		
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Couple c) {
		int res = 0;
		if(this.weight > c.weight)res=-1;
		else if(this.weight < c.weight) res = +1;
		else res = this.address.compareTo(c.address);
		return res;
	}

	public String toString(){
		return "Address: " + this.address 
		+ " Weight: " + this.weight;
	}

	public double getWeight() {
		return weight;
	}

	public String getAddress() {
		return address;
	}

}

