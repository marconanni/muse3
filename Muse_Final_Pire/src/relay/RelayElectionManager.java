package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

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
import relay.position.RelayPositionController;
import relay.position.WeightCalculator;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutSingleWithMessage;
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
	
	private String localClusterAddress = null;				//indirizzo locale (interfacciato sul CLUSTER)
	private String localClusterHeadAddress = null;			//indirizzo locale (interfacciato sul CLUSTER HEAD)
	private String connectedClusterHeadAddress = null;		//indirizzo del nodo (CLUSTER HEAD) a cui si è connessi es. Relay connesso BigBos (indirizzo big boss)
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
	private RelayPositionController relayPositionController = null;
	private RelayBatteryMonitor relayBatteryMonitor = null;				//il RelayBatteryMonitor per conoscere la situazione della propria batteria
	private WhoIsRelayServer whoIsRelayServer = null;					//thread che risponde ai messaggi di WHO_IS_RELAY con IM_RELAY
	
	private int activeRelay = 1;			//relay secondari collegati al bigboss (solo big boss)
	private int activeClient = 1;			//client collegati al relay - aumenta solo, non si aggiorna

	//parametri per il protocollo di elezione
	private Vector<Couple> possibleRelay = null;	//Vector da riempire con le Couple relative agli ELECTION_RESPONSE ricevuti dal Relay uscente
	private double W = -1;							//peso del nodo
	private double maxW = -1;						//massimo peso rilevato
	private boolean electing = false;				//boolean che indica se si è in stato di elezione o meno
	private boolean electinHead = false;			//boolean che indica se si è coinvolti in un elezione (elezioni di un nuovo big boss, nel relay secondario attivo viene settato a true)
	private boolean firstELECTION_DONE_SEND = false;
	private int indexELECTION_BEACON = 0;
	private int indexELECTION_RESPONSE = 0;
	private int indexELECTION_DONE = 0;
	private int client_visibity = 0;
	private int relay_visibility = 0;
	private String bestSubstituteRelayAddress = null;
	private InetAddress bestSubstituteRelayInetAddress = null;
	
	//vari Timeout necessari al RelayElectionManager
	private TimeOutSingleWithMessage timeoutSearch = null;
	private TimeOutSingleWithMessage timeout_ToElect = null;				//serve al nodo che deve essere sostituito scaduto il quale determina il nodo che lo sostituisce
	private TimeOutSingleWithMessage timeoutElectionBeacon = null;		//serve al nodo possibile sosituto in caso di elezione
	private TimeOutSingleWithMessage timeoutFailToElect = null;
//	private TimeOutFailToElect timeoutFailToElect = null;
//	private TimeOutElectionBeacon timeoutElectionBeacon = null;
	//private TimeOutClientDetection timeoutClientDetection = null;
	//private TimeOutEmElection timeoutEmElection = null;
	
	public enum RelayStatus {					//stati in cui si può trovare il RelayElectionManager
		OFF,
		WAITING_WHO_IS_RELAY,
		WAITING_WHO_IS_HEAD_NODE,
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
	public static RelayElectionManager getInstance(int type, boolean state, RelaySessionManager sessionManager){
		if(INSTANCE == null)
			try {
				INSTANCE = new RelayElectionManager(type, state, sessionManager);
			} catch (Exception e) {e.printStackTrace();}
			return INSTANCE;
	}

	/**
	 * Metodo per definire il tipo di nodo
	 * @param type true -> BIGBOSS , false ->RELAY
	 * @param state true -> ACTIVE , false ->POSSIBLE
	 */
	private void setNodeType(int type, boolean state){
		setBIGBOSS(false);
		setRELAY(false);
		setPOSSIBLE_BIGBOSS(false);
		setPOSSIBLE_RELAY(false);
		setCLIENT(false);
		if(type==0 && state)setBIGBOSS(true);
		else if(type==0 && !state) setPOSSIBLE_BIGBOSS(true);
		else if(type==1 && state) setRELAY(true);
		else if(type==1 && !state) setPOSSIBLE_RELAY(true);
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
	private RelayElectionManager(int type, boolean state, RelaySessionManager sessionManager) throws Exception{

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
		if(isRELAY()){
			comClusterHeadManager = RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true);
			comClusterHeadManager.start();
		}

		//Se parto come Relay BIG BOSS
		//WIFI Managed deve essere associata al AP e quindi connessa
		//WIFI Ad-Hoc deve solo essere configurata correttamente ma non essere necessariamente associata in quanto potrebbe essere l unico nodo in quell istante
		if(isBIGBOSS()){ 
			try {
				if(relayClusterWNICController.isOn() && relayClusterHeadWNICController.isConnected()) 
					becomeBigBossRelay(0);
				else{
					consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non può essere il BigBoss dato che o non vede l'AP o non è collegato alla rete Ad Hoc");
					throw new Exception("RelayElectionManager: ERRORE: questo nodo non può essere il BigBoss dato che o non vede l'AP o non è collegato alla rete Ad Hoc");
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
					if(isRELAY())
						searchingRelayClusterHead();
					else if(isPOSSIBLE_RELAY()||isPOSSIBLE_BIGBOSS())
						searchingRelayCluster();
				else{
					consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non può essere il Relay dato che o non è collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
					throw new Exception("RelayElectionManager: ERRORE: Questo nodo non può essere il Relay dato che o non è collegato col cluster Head e non  collegato alla rete Ad Hoc (cluster)");
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
	private void becomeBigBossRelay(int state){
		setNodeType(0, true);
		if(state==0){
			setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
			memorizeLocalClusterAddress();
			setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
			memorizelocalClusterHeadAddress();
			setConnectedClusterHeadAddress(NetConfiguration.SERVER_ADDRESS);
		}
		
		memorizeConnectedClusterHeadAddress();

		//Azzero tutti i timeout
		cancelTimeoutSearch();
		cancelTimeoutToElect();
		cancelTimeoutElectionBeacon();
		cancelTimeoutFailToElect();

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
			relayBatteryMonitor.start();
			
			
			//Risponde ai messaggi WHO_IS_RELAY
			whoIsRelayServer = new WhoIsRelayServer(consoleClusterWifiInterface,getConnectedClusterHeadAddress());
			whoIsRelayServer.start();
				
			if(state==0){
				actualStatus = RelayStatus.IDLE;
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): stato ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
				else System.out.println("RelayElectionManager.becomeBigBossRelay(): stato ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
			}
			else if(state==1){
				actualStatus=RelayStatus.MONITORING;
				startMonitoringRSSI();
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" RSSIMonitoring partiti, WhoIsRelay partiti, BIGBOSS sostituito");
				else System.out.println("RelayElectionManager STATO:"+actualStatus+" RSSIMonitoring partiti, WhoIsRelay partiti, BIGBOSS sostituito");
			}
			
			
			//	monitoring();
			
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	private void becomRelay(int state){
		setNodeType(1,true);
		if(state==0){
			setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
			memorizeLocalClusterAddress();
			setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
			memorizeLocalClusterAddress();
			
			memorizeConnectedClusterHeadAddress();
		}

		//Azzero tutti i timeout
		cancelTimeoutSearch();
		cancelTimeoutToElect();
		cancelTimeoutElectionBeacon();
		cancelTimeoutFailToElect();
		
		//Monitoraggio RSSI client
		relayPositionMonitor = new RelayPositionMonitor(
				ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
				TimeOutConfiguration.POSITION_CLIENTS_MONITOR_PERIOD,
				this,
				relayClusterWNICController.getDebugConsole());
		
		try{
			//risponde ai messaggi di RSSI_REQUEST prendendo il valore RSSI dalla scheda wifi collegata al cluster head
			relayPositionController = new RelayPositionController(relayClusterHeadWNICController,this);
			relayPositionController.start();
		}catch(WNICException e){e.getMessage();}
		
		//Client -> faccio partire nel momento in cui si collega qualche client...
		relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);

		whoIsRelayServer = new WhoIsRelayServer(consoleClusterWifiInterface,getConnectedClusterHeadAddress());
		whoIsRelayServer.start();
			
		if(state==0)actualStatus = RelayStatus.IDLE;
		
		if(state==1){
			actualStatus = RelayStatus.MONITORING;
			comClusterHeadManager = RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true);
			comClusterHeadManager.start();
		}
		
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildAckConnection(connectedClusterHeadInetAddress, PortConfiguration.PORT_ELECTION_IN, MessageCodeConfiguration.TYPERELAY);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {consoleClusterHeadWifiInterface.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");e.getStackTrace();}
	
		if(state==0){
			if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeRelay(): X -> STATO IDLE: WhoIsRelayServer partito, ACK_CONNECTION spedito");
			else System.out.println("RelayElectionManager.becomeRelay(): X -> STATO IDLE: WhoIsRelayServer partito, ACK_CONNECTION spedito");
		}
		if(state==1){
			startMonitoringRSSI();
			if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomeRelay(): ho sostituito il vecchio relay e Monitoraggio RSSI partito");
			else System.out.println("RelayElectionManager.becomeRelay(): ho sostituito il vecchio relay e Monitoraggio RSSI partito");
		}
		//monitoring();
	}
	
	private void becomePossibleRelay(){
		setNodeType(1,false);
		setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		memorizeLocalClusterAddress();
		setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
		memorizeLocalClusterAddress();
		
		memorizeConnectedClusterHeadAddress();

		//Azzero tutti i timeout
		cancelTimeoutSearch();
		
	
		actualStatus = RelayStatus.IDLE;
		if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager.becomePossibleRelay(): X -> STATO "+actualStatus);
		else System.out.println("RelayElectionManager.becomePossibleRelay(): X -> STATO "+actualStatus);
	}
	
	/**Metodo che consente al relay di trovare il Big Boss, ovvero il relay del cluster head.
	 */
	private void searchingRelayClusterHead(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCASTHEAD,PortConfiguration.WHO_IS_RELAY_PORT_IN);
			comClusterHeadManager.sendTo(dpOut);
		} catch (IOException e) {consoleClusterHeadWifiInterface.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		setTimeoutSearch(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_SEARCH,TimeOutConfiguration.TIME_OUT_SEARCH));
		actualStatus=RelayStatus.WAITING_WHO_IS_RELAY;
		if(consoleClusterHeadWifiInterface!=null)consoleClusterHeadWifiInterface.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
		else System.out.println("RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
	}
	
	/**Metodo che consente ad un nodo possibile relay di trovare relay attivo all'interno del CLUSTER in cui si trova.
	 */
	private void searchingRelayCluster(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCAST,PortConfiguration.WHO_IS_RELAY_PORT_IN);
			comClusterManager.sendTo(dpOut);
		} catch (IOException e) {consoleClusterWifiInterface.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		setTimeoutSearch(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_SEARCH,TimeOutConfiguration.TIME_OUT_SEARCH));
		actualStatus=RelayStatus.WAITING_WHO_IS_RELAY;
		if(consoleClusterWifiInterface!=null)consoleClusterWifiInterface.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
		else System.out.println("RelayElectionManager: stato ["+actualStatus.toString()+"], inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
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
				if(isRELAY()){
					setConnectedClusterHeadAddress(relayMessageReader.getPacketAddess().getHostAddress());
					
					setChanged();
					notifyObservers("RELAY_FOUND:"+getConnectedClusterHeadAddress());
				
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO "+actualStatus.toString()+": IM_RELAY arrivato, connectHeadRelayAddress: "+getConnectedClusterHeadAddress());
					else System.out.println("RelayElectionManager: STATO "+actualStatus.toString()+": IM_RELAY arrivato, connectRelayAddress: "+getConnectedClusterHeadAddress());
					becomRelay(0);
				}
				else if(isPOSSIBLE_RELAY()|| isPOSSIBLE_BIGBOSS()){
					setConnectedClusterHeadAddress(relayMessageReader.getHeadNodeAddress());
					
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" IM_RELAY arrivato clusterRelay:"+relayMessageReader.getPacketAddess()+" clusterHead:"+getConnectedClusterHeadAddress());
					else System.out.println("RelayElectionManager STATO:"+actualStatus+" IM_RELAY arrivato clusterRelay:"+relayMessageReader.getPacketAddess()+" clusterHead:"+getConnectedClusterHeadAddress());
					
					becomePossibleRelay();
				}
			}
			
		/*
			 * Conferma da parte di un nodo della connessione al nodo corrente
			 */
			if(relayMessageReader.getCode()==MessageCodeConfiguration.ACK_CONNECTION){
				if(relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPERELAY){
					addRelay();
					setChanged();
					notifyObservers("NEW_CONNECTED_RELAY:"+relayMessageReader.getPacketAddess().toString());
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo relay secondario connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
					else System.out.println("RelayElectionManager: nuovo relay secondario connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
				}
				else if (relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPECLIENT){
					addClient();
					setChanged();
					notifyObservers("NEW_CONNECTED_CLIENT:"+relayMessageReader.getPacketAddess().toString());
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo client connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
					else System.out.println("RelayElectionManager: nuovo client connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
				}
//				else if(relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPERELAYPASSIVE){
//					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo possibile relay sostituto connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
//					else System.out.println("RelayElectionManager: nuovo possibile relay sostituto connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
//				}
				
			}

			else if((relayMessageReader.getCode() == MessageCodeConfiguration.ELECTION_REQUEST) && 
					(!sameAddress(dpIn.getAddress()))){

				//rielezione nuovo nodo relay BigBoss/Relay secondario e sono un nodo possibile sostituto
				//un nodo possibile sostituti sta in ascolto solo dei messaggi broadcast emessi sulla rete (CLUSTER) in cui ne fa parte
				if((isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())&& (actualStatus==RelayStatus.IDLE)){
					try {
						
						if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ELECTION_REQUEST arrivato, sostituto BIGBOSS, AP/BigBoss visibility:"+relayClusterHeadWNICController.isConnected());
						else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_REQUEST arrivato, sostituto BIGBOSS, AP/BigBoss visibility:"+relayClusterHeadWNICController.isConnected());
							
						firstELECTION_DONE_SEND = false;
						if(possibleRelay != null){
							possibleRelay.clear();
							possibleRelay = null;
						}
						
						electing = true;
						client_visibity = 0;
						relay_visibility = 0;
						W = -1;
						
						//ap Visibility == true
						if(relayClusterHeadWNICController.isConnected()){

							timeoutElectionBeacon = RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_ELECTION_BEACON, TimeOutConfiguration.TIME_OUT_ELECTION_BEACON);
							timeoutFailToElect = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT, TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT);

							actualStatus  = RelayStatus.WAITING_BEACON;
							
							if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" Ap/BigBoss Visibility == true, TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti");
							else System.out.println("RelayElectionManager STATO:"+actualStatus+" Ap/BigBoss Visibility == true, TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti");
						} 			

					} catch (WNICException e) {
						e.printStackTrace();
					}
					
					setChanged();
					notifyObservers("ELECTION_REQUEST_RECEIVED");
				}
				//Nodo BIGBOSS ATTIVO: vengo iformato che è in corso una elezione di un relay secondario
				else if(isBIGBOSS() && actualStatus==RelayStatus.MONITORING){
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager STATO:"+actualStatus+" relay secondario IP:"+relayMessageReader.getPacketAddess()+" è in fase di elezione");
					else System.out.println("RelayElectionManager STATO:"+actualStatus+" relay secondario IP:"+relayMessageReader.getPacketAddess()+" è in fase di elezione");
				}
				
				//Nodo RELAY SECONDARIO ATTIVO: viene eletto un nuovo BigBoss
				else if(isRELAY() && actualStatus == RelayStatus.MONITORING){
					electinHead = true;
					
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_WARNING,"RelayElectionManager STATO:"+actualStatus+" ELECTION_REQUEST arrivato da IP:"+relayMessageReader.getPacketAddess());
					else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_REQUEST arrivato da IP:"+relayMessageReader.getPacketAddess());
				
					//mando il messaggio di ELECTION_BEACON_RELAY ai possibili nodi sostituti		
					DatagramPacket dpOut = null;

						try {
							//Messaggio destinato ai possibili sostituti
							dpOut = RelayMessageFactory.buildElectioBeaconRelay(0, BCAST,PortConfiguration.PORT_ELECTION_IN, activeClient);
							comClusterHeadManager.sendTo(dpOut);
						
						} catch (IOException e){e.printStackTrace();}

						timeoutFailToElect = RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT,TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT);
						actualStatus = RelayStatus.WAITING_END_NORMAL_ELECTION;
					
						if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ELECTION_BEACON_RELAY inviato e start TIMEOUT_FAIL_TO_ELECT");
						else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_BEACON_BEACON inviato e start TIMEOUT_FAIL_TO_ELECT");
				}
			}
							
			else if((relayMessageReader.getCode() == MessageCodeConfiguration.ELECTION_BEACON_RELAY) &&
						(isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())&&
						 actualStatus == RelayStatus.WAITING_BEACON){
						client_visibity+= relayMessageReader.getActiveClient();
						if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ELECTION_BEACON_RELAY arrivato, client_visibility = "+client_visibity);
						else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_BEACON_RELAY arrivato, client_visibility = "+client_visibity);
			}
			
			else if((relayMessageReader.getCode() == MessageCodeConfiguration.ELECTION_RESPONSE) &&
					(actualStatus == RelayStatus.WAITING_RESPONSE)
					&& (!sameAddress(dpIn.getAddress()))){

				possibleRelay.add(new Couple(dpIn.getAddress().getHostAddress(),relayMessageReader.getW()));

				Collections.sort(possibleRelay);

				maxW = possibleRelay.get(0).getWeight();
				setBestSubstituteRelayAddress(possibleRelay.get(0).getAddress());
				memorizeBestSubstituteRelayAddress();
				
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ELECTION_RESPONSE arrivato -> miglior nodo giunto " + bestSubstituteRelayAddress + " con peso: " + maxW);
				else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_RESPONSE arrivato -> miglior nodo giunto " + bestSubstituteRelayAddress + " con peso: " + maxW);
			}
			
			else if((relayMessageReader.getCode() == MessageCodeConfiguration.ELECTION_DONE) && 
					(actualStatus == RelayStatus.WAITING_END_NORMAL_ELECTION)){
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE arrivato: nuovo Relay: "+relayMessageReader.getNewRelayAddress()+"!="+localClusterAddress);
				else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE arrivato: nuovo Relay: "+relayMessageReader.getNewRelayAddress()+"!="+localClusterAddress);
				
				if(timeoutFailToElect != null){
					timeoutFailToElect.cancelTimeOutSingleWithMessage();
					timeoutFailToElect = null;
				}
				
				if(isRELAY()){
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"Nodo corrente è un relay secondario attivo ed è stato appena eletto un nuovo BIGBOSS");
					else System.out.println("Nodo corrente è un relay secondario attivo ed è stato appena eletto un nuovo BIGBOSS");
					
					setConnectedClusterHeadAddress(relayMessageReader.getNewRelayAddress());
					memorizeConnectedClusterHeadAddress();
					
					if(activeClient>0)actualStatus = RelayStatus.MONITORING;
					else actualStatus=RelayStatus.IDLE;
					
					setChanged();
					notifyObservers("NEW_RELAY:"+connectedClusterHeadAddress);
					
					/**** MANCA LA PARTE CHE LI DEVO DIRE AI POSSIBILI NODI SOSTITUTI**/
					
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager STATO:"+actualStatus+" nuovo Relay BigBoss salvato correttamente"); 
					else System.out.println("RelayElectionManager STATO:"+actualStatus+" nuovo Relay BigBoss salvato correttamente"); 
					
				}
				else if(isPOSSIBLE_RELAY()){
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"Nodo corrente è un possibile sostituto relay ed è stato appena eletto un nuovo Relay secondario, controllo se è questo nodo");
					else System.out.println("Nodo corrente è un possibile sostituto relay ed è stato appena eletto un nuovo Relay secondario, controllo se è questo il nodo eletto");
					
					try {
						if(sameAddress(InetAddress.getByName(relayMessageReader.getNewRelayAddress()))){
							becomRelay(1);
							
							/** QUI DEVO ANCORA MANDARE CONFERMA DELLA SOSTITUZIONE **/
						}
						else{
							setNodeType(1, false);
							//** devo memorizzare il nuovo relay**/
						}
					} catch (UnknownHostException e) {e.printStackTrace();}
						
				}
								
				else if(isPOSSIBLE_RELAY()){
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"Nodo corrente è un possibile sostituto del Big Boss ed è appena stato rieletto un nuovo BigBoss, controllo se è questo nodo");
					else System.out.println("Nodo corrente è un possibile sostituto del Big Boss ed è appena stato rieletto un nuovo BigBoss, controllo se è questo nodo");
					
					try {
						if(sameAddress(InetAddress.getByName(relayMessageReader.getNewRelayAddress()))){
							becomeBigBossRelay(1);
							
							/** QUI DEVO ANCORA MANDARE CONFERMA DELLA SOSTITUZIONE **/
						}
						else{
							setNodeType(0, false);
							//PROPAGAZIONE
							//** devo memorizzare il nuovo relay**/
						}
					} catch (UnknownHostException e) {e.printStackTrace();}
				}
			}
					
		}
		
		if(arg1 instanceof String){

			String event = (String) arg1;
			
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.WAITING_WHO_IS_RELAY){
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				if(isRELAY())
					searchingRelayClusterHead();
				else if(isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())
					searchingRelayCluster();
			}
			
			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay*/
			else if(event.equals("TIMEOUTSEARCH") &&	actualStatus == RelayStatus.WAITING_WHO_IS_HEAD_NODE){
				consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO, "RelayElectionManager: STATO OFF: TIMEOUT_SEARCH scattato");
				if(isRELAY())
					searchingRelayClusterHead();
				else if(isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())
					searchingRelayCluster();
			}
			/*[DISCONNECTION_WARNING sollevato il nodo corrente se ne sta andando]
			 * imRealy() imBigBoss()
			 * status = monitoring
			 */
			else if((event.equals("DISCONNECTION_WARNING")) && 
					(actualStatus == RelayStatus.MONITORING)){

				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_WARNING,"STATO "+actualStatus+": DISCONNECTION_WARNING sollevato, chiudo tutti i monitor e spedisco il messaggio di ELECTION_REQUEST");
				else System.out.println("STATO "+actualStatus+": DISCONNECTION_WARNING sollevato, chiudo tutti i monitor e spedisco il messaggio di ELECTION_REQUEST");
				W = -1;
				maxW = -1;

				if(whoIsRelayServer!=null)whoIsRelayServer.close();
				if(relayPositionAPMonitor!=null)relayPositionAPMonitor.close();
				if(relayPositionMonitor!=null)relayPositionMonitor.close();
				//if(relayBatteryMonitor!=null)relayBatteryMonitor.close();

				possibleRelay = new Vector<Couple>();

				DatagramPacket dpOut = null;

				try {
					//invio ai nodi collegato ad esso
					dpOut = RelayMessageFactory.buildElectionRequest(BCAST, PortConfiguration.PORT_ELECTION_IN);
					comClusterManager.sendTo(dpOut);
					//caso in cui sono un relay secondario informo il BigBoss
					if(isRELAY()){
						dpOut = RelayMessageFactory.buildElectionRequest(connectedClusterHeadInetAddress, PortConfiguration.PORT_ELECTION_IN);
						comClusterHeadManager.sendTo(dpOut);
					}
				} catch (IOException e) {e.printStackTrace();}

				timeoutToElect = RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_TO_ELECT,TimeOutConfiguration.TIME_OUT_TO_ELECT);

				electing = true;
				setBIGBOSS(false);
				setRELAY(false);

				actualStatus = RelayStatus.WAITING_RESPONSE;
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"STATO "+actualStatus+" ELECTION_REQUEST inviato e TIMEOUT_TO_ELECT partito");
				else System.out.println("STATO "+actualStatus+" ELECTION_REQUEST inviato e TIMEOUT_TO_ELECT partito");
			}
			
			else if((event.equals("TIMEOUTELECTIONBEACON")) &&
					(isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY()) &&
					actualStatus == RelayStatus.WAITING_BEACON){
				
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" TIMEOUT_ELECTION_BEACON scattato, procedo al calcolo del W");
				else System.out.println("RelayElectionManager STATO:"+actualStatus+" TIMEOUT_ELECTION_BEACON scattato, procedo al calcolo del W");

				//Stato instabile WeightCalculation
				weightCalculation();

				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager W calcolato:"+W);
				else System.out.println("RelayElectionManager W calcolato:"+W);

				DatagramPacket dpOut = null;

				try {
					dpOut = RelayMessageFactory.buildElectionResponse(indexELECTION_RESPONSE, W, connectedClusterHeadInetAddress, PortConfiguration.PORT_ELECTION_IN);
					comClusterManager.sendTo(dpOut);
					indexELECTION_RESPONSE++;
				} catch (IOException e) {e.printStackTrace();}

				actualStatus = RelayStatus.WAITING_END_NORMAL_ELECTION;
				
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" ECTION_RESPONSE inviato a "+connectedClusterHeadAddress+" attesa o che scatti il TIMEOUTFAILTOELECT o che ricevo risposta dal nodo da sosituire");
				else System.out.println("RelayElectionManager STATO:"+actualStatus+" ECTION_RESPONSE inviato a "+connectedClusterHeadAddress+" attesa o che scatti il TIMEOUTFAILTOELECT o che ricevo risposta dal nodo da sosituire");
			}
			
			else if((event.equals("TIMEOUTTOELECT")) &&
					(actualStatus == RelayStatus.WAITING_RESPONSE)){

				electing = false;
				//firstEM_ELECTIONarrived = false;
				
				if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager STATO:"+actualStatus+" TIMEOUT_TO_ELECT scattato");

				//Ho trovato nodi possibili sostituti
				if (!possibleRelay.isEmpty()) {

					DatagramPacket dpOut = null;

					try {
						//invio ai Relay
						dpOut = RelayMessageFactory.buildElectionDone(
								indexELECTION_DONE, bestSubstituteRelayAddress,
								BCAST, PortConfiguration.PORT_ELECTION_IN);
						comClusterManager.sendTo(dpOut);
						indexELECTION_DONE++;
						//firstELECTION_DONEsent = true;
					} catch (IOException e) {e.printStackTrace();}

					//faccio sapere al SESSIONMANAGER chi ho appena eletto
					setChanged();
					notifyObservers("NEW_RELAY:" + bestSubstituteRelayAddress);
					
					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_WARNING, "RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE + "+bestSubstituteRelayAddress+" inviato e passo allo stato di IDLE");
					else System.out.println("RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE + "+bestSubstituteRelayAddress+" inviato e passo allo stato di IDLE");
				}

				maxW = -1;				

				actualStatus = RelayStatus.IDLE;
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
	
	public void addClient(){
		activeClient++;
		//da levare quando è finito in quanto è il session manager che deve farlo partire
		startMonitoringRSSI();
	}
	
	public void removeClient(){
		activeClient--;
		if(activeClient==0 && activeRelay==0) stopMonitoringRSSI();
	}
	
	public void addRelay(){activeRelay++;startMonitoringRSSI();}
	public void removeRelay(){
		activeRelay--;
		if(activeClient==0 && activeRelay==0) stopMonitoringRSSI();
	}
	
	/**Metodo per calcolare il W tramite il WeightCalculation a cui passo il RelayWNICController 
	 * e il numero di clients rilevati 
	 */
	private void weightCalculation(){
		W = WeightCalculator.calculateWeight(relayClusterHeadWNICController,client_visibity);
	}
	
	private boolean sameAddress(InetAddress adr){
		if(adr.equals(localClusterHeadAddress))	return true;
		if(adr.equals(localClusterHeadInetAddress))	return true;
		else return false;
	}
		
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeLocalClusterAddress(){
		try {
			setLocalClusterInetAddress(InetAddress.getByName(localClusterAddress));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeConnectedClusterHeadAddress(){
		try {
			setConnectedClusterHeadInetAddress(InetAddress.getByName(connectedClusterHeadAddress));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizelocalClusterHeadAddress(){
		try {
			setLocalClusterHeadInetAddress(InetAddress.getByName(localClusterHeadAddress));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	private void memorizeBestSubstituteRelayAddress(){
		try {
			setBestSubstituteRelayInetAddress(InetAddress.getByName(bestSubstituteRelayAddress));
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
	
	private void setBestSubstituteRelayAddress(String bestSubstituteRelayAddress) {this.bestSubstituteRelayAddress = bestSubstituteRelayAddress;}
	private String getBestSubstituteRelayAddress() {return bestSubstituteRelayAddress;}
	
	private void setBestSubstituteRelayInetAddress(InetAddress bestSubstituteRelayInetAddress) {this.bestSubstituteRelayInetAddress = bestSubstituteRelayInetAddress;}
	private InetAddress getBestSubstituteRelayInetAddress() {return bestSubstituteRelayInetAddress;}
	
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
	public int getActiveClient(){ return activeClient;}
	public int getActiveRelay(){ return activeRelay;}
	
	//timeout
	private void setTimeoutSearch (TimeOutSingleWithMessage timeoutSearch){this.timeoutSearch = timeoutSearch;}
	private TimeOutSingleWithMessage getTimeoutSearch(){return timeoutSearch;}
	private void cancelTimeoutSearch(){
		if(getTimeoutSearch()!=null){
			getTimeoutSearch().cancelTimeOutSingleWithMessage();
			setTimeoutSearch(null);
		}
	}
	
	private void setTimeoutToElect (TimeOutSingleWithMessage timeoutToElect){this.timeoutToElect = timeoutToElect;}
	private TimeOutSingleWithMessage getTimeoutToElect(){return timeoutToElect;}
	private void cancelTimeoutToElect(){
		if(getTimeoutToElect()!=null){
			getTimeoutToElect().cancelTimeOutSingleWithMessage();
			setTimeoutToElect(null);
		}
	}
	
	private void setTimeoutElectionBeacon (TimeOutSingleWithMessage timeoutElectionBeacon){this.timeoutElectionBeacon = timeoutElectionBeacon;}
	private TimeOutSingleWithMessage getTimeoutElectionBeacon(){return timeoutElectionBeacon;}
	private void cancelTimeoutElectionBeacon(){
		if(getTimeoutElectionBeacon()!=null){
			getTimeoutElectionBeacon().cancelTimeOutSingleWithMessage();
			setTimeoutElectionBeacon(null);
		}
	}
	
	private void setTimeoutFailToElect (TimeOutSingleWithMessage timeoutFailToElect){this.timeoutFailToElect = timeoutFailToElect;}
	private TimeOutSingleWithMessage getTimeoutFailToElect(){return timeoutFailToElect;}
	private void cancelTimeoutFailToElect(){
		if(getTimeoutFailToElect()!=null){
			getTimeoutFailToElect().cancelTimeOutSingleWithMessage();
			setTimeoutFailToElect(null);
		}
	}
}
