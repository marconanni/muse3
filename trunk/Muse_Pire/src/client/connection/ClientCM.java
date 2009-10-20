package client.connection;

import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ClientCM extends AConnectionManager{
	
	public ClientCM(String name, int localElectionInputPort, int localElectionOutputPort, Observer observer){
		super(localElectionInputPort, localElectionOutputPort, observer);
		setManagerName("ClientElectionCM");
	}
}