package relay.connection;

import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class RelayCM extends AConnectionManager {
	
	public RelayCM(String name,String localAddress, int localInputPort, int localOutputPort, Observer observer){
		super( localAddress, localInputPort, localOutputPort, observer);
		setNameManager(name);
	}
}
