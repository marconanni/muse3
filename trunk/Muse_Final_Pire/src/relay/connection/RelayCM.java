package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti (modificato da Pire Dejaco)
 *
 */
public class RelayCM extends AConnectionManager {
	
	public RelayCM(String name,InetAddress localAddress,InetAddress bcastAddress, int localInputPort, int localOutputPort, Observer observer, boolean bcast){
		super( localAddress, bcastAddress, localInputPort, localOutputPort, observer, bcast);
		setNameManager(name);
	}
}
