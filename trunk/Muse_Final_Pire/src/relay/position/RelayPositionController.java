package relay.position;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.PortConfiguration;

import relay.RelayElectionManager;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.WNICException;

import debug.DebugConsole;

public class RelayPositionController implements Observer {
	private DatagramPacket notifyRSSI = null;
	private boolean enableToMonitor;
	private boolean started;

	private RelayCM  rrscm = null ;
	private RelayWNICController cwnic = null; 

	private static int sequenceNumber = 0;

	private RelayMessageReader cmr = null;
	private RelayElectionManager electionManger = null;
	private DebugConsole console = null;
	
	private boolean debug = false;
	
	/**Metodo per ottenere un ClientPositionController
	 * @param interf una String che rappresenta l'interfaccia di rete da gestire
	 * @param essidName una String che rappresenta la rete a cui l'interfaccia è connessa
	 * @throws WNICException 
	 */
	public RelayPositionController(RelayWNICController cwnic, RelayElectionManager electionManager) throws WNICException{
		rrscm = RelayConnectionFactory.getRSSIClusterHeadConnectionManager(this,true);
		this.electionManger = electionManager;
		this.cwnic = cwnic;
		setDebugConsole(cwnic.getDebugConsole());
		started = false;
	}

	/**Metodo per far partire il ClientPositionController
	 * che, da questa chiamata in poi, può rispondere alle
	 * richieste provenienti dal nodo Relay
	 */
	public void start(){
			rrscm.start();
			started = true;
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
				if(debug)debug(getDebugConsole(), DebugConfiguration.DEBUG_WARNING, "RelayPositionMonitorController: ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if((cmr.getCode() == MessageCodeConfiguration.REQUEST_RSSI)){
					RSSIvalue = cwnic.getSignalStrenghtValue();
					notifyRSSI = RelayMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue,((DatagramPacket)arg1).getAddress(), PortConfiguration.RSSI_PORT_IN, MessageCodeConfiguration.TYPERELAY, (electionManger.getActiveClient()==0||electionManger.getActiveClient()==0)?0:electionManger.getActiveClient());
					sequenceNumber++;
					rrscm.sendTo(notifyRSSI);
					if(debug)debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionMonitorController(): Inviato RSSI: "+ RSSIvalue +" a: " + ((DatagramPacket)arg1).getAddress()+":"+PortConfiguration.RSSI_PORT_IN);
				}
			} catch (WNICException e) {
				debug(getDebugConsole(), DebugConfiguration.DEBUG_ERROR,"RelayPositionMonitorController: Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
					new WNICException("RelayPositionMonitorController: Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
				
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
	public DebugConsole getDebugConsole(){return console;}
	public void setDebug(boolean db){this.debug = db;}
	
	private void debug(DebugConsole console,int type, String message){
		if(console!=null)console.debugMessage(type, message);
		else{
			if(type==DebugConfiguration.DEBUG_INFO|| type ==DebugConfiguration.DEBUG_WARNING)
				System.out.println(message);
			if(type==DebugConfiguration.DEBUG_ERROR)
				System.err.println(message);
		}
	}
}
