package relay.connection;


import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ProxyCM extends PConnectionManager {
	
	public ProxyCM(boolean changingRelay,String localAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, String localManagedAddress,  int localManagedInputOutputPort, Observer observer){
		super(changingRelay,localAdHocAddress, localAdHocInputPort, localAdHocOutputPort, localManagedAddress, localManagedInputOutputPort,observer);
		setNameManager("ProxyCM");
	}
}