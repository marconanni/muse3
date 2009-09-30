package client.connection;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observer;
import java.net.UnknownHostException;
import java.util.Observable;

import sun.misc.Cleaner;

/**
 * @author Luca Campeti
 *
 */
public class ClientRSSICM {
	
	ConnectionReceiverAndSender crs = null;
	Thread clientRSSICMThread = null;

	public ClientRSSICM(String localAddress, int localRSSIPort, Observer observer){
		crs = new ConnectionReceiverAndSender(observer,localAddress,localRSSIPort);
		crs.setManagerName("ClientRSSICM");
		clientRSSICMThread = new Thread(crs);
	}
	
	public void start(){
		if(clientRSSICMThread != null)clientRSSICMThread.start();
	}

	public void sendTo(DatagramPacket notifyRSSI){
		crs.sendTo(notifyRSSI);
	}
	
	public void close(){
		if(clientRSSICMThread != null){
			crs.close();
			clientRSSICMThread=null;
		}
	}

}


/*class TestClientRSSICM {
	
	public static void main(String args[]){
		ClientPortMapper cpm = ClientPortMapper.getInstance();
		TestRSSIObserver tro = new TestRSSIObserver();
		TestObserver to = new TestObserver();
		
		ClientRSSICM crc = ClientConnectionFactory.getRSSIConnectionManager(tro);
		AConnectionManager acm = new AConnectionManager(cpm.getLocalHostAddress(),5432,9876,to);

		crc.start();
		acm.start(); 
		
		DatagramPacket dp = null;
		for(int i=0;i<4;i++){
			byte[] buffer1 = new byte[1];
			buffer1[0]=(byte)i;
			dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),cpm.getPortRSSI());
			acm.sendTo(dp);
		}
	}
}

class TestRSSIObserver implements Observer{
	
	public TestRSSIObserver(){
		System.out.println("testRSSIObserver: creato");
	}

	public static String convertToString(byte[] content){
		String res = "";
		//for(int i = 0;i<1;i++)res = res + content[i] +", ";
		res = res + content[0];
		return res;
	}

	@Override
	public void update(Observable o, Object arg) {
		ClientPortMapper cpm = ClientPortMapper.getInstance();
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("\tRSSIObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		System.out.println("\tRSSIObserver: dati ricevuti: " +  TestObserver.convertToString(dp.getData()));
		System.out.println("\tRSSIObserver: l'Observable che me l'ha inviato è : " + o.getClass());
		byte[] buffer1 = {100};
		dp = new DatagramPacket(buffer1,buffer1.length,cpm.getLocalHostAddress(),5432);
		((ConnectionReceiverAndSender)o).sendTo(dp);
		
	}
}*/