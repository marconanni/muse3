package parameters;

/**Classe che permette di impostare i vari timeout
 * @author Pire Dejaco, Valerio Sandri, Marco Nanni
 * @version 1.1
 */
public class TimeOutConfiguration {

	/** Periodo con cui il RelayPositionClientsMonitor richiede ai Clients serviti il valore di RSSI rilevato nei suoi confronti */
	public static final long POSITION_CLIENTS_MONITOR_PERIOD = 4000;
	
	/** Periodo con cui il RelayPositionAPMonitor richiede al RelayWNICController il valore di RSSI rilevato nei confronti dell'AP */
	public static final long POSITION_AP_MONITOR_PERIOD = 4000;
		
	/** Periodo con cui il RelayBatteryMonitor rileva la carica rimanente nella batteria */
	public static final long BATTERY_MONITOR_PERIOD = 4000;
	
	/** timeout relativo al messaggio WHO_IS_RELAY */
	public static final int TIMEOUT_SEARCH = 10000;
	public static final String TIME_OUT_SEARCH = "TIMEOUTSEARCH";
		
	/** timeout relativo all'intera fase di elezione */
	public static final int TIMEOUT_FAIL_TO_ELECT = 5000;
	public static final String TIME_OUT_FAIL_TO_ELECT = "TIMEOUTFAILTOELECT";
	
	/** timeout relativo al messaggio FILE_REQ */
	public static final int TIMEOUT_FILE_REQUEST = 35000;
	public static final String TIME_OUT_FILE_REQUEST = "TIMEOUTFILEREQUEST";
		
	/** timeout relativo all'intera fase di elezione */
	public static final int TIMEOUT_TO_ELECT = 4000;
	public static final String TIME_OUT_TO_ELECT = "TIMEOUTTOELECT";
	
	/** timeout relativo al messaggio di risposta dei client al messagigo ELECTION_REQUEST */
	public static final int TIMEOUT_ELECTION_BEACON = 1500;
	public static final String TIME_OUT_ELECTION_BEACON = "TIMEOUTELECTIONBEACON";
	
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
	public static final String TIME_OUT_NOTIFY_RSSI = "TIMEOUTNOTIFYRSSI";
}
