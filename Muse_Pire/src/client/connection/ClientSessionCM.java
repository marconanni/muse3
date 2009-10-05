package client.connection;

import java.util.Observer;

/**
 * @author Luca Campeti
 *
 */
public class ClientSessionCM extends AConnectionManager {
	
	public ClientSessionCM(int localSessionInputPort, int localSessionOutputPort, Observer observer){
		super(localSessionInputPort, localSessionOutputPort, observer);
		setNameManager("ClientSessionCM");
	}
}
