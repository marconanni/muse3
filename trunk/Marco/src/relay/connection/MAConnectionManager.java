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
 * @author Luca Campeti, Marco Nanni
 */
public class MAConnectionManager extends AConnectionManager {

	private DatagramSocket managedOutputSocket = null;
	
	private String localManagedAddress = null;
	private int localManagedOutputPort = -1;

	/**Metodo per ottenere un MAConnectionManager
	 * @param localAdHocAddress un InetAddress che rappresenta l'indirizzo locale del nodo sulla rete Ad-Hoc
	 * @param localAdHocInputPort un int che rappresenta la porta su cui si ricevono i messaggi provenienti dalla rete Ad-Hoc
	 * @param localAdHocOutputPort un int che rappresenta la porta da cui si inviano i messaggi verso la rete Ad-Hoc
	 * @param localManagedAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Managed
	 * @param localManagedOutputPort un int che rappresenta la porta da cui si inviano i messaggi verso la rete Managed
	 * @param observer l'Observer che deve essere avvertito da parte del MAConnectionManager dell'arrivo di un messaggio 
	 *@param bcast : se settato a true si abilita anche la ricezione dei messaggi di broadcast sulla rete ad hoc
	 */
	public MAConnectionManager(InetAddress localAdHocAddress, InetAddress broadcastAdHocAddress, int localAdHocInputPort, int localAdHocOutputPort, String localManagedAddress,  int localManagedOutputPort, Observer observer, boolean bcast){
		super( localAdHocAddress,broadcastAdHocAddress,localAdHocInputPort,localAdHocOutputPort, observer,bcast);
			
		if(localManagedAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");
		
		this.localManagedAddress = localManagedAddress;
		this.localManagedOutputPort = localManagedOutputPort;
		
		try {
			managedOutputSocket = new DatagramSocket(localManagedOutputPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Metodo per inviare un DatagramPacket alla rete Superiore
	 * Il "to server" è da interpretare in maniera più lasca, un relay potrebbe fare una sendtoserver al BigBoss
	 * @param dp il DatagramPacket che si vuole inviare alla rete Managed
	 */
	public void sendToServer(DatagramPacket dp){
		try {
				managedOutputSocket.send(dp);
				System.out.println(managerName+".sendToServer(): messaggio inviato al Server");
				//System.out.println(managerName+".sendToServer(): messaggio inviato da: "+localManagedAddress+" porta: "+localManagedOutputPort);
				//System.out.println(managerName+".sendToServer(): messaggio inviato a: "+dp.getAddress().getHostAddress()+" porta: "+ dp.getPort());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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


