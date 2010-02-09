package client.connection;

import java.net.InetAddress;
import java.util.Observer;

import relay.connection.AConnectionManager;

/**
 * @author Luca Campeti, Dejaco Pire
 * @version 1.1
 */
public class ClientCM extends AConnectionManager{
	
	public ClientCM(String name,InetAddress localAddress,InetAddress bcastAddress, int localInputPort, int localOutputPort, Observer observer, boolean bcast){
		super( localAddress, bcastAddress, localInputPort, localOutputPort, observer, bcast);
		setNameManager(name);
	}
}