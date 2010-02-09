package relay.connection;


import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ProxyCM extends PConnectionManager {
	
	public ProxyCM(boolean changingRelay,InetAddress localAdHocAddress, InetAddress localAdHocBcastAddress, int localAdHocInputPort, int localAdHocOutputPort, InetAddress localManagedAddress,  int localManagedInputOutputPort, Observer observer, boolean bcast){
		super(changingRelay,localAdHocAddress, localAdHocBcastAddress, localAdHocInputPort, localAdHocOutputPort, localManagedAddress, localManagedInputOutputPort,observer,bcast);
		setNameManager("ProxyCM");
	}
}