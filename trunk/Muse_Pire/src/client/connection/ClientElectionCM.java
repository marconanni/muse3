package client.connection;

import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ClientElectionCM extends AConnectionManager{
	
	public ClientElectionCM(int localElectionInputPort, int localElectionOutputPort, Observer observer){
		super(localElectionInputPort, localElectionOutputPort, observer);
		setNameManager("ClientElectionCM");
	}
}