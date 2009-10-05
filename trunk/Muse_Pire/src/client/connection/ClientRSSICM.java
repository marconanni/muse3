package client.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Observer;

import parameters.Parameters;

/**
 * @author Luca Campeti
 *
 */
public class ClientRSSICM {
	
	AConnectionReceiver cr = null;
	AConnectionSender cs = null;
	Thread clientRSSICMThread_IN = null;
	Thread clientRSSICMThread_OUT = null;
	DatagramSocket outSocket = null;

	public ClientRSSICM(int localRSSIPortIn,int localRSSIPortOut, Observer observer){
		cr = new AConnectionReceiver(observer,localRSSIPortIn);
		cr.setManagerName("ClientRSSICM_IN");
		clientRSSICMThread_IN = new Thread(cr);
		cs = new AConnectionSender(observer,localRSSIPortOut);
		cs.setManagerName("ClientRSSICM_OUT");
		clientRSSICMThread_OUT = new Thread(cs);
	}
	
	public void start(){
		if(clientRSSICMThread_IN != null)clientRSSICMThread_IN.start();
		if(clientRSSICMThread_OUT != null)clientRSSICMThread_OUT.start();
	}

	public void sendTo(DatagramPacket notifyRSSI){
		
		cs.sendTo(notifyRSSI);
		
	}
	
	public void close(){
		if(clientRSSICMThread_IN != null){
			cr.close();
			clientRSSICMThread_IN=null;
		}
		if(clientRSSICMThread_OUT != null){
			cr.close();
			clientRSSICMThread_OUT=null;
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