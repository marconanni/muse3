package relay;

import java.net.InetAddress;
import java.net.UnknownHostException;

import parameters.Parameters;

public class RelayPortMapper {


	//PER I PROXY
	/*range di porte assegnabili ad un PROXY per la ricezione del flusso RTP dal SERVER*/
	//private boolean[] rangePortInRTPProxy = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 

	/*range di porte assegnabili ad un PROXY per l'invio del flusso RTP al CLIENT*/
	//private boolean[] rangePortOutRTPProxy = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 

	/*range di porte assegnabili ad un PROXY per ricevere messaggi START_TX e STOP_TX dal CLIENT*/
	//private boolean[] rangeAdHocPortInControlProxy = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 

	/*range di porte assegnabili ad un PROXY per inviare il messaggio di LEAVE alCLIENT*/
	private boolean[] rangeAdHocPortOutControlProxy = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 

	/**
	 * @param rangePortInRTPProxy the rangePortInRTPProxy to set
	 */
	//public void setRangePortInRTPProxy(int rangePortInRTPProxy) {
		
		//this.rangePortInRTPProxy[rangePortInRTPProxy - Parameters.PROXY_INITIAL_PORT_IN_RTP] = false;
	//}

	/**
	 * @param rangePortOutRTPProxy the rangePortOutRTPProxy to set
	 */
	//public void setRangePortOutRTPProxy(int rangePortOutRTPProxy) {
		//this.rangePortOutRTPProxy[rangePortOutRTPProxy - Parameters.PROXY_INITIAL_PORT_OUT_RTP] = false;
	//}

	/**
	 * @param rangeAdHocPortInControlProxy the rangeAdHocPortInControlProxy to set
	 */
	//public void setRangeAdHocPortInControlProxy(
			//int rangeAdHocPortInControlProxy) {
		//this.rangeAdHocPortInControlProxy[rangeAdHocPortInControlProxy - Parameters.PROXY_INITIAL_AD_HOC_PORT_IN_CONTROL] = false;
	//}

	/**
	 * @param rangeAdHocPortOutControlProxy the rangeAdHocPortOutControlProxy to set
	 */
	//public void setRangeAdHocPortOutControlProxy(
		//	int rangeAdHocPortOutControlProxy) {
		//this.rangeAdHocPortOutControlProxy[rangeAdHocPortOutControlProxy - Parameters.PROXY_INITIAL_AD_HOC_PORT_OUT_CONTROL] = false;
	//}

	/**
	 * @param rangeManagedPortInOutControlProxy the rangeManagedPortInOutControlProxy to set
	 */
	//public void setRangeManagedPortInOutControlProxy(
		//	int rangeManagedPortInOutControlProxy) {
		//this.rangeManagedPortInOutControlProxy[rangeManagedPortInOutControlProxy - Parameters.PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL] = false;
	//}

	/*range di porte assegnabili ad un PROXY per inviare il messaggio di FORWARD_REQ_FILE al SERVER e  
	 * i messaggi di START_TX e STOP_TX allo STREAMINGSERVER e per ricevere il messaggio ACK_RELAY_FORW*/
	private boolean[] rangeManagedPortInOutControlProxy = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 

	//PER IL RELAY
	/*porta su cui il relay attuale ascolta i messaggi di WHO_IS_RELAY e invia messaggi di IM_RELAY*/
	private int portInWhoIsRelay;
	private int portOutWhoIsRelay;


	/*porte delle socket per la gestione della sessione verso la rete Ad-Hoc, in ingresso e in uscita */
	private int portInAdHocSession;
	private int portOutAdHocSession;


	/*porte delle socket per la gestione della sessione verso la rete Managed, solo in 
	 *trasmissione per inviare REDIRECT al SERVER*/
	private int portOutManagedSession;


	/*porte delle socket per la gestione l'elezione, in ingresso e in uscita */
	private int portInAdHocElection;
	private int portOutAdHocElection;


	/*porte rispettivamente per l'invio periodico dei REQUEST_RSSI e per la ricezione dei NOTIFY_RSSI*/
	private int portOutRSSI;
	private int portInRSSI;
	

	/*indirizzi locali per il relay*/
	private InetAddress localAdHocAddress =null;
	private InetAddress localManagedAddress =null;



	public static final RelayPortMapper INSTANCE = new RelayPortMapper();  

	private RelayPortMapper()
	{
		for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
			//rangePortInRTPProxy[i] = true;
			//rangePortOutRTPProxy[i] = true;
			//rangeAdHocPortInControlProxy[i] = true;
			rangeAdHocPortOutControlProxy[i] = true;
			rangeManagedPortInOutControlProxy[i] = true;
		}
		portInWhoIsRelay = Parameters.WHO_IS_RELAY_PORT_IN;
		portOutWhoIsRelay = Parameters.WHO_IS_RELAY_PORT_OUT;
		portInAdHocElection = Parameters.RELAY_ELECTION_PORT_IN;
		portOutAdHocElection = Parameters.RELAY_ELECTION_PORT_OUT;
		portInAdHocSession = Parameters.RELAY_SESSION_AD_HOC_PORT_IN;
		portOutAdHocSession = Parameters.RELAY_SESSION_AD_HOC_PORT_OUT;
		portOutManagedSession = Parameters.RELAY_SESSION_MANAGED_PORT_OUT;
		portInRSSI = Parameters.RELAY_RSSI_RECEIVER_PORT;
		portOutRSSI = Parameters.RELAY_RSSI_SENDER_PORT;

		try {
			localAdHocAddress = InetAddress.getByName(Parameters.RELAY_AD_HOC_ADDRESS);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			localManagedAddress = InetAddress.getByName(Parameters.RELAY_MANAGED_ADDRESS);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Metodo per ottenere il riferimento all'unico RelayPortMapper attivo sul nodo CLIENT
	 * @return 
	 */
	public static RelayPortMapper getInstance() {
		return RelayPortMapper.INSTANCE;
	}


	/*private int assignPort(String typePort)
	{
		if(typePort.equals("ControlAdHocIn"))
		{
			for(int i = 0;i < 1000;i++){
				if(rangeAdHocPortInControlProxy[i]){
					rangeAdHocPortInControlProxy[i] = false;
					return i + Parameters.PROXY_INITIAL_AD_HOC_PORT_IN_CONTROL;
				}
			}
		}

		if(typePort.equals("ControlAdHocOut"))
		{
			for(int i = 0;i < 1000;i++){
				if(rangeAdHocPortOutControlProxy[i]){
					rangeAdHocPortOutControlProxy[i] = false;
					return i + Parameters.PROXY_INITIAL_AD_HOC_PORT_OUT_CONTROL;
				}
			}
		}

		if(typePort.equals("ControlManagedOut"))
		{
			for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
				if(rangeManagedPortInOutControlProxy[i]){
					rangeManagedPortInOutControlProxy[i] = false;
					return i + Parameters.PROXY_INITIAL_MANAGED_PORT_IN_OUT_CONTROL;
				}
			}
		}

		if(typePort.equals("StreamIn"))
		{
			for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
				if(rangePortInRTPProxy[i]){
					rangePortInRTPProxy[i] = false;
					rangePortInRTPProxy[i+1] = false;
					return i + Parameters.PROXY_INITIAL_PORT_IN_RTP;
				}
			}
		}

		if(typePort.equals("StreamOut"))
		{
			for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
				if(rangePortOutRTPProxy[i]){
					rangePortOutRTPProxy[i] = false;
					rangePortOutRTPProxy[i+1] = false;
					return i + Parameters.PROXY_INITIAL_PORT_OUT_RTP;
				}
			}
		}

		return -1;
	}*/

	/**Metodo per ottenere una porta libera da assegnare al PROXY per 
	 * ricevere i messaggi di START_TX e STOP_TX dal CLIENT
	 * @return un intero rappresentante la porta appena occupata
	 */
	/*public int getFirstFreeControlAdHocInPort()
	{
		return this.assignPort("ControlAdHocIn");
	}*/

	/**Metodo per ottenere una porta libera da assegnare al PROXY per 
	 * inviare il messaggio di ACK_CLIENT_REQ e di LEAVE al CLIENT
	 * @return un intero rappresentante la porta appena occupata
	 */
	/*public int getFirstFreeControlAdHocOutPort()
	{
		return this.assignPort("ControlAdHocOut");
	}*/

	/**Metodo per ottenere una porta libera da assegnare al PROXY per 
	 * inviare i messaggi di FORWARD_REQ_FILE e, in seguito, quelli di START_TX e STOP_TX al SERVER
	 * @return un intero rappresentante la porta appena occupata
	 */
	/*public int getFirstFreeControlManagedOutPort()
	{
		return this.assignPort("ControlManagedOut");
	}*/

	/**Metodo per ottenere una porta libera da assegnare al PROXY per 
	 * ricevere lo stream RTP dal SERVER
	 * @return un intero rappresentante la porta appena occupata
	 */
	/*public int getFirstFreeStreamInPort()
	{
		return this.assignPort("StreamIn");
	}*/

	/**Metodo per ottenere una porta libera da assegnare al PROXY per 
	 * inviare lo stream RTP al CLIENT
	 * @return un intero rappresentante la porta appena occupata
	 */
	/*public int getFirstFreeStreamOutPort()
	{
		return this.assignPort("StreamOut");
	}*/

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
//	public boolean[] getRangePortInRTPProxy() {
//		return rangePortInRTPProxy;
//	}

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
//	public boolean[] getRangePortOutRTPProxy() {
//		return rangePortOutRTPProxy;
//	}

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
//	public boolean[] getRangeAdHocPortInControlProxy() {
//		return rangeAdHocPortInControlProxy;
//	}

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
	public boolean[] getRangeAdHocPortOutControlProxy() {
		return rangeAdHocPortOutControlProxy;
	}

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
	public boolean[] getRangeManagedPortInOutControlProxy() {
		return rangeManagedPortInOutControlProxy;
	}
	
	/**Metodo per ottenere la porta su cui il RELAY attuale attende i messaggi WHO_IS_RELAY
	 * e tramite cui esso risponde inviando il messaggio IM_RELAY
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortInWhoIsRelay() {
		return portInWhoIsRelay;
	}
	
	public int getPortOutWhoIsRelay() {
		return portOutWhoIsRelay;
	}
	

	/**Metodo per ottenere la porta su cui il SessionManager del RELAY riceve i messaggi di sessione dalla rete Ad-Hoc
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortInAdHocSession() {
		return portInAdHocSession;
	}

	/**Metodo per ottenere la porta su cui il SessionManager de RELAY invia i messaggi di sessione alla rete Ad-Hoc
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortOutAdHocSession() {
		return portOutAdHocSession;
	}

	/**Metodo per ottenere la porta su cui il SessionManager del RELAY invia i messaggi di sessione al SERVER
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortOutManagedSession() {
		return portOutManagedSession;
	}

	/**Metodo per ottenere la porta su cui l'ElectionManager del RELAY riceve i messaggi di elezione dalla rete Ad-Hoc
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortInAdHocElection() {
		return portInAdHocElection;
	}

	/**Metodo per ottenere la porta su cui l'ElectionManager del RELAY invia i messaggi di elezione alla rete Ad-Hoc
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortOutAdHocElection() {
		return portOutAdHocElection;
	}

	/**Metodo per ottenere la porta su cui il PositioningManager dell'ElectionManager del RELAY attuale 
	 * riceve i messaggi di NOTIFY_RSSI dai CLIENTs serviti
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortInRSSI() {
		return portInRSSI;
	}

	/**Metodo per ottenere la porta su cui il PositioningManager dell'ElectionManager del RELAY attuale 
	 * invia i messaggi di REQUEST_RSSI ai CLIENTs serviti
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortOutRSSI() {
		return portOutRSSI;
	}

	/**Metodo per ottenere l'indirizzo del nodo locale sulla rete Ad-Hoc
	 * @return un InetAddress rappresentante l'indirizzo di cui sopra 
	 */
	public InetAddress getLocalAdHocHostAddress() {
		return localAdHocAddress;
	}

	/**Metodo per ottenere l'indirizzo del nodo locale sulla rete Managed
	 * @return un InetAddress rappresentante l'indirizzo di cui sopra 
	 */
	public InetAddress getLocalManagedHostAddress() {
		return localManagedAddress;
	}




	/**
	 * CODICE DI VERIFICA DEL PORTMAPPER DEL RELAY
	 * @param args
	 */

	/*public static void main(String[] args) {
		// TODO Auto-generated method stub

		 RelayPortMapper portMapper = RelayPortMapper.getInstance();

		 System.out.println("######### SESSIONE 1 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 2 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 3 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 4 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 5 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 6 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 7 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("######### SESSIONE 8 ########");
		 System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		 System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		 System.out.println("PortaElezioneIn: "+ portMapper.getPortElectionIn());
		 System.out.println("PortaElezioneOut: "+ portMapper.getPortElectionOut());
		 System.out.println("PortaStandardRequestServer: "+ portMapper.getPortSessionIn());
		 System.out.println("PortaStandardReceiveServer: "+ portMapper.getPortSessionOut());
		 System.out.println("PortaStandardRequestClient: "+ portMapper.getPortClientIn());
		 System.out.println("PortaStandardReceiveClient: "+ portMapper.getPortClientOut());
	}
	 */
}