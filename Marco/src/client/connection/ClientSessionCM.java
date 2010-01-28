package client.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 * Vecchia Classe non pi� usata, soppiantata da Client CM
 */
public class ClientSessionCM extends AConnectionManager {
	
	public ClientSessionCM(String localAddress, int localSessionInputPort, int localSessionOutputPort, Observer observer){
		super( localAddress, localSessionInputPort, localSessionOutputPort, observer);
		setNameManager("ClientSessionCM");
	}
}
