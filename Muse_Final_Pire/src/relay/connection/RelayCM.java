package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti (modificato da Pire Dejaco)
 *
 */
public class RelayCM extends AConnectionManager {
	
	public RelayCM(String name,InetAddress localAddress,InetAddress bcastAddress, int localInputPort, int localOutputPort, Observer observer){
		super( localAddress, bcastAddress, localInputPort, localOutputPort, observer);
		setNameManager(name);
	}
}
