package server.connection;

import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;

import parameters.Parameters;

import server.ServerPortMapper;

/**Classe statica che permette ai vari componenti del sistema di ottenere l'opportuno ConnectionManager 
 * @author Luca Campeti	
 */
public class ServerConnectionFactory {
	
	private static ServerPortMapper spm = ServerPortMapper.getInstance();
		
	/**Metoto statico per ottenere un'istanza di ServerCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ServerCM
	 * @return un istanza di ServerCM
	 */
	public static ServerCM getSessionConnectionManager(Observer obser){
		return new ServerCM(spm.getLocalHostAddress().getHostAddress(),Parameters.SERVER_SESSION_PORT_IN,obser);
	}
	
	/**Metoto statico per ottenere un'istanza di StreamingServerCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal StreamingServerCM
	 * @return un istanza di StreamingServerCM
	 */
	public static StreamingServerCM getStreamingServerConnectionManager(Observer obser){
		return new StreamingServerCM(spm.getLocalHostAddress(),spm.getFirstFreeControlPort(),obser);	
	}
}


/*class TestServerConnectionFactory{
	public static void main(String args[]){
		TestObserver to = new TestObserver();
		ServerPortMapper spm = ServerPortMapper.getInstance();
		ServerCM scm = ServerConnectionFactory.getSessionConnectionManager(to);
		StreamingServerCM sscm = ServerConnectionFactory.getStreamingServerConnectionManager(to);
		scm.start();
		sscm.start();
		DatagramPacket dp = null;
		byte[] buffer1 = new byte[1];
		buffer1[0]=32;
		dp = new DatagramPacket(buffer1,buffer1.length,spm.getLocalHostAddress(),spm.getPortServerIn());
		sscm.sendTo(dp);
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
		System.out.println("\tObserver: ricevuto da: " +  o.getClass().toString());
	}
}*/