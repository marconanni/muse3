package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import client.ClientMessageFactory;
import client.timeout.ClientTimeoutFactory;
import debug.DebugConsole;

import parameters.Parameters;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.connection.WhoIsRelayServer;
import relay.positioning.RelayPositionAPMonitor;
import relay.positioning.RelayPositionMonitor;
import relay.battery.RelayBatteryMonitor;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutClientDetection;
import relay.timeout.TimeOutElectionBeacon;
import relay.timeout.TimeOutFailToElect;
import relay.timeout.TimeOutSearch;
import relay.timeout.TimeOutEmElection;
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
		IDLE, 
		ACTIVE_NORMAL_ELECTION,
		WAITING_END_NORMAL_ELECTION,
		ACTIVE_EMERGENCY_ELECTION,
		WAITING_END_EMERGENCY_ELECTION,
		MONITORING,
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
	private String actualRelayAddress = null;
	
	//indirizzo attuale del Relay (big boss) a cui è connesso -> solo relay normale
	private String actualConnectedRelayAddress = null;
	
	//indirizzo del Relay in forma InetAddress
	private InetAddress relayInetAddress = null;
	
	//indirizzo del Relay in forma InetAddress
	private InetAddress connectedRelayInetAddress = null;

	//Vector da riempire con le Couple relative agli ELECTION_RESPONSE ricevuti dal Relay uscente
	private Vector<Couple> possibleRelay = null; 
	
	private RelayCM RSSIService;

	//peso del nodo 
	private double W = -1;
	
	//massimo peso rilevato
	private double maxW = -1;

	//boolean che indica se si è in stato di elezione o meno
	//private boolean electing = false;
	
	//boolean che indica se è stato già ricevuto un EM_ELECTION
	//private boolean firstEM_ELECTIONarrived = false;
	
	//boolean che indica se è stato già inviato un ELECTION_DONE
	//private boolean firstELECTION_DONEsent = false;

	//boolean che indica se si è il Relay principale (BIG BOSS collegato al nodo server) o meno
	private boolean imBigBoss = false;
	
	//boolean che indica se si è il Relay secondario o meno
	private boolean imRelay = false;
	
	//numero di clients rilevati al momento a 1 hop da questo nodo
	private int counter_clients = 0;

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
	//private TimeOutFailToElect timeoutFailToElect = null;
	//private TimeOutElectionBeacon timeoutElectionBeacon = null;
	//private TimeOutClientDetection timeoutClientDetection = null;
	//private TimeOutEmElection timeoutEmElection = null;
	//private TimeOutToElect timeoutToElect = null;

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
	private RelayElectionManager(boolean imBigBoss, boolean imRelay, RelaySessionManager sessionManager) throws Exception{

		this.actualStatus = RelayStatus.OFF;
		this.imBigBoss = imBigBoss;
		this.imRelay = imRelay;
		this.addObserver(sessionManager);
		this.console = new DebugConsole();
		this.console.setTitle("RELAY ELECTION MANAGER DEBUG CONSOLE");

		//Ogni relay deve avere due interfaccie WIFI, una per il nodo server e una per la rete ad hoc
		try {
			relayAPWNICController = WNICFinder.getCurrentWNIC(
					Parameters.NAME_OF_MANAGED_RELAY_INTERFACE,
					Parameters.NAME_OF_MANAGED_NETWORK,
					Parameters.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
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
		
		//parto come cliente normale
		else{
			searchingRelay();
		}
	}


	/**Metodo per ottenere l'instanza della classe singleton RelayElectionManager
	 * @param imRelay un boolean che indica se il nodo è il Relay attuale o meno.
	 * @return un riferimento al singleton RelayElectionManager
	 */
	public static RelayElectionManager getInstance(boolean imBigBoss, boolean imRelay, RelaySessionManager sessionManager){
		if(INSTANCE == null)
			try {
				INSTANCE = new RelayElectionManager(imBigBoss, imRelay, sessionManager);
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
		imRelay = true;

		actualRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
		memorizeRelayAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
//		if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
//		if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();
		/*Fine Vedere se sta parte serve*/

		try {
			relayPositionAPMonitor = new RelayPositionAPMonitor(
					relayAPWNICController,	
					Parameters.POSITION_AP_MONITOR_PERIOD,
					this);

			//Compresi client e realy secondari
			relayPositionMonitor = new RelayPositionMonitor(
					relayAHWNICController,
					Parameters.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
					Parameters.POSITION_CLIENTS_MONITOR_PERIOD,
					this);
			
			

			//relayBatteryMonitor = new RelayBatteryMonitor(Parameters.BATTERY_MONITOR_PERIOD,this);

			whoIsRelayServer = new WhoIsRelayServer(imBigBoss,console);

			//relayPositionAPMonitor.start();
			
			//fare partire solo quando qualcuno si collega a lui..
			//relayPositionClientsMonitor.start();
			//relayBatteryMonitor.start();
			whoIsRelayServer.start();
			
			//in teoria solo quando ce traffico...
			relayPositionMonitor.start();
			relayPositionMonitor.startRSSIMonitor();
			
			actualStatus = RelayStatus.MONITORING;
			
			console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): X -> STATO MONITORING: whoIsRelayServer partiti");

		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	/**Metodo che consente di far si che questo nodo diventi il Relay attuale,
	 * memorizzando l'indirizzo locale come indirizzo del Relay, creando e facendo
	 * partire il RelayPositionAPMonitor, il RelayPositionClientsMonitor,
	 * il RelayBatteryMonitor e il WhoIsRelayServer. Poi passa allo stato di MONITORING.
	 */
	
	private void searchingBigBossRelay(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsBigBossRelay(BCAST,Parameters.WHO_IS_RELAY_PORT_IN);
			comManager.sendTo(dpOut);
		} catch (IOException e) {console.debugMessage(Parameters.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this,	Parameters.TIMEOUT_SEARCH);
		console.debugMessage(Parameters.DEBUG_WARNING,"RelayElectionManager: stato OFF, inviato WHO_IS_BIG_BOSS_RELAY e start del TIMEOUT_SEARCH");
	}

	private void becomRelay(){

		imBigBoss = false;
		imRelay = true;

		actualRelayAddress = Parameters.RELAY_AD_HOC_ADDRESS;
		memorizeRelayAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
//		if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
//		if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();
		/*Fine Vedere se sta parte serve*/

		//Compresi client e realy secondari
		relayPositionMonitor = new RelayPositionMonitor(relayAHWNICController,
				Parameters.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
				Parameters.POSITION_CLIENTS_MONITOR_PERIOD,
				this);
		
		//relayPositionController = new RelayPositionController(relayAHWNICController);
		//relayPositionController.setConnectedRelayAddress(actualConnectedRelayAddress);
		
		//Client -> faccio partire nel momento in cui si collega qualche client...
		//relayBatteryMonitor = new RelayBatteryMonitor(Parameters.BATTERY_MONITOR_PERIOD,this);

		whoIsRelayServer = new WhoIsRelayServer(imBigBoss,console);
		
		relayPositionMonitor.start();
		relayPositionMonitor.startRSSIMonitor();

		//relayPositionController.start();
		//relayBatteryMonitor.start();
		whoIsRelayServer.start();

		actualStatus = RelayStatus.MONITORING;

		System.out.println("RelayElectionManager.becomeRelay(): X -> STATO MONITORING: " +
		"Monitors e whoIsRelayServer partiti");
	}



	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public synchronized void update(Observable arg0, Object arg1) {
		System.out.println("Arrivato un nuovo messaggio");

		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){

			DatagramPacket dpIn = (DatagramPacket)arg1;
			relayMessageReader = new RelayMessageReader();

			try {
				relayMessageReader.readContent(dpIn);
			} catch (IOException e) {e.printStackTrace();}

			if(relayMessageReader.getCode() == Parameters.IM_BIGBOSS && actualStatus == RelayStatus.OFF){

				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager: STATO IDLE: IM_BIG_BOSS_RELAY arrivato");

				actualConnectedRelayAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedRelayAddress();

				setChanged();
				notifyObservers("RELAY_FOUND:"+relayMessageReader.getActualConnectedRelayAddress());
								
				console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager: STATO IDLE: IM_BIG_BOSS_RELAY arrivatoa actualConnectRelayAddress: "+actualConnectedRelayAddress);
				becomRelay();
			}
		}
		if(arg1 instanceof String){

			String event = (String) arg1;
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.OFF){
				console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				if(imRelay)
					searchingBigBossRelay();
				else
					searchingRelay();
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


			/*[ELECTION_REQUEST arrivato && ap Visibility == false] / 
			 *electing = true
			 */

			/*[ELECTION_REQUEST arrivato && ap Visibility == true] / 
			 *electing = true
			 *counter_clients = 0
			 *status = ActiveNormalElection
			 *start TIMEOUT_FAIL_TO_ELECT
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_REQUEST && 
					actualStatus == RelayStatus.IDLE 
					//Parte che evita la ricezione dei propri messaggi a livello alto 		
					&& (!dpIn.getAddress().getHostAddress().equals(Parameters.RELAY_AD_HOC_ADDRESS) || electionRequestAutoEnable)
			){

				if(timeoutSearch != null){
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				try {
					debugPrint("RelayElectionManager: STATO IDLE:  ELECTION_REQUEST arrivato && ap Visibility == "+ 
							relayWNICController.isConnected());
				} catch (WNICException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//Questa parte c'è nel caso il nodo ex relay non si disconnetta ma rientri nella Dense Manet
				//Solo quando riceve una richiesta di elezione può liberarsi delle vecchie informazioni sulla 
				//precedente elezione

				firstELECTION_DONEsent = false;

				if(possibleRelay != null){
					possibleRelay.clear();
					possibleRelay = null;
				}

				actualRelayAddress = null;
				electing = true;
				counter_clients = 0;
				W = -1;

				try {

					//ap Visibility == true
					if(relayWNICController.isConnected()){

						timeoutElectionBeacon = RelayTimeoutFactory.getTimeOutElectionBeacon(this, Parameters.TIMEOUT_ELECTION_BEACON);
						timeoutFailToElect = RelayTimeoutFactory.getTimeOutFailToElect(this, Parameters.TIMEOUT_FAIL_TO_ELECT);

						actualStatus  = RelayStatus.ACTIVE_NORMAL_ELECTION;
						debugPrint("RelayElectionManager: STATO IDLE -> ACTIVE_NORMAL_ELECTION:  " +
								"ELECTION_REQUEST arrivato && ap Visibility == true " +
						"e TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti");
					} 			

				} catch (WNICException e) {
					e.printStackTrace();
				}
				
				setChanged();
				notifyObservers("ELECTION_REQUEST_RECEIVED");

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

			/*[ELECTION_BEACON arrivato] / 
			 *counter_clients++ 
			 */
			/*else if(relayMessageReader.getCode() == Parameters.ELECTION_BEACON && 
					actualStatus == RelayStatus.ACTIVE_NORMAL_ELECTION){

				counter_clients++;
				debugPrint("RelayElectionManager: STATO ACTIVE_NORMAL_ELECTION: ELECTION_BEACON arrivato " +
						"(counter_clients:"+counter_clients+")");
			}
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



			/**INIZIO STATO MONITORING**/

			/*[DISCONNECTION_WARNING sollevato]
			 * imRelay = true
			 * status = monitoring
			 */
			/*else if(event.equals("DISCONNECTION_WARNING") &&
					actualStatus == RelayStatus.MONITORING){

				debugPrint("RelayElectionManager: STATO MONITORING: DISCONNECTION_WARNING sollevato " +
				"da uno dei Monitor");

				imRelay = false;

				actualRelayAddress = null;
				relayInetAddress = null;
				W = -1;
				maxW = -1;

				whoIsRelayServer.close();
				relayPositionAPMonitor.close();
				relayPositionClientsMonitor.close();
				relayBatteryMonitor.close();

				debugPrint("RelayElectionManager: STATO MONITORING: chiusura dei Monitor e del whoIsRelayServer");

				possibleRelay = new Vector<Couple>();

				DatagramPacket dpOut = null;

				try {
					//invio ai Relay
					dpOut = RelayMessageFactory.buildElectionRequest(BCAST, Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					dpOut = RelayMessageFactory.buildElectionRequest(BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
					comManager.sendTo(dpOut);
				} catch (IOException e) {
					e.printStackTrace();
				}

				timeoutToElect = RelayTimeoutFactory.getTimeOutToElect(this, Parameters.TIMEOUT_TO_ELECT);

				electing = true;

				actualStatus = RelayStatus.WAITING_RESPONSE;

				debugPrint("RelayElectionManager: STATO MONITORING -> WAITING_RESPONSE: " +
						"DISCONNECTION_WARNING sollevato da uno dei Monitor, ELECTION_REQUEST inviato, " +
				"TIMEOUT_TO_ELECT partito ");
			}
			/**FINE STATO MONITORING**/



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

		/*[TIMEOUT_SEARCH scattato] oppure [if imRelay == false]/
		 * relayAddress = null 
		 */
		actualRelayAddress = null;

		//se non sono in elezione 

			DatagramPacket dpOut = null;

			try {
				dpOut = RelayMessageFactory.buildWhoIsRelay(BCAST, Parameters.WHO_IS_RELAY_PORT_IN);
				comManager.sendTo(dpOut);
			} catch (IOException e) {
				e.printStackTrace();
			}

			/*[WHO_IS_RELAY inviato] /
			 * start TIMEOUT_SEARCH
			 * starus = Idle 
			 */

			timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this, Parameters.TIMEOUT_SEARCH);
			actualStatus = RelayStatus.IDLE;

			System.out.println("RelayElectionManager.searchingRelay(): STATO INSTABILE SEARCHING_RELAY -> IDLE: " +
			"WHO_IS_RELAY inviato e TIMEOUT_SEARCH partito");
	}


	


	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeRelayAddress(){

		try {
			relayInetAddress = InetAddress.getByName(actualRelayAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeConnectedRelayAddress(){

		try {
			connectedRelayInetAddress = InetAddress.getByName(actualConnectedRelayAddress);
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
	public String getActualRelayAddress() {return actualRelayAddress;}
	public String getActualConnectedRelayAddress() {return actualConnectedRelayAddress;}
	public void setActualRelayAddress(String actualRelayAddress) {this.actualRelayAddress = actualRelayAddress;}
	public void setActualConnectedRelayAddress(String actualConnectedRelayAddress) {this.actualConnectedRelayAddress = actualConnectedRelayAddress;}
	public InetAddress getRelayInetAddress() {return relayInetAddress;}
	public InetAddress getConnectedRelayInetAddress() {return connectedRelayInetAddress;}
	public void setRelayInetAddress(InetAddress relayInetAddress) {this.relayInetAddress = relayInetAddress;}
	public void setConnectedRelayInetAddress(InetAddress connectedRelayInetAddress) {this.connectedRelayInetAddress = connectedRelayInetAddress;}

	public boolean isImRelay() {return imRelay;}
	public boolean isImBigBossRelay(){return imBigBoss;}
	public void setImRelay(boolean imRelay) {this.imRelay = imRelay;}
	public void setImBigBossRelay(boolean imBigBoss) {this.imBigBoss = imBigBoss;}
	
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

	public TimeOutSearch getTimeoutSearch() {return timeoutSearch;}
	public void setTimeoutSearch(TimeOutSearch timeoutSearch) {this.timeoutSearch = timeoutSearch;}
	
	
	public Vector<Couple> getPossibleRelay() {
		return possibleRelay;
	}


	public void setPossibleRelay(Vector<Couple> possibleRelay) {
		this.possibleRelay = possibleRelay;
	}


	public double getW() {
		return W;
	}


	public void setW(double w) {
		W = w;
	}


	public double getMaxW() {
		return maxW;
	}


	public void setMaxW(double maxW) {
		this.maxW = maxW;
	}


//	public boolean isElecting() {
//		return electing;
//	}
//
//
//	public void setElecting(boolean electing) {
//		this.electing = electing;
//	}
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





	public int getCounter_clients() {
		return counter_clients;
	}


	public void setCounter_clients(int counter_clients) {
		this.counter_clients = counter_clients;
	}





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


	public static InetAddress getBCAST() {
		return BCAST;
	}


	public static void setBCAST(InetAddress bcast) {
		BCAST = bcast;
	}


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
//	public TimeOutToElect getTimeoutToElect() {
//		return timeoutToElect;
//	}
//
//
//	public void setTimeoutToElect(TimeOutToElect timeoutToElect) {
//		this.timeoutToElect = timeoutToElect;
//	}


//	public boolean isFirstELECTION_DONEsent() {
//		return firstELECTION_DONEsent;
//	}
//
//
//	public void setFirstELECTION_DONEsent(boolean firstELECTION_DONEsent) {
//		this.firstELECTION_DONEsent = firstELECTION_DONEsent;
//	}


	public RelayBatteryMonitor getRelayBatteryMonitor() {
		return relayBatteryMonitor;
	}


	public void setRelayBatteryMonitor(RelayBatteryMonitor relayBatteryMonitor) {
		this.relayBatteryMonitor = relayBatteryMonitor;
	}


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

class TesterRelayElectionManager{


	public static void stamp(RelayElectionManager rem, int i){
		try {
			System.out.println("\n*********************("+i+") "+rem.getActualStatus().toString().toUpperCase()+"********************");
			System.out.println("Status: " + rem.getActualStatus());
			System.out.println("actualRelayAddress: " + rem.getActualRelayAddress());
			System.out.println("relayInetAddress: " + ((rem.getRelayInetAddress()==null)?"null":rem.getRelayInetAddress().getHostAddress()));
			System.out.println("imRelay: " + rem.isImRelay());
			System.out.println("W: " + rem.getW());
			System.out.println("maxW: " + rem.getMaxW());
			System.out.println("counter_clients: " + rem.getCounter_clients());
			//System.out.println("electing: " + rem.isElecting());
			//System.out.println("first ELECTION_DONE sent: " + rem.isFirstELECTION_DONEsent());
			//System.out.println("first EM_ELECTION arrived: " + rem.isFirstEM_ELECTIONarrived());
			System.out.println("apVisibility: " + rem.getRelayAPWNICController().isConnected());
			System.out.println("aHVisibility: " + rem.getRelayAHWNICController().isConnected());
			System.out.println("********************************************************************************");

		} catch (WNICException e1) {
			e1.printStackTrace();
		}
	}


	public static void main(String args[]){

		DatagramSocket dsNet = null;

		try {
			dsNet = new DatagramSocket(9999);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		RelaySessionManager session = RelaySessionManager.getInstance();
		RelayElectionManager rem = RelayElectionManager.getInstance(true,true, session);

		//STAMPA VERIFICA 1
		//stamp(rem, 1);


		//STATO IDLE && IM_RELAY ARRIVATO																	OK			
		/* Status = IDLE
		 * actualRelayAddress = 192.168.0.123
		 * relayInetAddress = 192.168.0.123
		 * imRelay = false
		 * W = -1
		 * maxW = -1
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*try {

			DatagramPacket dpOut = RelayMessageFactory.buildImRelay("192.168.0.123", rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA VERIFICA 2  
		stamp(rem, 2);
		 */

		//STATO IDLE && ELECTION_REQUEST ARRIVATO && apVisibility == false									OK
		/* Status = IDLE
		 * actualRelayAddress = null
		 * relayInetAddress = 192.168.0.123 (non si sa mai)
		 * imRelay = false
		 * W = -1
		 * maxW = -1
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = false
		 */

		//per abilitare la ricezione di ElectionRequest provenienti da localhost
//		rem.setElectionRequestAutoEnable(true); 
//
//		//per mettere apVisibility = false
//		((RelayDummyController)rem.getRelayWNICController()).setApVisibility(false); 
//
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(500);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		//STAMPA VERIFICA 3  
//		stamp(rem, 3);
//
//
//
//		//STATO IDLE && ELECTION_DONE ARRIVATO && apVisibility == false									OK
//		/* Status = IDLE
//		 * actualRelayAddress = 192.168.0.11
//		 * relayInetAddress = 192.168.0.11
//		 * imRelay = false
//		 * W = -1
//		 * maxW = -1
//		 * counter_clients = 0
//		 * electing = false
//		 * first ELECTION_DONE sent = false
//		 * first EM_ELECTION arrived = false
//		 * apVisibility = false
//		 */
//		/*
//		//per abilitare la ricezione di ElectionRequest provenienti da localhost
//		rem.setElectionDoneAutoEnable(true); 
//
//		//per mettere apVisibility = false
//		((RelayDummyController)rem.getRelayWNICController()).setApVisibility(false); 
//
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildElectionDone(0,"192.168.0.11", rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(500);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		//STAMPA VERIFICA 3bis  
//		stamp(rem, 3);
//		 */
//
//
//		//STATO IDLE && EM_ELECTION ARRIVATO && apVisibility == false										OK
//		/* Status = WAITING_END_EMERGENCY_ELECTION
//		 * actualRelayAddress = 192.168.0.7 (ci sta solo questo)
//		 * relayInetAddress = 192.168.0.7
//		 * imRelay = false
//		 * W = -1
//		 * maxW = 24
//		 * counter_clients = 0
//		 * electing = true
//		 * first ELECTION_DONE sent = false
//		 * first EM_ELECTION arrived = true
//		 * apVisibility = false
//		 */
//
//		//per abilitare la ricezione di ElectionRequest provenienti da localhost
//		rem.setEmElectionAutoEnable(true); 
//
//		//per mettere apVisibility = false
//		((RelayDummyController)rem.getRelayWNICController()).setApVisibility(false); 
//
//		try{
//			DatagramPacket dpOut = RelayMessageFactory.buildEmElection(3, 23, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			dpOut = RelayMessageFactory.buildEmElection(4, 24, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			dpOut = RelayMessageFactory.buildEmElection(5, 22, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(500);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		//interrompo il TIMEOUT_EM_ELECTION
//		rem.getTimeoutEmElection().cancelTimeOutEmElection();
//
//		//STAMPA VERIFICA 3tris  
//		stamp(rem, 3);



		//STATO IDLE && ELECTION_REQUEST ARRIVATO && apVisibility == true									OK
		/* Status = ACTIVE_NORMAL_ELECTION
		 * actualRelayAddress = null
		 * relayInetAddress = 192.168.0.123 (per avere cmq l'indirizzo a cui spedire l'ELECTION_RESPONSE)
		 * imRelay = false
		 * W = -1
		 * maxW = -1
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di ElectionRequest provenienti da localhost
		rem.setElectionRequestAutoEnable(true); 

		//apVisibility = true a default

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//interrompo i timeout che han fatto start all'arrivo dell'ELECTION_REQUEST
		rem.getTimeoutElectionBeacon().cancelTimeOutElectionBeacon();
		rem.getTimeoutFailToElect().cancelTimeOutFailToElect();

		//STAMPA VERIFICA 4  
		stamp(rem, 4);*/



		//STATO ACTIVE_NORMAL_ELECTION && ELECTION_BEACONT ARRIVATO (X 3)									OK
		/* Status = ACTIVE_NORMAL_ELECTION
		 * actualRelayAddress = null
		 * relayInetAddress = 192.168.0.123 (per avere cmq l'indirizzo a cui spedire l'ELECTION_RESPONSE)
		 * imRelay = false
		 * W = -1
		 * maxW = -1
		 * counter_clients = 3
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di ElectionRequest provenienti da localhost
		rem.setElectionRequestAutoEnable(true); 

		//apVisibility = true a default

		try {

			DatagramPacket dpOut = ClientMessageFactory.buildElectioBeacon(1, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = ClientMessageFactory.buildElectioBeacon(2, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = ClientMessageFactory.buildElectioBeacon(3, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA VERIFICA 5
		stamp(rem, 5);*/



		//STATO ACTIVE_NORMAL_ELECTION && TIMEOUT_ELECTION_BEACON											OK
		/* Status = WAITING_END_NORMAL_ELECTION
		 * actualRelayAddress = null
		 * relayInetAddress = 192.168.0.123 (per avere cmq l'indirizzo a cui spedire l'ELECTION_RESPONSE)
		 * imRelay = false
		 * W = 20.1 (il peso calcolato durante lo stato instabile Weight_Calculation)
		 * maxW = -1
		 * counter_clients = 3
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//faccio scattare a mano il TIMEOUT_ELECTION_BEACON
		rem.update(null, "TIMEOUTELECTIONBEACON");

		//STAMPA VERIFICA 6
		stamp(rem, 6);*/



		//STATO WAITING_END_NORMAL_ELECTION && ELECTION_DONE ARRIVATO && IP RICEVUTO != localhost 			OK
		/* Status = IDLE
		 * actualRelayAddress = 192.168.0.3
		 * relayInetAddress = 192.168.0.3 
		 * imRelay = false
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di ElectionDone provenienti da localhost
		rem.setElectionDoneAutoEnable(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionDone(3,"192.168.0.3", rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA VERIFICA 7
		stamp(rem, 7);

		 */


		//STATO WAITING_END_NORMAL_ELECTION && ELECTION_DONE ARRIVATO && IP RICEVUTO == localhost 			OK
		/* Status = MONITORING
		 * actualRelayAddress = 192.168.0.7
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = true
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di ElectionDone provenienti da localhost
		rem.setElectionDoneAutoEnable(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionDone(3,"192.168.0.7", rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//fermo il RelayPositionClientsMonitor
		rem.getRelayPositionClientsMonitor().close();

		//fermo il RelayPositionAPMonitor
		rem.getRelayPositionAPMonitor().close();

		//fermo il RelayPositionAPMonitor
		rem.getWhoIsRelayServer().close();

		//STAMPA VERIFICA 8
		stamp(rem, 8);

		 */



		//STATO WAITING_END_NORMAL_ELECTION && TIMEOUT_FAIL_TO_ELECT SCATTATO									OK
		/* Status = ACTIVE_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè in emergenza considero me stesso come relay attuale all'inizio)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*		
		//faccio scattare il TIMEOUT_FAIL_TO_ELECT 
		rem.update(null,"TIMEOUTFAILTOELECT");

		//fermo il TIMEOUT_CLIENT_DETECTION
		rem.getTimeoutClientDetection().cancelTimeOutClientDetection();

		//STAMPA VERIFICA 9
		stamp(rem, 9);

		 */



		//STATO WAITING_END_NORMAL_ELECTION && EM_EL_DET_RELAY ARRIVATO											OK
		/* Status = ACTIVE_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè in emergenza considero me stesso come relay attuale all'inizio)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di EmElDetRelay provenienti da localhost
		rem.setEmElDetRelayAutoEnable(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(7, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//fermo il TIMEOUT_CLIENT_DETECTION
		rem.getTimeoutClientDetection().cancelTimeOutClientDetection();

		//STAMPA VERIFICA 10
		stamp(rem, 10);

		 */



		//STATO WAITING_END_NORMAL_ELECTION && EM_EL_DET_CLIENT ARRIVATO											OK
		/* Status = ACTIVE_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè in emergenza considero me stesso come relay attuale all'inizio)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 1
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di EmElDetRelay provenienti da localhost
		rem.setEmElDetRelayAutoEnable(true);

		try {

			DatagramPacket dpOut = ClientMessageFactory.buildEmElDetClient(7, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//fermo il TIMEOUT_CLIENT_DETECTION
		rem.getTimeoutClientDetection().cancelTimeOutClientDetection();

		//STAMPA VERIFICA 11
		stamp(rem, 11);*/



		//STATO ACTIVE_EMERGENCY_ELECTION && EM_EL_DET_CLIENT ARRIVATO												OK
		/* Status = ACTIVE_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè in emergenza considero me stesso come relay attuale all'inizio)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1
		 * maxW = -1 
		 * counter_clients = 2
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		try {

			DatagramPacket dpOut = ClientMessageFactory.buildEmElDetClient(7, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA VERIFICA 12
		stamp(rem, 12);*/


		//STATO ACTIVE_EMERGENCY_ELECTION && EM_ELECTION ARRIVATO	(X3)										OK
		/* Status = ACTIVE_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè in emergenza considero me stesso come relay attuale all'inizio)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1
		 * maxW = 30.0
		 * counter_clients = 2
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = true
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di EmElection provenienti da localhost
		rem.setEmElectionAutoEnable(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildEmElection(7, 23, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = RelayMessageFactory.buildEmElection(8, 17, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = RelayMessageFactory.buildEmElection(9, 30, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA VERIFICA 13
		stamp(rem, 13);

		 */


		//STATO ACTIVE_EMERGENCY_ELECTION && TIMEOUT_CLIENT_DETECTION SCATTATO									OK
		/* Status = WAITING_END_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè gli EM_ELECTION danno informazione sul loro mittente direttamente dal Datagramma)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = <quello calcolato localmente>
		 * maxW = 30.0
		 * counter_clients = 2
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = true
		 * apVisibility = true
		 */
		/*
		//faccio scattare il TIMEOUT_CLIENT_DETECTION 
		rem.update(null,"TIMEOUTCLIENTDETECTION");

		//fermo il TIMEOUT_EM_ELECTION
		rem.getTimeoutEmElection().cancelTimeOutEmElection();


		//STAMPA VERIFICA 14
		stamp(rem, 14);
		 */

		//STATO ACTIVE_EMERGENCY_ELECTION && TIMEOUT_CLIENT_DETECTION SCATTATO									OK
		/* Status = WAITING_END_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè gli EM_ELECTION danno informazione sul loro mittente direttamente dal Datagramma)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = <quello calcolato localmente>
		 * maxW = 30.0
		 * counter_clients = 2
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = true
		 * apVisibility = true
		 */
		/*	
		//faccio si che il Relay non veda più l'AP
		((RelayDummyController)rem.getRelayWNICController()).setApVisibility(false);

		//faccio scattare il TIMEOUT_CLIENT_DETECTION 
		rem.update(null,"TIMEOUTCLIENTDETECTION");

		//fermo il TIMEOUT_EM_ELECTION
		rem.getTimeoutEmElection().cancelTimeOutEmElection();


		//STAMPA VERIFICA 14bis
		stamp(rem, 14);

		 */

		//STATO WAITING_END_EMERGENCY_ELECTION && EM_ELECTION ARRIVATO									OK
		/* Status = WAITING_END_EMERGENCY_ELECTION
		 * actualRelayAddress = 192.168.0.7 (perchè gli EM_ELECTION danno informazione sul loro mittente direttamente dal Datagramma)
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = <quello di prima> (-1.0 nel caso di IDLE -> WAITING_END_EMERGENCY_ELECTION && apVisibility == false [STAMPA 3tris] )
		 * maxW = 33.0
		 * counter_clients = 2 (0 nel caso di IDLE -> WAITING_END_EMERGENCY_ELECTION && apVisibility == false [STAMPA 3tris] )
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = true
		 * apVisibility = true (false nel caso di IDLE -> WAITING_END_EMERGENCY_ELECTION && apVisibility == false [STAMPA 3tris] )
		 */
//
//		try{
//			DatagramPacket dpOut = RelayMessageFactory.buildEmElection(9, 33, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(500);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		//STAMPA VERIFICA 15
//		stamp(rem, 15);



		//STATO WAITING_END_EMERGENCY_ELECTION && TIMEOUT_EM_ELECTION SCATTATO &&							OK
		//&& actualRelayAddress == localhost	
		/* Status = MONITORING
		 * actualRelayAddress = 192.168.0.7 
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = true
		 * W = -1.0
		 * maxW = -1.0
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*		
		//faccio scattare il TIMEOUT_EM_ELECTION 
		rem.update(null,"TIMEOUTEMELECTION");

		//spengo il RelayPositionClientMonitor
		//rem.getRelayPositionClientsMonitor().close();

		//spengo il RelayPositionAPMonitor
		//rem.getRelayPositionAPMonitor().close();

		//spengo il WhoIsRelayServer
		//rem.getWhoIsRelayServer().close();


		//STAMPA VERIFICA 16
		stamp(rem, 16);*/



		//STATO WAITING_END_EMERGENCY_ELECTION && TIMEOUT_EM_ELECTION SCATTATO &&							OK
		//&& actualRelayAddress != localhost	
		/* Status = IDLE
		 * actualRelayAddress = 192.168.0.11 
		 * relayInetAddress = 192.168.0.11
		 * imRelay = false
		 * W = -1.0
		 * maxW = -1.0
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true (false nel caso di IDLE -> WAITING_END_EMERGENCY_ELECTION && apVisibility == false [STAMPA 3tris] )
		 */

		//mi tocca settare l'indirizzo del relay a mano 
//		rem.setActualRelayAddress("192.168.0.11");
//		try {
//			rem.setRelayInetAddress(InetAddress.getByName("192.168.0.11"));
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		//faccio scattare il TIMEOUT_EM_ELECTION 
//		rem.update(null,"TIMEOUTEMELECTION");
//
//
//		//STAMPA VERIFICA 17
//		stamp(rem, 17);





		//STATO MONITORING && DISCONNECTION_WARNING SOLLEVATO 												OK	
		/* Status = WAITING_RESPONSE
		 * actualRelayAddress = null 
		 * relayInetAddress = null 
		 * imRelay = false
		 * W = -1.0
		 * maxW = -1.0
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*	
		//sollevo un DISCONNECTION_WARNING 
		rem.update(null,"DISCONNECTION_WARNING");

		//fermo il TIMEOUT_TO_ELECT
		rem.getTimeoutToElect().cancelTimeOutToElect(); 

		//STAMPA VERIFICA 18
		stamp(rem, 18);*/


		//STATO WAITING_RESPONSE && ELECTION_RESPONSE ARRIVATO (X3)											OK	
		/* Status = WAITING_RESPONSE
		 * actualRelayAddress = 192.168.0.7 (purtroppo c'è solo localhost) 
		 * relayInetAddress = 192.168.0.7 (me lo memorizzo cmq,, può essere utile nel caso non ci sia disconnessione)
		 * imRelay = false
		 * W = -1.0
		 * maxW = 24.0
		 * counter_clients = 0
		 * electing = true
		 * first ELECTION_DONE sent = false
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */
		/*
		//per abilitare la ricezione di ElectionResponse provenienti da localhost
		rem.setElectionResponseAutoEnable(true);

		try{
			DatagramPacket dpOut = RelayMessageFactory.buildElectionResponse(13, 23, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = RelayMessageFactory.buildElectionResponse(14, 24, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			dpOut = RelayMessageFactory.buildElectionResponse(15, 22, rem.getBCAST(), Parameters.RELAY_ELECTION_PORT_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		//STAMPA VERIFICA 19
		stamp(rem, 19);
		 */


		//STATO WAITING_RESPONSE && TIMEOUT_TO_ELECT SCATTATO && possibleRelay.size() > 0   				   OK	
		/* Status = IDLE
		 * actualRelayAddress = 192.168.0.7  
		 * relayInetAddress = 192.168.0.7 
		 * imRelay = false
		 * W = -1.0
		 * maxW = -1.0
		 * counter_clients = 0
		 * electing = false
		 * first ELECTION_DONE sent = true
		 * first EM_ELECTION arrived = false
		 * apVisibility = true
		 */

		//

		/*	//faccio scattare il TIMEOUT_TO_ELECT
		rem.update(null,"TIMEOUTTOELECT");


		//STAMPA VERIFICA 20
		stamp(rem, 20);*/

		/*//per abilitare la ricezione di ElectionDone provenienti da localhost
		rem.setElectionDoneAutoEnable(true);

		rem.chooseAnotherRelay();

		//STAMPA VERIFICA 21
		stamp(rem, 21);*/


		/*
		//dovrebbe andare
		rem.chooseAnotherRelay();

		//non dovrebbe andare
		rem.chooseAnotherRelay();

		//non dovrebbe andare
		rem.chooseAnotherRelay();*/

		//rem.getComManager().close();

	}
}