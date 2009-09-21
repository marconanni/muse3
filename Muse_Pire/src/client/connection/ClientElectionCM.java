package client.connection;

import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ClientElectionCM extends AConnectionManager{
	
	public ClientElectionCM(String localAddress, int localElectionInputPort, int localElectionOutputPort, Observer observer){
		super( localAddress, localElectionInputPort, localElectionOutputPort, observer);
		setNameManager("ClientElectionCM");
	}
}