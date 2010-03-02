package parameters;

public class BufferConfiguration {
	
	/**
	 * caratteristiche del buffer lato proxy
	 */
	public static final int PROXY_BUFFER = 120;
	
	public static final int PROXY_SOGLIA_INFERIORE_NORMAL  = 20;
	
	public static final int PROXY_SOGLIA_INFERIORE_ELECTION  = 80;
	
	public static final int PROXY_SOGLIA_SUPERIORE_NORMAL  = 80;
	
	public static final int PROXY_SOGLIA_SUPERIORE_ELECTION  = 80;

	
	
	/**
	 * dimensione del buffer lato client
	 */
	public static final int CLIENT_BUFFER = 250;
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno STOP_TX al PROXY
	 */
	public static final int BUFFER_THS_STOP_TX = 200;
	/**
	 * Indica il valore di frames contenuti nel buffer del CLIENT raggiunto il quale bisogna 
	 * inviare uno START_TX al PROXY
	 */
	public static final int BUFFER_THS_START_TX = 40;
	
	/**
	 * indica il numero di frame raggiunto il quale inizia la riproduzione del brano
	 */ 
	public static final int BUFFER_THS_START_POINT = 45;
	
	
	/**
	 * Specifica la cadenza con la quale vengono prelevati i frame dal buffer circolare da parte del multiplexer
	 */
	public static final int TTW = 65;

}
