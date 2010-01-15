package relay.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;


/**Classe che permette di ricevere messaggi su una Socket. Ad ogni ricezione, tramite il pattern Observer-Observable,
 * è in grado di avvertire l'Observer passato per parametro al costruttore.
 * @author Luca Campeti
 *
 */
public class AConnectionReceiver extends Observable implements Runnable{

	private DatagramSocket inSocket = null;
	private DatagramPacket dataIn = null;
	private byte[] buffer = null;

	private Object sync = new Object();
	private boolean stopped = false;
	protected String managerName = "AConnectionReceiver";
	
	private InetAddress localAddress = null;
	private int localInputPort = -1;
	
	/**Metodo per ottenere un AConnectionReceiver
	 * @param observer l'Observer che deve essere avvertito alla ricezione di un messaggio
	 * @param localAddress l'indirizzo locale del nodo
	 * @param localInputPort la porta tramite cui ricevere i messaggi
	 */
	public AConnectionReceiver(Observer observer, String localAddress, int localInputPort){
		
		if(localAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");
		
		try {
			this.localAddress = InetAddress.getByName(localAddress);
		} catch (UnknownHostException e1) {e1.printStackTrace();}
		this.localInputPort = localInputPort;
		
		try {
			System.out.println("INSOCKET: "+this.localAddress+":"+localInputPort);
			inSocket = new DatagramSocket(localInputPort, this.localAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		addObserver(observer);
	}

	
	public void run(){
		buffer = new byte[256];

		while(true){

			if(stopped){
				synchronized (sync) {
					try {
						sync.wait();
						if(dataIn != null) {
							setChanged();
							notifyObservers(dataIn);
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			dataIn = new DatagramPacket(buffer, buffer.length);

			try {
				inSocket.receive(dataIn);
				System.out.println(managerName+": ricevuto messaggio da : " +dataIn.getAddress().getHostAddress() + " porta: " +dataIn.getPort());
			}
			catch (IOException e) {
				System.out.println(managerName+": " + e.getMessage());
				break;
			}

			if(stopped) continue;
			
			setChanged();
			notifyObservers(dataIn);
			dataIn=null;
		}
	}

	/**Metodo per chiudere la socket su cui l'AConnectionReceiver riceve i messaggi
	 */
	public void close(){
		if(inSocket != null) inSocket.close();
	}

	/**Metodo per stoppare la ricezione dei messaggi da parte dell'AConnectionReceiver
	 */
	public void pauseReception(){
		stopped = true;
	}

	/**Metodo per riprendere la ricezione dei messaggi da parte dell'AConnectionReceiver
	 */
	public void resumeReception(){
		stopped = false;
		synchronized (sync) {sync.notifyAll();}
	}

	/**Metodo per ottenere il nome del Manager che sta utilizzando l'AConnectionReceiver
	 * @return una String che rappresenta il nome del Manager che sfrutta l'AConnectionReceiver
	 */
	public String getManagerName() {
		return managerName;
	}

	/**Metodo per impostare il nome del Manager che sta utilizzando l'AConnectionReceiver
	 * @param managerName una String che rappresenta il nome del Manager che sfrutta l'AConnectionReceiver 
	 */
	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}

}