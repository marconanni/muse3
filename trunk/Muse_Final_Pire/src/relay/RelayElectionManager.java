package relay;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;
import parameters.NetConfiguration;
import parameters.TimeOutConfiguration;

import relay.battery.RelayBatteryMonitor;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;

import relay.messages.RelayMessageReader;
import relay.position.RelayPositionAPMonitor;
import relay.position.RelayPositionMonitor;
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
	private static InetAddress BCAST = null;				//indirizzo broadcast in forma InetAddress
	
	private RelayCM comClusterManager = null;				//il manager per le comunicazioni nel proprio cluster
	private RelayCM comClusterHeadManager = null;			//il manager per le comunicazioni col cluster head	
	
	private RelayMessageReader relayMessageReader = null;				//il RelayMessageReader per leggere il contenuto dei messaggi ricevuti
	private RelayWNICController relayClusterWNICController = null;		//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster)
	private RelayWNICController relayClusterHeadWNICController = null;	//il RelayWNICController per ottenere informazioni dalla scheda di rete interfacciata alla rete AdHoc (cluster head)
	private RelayPositionAPMonitor relayPositionAPMonitor = null;		//il RelayPositionAPMonitor per conoscere la propria posizione nei confronti dell'AP
	private RelayPositionMonitor relayPositionMonitor = null;			//il RelayPositionMonitor per conoscere la propria posizione nei confronti dei client
	private RelayBatteryMonitor relayBatteryMonitor = null;				//il RelayBatteryMonitor per conoscere la situazione della propria batteria
	
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
			if((imBigBoss)||(NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS==NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS)){
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
					console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Questo nodo non può essere il BigBoss dato che non vede l'AP o non è collegato alla rete Ad Hoc");
					throw new Exception("RelayElectionManager: ERRORE: questo nodo non può essere il BigBoss dato che non vede l'AP o non è collegato alla rete Ad Hoc");
				}
			} catch (WNICException e) {
				console.debugMessage(DebugConfiguration.DEBUG_ERROR, "Problemi con il RelayWNICController: " + e.getStackTrace());
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

	private static void memorizeBCAST(){
		if(IMBIGBOSS){
			try {
				BCAST  = InetAddress.getByName(NetConfiguration.CLUSTER_HEAD_BROADCAST_ADDRESS);
			} catch (UnknownHostException e) {e.printStackTrace();}
		}else{
			try {
				BCAST  = InetAddress.getByName(NetConfiguration.CLUSTER_BROADCAST_ADDRESS);
			} catch (UnknownHostException e) {e.printStackTrace();}
		}
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
											this);
			
			
			//Monitoraggio della batteria
			relayBatteryMonitor = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,this);
			//Risponde ai messaggi WHO_IS_RELAY  (in caso di big boss anche ai messaggi WHO_IS_BIG_BOSS
			whoIsRelayServer = new WhoIsRelayServer(imBigBoss,console);
			
			relayPositionAPMonitor.start();
			relayPositionMonitor.start();
			relayPositionMonitor.startRSSIMonitor();
			whoIsRelayServer.start();
			//relayBatteryMonitor.start();
		
			actualStatus = RelayStatus.IDLE;
				
			console.debugMessage(Parameters.DEBUG_INFO, "RelayElectionManager.becomeBigBossRelay(): X -> STATO ["+actualStatus.toString()+"]: APMonitoring e WhoIsRelayServer partiti");
			
		} catch (WNICException e) {e.printStackTrace();System.exit(2);}
	}

	
	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		
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
	

	public static void setIMRELAY(boolean imrelay) {IMRELAY = imrelay;}
	public static boolean isIMRELAY() {return IMRELAY;}
	public static void setIMBIGBOSS(boolean imbigboss) {IMBIGBOSS = imbigboss;}
	public static boolean isIMBIGBOSS() {return IMBIGBOSS;}

	public static void setINSTANCE(RelayElectionManager iNSTANCE) {INSTANCE = iNSTANCE;}
	public static RelayElectionManager getINSTANCE() {return INSTANCE;}
	
	



}
