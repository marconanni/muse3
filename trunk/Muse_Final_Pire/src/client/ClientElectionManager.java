package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;
import parameters.TimeOutConfiguration;

import client.connection.ClientCM;
import client.connection.ClientConnectionFactory;
import client.messages.ClientMessageFactory;
import client.messages.ClientMessageReader;
import client.position.ClientPositionController;
import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutSingleWithMessage;
import client.wnic.ClientWNICController;
import client.wnic.WNICFinder;

import debug.DebugConsole;

/**
 * @author Leo Di Carlo, Luca Campeti e Pire Dejaco
 * @version 1.1
 *
 */

public class ClientElectionManager extends Observable implements Observer{

	private static InetAddress BCAST = null;					//indirizzo broadcast in forma InetAddress
	public String localAddress = null;						//indirizzo locale del client
	public InetAddress localInetAddress = null;				//indirizzo locale del client in forma InetAddress
	private String connectedRelayAddress = null;			//indirizzo del Relay a cui il client è attualmente collegato
	private InetAddress connectedRelayInetAddress = null;	//indirizzo del Relay a cui il client è attualmente collegato in forma InetAddress
	
	private ClientStatus actualStatus = null;				//stato attuale del ClientElectionManager
	
	private boolean electing = false;						//boolean che indica se si è in stato di elezione
	private boolean imServed = false;						//boolean che indica se questo nodo Client è coinvolto in una sessione RTP
	private boolean firstELECTION_DONE = false;
	private int indexEM_EL_DET_CLIENT = 0;					//indici dei messaggi inviati
	private int indexELECTION_BEACON = 0;
	
	private TimeOutSingleWithMessage timeoutSearch = null;				//vari Timeout necessari al ClientElectionManager
	private TimeOutSingleWithMessage timeoutFailToElect = null;	
	private boolean stop = false;
		
	private ClientCM comManager = null;									//il manaegr per le comunicazioni
	private ClientWNICController clientWNICController = null;			//il ClientWNICController per ottenere le informazioni dalla schede di rete interfacciata alla rete Ad-Hoc
	private ClientPositionController clientPositionController = null;	//Ascolta i messaggi di RSSIREQUEST e risponde col proprio valore RSSI rilevato nel confronto del relay a cui è collegato
	private ClientMessageReader clientMessageReader = null;				//il ClientMessageReader per leggere il contenuto dei messaggi ricevuti
	private static ClientElectionManager INSTANCE = null;					//istanza singleton del ClientElectionManager
	private DebugConsole consoleElectionManager = null;					//Serve per mostrare i messaggi di debug del CLientElectionManager
	private DebugConsole consoleWifiInterface = null;
	//private ClientFrameController frameController = null;				//ClientFrameController assegnabile al ClientElectionManager
	
	private String event = null;
	
	public enum ClientStatus {//stati in cui si può trovare il ClientElectionManager
		OFF,
		WAITING_WHO_IS_RELAY, 
		IDLE, 
		ACTIVE, 
		WAITING_END_ELECTION,
		EM_ELECTION}			
	
	static {
		try {
			BCAST = InetAddress.getByName(NetConfiguration.CLIENT_BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	

	/**
	 * Costruttore per ottenere un nuovo ClientElectionManager.
	 */
	public ClientElectionManager() {}
	

	/**Inizializzazione del ClientElectionManager.
	 * Fa partire il Connection Manager ed in seguito si mette alla ricerca di un Relay.
	 */
	public void init(){
		
		setActualStatus(ClientStatus.OFF);
		setLocalAddress(NetConfiguration.CLIENT_ADDRESS);
		memorizeLocalAddress();
		setComManager(ClientConnectionFactory.getElectionConnectionManager(this,true));
		getComManager().start();
	
		try {
			setClientWNICController(WNICFinder.getCurrentWNIC(NetConfiguration.NAME_OF_CLIENT_WIFI_INTERFACE, NetConfiguration.NAME_OF_CLIENT_NETWORK));
			getClientWNICController().setDebugConsole(getConsoleWifiInterface());
			getClientWNICController().init();
			setClientPositionController(new ClientPositionController(getClientWNICController()));
		} catch (Exception e){
			debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,e.getMessage());
			stop = true;}
		
		if(stop)
			debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR,"ClientElectionManager: creato ed entrato in STATO di:"+getActualStatus()+" e non può continuare in quanto la scheda di rete non è configurata correttamente");
		else{
			debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"ClientElectionManager: Scheda wireless configurata correttamente ora cerca il relay");
			searchingRelay();
		}
	}

	/**Metodo per ottenere l'istanza della classe singleton ClientElectionManager
	 * @return un riferimento al singleton ClientElectionManager
	 */
	public static ClientElectionManager getINSTANCE(){
		if(INSTANCE == null) INSTANCE = new ClientElectionManager();
		return INSTANCE;
	}
	
	/**Metodo che consente di mettersi alla ricerca di un Relay (se non si è in elezione, o se si è in attesa della fine di un'elezione di emergenza)
	 * inviando un WHO_IS_RELAY, facendo partire subito dopo il TIMEOUT_SEARCH
	 */
	private void searchingRelay(){

		//se non sono in elezione oppure so che è cominciata un elezione di emergenza
		if (!isElecting() || (isElecting() && !isImServed())) {
			DatagramPacket dpOut = null;
			try {
				dpOut = ClientMessageFactory.buildWhoIsRelay(BCAST, PortConfiguration.WHO_IS_RELAY_PORT_IN);
				getComManager().sendTo(dpOut);
				debug(getConsoleWifiInterface(),DebugConfiguration.DEBUG_INFO,"Messaggio WHO_IS_RELAY inviato.");
			} catch (IOException e) {
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "Errore nel spedire il messaggio di WHO_IS_RELAY");
				e.getStackTrace();}
			
			setActualStatus(ClientStatus.WAITING_WHO_IS_RELAY);
			
			setTimeoutSearch(ClientTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_SEARCH,TimeOutConfiguration.TIME_OUT_SEARCH));
			debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": start del TIMEOUT_SEARCH");
		}
	}
	
	/**Metodo che viene richiamato dal Observable (in questo caso dal ClientElectionCM appena riceve un messaggio)
	 * @param pacchetto ricevuto
	 */
	@Override
	public synchronized void update(Observable arg0, Object arg1) {

		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){
			DatagramPacket dpIn = (DatagramPacket)arg1;
			setClientMessageReader(new ClientMessageReader());
			try {
				getClientMessageReader().readContent(dpIn);
			} catch (IOException e) {
				debug(getConsoleElectionManager(),DebugConfiguration.DEBUG_ERROR,"ERRORE durante la lettura del pacchetto election");
				e.printStackTrace();}

			/** IM_RELAY
			 *  WAITING_WHO_IS_RELAY
			 *  Client si collega al nodo che gli risponde al messaggio di WHO_IS_RELAY (BigBoss o relay attivo)
			 */
			if((getClientMessageReader().getCode() == MessageCodeConfiguration.IM_RELAY) && 
				(getActualStatus() == ClientStatus.WAITING_WHO_IS_RELAY)){

				
				setConnectedRelayAddress(getClientMessageReader().getPacketAddress().getHostAddress());
				memorizeConnectedRelayAddress();
				setElecting(false);
				setActualStatus(ClientStatus.IDLE);
				
				cancelTimeoutSearch();
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": IM_RELAY arrivato,actualRelayAddress: " + getConnectedRelayAddress()+".\nPronto per richiesta file e iniziare streaming RTP.");
				//Notifico ClientSessionManager e anche il relay a cui sono connesso
				setChanged();
				notifyObservers("RELAY_FOUND:"+getConnectedRelayAddress());
				
				DatagramPacket dpOut = null;
				try {
					dpOut = ClientMessageFactory.buildAckConnection(getConnectedRelayInetAddress(), PortConfiguration.PORT_ELECTION_IN,MessageCodeConfiguration.TYPECLIENT);
					getComManager().sendTo(dpOut);
				} catch (IOException e) {
					debug(getConsoleWifiInterface(),DebugConfiguration.DEBUG_ERROR,"Errore nel spedire il messaggio di ACK_CONNECTION");
					e.getStackTrace();}

				debug(getConsoleWifiInterface(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": client creato e ACK_CONNECTION spedito");
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": Simulo sessione RTP e attivo servizio PositionControlling settando ImServ a true");
				//PARTE CHE DEVE RICHIAMARE LA SESSION MANAGER
				setImServed(true);
			}
			
			/** ELECTION_REQUEST
			 *  ogni nodo all'interno del cluster prende atto che c'è un elezione in corso, ma solo coloro che sono attivi ne partecipano
			 */
			else if(getClientMessageReader().getCode() == MessageCodeConfiguration.ELECTION_REQUEST){ 
					
				cancelTimeoutSearch();
				clearElection();
				setElecting(true);

				
				if(getActualStatus() == ClientStatus.ACTIVE){
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": ELECTION_REQUEST arrivato e ho una sessione RTP in corso.");
				
					//ulteriore controllo ma per forza deve essere true altrimenti lo stato sarebbe IDLE
					if (isImServed()) {

						DatagramPacket dpOut = null;

						try {
							//Messaggio destinato ai possibili sostituti
							dpOut = ClientMessageFactory.buildElectioBeaconRelay(getIndexELECTION_BEACON(), BCAST,PortConfiguration.PORT_ELECTION_IN,1);
							getComManager().sendTo(dpOut);
							//Messaggio destinati ai client coinvolti nella elezione
							dpOut = ClientMessageFactory.buildElectioBeacon(getIndexELECTION_BEACON(), BCAST, PortConfiguration.PORT_ELECTION_IN);
							getComManager().sendTo(dpOut);
							
							addIndexELECTION_BEACON(1);
						} catch (IOException e){e.printStackTrace();}

						setTimeoutFailToElect(ClientTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT,TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
						
						setActualStatus(ClientStatus.WAITING_END_ELECTION);
						
						debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_BEACON inviato e start TIMEOUT_FAIL_TO_ELECT");
					}

					setChanged();
					notifyObservers("RECIEVED_ELECTION_REQUEST");
				}
			}
			
			/** ELECTION_BEACON
			 *  Messaggio per propagare lo stato di elezione
			 */
			
			else if((getClientMessageReader().getCode()==MessageCodeConfiguration.ELECTION_BEACON) &&
					(!sameAddress(getClientMessageReader().getPacketAddress()))){
				
				setElecting(true);
				setConnectedRelayAddress(null);
				setConnectedRelayInetAddress(null);
				
				if((getIndexELECTION_BEACON()==0) && (getActualStatus()==ClientStatus.ACTIVE)){
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_WARNING,"Stato."+getActualStatus()+": arrivato ELECTION_BACON e index = "+getIndexELECTION_BEACON());
					DatagramPacket dpOut = null;

					try {
						//Messaggio destinato ai possibili sostituti
						dpOut = ClientMessageFactory.buildElectioBeaconRelay(getIndexELECTION_BEACON(), BCAST,PortConfiguration.PORT_ELECTION_IN, 1);
						getComManager().sendTo(dpOut);
						//Messaggio destinati ai client coinvolti nella elezione
						dpOut = ClientMessageFactory.buildElectioBeacon(getIndexELECTION_BEACON(), BCAST, PortConfiguration.PORT_ELECTION_IN);
						getComManager().sendTo(dpOut);
						
						addIndexELECTION_BEACON(1);
						
					} catch (IOException e){e.printStackTrace();}

					setTimeoutFailToElect(ClientTimeoutFactory.getSingeTimeOutWithMessage(this,TimeOutConfiguration.TIMEOUT_FAIL_TO_ELECT,TimeOutConfiguration.TIME_OUT_FAIL_TO_ELECT));
					setActualStatus(ClientStatus.WAITING_END_ELECTION);
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ELECTION_BEACON inviato e start TIMEOUT_FAIL_TO_ELECT");
				}
				
			}
			
			/** ELECTION_DONE
			 *  WAITING_END_ELECTION / IDLE
			 *  Arrivato il messaggio che contiene l indirizzo del nuovo nodo relay eletto
			 */
			else if((getClientMessageReader().getCode()==MessageCodeConfiguration.ELECTION_DONE)&&
					(getActualStatus()==ClientStatus.WAITING_END_ELECTION || getActualStatus()==ClientStatus.IDLE) &&
					(!isFirstELECTION_DONE())){
				
				setConnectedRelayAddress(getClientMessageReader().getNewRelayAddress());
				memorizeConnectedRelayAddress();
				
				if(getActualStatus()==ClientStatus.WAITING_END_ELECTION){
					cancelTimeoutFailToElect();
					setActualStatus(ClientStatus.ACTIVE);
				}
				getComManager().sendTo(prepareRepropagation(dpIn));
				setFirstELECTION_DONE(true);
			}
			
			//FASE DI EMERGENZA
			/** EM_EL_DET_CLIENT / EM_EL_DET_RELAY
			 *  EM_ELECTION / IDLE
			 *  Nodo con sessione RTP è in stato di EM_ELECTION ed ha già spedito EM_DET_CLIENT quindi non fa niente, mentre un client senza sessione RTP (IDLE) manda il messaggio
			 */
			else if((getClientMessageReader().getCode() == MessageCodeConfiguration.EM_EL_DET_CLIENT || getClientMessageReader().getCode() == MessageCodeConfiguration.EM_EL_DET_RELAY) && 
					(getActualStatus() == ClientStatus.IDLE || getActualStatus()==ClientStatus.EM_ELECTION || getActualStatus()==ClientStatus.WAITING_END_ELECTION)){
				
				cancelTimeoutFailToElect();
				clearElection();
				setElecting(true);
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO, "Stato."+getActualStatus()+": arrivato EM_EL_DET_X e ho mandato "+getIndexEM_EL_DET_CLIENT()+" messagio EM_EL_DET_CLIENT");
	
				if(getIndexEM_EL_DET_CLIENT()==0){
					setActualStatus(ClientStatus.EM_ELECTION);
					DatagramPacket dpOut = null;
					try {
						dpOut = ClientMessageFactory.buildEmElDetClient(getIndexEM_EL_DET_CLIENT(), BCAST, PortConfiguration.PORT_ELECTION_IN);
						getComManager().sendTo(dpOut);
						addIndexEM_EL_DET_CLIENT(1);
						debug(getConsoleWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": EM_EL_DET_CLIENT spedito.");
					} catch (IOException e) {e.printStackTrace();}

					setChanged();
					notifyObservers("EMERGENCY_ELECTION");
				}
			}
		}
			
			
		//PARTE PER LA GESTIONE DELLE NOTIFICHE NON PROVENIENTI DALLA RETE
		if(arg1 instanceof String){
			setEvent((String) arg1);
			
			/** TIMEOUTSEARCH
			 *  WAITING_WHO_IS_RELAY
			 *  Nodo in cerca del relay attivo, non ha ricevuto risposta -> continua a cercare*/
			if((getEvent().equals("TIMEOUTSEARCH")) &&	
				(getActualStatus() == ClientStatus.WAITING_WHO_IS_RELAY)){
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": TIMEOUT_SEARCH scattato, continuo a cercare il relay.");
				searchingRelay();
			}

			/** TIMEOUT_FAIL_TO_ELECT
			 *  WAITING_END_ELECTION / IDLE
			 *  */
			else if((getEvent().equals("TIMEOUTFAILTOELECT")) &&
					(getActualStatus() == ClientStatus.WAITING_END_ELECTION || getActualStatus()==ClientStatus.IDLE)){
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": TIMEOUT_FAIL_TO_ELECT scattato, ANNULLO TUTTO -> FASE DI EMERGENZA");

				clearElection();
				
				DatagramPacket dpOut = null;

				try {
					dpOut = ClientMessageFactory.buildEmElDetClient(getIndexEM_EL_DET_CLIENT(), BCAST, PortConfiguration.PORT_ELECTION_IN);
					getComManager().sendTo(dpOut);
					addIndexEM_EL_DET_CLIENT(1);
					debug(getConsoleWifiInterface(),DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": EM_EL_DET_CLIENT spedito.");
				} catch (IOException e) {e.printStackTrace();}

				setActualStatus(ClientStatus.EM_ELECTION);

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");
				
				debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": in attesa di EM_EL_DET_X o EM_ELECTION");

			}
		}
	}
	
	public void clearElection(){
		setImServed(false);
		setConnectedRelayAddress(null);
		setConnectedRelayInetAddress(null);
		setFirstELECTION_DONE(false);
		clearIndexELECTION_BEACON();
	}


	/**Metodo che impone al ClientElectionManager, in opportune condizioni, di mettersi alla ricerca del Relay attuale
	 */
	public synchronized void tryToSearchTheRelay(){
		if(getConnectedRelayAddress() == null) searchingRelay();
	}


	/**Metodo che consente di avvertire il ClientElectionManager che è in atto una sessione RTP o che ne è stata conclusa una
	 * @param imServed un boolean che a true indica che c'è una sessione RTP in corso, a false indica che non c'è (più) sessione RTP in corso
	 */
	public synchronized void setImServed(boolean imS) {

		//entro solo se non sono in fase di rielezione uno dei due deve essere false
		if(!isElecting() || !imS ){
			setImServed(imS);
			if (isImServed()) {
				if (getConnectedRelayAddress() != null) {
					getClientPositionController().start();
					setActualStatus(ClientStatus.ACTIVE);
					
					debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_INFO,"Stato."+getActualStatus()+": ClientPositionController AVVIATO (risponde ai messaggi RSSI_REQUEST");
				}
			}else{
				if(getClientPositionController() != null) getClientPositionController().close();
				setActualStatus(ClientStatus.IDLE);
			}
			
		}
		else{
			debug(getConsoleElectionManager(), DebugConfiguration.DEBUG_ERROR, "Stato."+getActualStatus()+": setImServed(): ERRORE: non posso essere servito se non so chi è il Relay attuale");
		}
	}

//	/**Metodo per modificare l'indirizzo del destinatario del messaggio di ELECTION_DONE nell'indirizzo BROADCAST
//	 * e la porta nella porta di ricezione di messaggi di elezione dei Relay
//	 * @param dpIn il DatagramPacket contenente il messaggio ELECTION_DONE da ripropagare
//	 * @return un DatagramPacket pronto per essere spedito in maniera da ripropagare l'ELECTION_DONE 
//	 */
	private DatagramPacket prepareRepropagation(DatagramPacket dpIn){
		dpIn.setAddress(BCAST);
		dpIn.setPort(PortConfiguration.PORT_ELECTION_IN);
		return dpIn;
	}

	private boolean sameAddress(InetAddress adr){
		if(adr.equals(getLocalInetAddress()))return true;
		else return false;
	}

	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeLocalAddress(){
		try {
			setLocalInetAddress(InetAddress.getByName(getLocalAddress()));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per memorizzare l'InetAddress relativo all'actualRelayAddress
	 * che è in forma di String 
	 */
	private void memorizeConnectedRelayAddress(){
		try {
			setConnectedRelayInetAddress(InetAddress.getByName(getConnectedRelayAddress()));
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodi get e set delle rispettive variabili */
	
	public void setActualStatus(ClientStatus actualStatus) {this.actualStatus = actualStatus;}
	public ClientStatus getActualStatus() {return actualStatus;}
	
	public void setConsoleElectionManager(DebugConsole consoleElectionManager){this.consoleElectionManager = consoleElectionManager;}
	public DebugConsole getConsoleElectionManager(){return consoleElectionManager;}
	public void setConsoleWifiInterface(DebugConsole consoleWifiInterface){this.consoleWifiInterface = consoleWifiInterface;}
	public DebugConsole getConsoleWifiInterface(){return consoleWifiInterface;}

	
	public boolean isImServed() {return imServed;}
	
	public ClientCM getComManager() {return comManager;}
	public void setComManager(ClientCM comManager) {this.comManager = comManager;}

	
	public void setLocalAddress(String localAddress) {this.localAddress = localAddress;}
	public String getLocalAddress() {return localAddress;}
	public void setLocalInetAddress(InetAddress localInetAddress) {this.localInetAddress = localInetAddress;}
	public InetAddress getLocalInetAddress() {return localInetAddress;}
	public void setConnectedRelayAddress(String connectedRelayAddress) {this.connectedRelayAddress = connectedRelayAddress;}
	public String getConnectedRelayAddress() {return connectedRelayAddress;}
	public void setConnectedRelayInetAddress(InetAddress connectedRelayInetAddress) {this.connectedRelayInetAddress = connectedRelayInetAddress;}
	public InetAddress getConnectedRelayInetAddress() {return connectedRelayInetAddress;}
	
	public static InetAddress getBCAST() {return BCAST;}
	
	private boolean isFirstELECTION_DONE(){return this.firstELECTION_DONE;}
	private void setFirstELECTION_DONE(boolean firstELECTION_DONE){this.firstELECTION_DONE = firstELECTION_DONE;}
	
	private boolean isElecting() {return electing;}
	private void setElecting(boolean electing) {this.electing = electing;}
	
	private void clearIndexEM_DET_CLIENT(){this.indexEM_EL_DET_CLIENT=0;}
	private void addIndexEM_EL_DET_CLIENT(int indexEM_EL_DET_CLIENT) {this.indexEM_EL_DET_CLIENT+= indexEM_EL_DET_CLIENT;}
	private int getIndexEM_EL_DET_CLIENT() {return indexEM_EL_DET_CLIENT;}

	
	private void clearIndexELECTION_BEACON(){this.indexELECTION_BEACON=0;}
	private void addIndexELECTION_BEACON(int indexELECTION_BEACON){this.indexELECTION_BEACON+=indexELECTION_BEACON;}
	private int getIndexELECTION_BEACON(){return indexELECTION_BEACON;}

	
	private void setClientWNICController(ClientWNICController clientWNICController){this.clientWNICController = clientWNICController;}
	private ClientWNICController getClientWNICController(){return clientWNICController;}
	private ClientPositionController getClientPositionController() {return clientPositionController;}
	private void setClientPositionController(ClientPositionController clientPositionController) {this.clientPositionController = clientPositionController;}
	private ClientMessageReader getClientMessageReader() {return clientMessageReader;}
	private void setClientMessageReader(ClientMessageReader clientMessageReader) {this.clientMessageReader = clientMessageReader;}
	
	private void setEvent(String event){this.event=event;}
	private String getEvent(){return event;}
	
	//Timeout
	private void setTimeoutSearch(TimeOutSingleWithMessage timeoutSearch){this.timeoutSearch = timeoutSearch;}
	private TimeOutSingleWithMessage getTimeoutSearch(){return timeoutSearch;}
	private void cancelTimeoutSearch(){
		if(getTimeoutSearch()!=null){
			getTimeoutSearch().cancelTimeOutSingleWithMessage();
			setTimeoutSearch(null);
		}
	}
	
	private void setTimeoutFailToElect(TimeOutSingleWithMessage timeoutFailToElect){this.timeoutFailToElect = timeoutFailToElect;}
	private TimeOutSingleWithMessage getTimeoutFailToElect(){return timeoutFailToElect;}
	private void cancelTimeoutFailToElect(){
		if(getTimeoutFailToElect()!=null){
			getTimeoutFailToElect().cancelTimeOutSingleWithMessage();
			setTimeoutSearch(null);
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

	//public ClientFrameController getFrameController() {return frameController;}
	//public void setFrameController(ClientFrameController frameController) {//this.frameController = frameController;/}
}

//class TesterClientElectionManager{
//
//	public static void stamp(ClientElectionManager cem, int i){
//		System.out.println("\n*****************("+i+") "+cem.getActualStatus().toString().toUpperCase()+"**************************************************");
//		System.out.println("Status: " + cem.getActualStatus());
//		System.out.println("actualRelayAddress: " + cem.getActualRelayAddress());
//		System.out.println("electing: " + cem.isElecting());
//		System.out.println("imServed: " + cem.isImServed());
//		System.out.println("first EM_EL sent: " + cem.isFirstEM_ELsent());
//		System.out.println("********************************************************************************");
//	}
//
//
//	public static void main(String args[]){
//
//		DatagramSocket dsNet = null;
//		DebugConsole console = new DebugConsole();
//		console.setTitle("TEST MAIN CLIENT ELECTIONMANAGER");
//
//		try {
//			dsNet = new DatagramSocket(9999);
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
//
//		ClientElectionManager cem = ClientElectionManager.getINSTANCE();
//
//		//STATO IDLE && IM_RELAY ARRIVATO																	OK
//		/* Status: IDLE
//		 * actualRelayAddress: 192.168.0.4
//		 * electing: false
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//		
//		
//
//		try {
//
//			Thread.sleep(1000);
//
//			DatagramPacket dpOut = RelayMessageFactory.buildImRelay("192.168.0.1", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(5000);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		//STAMPA 1
//		stamp(cem,1);
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildRequestRSSI(5, InetAddress.getByName(Parameters.BROADCAST_ADDRESS), Parameters.CLIENT_RSSI_PORT);
//
//			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());
//
//			dsNet.send(dpOut);
//
//			Thread.sleep(5000);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			
//			
//		}
//
//
//
//
//		//STATO IDLE && TIMEOUT_SEARCH SCATTATO	!!!!ATTENZIONE ANCHE QUESTO DA PROBLEMI !!!!				OK dopo aver modificato i Timeouts
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: false
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//		
//		//cem.getTimeoutSearch().cancelTimeOutSearch();
//		//cem.update(null, "TIMEOUTSEARCH");
//		System.out.println("TIMEOUTSEARCH FERMATO");
//
//
//		//STAMPA 2
//		stamp(cem,2);
//
//		
//
//
//
//		//STATO IDLE && imServed = false && ELECTION_REQUEST ARRIVATO										OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//			
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 3
//		stamp(cem,3);
//
//		 
//
//
//
//		//STATO IDLE && EM_EL_DET_X ARRIVATO && firstEM_ELsent = false										OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: true
//		 */
//
//		
//		cem.setFirstEM_ELsent(false);
//
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(34, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 4
//		stamp(cem,4);
//		
//		 
//
//
//		//STATO IDLE && EM_EL_DET_X ARRIVATO && firstEM_ELsent = true										OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: true
//		 */
//		
//		cem.setFirstEM_ELsent(true);
//
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(34, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 5
//		stamp(cem,5);
//
//		 
//
//
//		//STATO IDLE && imServed = true && ELECTION_REQUEST ARRIVATO										OK
//		/* Status: WAITING_END_ELECTION
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: true
//		 * first EM_EL sent: false
//		 */
//
//		cem.setImServed(true);
//
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//fermo il TIMEOUT_FAIL_TO_ELECT
//		//cem.getTimeoutFailToElect().cancelTimeOutFailToElect();
//
//		//fermo il ClientPositionController
//		cem.getClientPositionController().close();
//
//		//STAMPA 6
//		stamp(cem,6);
//
//
//
//
//
//		//TENTATIVO DI setImServed = false sebbene con electing = true dovrebbe andare						OK
//		/* Status: WAITING_END_ELECTION
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//
//		cem.setImServed(false);
//
//		//STAMPA 7
//		stamp(cem,7);
//
//
//
//
//
//		//TENTATIVO DI setImServed = true non deve andare													OK
//		/* Status: WAITING_END_ELECTION
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//
//		cem.setImServed(true);
//
//		//STAMPA 8
//		stamp(cem,8);
//
//
//
//
//
//		//STATO WAITING_END_NORMAL_ELECTION && RICEZIONE EM_EL_DET_X										OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: true
//		 */
//		
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(5, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 9
//		stamp(cem,9);
//
//		 
//
//
//		//STATO WAITING_END_NORMAL_ELECTION && RICEZIONE ELECTION_DONE										OK
//		/* Status: IDLE
//		 * actualRelayAddress: 192.168.0.123
//		 * electing: false
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */
//		
//		try {
//
//			DatagramPacket dpOut = RelayMessageFactory.buildElectionDone(5, "192.168.0.123", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 10
//		stamp(cem,10);
//
//		 
//
//
//		//STATO WAITING_END_ELECTION && TIMEOUT_FAIL_TO_ELECT SCATTATO 								OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: true
//		 */
//
//		cem.update(null, "TIMEOUTFAILTOELECT");
//
//		//STAMPA 11
//		stamp(cem,11);
//
//
//
//		//Portarsi qui in stato Idle && electing == true && firstEM_ELsent = true;
//
//		//RIPRESA DELLA RICERCA DEL RELAY DOPO ESSERSI ACCORTI DI UN'EMERGENCY ELECTION				OK
//		/* Status: IDLE
//		 * actualRelayAddress: null
//		 * electing: true
//		 * imServed: false
//		 * first EM_EL sent: true
//		 */
//
//		cem.tryToSearchTheRelay();
//
//		//fermo TIMEOUT_SEARCH
//		//cem.getTimeoutSearch().cancelTimeOutSearch();
//
//		//STAMPA 12
//		stamp(cem,12);
//
//
//
//
//
//		//STATO WAITING_END_ELECTION && TIMEOUT_FAIL_TO_ELECT SCATTATO 								OK
//		/* Status: IDLE
//		 * actualRelayAddress: 192.168.0.9
//		 * electing: false
//		 * imServed: false
//		 * first EM_EL sent: false
//		 */		
//
//		try {
//
//			Thread.sleep(1000);
//
//			DatagramPacket dpOut = RelayMessageFactory.buildImRelay("192.168.0.9", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);
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
//		//STAMPA 13
//		stamp(cem,13);
//
//		cem.getComManager().close();
//
//	}
//}
//
//
//
///*class TestClientElectionManager{
//
//	public static void main(String args[]){
//
//		ClientElectionManager cem = ClientElectionManager.getINSTANCE();
//		ClientSessionManager csm = ClientSessionManager.getInstance();
//		cem.addObserver(csm);
//		cem.setImServed(true);	
//	}
//}*/