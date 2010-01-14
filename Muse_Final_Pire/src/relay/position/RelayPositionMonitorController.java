package relay.position;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.PortConfiguration;

import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.wnic.RelayWNICController;
import relay.wnic.WNICFinder;
import relay.wnic.exception.OSException;
import relay.wnic.exception.WNICException;

import debug.DebugConsole;

public class RelayPositionMonitorController implements Observer {
	private DatagramPacket notifyRSSI = null;
	private boolean enableToMonitor;
	private boolean started;

	private RelayCM  rrscm = null ;
	private RelayWNICController cwnic = null; 

	private static int sequenceNumber = 0;

	private RelayMessageReader cmr = null;
	private DebugConsole console = null;
	
	/**Metodo per ottenere un ClientPositionController
	 * @param interf una String che rappresenta l'interfaccia di rete da gestire
	 * @param essidName una String che rappresenta la rete a cui l'interfaccia è connessa
	 * @throws WNICException 
	 */
	public RelayPositionMonitorController(String interf, String essidName) throws WNICException{
		rrscm = RelayConnectionFactory.getRSSIClusterHeadConnectionManager(this);
		
		try {
			cwnic = WNICFinder.getCurrentWNIC(interf, essidName,0);
		} catch (OSException e) {new WNICException("RelayPositionMonitorController: Errore nel creare il controller per la scheda wireless ["+e.getMessage()+"]");}
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
			rrscm.start();
			started = true;
		}
	}
	
	public void stopReceiving(){
		rrscm.stopReceiving();
		enableToMonitor = false;
		started = false;
	}
	
	public void resumeReceiving(){
		if(enableToMonitor){
			rrscm.resumeReceiving();
			started = true;
		}
	}

	/**Metodo per chiudere il ClientPositionController
	 */
	public void close(){
		if(started){
			started = false;
			rrscm.close();
			cwnic = null;
		}
	}

	public synchronized void update(Observable arg0, Object arg1) {

		if(arg1 instanceof DatagramPacket){
			int RSSIvalue =-1;
			cmr = new RelayMessageReader();
			try {
				cmr.readContent((DatagramPacket)arg1);
				console.debugMessage(DebugConfiguration.DEBUG_WARNING, "RelayPositionMonitorController: ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if((cmr.getCode() == MessageCodeConfiguration.REQUEST_RSSI)){
					RSSIvalue = cwnic.getSignalStrenghtValue();
					notifyRSSI = RelayMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue,((DatagramPacket)arg1).getAddress(), PortConfiguration.RSSI_PORT_IN);
					sequenceNumber++;
					rrscm.sendTo(notifyRSSI);
					console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionMonitorController(): Inviato RSSI: "+ RSSIvalue +" a: " + ((DatagramPacket)arg1).getAddress()+":"+PortConfiguration.RSSI_PORT_IN);
				}else{
					console.debugMessage(DebugConfiguration.DEBUG_INFO,"ClientPositionController.update(): Faccio niente");
				
				}
			} catch (WNICException e) {
					console.debugMessage(DebugConfiguration.DEBUG_ERROR,"ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
					new WNICException("ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
				
			} catch (IOException e) {e.printStackTrace();}
			cmr = null;
		}
	}

	public RelayWNICController getRelayWNICController(){
		return this.cwnic;
	}
	
	/**Server per visualizzare i messagi di debug
	 */
	public void setDebugConsole(DebugConsole console) {this.console=console;}
}
