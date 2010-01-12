package parameters;

public class NetConfiguration {
	
	/*
	 ********************* CONFIGURAZIONE RELAY *****************************
	 * 
	 * */	
	/**Discriminazione di due tipi di relay
	 * relay principale (collegato col nodo server) -> BIG BOSS	IMRELAY = true && IMBIGBOSS = true
	 * relay secondario (collegato col nodo BIG BOSS) 			IMRELAY = true && IMBIGBOSS = false
	 */
	public static final boolean IMRELAY=true;
	public static final boolean IMBIGBOSS=true;
	
	/*
	 ********************* CONFIGURAZIONE DELLA RETE *****************************
	 * 
	 * */	
	/** indirizzo IP del SERVER */
	public static final String SERVER_ADDRESS = "192.168.2.4";
	
	/** indirizzo IP del CLIENT */
	public static final String CLIENT_ADDRESS = "192.168.0.4";
	
	/** indirizzo IP del RELAY nella rete Ad-Hoc di sua competenza (cluster)*/
	public static final String RELAY_AD_HOC_CLUSTER_ADDRESS = "192.168.0.2";					//non server al bigboss

	/** indirizzo IP del RELAY nella rete Ad-Hoc di competenza del BigBoss (cluster head)*/
	public static final String RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS = "192.168.0.2";
	
	/** indirizzo IP del RELAY sulla rete Managed (BigBoss) */
	public static final String RELAY_MANAGED_ADDRESS = "192.168.2.4";							//non server al relay attivo
	
	/** indirizzo IP di BROADCAST sulla rete Ad-Hoc (cluster) */
	public static final String CLUSTER_BROADCAST_ADDRESS = "192.168.0.255"; 
	
	/** indirizzo IP di BROADCAST sulla rete Ad-Hoc (cluster head) */
	public static final String CLUSTER_HEAD_BROADCAST_ADDRESS = "192.168.0.255"; 
	
	/** Nome della rete Ad-Hoc (cluster) */
	public static final String NAME_OF_AD_HOC_CLUSTER_NETWORK = "marco";
	
	/** Nome della rete Ad-Hoc (cluster head) */
	public static final String NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK = "marco";
	
	/** Nome della rete Managed */
	public static final String NAME_OF_MANAGED_NETWORK = "lord";
	
	/** Nome dell'interfaccia con cui il Relay si affaccia sulla rete Ad-Hoc (cluster)*/
	public static final String NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE = "wlan1";
	
	/** Nome dell'interfaccia con cui il Relay si affaccia sulla rete Ad-Hoc (cluster head)*/
	public static final String NAME_OF_AD_HOC_CLUSTER_HEAD_WIFI_INTERFACE = "wlan1";
	
	/**Nome dell'interfaccia con cui il Relay si affaccia sulla rete Managed*/
	public static final String NAME_OF_MANAGED_WIFI_INTERFACE  = "wlan0";						//serve solo al bigboss
	
	/** Nome dell'interfaccia con cui il Client si affaccia sulla rete Ad-Hoc */
	public static final String NAME_OF_CLIENT_WIFI_INTERFACE = "wlan0";
	
	/** Nome dell'interfaccia con cui il Server si affaccia sulla rete Managed (cablato all'AP) */
	public static final String NAME_OF_SERVER_INTERFACE = "eth0";
}
