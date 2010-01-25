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
	public static final boolean IMBIGBOSS=false;
	public static final boolean IMPOSSIBLEBIGBOSS = false;
	public static final boolean IMPOSSIBLERELAY = false;
	public static final boolean IMCLIENT = false;
	
	/*
	 ********************* CONFIGURAZIONE DELLA RETE *****************************
	 * 
	 * */	
	/** SERVER e BIGBOSS*/
	public static final String SERVER_ADDRESS = "192.168.186.230";					//indirizzo IP del SERVE
	public static final String NAME_OF_SERVER_INTERFACE = "eth0";					//Nome dell'interfaccia con cui il Server si affaccia sulla rete Managed (cablato all'AP) 
	
	//nel caso di big boss questi parametri sono uguali ai parametri del cluster head
	public static final String NAME_OF_RELAY_MANAGED_NETWORK = "ALMAWIFI";				//Nome della rete Managed
	public static final String NAME_OF_RELAY_MANAGED_WIFI_INTERFACE  = "wlan0";			//Nome dell'interfaccia con cui il Relay si affaccia sulla rete Managed
	public static final String RELAY_MANAGED_ADDRESS = "192.168.186.230";				//indirizzo IP del RELAY sulla rete Managed (BigBoss)
	public static final String RELAY_MANAGED_BROADCAST_ADDRESS = "192.168.187.255";
	
	/** CLUSTER HEAD*/
	public static final String NAME_OF_RELAY_CLUSTER_HEAD_NETWORK = "BIGBOSS";		//Nome della rete Ad-Hoc (cluster head)
	public static final String NAME_OF_RELAY_CLUSTER_HEAD_WIFI_INTERFACE = "wlan1";//Nome dell'interfaccia con cui il Relay si affaccia sulla rete Ad-Hoc (cluster head)
	public static final String RELAY_CLUSTER_HEAD_ADDRESS = "192.168.30.3";	//indirizzo IP del RELAY nella rete Ad-Hoc di competenza del BigBoss (cluster head)
	public static final String RELAY_CLUSTER_HEAD_BROADCAST_ADDRESS = "192.168.30.255";	//indirizzo IP di BROADCAST sulla rete Ad-Hoc (cluster head)
	
	/** CLUSTER */
	public static final String NAME_OF_RELAY_CLUSTER_NETWORK = "RELAY1";			//Nome della rete Ad-Hoc (cluster) 
	public static final String NAME_OF_RELAY_CLUSTER_WIFI_INTERFACE = "wlan0";		//Nome dell'interfaccia con cui il Relay si affaccia sulla rete Ad-Hoc (cluster)
	public static final String RELAY_CLUSTER_ADDRESS = "192.168.5.3";		//indirizzo IP del RELAY nella rete Ad-Hoc di sua competenza (cluster) non server al bigboss
	public static final String RELAY_CLUSTER_BROADCAST_ADDRESS = "192.168.5.255"; 		//indirizzo IP di BROADCAST sulla rete Ad-Hoc (cluster)
	
	/** CLIENT */
	public static final String NAME_OF_CLIENT_NETWORK = "lord";
	public static final String NAME_OF_CLIENT_WIFI_INTERFACE = "wlan0";				//Nome dell'interfaccia con cui il Client si affaccia sulla rete Ad-Hoc
	public static final String CLIENT_ADDRESS = "192.168.2.4";						//indirizzo IP del CLIENT
	public static final String CLIENT_BROADCAST_ADDRESS = "192.168.2.255";
	
	


	
	

}
