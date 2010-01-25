package relay.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.PortConfiguration;

import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import debug.DebugConsole;


/**Classe che rappresenta un oggetto in grado di rispondere a messaggi broadcast WHO_IS_RELAY con un messaggio
 * IM_RELAY nel caso il nodo locale sia il Relay attuale del cluster di riferimento.
 * @author Pire Dejaco
 * @version 1.1
 *
 */
public class WhoIsRelayServer implements Observer {

	private RelayCM whoIsRealayManger = null;
	private RelayMessageReader rmr = null;
	private DebugConsole console = null;


	/**Metodo per ottenere un WhoIsRelayServer
	 * @param localAddress l'indirizzo locale del nodo
	 * @param localInOutPort la porta tramite cui ricevere i messaggi
	 */
	public WhoIsRelayServer(DebugConsole console){
		this.console=console;
		whoIsRealayManger = RelayConnectionFactory.getWhoIsRelayConnectionManager(this,true);
	}
	
	public void start(){
		this.whoIsRealayManger.start();
		console.debugMessage(DebugConfiguration.DEBUG_INFO,"WhoIsRelayServer: partito");
	}


	/**Metodo per chiudere la socket su cui l'AConnectionReceiver riceve i messaggi
	 */
	public void close(){
		this.whoIsRealayManger.close();
		console.debugMessage(DebugConfiguration.DEBUG_INFO,"WhoIsRelayServer: fermato");
	}

	/**Metodo per ottenere il nome del Manager che sta utilizzando l'AConnectionReceiver
	 * @return una String che rappresenta il nome del Manager che sfrutta l'AConnectionReceiver
	 */
	public String getManagerName() {
		return this.whoIsRealayManger.getManagerName();
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
			System.out.println("WHOISRELAYSERVER ricevuto messaggio");
		// TODO Auto-generated method stub
			try {
				rmr = new RelayMessageReader();
			
				rmr.readContent((DatagramPacket)arg);
			
				if(rmr.getCode()==MessageCodeConfiguration.WHO_IS_RELAY){
					InetAddress remoteAddress = dp.getAddress();
					int remotePort = dp.getPort();
					console.debugMessage(DebugConfiguration.DEBUG_INFO, this.whoIsRealayManger.getManagerName()+": ricevuto WHO_IS_RELAY da: "+remoteAddress.getHostAddress()+ " dalla porta: " +remotePort);
					dp = RelayMessageFactory.buildImRelay(remoteAddress,(remotePort == PortConfiguration.CLIENT_PORT_ELECTION_OUT)?PortConfiguration.CLIENT_PORT_ELECTION_IN : PortConfiguration.RELAY_ELECTION_CLUSTER_PORT_IN);
		
					this.whoIsRealayManger.sendTo(dp);
					//System.out.println( this.whoIsRealayManger.getManagerName()+":  messaggio IM_RELAY inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ remotePort);
					console.debugMessage(DebugConfiguration.DEBUG_INFO, this.whoIsRealayManger.getManagerName()+":  messaggio IM_RELAY inviato a: "	+ remoteAddress.getHostAddress() + " alla porta: "+ ((remotePort == PortConfiguration.CLIENT_PORT_ELECTION_OUT)?PortConfiguration.CLIENT_PORT_ELECTION_IN : PortConfiguration.RELAY_ELECTION_CLUSTER_PORT_IN));
				}
			} catch (IOException e) {e.printStackTrace();}
			dp=null;
		}
		
	}
}