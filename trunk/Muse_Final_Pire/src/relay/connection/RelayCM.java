package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti (modificato da Pire Dejaco)
 *
 */
public class RelayCM extends AConnectionManager {
	
	public RelayCM(String name,InetAddress localAddress, int localInputPort, int localOutputPort, Observer observer){
		super( localAddress, localInputPort, localOutputPort, observer);
		setNameManager(name);
	}
}
