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
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutSearch;
import relay.wnic.RelayWNICController;
import relay.wnic.WNICFinder;
import relay.wnic.exception.WNICException;
import debug.DebugConsole;


public class RelayElectionManager extends Observable implements Observer{
	
	
	private DebugConsole console = null;					//Debug consolle del RelayElectionManager
	
	
	private RelayStatus actualStatus = null;				//stato attuale del RelayElectionManager
	
	private String localRelayAddress = null;				//indirizzo del Relay attuale in forma String
	private String connectedRelayAddress = null;			//indirizzo attuale del Relay (big boss) a cui è connesso -> solo relay normale
	private String serverAddress = null;					//indirizzo del server a cui il relay big boss è connesso -> solo relay big boss
	private InetAddress localRelayInetAddress = null;		//indirizzo del Relay in forma InetAddress
	private InetAddress connectedRelayInetAddress = null;	//indirizzo del Relay in forma InetAddress
	private InetAddress serverInetAddress = null;			//indirizzo del Server in forma InetAddress
	
	private static RelayElectionManager INSTANCE = null;	//istanza singleton del RelayElectionManager
	private static boolean IMBIGBOSS = false;				//boolean che indica se si è il BigBoss attivo (connesso al nodo server) o meno
	private static boolean IMRELAY = false;					//boolean che indica se si è il Relay e attivo o meno
	private static InetAddress BCAST = null;				//indirizzo broadcast del cluster locale in forma InetAddress
	private static InetAddress BCASTHEAD = null;			//indirizzo broadcast del cluster head in forma InetAddress
	
	private RelayCM comClusterManager = null;				//il manager per le comunicazioni nel proprio cluster
	private RelayCM comClusterHeadManager = null;			//il manager per le comunicazioni col cluster head	
	
	private RelayMessageReader relayMessageReader = null;				//il RelayMessageReader per leggere il contenuto dei messaggi ricevuti
	private RelayWNICController relayClusterWNICController = null;		//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster)
	private RelayWNICController relayClusterHeadWNICController = null;	//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster head)
	private RelayPositionAPMonitor relayPositionAPMonitor = null;		//il RelayPositionAPMonitor per conoscere la propria posizione nei confronti dell'AP
	private RelayPositionMonitor relayPositionMonitor = null;			//il RelayPositionMonitor per conoscere la propria posizione nei confronti dei client
	private RelayBatteryMonitor relayBatteryMonitor = null;				//il RelayBatteryMonitor per conoscere la situazione della propria batteria
	private WhoIsRelayServer whoIsRelayServer = null;					//thread che risponde ai messaggi di WHO_IS_RELAY con IM_RELAY
	
	private int active_relays = 0;								//relay secondari collegati al bigboss (solo big boss)
	
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
	
//	byte [] mask = { (byte)255, 0, 0, 0 };
//	byte[] addrBytes = InetAddress.getByName("126.5.6.7").getAddress();
//	for (int i=0; i < 4; i++) {
//	  addrBytes[i] |= ((byte)0xFF) ^ mask[i];
//	}
//	InetAddress bcastAddr = InetAddress.getByAddress(addrBytes);

	
//	byte [] mask = { (byte)255, (byte)255, (byte)255, 0 };
//	byte[] addrBytes = InetAddress.getByName(NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS).getAddress();
//	for (int i=0; i < 4; i++) {
//	  addrBytes[i] |= ((byte)0xFF) ^ mask[i];
//	}
//	InetAddress bcastAddr = InetAddress.getByAddress(addrBytes);

	
	static { 
		try {
			BCAST  = InetAddress.getByName(NetConfiguration.CLUSTER_BROADCAST_ADDRESS);
			BCASTHEAD  = InetAddress.getByName(NetConfiguration.CLUSTER_HEAD_BROADCAST_ADDRESS);
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
	 */
	private RelayElectionManager(boolean imBigBoss, boolean imRelay, RelaySessionManager sessionManager) throws Exception{

		this.actualStatus = RelayStatus.OFF;
		setIMBIGBOSS(imBigBoss);
		setIMRELAY(imRelay);
		this.addObserver((Observer) sessionManager);
		this.console = new DebugConsole();
		this.console.setTitle("RELAY ELECTION MANAGER DEBUG CONSOLE");
		
		//Controllo delle interfacce WIFI
		try {
			//caso in cui sono nel cluster head (rete ad hoc principale)
			//devo avere il controllo della scheda di rete interfacciata alla rete Managed nei confronti del server
			//nodi bigboss o possibili nodi sostituti
			if((IMBIGBOSS)||(NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS.compareTo(NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS))==0){
				relayClusterHeadWNICController = WNICFinder.getCurrentWNIC(
						NetConfiguration.NAME_OF_MANAGED_WIFI_INTERFACE,
						NetConfiguration.NAME_OF_MANAGED_NETWORK,
						ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
			}
			
			//se invece in una delle reti cluster secondarie 
			//devo avere il controllo della scheda di rete interfacciata alla rete cluster head
			//devo mandare il segnale RSSI al big boss quando richiesto
			else{
				relayClusterHeadWNICController = WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK,
					ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
			}
			
			relayClusterWNICController = WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_AD_HOC_CLUSTER_NETWORK,
					ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL);
			
		} catch (WNICException e) {System.err.println("ERRORE:"+e.getMessage());System.exit(1);}
		
		comClusterManager = RelayConnectionFactory.getClusterElectionConnectionManager(this);
		comClusterManager.start();
		
		//Il big boss non comunica col server e quindi non serve un connection Manager col server
		//Ogni relay di ogni cluster comunica col big boss, ovvero il relay del cluster head
		if(!IMBIGBOSS && IMRELAY){
			comClusterHeadManager = RelayConnectionFactory.getClusterHeadElectionConnectionManager(this);
			comClusterHeadManager.start();
		}

		//Se parto come Relay BIG BOSS
		if(IMBIGBOSS){ 
			try {
				if(relayClusterWNICController.isConnected() && relayClusterHeadWNICController.isConnected()) 
					becomeBigBossRelay();
				else{
					console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non pu� essere il BigBoss dato che o non vede l'AP o non � collegato alla rete Ad Hoc");
					throw new Exception("RelayElectionManager: ERRORE: questo nodo non pu� essere il BigBoss dato che o non vede l'AP o non � collegato alla rete Ad Hoc");
				}
			} catch (WNICException e) {
				console.debugMessage(DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager: ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}

		//Parto da relay secondario
		else if(IMRELAY){
			try{
				if(relayClusterWNICController.isConnected() && relayClusterHeadWNICController.isConnected())
					searchingBigBossRelay();
				else{
					console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non pu� essere il Relay dato che o non � collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
					throw new Exception("RelayElectionManager: ERRORE: Questo nodo non pu� essere il Relay dato che o non � collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
				}
					
			} catch (WNICException e) {
				console.debugMessage(DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
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
		setIMBIGBOSS(true);
		setIMRELAY(true);
		localRelayAddress = NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS;
		memorizeLocalRelayAddress();

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
			
			
			//Monitoraggio della batteria
			relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);
			
			//Risponde ai messaggi WHO_IS_RELAY
			whoIsRelayServer = new WhoIsRelayServer(console);
			whoIsRelayServer.start();
				
			actualStatus = RelayStatus.IDLE;
			console.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): stato ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
			monitoring();
			
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	/**Metodo che consente al relay di trovare il Big Boss, ovvero il relay del cluster head.
	 */
	private void searchingBigBossRelay(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCASTHEAD,PortConfiguration.WHO_IS_RELAY_PORT_IN);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		timeoutSearch = RelayTimeoutFactory.getTimeOutSearch(this,	TimeOutConfiguration.TIMEOUT_SEARCH);
		actualStatus=RelayStatus.WAITING_WHO_IS_RELAY;
		console.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
	}
	
	private void becomRelay(){
		setIMBIGBOSS(false);
		setIMRELAY(true);
		localRelayAddress = NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS;
		memorizeLocalRelayAddress();

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
		
	
		//Client -> faccio partire nel momento in cui si collega qualche client...
		relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);

		whoIsRelayServer = new WhoIsRelayServer(console);
		
		whoIsRelayServer.start();
		//relayBatteryMonitor.start();
	
		actualStatus = RelayStatus.IDLE;
		
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildAckConnection(connectedRelayInetAddress, PortConfiguration.RELAY_ELECTION_CLUSTER_HEAD_PORT_IN, MessageCodeConfiguration.TYPERELAY);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");e.getStackTrace();}
	
		console.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeRelay(): X -> STATO IDLE: WhoIsRelayServer partito, ACK_CONNECTION spedito");
		monitoring();
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
			   (actualStatus == RelayStatus.WAITING_WHO_IS_RELAY) &&
			   (IMRELAY)){
				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}
				connectedRelayAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedRelayAddress();
				setChanged();
				notifyObservers("RELAY_FOUND:"+relayMessageReader.getActualConnectedRelayAddress());
				console.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO "+actualStatus.toString()+": IM_RELAY arrivato, connectRelayAddress: "+connectedRelayAddress);
				becomRelay();
			}
			
			/*
			 * Client o relay passivo in cerca di un relay attivo
			 */
			else if(relayMessageReader.getCode() == MessageCodeConfiguration.IM_RELAY && actualStatus == RelayStatus.WAITING_WHO_IS_RELAY){
				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}
				actualStatus = RelayStatus.IDLE;
				connectedRelayAddress = relayMessageReader.getActualConnectedRelayAddress();
				memorizeConnectedRelayAddress();
				setChanged();
				notifyObservers("RELAY_FOUND:"+connectedRelayAddress);
				console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: STATO IDLE: IM_RELAY arrivato: connectedRelayAddress: "+ connectedRelayAddress);
			}
			
			/*
			 * Conferma da parte di un nodo della connessione al Big Boss
			 */
			if((relayMessageReader.getCode()==MessageCodeConfiguration.ACK_CONNECTION) && 
			   (IMBIGBOSS)){
				if(relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPERELAY)
					active_relays++;
				monitoring();
				setChanged();
				notifyObservers("NEW_CONNECTED_RELAY:"+relayMessageReader.getPacketAddess().toString());
				console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo relay secondario connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
			}
		}
		
		if(arg1 instanceof String){

			String event = (String) arg1;
			
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.WAITING_WHO_IS_RELAY){
				console.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				if(IMRELAY)
					searchingBigBossRelay();
//				else
//					searchingRelay();
			}
		}
	}
	
	public void monitoring(){
		relayPositionAPMonitor.start();
		relayPositionMonitor.start();
		relayPositionMonitor.startRSSIMonitor();
		//relayBatteryMonitor.start();
		actualStatus = RelayStatus.MONITORING;
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeLocalRelayAddress(){
		try {
			localRelayInetAddress = InetAddress.getByName(localRelayAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeConnectedRelayAddress(){
		try {
			connectedRelayInetAddress = InetAddress.getByName(connectedRelayAddress);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	
	
	/**
	 * Metodi getter e setter
	 */
	public void setConsole(DebugConsole console) {this.console = console;}
	public DebugConsole getConsole() {return console;}
	
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

	public void setLocalRelayAddress(String localRelayAddress) {this.localRelayAddress = localRelayAddress;}
	public String getLocalRelayAddress() {return localRelayAddress;}

	public void setConnectedRelayAddress(String connectedRelayAddress) {this.connectedRelayAddress = connectedRelayAddress;}
	public String getConnectedRelayAddress() {return connectedRelayAddress;}
	
	public void setServerAddress(String serverAddress) {this.serverAddress = serverAddress;}
	public String getServerAddress() {return serverAddress;}

	public void setLocalRelayInetAddress(InetAddress localRelayInetAddress) {this.localRelayInetAddress = localRelayInetAddress;}
	public InetAddress getLocalRelayInetAddress() {return localRelayInetAddress;}

	public void setConnectedRelayInetAddress(InetAddress connectedRelayInetAddress) {this.connectedRelayInetAddress = connectedRelayInetAddress;}
	public InetAddress getConnectedRelayInetAddress() {return connectedRelayInetAddress;}
	
	public void setServerInetAddress(InetAddress serverInetAddress) {this.serverInetAddress = serverInetAddress;}
	public InetAddress getServerInetAddress() {return serverInetAddress;}
	
	public void setRelayMessageReader(RelayMessageReader relayMessageReader){this.relayMessageReader = relayMessageReader;}
	public RelayMessageReader getRelayMessageReader(){return relayMessageReader;}
	
	public void setRelayBatteryMonitor(RelayBatteryMonitor relayBatteryMonitor){this.relayBatteryMonitor = relayBatteryMonitor;}
	public RelayBatteryMonitor getRelayBatteryMonitor(){return relayBatteryMonitor;}
	
	public void setWhoIsRelayServer(WhoIsRelayServer whoIsRelayServer){this.whoIsRelayServer = whoIsRelayServer;}
	public WhoIsRelayServer getWhoIsRelayServer(){return whoIsRelayServer;}
	

	public static void setIMRELAY(boolean imrelay) {IMRELAY = imrelay;}
	public static boolean isIMRELAY() {return IMRELAY;}
	public static void setIMBIGBOSS(boolean imbigboss) {IMBIGBOSS = imbigboss;}
	public static boolean isIMBIGBOSS() {return IMBIGBOSS;}

	public static void setINSTANCE(RelayElectionManager iNSTANCE) {INSTANCE = iNSTANCE;}
	public static RelayElectionManager getINSTANCE() {return INSTANCE;}
	
	



}
