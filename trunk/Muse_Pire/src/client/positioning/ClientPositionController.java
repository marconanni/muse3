package client.positioning;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.Parameters;

import relay.RelayMessageReader;
import relay.positioning.RelayPositionClientsMonitor;
import client.wnic.exception.InvalidParameter;
import client.wnic.exception.WNICException;


import client.ClientMessageFactory;
import client.ClientMessageReader;
import client.connection.ClientConnectionFactory;
import client.connection.ClientRSSICM;
import client.wnic.ClientDummyController;
import client.wnic.ClientWNICController;
import client.wnic.ClientWNICLinuxController;


/**Classe che rappresenta un oggetto che è in grado di rispondere alle richieste del Relay 
 * che sta servendo questo nodo Client in merito alla potenza di segnale rilevata dal Client 
 * stesso nei confronti del nodo Relay
 * @author Luca Campeti	
 */
public class ClientPositionController implements Observer{

	private DatagramPacket notifyRSSI = null;
	private String interfaceToManage ="eth1";
	private boolean enableToMonitor;
	private boolean started;

	private ClientRSSICM  crcm = null ;
	private ClientWNICController cwnic = null; 

	private InetAddress relayAddress = null;
	private static int sequenceNumber = 0;

	private ClientMessageReader cmr = null;


	/**Metodo per ottenere un ClientPositionController
	 * @param interf una String che rappresenta l'interfaccia di rete da gestire
	 * @param essidName una String che rappresenta la rete a cui l'interfaccia è connessa
	 * @throws WNICException 
	 */
	public ClientPositionController(String interf, String essidName) throws WNICException{
		interfaceToManage = interf;

		crcm = ClientConnectionFactory.getRSSIConnectionManager(this);
		
		/**DISABILITARE IL CLIENTWNICLINUXCONTROLLER E ABILITARE IL CLIENTDUMMYCONTROLLER SOLO PER I TEST**/
		//cwnic = new ClientDummyController();
		cwnic = new ClientWNICLinuxController(interfaceToManage, essidName);
		
		enableToMonitor = false;
		started = false;
	}

	/**Metodo per far partire il ClientPositionController
	 * che, da questa chiamata in poi, può rispondere alle
	 * richieste provenienti dal nodo Relay
	 */
	public void start(){
		if(enableToMonitor){
			crcm.start();
			started = true;
		}
	}


	/**Metodo per chiudere il ClientPositionController
	 */
	public void close(){
		if(started){
			started = false;
			crcm.close();
			try {
				cwnic.resetAddressToMonitor(interfaceToManage);
			} catch (WNICException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cwnic = null;
		}
	}


	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */

	public synchronized void update(Observable arg0, Object arg1) {

		if(arg1 instanceof DatagramPacket){

			int RSSIvalue =-1;
			cmr = new ClientMessageReader();
			try {
				cmr.readContent((DatagramPacket)arg1);
				System.out.println("ClientPositionController.update(): ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if(cmr.getCode() == Parameters.REQUEST_RSSI){
					RSSIvalue = cwnic.getSignalStrenghtValue();
					notifyRSSI = ClientMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue, relayAddress, Parameters.RELAY_RSSI_RECEIVER_PORT);
					sequenceNumber++;
					crcm.sendTo(notifyRSSI);
					System.out.println("ClientPositionController.update(): inviato RSSI: "+ RSSIvalue +" a: " + relayAddress+":"+Parameters.RELAY_RSSI_RECEIVER_PORT);
				}	
			} catch (WNICException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			cmr = null;
		}
	}


	/**Metodo per impostare l'osservazione dei valori di RSSI nei 
	 * confronti dell'indirizzo del Relay
	 * @param rA una String che rappresenta l'indirizzo del Relay
	 */
	public void setRelayAddress(String rA) {
		try {
			relayAddress = InetAddress.getByName(rA);
			cwnic.setRelayAddress(rA);
			enableToMonitor = true;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (WNICException e) {
			e.printStackTrace();
		}
	}
}


class TestClientPositionController {
	public static void main(String args[]){
		System.out.println("TestClientPositionController");
		TestObserver to = new TestObserver();
		ClientPositionController cpc = null;
		try {
			cpc = new ClientPositionController("wlan0","lord");
			cpc.setRelayAddress("192.168.1.3");
			cpc.start();
		} catch (WNICException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//RelayPositionClientsMonitor rpcm = new RelayPositionClientsMonitor(15,3000,to);
		//rpcm.start();		

		try {
			Thread.sleep(12010);
			cpc.setRelayAddress("192.168.1.3");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		try {
			Thread.sleep(12010);
			cpc.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//rpcm.close();

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
		String dp  = (String)arg;
		System.out.println("\tObserver: ricevuta notifica: " + dp);
		System.out.println("\tObserver: notifica ricevuta da: " + ((RelayPositionClientsMonitor)o).toString());
	}
}

/*class TestClientPositionController {
	public static void main(String args[]){
		System.out.println("TestClientPositionController");
		TestObserver to = new TestObserver();
		RelayPositionClientsMonitor rpcm = new RelayPositionClientsMonitor(3,3000,to);
		rpcm.start();		

		try {
			Thread.sleep(11990);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		rpcm.close();
		to.close();
	}	
}


class TestObserver implements Observer{

	private ClientRSSICM crcm = null;
	private ClientMessageReader cmr = null;
	private RelayMessageReader rmr = null;

	public TestObserver(){
		System.out.println("testObserver: creato");
		crcm = ClientConnectionFactory.getRSSIConnectionManager(this);
		crcm.start();
	}

	public static String convertToString(byte[] content){
		String res = "";
		//for(int i = 0;i<1;i++)res = res + content[i] +", ";
		res = res + content[0];
		return res;
	}


	public synchronized void  update(Observable o, Object arg) {
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("\tObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		int code = -1;
		cmr = new ClientMessageReader();
		try {
			cmr.readContent(dp);
			code = cmr.getCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(code == Parameters.REQUEST_RSSI){

			System.out.println("\tObserver: arrivato REQUEST_RSSI....");

			InetAddress ia = null;
			try {
				ia = InetAddress.getByName(Parameters.RELAY_AD_HOC_ADDRESS);
				DatagramPacket notRSSI = ClientMessageFactory.buildNotifyRSSI(0, 666,ia, Parameters.RELAY_RSSI_RECEIVER_PORT);
				crcm.sendTo(notRSSI);
				System.out.println("update(): inviato il messaggio NOTIFY_RSSI");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if(code == Parameters.NOTIFY_RSSI){
			System.out.println("\tObserver: arrivato NOTIFY_RSSI -> valore ottenuto:  " + rmr.getRSSI());
		}
	}

	public void close(){
		crcm.close();
		crcm = null;
	}
}*/