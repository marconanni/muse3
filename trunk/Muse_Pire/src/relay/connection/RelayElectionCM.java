package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class RelayElectionCM extends AConnectionManager {
	
	public RelayElectionCM(String localAdHocAddress, int localElectionInputPort, int localElectionOutputPort, Observer observer){
		super( localAdHocAddress, localElectionInputPort, localElectionOutputPort, observer);
		setNameManager("RelayElectionCM");
	}
}
