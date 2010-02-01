package parameters;

public class BufferConfiguration {
	
	/**
	 * dimensione del buffer lato proxy
	 */
	public static final int PROXY_BUFFER = 150;
	
	/**
	 * dimensione del buffer lato client
	 */
	public static final int CLIENT_BUFFER = 100;
	
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno STOP_TX al PROXY
	 */
	public static final int BUFFER_THS_STOP_TX = 60;
	
	
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno START_TX al PROXY
	 */
	public static final int BUFFER_THS_START_TX = 35;
	

	public static final int BUFFER_THS_START_POINT = 67;
	
	
	/**
	 * Specifica la cadenza con la quale vengono prelevati i frame dal buffer circolare da parte del multiplexer
	 */
	public static final int TTW = 65;
	
	public static final int PLAYBACK_DELAY_START = 20000;
}
