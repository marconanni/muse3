package relay.positioning;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import client.wnic.ClientWNICController;

import parameters.Parameters;
import relay.RelayMessageFactory;
import relay.RelayMessageReader;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.wnic.RelayWNICController;
import relay.wnic.WNICFinder;
import relay.wnic.exception.OSException;
import relay.wnic.exception.WNICException;
import debug.DebugConsole;

public class RelayPositionController  implements Observer {
	private DatagramPacket notifyRSSI = null;
	private boolean enableToMonitor;
	private boolean started;

	private RelayCM  crscm = null ;
	private RelayWNICController cwnic = null; 

	private InetAddress connectedRelayAddress = null;
	private static int sequenceNumber = 0;

	private RelayMessageReader cmr = null;
	private DebugConsole console = null;
	
	/**Metodo per ottenere un ClientPositionController
	 * @param interf una String che rappresenta l'interfaccia di rete da gestire
	 * @param essidName una String che rappresenta la rete a cui l'interfaccia è connessa
	 * @throws WNICException 
	 */
	public RelayPositionController(RelayWNICController cwnic) throws WNICException{
		crscm = RelayConnectionFactory.getRSSIConnectionManager(this);
		this.cwnic=cwnic;
//		try {
//			cwnic = WNICFinder.getCurrentWNIC(interf, essidName,0);
//		} catch (OSException e) {new WNICException("ClientPositionController: Errore nel creare il controller per la scheda wireless ["+e.getMessage()+"]");}
		console = cwnic.getDebugConsole();
		enableToMonitor = false;
		started = false;
	}

	/**Metodo per far partire il ClientPositionController
	 * che, da questa chiamata in poi, può rispondere alle
	 * richieste provenienti dal nodo Relay
	 */
	public void start(){
		if(enableToMonitor){
			crscm.start();
			started = true;
		}
	}

	/**Metodo per chiudere il ClientPositionController
	 */
	public void close(){
		if(started){
			started = false;
			crscm.close();
			cwnic = null;
		}
	}

	public synchronized void update(Observable arg0, Object arg1) {

		if(arg1 instanceof DatagramPacket){

			int RSSIvalue =-1;
			cmr = new RelayMessageReader();
			try {
				cmr.readContent((DatagramPacket)arg1);
				console.debugMessage(Parameters.DEBUG_WARNING, "RelayPositionController.update(): ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if((cmr.getCode() == Parameters.REQUEST_RSSI)&&((DatagramPacket)arg1).getAddress().equals(connectedRelayAddress)){
						console.debugMessage(Parameters.DEBUG_INFO,"ClientPositionController.update(): Devo inviare risp a: " + connectedRelayAddress);
						RSSIvalue = cwnic.getSignalStrenghtValue();
						notifyRSSI = RelayMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue, connectedRelayAddress, Parameters.RSSI_PORT_IN);
						sequenceNumber++;
						crscm.sendTo(notifyRSSI);
						console.debugMessage(Parameters.DEBUG_INFO,"ClientPositionController.update(): Inviato RSSI: "+ RSSIvalue +" a: " + connectedRelayAddress+":"+Parameters.RSSI_PORT_IN);
				}
			} catch (WNICException e) {
					console.debugMessage(Parameters.DEBUG_ERROR,"ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
					new WNICException("ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
				
			} catch (IOException e) {e.printStackTrace();System.out.println("ERRORE");}
			cmr = null;
		}
	}

	
	/**Metodo per impostare l'osservazione dei valori di RSSI nei 
	 * confronti dell'indirizzo del Relay
	 * @param rA una String che rappresenta l'indirizzo del Relay
	 */
	public void setConnectedRelayAddress(String rA) {
		try {
			connectedRelayAddress = InetAddress.getByName(rA);
			enableToMonitor = true;
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Server per visualizzare i messagi di debug
	 */
	public void setDebugConsole(DebugConsole console) {this.console=console;}
}


