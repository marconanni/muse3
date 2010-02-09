package parameters;

public class SessionConfiguration {
	
	/**
	 * dimensione del buffer lato proxy
	 */
	public static final int PROXY_BUFFER = 80;
	
	/**
	 * dimensione del buffer lato client
	 */
	public static final int CLIENT_BUFFER = 100;
	
	/**
	 * Specifica il numero di sessioni attive massime che si possono avere sul Relay 
	 */
	public static final int RANGE_ACTIVE_SESSIONS = 1000;

	
	/**
	 * Specifica la cadenza con la quale vengono prelevati i frame dal buffer circolare da parte del multiplexer
	 */
	public static final int TTW = 20;
	
	public static final int PLAYBACK_DELAY_START = 20000;
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno STOP_TX al PROXY
	 */
	public static final int BUFFER_THS_STOP_TX = 60;
	
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno START_TX al PROXY
	 */
	public static final int BUFFER_THS_START_TX = 0;
	

	//public static final int BUFFER_THS_START_POINT = 60;
	public static final int BUFFER_THS_START_POINT = 50;


}
