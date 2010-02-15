package relay.connection;


import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Marco nanni
 * 
 * Questa classe incapsula due relayCM uno indirizzato verso la rete superiore, uno idirizzato verso la rete inferiore.
 *
 *
 */
public class ProxyCM {
	
	private RelayCM clusterCM; // è il connectionManager che si occupa della rete inferiore ( del cluster di cui èresponsabile il relay)
	private RelayCM clusterHeadCM;  // è il connectionManager che si occupa della rete superiore (quella sulla quale si trova il big boss
									// se questo è il connectionManager di un proxy su un relay secondario o quella su cui c'è il server
									// se questo è il connectionManager di un proxy sul big boss.
	
	/**
	 * Crea un Proxy Cm che contiente al suo interno due relayConnectionmanager
	 * Il broadcast non è abilitato, visto che, al momento, il proxy non riceve messaggi in broadcast.
	 * @param localAdHocAddress l'indirizzo sulla rete "inferiore", quella verso cuiil proxy invia lo stream
	 * @param localAdHocInputPort la porta sulla quale ricevere i messaggi sulla rete inferiore
	 * @param localAdHocOutputPort la porta dalla quale inviare i messaggi sulla rete inferiore
	 * @param localManagedAddress l'indirizzo sulla rete "superiore", quella sulla quale il proxy riceve lo stream
	 * @param localManagedInputPort la porta sulla quale ricevere i messaggi sulla rete superiore
	 * @param localManagedOutputPort la porta dalla quale inviare i messaggi sulla rete inferiore
	 * @param observer : il riferimento a chi dovrà essere notificato all'arrio di un messaggio su una delle due reti (il proxy)
	 */
	
	public ProxyCM(InetAddress localAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, InetAddress localManagedAddress,  int localManagedInputPort, int localManagedOutputPort, Observer observer){
		clusterCM = new RelayCM("ProxyClusterCM", localAdHocAddress, null, localAdHocInputPort, localManagedOutputPort, observer, false);
		clusterHeadCM= new RelayCM("ProxyClusterHeadCM", localManagedAddress, null, localManagedInputPort, localManagedOutputPort, observer, false);
		
	}

	
	/**
	 * chiude le porte del CM
	 */
	
	public void close() {
		clusterCM.close();
		clusterHeadCM.close();
		
	}

	
	public int getLocalAdHocInputPort() {
		return clusterCM.getLocalInputPort();
	}

	
	public int getLocalAdHocOutputPort() {
		
		return clusterHeadCM.getLocalOutputPort();
	}


	public int getLocalManagedInputOutputPort() {
		
		return clusterHeadCM.getLocalInputPort();
	}

	 
	public void sendTo(DatagramPacket dp) {
		// Auto-generated method stub
		clusterCM.sendTo(dp);
	}

	 
	public void sendToServer(DatagramPacket dp) {
		// Auto-generated method stub
		clusterHeadCM.sendTo(dp);
	}

	 
	/**
	 * Metodo che avvia i thread che attendono i messaggi e,
	 * quando arrivano avvisano l'opserver con un evento che è 
	 * il messaggio stesso.
	 */
	public void start() {
		clusterCM.start();
		clusterHeadCM.start();
	}
	
	
	
	
	
	
	
	
}