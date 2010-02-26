package parameters;

public class BufferConfiguration {
	
	/**
	 * caratteristiche del buffer lato proxy
	 */
//	public static final int PROXY_BUFFER = 160;
//	
//	public static final int PROXY_SOGLIA_INFERIORE_NORMAL  = 0;
//	
//	public static final int PROXY_SOGLIA_INFERIORE_ELECTION  = 80;
//	
//	public static final int PROXY_SOGLIA_SUPERIORE_NORMAL  = 80;
//	
//	public static final int PROXY_SOGLIA_SUPERIORE_ELECTION  = 80;
	public static final int PROXY_BUFFER = 300;
	
	public static final int PROXY_SOGLIA_INFERIORE_NORMAL  = 250;
	
	public static final int PROXY_SOGLIA_INFERIORE_ELECTION  = 0;
	
	public static final int PROXY_SOGLIA_SUPERIORE_NORMAL  = 400;
	
	public static final int PROXY_SOGLIA_SUPERIORE_ELECTION  = 0;
	
	
	/**
	 * dimensione del buffer lato client
	 */
	public static final int CLIENT_BUFFER = 300;
	
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno STOP_TX al PROXY
	 */
	public static final int BUFFER_THS_STOP_TX = 250;
	
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno START_TX al PROXY
	 */
	public static final int BUFFER_THS_START_TX = 30;
	

	public static final int BUFFER_THS_START_POINT = 40;
	
	
	/**
	 * Specifica la cadenza con la quale vengono prelevati i frame dal buffer circolare da parte del multiplexer
	 */
	public static final int TTW = 65;
	
//	public static final int PLAYBACK_DELAY_START = 2000;
}
