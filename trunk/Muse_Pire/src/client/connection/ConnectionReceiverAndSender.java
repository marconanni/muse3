package client.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Observable;
import java.util.Observer;


/**Classe che fornisce un oggeto che è in grado di inviare un messaggio 
 * solo dopo averne ricevuto uno sempre sulla stessa Socket
 * @author Luca Campeti
 *
 */
public class ConnectionReceiverAndSender extends Observable implements Runnable{

	private DatagramSocket inOutSocket = null;
	private DatagramPacket dataIn = null;
	private DatagramPacket dataOut = null;
	private byte[] buffer = null;
	
	protected String managerName = "ConnectionReceiverAndSender";

	private Object sync = new Object();

	//private String localAddress = null;
	//private int localInputPort = -1;

	/**Metodo per ottenere un ConnectionReceiverAndSender 
	 * @param observer l'Observer che deve essere avvertito alla ricezione di ogni messaggio
	 * @param localAddress una Stringa che rappresenta l'indirizzo locale del nodo
	 * @param localInputPort un int che rappresenta la porta tramite cui ricevere i messaggi
	 */
	public ConnectionReceiverAndSender(Observer observer,int localInputPort){
		
		try {
			inOutSocket = new DatagramSocket(localInputPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		addObserver(observer);
	}

	public void run(){
		buffer = new byte[256];

		while(true){

			//System.out.println(managerName+" : attesa messaggio da parte di " + localAddress + " porta: " +localInputPort);
			dataIn = new DatagramPacket(buffer, buffer.length);

			try {
				inOutSocket.receive(dataIn);
				System.out.println(managerName+" : ricevuto messaggio da : " + dataIn.getAddress().getHostAddress()+" porta: "+dataIn.getPort());
				/*
				if(dataIn.getAddress().getHostAddress().equals(localAddress)){
					System.out.println(managerName+" IGNORATO");
					continue;
				}
				*/
			}
			catch (IOException e) {
				System.out.println(managerName+" : " + e.getMessage());
				break;
			}

			setChanged();
			notifyObservers(dataIn);


			if(dataOut == null){
				synchronized (sync) {
					try {sync.wait();} catch (Exception e) {}
				}	
			}

			try {
				inOutSocket.send(dataOut);
				System.out.println(managerName+".sendToAdHoc() : messaggio inviato a: " +dataOut.getAddress().getHostAddress() + " porta: " +dataOut.getPort() );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			dataIn = null;
			dataOut=null;
		}
	}
	
	/**Metodo per chiudere il ConnectionReceiverAndSender
	 */
	public void close(){
		if(inOutSocket != null) inOutSocket.close();
	}
	
	/**Metodo per inviare un Datagramma a un destinatario nella rete Ad-Hoc
	 * @param notifyRSSI il DatagramPacket da inviare
	 */
	public void sendTo(DatagramPacket notifyRSSI){
		dataOut = notifyRSSI;
		synchronized (sync) {
			sync.notifyAll();
		}
	}
	
	/**Metodo per ottenere il nome del Manager che sta utilizzando il ConnectionReceiverAndSender
	 * @return una Stringa che rappresenta il nome del Manager che sta utilizzando il ConnectionReceiverAndSender
	 */
	public String getManagerName() {
		return managerName;
	}

	/**Metodo per impostare il nome del Manager che sta utilizzando il ConnectionReceiverAndSender
	 * @param managerName una String che rappresenta il nome del Manager che sta utilizzando il ConnectionReceiverAndSender
	 */
	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}

}

/*class TestConnectionReceiver{

	public static void main (String args[]){

		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Observer observer = new testObserver();
		ConnectionReceiver cr  = null;

		cr = new ConnectionReceiver(observer, localhost,6000);

		Thread t = new Thread(cr);
		t.start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		byte[] buffer = {1,2,3,4};
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,localhost,6000);
		DatagramSocket ds = null;

		try {
			ds = new DatagramSocket(5000, localhost);
			ds.send(dp);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cr.close();
	}
}

class testObserver implements Observer{

	public testObserver(){
		System.out.println("testObserver: creato");
	}

	@Override
	public void update(Observable o, Object arg) {
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("Observer: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		System.out.println("Observer: dati ricevuti: " + dp.getData());
	}

}
 **/