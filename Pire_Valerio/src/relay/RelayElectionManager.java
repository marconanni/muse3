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
	private String event = null;
	
	private String localClusterAddress = null;				//indirizzo locale (interfacciato sul CLUSTER)
	private String localClusterHeadAddress = null;			//indirizzo locale (interfacciato sul CLUSTER HEAD)
	private String connectedClusterHeadAddress = null;		//indirizzo del nodo (CLUSTER HEAD) a cui si è connessi es. Relay connesso BigBos (indirizzo big boss)
	private InetAddress localClusterInetAddress = null;		//indirizzo del Relay in forma InetAddress
	private InetAddress localClusterHeadInetAddress = null;			//indirizzo del Server in forma InetAddress
	private InetAddress connectedClusterHeadInetAddress = null;	//indirizzo del Relay in forma InetAddress

	
	private static RelayElectionManager INSTANCE = null;	//istanza singleton del RelayElectionManager
	private static boolean BIGBOSS = true;					//boolean che indica che si è il BIGBOSS (connesso al server)
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
	
	private RelaySessionManager relaySessionManager = null;
	
	private String newRelayLocalClusterAddress;
	private String oldRelayLocalClusterAddress;
	private String oldRelayLocalClusterHeadAddress;
	private String headNodeAddress;
	


	private int activeRelay = 0;			//relay secondari collegati al bigboss (solo big boss)
	private int activeClient = 0;			//client collegati al relay - aumenta solo, non si aggiorna

	//parametri per il protocollo di elezione
	private Vector<Couple> possibleRelay = null;	//Vector da riempire con le Couple relative agli ELECTION_RESPONSE ricevuti dal Relay uscente
	private double W = -1;							//peso del nodo
	private boolean electing = false;				//boolean che indica se si è in stato di elezione o meno
	private boolean electingHead = false;			//boolean che indica se si è coinvolti in un elezione (elezioni di un nuovo big boss, nel relay secondario attivo viene settato a true)
	private boolean firstELECTION_DONE = false;
	private boolean firstELECTION_REQUEST = false;
	private int client_visibity = 0;
	private String bestSubstituteRelayAddress = null;
	private InetAddress bestSubstituteRelayInetAddress = null;
	private String relayToElect = null;
	
	//vari Timeout necessari al RelayElectionManager
	private TimeOutSingleWithMessage timeoutSearch = null;
	private TimeOutSingleWithMessage timeoutToElect = null;				//serve al nodo che deve essere sostituito scaduto il quale determina il nodo che lo sostituisce
	private TimeOutSingleWithMessage timeoutElectionBeacon = null;		//serve al nodo possibile sosituto in caso di elezione
	private TimeOutSingleWithMessage timeoutFailToElect = null;
	private Vector<DatagramPacket> dbBeaconStore = null;
//	private TimeOutFailToElect timeoutFailToElect = null;
//	private TimeOutElectionBeacon timeoutElectionBeacon = null;
//	private TimeOutClientDetection timeoutClientDetection = null;
//	private TimeOutEmElection timeoutEmElection = null;
	

	//per i test
	private long startElectionNoTimeOut = -1;
	private long stopElectionNoTimeOut = -1;
	private long intermedio = -1;
	private long startElection = -1;
	private long stopElection = -1;
	
	private static int NODE = 1;
	private static int NUMBERPOSSIBLERELAY = 1;
	
	private int numberOfNode = -1;
	private int numberOfPossibleRelay = -1;

	private boolean test = true;
	

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
	public static RelayElectionManager getInstance(int type, boolean state){
		if(INSTANCE == null)
			try {
				INSTANCE = new RelayElectionManager(type, state);
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
	private RelayElectionManager(int type, boolean state) throws Exception{
		setActualStatus(RelayStatus.OFF);
		setNodeType(type, state);
		
		setConsoleElectionManager(new DebugConsole("RELAY ELECTION MANAGER"));
			
		//Controllo delle interfacce WIFI
		try {
			//CLUSTER HEAD
			//caso in cui sono nel cluster head (rete ad hoc principale)
			//devo avere il controllo della scheda di rete interfacciata alla rete Managed nei confronti del server
			//nodi bigboss o possibili nodi sostituti
			if((isBIGBOSS())||isPOSSIBLE_BIGBOSS()){
				
				setRelayClusterHeadWNICController(WNICFinder.getCurrentWNIC(
						NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE,
						NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_NETWORK,
						0));
				
				setConsoleClusterHeadWifiInterface(new DebugConsole("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE));
				getRelayClusterHeadWNICController().setDebugConsole(getConsoleClusterHeadWifiInterface());
				getRelayClusterHeadWNICController().init();
			}
			
			//se invece in una delle reti cluster secondarie 
			//devo avere il controllo della scheda di rete interfacciata alla rete cluster head
			//per comunicare col BIGBOSS
			else{
				setRelayClusterHeadWNICController(WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_NETWORK,
					1));
				
				setConsoleClusterHeadWifiInterface(new DebugConsole("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE));
				getRelayClusterHeadWNICController().setDebugConsole(getConsoleClusterHeadWifiInterface());
				getRelayClusterHeadWNICController().init();
			}
			
			//CLUSTER (RETE LOCALE)
			setRelayClusterWNICController(WNICFinder.getCurrentWNIC(
					NetConfiguration.NAME_OF_RELAY_CLUSTER_WIFI_INTERFACE,
					NetConfiguration.NAME_OF_RELAY_CLUSTER_NETWORK,
					1));
			
			setConsoleClusterWifiInterface(new DebugConsole("WIFI INTERFACE: "+NetConfiguration.NAME_OF_RELAY_CLUSTER_WIFI_INTERFACE));
			getRelayClusterWNICController().setDebugConsole(getConsoleClusterWifiInterface());
			getRelayClusterWNICController().init();
			
		} catch (WNICException e) {System.err.println("ERRORE:"+e.getMessage());System.exit(1);}
		
		//comunicazione all'interno del prorpio CLUSTER
		setComClusterManager(RelayConnectionFactory.getClusterElectionConnectionManager(this,true));
		getComClusterManager().start();
		
		//comunicazione col CLUSTER HEAD
		//Il big boss non comunica col server e quindi non serve un connection Manager col server
		//Ogni relay di ogni cluster comunica col big boss, ovvero il relay del cluster head
		if(isBIGBOSS() || isRELAY()){
			setComClusterHeadManager(RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true));
			getComClusterHeadManager().start();
		}
//		if(isRELAY()){
//			setComClusterHeadManager(RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true));
//			getComClusterHeadManager().start();
//		}

		//Se parto come Relay BIG BOSS
		//WIFI Managed deve essere associata al AP e quindi connessa
		//WIFI Ad-Hoc deve solo essere configurata correttamente ma non essere necessariamente associata in quanto potrebbe essere l unico nodo in quell istante
		if(isBIGBOSS()){ 
			try {
				if(getRelayClusterWNICController().isOn() && getRelayClusterHeadWNICController().isConnected()) 
					becomeBigBossRelay(0);
				else{
					debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"Questo nodo non può essere il BigBoss dato che o non vede l'AP o non è collegato alla rete Ad Hoc.");
					throw new Exception("RelayElectionManager ERRORE: Questo nodo non può essere il BigBoss dato che o non vede l'AP o non è collegato alla rete Ad Hoc.");
				}
			} catch (WNICException e) {
				debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}

		//Parto da relay secondario o possibile relay secondario o possibile BigBoss
		else if(isRELAY()||isPOSSIBLE_RELAY()||isPOSSIBLE_BIGBOSS()){
			try{
				if(getRelayClusterWNICController().isOn() && getRelayClusterHeadWNICController().isConnected())
					if(isRELAY())
						searchingRelayClusterHead();
					else if(isPOSSIBLE_RELAY()||isPOSSIBLE_BIGBOSS())
						becomePossibleRelay();
				else{
					debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"Questo nodo non può essere il Relay o possibile sostituto dato che o non è interfacciato alla rete CLUSTER HEAD o alla rete CLUSTER.");
					throw new Exception("RelayElectionManager ERRORE: Questo nodo non può essere il Relay o possibile sostituto dato che o non è interfacciato alla rete CLUSTER HEAD o alla rete CLUSTER.");
				}
					
			} catch (WNICException e) {
				debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
				throw new Exception("RelayElectionManager ERRORE: Problemi con il RelayWNICController: "+ e.getStackTrace());
			} 
		}
			
		
		//parto come cliente normale  (relay o bigboss passivo)
//		else{
//			searchingRelay();
//		}
	}

	
	/**Metodo che consente di far si che questo nodo diventi il Relay BIGBOSS attuale,
	 * fa partire il RelayPositionAPMonitor, il RelayPositionClientsMonitor,
	 * il RelayBatteryMonitor e il WhoIsRelayServer. Poi passa allo stato di MONITORING.
	 */
	private void becomeBigBossRelay(int state){
		setNodeType(0, true);
		setFirstELECTION_REQUEST(false);
		if(state==0){
			setFirstELECTION_DONE(false);
			setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
			memorizeLocalClusterAddress();
			setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
			memorizelocalClusterHeadAddress();
			setConnectedClusterHeadAddress(NetConfiguration.SERVER_ADDRESS);
		}
		
		if(state == 1){
			setComClusterHeadManager(RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true));
			getComClusterHeadManager().start();
		}
		
		memorizeConnectedClusterHeadAddress();

		//Azzero tutti i timeout
		cancelTimeoutSearch();
		cancelTimeoutToElect();
		cancelTimeoutElectionBeacon();
		cancelTimeoutFailToElect();

		try {
			//Monitoraggio RSSI nei confronti del server
			setRelayPositionAPMonitor(new RelayPositionAPMonitor(
											getRelayClusterHeadWNICController(),	
											TimeOutConfiguration.POSITION_AP_MONITOR_PERIOD,
											this));

			//Monitoraggio RSSI client e relay attivi
			setRelayPositionMonitor(new RelayPositionMonitor(
											ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
											TimeOutConfiguration.POSITION_CLIENTS_MONITOR_PERIOD,
											this,
											getRelayClusterWNICController().getDebugConsole()));
			
			if(state==1)getRelayPositionMonitor().start();
			
			getRelayPositionAPMonitor().start();
			
			//Monitoraggio della batteria
			setRelayBatteryMonitor(new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this));
			//getRelayBatteryMonitor().start();
			
			
			//Risponde ai messaggi WHO_IS_RELAY
			setWhoIsRelayServer( new WhoIsRelayServer(getConsoleClusterWifiInterface(),getConnectedClusterHeadAddress()));
			getWhoIsRelayServer().start();
			
			if(getRelaySessionManager()==null)
				setRelaySessionManager(RelaySessionManager.getInstance());
			
			getRelaySessionManager().setElectionManager(this);
			this.addObserver((Observer) getRelaySessionManager());
			
				
			if(state==0){
				setActualStatus(RelayStatus.IDLE);
				debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": APMonitoring e WhoIsRelayServer partiti");
			}
			else if(state==1){
				setActualStatus(RelayStatus.MONITORING);
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": RSSIMonitoring partiti, WhoIsRelay partiti, BIGBOSS sostituito");
			}
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}
	
	/**Metodo che consente di far si che questo nodo diventi un relay secondario attivo,
	 * fa partire il RelayPositionController (servizio che risponde ai messggi RSSI_REQUEST),
	 * il RelayBatteryMonitor, WhoIsRelayServer. Poi passa allo stato di IDLE/MONITORING. */
	private void becomRelay(int state){
		setNodeType(1,true);
		setFirstELECTION_REQUEST(false);
		if(state==0){
			setFirstELECTION_DONE(false);
			setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
			memorizeLocalClusterAddress();
			setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
			memorizeLocalClusterAddress();
		}
		if(state==1){
			setComClusterHeadManager(RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true));
			getComClusterHeadManager().start();
		}
		memorizeConnectedClusterHeadAddress();
		
		//Azzero tutti i timeout
		cancelTimeoutSearch();
		cancelTimeoutToElect();
		cancelTimeoutElectionBeacon();
		cancelTimeoutFailToElect();
		try{
			//Monitoraggio RSSI client
			if(getRelayPositionMonitor()==null){
				setRelayPositionMonitor(new RelayPositionMonitor(
					ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL,
					TimeOutConfiguration.POSITION_CLIENTS_MONITOR_PERIOD,
					this,
					getRelayClusterWNICController().getDebugConsole()));
			}
		
			//risponde ai messaggi di RSSI_REQUEST prendendo il valore RSSI dalla scheda wifi collegata al cluster head
			if(getRelayPositionController()==null){
				setRelayPositionController(new RelayPositionController(getRelayClusterHeadWNICController(),this));
			}
			getRelayPositionController().start();
		
		
			//Client -> faccio partire nel momento in cui si collega qualche client...
			if(getRelayBatteryMonitor()==null)
				setRelayBatteryMonitor(new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this));
			//getRelayBatteryMonitor().start();
			
			if(getWhoIsRelayServer()==null)
				setWhoIsRelayServer(new WhoIsRelayServer(getConsoleClusterWifiInterface(),getConnectedClusterHeadAddress()));
			getWhoIsRelayServer().start();
			
			if(getRelaySessionManager()==null){
					setRelaySessionManager(RelaySessionManager.getInstance() );
			
					getRelaySessionManager().setElectionManager(this);
					this.addObserver((Observer) getRelaySessionManager());
			}
				
			if(state==0)setActualStatus(RelayStatus.IDLE);
			
			if(state==1){
				setActualStatus(RelayStatus.MONITORING);
				if(getComClusterHeadManager()==null){
					setComClusterHeadManager(RelayConnectionFactory.getClusterHeadElectionConnectionManager(this,true));
					getComClusterHeadManager().start();
				}
			}
		}catch(WNICException e){e.printStackTrace();System.exit(2);}
		
		//MANDO ACK al BIGBOSS per notificare che mi sono collegato a lui
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildAckConnection(getConnectedClusterHeadInetAddress(), PortConfiguration.PORT_ELECTION_IN, MessageCodeConfiguration.TYPERELAY);
			getComClusterHeadManager().sendTo(dpOut);
			debug(getConsoleClusterHeadWifiInterface(),DebugConfiguration.DEBUG_INFO,"RelayElectionManger: Stato."+getActualStatus()+" sono diventato il NEW RELAY e ACK sepdito a "+getConnectedClusterHeadAddress()+":"+PortConfiguration.PORT_ELECTION_IN);
		} catch (IOException e) {debug(getConsoleClusterHeadWifiInterface(), DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");e.getStackTrace();}
	
		if(state==0){
			debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": WhoIsRelayServer partito, ACK_CONNECTION spedito");
		}
		if(state==1){
			startMonitoringRSSI();
			debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": ho sostituito il vecchio relay e Monitoraggio RSSI partito");
		}
	}
	
	/**Metodo che consente di far si che questo nodo diventi un possibile sostituto del nodo relay*/
	private void becomePossibleRelay(){
		if(isPOSSIBLE_BIGBOSS())setNodeType(0,false);
		if(isPOSSIBLE_RELAY())setNodeType(1,false);
		setFirstELECTION_REQUEST(false);
		
		setDbBeaconStore(new Vector<DatagramPacket>());
		
		setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		memorizeLocalClusterAddress();
		setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
		memorizeLocalClusterAddress();
		if(isPOSSIBLE_BIGBOSS()){
			if(getRelaySessionManager()==null){
				setRelaySessionManager(RelaySessionManager.getInstance() );
		
				getRelaySessionManager().setElectionManager(this);
				this.addObserver((Observer) getRelaySessionManager());
		}
		}
		
		//memorizeConnectedClusterHeadAddress();

		//cancelTimeoutSearch();
	
		setActualStatus(RelayStatus.IDLE);
		debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": creato nodo possibile sostituto.");
	}
	
	/**Metodo che consente al relay di trovare il Big Boss, ovvero il relay del cluster head.*/
	private void searchingRelayClusterHead(){
		DatagramPacket dpOut = null;
		try {
			dpOut = RelayMessageFactory.buildWhoIsRelay(BCASTHEAD,PortConfiguration.WHO_IS_RELAY_PORT_IN);
			getComClusterHeadManager().sendTo(dpOut);
		} catch (IOException e) {debug(getConsoleClusterHeadWifiInterface(), DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
		setTimeoutSearch(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_SEARCH,TimeOutConfiguration.TIME_OUT_SEARCH));
		setActualStatus(RelayStatus.WAITING_WHO_IS_RELAY);
		debug(getConsoleClusterHeadWifiInterface(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
	}
	
	/**Metodo che consente ad un nodo possibile relay di trovare relay attivo all'interno del CLUSTER in cui si trova.*/
//	private void searchingRelayCluster(){
//		DatagramPacket dpOut = null;
//		try {
//			dpOut = RelayMessageFactory.buildWhoIsRelay(BCAST,PortConfiguration.WHO_IS_RELAY_PORT_IN);
//			getComClusterManager().sendTo(dpOut);
//		} catch (IOException e) {debug(getConsoleClusterWifiInterface(), DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di WHO_IS_RELAY");e.getStackTrace();}
//		setTimeoutSearch(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_SEARCH,TimeOutConfiguration.TIME_OUT_SEARCH));
//		setActualStatus(RelayStatus.WAITING_WHO_IS_RELAY);
//		debug(getConsoleClusterWifiInterface(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": inviato WHO_IS_RELAY e start del TIMEOUT_SEARCH");
//	}
	
	
	//@Override
	public void update(Observable arg0, Object arg1) {
		
		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){
			DatagramPacket dpIn = (DatagramPacket)arg1;
			setRelayMessageReader(new RelayMessageReader());

			try {
				getRelayMessageReader().readContent(dpIn);
			} catch (IOException e) {e.printStackTrace();}
			
			
			/** IM_RELAY
			 *  WAITING_WHO_IS_RELAY
			 *  Risposta da parte del nodo relay attivo del CLUSTER di riferimento*/
			debug(getConsoleElectionManager(),3,"Arrivato messaggio :"+getRelayMessageReader().getCode()+" e stato:"+getActualStatus());
			if((getRelayMessageReader().getCode() == MessageCodeConfiguration.IM_RELAY) && 
			   (getActualStatus() == RelayStatus.WAITING_WHO_IS_RELAY)){
				
				//Relay secondario attivo in cerca del big boss
				if(isRELAY()){
					setConnectedClusterHeadAddress(getRelayMessageReader().getPacketAddess().getHostAddress());
					
					setChanged();
					notifyObservers("RELAY_FOUND:"+getConnectedClusterHeadAddress());
				
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": IM_RELAY arrivato, connectHeadRelayAddress: "+getConnectedClusterHeadAddress());
					becomRelay(0);
				}
				//else if(isCLIENT()){}
//				else if(isPOSSIBLE_RELAY()|| isPOSSIBLE_BIGBOSS()){
//					setConnectedClusterHeadAddress(getRelayMessageReader().getPacketAddess().getHostAddress());
//					
//					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" IM_RELAY arrivato clusterRelay:"+relayMessageReader.getPacketAddess()+" clusterHead:"+getConnectedClusterHeadAddress());
//					becomePossibleRelay();
//				}
			}
			
			/** ACK_CONNECTION
			 * 	Conferma da parte di un nodo della connessione al nodo corrente
			 */
			if((getRelayMessageReader().getCode()==MessageCodeConfiguration.ACK_CONNECTION) &&
					(!sameAddress(getRelayMessageReader().getPacketAddess()))){
				if(getRelayMessageReader().getTypeNode()==MessageCodeConfiguration.TYPERELAY){
					addRelay();
					setChanged();
					notifyObservers("NEW_CONNECTED_RELAY:"+getRelayMessageReader().getPacketAddess().toString());
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": nuovo relay secondario connesso -> ip :"+getRelayMessageReader().getPacketAddess().toString());
				}
				else if (relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPECLIENT){
					addClient();
					setChanged();
					notifyObservers("NEW_CONNECTED_CLIENT:"+getRelayMessageReader().getPacketAddess().toString());
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": nuovo client connesso -> ip :"+getRelayMessageReader().getPacketAddess().toString());
				}
//				else if(relayMessageReader.getTypeNode()==MessageCodeConfiguration.TYPERELAYPASSIVE){
//					if(consoleElectionManager!=null)consoleElectionManager.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayElectionManager: nuovo possibile relay sostituto connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
//					else System.out.println("RelayElectionManager: nuovo possibile relay sostituto connesso -> ip :"+relayMessageReader.getPacketAddess().toString());
//				}
				
			}
			
		

			/**	ELECTION_REQUEST
			 *  Un nodo Relay attivo sta uscendo dal proprio cluster -> in cerca di un nuovo sostituto
			 */
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_REQUEST) && 
					(!sameAddress(getRelayMessageReader().getPacketAddess())) &&
					(!getFirstELECTION_REQUEST())){
					
				setFirstELECTION_REQUEST(true);
				setFirstELECTION_DONE(false);
					
				setStartElection(System.currentTimeMillis());
				setNumberOfNode(0);
					

				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "Arrivato ELECTION_REQUEST");
				//rielezione nuovo nodo relay BigBoss/Relay secondario e sono un nodo possibile sostituto
				//un nodo possibile sostituti sta in ascolto solo dei messaggi broadcast emessi sulla rete (CLUSTER) in cui ne fa parte
				if((isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())&& (getActualStatus()==RelayStatus.IDLE)){
					try {
						
						if(isPOSSIBLE_BIGBOSS())debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_REQUEST arrivato da:"+getRelayMessageReader().getPacketAddess()+", sostituto BIGBOSS, AP visibility:"+getRelayClusterHeadWNICController().isConnected());
						if(isPOSSIBLE_RELAY())debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_REQUEST arrivato, sostituto Relay secondario, BIGBOSS CLUSTER visibility:"+getRelayClusterHeadWNICController().isConnected());							
						
						setElecting(true);
						setConnectedClusterHeadAddress(getRelayMessageReader().getPacketAddess().getHostAddress());
						memorizeConnectedClusterHeadAddress();
						
						clearClientVisibility();
						clearW();
						
						//AP/BIGBOSS CLUSTER Visibility == true
						if(getRelayClusterHeadWNICController().isConnected()){
							if(!test){
								setTimeoutElectionBeacon(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_ELECTION_BEACON, TimeOutConfiguration.TIME_OUT_ELECTION_BEACON));
								setTimeoutFailToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT, TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
							}

							setActualStatus(RelayStatus.WAITING_BEACON);
							
							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": Ap/BigBoss Visibility == true, TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti");
						} 			

					} catch (WNICException e) {e.printStackTrace();}
					
					setChanged();
					notifyObservers("ELECTION_REQUEST_RECEIVED");
				}
				
				//Nodo BIGBOSS ATTIVO: vengo iformato che è in corso una elezione di un relay secondario
				else if((isBIGBOSS()) && 
						(getActualStatus()==RelayStatus.MONITORING)){
					removeRelay();
					setFirstELECTION_REQUEST(true);
					setFirstELECTION_DONE(false);
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": relay secondario IP:"+getRelayMessageReader().getPacketAddess()+" è in cerca di un suo sostituto. removeRelay()");
					setRelayToElect(getRelayMessageReader().getPacketAddess().getHostAddress());					
					if(!test)
					setTimeoutFailToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT, TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
					//QUI DEVO INFORMARE IL SESSION MANAGER
				}
				
				//Nodo RELAY SECONDARIO ATTIVO: viene eletto un nuovo BigBoss
				else if((isRELAY()) && 
						(getActualStatus() == RelayStatus.MONITORING)){
					setElectingHead(true);
					setConnectedClusterHeadAddress(null);
					setConnectedClusterHeadInetAddress(null);
					setFirstELECTION_REQUEST(true);
					setFirstELECTION_DONE(false);
				
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": ELECTION_REQUEST arrivato da (BIGBOSS) IP:"+getRelayMessageReader().getPacketAddess());
					
					//mando il messaggio di ELECTION_BEACON_RELAY ai possibili nodi sostituti		
					DatagramPacket dpOut = null;

					try {
						//Messaggio destinato ai possibili sostituti
						dpOut = RelayMessageFactory.buildElectioBeacon(0, BCASTHEAD,PortConfiguration.PORT_ELECTION_IN, getActiveClient());
						getComClusterHeadManager().sendTo(dpOut);
						
					} catch (IOException e){e.printStackTrace();}
					
					if(!test)
						setTimeoutFailToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT,TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
					setActualStatus(RelayStatus.WAITING_END_NORMAL_ELECTION);
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_BEACON_RELAY inviato e start TIMEOUT_FAIL_TO_ELECT");
				}
			}
			
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_REQUEST) && 
					(!sameAddress(getRelayMessageReader().getPacketAddess())) &&
					(getFirstELECTION_REQUEST())){
				setConnectedClusterHeadAddress(getRelayMessageReader().getPacketAddess().getHostAddress());
				memorizeConnectedClusterHeadAddress();
			}
			
			/** ELECTION_BEACON_RELAY
			 *  WAITING_BEACON
			 *  solo nodo possibile sostituti
			 */
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_BEACON) &&
					(!sameAddress(getRelayMessageReader().getPacketAddess()))){
				
				setFirstELECTION_REQUEST(true);
				setFirstELECTION_DONE(false);
				
				//Significa che ho ricevuto l'election request
				if((isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())&&
						 getActualStatus() == RelayStatus.WAITING_BEACON){
					addClientVisibility(getRelayMessageReader().getActiveClient());
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_BEACON_RELAY arrivato, client_visibility = "+getClientVisibility());
					
					if(test){
						setNumberOfNode(getNumberOfNode()+1);
					
						if(getNumberOfNode()==NODE){
							
							//Stato instabile WeightCalculation
							weightCalculation();

							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"W calcolato:"+getW());
							
							DatagramPacket dpOut = null;

							try {
								dpOut = RelayMessageFactory.buildElectionResponse(0, getW(), getConnectedClusterHeadInetAddress(), PortConfiguration.PORT_ELECTION_IN);
								getComClusterManager().sendTo(dpOut);
								debug(getConsoleClusterWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ECTION_RESPONSE inviato a "+getConnectedClusterHeadAddress());
							} catch (IOException e) {e.printStackTrace();}

							setActualStatus(RelayStatus.WAITING_END_NORMAL_ELECTION);
							
							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": attesa che ricevo risposta (ELECTIONE_DONE) dal nodo da sosituire ");
						}
					}
				}
				
				//non ho ricevuto l election request ancora
				else if((isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY())&&
						 getActualStatus() == RelayStatus.IDLE){		

						
					setStartElection(System.currentTimeMillis());
					setNumberOfNode(0);
					
					try {
						
						if(isPOSSIBLE_BIGBOSS())debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_REQUEST arrivato da:"+getRelayMessageReader().getPacketAddess()+", sostituto BIGBOSS, AP visibility:"+getRelayClusterHeadWNICController().isConnected());
						if(isPOSSIBLE_RELAY())debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_REQUEST arrivato, sostituto Relay secondario, BIGBOSS CLUSTER visibility:"+getRelayClusterHeadWNICController().isConnected());							
						
						setElecting(true);
						setConnectedClusterHeadAddress(getRelayMessageReader().getPacketAddess().getHostAddress());
						memorizeConnectedClusterHeadAddress();
						
						clearClientVisibility();
						clearW();
						
						//AP/BIGBOSS CLUSTER Visibility == true
						if(getRelayClusterHeadWNICController().isConnected()){
							if(!test){
								setTimeoutElectionBeacon(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_ELECTION_BEACON, TimeOutConfiguration.TIME_OUT_ELECTION_BEACON));
								setTimeoutFailToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT, TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
							}

							setActualStatus(RelayStatus.WAITING_BEACON);
							addClientVisibility(getRelayMessageReader().getActiveClient());
							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": Ap/BigBoss Visibility == true, TIMEOUT_ELECTION_BEACON, TIMEOUT_FAIL_TO_ELECT partiti");
							
							if(test){
								setNumberOfNode(getNumberOfNode()+1);
								
								if(getNumberOfNode()==NODE){
										
									//Stato instabile WeightCalculation
									weightCalculation();

									debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"W calcolato:"+getW());
										
									DatagramPacket dpOut = null;

									try {
										dpOut = RelayMessageFactory.buildElectionResponse(0, getW(), getConnectedClusterHeadInetAddress(), PortConfiguration.PORT_ELECTION_IN);
										getComClusterManager().sendTo(dpOut);
										debug(getConsoleClusterWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ECTION_RESPONSE inviato a "+getConnectedClusterHeadAddress());
									} catch (IOException e) {e.printStackTrace();}

									setActualStatus(RelayStatus.WAITING_END_NORMAL_ELECTION);
										
									debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": attesa che ricevo risposta (ELECTIONE_DONE) dal nodo da sosituire ");
								}
							}
						} 			

					} catch (WNICException e) {e.printStackTrace();}
					
					setChanged();
					notifyObservers("ELECTION_REQUEST_RECEIVED");
				}
				
				else if((isRELAY()) && 
						(getActualStatus() == RelayStatus.MONITORING)){
					setElectingHead(true);
					setConnectedClusterHeadAddress(null);
					setConnectedClusterHeadInetAddress(null);

				
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": ELECTION_REQUEST arrivato da (BIGBOSS) IP:"+getRelayMessageReader().getPacketAddess());
					
					//mando il messaggio di ELECTION_BEACON_RELAY ai possibili nodi sostituti		
					DatagramPacket dpOut = null;

					try {
						//Messaggio destinato ai possibili sostituti
						dpOut = RelayMessageFactory.buildElectioBeacon(0, BCASTHEAD,PortConfiguration.PORT_ELECTION_IN, getActiveClient());
						getComClusterHeadManager().sendTo(dpOut);
						
					} catch (IOException e){e.printStackTrace();}
					
					if(!test)
						setTimeoutFailToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT,TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
					
					setActualStatus(RelayStatus.WAITING_END_NORMAL_ELECTION);
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+" ELECTION_BEACON_RELAY inviato e start TIMEOUT_FAIL_TO_ELECT");
				}
			}
			
			
			/** ELECTION_RESPONSE
			 *  WAITING_RESPONSE
			 *  nodo da sostituire riceve risposta da parte dei nodi possibili sostituti
			 */
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_RESPONSE) &&
					(getActualStatus()==RelayStatus.WAITING_RESPONSE)
					&& (!sameAddress(getRelayMessageReader().getPacketAddess()))){

				getPossibleRelay().add(new Couple(getRelayMessageReader().getPacketAddess().getHostAddress(),getRelayMessageReader().getW()));

				Collections.sort(getPossibleRelay());

				setBestSubstituteRelayAddress(getPossibleRelay().get(0).getAddress());
				memorizeBestSubstituteRelayAddress();
				setNumberOfPossibleRelay(getNumberOfPossibleRelay()+1);
				if(test){
					if(getNumberOfPossibleRelay() ==NUMBERPOSSIBLERELAY){
						setElecting(false);
						
						debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": elezione finita, scelgo un nodo sostituto se ce ne sono.");

						//Ho trovato nodi possibili sostituti
						if (!possibleRelay.isEmpty()) {

							DatagramPacket dpOut = null;

							try {
								//invio ai Relay
								dpOut = RelayMessageFactory.buildElectionDone(	0, getBestSubstituteRelayAddress(),getLocalClusterAddress(),getLocalClusterHeadAddress(),getConnectedClusterHeadAddress(),BCAST, PortConfiguration.PORT_ELECTION_IN);
								getComClusterManager().sendTo(dpOut);
							
								dpOut = RelayMessageFactory.buildElectionDone(	0, getBestSubstituteRelayAddress(),getLocalClusterAddress(),getLocalClusterHeadAddress(),getConnectedClusterHeadAddress(),BCASTHEAD, PortConfiguration.PORT_ELECTION_IN);
								getComClusterHeadManager().sendTo(dpOut);
								//firstELECTION_DONEsent = true;
							} catch (IOException e) {e.printStackTrace();}

							//faccio sapere al SESSIONMANAGER chi ho appena eletto
						
							setStopElection(System.currentTimeMillis());
							debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"Durata elezione fino alla notifica al Session Manager : "+ (getStopElection()-getStartElection())+" ms");
							setChanged();
							notifyObservers("NEW_RELAY:" + getBestSubstituteRelayAddress()+":"+getLocalClusterAddress()+":"+getLocalClusterHeadAddress()+":"+getConnectedClusterHeadAddress());
						
							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE + "+bestSubstituteRelayAddress+" inviato e passo allo stato di IDLE");
						
						}
						else{
							debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "RelayElectionManager STATO:"+actualStatus+" non ci sono nodi sostituti --> FASE DI EMERGENZA, questo nodo nn ne fa parte");
							//FASE DI EMERGENZA
						}
						getComClusterHeadManager().close();
						setComClusterHeadManager(null);
						setActualStatus(RelayStatus.IDLE);
						if(isBIGBOSS())setNodeType(0,false);
						if(isRELAY())setNodeType(1,false);
						
					}
				}else
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_RESPONSE arrivato -> miglior nodo giunto " + getBestSubstituteRelayAddress() + " con peso: " + getPossibleRelay().get(0).getWeight());
			}
			
			/** ELECTION_DONE
			 *  WAITING_END_NORMAL_ELECTION
			 *  risposta da parte del nodo che è stato sostituito con allegato l indirizzo del nuovo relay eletto
			 */
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_DONE) && 
					//(getActualStatus() == RelayStatus.WAITING_END_NORMAL_ELECTION) &&
					(!sameAddress(getRelayMessageReader().getPacketAddess())) &&
					(!getFirstELECTION_DONE())){
				
				setNewRelayLocalClusterAddress(getRelayMessageReader().getNewRelayLocalClusterAddress());
				setOldRelayLocalClusterAddress(getRelayMessageReader().getOldRelayLocalClusterAddress());
				setOldRelayLocalClusterHeadAddress(getRelayMessageReader().getOldRelayLocalClusterHeadAddress());
				setHeadNodeAddress(getRelayMessageReader().getHeadNodeAddress());
				
				setFirstELECTION_DONE(true);
				setFirstELECTION_REQUEST(false);
				
				cancelTimeoutFailToElect();
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_DONE arrivato: nuovo Relay: "+getRelayMessageReader().getNewRelayLocalClusterAddress());
			
				
				//caso in cui viene eletto un nuovo BIGBOSS devo ricollegare il relay attivo
				if(isRELAY() && (getActualStatus() == RelayStatus.WAITING_END_NORMAL_ELECTION) ){
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Nodo corrente è un relay secondario attivo ed è stato appena eletto un nuovo BIGBOSS, lo memorizzo.");
										
					setConnectedClusterHeadAddress(getRelayMessageReader().getNewRelayLocalClusterAddress());
					memorizeConnectedClusterHeadAddress();
					
					setElectingHead(false);
					
					if(getActiveClient()>0)setActualStatus(RelayStatus.MONITORING);
					else setActualStatus(RelayStatus.IDLE);
					
					
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+" nuovo Relay BigBoss salvato correttamente"); 
										
				}
				else if(isPOSSIBLE_RELAY()|| isPOSSIBLE_BIGBOSS()){
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Nodo corrente è un possibile sostituto BIGBOSS/RELAY ATTIVO, controllo se è questo nodo eletto");
					setElecting(false);
					try {
						if(sameAddress(InetAddress.getByName(getRelayMessageReader().getNewRelayLocalClusterAddress()))){
							setConnectedClusterHeadAddress(getRelayMessageReader().getHeadNodeAddress());
							if(isPOSSIBLE_RELAY()){
								debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "Stato."+getActualStatus()+": ... DIVENTO RELAY ATTIVO..");
								becomRelay(1);
							}
							else if(isPOSSIBLE_BIGBOSS()){
								
								debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "Stato."+getActualStatus()+": ... DIVENTO BIGBOSS..");
								becomeBigBossRelay(1);
							}
							/** la conferma lo manda il SESSION_MANAGER con il messaggio di REQUEST_SESSION**/
						}
						else{
							setActualStatus(RelayStatus.IDLE);
							if(isPOSSIBLE_RELAY()){
								setNodeType(1, false);
								debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "Stato."+getActualStatus()+": ...NON SONO STATO SCELTO COME RELAY ATTIVO..");
							}
							else if(isPOSSIBLE_BIGBOSS()){
								setNodeType(0, false);
								debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "Stato."+getActualStatus()+": ...NON SONO STATO SCELTO COME BIGBOSS..");
							}
						}
					} catch (UnknownHostException e) {e.printStackTrace();}
						
				}
				
				
				setStopElection(System.currentTimeMillis());
				debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"Durata elezione fino alla notifica al Session Manager : "+ (getStopElection()-getStartElection())+" ms");
				setChanged();
				notifyObservers("NEW_RELAY:"+getNewRelayLocalClusterAddress()+":"+getOldRelayLocalClusterAddress()+":"+getOldRelayLocalClusterHeadAddress()+":"+getHeadNodeAddress());
					
				//propagazione del messaggio ELECTION_DONE
				getComClusterManager().sendTo(prepareRepropagationCluster(dpIn));
				//Se si tratta di una elezione a livello cluster faccio partire una propagazione al livello cluster head per essere sicuro che gli arriva il messaggio al bigboss
				if(isRELAY()){
					getComClusterHeadManager().sendTo(prepareRepropagationClusterHead(dpIn));
					debug(getConsoleClusterWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": Propagazione messaggio ELECTION_DONE sul CLUSTER HEAD inviato....");
				}
			}
			
			//ho rieletto un nuovo relay secondario ed è arrivato il messaggio al bigboss corrente
			else if((getRelayMessageReader().getCode() == MessageCodeConfiguration.ELECTION_DONE) && 
					(isBIGBOSS())&&
					(getActualStatus()==RelayStatus.MONITORING) &&
					(!getFirstELECTION_DONE())){
				try {
					if(!sameAddress(InetAddress.getByName(getRelayMessageReader().getNewRelayLocalClusterAddress())))
						setChanged();
					
					setStopElection(System.currentTimeMillis());
					debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"Durata elezione fino alla notifica al Session Manager : "+ (getStopElection()-getStartElection())+" ms");
					notifyObservers("NEW_RELAY:"+getRelayMessageReader().getNewRelayLocalClusterAddress()+":"+getRelayMessageReader().getOldRelayLocalClusterAddress()+":"+getRelayMessageReader().getOldRelayLocalClusterHeadAddress()+":"+getRelayMessageReader().getHeadNodeAddress());
					debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": è stato eletto un nuovo relay secondario:\nvecchio relay:"+getRelayMessageReader().getOldRelayLocalClusterHeadAddress()+"\nnuovo relay manderà un messaggio di ACK_CONNECTION");
					setFirstELECTION_DONE(true);
					setFirstELECTION_REQUEST(false);
				} catch (UnknownHostException e) {e.printStackTrace();}
			
			}
					
		}
		
		if(arg1 instanceof String){

			setEvent((String) arg1);
			
			/** TIMEOUT_SEARCH
			 *  WAITING_WHO_IS_RELAY
			 *  nodo relay secondario in cerca del BigBoss
			 */
			if((getEvent().equals("TIMEOUTSEARCH")) &&
				getActualStatus() == RelayStatus.WAITING_WHO_IS_RELAY){
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": TIMEOUT_SEARCH scattato");
				searchingRelayClusterHead();
			}
			
			/** DISCONNECTION_WARNING
			 * 	MONITORING
			 * 	il nodo corrente è attivo e se ne sta andando
			 */
			else if((getEvent().equals("DISCONNECTION_WARNING")) && 
					(getActualStatus() == RelayStatus.MONITORING)){

				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": DISCONNECTION_WARNING sollevato, chiudo tutti i monitor e spedisco il messaggio di ELECTION_REQUEST");
				setW(-1);
				setNumberOfPossibleRelay(0);

				setStartElection(System.currentTimeMillis());
				
				if(getWhoIsRelayServer()!=null)getWhoIsRelayServer().close();
				setWhoIsRelayServer(null);
				if(getRelayPositionAPMonitor()!=null)getRelayPositionAPMonitor().close();
				setRelayPositionAPMonitor(null);
				if(getRelayPositionMonitor()!=null)getRelayPositionMonitor().close();
				setRelayPositionMonitor(null);
				if(getRelayPositionController()!=null)getRelayPositionController().close();
				setRelayPositionController(null);
				//if(getRelayBatteryMonitor()!=null)ggetRelayBatteryMonitor().close();

				setPossibleRelay(new Vector<Couple>());

				DatagramPacket dpOut = null;

				try {
					//invio ai nodi collegato ad esso
					dpOut = RelayMessageFactory.buildElectionRequest(BCAST, PortConfiguration.PORT_ELECTION_IN);
					getComClusterManager().sendTo(dpOut);
					
					dpOut = RelayMessageFactory.buildElectionRequest(getConnectedClusterHeadInetAddress(), PortConfiguration.PORT_ELECTION_IN);
					getComClusterHeadManager().sendTo(dpOut);
					
				} catch (IOException e) {e.printStackTrace();}

				if(!test)
				setTimeoutToElect(RelayTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_TO_ELECT,TimeOutConfiguration.TIME_OUT_TO_ELECT));

				setElecting(true);

				setActualStatus(RelayStatus.WAITING_RESPONSE);
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_REQUEST inviato e TIMEOUT_TO_ELECT partito");
				
			}
			
			/** TIMEOUTELECTIONBEACON
			 * 	WAITING_BEACON
			 * 	il nodo corrente è un possibile sostituto,presume di aver ricevuto messaggi da ogni nodo coinvolto, calcola il proprio peso W 
			 */
			else if((getEvent().equals(TimeOutConfiguration.TIME_OUT_ELECTION_BEACON)) &&
					(isPOSSIBLE_BIGBOSS()||isPOSSIBLE_RELAY()) &&
					getActualStatus() == RelayStatus.WAITING_BEACON){
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+"; TIMEOUT_ELECTION_BEACON scattato, procedo al calcolo del W");
				
				//Stato instabile WeightCalculation
				weightCalculation();

				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"W calcolato:"+getW());
				
				DatagramPacket dpOut = null;

				try {
					dpOut = RelayMessageFactory.buildElectionResponse(0, getW(), getConnectedClusterHeadInetAddress(), PortConfiguration.PORT_ELECTION_IN);
					getComClusterManager().sendTo(dpOut);
					debug(getConsoleClusterWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ECTION_RESPONSE inviato a "+getConnectedClusterHeadAddress());
				} catch (IOException e) {e.printStackTrace();}

				setActualStatus(RelayStatus.WAITING_END_NORMAL_ELECTION);
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": attesa o che scatti il TIMEOUTFAILTOELECT o che ricevo risposta (ELECTIONE_DONE) dal nodo da sosituire ");
			}
			
			/** TIMEOUTTOELECT
			 * 	WAITING_RESPONSE
			 * 	il nodo corrente valuta i suoi possibili nodi sostituti e ne sceglie uno
			 */
			else if(getEvent().equals(TimeOutConfiguration.TIME_OUT_TO_ELECT)){
				
				//Sono il vecchio relay o il vecchio bigboss, rivecuto risposta da possibili nodi sostituti, eleggo quello migliore
				//se nn ce ne sono -->FASE di ERMERGENZA
				if(getActualStatus() == RelayStatus.WAITING_RESPONSE){
				
					setElecting(false);
				
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": TIMEOUT_TO_ELECT scattato, scelgo un nodo sostituto se ce ne sono.");

					//Ho trovato nodi possibili sostituti
					if (!possibleRelay.isEmpty()) {

						DatagramPacket dpOut = null;

						try {
							//invio ai Relay
							dpOut = RelayMessageFactory.buildElectionDone(	0, getBestSubstituteRelayAddress(),getLocalClusterAddress(),getLocalClusterHeadAddress(),getConnectedClusterHeadAddress(),BCAST, PortConfiguration.PORT_ELECTION_IN);
							getComClusterManager().sendTo(dpOut);
						
							dpOut = RelayMessageFactory.buildElectionDone(	0, getBestSubstituteRelayAddress(),getLocalClusterAddress(),getLocalClusterHeadAddress(),getConnectedClusterHeadAddress(),BCASTHEAD, PortConfiguration.PORT_ELECTION_IN);
							getComClusterHeadManager().sendTo(dpOut);
							//firstELECTION_DONEsent = true;
						} catch (IOException e) {e.printStackTrace();}

						//faccio sapere al SESSIONMANAGER chi ho appena eletto
					
						setChanged();
						notifyObservers("NEW_RELAY:" + getBestSubstituteRelayAddress()+":"+getLocalClusterAddress()+":"+getLocalClusterHeadAddress()+":"+getConnectedClusterHeadAddress());
					
						debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "RelayElectionManager STATO:"+actualStatus+" ELECTION_DONE + "+bestSubstituteRelayAddress+" inviato e passo allo stato di IDLE");
					
					}
					else{
						debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "RelayElectionManager STATO:"+actualStatus+" non ci sono nodi sostituti --> FASE DI EMERGENZA, questo nodo nn ne fa parte");
						//FASE DI EMERGENZA
					}
					getComClusterHeadManager().close();
					setComClusterHeadManager(null);
					setActualStatus(RelayStatus.IDLE);
					if(isBIGBOSS())setNodeType(0,false);
					if(isRELAY())setNodeType(1,false);
				}
				
				//Sono big boss, ed è appena fallito la rielezione di un nuovo relay secondario posso annullare tutti i flussi
				//del vecchio relay
				else if(getActualStatus() == RelayStatus.MONITORING){
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": Rielezione nuovo relay secondario fallita: IP veccio relay:"+getRelauToElect());
					
				}
				
				//Sono relay secondario ed è fallito la rielezione di un nuovo BIGBOSS annullo tutto e fase di emergenza
				else if(getActualStatus() == RelayStatus.WAITING_END_NORMAL_ELECTION){
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": Rielezione nuovo BIGBOSS fallita -->fase di EMERGENZA");
				}
			}
			else if(getEvent().equals(TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT)){
				if(isBIGBOSS() && getActualStatus()==RelayStatus.MONITORING){
					//è in corso un elezione di emergenza di un relay secondario...
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING, "RElayElectionManager Stato."+getActualStatus()+" TIME_OUT_FAIL_TO_ELECT scattato --> fase di emergenza per rieleggere un nuovo relay secondario. Vecchio relay IP:"+getRelauToElect());
				}
				else if(isRELAY() && getActualStatus()==RelayStatus.WAITING_END_NORMAL_ELECTION){
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "RelayElectionManager Stato."+getActualStatus()+" TIME_OUT_FAIL_TO_ELECT scattato --> in corso fase di emergenza per la rielezione di un nuovo BIGBOSS... tutto perso");
				}
				else if(isPOSSIBLE_BIGBOSS()|| isPOSSIBLE_RELAY())
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "RelayElectionManager Stato."+getActualStatus()+" TIME_OUT_FAIL_TO_ELECT scattato --> fase di emergenza");
			}
		}
	}
	
	public void startMonitoringRSSI(){
		if(getActualStatus()!=RelayStatus.MONITORING){
			
			//relayBatteryMonitor.start();
			setActualStatus(RelayStatus.MONITORING);
		}
//		if(getRelayPositionMonitor().isStopped()){
//			getRelayPositionMonitor().start();
//		}
		if(!getRelayPositionMonitor().isStarted())
			getRelayPositionMonitor().start();
			
	}
	public void stopMonitoringRSSI(){
		getRelayPositionMonitor().stop();
		//relayBatteryMonitor.stop();
		setActualStatus(RelayStatus.IDLE);
	}
	
	public void addClient(){
		setActiveClient(getActiveClient()+1);
		//da levare quando è finito in quanto è il session manager che deve farlo partire
		startMonitoringRSSI();
	}
	
	public void removeClient(){
		setActiveClient(getActiveClient()-1);
		if(getActiveClient()==0 && getActiveRelay()==0) 
			stopMonitoringRSSI();
	}
	
	public void addRelay(){
		setActiveRelay(getActiveRelay()+1);
		startMonitoringRSSI();
	}
	
	public void removeRelay(){
		setActiveRelay(getActiveRelay()-1);
		if(getActiveClient()==0 && getActiveRelay()==0) 
			stopMonitoringRSSI();
	}
	
	/**Metodo per calcolare il W tramite il WeightCalculation a cui passo il RelayWNICController 
	 * e il numero di clients rilevati 
	 */
	private void weightCalculation(){
		setW(WeightCalculator.calculateWeight(getRelayClusterHeadWNICController(),getClientVisibility()));
	}
	
	private boolean sameAddress(InetAddress adr){
		
		if(adr.equals(getLocalClusterInetAddress()))return true;
		if(adr.equals(getLocalClusterHeadInetAddress()))return true;
		else return false;
	}
	
	/**Metodo per costruire il pacchetto di ELECTION_DONE da ripropagare
	 * @param il DatagramPacket contenente il messaggio ELECTION_DONE da ripropagare
	 * @return il DatagramPacket pronto per essere inviato per ripropagare il messaggio ELECTION_DONE
	 */
	private DatagramPacket prepareRepropagationCluster(DatagramPacket dpIn){

		dpIn.setAddress(BCAST);
		dpIn.setPort(PortConfiguration.PORT_ELECTION_IN);
		return dpIn;
	}
	
	private DatagramPacket prepareRepropagationClusterHead(DatagramPacket dpIn){
		dpIn.setAddress(BCASTHEAD);
		dpIn.setPort(PortConfiguration.PORT_ELECTION_IN);
		return dpIn;
	}
		
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeLocalClusterAddress(){
		try {
			setLocalClusterInetAddress(InetAddress.getByName(getLocalClusterAddress()));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizeConnectedClusterHeadAddress(){
		try {
			setConnectedClusterHeadInetAddress(InetAddress.getByName(getConnectedClusterHeadAddress()));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String */
	private void memorizelocalClusterHeadAddress(){
		try {
			setLocalClusterHeadInetAddress(InetAddress.getByName(getLocalClusterHeadAddress()));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	private void memorizeBestSubstituteRelayAddress(){
		try {
			setBestSubstituteRelayInetAddress(InetAddress.getByName(getBestSubstituteRelayAddress()));
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
	
	private void setRelayPositionAPMonitor(RelayPositionAPMonitor relayPositionAPMonitor){this.relayPositionAPMonitor = relayPositionAPMonitor;}
	private RelayPositionAPMonitor getRelayPositionAPMonitor(){return relayPositionAPMonitor;}
	
	private void setRelayPositionMonitor(RelayPositionMonitor relayPositionMonitor){this.relayPositionMonitor = relayPositionMonitor;}
	private RelayPositionMonitor getRelayPositionMonitor(){return relayPositionMonitor;}
	
	private void setRelayPositionController(RelayPositionController relayPositionController){this.relayPositionController = relayPositionController;}
	private RelayPositionController getRelayPositionController(){return relayPositionController;}
	
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
	public InetAddress getBestSubstituteRelayInetAddress() {return bestSubstituteRelayInetAddress;}
	
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
	
	private void setActiveClient(int activeClient){this.activeClient = activeClient;}
	public int getActiveClient(){ return activeClient;}
	
	private void setActiveRelay(int activeRelay){this.activeRelay = activeRelay;}
	public int getActiveRelay(){ return activeRelay;}
	
	private void setElecting(boolean electing){this.electing=electing;}
	public boolean isElecting(){return electing;}
	
	private void setElectingHead(boolean electingHead){this.electingHead=electingHead;}
	public boolean isElectingHead(){return electingHead;}
	
	private void clearClientVisibility(){this.client_visibity=0;}
	private void addClientVisibility(int client){this.client_visibity+=client;}
	private int getClientVisibility(){return client_visibity;}
	
	private void clearW(){this.W=-1;}
	private void setW(double W){this.W=W;}
	private double getW(){return W;}
	
	private void setPossibleRelay(Vector<Couple> possibleRelay){this.possibleRelay = possibleRelay;}
	private Vector<Couple> getPossibleRelay(){return possibleRelay;}
	
	private void setFirstELECTION_DONE(boolean firstELECTION_DONE){this.firstELECTION_DONE = firstELECTION_DONE;}
	private boolean getFirstELECTION_DONE(){return firstELECTION_DONE;}
	
	private void setFirstELECTION_REQUEST(boolean firstELECTION_REQUEST){this.firstELECTION_REQUEST = firstELECTION_REQUEST;}
	private boolean getFirstELECTION_REQUEST(){return firstELECTION_REQUEST;}
	
	private void setEvent(String event){this.event=event;}
	private String getEvent(){return event;}
	
	private void setRelayToElect(String relayToElect){this.relayToElect = relayToElect;}
	private String getRelauToElect(){return relayToElect;}
	
	private void setRelaySessionManager(RelaySessionManager relaySessionManager){this.relaySessionManager = relaySessionManager;}	
	public RelaySessionManager getRelaySessionManager() {return relaySessionManager;}
	
	
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
	
	private void debug(DebugConsole console,int type, String message){
		if(console!=null)console.debugMessage(type, message);
		else{
			if(type==DebugConfiguration.DEBUG_INFO|| type ==DebugConfiguration.DEBUG_WARNING)
				System.out.println(message);
			if(type==DebugConfiguration.DEBUG_ERROR)
				System.err.println(message);
		}
	}
	
	public String getNewRelayLocalClusterAddress(){return newRelayLocalClusterAddress;}
	public void setNewRelayLocalClusterAddress(String newRelayLocalClusterAddress){this.newRelayLocalClusterAddress = newRelayLocalClusterAddress;}
	public String getHeadNodeAddress(){ return headNodeAddress;}
	public void setHeadNodeAddress(String headNodeAddress){this.headNodeAddress = headNodeAddress;}
	public String getOldRelayLocalClusterAddress(){return oldRelayLocalClusterAddress;}
	public void setOldRelayLocalClusterAddress(String oldRelayLocalClusterAddress){this.oldRelayLocalClusterAddress = oldRelayLocalClusterAddress;}
	public String getOldRelayLocalClusterHeadAddress(){return oldRelayLocalClusterHeadAddress;}
	public void setOldRelayLocalClusterHeadAddress(String oldRelayLocalClusterHeadAddress){this.oldRelayLocalClusterHeadAddress = oldRelayLocalClusterHeadAddress;}
	
	//per i test
	public long getStartElectionNoTimeOut() {return startElectionNoTimeOut;}
	public void setStartElectionNoTimeOut(long startElectionNoTimeOut) {this.startElectionNoTimeOut = startElectionNoTimeOut;}
	public long getStopElectionNoTimeOut() {return stopElectionNoTimeOut;}
	public void setStopElectionNoTimeOut(long stopElectionNoTimeOut) {this.stopElectionNoTimeOut = stopElectionNoTimeOut;}
	public long getIntermedio() {return intermedio;}
	public void setIntermedio(long intermedio) {this.intermedio = intermedio;}
	public long getStartElection() {return startElection;}
	public void setStartElection(long startElection) {this.startElection = startElection;}
	public long getStopElection() {return stopElection;}
	public void setStopElection(long stopElection) {this.stopElection = stopElection;}	
	public int getNumberOfNode() {return numberOfNode;}
	public void setNumberOfNode(int numberOfNode) {this.numberOfNode = numberOfNode;}
	public int getNumberOfPossibleRelay() {return numberOfPossibleRelay;}
	public void setNumberOfPossibleRelay(int numberOfPossibleRelay) {this.numberOfPossibleRelay = numberOfPossibleRelay;}
	public Vector<DatagramPacket> getDbBeaconStore() {return dbBeaconStore;}
	public void setDbBeaconStore(Vector<DatagramPacket> dbBeaconStore) {this.dbBeaconStore = dbBeaconStore;}
	
}
