package relay.connection;


import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ProxyCM extends PConnectionManager {
	
	public ProxyCM(boolean changingRelay,InetAddress localAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, InetAddress localManagedAddress,  int localManagedInputOutputPort, Observer observer){
		super(changingRelay,localAdHocAddress, localAdHocInputPort, localAdHocOutputPort, localManagedAddress, localManagedInputOutputPort,observer);
		setNameManager("ProxyCM");
	}
}