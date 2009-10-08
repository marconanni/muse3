package relay.connection;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observer;
import java.util.Observable;


/**Classe che rappresenta un oggeto che è capace di ricevere messaggi su una Socket e contemporaneamente inviarne altri su
 * un'altra Socket per quel che riguarda la rete Ad-Hoc. Inoltre può inviare in maniera indipendente messaggi a destinatari
 * sulla rete Managed.
 * @author Luca Campeti
 */
public class MAConnectionManager extends AConnectionManager {

	private DatagramSocket managedOutputSocket = null;
	
	private InetAddress localManagedAddress = null;

	/**Metodo per ottenere un MAConnectionManager
	 * @param localAdHocAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Ad-Hoc
	 * @param localAdHocInputPort un int che rappresenta la porta su cui si ricevono i messaggi provenienti dalla rete Ad-Hoc
	 * @param localAdHocOutputPort un int che rappresenta la porta da cui si inviano i messaggi verso la rete Ad-Hoc
	 * @param localManagedAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Managed
	 * @param localManagedOutputPort un int che rappresenta la porta da cui si inviano i messaggi verso la rete Managed
	 * @param observer l'Observer che deve essere avvertito da parte del MAConnectionManager dell'arrivo di un messaggio 
	 */
	public MAConnectionManager(String localAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, String localManagedAddress,  int localManagedOutputPort, Observer observer){
		super( localAdHocAddress,localAdHocInputPort,localAdHocOutputPort, observer);
			
		if(localManagedAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");
		
		
		
		try {
			this.localManagedAddress = InetAddress.getByName(localManagedAddress);
		} catch (UnknownHostException e1) {e1.printStackTrace();}
		
		
		try {
			managedOutputSocket = new DatagramSocket(localManagedOutputPort,this.localManagedAddress);
		} catch (SocketException e) {e.printStackTrace();}
	}

	/**Metodo per inviare un DatagramPacket alla rete Managed
	 * @param dp il DatagramPacket che si vuole inviare alla rete Managed
	 */
	public void sendToServer(DatagramPacket dp){
		try {
				managedOutputSocket.send(dp);
				System.out.println(managerName+".sendToServer(): messaggio inviato al Server");
		} catch (IOException e) {e.printStackTrace();}
	}

	/**Metodo per chiudere le Socket del MAConnectionManager  
	 */
	public void close(){
		if(managedOutputSocket != null) managedOutputSocket.close();
		managedOutputSocket = null;
		super.close();
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

		MAConnectionManager cm = new MAConnectionManager(localhost,5000,6000,localhost,8000,to);
		cm.start();

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer = {1};
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,localhost,5000);
		System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
		cm.sendTo(dp, false);

		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer1 = {2};
		dp = new DatagramPacket(buffer1,buffer1.length,localhost,5000);
		System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
		cm.stopReceiving();
		cm.sendTo(dp,true);

		for(int i=3;i<6;i++){
			byte[] bufferL = new byte[1];
			bufferL[0]=(byte)i;
			dp = new DatagramPacket(bufferL,bufferL.length,localhost,5000);
			System.out.println("dp: "+ TestObserver.convertToString(dp.getData()));
			cm.stopReceiving();
			cm.sendTo(dp, false);
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


