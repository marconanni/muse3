package parameters;

public class PortConfiguration {

	//SERVER
	/** porte utilizzate per la gestione della sessione sul SERVER*/
//	public static final int SERVER_SESSION_PORT_IN = 2001;
	
	/** valori iniziali dei range di porte di controllo ingresso-uscita 
	 *  assegnabili ad uno STREAMING SERVER creato per gestire una sessione RTP
	 */
	//public static final int STREAMINGSERVER_INITIAL_PORT_IN_OUT_CONTROL = 10000;
		
	/** valori iniziali dei range di porte RTP in uscita
	 *  assegnabili ad uno STREAMINGSERVER creato per gestire una sessione RTP 
	 */
//	public static final int STREAMINGSERVER_INITIAL_PORT_OUT_RTP = 11000;
	
	//CLIENT
	/** porta per la gestione della sessione sui CLIENT in ingresso */
	//public static final int CLIENT_PORT_SESSION_IN = 3000;
	
	/** porta per la gestione della sessione sui CLIENT in uscita */
//	public static final int CLIENT_PORT_SESSION_OUT = 3001;
		
	/** porta per la gestione della elezione sui CLIENT in ingresso */
	public static final int CLIENT_PORT_ELECTION_IN = 3002;
	
	/** porta per la gestione della elezione sui CLIENT in uscita.
	 * Attraverso questa porta  il CLIENT spedisce anche i messaggi
	 * WHO_IS_RELAY */
	public static final int CLIENT_PORT_ELECTION_OUT = 3003;
	
	/** porta per la ricezione del flusso RTP sul CLIENT */
//	public static final int CLIENT_PORT_RTP_IN = 3006;
	
	//RELAY E PROXY
	/** porta su cui il Thread periodico del Relay Attuale ascolta i messaggi WHO_IS_RELAY
	 *  e invia subito dopo la risposta IM_RELAY al mittente del WHO_IS_RELAY */
	public static final int WHO_IS_RELAY_PORT_IN= 4000;
	public static final int WHO_IS_RELAY_PORT_OUT = 4001;
	
	/** porta per la gestione della sessione sul RELAY verso la rete ad-hoc in ingresso */
	//public static final int RELAY_SESSION_AD_HOC_PORT_IN = 4002;
	
	/** porta per la gestione della sessione sul RELAY verso la rete ad-hoc in uscita */
	//public static final int RELAY_SESSION_AD_HOC_PORT_OUT = 4003;
		
	/** porta per la gestione della sessione sul RELAY verso il SERVER, in uscita */
	//public static final int RELAY_SESSION_MANAGED_PORT_OUT = 4004;
			
	/** porta per la gestione della elezione sul RELAY, rispettivamente in ingresso */
	public static final int RELAY_ELECTION_CLUSTER_PORT_IN = 4005;
		
	/** porta per la gestionee della elezione sul RELAY, rispettivamente in uscita */
	public static final int RELAY_ELECTION_CLUSTER_PORT_OUT = 4006;
	
	/** porta per la gestione dei messaggi nei confronti del cluster head (relay<-->bigboss) */
	public static final int RELAY_ELECTION_CLUSTER_HEAD_PORT_IN = 4005;
		
	/** porta per la gestionee della elezione sul RELAY, rispettivamente in uscita */
	public static final int RELAY_ELECTION_CLUSTER_HEAD_PORT_OUT = 4006;
		
	/** valori iniziali dei range di porte RTP in ingresso 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	//public static final int PROXY_INITIAL_PORT_IN_RTP = 5000;
	
	/** valori iniziali dei range di porte RTP in uscita 
	 * assegnabili ad un PROXY creato per gestire una sessione RTP */
	//public static final int PROXY_INITIAL_PORT_OUT_RTP = 6000;
		
	/** valori iniziali dei range di porte di controllo in ingresso dalla rete ad-hoc
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	//public static final int PROXY_INITIAL_AD_HOC_PORT_IN_CONTROL = 7000;
	
	/** valori iniziali dei range di porte di controllo in uscita dalla rete ad-hoc 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	//public static final int PROXY_INITIAL_AD_HOC_PORT_OUT_CONTROL = 8000;
	
	/** valori iniziali dei range di porte di controllo in ingresso/uscita sulla rete managed 
	 *  assegnabili ad un PROXY creato per gestire una sessione RTP */
	//public static final int PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL = 9000;
	
	//Client && Relay
	/** porta su cui il CLIENT e RELAY riceve i messaggi di REQUEST_RSSI
	 *  e la porta da cui invia NOTIFY_RSSI al RELAY corrispondente*/
	public static final int RSSI_PORT_IN = 3004;
	public static final int RSSI_PORT_OUT = 3005;
}
