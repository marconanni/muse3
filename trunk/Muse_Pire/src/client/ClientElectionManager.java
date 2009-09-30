package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.Parameters;
import relay.RelayMessageFactory;

import client.ClientMessageReader;
import client.timeout.ClientTimeoutFactory;
import client.connection.ClientConnectionFactory;
import client.connection.ClientElectionCM;
import client.positioning.ClientPositionController;

import client.timeout.TimeOutFailToElect;
import client.timeout.TimeOutSearch;

import client.wnic.exception.WNICException;
import debug.DebugConsole;

/**
 * @author Leo Di Carlo, Luca Campeti
 *
 */

public class ClientElectionManager extends Observable implements Observer{

	//ClientFrameController assegnabile al ClientElectionManager
	//private ClientFrameController frameController = null;
	private DebugConsole console = null;

	//stati in cui si può trovare il ClientElectionManager
	public enum ClientStatus {
		INSTABLE,
		IDLE, 
		WAITING_END_ELECTION,
	}

	//il manaegr per le comunicazioni	
	private ClientElectionCM comManager = null;

	//indirizzo broadcast in forma InetAddress
	private static InetAddress BCAST = null;
	
	static { 
		try {
			BCAST  = InetAddress.getByName(Parameters.BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	//stato attuale del ClientElectionManager
	private ClientStatus actualStatus = null;

	//indirizzo del Relay attuale in forma String
	private String actualRelayAddress = null;

	//indirizzo del Relay in forma InetAddress
	//private InetAddress actualRelayInetAddress = null;	

	//boolean che indica se si è in stato di elezione
	private boolean electing = false;

	//boolean che indica se è stato già spedito un messaggio EM_EL_DET_CLIENT
	private boolean firstEM_ELsent = false ;

	//boolean che indica se questo nodo Client è coinvolto in una sessione RTP
	private boolean imServed = false;

	//il ClientPositionController per rispondere ai messaggi REQUEST_RSSI con il valore di RSSI
	//rilevato dal Client nei confronti del Relay attuale
	private ClientPositionController clientPositionController = null;

	//il ClientMessageReader per leggere il contenuto dei messaggi ricevuti
	private ClientMessageReader clientMessageReader = null;

	//vari Timeout necessari al ClientElectionManager
	private TimeOutSearch timeoutSearch = null;
	private TimeOutFailToElect timeoutFailToElect = null;

	//indici dei messaggi inviati
	private int indexEM_EL_DET_CLIENT = 0;
	private int indexELECTION_BEACON = 0;

	//istanza singleton del ClientElectionManager
	private static ClientElectionManager INSTANCE = null;


	/**Costruttore per ottenere un nuovo ClientElectionManager.
	 * Fa partire il Connection Manager ed in seguito si mette
	 * alla ricerca del Relay attuale.
	 */
	private ClientElectionManager(DebugConsole console){
		this.setDebugConsole(console);

		comManager = ClientConnectionFactory.getElectionConnectionManager(this);
		comManager.start();

		searchingRelay();

		console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: creato ed entrato in STATO di:"+actualStatus+" inviato WHO_IS_RELAY e start TIMEOUT_SEARCH");
	}


	/**Metodo per ottenere l'istanza della classe singleton ClientElectionManager
	 * @return un riferimento al singleton ClientElectionManager
	 */
	public static ClientElectionManager getINSTANCE(DebugConsole console){
		if(INSTANCE == null) INSTANCE = new ClientElectionManager(console);
		return INSTANCE;
	}


	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */

	public synchronized void update(Observable arg0, Object arg1) {

		//PARTE PER LA GESTIONE DEI MESSAGGI PROVENIENTI DALLA RETE
		if(arg1 instanceof DatagramPacket){

			DatagramPacket dpIn = (DatagramPacket)arg1;
			clientMessageReader = new ClientMessageReader();

			try {
				clientMessageReader.readContent(dpIn);
			} catch (IOException e) {e.printStackTrace();}

			/*Cominciamo ad esaminare i vari casi che si possono presentare, in base allo stato in cui ci si trova 
				e al codice del messaggio che è appena arrivato*/


			/** INIZIO STATO IDLE**/	
			
			//Da vedere a quale relay il cliente si deve collegare
			//SOLUZIONI:
			//1) al primo che risponde (synchronize il cambio di actualstatus)
			//2) memorizzare tutte le risposte e ricevute in coppia (ip,RSSI) del relay e poi prendere quello con il segnale + alto->+ vicino
			//3) Non ci poniamo questo problema e ci colleghiamo al relay staticamente (richiede di conoscere l'ip del relay on startup)

			if(clientMessageReader.getCode() == Parameters.IM_RELAY && actualStatus == ClientStatus.INSTABLE){

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO INSTABLE: IM_RELAY arrivato: ->STATO IDLE");
				
				actualStatus = ClientStatus.IDLE;

				if(timeoutSearch != null) {
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				actualRelayAddress = clientMessageReader.getActualRelayAddress();
				electing = false;
				firstEM_ELsent = false;

				
				//Notifico ClientSessionManager
				setChanged();
				notifyObservers("RELAY_FOUND:"+actualRelayAddress);


				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: IM_RELAY arrivato e actualRelayAddress: " + actualRelayAddress);
				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: Simulo sessione RTP e attivo servizio PositionControlling settando ImServ a true");
				setImServed(true);

				
			}


			/*[ELECTION_DONE ricevuto] / 
			 *relayAddress = <IP del NEW RELAY>
			 *reset TIMEOUT_SEARCH
			 */
			else if(clientMessageReader.getCode() == Parameters.ELECTION_DONE && actualStatus == ClientStatus.IDLE){

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: ELECTION_DONE arrivato");

				if(timeoutSearch != null){
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				electing = false;
				firstEM_ELsent = false;

				if(actualRelayAddress == null ||!actualRelayAddress.equals(clientMessageReader.getNewRelayAddress())){

					actualRelayAddress = clientMessageReader.getNewRelayAddress();


					if (imServed) preparePositionController();


					//Notifico ClientSessionManager
					setChanged();
					notifyObservers("NEW_RELAY:"+actualRelayAddress);


					//RIPROPAGAZIONE UNA TANTUM DELL'ELECTION_DONE
					comManager.sendTo(prepareRepropagation(dpIn));

					console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: ELECTION_DONE ripropagato ");
				}

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: ELECTION_DONE arrivato e actualRelayAddress: " + actualRelayAddress);
			}


			/*[ELECTION_REQUEST arrivato && !served] / 
			 *electing = true
			 */

			/*[ELECTION_REQUEST arrivato && served] / 
			 *invio ELECTION_BEACON
			 *status = WaitingEndNormalElection
			 *start TIMEOUT_FAIL_TO_ELECT
			 *relayAddress = null;
			 */
			else if(clientMessageReader.getCode() == Parameters.ELECTION_REQUEST && actualStatus == ClientStatus.IDLE){

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: ELECTION_REQUEST arrivato " + (imServed?"e sono servito":"ma non sono servito"));

				if(timeoutSearch != null){
					timeoutSearch.cancelTimeOutSearch();
					timeoutSearch = null;
				}

				electing = true;
				actualRelayAddress = null;

				if (imServed) {

					DatagramPacket dpOut = null;

					try {
						dpOut = ClientMessageFactory.buildElectioBeacon(indexELECTION_BEACON, BCAST,Parameters.RELAY_ELECTION_PORT_IN);
						comManager.sendTo(dpOut);
						indexELECTION_BEACON++;
					} catch (IOException e) {
						e.printStackTrace();
					}

					timeoutFailToElect = ClientTimeoutFactory.getTimeOutFailToElect(this,Parameters.TIMEOUT_FAIL_TO_ELECT);

					actualStatus = ClientStatus.WAITING_END_ELECTION;

					console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE -> WAITING_END_ELECTION: ELECTION_REQUEST arrivato && imServed==true e start TIMEOUT_FAIL_TO_ELECT");
				}

				setChanged();
				notifyObservers("RECIEVED_ELECTION_REQUEST");
			}


			/*[ arrivato EM_EL_DET_X && !served] / 
			 *if(firstEM_EL)
			 *{
			 *invio EM_EL_DET_CLIENT
			 *firstEM_EL = false
			 *}
			 */
			else if((clientMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT || clientMessageReader.getCode() == Parameters.EM_EL_DET_RELAY) && 
					actualStatus == ClientStatus.IDLE){

				actualRelayAddress = null;

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: arrivato EM_EL_DET_X  "+(firstEM_ELsent?"ma ho già inviato EM_EL_DET_CLIENT":"è non ho ancora inviato EM_EL_DET_CLIENT"));

				electing = true;

				if(!firstEM_ELsent){

					firstEM_ELsent = true;

					DatagramPacket dpOut = null;

					try {
						dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.RELAY_ELECTION_PORT_IN);
						comManager.sendTo(dpOut);
						dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
						comManager.sendTo(dpOut);
						indexEM_EL_DET_CLIENT++;
					} catch (IOException e) {
						e.printStackTrace();
					}

					setChanged();
					notifyObservers("EMERGENCY_ELECTION");

					console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE: arrivato EM_EL_DET_X e EM_EL_DET_CLIENT inviato");
				}
			}
			/**FINE STATO IDLE**/	



			/**INIZIO STATO WAITING_END_ELECTION**/

			/*[ELECTION_DONE ricevuto] / 
			 *relayAddress = <IP in ELECTION_DONE>
			 *status = idle
			 *reset TIMEOUT_FAIL_TO_ELECT 
			 */
			else if(clientMessageReader.getCode() == Parameters.ELECTION_DONE &&
					actualStatus == ClientStatus.WAITING_END_ELECTION){

				if(timeoutFailToElect != null) {
					timeoutFailToElect.cancelTimeOutFailToElect();
					timeoutFailToElect = null;
				}

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION: ELECTION_DONE ricevuto");

				actualRelayAddress = clientMessageReader.getNewRelayAddress();
				
				preparePositionController();
				
				electing = false;
				
				//memorizeRelayAddress();

				//RIPROPAGAZIONE UNA TANTUM DELL'ELECTION_DONE
				comManager.sendTo(prepareRepropagation(dpIn));

				actualStatus = ClientStatus.IDLE;

				setChanged();
				notifyObservers("NEW_RELAY:"+ actualRelayAddress);

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION -> IDLE: ELECTION_DONE ricevuto: actualRelayAddress: " + actualRelayAddress);
			}

			/*[EM_EL_CLIENT_DET || EM_EL_RELAY_DET] / 
			 *reset TIMEOUT_FAIL_TO_ELECT 
			 *invio EM_EL_CLIENT_DET
			 *status = idle
			 */
			else if((clientMessageReader.getCode() == Parameters.EM_EL_DET_CLIENT ||
					clientMessageReader.getCode() == Parameters.EM_EL_DET_RELAY)&&
					actualStatus == ClientStatus.WAITING_END_ELECTION){

				if(timeoutFailToElect != null) {
					timeoutFailToElect.cancelTimeOutFailToElect();
					timeoutFailToElect = null;
				}

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION: EM_EL_RELAY_DET_X ricevuto");

				imServed = false;
				clientPositionController.close();
				System.err.println("ClientPositionController SPENTO");

				DatagramPacket dpOut = null;

				try {
					dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
					comManager.sendTo(dpOut);
					indexEM_EL_DET_CLIENT++;
				} catch (IOException e) {
					e.printStackTrace();
				}


				firstEM_ELsent = true;

				actualStatus = ClientStatus.IDLE;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION -> IDLE: EM_EL_RELAY_DET_X ricevuto: inviato EM_EL_DET_CLIENT");

			}
			/**FINE STATO WAITING_END_ELECTION**/

		}


		//PARTE PER LA GESTIONE DELLE NOTIFICHE NON PROVENIENTI DALLA RETE
		if(arg1 instanceof String){

			String event = (String) arg1;


			/**INIZIO STATO IDLE**/

			/*[TIMEOUT_SEARCH scattato] --> SearchingRelay
			 */
			if(event.equals("TIMEOUTSEARCH") &&
					actualStatus == ClientStatus.INSTABLE){

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO IDLE:  TIMEOUT_SEARCH scattato");

				//Stato instabile SearchingRelay
				searchingRelay();
			}
			/**FINE STATO IDLE**/



			/**INIZIO STATO WAITING_END_ELECTION**/

			/*[TIMEOUT_FAIL_TO_ELECT scattato]/
			 * invio EM_EL_CLIENT_DET
			 * status = idle
			 */
			else if(event.equals("TIMEOUTFAILTOELECT") &&
					actualStatus == ClientStatus.WAITING_END_ELECTION){

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION: TIMEOUT_FAIL_TO_ELECT scattato");

				imServed = false;
				clientPositionController.close();
				System.err.println("ClientPositionController SPENTO");

				DatagramPacket dpOut = null;

				try {
					dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.RELAY_ELECTION_PORT_IN);
					comManager.sendTo(dpOut);
					dpOut = ClientMessageFactory.buildEmElDetClient(indexEM_EL_DET_CLIENT, BCAST, Parameters.CLIENT_PORT_ELECTION_IN);
					comManager.sendTo(dpOut);
					indexEM_EL_DET_CLIENT++;
				} catch (IOException e) {
					e.printStackTrace();
				}


				firstEM_ELsent = true;

				actualStatus = ClientStatus.IDLE;

				setChanged();
				notifyObservers("EMERGENCY_ELECTION");

				console.debugMessage(Parameters.DEBUG_INFO,"ClientElectionManager: STATO WAITING_END_ELECTION -> IDLE: TIMEOUT_FAIL_TO_ELECT scattato: inviato EM_EL_DET_CLIENT");

			}
			/**FINE STATO WAITING_END_ELECTION**/
		}
	}


	/**Metodo per impostare un FrameController per il debug
	 * @param frameController il ClientFrameController da impostare per il debug
	 */
	//public void setFrameController(ClientFrameController frameController) {
		//this.frameController = frameController;
	//}

	public void setDebugConsole(DebugConsole console) {
		this.console = console;
	}

	/**Metodo che impone al ClientElectionManager, in opportune condizioni, 
	 * di mettersi alla ricerca del Relay attuale
	 */
	public synchronized void tryToSearchTheRelay(){

		if(actualRelayAddress == null) searchingRelay();

	}


	/**Metodo che consente di avvertire il ClientElectionManager che è in atto una sessione RTP
	 * o che ne è stata conclusa una
	 * @param imServed un boolean che a true indica che c'è una sessione RTP in corso, a false indica che non
	 * c'è (più) sessione RTP in corso
	 */
	public synchronized void setImServed(boolean imS) {
		System.out.println("setImServed 1");

		//entro solo se non sono in fase di rielezione
		//uno dei due deve essere false
		if(!electing || !imS ){
			System.out.println("setImServed 2");

			this.imServed = imS;

			if (this.imServed) {
				System.out.println("setImServed 3");

				if (actualRelayAddress != null) {
					System.out.println("setImServed 4");
					
					try {

						clientPositionController = new ClientPositionController(Parameters.NAME_OF_CLIENT_INTERFACE,Parameters.NAME_OF_AD_HOC_NETWORK);
						clientPositionController.getClientWNICController().setDebugConsole(this.console);
						clientPositionController.setDebugConsole(this.console);

						preparePositionController();
						clientPositionController.start();
						console.debugMessage(Parameters.DEBUG_INFO,"ClientPositionController AVVIATO");

					} catch (WNICException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}

				System.err.println("ClientElectionManager.setImServed(): imServed settato a: " + imServed);
			}
			else if(clientPositionController != null) clientPositionController.close();
		}
		else System.err.println("ClientElectionManager.setImServed(): ERRORE: non posso essere servito se non so chi è il Relay attuale");
	}


	/**Metodo che consente di mettersi alla ricerca di un Relay (se non si è in elezione,
	 * o se si è in attesa della fine di un'elezione di emergenza)
	 * inviando un WHO_IS_RELAY, facendo partire subito dopo 
	 * il TIMEOUT_SEARCH  e impostando lo stato in IDLE
	 */
	private void searchingRelay(){

		//se non sono in elezione oppure so che è cominciata un elezione di emergenza
		if (!electing || (electing && firstEM_ELsent && !imServed)) {
			actualRelayAddress = null;
			DatagramPacket dpOut = null;
			try {
				dpOut = ClientMessageFactory.buildWhoIsRelay(BCAST,	Parameters.WHO_IS_RELAY_PORT);
				comManager.sendTo(dpOut);

			} catch (IOException e) {e.printStackTrace();}
			timeoutSearch = ClientTimeoutFactory.getTimeOutSearch(this,	Parameters.TIMEOUT_SEARCH);
			
			actualStatus = ClientStatus.INSTABLE;
			this.console.debugMessage(Parameters.DEBUG_WARNING,"ClientElectionManager: STATO INSTABILE SEARCHING_RELAY -> INSTABLE: WHO_IS_RELAY inviato e start del TIMEOUT_SEARCH");
		}
	}


	/**Metodo per modificare l'indirizzo del destinatario del messaggio di ELECTION_DONE nell'indirizzo BROADCAST
	 * e la porta nella porta di ricezione di messaggi di elezione dei Relay
	 * @param dpIn il DatagramPacket contenente il messaggio ELECTION_DONE da ripropagare
	 * @return un DatagramPacket pronto per essere spedito in maniera da ripropagare l'ELECTION_DONE 
	 */
	private DatagramPacket prepareRepropagation(DatagramPacket dpIn){

		dpIn.setAddress(BCAST);
		dpIn.setPort(Parameters.RELAY_ELECTION_PORT_IN);
		return dpIn;
	}


	/**Metodo per creare e avviare il ClientPositionController 
	 */
	private void preparePositionController(){

		clientPositionController.setRelayAddress(actualRelayAddress);
	}


	/**Metodo per avere stampate di debug
	 * @param message una String che rappresenta il messaggio da presentare
	 */
	//private void debugPrint(String message){
		//System.err.println(message);
		//if(this.frameController != null)this.frameController.debugMessage(message);	

	//}

	/*	private void memorizeRelayAddress(){

		try {
			actualRelayInetAddress = InetAddress.getByName(actualRelayAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}*/


	public boolean isImServed() {
		return imServed;
	}


	public ClientElectionCM getComManager() {
		return comManager;
	}


	public void setComManager(ClientElectionCM comManager) {
		this.comManager = comManager;
	}


	public ClientStatus getActualStatus() {
		return actualStatus;
	}


	public void setActualStatus(ClientStatus actualStatus) {
		this.actualStatus = actualStatus;
	}


	public String getActualRelayAddress() {
		return actualRelayAddress;
	}


	public void setActualRelayAddress(String actualRelayAddress) {
		this.actualRelayAddress = actualRelayAddress;
	}


	public boolean isElecting() {
		return electing;
	}


	public void setElecting(boolean electing) {
		this.electing = electing;
	}


	public boolean isFirstEM_ELsent() {
		return firstEM_ELsent;
	}


	public void setFirstEM_ELsent(boolean firstEM_ELsent) {
		this.firstEM_ELsent = firstEM_ELsent;
	}


	public ClientPositionController getClientPositionController() {
		return clientPositionController;
	}


	public void setClientPositionController(
			ClientPositionController clientPositionController) {
		this.clientPositionController = clientPositionController;
	}


	public ClientMessageReader getClientMessageReader() {
		return clientMessageReader;
	}


	public void setClientMessageReader(ClientMessageReader clientMessageReader) {
		this.clientMessageReader = clientMessageReader;
	}


	public TimeOutSearch getTimeoutSearch() {
		return timeoutSearch;
	}


	public void setTimeoutSearch(TimeOutSearch timeoutSearch) {
		this.timeoutSearch = timeoutSearch;
	}


	public TimeOutFailToElect getTimeoutFailToElect() {
		return timeoutFailToElect;
	}


	public void setTimeoutFailToElect(TimeOutFailToElect timeoutFailToElect) {
		this.timeoutFailToElect = timeoutFailToElect;
	}


	public int getIndexEM_EL_DET_CLIENT() {
		return indexEM_EL_DET_CLIENT;
	}


	public void setIndexEM_EL_DET_CLIENT(int indexEM_EL_DET_CLIENT) {
		this.indexEM_EL_DET_CLIENT = indexEM_EL_DET_CLIENT;
	}


	public int getIndexELECTION_BEACON() {
		return indexELECTION_BEACON;
	}


	public void setIndexELECTION_BEACON(int indexELECTION_BEACON) {
		this.indexELECTION_BEACON = indexELECTION_BEACON;
	}


	//public ClientFrameController getFrameController() {
		//return frameController;
	//}


	public static InetAddress getBCAST() {
		return BCAST;
	}

}


class TesterClientElectionManager{

	public static void stamp(ClientElectionManager cem, int i){
		System.out.println("\n*****************("+i+") "+cem.getActualStatus().toString().toUpperCase()+"**************************************************");
		System.out.println("Status: " + cem.getActualStatus());
		System.out.println("actualRelayAddress: " + cem.getActualRelayAddress());
		System.out.println("electing: " + cem.isElecting());
		System.out.println("imServed: " + cem.isImServed());
		System.out.println("first EM_EL sent: " + cem.isFirstEM_ELsent());
		System.out.println("********************************************************************************");
	}


	public static void main(String args[]){

		DatagramSocket dsNet = null;
		DebugConsole console = new DebugConsole();
		console.setTitle("TEST MAIN CLIENT ELECTIONMANAGER");

		try {
			dsNet = new DatagramSocket(9999);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		ClientElectionManager cem = ClientElectionManager.getINSTANCE(console);

		//STATO IDLE && IM_RELAY ARRIVATO																	OK
		/* Status: IDLE
		 * actualRelayAddress: 192.168.0.4
		 * electing: false
		 * imServed: false
		 * first EM_EL sent: false
		 */
		
		

		try {

			Thread.sleep(1000);

			DatagramPacket dpOut = RelayMessageFactory.buildImRelay("192.168.0.1", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(5000);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 1
		stamp(cem,1);
		try {

			DatagramPacket dpOut = RelayMessageFactory.buildRequestRSSI(5, InetAddress.getByName(Parameters.BROADCAST_ADDRESS), Parameters.CLIENT_RSSI_PORT);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(5000);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
			
			
		}




		//STATO IDLE && TIMEOUT_SEARCH SCATTATO	!!!!ATTENZIONE ANCHE QUESTO DA PROBLEMI !!!!				OK dopo aver modificato i Timeouts
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: false
		 * imServed: false
		 * first EM_EL sent: false
		 */
		
		//cem.getTimeoutSearch().cancelTimeOutSearch();
		//cem.update(null, "TIMEOUTSEARCH");
		System.out.println("TIMEOUTSEARCH FERMATO");


		//STAMPA 2
		stamp(cem,2);

		



		//STATO IDLE && imServed = false && ELECTION_REQUEST ARRIVATO										OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: false
		 */
			
		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 3
		stamp(cem,3);

		 



		//STATO IDLE && EM_EL_DET_X ARRIVATO && firstEM_ELsent = false										OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: true
		 */

		
		cem.setFirstEM_ELsent(false);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(34, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 4
		stamp(cem,4);
		
		 


		//STATO IDLE && EM_EL_DET_X ARRIVATO && firstEM_ELsent = true										OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: true
		 */
		
		cem.setFirstEM_ELsent(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(34, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 5
		stamp(cem,5);

		 


		//STATO IDLE && imServed = true && ELECTION_REQUEST ARRIVATO										OK
		/* Status: WAITING_END_ELECTION
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: true
		 * first EM_EL sent: false
		 */

		cem.setImServed(true);

		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionRequest(cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//fermo il TIMEOUT_FAIL_TO_ELECT
		//cem.getTimeoutFailToElect().cancelTimeOutFailToElect();

		//fermo il ClientPositionController
		cem.getClientPositionController().close();

		//STAMPA 6
		stamp(cem,6);





		//TENTATIVO DI setImServed = false sebbene con electing = true dovrebbe andare						OK
		/* Status: WAITING_END_ELECTION
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: false
		 */

		cem.setImServed(false);

		//STAMPA 7
		stamp(cem,7);





		//TENTATIVO DI setImServed = true non deve andare													OK
		/* Status: WAITING_END_ELECTION
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: false
		 */

		cem.setImServed(true);

		//STAMPA 8
		stamp(cem,8);





		//STATO WAITING_END_NORMAL_ELECTION && RICEZIONE EM_EL_DET_X										OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: true
		 */
		
		try {

			DatagramPacket dpOut = RelayMessageFactory.buildEmElDetRelay(5, cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 9
		stamp(cem,9);

		 


		//STATO WAITING_END_NORMAL_ELECTION && RICEZIONE ELECTION_DONE										OK
		/* Status: IDLE
		 * actualRelayAddress: 192.168.0.123
		 * electing: false
		 * imServed: false
		 * first EM_EL sent: false
		 */
		
		try {

			DatagramPacket dpOut = RelayMessageFactory.buildElectionDone(5, "192.168.0.123", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 10
		stamp(cem,10);

		 


		//STATO WAITING_END_ELECTION && TIMEOUT_FAIL_TO_ELECT SCATTATO 								OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: true
		 */

		cem.update(null, "TIMEOUTFAILTOELECT");

		//STAMPA 11
		stamp(cem,11);



		//Portarsi qui in stato Idle && electing == true && firstEM_ELsent = true;

		//RIPRESA DELLA RICERCA DEL RELAY DOPO ESSERSI ACCORTI DI UN'EMERGENCY ELECTION				OK
		/* Status: IDLE
		 * actualRelayAddress: null
		 * electing: true
		 * imServed: false
		 * first EM_EL sent: true
		 */

		cem.tryToSearchTheRelay();

		//fermo TIMEOUT_SEARCH
		//cem.getTimeoutSearch().cancelTimeOutSearch();

		//STAMPA 12
		stamp(cem,12);





		//STATO WAITING_END_ELECTION && TIMEOUT_FAIL_TO_ELECT SCATTATO 								OK
		/* Status: IDLE
		 * actualRelayAddress: 192.168.0.9
		 * electing: false
		 * imServed: false
		 * first EM_EL sent: false
		 */		

		try {

			Thread.sleep(1000);

			DatagramPacket dpOut = RelayMessageFactory.buildImRelay("192.168.0.9", cem.getBCAST(), Parameters.CLIENT_PORT_ELECTION_IN);

			System.out.println("indirizzo: " + dpOut.getAddress().getHostAddress()+" porta: " +dpOut.getPort());

			dsNet.send(dpOut);

			Thread.sleep(500);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//STAMPA 13
		stamp(cem,13);

		cem.getComManager().close();

	}
}



/*class TestClientElectionManager{

	public static void main(String args[]){

		ClientElectionManager cem = ClientElectionManager.getINSTANCE();
		ClientSessionManager csm = ClientSessionManager.getInstance();
		cem.addObserver(csm);
		cem.setImServed(true);	
	}
}*/