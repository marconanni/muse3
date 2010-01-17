package relay.connection;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Observer;


/**Classe che permette di inviare messaggi su una Socket e, contemporaneamente, 
 * è in grado di attendere messaggi su un'altra Socket e gestirli avvertendo 
 * l'Observer passato per parametro al costruttore 
 * @author Luca Campeti (modificato da Pire Dejaco)
 */
public class AConnectionManager {

	private AConnectionReceiver receiverLocalAdHoc = null;
	private AConnectionReceiver receiverBcastAdHoc = null;
	private Thread receiverLocalAdHocThread = null;
	private Thread receiverBcastAdHocThread = null;
	private DatagramSocket adHocOutputSocket = null;
	private boolean started = false;
	protected String managerName = "AConnectionManager";
	private InetAddress localAddress;
	private InetAddress bcastAddress;
	private int localOutputPort = -1;
	private int localInputPort = -1;
	
	/**Metodo per ottenere un AConnectionManager
	 * @param localAdHocAddress una Stringa che rappresenta l'indirizzo locale sulla rete Ad-Hoc del nodo
	 * @param localAdHocInputPort un int che rappresenta la porta di ricezione dei messaggi sulla rete Ad-Hoc
	 * @param localAdHocOutputPort un int che rappresenta la porta di invio dei messaggi sulla rete Ad-Hoc
	 * @param observer l'Observer che deve essere avvertito alla ricezione di un messaggio
	 */
	public AConnectionManager(InetAddress localAddress,InetAddress bcastAddress, int localInputPort, int localOutputPort, Observer observer){
		if(localAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");
		this.setLocalAddress(localAddress);
		this.setBcastAddress(bcastAddress);
		this.setLocalOutputPort(localOutputPort);
		this.setLocalInputPort(localInputPort);
	
		try {
			adHocOutputSocket = new DatagramSocket(localOutputPort,localAddress);
		} catch (SocketException e) {e.printStackTrace();}

		receiverLocalAdHoc = new AConnectionReceiver(observer,localAddress,localInputPort);
		receiverBcastAdHoc = new AConnectionReceiver(observer,bcastAddress,localInputPort);
		receiverLocalAdHocThread = new Thread(receiverLocalAdHoc);
		receiverBcastAdHocThread = new Thread(receiverBcastAdHoc);
	}

	/**Metodo per far partire la ricezione dei messaggi dalla rete Ad-Hoc*/
	public void start(){
		if(receiverLocalAdHoc!=null){
			receiverLocalAdHocThread.start();
			started = true;
		}
		if(receiverBcastAdHoc!=null){
			receiverBcastAdHocThread.start();
			started = true;
		}
	}

	/**Metodo per spedire un DatagramPacket verso un destinatario nella rete Ad-Hoc
	 * @param dp DatagramPacket da inviare alla rete Ad-Hoc*/
	public void sendTo(DatagramPacket dp){
		try {
			adHocOutputSocket.send(dp);
		} catch (IOException e) {e.printStackTrace();}
	}
	
	/**Metodo per chiudere il AConnectionReceiver e la Socket d'invio locale */
	public void close(){
		if(adHocOutputSocket != null) adHocOutputSocket.close();
		if(receiverLocalAdHoc != null) receiverLocalAdHoc.close();
		if(receiverBcastAdHoc != null) receiverBcastAdHoc.close();
		receiverLocalAdHoc = null;
		receiverLocalAdHocThread = null;
		receiverBcastAdHoc = null;
		receiverBcastAdHocThread = null;
		adHocOutputSocket = null;
	}

	/**Metodo per interrompere la ricezione dei messaggi*/
	public void stopReceiving(){
		receiverLocalAdHoc.pauseReception();
		receiverBcastAdHoc.pauseReception();
		started = false;
	}

	/**Metodo per riprendere la ricezione dei messaggi*/
	public void resumeReceiving(){
		receiverLocalAdHoc.resumeReception();
		receiverBcastAdHoc.resumeReception();
		started = true;
	}

	/**Metodo per sapere se il AConnectionManager è partito
	 * @return true se la ricezione dei messaggi AConnectionManager, false altrimenti*/
	public boolean isStarted(){return started;}
	
	/**Metodo per ottenere il nome del Manager che sta utilizzando l'AConnectionManager
	 * @return una String che rappresenta il nome del Manager che sta utilizzando l'AConnectionManager*/
	public String getManagerName() {return managerName;}

	/**Metodo per impostare il nome del Manager che sta utilizzando l'AConnectionManager
	 * @param managerName una String che rappresenta il nome del Manager che sta utilizzando l'AConnectionManager*/
	public void setNameManager(String managerName) {
		this.managerName = managerName;
		if(receiverLocalAdHoc != null)receiverLocalAdHoc.setManagerName(this.managerName);
		if(receiverBcastAdHoc != null)receiverBcastAdHoc.setManagerName(this.managerName);
	}
	
	public void setLocalAddress(InetAddress localAddress){this.localAddress = localAddress;}
	public InetAddress getLocalAddress(){return localAddress;}
	public void setBcastAddress(InetAddress bcastAddress){this.bcastAddress = bcastAddress;}
	public InetAddress getBcastAddress(){return bcastAddress;}
	public void setLocalOutputPort(int localOutputPort) {this.localOutputPort = localOutputPort;}
	public int getLocalOutputPort() {return localOutputPort;}
	public void setLocalInputPort(int localInputPort) {this.localInputPort = localInputPort;}
	public int getLocalInputPort() {return localInputPort;}
}