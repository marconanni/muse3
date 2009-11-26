package relay.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
import debug.DebugConsole;
import parameters.Parameters;
import relay.RelayMessageFactory;
import relay.RelayMessageReader;


/**Classe che rappresenta un oggetto in grado di rispondere a messaggi broadcast WHO_IS_RELAY con un messaggio
 * IM_RELAY nel caso il nodo locale sia il Relay attuale.
 * @author Luca Campeti
 *
 */
public class WhoIsRelayServer implements Observer {

	private RelayCM whoIsRealayManger = null;
	private RelayMessageReader rmr = null;
	private boolean imBigBoss = false;
	private DebugConsole console = null;


	/**Metodo per ottenere un WhoIsRelayServer
	 * @param localAddress l'indirizzo locale del nodo
	 * @param localInOutPort la porta tramite cui ricevere i messaggi
	 */
	public WhoIsRelayServer(boolean imBigBoss, DebugConsole console){
		this.console=console;
		this.imBigBoss = imBigBoss;
		whoIsRealayManger = RelayConnectionFactory.getWhoIsRelayConnectionManager(this);

	}
	
	public void start(){
		this.whoIsRealayManger.start();
		console.debugMessage(Parameters.DEBUG_INFO,"WhoIsRelayServer: partito");
	}


	/**Metodo per chiudere la socket su cui l'AConnectionReceiver riceve i messaggi
	 */
	public void close(){
		this.whoIsRealayManger.close();
		console.debugMessage(Parameters.DEBUG_INFO,"WhoIsRelayServer: fermato");
	}

	/**Metodo per ottenere il nome del Manager che sta utilizzando l'AConnectionReceiver
	 * @return una String che rappresenta il nome del Manager che sfrutta l'AConnectionReceiver
	 */
	public String getManagerName() {
		return this.whoIsRealayManger.getManagerName();
	}
	
	public boolean getImBigBoss() {
		return this.imBigBoss;
	}

	/**Metodo per impostare il nome del Manager che sta utilizzando l'AConnectionReceiver
	 * @param managerName una String che rappresenta il nome del Manager che sfrutta l'AConnectionReceiver 
	 */
	public void setManagerName(String managerName) {
		this.whoIsRealayManger.setNameManager(managerName);
	}


	@Override
	public void update(Observable o, Object arg) {
		
		if(arg instanceof DatagramPacket){
			DatagramPacket dp =(DatagramPacket) arg;
		// TODO Auto-generated method stub
			try {
				rmr = new RelayMessageReader();
			
				rmr.readContent((DatagramPacket)arg);
			
				if(rmr.getCode()==Parameters.WHO_IS_RELAY){
					InetAddress remoteAddress = dp.getAddress();
					int remotePort = dp.getPort();
					//System.out.println(this.whoIsRealayManger.getManagerName()+": ricevuto WHO_IS_RELAY da: "+remoteAddress.getHostAddress()+ " dalla porta: " +remotePort);
					console.debugMessage(Parameters.DEBUG_INFO, this.whoIsRealayManger.getManagerName()+": ricevuto WHO_IS_RELAY da: "+remoteAddress.getHostAddress()+ " dalla porta: " +remotePort);
					dp = RelayMessageFactory.buildImRelay(remoteAddress,(remotePort == Parameters.CLIENT_PORT_ELECTION_OUT)?Parameters.CLIENT_PORT_ELECTION_IN : Parameters.RELAY_ELECTION_PORT_IN);
		
					this.whoIsRealayManger.sendTo(dp);
					//System.out.println( this.whoIsRealayManger.getManagerName()+":  messaggio IM_RELAY inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ remotePort);
					console.debugMessage(Parameters.DEBUG_INFO, this.whoIsRealayManger.getManagerName()+":  messaggio IM_RELAY inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ ((remotePort == Parameters.CLIENT_PORT_ELECTION_OUT)?Parameters.CLIENT_PORT_ELECTION_IN : Parameters.RELAY_ELECTION_PORT_IN));
				}
				if((rmr.getCode()==Parameters.WHO_IS_BIGBOSS)&&(imBigBoss)){
					InetAddress remoteAddress = dp.getAddress();
					int remotePort = dp.getPort();
					//System.out.println(this.whoIsRealayManger.getManagerName()+": ricevuto WHO_IS_BIGBOSS da: "+remoteAddress.getHostAddress()+ " dalla porta: " +remotePort);
					console.debugMessage(Parameters.DEBUG_INFO, this.whoIsRealayManger.getManagerName()+": ricevuto WHO_IS_BIGBOSS da: "+remoteAddress.getHostAddress()+ " dalla porta: " +remotePort);
					dp = RelayMessageFactory.buildImBigBoss(remoteAddress,(remotePort == Parameters.CLIENT_PORT_ELECTION_OUT)?Parameters.CLIENT_PORT_ELECTION_IN : Parameters.RELAY_ELECTION_PORT_IN);
		
					this.whoIsRealayManger.sendTo(dp);
					//System.out.println( this.whoIsRealayManger.getManagerName()+":  messaggio IM_BIGBOSS inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ remotePort);
					console.debugMessage(Parameters.DEBUG_INFO,  this.whoIsRealayManger.getManagerName()+":  messaggio IM_BIGBOSS inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ ((remotePort == Parameters.CLIENT_PORT_ELECTION_OUT)?Parameters.CLIENT_PORT_ELECTION_IN : Parameters.RELAY_ELECTION_PORT_IN));
				}
			} catch (IOException e) {e.printStackTrace();}
			dp=null;
		}
		
	}
}



class TestWhoIsRelayServer {

	public static void main(String args[]){
		System.out.println("TestWhoIsRelayServer");

		//WhoIsRelayServer wirs = new WhoIsRelayServer(true);
		//wirs.start();

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