package parameters;

public class ElectionConfiguration {

	/*
	 ************ SOGLIE DI DISCONNESSIONE E DI RIELEZIONE ********************
	 * 
	 * */
	
	/**Soglia superata la quale si considera molto probabile il presentarsi di una disconnessione dall'AP */
	public static final double AP_DISCONNECTION_THRS = 70;
	
	/** Numero massimo di riscontri positivi per la disconnessione dall'AP raggiunto 
	 *  il quale la disconnessione è considerata come certa */
	public static final int NUMBER_OF_AP_DISCONNECTION_DETECTION = 3;
	
	/** Soglia al di sopra della quale un valore RSSI non viene considerato nel calcolo della media
	 *  degli RSSI rilevati dai Clients serviti nei confronti del Relay attuale */
	public static final double VALID_RSSI_THRS = 70;
	
	/** Soglia al di sotto della quale si considera la batteria come esaurita */
	public static final double BATTERY_LOW_THRS = 0.10;
	
	/** Soglia superata la quale si considera molto probabile il presentarsi di una disconnessione
	 *  dai Clients che il Relay attuale sta servendo. */
	public static final double CLIENTS_DISCONNECTION_THRS = 40;
	
	/** Numero massimo di riscontri positivi per la disconnessione dai Clients serviti raggiunto 
	 *  il quale tale disconnessione è considerata come certa */
	public static final double NUMBER_OF_CLIENTS_DISCONNECTION_DETECTION =4;
	
	/*
	 *************** PARAMETRI PER IL CALCOLO DEL PESO W **********************
	 * 
	 * */	
	/** Peso da assegnare al numero di Clients rilevati dal possibile Relay nel calcolo del W complessivo del nodo */
	public static final double W_OF_NUMBER_OF_CLIENTS = 5;
	
	/** Peso da assegnare all'inverso del valore di RSSI rilevato nei confronti dell'AP nel calcolo del W complessivo del nodo */
	public static final double W_OF_INVERSE_RSSI_AP_VALUE = 4;
	
	/** Peso da assegnare al livello attuale della batteria nel calcolo del W complessivo del nodo */
	public static final double W_OF_BATTERY_LEVEL = 3;
	
	/** Numero di campioni da memorizzare nella classe RelayPositionClientsMonitor affinchè il Grey Model possa effettuare la previsione*/
	public static final int NUMBER_OF_SAMPLE_FOR_CLIENTS_GREY_MODEL = 5;
		
	/** Numero di campioni da memorizzare nella classe AccessPointData affinchè il Grey Model possa effettuare la previsione */
	public static final int NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL = 10;
	
	/**Caratterizzazione numerica della capacità in bpms dell'interfaccia WIFI connessa alla rete Ad-Hoc
	 */
	public static final double CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE = 1;
	
	/**Caratterizzazione numerica della capacità in bpms dell'interfaccia WIFI connessa alla rete Managed
	 */
	public static final double CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE = 1;
	/**Peso da assegnare alla caratteristica dell'interfaccia di rete connessa alla rete Ad-Hoc nel calcolo del W complessivo
	 * del nodo
	 */
	public static final double W_OF_CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE = 1;
	
	/**Peso da assegnare alla caratteristica dell'interfaccia di rete connessa alla rete Managed nel calcolo del W complessivo
	 * del nodo
	 */
	public static final double W_OF_CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE = 1;
	
	
	
}
