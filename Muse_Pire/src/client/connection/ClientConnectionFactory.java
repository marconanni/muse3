package client.connection;

import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;


import client.ClientPortMapper;


/**Classe statica che permette ai vari componenti del sistema di ottenere l'opportuno ConnectionManager 
 * @author Luca Campeti	
 *
 */
public class ClientConnectionFactory {

	private static ClientPortMapper cpm = ClientPortMapper.getInstance();

	/**Metoto statico per ottenere un'istanza di ClientSessionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientSessionCM
	 * @return un istanza di ClientSessionCM
	 */
	public static ClientSessionCM getSessionConnectionManager(Observer obser){
		return new ClientSessionCM(cpm.getLocalHostAddress().getHostAddress(),cpm.getPortSessionIn(),cpm.getPortSessionOut(),obser);
	}

	/**Metoto statico per ottenere un'istanza di ClientElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientElectionCM
	 * @return un istanza di ClientElectionCM
	 */
	public static ClientElectionCM getElectionConnectionManager(Observer obser){
		return new ClientElectionCM(cpm.getLocalHostAddress().getHostAddress(),cpm.getPortElectionIn(),cpm.getPortElectionOut(),obser);
	}

	/**Metoto statico per ottenere un'istanza di ClientRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientRSSICM
	 * @return un istanza di ClientRSSICM
	 */
	public static ClientRSSICM getRSSIConnectionManager(Observer obser){
		return new ClientRSSICM(cpm.getLocalHostAddress().getHostAddress(),cpm.getPortRSSI(),obser);
	}

}


/*class TestClientConnectionFactory{
	public static void main(String args[]){
		TestObserver to = new TestObserver();
		ClientPortMapper cpm = ClientPortMapper.getInstance();
		ClientSessionCM cscm = ClientConnectionFactory.getSessionConnectionManager(to);
		ClientElectionCM cecm = ClientConnectionFactory.getElectionConnectionManager(to);
		ClientRSSICM crssicm = ClientConnectionFactory.getRSSIConnectionManager(to);
		cscm.start();
		cecm.start();
		crssicm.start();

		DatagramPacket dp = null;
		byte[] buffer1 = new byte[1];
		buffer1[0]=1;
		//ClientElectionManager invia un messaggio al ClientSessionManager
		dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),cpm.getPortSessionIn());
		cecm.sendTo(dp);

		//ClientSessionManager invia un messaggio al ClientRSSIManager
		buffer1[0]=2;
		dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),cpm.getPortRSSI());
		cscm.sendTo(dp);
		cscm.sendTo(dp);
		
		//ClientRSSIManager invia un messaggio al ClientSessionManager
		buffer1[0]=3;
		dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),cpm.getPortSessionIn());
		crssicm.sendTo(dp);
		
		//ClientSessionManager invia un messaggio al ClientElectionManager
		buffer1[0]=4;
		dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),cpm.getPortElectionIn());
		cscm.sendTo(dp);
		
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
		System.out.println("\tObserver: ricevuto datagramma da: " +  o.getClass().toString());
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("\tObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		System.out.println("\tObserver: dati ricevuti: " +  TestObserver.convertToString(dp.getData()));

	}
}*/