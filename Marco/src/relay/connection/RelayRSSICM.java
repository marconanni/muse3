package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class RelayRSSICM extends AConnectionManager {

	public RelayRSSICM(String localAddress, int localRSSIInputPort, int localRSSIOutputPort, Observer observer){
		super(localAddress, localRSSIInputPort, localRSSIOutputPort, observer);
		setNameManager("RelayRSSICM");
	}
}