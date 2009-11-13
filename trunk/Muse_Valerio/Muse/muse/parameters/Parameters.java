package parameters;
/**
 * Parameters.java
 * Classe contenente i parametri utilizzati per il settaggio del proxy
 * @author Ambra Montecchia
 * @version 1.2
 * */

public class Parameters {
	
	/**
	 * Nome del file che si vuole riprodurre (il file deve trovarsi nella cartella mp3)
	 */
	//public static final String fileName="prova2.mp3";
	
	/**
	 * dimensione del buffer lato proxy
	 */
	//public static final int PROXY_BUFFER = 100;
	
	/**
	 * dimensione del buffer lato client
	 */
	public static final int CLIENT_BUFFER = 5000;
	
	/**
	 * porta su cui il proxy riceve informazioni di controllo
	 * relative al livello di riempimento del buffer del client
	 */
	//public static final int PROXY_BUFFER_CONTROL = 4040;
	
	/**
	 * porta su cui il proxy riceve lo stream dal server
	 */
	//public static final int PROXY_RECEIVE_PORT = 4030;
	
	/**
	 * porta su cui il proxy invia lo stream al client
	 */
	//public static final int PROXY_SEND_PORT = 4020;
	
	/**
	 * porta su cui il proxy invia i messaggi di controllo al client
	 */
	//public static final int PROXY_CONTROL = 4010;
	
	/**
	 * porta su cui � in ascolto il componente responsabile dell'attivazione del proxy
	 */
	//public static final int PROXY_ACTIVATOR_PORT = 4000;
	
	/**
	 * porta su cui � in ascolto il componente responsabile dell'attivazione del client
	 */
	public static final int CLIENT_ACTIVATOR_PORT = 3000;
	
	/**
	 * porta su cui � in ascolto il main thread del server
	 * per la ricezione di richieste ddi file da parte del client
	 */
	public static final int SERVER_REQUEST = 9000;
	
	/**
	 * numero di pacchetti che costituiscono il treno
	 */
	public static final int TRAIN_LENGTH = 61;
	
	/**
	 * indirizzo IP del server
	 */
	public static final String SERVER_ADDRESS = "192.168.70.109";
	//public static final String SERVER_ADDRESS = "192.255.0.1";
	//public static final String SERVER_ADDRESS = "127.0.1.1";

	
	/**
	 * indirizzo IP del proxy
	 */
	//public static final String PROXY_ADDRESS = "192.168.70.81";
	//public static final String PROXY_ADDRESS = "192.255.0.1";
	//public static final String PROXY_ADDRESS = "127.0.1.1";

	/**
	 *indirizzo IP statico del client usato dal DummyController
	 *usare l'indirizzo esterno sulla LAN della propria scheda di rete ethernet o wifi
	 *e non 127.0.0.1 per inoltrare comunque i pacchetti sulla LAN
	 *se si vuole testare il funzionamento della LAN
	 */

	public static final String CLIENT_ADDRESS = "192.168.70.109";
	//public static final String CLIENT_ADDRESS = "192.255.0.2";
	//public static final String CLIENT_ADDRESS= "127.0.1.1";
	
	/**
	 * Indica se la predizione dell'handoff � abilitata
	 */
	public static final boolean enableHandoff=false;
	
	/**
	 * timeout per la ricezione del messaggio di errore
	 */
	public static final int TIMEOUT = 7000;
	
	/**
	 * valore minimo di throughput
	 */
	public static final double MIN_THROUGHPUT = 0.5;

	/**
	 * soglia di sicurezza per il calcolo della durata dei periodi di off (low water)
	 */
	public static final int SECURITY_THRESHOLD = 0;
	
	/**
	 * dimensione media in bytes dei pacchetti del flusso RTP
	 */
	public static final int PACKET_SIZE = 1270;
	
	/**
	 * numero di pacchetti che devono essere spediti al client prima dello start point
	 */
	public static final int SP_PACK = 100;
	
	/**
	 * frequenza di monitoraggio degli handoff in millisecondi
	 */
	public static final long handOffMonitoringTime=8000;
	
	/**
	 * Istante di previsione dell'handoff in secondi
	 */
	public static final int preditioTimeSec=12;
	
	/**
	 * numero di predizioni positive prima di inviare un handoff
	 */
	public static final int numberOfPrediction=3;
	
	/**
	 * soglia di sicurezza nel calcolo dei tempi dell'handoff
	 */
	public static final long handoff_security_thrs=2000;
	
	/**
	 * histeresys prediction threshold
	 * soglia che fa scattare l'alta probabilita' di handoff
	 */
	public static final int HPT=10;
	
	/**
	 * histeresys inferior threshold
	 * soglia per determinare l'istante dell'handoff vero e proprio
	 */
	public static final int HHT=6;
	
	/**
	 * numero degli RSSI precedenti da tenere memorizzati per ogni access point
	 */
	public static final int NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT = 10;
	
	/**
	 * Tempo minimo fra due letture dal file rssi.txt nel caso si usi il DummyController
	 */
	public static final long updateTime=5000;
	
	/**
	 * Porta su cui viene effettuato il probing per la stima della banda
	 */
	//public static final int proxyProbingPort = 10202;
	
	public static final int TTWTransmit = 10;
	
	public static final int TTW = 70;
	
	/*****************CODICI MESSAGGI*******************/
	
	//codice errore generico
	public static final int ERROR = 0;
	
	//codice richiesta attivazione
	public static final int ACTIVATE = 1;
	
	//codice acknowledge generico
	public static final int ACK = 2;
	
	//codice acknowledge generico
	//public static final int TX_STARTED = 3;
	
	//codice inizio periodo di off
	public static final int START_OFF = 4;
	
	//codice fine periodo di off
	public static final int START_ON = 5;
	
	//codice richiesta sospensione trasmissione per basso throughput
	public static final int LOW_THROUGHPUT = 6;
	
	//codice richiesta sospensione trasmissione per buffer pieno
	public static final int BUFFER_FULL = 7;
	
	//codice prosecuzione trasmissione
	public static final int HANDOFF = 8;
	
	//codice inizio playback
	public static final int START_PLAYBACK = 9;
	
	//richiesta di invio file da parte del client
	public static final int FILE_REQUEST = 10;
	
	//conferma di avvenuta ricezione della richiesta di invio file
	public static final int CONFIRM_REQUEST = 11;
	
	//codice conferma attivazione del proxy
	public static final int CONFIRM_ACTIVATION = 12;
	
	//codice stima della banda
	public static final int BAND_ESTIMATION = 13;
	
	//codice richiesta stima della banda
	public static final int BAND_ESTIMATION_REQ = 14;
	
	//codice richiesta files
	public static final int FILES_REQ = 15;
	
	//codice risultato lista file
	public static final int FILES_RESPONSE = 16;
	
}