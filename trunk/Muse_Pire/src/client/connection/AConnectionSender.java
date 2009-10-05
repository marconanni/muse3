package client.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Observable;
import java.util.Observer;

public class AConnectionSender extends Observable implements Runnable{

	private DatagramSocket outSocket = null;
	private DatagramPacket dataOut = null;

	private Object sync = new Object();
	
	protected String managerName = "AConnectionSender";
	
	
	/**Metodo per ottenere un AConnectionReceiver
	 * @param observer l'Observer che deve essere avvertito alla ricezione di un messaggio
	 * @param localAddress l'indirizzo locale del nodo
	 * @param localInputPort la porta tramite cui ricevere i messaggi
	 */
	public AConnectionSender(Observer observer, int localOutputPort){
	try {
			outSocket = new DatagramSocket(localOutputPort);
		} catch (SocketException e) {e.printStackTrace();}
		addObserver(observer);
	}

	
	public void run(){
		while(true){

			if(dataOut==null){
				synchronized (sync) {
					try{sync.wait();}catch (Exception e) {}
					
				}
			}
			try{
				outSocket.send(dataOut);
			}catch (IOException e) {}
			dataOut=null;
		}
	}

	/**Metodo per chiudere la Socket su cui l'AConnectionReceiver riceve i messaggi
	 */
	public void close(){
		if(outSocket != null) outSocket.close();
	}
	
	public void sendTo(DatagramPacket notifyRSSI){
		dataOut=notifyRSSI;
		synchronized (sync) {
			sync.notifyAll();
		}
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