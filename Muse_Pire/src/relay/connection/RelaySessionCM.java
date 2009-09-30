package relay.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class RelaySessionCM extends MAConnectionManager {
	
	public RelaySessionCM(String localAdHocAddress, int localAdHocSessionInputPort, int localAdHocSessionOutputPort, String localManagedAddress, int localManagedSessionOutputPort, Observer observer){
		super(localAdHocAddress, localAdHocSessionInputPort, localAdHocSessionOutputPort, localManagedAddress, localManagedSessionOutputPort, observer);
		setNameManager("RelaySessionCM");
	}
}