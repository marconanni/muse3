package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;
import parameters.TimeOutConfiguration;

import relay.battery.RelayBatteryMonitor;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.connection.WhoIsRelayServer;

import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.position.RelayPositionAPMonitor;
import relay.position.RelayPositionMonitor;
import relay.position.RelayPositionMonitorController;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutSearch;
import relay.wnic.RelayWNICController;
import relay.wnic.WNICFinder;
import relay.wnic.exception.WNICException;
import debug.DebugConsole;


public class RelayElectionManager extends Observable implements Observer{
	
	//Le varie console dei messaggi
	private DebugConsole consoleElectionManager = null;					//Debug console del RelayElectionManager
	private DebugConsole consoleClusterWifiInterface = null;			//Debug console della scheda Wifi collegata al Cluster locale
	private DebugConsole consoleClusterHeadWifiInterface = null;		//Debug console della scheda Wifi collegata al Cluster Head
	
	private RelayStatus actualStatus = null;				//stato attuale del RelayElectionManager
	
	private String localClusterAddress = null;				//indirizzo del Relay attuale in forma String
	private String localClusterHeadAddress = null;
	private String connectedClusterHeadAddress = null;		//indirizzo attuale del Relay (big boss) a cui è connesso -> solo relay normale
	private InetAddress localClusterInetAddress = null;		//indirizzo del Relay in forma InetAddress
	private InetAddress localClusterHeadInetAddress = null;			//indirizzo del Server in forma InetAddress
	private InetAddress connectedClusterHeadInetAddress = null;	//indirizzo del Relay in forma InetAddress

	
	private static RelayElectionManager INSTANCE = null;	//istanza singleton del RelayElectionManager
	private static boolean BIGBOSS = false;					//boolean che indica che si è il BIGBOSS (connesso al server)
	private static boolean RELAY = false;					//boolean che indica che si è un RELAY attivo (connesso al BIGBOSS)
	private static boolean POSSIBLE_BIGBOSS = false;		//boolean che indica che si è un possibile sostituto del BIGBOSS (BIGBOSS passivo)
	private static boolean POSSIBLE_RELAY = false;			//boolean che indica che si è un possibile sostituto di un RELAY attivo (RELAY passivo)
	private static boolean CLIENT = false;					//boolean che indica che si è semplicemente un client
	private static InetAddress BCAST = null;				//indirizzo broadcast del cluster locale in forma InetAddress
	private static InetAddress BCASTHEAD = null;			//indirizzo broadcast del cluster head in forma InetAddress
	
	private RelayCM comClusterManager = null;				//il manager per le comunicazioni nel proprio cluster
	private RelayCM comClusterHeadManager = null;			//il manager per le comunicazioni col cluster head	
	
	private RelayMessageReader relayMessageReader = null;				//il RelayMessageReader per leggere il contenuto dei messaggi ricevuti
	private RelayWNICController relayClusterWNICController = null;		//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster)
	private RelayWNICController relayClusterHeadWNICController = null;	//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster head)
	private RelayPositionAPMonitor relayPositionAPMonitor = null;		//il RelayPositionAPMonitor per conoscere la propria posizione nei confronti dell'AP
	private RelayPositionMonitor relayPositionMonitor = null;			//il RelayPositionMonitor per conoscere la propria posizione nei confronti dei client
	private RelayPositionMonitorController relayPositionMonitorController = null;
	private RelayBatteryMonitor relayBatteryMonitor = null;				//il RelayBatteryMonitor per conoscere la situazione della propria batteria
	private WhoIsRelayServer whoIsRelayServer = null;					//thread che risponde ai messaggi di WHO_IS_RELAY con IM_RELAY
	
	private int active_relays = 0;								//relay secondari collegati al bigboss (solo big boss)
	private int active_client = 0;								//client collegati al relay - aumenta solo, non si aggiorna
	
	//vari Timeout necessari al RelayElectionManager
	private TimeOutSearch timeoutSearch = null;
//	private TimeOutToElect timeoutToElect = null;
//	private TimeOutFailToElect timeoutFailToElect = null;
//	private TimeOutElectionBeacon timeoutElectionBeacon = null;
	//private TimeOutClientDetection timeoutClientDetection = null;
	//private TimeOutEmElection timeoutEmElection = null;
	
	public enum RelayStatus {					//stati in cui si può trovare il RelayElectionManager
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

	static { 
		try {
			BCAST  = InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_BROADCAST_ADDRESS);
			BCASTHEAD  = InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_HEAD_BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {e.printStackTrace();}
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

	/**
	 * Metodo per definire il tipo di nodo
	 * @param type true -> BIGBOSS , false ->RELAY
	 * @param state true -> ACTIVE , false ->POSSIBLE
	 */
	private void setNodeType(boolean type, boolean state){
		setBIGBOSS(false);
		setRELAY(false);
		setPOSSIBLE_BIGBOSS(false);
		setPOSSIBLE_RELAY(false);
		setCLIENT(false);
		if(type && state)setBIGBOSS(true);
		else if(type && !state) setPOSSIBLE_BIGBOSS(true);
		else if(!type && state) setRELAY(true);
		else if(!type && !state) setPOSSIBLE_RELAY(true);
		else setCLIENT(true);
	}

	/**Costruttore per ottenere un RelayElectionManager. 
	 * Fa partire due connection Manager a seconda se imRelay o imBigBoss sia true
	 * 
	 * CASO IMBIGBOSS : comClusterManager --> connection Manager all'interno del cluster
	 * 
	 * CASO IMRELAY:	comClusterManager --> connection Manager all'interno del cluster
	 * 					comClusterHeadManager --> connection Manager con il relay BigBoss
	 * 
	 * ALtrimenti Relay Passivo.
	 * @param 
	 * @throws Exception
	 * 
	 */
	private RelayElectionManager(boolean type, boolean state, RelaySessionManager sessionManager) throws Exception{

		this.actualStatus = RelayStatus.OFF;
		setNodeType(type, state);
		this.addObserver((Observer) sessionManager);
		this.consoleElectionManager = new DebugConsole();
		this.consoleElectionManager.setTitle("RELAY ELECTION MANAGER");
		
		//Controllo delle interfacce WIFI
		try {
			//caso in cui sono nel cluster head (rete ad hoc principale)
			//devo avere il controllo della scheda di rete interfacciata alla rete Managed nei confronti del server
			//nodi bigboss o possibili nodi sostituti
			if((isBIGBOSS())||isPOSSIBLE_BIGBOSS()){
				
				relayClusterHeadWNICController = WNICFinder.getCurrentWNIC(
						NetConfiguration.NAME_OF_RELAY_MANAGED_WIFI_INTERFACE,
						NetConfiguration.NAME_OF_RELAY_MANAGED_NETWORK,
						0);
				this.consoleClusterHeadWifiInterface = new DebugConsole();
				this.consoleClusterHeadWifiInterface.setTitle("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_MANAGED_WIFI_INTERFACE);
				relayClusterHeadWNICController.setDebugConsole(this.consoleClusterHeadWifiInterface);
				relayClusterHeadWNICController.init();
			}
			
			//se invece in una delle reti cluster secondarie 
			//devo avere il controllo della scheda di rete interfacciata alla rete cluster head
			//per comunicare col BIGBOSS
			else{
				relayClusterHeadWNICController = WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_NETWORK,
					1);
				this.consoleClusterHeadWifiInterface = new DebugConsole();
				this.consoleClusterHeadWifiInterface.setTitle("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE);
				relayClusterHeadWNICController.setDebugConsole(this.consoleClusterHeadWifiInterface);
				relayClusterHeadWNICController.init();
			}
			
			relayClusterWNICController = WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_RELAY_CLUSTER_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_RELAY_CLUSTER_NETWORK,
					1);
			this.consoleClusterWifiInterface = new DebugConsole();
			this.consoleClusterWifiInterface.setTitle("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_CLUSTER_WIFI_INTERFACE);
			relayClusterWNICController.setDebugConsole(this.consoleClusterWifiInterface);
			relayClusterWNICController.init();
			
		} catch (WNICException e) {System.err.println("ERRORE:"+e.getMessage());System.exit(1);}
		
		comClusterManager = RelayConnectionFactory.getClusterElectionConnectionManager(this,true);
		comClusterManager.start();
		
		//Il big boss non comunica col server e quindi non serve un connection Manager col server
		//Ogni relay di ogni cluster comunica col big boss, ovvero il relay del cluster head
		if(isRELAY() || isPOSSIBLE_RELAY()){
			comClusterHeadManager = RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true);
			comClusterHeadManager.start();
		}

		//Se parto come Relay BIG BOSS
		//WIFI Managed deve essere associata al AP e quindi connessa
		//WIFI Ad-Hoc deve solo essere configurata correttamente ma non essere necessariamente associata in quanto potrebbe essere l unico nodo in quell istante
		if(isBIGBOSS()){ 
			try {
				if(relayClusterWNICController.isOn() && relayClusterHeadWNICController.isConnected()) 
					becomeBigBossRelay();
				else{
					consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non pu� essere il BigBoss dato che o non vede l'AP o non � collegato alla rete Ad Hoc");
					throw new Exception("RelayElectionManager: ERRORE: questo nodo non pu� essere il BigBoss dato che o non vede l'AP o non � collegato alla rete Ad Hoc");
				}
			} catch (WNICException e) {
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager: ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}

		//Parto da relay secondario o possibile relay secondario o possibile BigBoss
		else if(isRELAY()||isPOSSIBLE_RELAY()||isPOSSIBLE_BIGBOSS()){
			try{
				if(relayClusterWNICController.isOn() && relayClusterHeadWNICController.isConnected())
					searchingRelay();
				else{
					consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non pu� essere il Relay dato che o non � collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
					throw new Exception("RelayElectionManager: ERRORE: Questo nodo non pu� essere il Relay dato che o non � collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
				}
					
			} catch (WNICException e) {
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager: ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}
			
		
		//parto come cliente normale  (relay o bigboss passivo)
//		else{
//			searchingRelay();
//		}
	}

	
	/**Metodo che consente di far si che questo nodo diventi il Relay attuale,
	 * memorizzando l'indirizzo locale come indirizzo del Relay, creando e facendo
	 * partire il RelayPositionAPMonitor, il RelayPositionClientsMonitor,
	 * il RelayBatteryMonitor e il WhoIsRelayServer. Poi passa allo stato di MONITORING.
	 */
	private void becomeBigBossRelay(){
		setNodeType(true, true);
		localClusterAddress = NetConfiguration.RELAY_CLUSTER_ADDRESS;
		memorizeLocalClusterAddress();
		localClusterHeadAddress = NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS;
		memorizelocalClusterHeadAddress();
		connectedClusterHeadAddress = NetConfiguration.SERVER_ADDRESS;
		memorizeConnectedClusterHeadAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
		//if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
		//if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();

		try {
			//Monitoraggio RSSI nei confronti del server
			relayPositionAPMonitor = new RelayPositionAPMonitor(
											relayClusterHeadWNICController,	
											TimeOutConfiguration.POSITION_AP_MONITOR_PERIOD,
											this);

			//Monitoraggio RSSI client e relay attivi
			relayPositionMonitor = new RelayPositionMonitor(
											ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
											TimeOutConfiguration.POSITION_CLIENTS_MONITOR_PERIOD,
											this,
											relayClusterWNICController.getDebugConsole());
			
			
			relayPositionAPMonitor.start();
			
			//Monitoraggio della batteria
			relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);
			
			
			//Risponde ai messaggi WHO_IS_RELAY
			whoIsRelayServer = new WhoIsRelayServer(consoleClusterWifiInterface);
			whoIsRelayServer.start();
				
			actualStatus = RelayStatus.IDLE;
			consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): stato ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
			//	monitoring();
			
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	/**Metodo che consente al relay di trovare il Big Boss, ovvero il relay del cluster head.
	 */
	private void searchingRelay(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCASTHEAD,PortConfiguration.WHO_IS_RELAY_PORT_IN);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {consoleClusterHeadWifiInterface.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this,	TimeOutConfiguration.TIMEOUT_SEARCH);
		actualStatus=RelayStatus.WAITING_WHO_IS_RELAY;
		consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
	}
	
	private void becomRelay(){
		setNodeType(false,true);
		localClusterAddress = NetConfiguration.RELAY_CLUSTER_ADDRESS;
		memorizeLocalClusterAddress();
		localClusterHeadAddress = NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS;
		memorizeLocalClusterAddress();

		//Azzero tutti i timeout
		if(timeoutSearch != null) timeoutSearch.cancelTimeOutSearch();
//		if(timeoutFailToElect != null) timeoutFailToElect.cancelTimeOutFailToElect();
//		if(timeoutElectionBeacon != null) timeoutElectionBeacon.cancelTimeOutElectionBeacon();
//		if(timeoutClientDetection != null) timeoutClientDetection.cancelTimeOutClientDetection();
//		if(timeoutEmElection != null) timeoutEmElection.cancelTimeOutEmElection();
		/*Fine Vedere se sta parte serve*/

		//Monitoraggio RSSI client
		relayPositionMonitor = new RelayPositionMonitor(
				ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
				TimeOutConfiguration.POSITION_CLIENTS_MONITOR_PERIOD,
				this,
				relayClusterWNICController.getDebugConsole());
		
		try{
			//risponde ai messaggi di RSSI_REQUEST prendendo il valore RSSI dalla scheda wifi collegata al cluster head
			relayPositionMonitorController = new RelayPositionMonitorController(relayClusterHeadWNICController);
			relayPositionMonitorController.start();
		}catch(WNICException e){e.getMessage();}
		
		//Client -> faccio partire nel momento in cui si collega qualche client...
		relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);

		whoIsRelayServer = new WhoIsRelayServer(consoleClusterWifiInterface);
		
		whoIsRelayServer.start();
		//relayBatteryMonitor.start();
	
		actualStatus = RelayStatus.IDLE;
		
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildAckConnection(connectedClusterHeadInetAddress, PortConfiguration.RELAY_ELECTION_CLUSTER_PORT_IN, MessageCodeConfiguration.TYPERELAY);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {consoleClusterHeadWifiInterface.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");e.getStackTrace();}
	
		consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeRelay(): X -> STATO IDLE: WhoIsRelayServer partito, ACK_CONNECTION spedito");
		//monitoring();
	}

	
	@Override
	public void update(Observable arg0, Object arg1) {
		
		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){
			DatagramPacket dpIn = (DatagramPacket)arg1;
			relayMessageReader = new RelayMessageReader();

			try {
				relayMessageReader.readContent(dpIn);
			} catch (IOException e) {e.printStackTrace();}
			
			/*
			 * Relay secondario attivo in cerca del big boss
			 */
			if((relayMessageReader.getCode() == MessageCodeConfiguration.IM_RELAY) && 
			   (actualStatus == RelayStatus.WAITING_WHO_IS_RELAY)){
				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}
				connectedClusterHeadAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedClusterHeadAddress();
				setChanged();
				notifyObservers("RELAY_FOUND:"+relayMessageReader.getActualConnectedRelayAddress());
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO "+actualStatus.toString()+": IM_RELAY arrivato, connectRelayAddress: "+connectedClusterHeadAddress);
				if(isRELAY()) becomRelay();
			}
			
	
			/*
			 * Conferma da parte di un nodo della connessione al Big Boss
			 */
			if((relayMessageReader.getCode()==MessageCodeConfiguration.ACK_CONNECTION) && 
			   (isBIGBOSS())){
				if(relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPERELAY)
					addRelay();
				else
					addClient();
				
				setChanged();
				notifyObservers("NEW_CONNECTED_RELAY:"+relayMessageReader.getPacketAddess().toString());
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo relay secondario connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
			}
		}
		
		if(arg1 instanceof String){

			String event = (String) arg1;
			
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.WAITING_WHO_IS_RELAY){
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				searchingRelay();
			}
		}
	}
	
	public void startMonitoringRSSI(){
		if(actualStatus!=RelayStatus.MONITORING){
			relayPositionMonitor.start();
			//relayBatteryMonitor.start();
			actualStatus = RelayStatus.MONITORING;
		}
			
	}
	public void stopMonitoringRSSI(){
		relayPositionMonitor.stop();
		//relayBatteryMonitor.stop();
		actualStatus = RelayStatus.IDLE;
	}
	
	public void addClient(){active_client++;startMonitoringRSSI();}
	public void removeClient(){
		active_client--;
		if(active_client==0 && active_relays==0) stopMonitoringRSSI();
	}
	
	public void addRelay(){active_relays++;startMonitoringRSSI();}
	public void removeRelay(){
		active_relays--;
		if(active_client==0 && active_relays==0) stopMonitoringRSSI();
	}
	
	
	
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeLocalClusterAddress(){
		try {
			localClusterInetAddress = InetAddress.getByName(localClusterAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeConnectedClusterHeadAddress(){
		try {
			connectedClusterHeadInetAddress = InetAddress.getByName(connectedClusterHeadAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizelocalClusterHeadAddress(){
		try {
			localClusterHeadInetAddress = InetAddress.getByName(localClusterHeadAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**
	 * Metodi getter e setter
	 */
	public void setConsoleElectionManager(DebugConsole consoleElectionManager) {this.consoleElectionManager = consoleElectionManager;}
	public DebugConsole getConsoleElectionManager() {return consoleElectionManager;}
	public void setConsoleClusterWifiInterface(DebugConsole consoleClusterWifiInterface) {this.consoleClusterWifiInterface = consoleClusterWifiInterface;}
	public DebugConsole getConsoleClusterWifiInterface() {return consoleClusterWifiInterface;}
	public void setConsoleClusterHeadWifiInterface(DebugConsole consoleClusterHeadWifiInterface) {this.consoleClusterHeadWifiInterface = consoleClusterHeadWifiInterface;}
	public DebugConsole getConsoleClusterHeadWifiInterface() {return consoleClusterHeadWifiInterface;}
	
	
	
	public static void setBCAST(InetAddress bcast) {BCAST = bcast;}
	public static InetAddress getBCAST() {return BCAST;}

	public void setComClusterManager(RelayCM comClusterManager) {this.comClusterManager = comClusterManager;}
	public RelayCM getComClusterManager() {return comClusterManager;}
	
	public void setComClusterHeadManager(RelayCM comClusterHeadManager) {this.comClusterHeadManager = comClusterHeadManager;}
	public RelayCM getComClusterHeadManager() {return comClusterHeadManager;}
	
	public void setRelayClusterWNICController(RelayWNICController relayClusterWNICController){this.relayClusterWNICController = relayClusterWNICController;}
	public RelayWNICController getRelayClusterWNICController(){return relayClusterWNICController;}
	
	public void setRelayClusterHeadWNICController(RelayWNICController relayClusterHeadWNICController){this.relayClusterHeadWNICController = relayClusterHeadWNICController;}
	public RelayWNICController getRelayClusterHeadWNICController(){return relayClusterHeadWNICController;}
	
	public void setActualStatus(RelayStatus actualStatus) {this.actualStatus = actualStatus;}
	public RelayStatus getActualStatus() {return actualStatus;}

	
	public void setLocalClusterAddress(String localClusterAddress) {this.localClusterAddress = localClusterAddress;}
	public String getLocalClusterAddress() {return localClusterAddress;}

	public void setConnectedClusterHeadAddress(String connectedClusterHeadAddress) {this.connectedClusterHeadAddress = connectedClusterHeadAddress;}
	public String getConnectedClusterHeadAddress() {return connectedClusterHeadAddress;}
	
	public void setLocalClusterHeadAddress(String localClusterHeadAddress) {this.localClusterHeadAddress = localClusterHeadAddress;}
	public String getLocalClusterHeadAddress() {return localClusterHeadAddress;}

	public void setLocalClusterInetAddress(InetAddress localClusterInetAddress) {this.localClusterInetAddress = localClusterInetAddress;}
	public InetAddress getLocalClusterInetAddress() {return localClusterInetAddress;}

	public void setConnectedClusterHeadInetAddress(InetAddress connectedClusterHeadInetAddress) {this.connectedClusterHeadInetAddress = connectedClusterHeadInetAddress;}
	public InetAddress getConnectedClusterHeadInetAddress() {return connectedClusterHeadInetAddress;}

	public void setLocalClusterHeadInetAddress(InetAddress localClusterHeadInetAddress) {this.localClusterHeadInetAddress = localClusterHeadInetAddress;}
	public InetAddress getLocalClusterHeadInetAddress() {return localClusterHeadInetAddress;}
	
	public void setRelayMessageReader(RelayMessageReader relayMessageReader){this.relayMessageReader = relayMessageReader;}
	public RelayMessageReader getRelayMessageReader(){return relayMessageReader;}
	
	public void setRelayBatteryMonitor(RelayBatteryMonitor relayBatteryMonitor){this.relayBatteryMonitor = relayBatteryMonitor;}
	public RelayBatteryMonitor getRelayBatteryMonitor(){return relayBatteryMonitor;}
	
	public void setWhoIsRelayServer(WhoIsRelayServer whoIsRelayServer){this.whoIsRelayServer = whoIsRelayServer;}
	public WhoIsRelayServer getWhoIsRelayServer(){return whoIsRelayServer;}
	
	public static void setRELAY(boolean relay) {RELAY = relay;}
	public static boolean isRELAY() {return RELAY;}
	public static void setBIGBOSS(boolean bigboss) {BIGBOSS = bigboss;}
	public static boolean isBIGBOSS() {return BIGBOSS;}
	public static void setPOSSIBLE_RELAY(boolean possible_relay) {POSSIBLE_RELAY = possible_relay;}
	public static boolean isPOSSIBLE_RELAY() {return POSSIBLE_RELAY;}
	public static void setPOSSIBLE_BIGBOSS(boolean possible_bigboss) {POSSIBLE_BIGBOSS=possible_bigboss;}
	public static boolean isPOSSIBLE_BIGBOSS() {return POSSIBLE_BIGBOSS;}
	public static void setCLIENT(boolean client) {CLIENT=client;}
	public static boolean isCLIENT() {return CLIENT;}
	

	public static void setINSTANCE(RelayElectionManager iNSTANCE) {INSTANCE = iNSTANCE;}
	public static RelayElectionManager getINSTANCE() {return INSTANCE;}
	
	



}
