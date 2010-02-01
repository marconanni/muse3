package client.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;

//import parameters.NetConfiguration;
import parameters.*;
//import parameters.PortConfiguration;


public class ClientPortMapper {
	
	/*indirizzo IP del client*/
	private InetAddress localAddress = null;
	private InetAddress localBCASTAddress = null;

	/*porte delle socket di elezione in ricezione e trasmissione */
	private int portElectionIn;
	private int portElectionOut;

	/*porte delle socket di richieste dei client in ricezione e trasmissione */
	private int portSessionIn;
	private int portSessionOut;
	
	/*porta della socket del client per la gestione dell'arrivo di REQUEST_RSSI
	/* e per l'invio della NOTIFY_RSSI */
	private int portRSSIOut;
	private int portRSSIIn;
	
	/*porta della socket del client per la ricezione dello stream RTP
	/* e per l'invio della NOTIFY_RSSI */
	private int portRTPIn;

	public static final ClientPortMapper INSTANCE = new ClientPortMapper();  

	private ClientPortMapper()
	{
		portElectionIn = PortConfiguration.PORT_ELECTION_IN;
		portElectionOut = PortConfiguration.PORT_ELECTION_OUT;
		portSessionIn = PortConfiguration.CLIENT_PORT_SESSION_IN;
		portSessionOut = PortConfiguration.CLIENT_PORT_SESSION_OUT;
		portRSSIOut = PortConfiguration.RSSI_PORT_OUT;
		portRSSIIn = PortConfiguration.RSSI_PORT_IN;
		//portRTPIn = PortConfiguration.CLIENT_PORT_RTP_IN;
		
		try {
			localAddress = InetAddress.getByName(NetConfiguration.CLIENT_ADDRESS);
			//localBCASTAddress = InetAddress.getByName(NetConfiguration.CLIENT_BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {e.printStackTrace();}
	}

	/**Metodo per ottenere il riferimento all'unico ClientPortMapper attivo sul nodo CLIENT
	 * @return 
	 */
	public static ClientPortMapper getInstance() {
		return ClientPortMapper.INSTANCE;
	}

	
	/**Metodo per ottenere l'indirizzo locale del CLIENT
	 * @return un InetAddress rappresentante l'indirizzo di cui sopra
	 */
	public InetAddress getLocalAddress() {
		return localAddress;
	}
	
	/**Metodo per ottenere l'indirizzo broadcast locale del CLIENT
	 * @return un InetAddress rappresentante l'indirizzo di cui sopra
	 */
	public InetAddress getLocalBcastAddress() {
		return localBCASTAddress;
	}
	
	/**Metodo per ottenere la porta su cui il CLIENT riceve i messaggi REQUEST_RSSI e 
	 * invia le risposte NOTIFY_RSSI
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortRSSIIn() {
		return portRSSIIn;
	}
	
	/**Metodo per ottenere la porta su cui il CLIENT riceve i messaggi REQUEST_RSSI e 
	 * invia le risposte NOTIFY_RSSI
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortRSSIOut() {
		return portRSSIOut;
	}

	/**Metodo per ottenere la porta su l'ElectionManager del CLIENT riceve i messaggi di elezione 
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortElectionIn() {
		return portElectionIn;
	}

	/**Metodo per ottenere la porta su l'ElectionManager del CLIENT invia i messaggi di elezione 
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortElectionOut() {
		return portElectionOut;
	}

	/**Metodo per ottenere la porta su il SessionManager del CLIENT riceve i messaggi di elezione 
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortSessionIn() {
		return portSessionIn;
	}

	/**Metodo per ottenere la porta su il SessionManager del CLIENT invia i messaggi di elezione 
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortSessionOut() {
		return portSessionOut;
	}

	/**Metodo per ottenere la porta su il CLIENT riceve lo stream RTP dal RELAY 
	 * @return un intero rappresentante la porta di cui sopra
	 */
	public int getPortRTPIn() {
		return portRTPIn;
	}
}
