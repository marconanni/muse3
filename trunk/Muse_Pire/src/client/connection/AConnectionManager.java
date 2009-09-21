package client.connection;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;


/**Classe che permette di inviare messaggi su una Socket e, contemporaneamente, 
 * è in grado di attendere messaggi su un'altra Socket e gestirli avvertendo 
 * l'Observer passato per parametro al costruttore 
 * @author Luca Campeti
 *
 */
public class AConnectionManager {

	private AConnectionReceiver receiverAdHoc = null;
	private Thread receiverAdHocThread = null;

	private DatagramSocket adHocOutputSocket = null;

	private boolean started = false;
	protected String managerName = "AConnectionManager";
	
	private String localAdHocAddress = null;
	private int localAdHocOutputPort = -1;

	/**Metodo per ottenere un AConnectionManager
	 * @param localAdHocAddress una Stringa che rappresenta l'indirizzo locale sulla rete Ad-Hoc del nodo
	 * @param localAdHocInputPort un int che rappresenta la porta di ricezione dei messaggi sulla rete Ad-Hoc
	 * @param localAdHocOutputPort un int che rappresenta la porta di invio dei messaggi sulla rete Ad-Hoc
	 * @param observer l'Observer che deve essere avvertito alla ricezione di un messaggio
	 */
	public AConnectionManager(String localAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, Observer observer){
		if(localAdHocAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");

		this.localAdHocAddress = localAdHocAddress;
		this.localAdHocOutputPort = localAdHocOutputPort;
		
		try {
			adHocOutputSocket = new DatagramSocket(localAdHocOutputPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		receiverAdHoc = new AConnectionReceiver(observer,localAdHocAddress,localAdHocInputPort);
		receiverAdHocThread = new Thread(receiverAdHoc);
	}


	/**Metodo per far partire la ricezione dei messaggi dalla rete Ad-Hoc
	 */
	public void start(){
		if(receiverAdHoc!=null){
			receiverAdHocThread.start();
			started = true;
			System.out.println(managerName+".start(): PARTITO");
		}
	}


	/**Metodo per spedire un DatagramPacket verso un destinatario nella rete Ad-Hoc
	 * @param dp DatagramPacket da inviare alla rete Ad-Hoc
	 */
	public void sendTo(DatagramPacket dp){
		try {
			adHocOutputSocket.send(dp);
			System.out.println(managerName+".sendToAdHoc() : messaggio inviato a: " +dp.getAddress().getHostAddress() + " porta: " +dp.getPort() );
			//System.out.println(managerName+".sendToAdHoc() : messaggio inviato da: " + localAdHocAddress + " porta: " + localAdHocOutputPort );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**Metodo per chiudere il AConnectionReceiver e la Socket d'invio locale
	 */
	public void close(){
		if(adHocOutputSocket != null) adHocOutputSocket.close();
		if(receiverAdHoc != null) receiverAdHoc.close();
		receiverAdHoc = null;
		receiverAdHocThread = null;
		adHocOutputSocket = null;
	}


	/**Metodo per interrompere la ricezione dei messaggi
	 */
	public void stopReceiving(){
		receiverAdHoc.pauseReception();
	}

	/**Metodo per riprendere la ricezione dei messaggi
	 */
	public void resumeReceiving(){
		receiverAdHoc.resumeReception();
	}

	/**Metodo per sapere se il AConnectionManager è partito
	 * @return true se la ricezione dei messaggi AConnectionManager, false altrimenti
	 */
	public boolean isStarted(){
		return started;
	}


	/**Metodo per ottenere il nome del Manager che sta utilizzando l'AConnectionManager
	 * @return una String che rappresenta il nome del Manager che sta utilizzando l'AConnectionManager
	 */
	public String getManagerName() {
		return managerName;
	}


	/**Metodo per impostare il nome del Manager che sta utilizzando l'AConnectionManager
	 * @param managerName una String che rappresenta il nome del Manager che sta utilizzando l'AConnectionManager
	 */
	public void setNameManager(String managerName) {
		this.managerName = managerName;
		if(receiverAdHoc != null)receiverAdHoc.setManagerName(this.managerName);
	}

}

/*class TestConnectionManager{

	public static void main(String args[]){
		TestObserver to = new TestObserver();
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AConnectionManager cm = new AConnectionManager(localhost,5000,6000,to);
		cm.start();

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer = {0};
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,localhost,5000);
		System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
		cm.sendTo(dp);

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer1 = {100};
		dp = new DatagramPacket(buffer1,buffer1.length,localhost,5000);
		System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
		cm.stopReceiving();
		cm.sendTo(dp);

		for(int i=3;i<6;i++){
			byte[] bufferL = new byte[1];
			bufferL[0]=(byte)i;
			dp = new DatagramPacket(bufferL,bufferL.length,localhost,5000);
			System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
			cm.stopReceiving();
			cm.sendTo(dp);
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cm.resumeReceiving();

		cm.close();
	}	
}

class TestObserver implements Observer{

	public TestObserver(){
		System.out.println("testObserver: creato");
	}

	public static String convertToString(byte[] content){
		String res = "";
		//for(int i = 0;i<1;i++)res = res + content[i] +", ";
		res = res + content[0];
		return res;
	}

	@Override
	public void update(Observable o, Object arg) {
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("\tObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		System.out.println("\tObserver: dati ricevuti: " +  TestObserver.convertToString(dp.getData()));
	}
}*/
