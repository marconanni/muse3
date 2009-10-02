package relay.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import client.ClientMessageReader;

import parameters.Parameters;

import relay.RelayMessageFactory;
import relay.RelayMessageReader;


/**Classe che rappresenta un oggetto in grado di rispondere a messaggi broadcast WHO_IS_RELAY con un messaggio
 * IM_RELAY nel caso il nodo locale sia il Relay attuale.
 * @author Luca Campeti
 *
 */
public class WhoIsRelayServer extends Thread{

	private DatagramSocket inOutSocket = null;
	private DatagramPacket dp = null; 

	private RelayMessageReader rmr = null;
	private byte[] buffer = null;

	private boolean started = false;
	protected String managerName = "WhoIsRelayServer";

	private String localAddress = null;
	private int localInOutPort = -1;

	/**Metodo per ottenere un WhoIsRelayServer
	 * @param localAddress l'indirizzo locale del nodo
	 * @param localInOutPort la porta tramite cui ricevere i messaggi
	 */
	public WhoIsRelayServer( String localAddress, int localInOutPort){

		if(localAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");

		this.localAddress = localAddress;
		this.localInOutPort = localInOutPort;

		try {
			inOutSocket = new DatagramSocket(localInOutPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}


	public void run(){
		buffer = new byte[256];
		started = true;
		while(true){

			//System.out.println(managerName+": attesa messaggio WHO_IS_RELAY su porta: " + localInOutPort);
			dp = new DatagramPacket(buffer, buffer.length);

			try {
				inOutSocket.receive(dp);
				rmr = new RelayMessageReader();
				rmr.readContent(dp);

				if(rmr.getCode() == Parameters.WHO_IS_RELAY){

					InetAddress remoteAddress = dp.getAddress();
					int remotePort = dp.getPort();

					System.out.println(managerName+": ricevuto WHO_IS_RELAY da: " 
							+remoteAddress.getHostAddress() 
							+ " dalla porta: " +remotePort);

					dp = RelayMessageFactory.buildImRelay(localAddress, remoteAddress, 
							(remotePort == Parameters.CLIENT_PORT_ELECTION_OUT)?Parameters.CLIENT_PORT_ELECTION_IN : Parameters.RELAY_ELECTION_PORT_IN);
					
					//ClientMessageReader cmr = new ClientMessageReader();
					//cmr.readContent(dp);
					
					//System.out.println("cmr.getCode(): "+cmr.getCode()+ " cmr.getRelayAddress(): " + cmr.getRelayAddress());
					
					inOutSocket.send(dp);
					System.out.println( managerName+":  messaggio IM_RELAY inviato a: "
							+ remoteAddress.getHostAddress() + " alla porta: " 
							+ remotePort);
				}
			}
			catch (IOException e) {
				System.out.println(managerName+": " + e.getMessage());
				break;
			}

			dp=null;
		}
	}

	/**Metodo per chiudere la socket su cui l'AConnectionReceiver riceve i messaggi
	 */
	public void close(){
		if(inOutSocket != null) inOutSocket.close();
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



class TestWhoIsRelayServer {

	public static void main(String args[]){
		System.out.println("TestWhoIsRelayServer");

		InetAddress BCAST = null;
		DatagramSocket ds = null;
		DatagramPacket dp = null;
		ClientMessageReader cmr = null; 

		WhoIsRelayServer wirs = new WhoIsRelayServer(Parameters.RELAY_AD_HOC_ADDRESS, Parameters.WHO_IS_RELAY_PORT);
		wirs.start();

		/*try {
			BCAST = InetAddress.getByName(Parameters.BROADCAST_ADDRESS);

			ds = new DatagramSocket(9999);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}


		for(int i = 0; i<50; i++){
			try {
				
				dp = RelayMessageFactory.buildWhoIsRelay(BCAST, Parameters.WHO_IS_RELAY_PORT);
				ds.send(dp);
				System.out.println("TestWhoIsRelayServer: inviato WHO_IS_RELAY in Broadcast alla porta " + Parameters.WHO_IS_RELAY_PORT);
				
				byte[] buffer = new byte[256];
				DatagramPacket in = new DatagramPacket(buffer,buffer.length);
				
				ds.receive(in);
				cmr = new ClientMessageReader();
				cmr.readContent(in);
				if(cmr.getCode() == Parameters.IM_RELAY){

					System.err.println("TestWhoIsRelayServer: ricevuto IM_RELAY da: " 
										+ in.getAddress().getHostAddress() 
										+ " dalla porta: " +in.getPort());
					
					System.err.println("TestWhoIsRelayServer: il Relay è :" +cmr.getActualRelayAddress()); 

				}
						
				Thread.sleep(4000);
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		//wirs.close();
	}
}