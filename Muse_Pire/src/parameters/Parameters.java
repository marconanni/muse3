package parameters;

public class Parameters {
	
	/*
	 * ************************* DEBUG MESSAGE *************************************
	 * */	
	public static final int DEBUG_INFO=0;
	public static final int DEBUG_WARNING=2;
	public static final int DEBUG_ERROR=3;
	
	/*
	 ********************* CONFIGURAZIONE DELLA RETE *****************************
	 * 
	 * */	
	/** indirizzo IP del SERVER */
	public static final String SERVER_ADDRESS = "192.168.0.4";
	//public static final String SERVER_ADDRESS = "192.255.0.1";
	
	/** indirizzo IP del CLIENT */
	//public static final String CLIENT_ADDRESS = "192.168.1.11";
	public static final String CLIENT_ADDRESS = "192.168.0.4";
	//public static final String CLIENT_ADDRESS = "localhost";
	
	/** indirizzo IP del RELAY sulla rete Ad-Hoc */
	//public static final String RELAY_AD_HOC_ADDRESS = "192.168.2.2";
	public static final String RELAY_AD_HOC_ADDRESS = "192.168.0.4";
	//public static final String RELAY_AD_HOC_ADDRESS = "localhost";
	
	/** indirizzo IP del RELAY sulla rete Managed */
	//public static final String RELAY_MANAGED_ADDRESS = "192.168.2.2";
	public static final String RELAY_MANAGED_ADDRESS = "192.168.0.4";
	
	/** indirizzo IP di BROADCAST sulla rete Ad-Hoc */
	//public static final String BROADCAST_ADDRESS = "192.168.1.255";
	public static final String BROADCAST_ADDRESS = "192.168.0.255"; 
	
	/** Nome della rete Ad-Hoc */
	public static final String NAME_OF_AD_HOC_NETWORK = "marco";
	
	/** Nome della rete Managed */
	public static final String NAME_OF_MANAGED_NETWORK = "MUSE2_SSID";
	
	/** Nome dell'interfaccia con cui il Relay si affaccia sulla rete Ad-Hoc */
	public static final String NAME_OF_AD_HOC_RELAY_INTERFACE = "wlan0";
	
	/**Nome dell'interfaccia con cui il Relay si affaccia sulla rete Managed*/
	public static final String NAME_OF_MANAGED_RELAY_INTERFACE  = "wlan2";
	
	/** Nome dell'interfaccia con cui il Client si affaccia sulla rete Ad-Hoc */
	public static final String NAME_OF_CLIENT_INTERFACE = "wlan0";
	
	/** Nome dell'interfaccia con cui il Server si affaccia sulla rete Managed (cablato all'AP) */
	public static final String NAME_OF_SERVER_INTERFACE = "eth0";
	
	/*
	 ********************* CONFIGURAZIONE RELAY *****************************
	 * 
	 * */	
	/**Discriminazione di due tipi di relay
	 * relay principale (collegato col nodo server) -> BIG BOSS	IMRELAY = true && IMBIGBOSS = true
	 * relay secondario (collegato col nodo BIG BOSS) 			IMRELAY = true && IMBIGBOSS = false
	 */
	public static final boolean IMRELAY=false;
	public static final boolean IMBIGBOSS=false;
	
	/** Specifica il numero di sessioni attive massime che si possono avere sul Relay */
	public static final int RANGE_ACTIVE_SESSIONS = 1000;
	
	/*
	 ********************* PORTE DI COMUNICAZIONE *****************************
	 * 
	 * */	
	//SERVER
	/** porte utilizzate per la gestione della sessione sul SERVER*/
	public static final int SERVER_SESSION_PORT_IN = 2001;
	
	/** valori iniziali dei range di porte di controllo ingresso-uscita 
	 *  assegnabili ad uno STREAMING SERVER creato per gestire una sessione RTP
	 */
	public static final int STREAMINGSERVER_INITIAL_PORT_IN_OUT_CONTROL = 10000;
		
	/** valori iniziali dei range di porte RTP in uscita
	 *  assegnabili ad uno STREAMINGSERVER creato per gestire una sessione RTP 
	 */
	public static final int STREAMINGSERVER_INITIAL_PORT_OUT_RTP = 11000;
	
	//CLIENT
	/** porta per la gestione della sessione sui CLIENT in ingresso */
	public static final int CLIENT_PORT_SESSION_IN = 3000;
	
	/** porta per la gestione della sessione sui CLIENT in uscita */
	public static final int CLIENT_PORT_SESSION_OUT = 3001;
		
	/** porta per la gestione della elezione sui CLIENT in ingresso */
	public static final int CLIENT_PORT_ELECTION_IN = 3002;
	
	/** porta per la gestione della elezione sui CLIENT in uscita.
	 * Attraverso questa porta  il CLIENT spedisce anche i messaggi
	 * WHO_IS_RELAY */
	public static final int CLIENT_PORT_ELECTION_OUT = 3003;
	
	/** porta su cui il CLIENT riceve i messaggi di REQUEST_RSSI
	 *  e la porta da cui invia NOTIFY_RSSI al RELAY
	 */
	public static final int CLIENT_RSSI_PORT_IN = 3004;
	public static final int CLIENT_RSSI_PORT_OUT = 3005;
	
	/** porta per la ricezione del flusso RTP sul CLIENT */
	public static final int CLIENT_PORT_RTP_IN = 3006;
	
	//RELAY E PROXY
	/** porta su cui il Thread periodico del Relay Attuale ascolta i messaggi WHO_IS_RELAY
	 *  e invia subito dopo la risposta IM_RELAY al mittente del WHO_IS_RELAY */
	public static final int WHO_IS_RELAY_PORT_IN= 4000;
	public static final int WHO_IS_RELAY_PORT_OUT = 4001;
	
	/** porta per la gestione della sessione sul RELAY verso la rete ad-hoc in ingresso */
	public static final int RELAY_SESSION_AD_HOC_PORT_IN = 4002;
	
	/** porta per la gestione della sessione sul RELAY verso la rete ad-hoc in uscita */
	public static final int RELAY_SESSION_AD_HOC_PORT_OUT = 4003;
		
	/** porta per la gestione della sessione sul RELAY verso il SERVER, in uscita */
	public static final int RELAY_SESSION_MANAGED_PORT_OUT = 4004;
			
	/** porta per la gestione della elezione sul RELAY, rispettivamente in ingresso */
	public static final int RELAY_ELECTION_PORT_IN = 4005;
		
	/** porta per la gestionee della elezione sul RELAY, rispettivamente in uscita */
	public static final int RELAY_ELECTION_PORT_OUT = 4006;
		
	/** porta su cui il RELAY riceve i messaggi di NOTIFY_RSSI inviati dai CLIENTS */
	public static final int RELAY_RSSI_RECEIVER_PORT = 4007;
	
	/** porta da cui il relay invia i REQUEST_RSSI ai CLIENTS */
	public static final int RELAY_RSSI_SENDER_PORT = 4008;
	
	/** valori iniziali dei range di porte RTP in ingresso 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	public static final int PROXY_INITIAL_PORT_IN_RTP = 5000;
	
	/** valori iniziali dei range di porte RTP in uscita 
	 * assegnabili ad un PROXY creato per gestire una sessione RTP */
	public static final int PROXY_INITIAL_PORT_OUT_RTP = 6000;
		
	/** valori iniziali dei range di porte di controllo in ingresso dalla rete ad-hoc
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	public static final int PROXY_INITIAL_AD_HOC_PORT_IN_CONTROL = 7000;
	
	/** valori iniziali dei range di porte di controllo in uscita dalla rete ad-hoc 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	public static final int PROXY_INITIAL_AD_HOC_PORT_OUT_CONTROL = 8000;
	
	/** valori iniziali dei range di porte di controllo in ingresso/uscita sulla rete managed 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	public static final int PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL = 9000;
	
	/*
	 ********************* CONFIGURAZIONE BUFFER *****************************
	 * 
	 * */
	/** dimensione del buffer lato proxy */
	public static final int PROXY_BUFFER = 150;
	
	/** dimensione del buffer lato client */
	public static final int CLIENT_BUFFER = 100;
	
	/** Specifica la cadenza con la quale vengono prelevati i frame dal buffer circolare da parte del multiplexer */
	public static final int TTW = 65;
	
	public static final int PLAYBACK_DELAY_START = 20000;
	
	/*
	 ***************** CODICI MESSAGGI DI COMUNICAZIONE **********************
	 * 
	 * */
	public static final int WHO_IS_BIGBOSS = 0;
	public static final int IM_BIGBOSS = 1;
	public static final int WHO_IS_RELAY = 2;
	public static final int IM_RELAY = 3;

	public static final int ELECTION_REQUEST = 2;
	public static final int ELECTION_BEACON = 3;
	public static final int ELECTION_RESPONSE = 4;
	public static final int ELECTION_DONE = 5;
	
	public static final int EM_EL_DET_CLIENT = 6;
	public static final int EM_EL_DET_RELAY = 7;
	public static final int EM_ELECTION = 8;
	
	public static final int REQUEST_SESSION= 9;
	public static final int SESSION_INFO = 10;
	public static final int ACK_SESSION = 11;
	public static final int REDIRECT = 12;
	public static final int LEAVE = 13;
	
	public static final int REQUEST_RSSI = 20;
	public static final int NOTIFY_RSSI = 21;


	public static final int REQUEST_FILE = 14;
	public static final int FORWARD_REQ_FILE = 15;		//Richiesta file al server
	public static final int ACK_RELAY_FORW = 16;
	public static final int ACK_CLIENT_REQ = 17;
	
	public static final int START_TX = 18;
	public static final int STOP_TX = 19;
	
	
	
	public static final int ACK = 22;	
	
	/*
	 * AGGIUNTO DA CARLO 4-12-2008 13.50
	 */
	public static final int SERVER_UNREACHEABLE = 23;
	public static final int SESSION_INVALIDATION = 24;
		
	/*
	 ********************* SETTAGGIO DEI TIMEOUT E PERIODI **************************
	 * 
	 * */
	/** Periodo con cui il RelayPositionClientsMonitor richiede ai Clients serviti il valore di RSSI rilevato nei suoi confronti */
	public static final long POSITION_CLIENTS_MONITOR_PERIOD = 4000;
	
	/** Periodo con cui il RelayPositionAPMonitor richiede al RelayWNICController il valore di RSSI rilevato nei confronti dell'AP */
	public static final long POSITION_AP_MONITOR_PERIOD = 4000;
		
	/** Periodo con cui il RelayBatteryMonitor rileva la carica rimanente nella batteria */
	public static final long BATTERY_MONITOR_PERIOD = 4000;
	
	/** timeout relativo al messaggio WHO_IS_RELAY */
	public static final int TIMEOUT_SEARCH = 10000;
	
	/** timeout relativo all'intera fase di elezione */
	public static final int TIMEOUT_FAIL_TO_ELECT = 4000;
	
	/** timeout relativo al messaggio FILE_REQ */
	public static final int TIMEOUT_FILE_REQUEST = 35000;
		
	/** timeout relativo all'intera fase di elezione */
	public static final int TIMEOUT_TO_ELECT = 3000;
	
	/** timeout relativo al messaggio di risposta dei client al messagigo ELECTION_REQUEST */
	public static final int TIMEOUT_ELECTION_BEACON = 1000;
	
	/** timeout relativo alla prima fase del protocollo di emergenza */
	public static final int TIMEOUT_CLIENT_DETECTION = 2000;
	
	/** timeout relativo all'intera fase di elezione d'emergenza */
	public static final int TIMEOUT_EM_ELECTION = 4000;
	
	/** timeout relativo all'attesa del messaggio di SESSION_REQUEST */
	public static final int TIMEOUT_SESSION_REQUEST = 1000;
	
	/** timeout relativo al messaggio di SESSION_INFO */
	public static final int TIMEOUT_SESSION_INFO = 1000;
	
	/** timeout relativo al messaggio di ACK_SESSION_INFO */
	public static final int TIMEOUT_ACK_SESSION_INFO = 2000;
	
	/** timeout relativo al messaggio ACK_CLIENT_REQ */
	public static final int TIMEOUT_ACK_CLIENT_REQ = 200000;
	
	/** timeout relativo all'interruzione della sessione da parte del client */
	public static final int TIMEOUT_SESSION_INTERRUPTED = 120000;
	
	/** timeout relativo al messaggio di ACK_FORWARD */
	public static final int TIMEOUT_ACK_FORWARD = 60000;
	
	/** timeout relativo al messaggio di NOTIFY_RSSI */
	public static final int TIMEOUT_NOTIFY_RSSI = 1000;
	
	/*
	 ************ SOGLIE DI DISCONNESSIONE E DI RIELEZIONE ********************
	 * 
	 * */
	/**Soglia superata la quale si considera molto probabile il presentarsi di una disconnessione dall'AP */
	public static final double AP_DISCONNECTION_THRS = 69;
	
	/** Numero massimo di riscontri positivi per la disconnessione dall'AP raggiunto 
	 *  il quale la disconnessione è considerata come certa */
	public static final int NUMBER_OF_AP_DISCONNECTION_DETECTION = 1;
	
	/** Soglia al di sopra della quale un valore RSSI non viene considerato nel calcolo della media
	 *  degli RSSI rilevati dai Clients serviti nei confronti del Relay attuale */
	public static final double VALID_RSSI_THRS = 65;
	
	/** Soglia superata la quale si considera molto probabile il presentarsi di una disconnessione
	 *  dai Clients che il Relay attuale sta servendo. */
	public static final double CLIENTS_DISCONNECTION_THRS = 69;
	
	/** Numero massimo di riscontri positivi per la disconnessione dai Clients serviti raggiunto 
	 *  il quale tale disconnessione è considerata come certa */
	public static final double NUMBER_OF_CLIENTS_DISCONNECTION_DETECTION = 1;
		
	/*
	 *************** PARAMETRI PER IL CALCOLO DEL PESO W **********************
	 * 
	 * */
	/** Soglia al di sotto della quale si considera la batteria come esaurita */
	public static final double BATTERY_LOW_THRS = 0.20;
	
	/** Peso da assegnare al numero di Clients rilevati dal possibile Relay nel calcolo del W complessivo del nodo */
	public static final double W_OF_NUMBER_OF_CLIENTS = 5;
	
	/** Peso da assegnare all'inverso del valore di RSSI rilevato nei confronti dell'AP nel calcolo del W complessivo del nodo */
	public static final double W_OF_INVERSE_RSSI_AP_VALUE = 4;
	
	/** Peso da assegnare al livello attuale della batteria nel calcolo del W complessivo del nodo */
	public static final double W_OF_BATTERY_LEVEL = 3;
	
	/*
	 **************************** PARAMETRI VARI ***************************
	 * 
	 * */
	/** Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna inviare uno STOP_TX al PROXY */
	//public static final int BUFFER_THS_STOP_TX = 60;
		
	/** Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna inviare uno START_TX al PROXY */
	//public static final int BUFFER_THS_START_TX = 35;
	//public static final int BUFFER_THS_START_POINT = 67;
	
	/** Caratterizzazione numerica della capacità in bpms dell'interfaccia WIFI connessa alla rete Ad-Hoc */
	public static final double CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE = 1;
	
	/** Caratterizzazione numerica della capacità in bpms dell'interfaccia WIFI connessa alla rete Managed */
	public static final double CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE = 1;

	/** Peso da assegnare alla caratteristica dell'interfaccia di rete connessa alla rete Ad-Hoc nel calcolo del W complessivo del nodo */
	public static final double W_OF_CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE = 1;
	
	/** Peso da assegnare alla caratteristica dell'interfaccia di rete connessa alla rete Managed nel calcolo del W complessivo del nodo */
	public static final double W_OF_CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE = 1;
		
	/** Numero di campioni da memorizzare nella classe RelayPositionClientsMonitor affinchè il Grey Model possa effettuare la previsione*/
	public static final int NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL = 15;
		
	/** Numero di campioni da memorizzare nella classe AccessPointData affinchè il Grey Model possa effettuare la previsione */
	public static final int NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL = 15;



/*	*//**
	 * Indica se la predizione dell'handoff � abilitata
	 *//*
	public static final boolean enableHandoff=false;
	
	*//**
	 * timeout per la ricezione del messaggio di errore
	 *//*
	public static final int TIMEOUT = 7000;
	
	*//**
	 * valore minimo di throughput
	 *//*
	public static final double MIN_THROUGHPUT = 0.5;

	*//**
	 * soglia di sicurezza per il calcolo della durata dei periodi di off (low water)
	 *//*
	public static final int SECURITY_THRESHOLD = 0;
	
	*//**
	 * dimensione media in bytes dei pacchetti del flusso RTP
	 *//*
	public static final int PACKET_SIZE = 1270;
	
	*//**
	 * numero di pacchetti che devono essere spediti al client prima dello start point
	 *//*
	public static final int SP_PACK = 100;
	
	*//**
	 * frequenza di monitoraggio degli handoff in millisecondi
	 *//*
	public static final long handOffMonitoringTime=8000;
	
	*//**
	 * Istante di previsione dell'handoff in secondi
	 *//*
	public static final int preditioTimeSec=12;
	
	*//**
	 * numero di predizioni positive prima di inviare un handoff
	 *//*
	public static final int numberOfPrediction=3;
	
	*//**
	 * soglia di sicurezza nel calcolo dei tempi dell'handoff
	 *//*
	public static final long handoff_security_thrs=2000;
	
	*//**
	 * histeresys prediction threshold
	 * soglia che fa scattare l'alta probabilita' di handoff
	 *//*
	public static final int HPT=10;
	
	*//**
	 * histeresys inferior threshold
	 * soglia per determinare l'istante dell'handoff vero e proprio
	 *//*
	public static final int HHT=6;
	
	*//**
	 * numero degli RSSI precedenti da tenere memorizzati per ogni access point
	 *//*
	public static final int NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT = 10;
	
	*//**
	 * Tempo minimo fra due letture dal file rssi.txt nel caso si usi il DummyController
	 *//*
	public static final long updateTime=5000;
	
	*//**
	 * Porta su cui viene effettuato il probing per la stima della banda
	 *//*
	public static final int proxyProbingPort = 10202;
	
	public static final int TTWTransmit = 10;
	
	public static final int TTW = 70;*/
	

}