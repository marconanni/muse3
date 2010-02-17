package relay.connection;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;


/**Classe che rappresenta un oggetto in grado di spedire messaggi verso la rete 
 * Managed solo dopo averne ricevuto uno dalla stessa rete, Inoltre è in grado di inviare messaggi alla rete Ad-Hoc
 * e contemporaneamente riceverne.
 * @author Luca Campeti
 *
 */
public class PConnectionManager extends AConnectionManager  {

	private DatagramSocket managedInputOutputSocket = null;

	private boolean ackFromServerReceived =false;
	private boolean ackToClientSent =false;
	private boolean changingRelay = false;

	private String localManagedAddress = null;
	private int localManagedInputOutputPort = -1;
	
	//informazioni per il Proxy
	private int localAdHocInputPort = -1;
	private int localAdHocOutputPort = -1;

	private Observer observer = null;
	private PrivateObservable temporaryObservable = null;


	/**Metodo per ottenere un PConnectionManager
	 * @param cR true se il PConnectionManager viene creato in seguito alla necessità di un passaggio di sessione RTP tra il vecchio e il nuovo Relay, false altrimenti
	 * @param localAdHocAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Ad-Hoc
	 * @param localAdHocInputPort un int che rappresenta la porta da cui ricevere messaggi dalla rete Ad-Hoc
	 * @param localAdHocOutputPort un int che rappresenta la porta attraverso cui inviare messaggi alla rete Ad-Hoc
	 * @param localManagedAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Managed
	 * @param localManagedInputOutputPort un int che rappresenta la porta attraverso cui inviare e ricevere messaggi alla/dalla rete Managed
	 * @param observer l'Observer che deve essere avvertito dal PConnectionManager all'arrivo di un messaggio
	 */
	public PConnectionManager(boolean cR, InetAddress localAdHocAddress,InetAddress localAdHocBcastAddress, int localAdHocInputPort, int localAdHocOutputPort, InetAddress localManagedAddress,  int localManagedInputOutputPort, Observer observer, boolean bcast){
		super( localAdHocAddress, localAdHocBcastAddress, localAdHocInputPort,localAdHocOutputPort, observer, bcast);

		if(localManagedAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");

		this.localManagedAddress = localManagedAddress.getHostAddress();
		this.localManagedInputOutputPort = localManagedInputOutputPort;
//		this.localManagedInputOutputPort = 7000;
		
		this.localAdHocInputPort = localAdHocInputPort;
		this.localAdHocOutputPort = localAdHocOutputPort;

		temporaryObservable = new PrivateObservable();
		temporaryObservable.addObserver(observer);

		changingRelay = cR;

		try {
			managedInputOutputSocket = new DatagramSocket(localManagedInputOutputPort,localManagedAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}


	/**Metodo per unviare un DatagramPacket verso un destinatario nella rete Managed
	 * @param dp DatagramPacket da inviare verso la rete Managed
	 */
	public void sendToServer(DatagramPacket dp){
		try {
			System.out.println(managerName+".sendToServer(): messaggio inviato al Server");
			System.out.println(managerName+".sendToServer(): messaggio inviato a: "+dp.getAddress().getHostAddress()+" porta: "+ dp.getPort());
			managedInputOutputSocket.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**Metodo per inviare un DatagramPacket verso un destinatario nella rete Ad-Hoc 
	 *@param dp DatagramPacket da inviare verso la rete Ad-Hoc
	 */
	public void sendTo(DatagramPacket dp){
		super.sendTo(dp);
		ackToClientSent = true;
	}


	/**Metodo per bloccarsi in attesa di un messaggio proveniente dalla rete Managed
	 */
	public void waitStreamingServerResponse(){
		if(!changingRelay){
			try {
				byte[] buffer = new byte[256];
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				System.out.println(managerName+".waitServerResponse() : mi pongo in attesa del messaggio da parte del Server sulla porta: " + localManagedInputOutputPort);
				managedInputOutputSocket.receive(dp);
				/*
				do {managedInputOutputSocket.receive(dp);}
				while ((dp.getAddress().getHostAddress()) != Parameters.SERVER_ADDRESS);
				 */
				System.out.println(managerName+".waitServerResponse() : messaggio ricevuto da: " + dp.getAddress().getHostAddress() + " porta: " +dp.getPort());
				ackFromServerReceived = true;
				temporaryObservable.triggerAChange();
				temporaryObservable.notifyObservers(dp);
				temporaryObservable.deleteObserver(this.observer);	
				temporaryObservable = null;
				observer = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**Metodo per far partire la ricezione dei messaggi provenienti dalla rete Ad-Hoc da parte del PConnectionManager 
	 */
	public void start(){
//		if((ackFromServerReceived && ackToClientSent && !changingRelay) || changingRelay){
			super.start();
//		}
	}

	/**Metodo per chiudere tutte le Socket del PConnectionManager
	 */
	public void close(){
		if(managedInputOutputSocket != null) managedInputOutputSocket.close();
		managedInputOutputSocket = null;
		super.close();
	}


	/**Metodo per capire se il PConnectionManager ha già ricevuto la risposta dal Server
	 * @return true se la risposta è arrivata, false altrimenti
	 */
	public boolean isAckFromServerReceived() {
		return ackFromServerReceived;
	}


	/**Metodi per capire se il PConnectionManager ha già inviato un messaggio alla rete Ad-Hoc
	 * @return true se il primo messaggio verso la rete Ad-Hoc è stato mandato, false altrimenti
	 */
	public boolean isAckToClientSent() {
		return ackToClientSent;
	}


	/**Metodop per capire se il PConnectionManager è stato creato per supportare uno scambio di 
	 * sessione dal vecchio al nuovo Relay o meno.
	 * @return true se se il PConnectionManager è stato creato per supportare uno scambio di 
	 * sessione, false altrimenti.
	 */
	public boolean isChangingRelay() {
		return changingRelay;
	}


	/**Metodo per ottenere la porta attraverso cui il PConnectionManager riceve messaggi dalla rete Ad-Hoc  
	 * @return un int che rappresenta la porta di cui sopra
	 */
	public int getLocalAdHocInputPort() {
		return localAdHocInputPort;
	}


	/**Metodo per ottenere la porta attraverso cui il PConnectionManager invia messaggi alla rete Ad-Hoc 
	 * @return un int che rappresenta la porta di cui sopra
	 */
	public int getLocalAdHocOutputPort() {
		return localAdHocOutputPort;
	}
	
	/**Metodo per ottenere la porta attraverso cui il PConnectionManager invia/riceve messaggi dalla rete Managed
	 * @return un int che rappresenta la porta di cui sopra
	 */
	public int getLocalManagedInputOutputPort() {
		return localManagedInputOutputPort;
	}

}


final class PrivateObservable extends Observable{

	public PrivateObservable(){
		super();
	}

	public void triggerAChange(){
		this.setChanged();
	}
}

