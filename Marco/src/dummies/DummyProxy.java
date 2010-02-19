package dummies;

import java.net.InetAddress;
import java.util.Observer;

/**
 * Al momento la classe dummy proxy non aggiunge altre funzionalità alla classe
 * base base dummy dummy proxy, in futuro vedi. per capire il suo funzionamento 
 * guardare la superclasse.
 * @author marco
 *
 */

public class DummyProxy extends  DummyDummyProxy {

	public DummyProxy(Observer sessionManager, boolean newProxy,
			String clientAddress, int clientStreamPort, int proxyStreamPortOut,
			int proxyStreamPortIn, int serverStreamPort,
			int recoverySenderPort, String streamingServerAddress,
			String recoverySenderAddress, int serverCtrlPort,
			int proxyCtrlPort, boolean servingClient) {
		super(sessionManager, newProxy, clientAddress, clientStreamPort,
				proxyStreamPortOut, proxyStreamPortIn, serverStreamPort,
				recoverySenderPort, streamingServerAddress, recoverySenderAddress,
				serverCtrlPort, proxyCtrlPort, servingClient);
		
	}
	
	

	
	
	
	

}
