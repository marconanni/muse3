package client.positioning;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import parameters.Parameters;

import client.wnic.exception.OSException;
import client.wnic.exception.WNICException;


import client.ClientMessageFactory;
import client.ClientMessageReader;
import client.connection.ClientCM;
import client.connection.ClientConnectionFactory;
import client.wnic.ClientWNICController;
import client.wnic.WNICFinder;
import debug.DebugConsole;


/**Classe che rappresenta un oggetto che è in grado di rispondere alle richieste del Relay 
 * che sta servendo questo nodo Client in merito alla potenza di segnale rilevata dal Client 
 * stesso nei confronti del nodo Relay
 * @author Luca Campeti	
 */
public class ClientPositionController implements Observer{

	private DatagramPacket notifyRSSI = null;
	private boolean enableToMonitor;
	private boolean started;

	private ClientCM  crscm = null ;
	private ClientWNICController cwnic = null; 

	private InetAddress relayAddress = null;
	private static int sequenceNumber = 0;

	private ClientMessageReader cmr = null;
	private DebugConsole console = null;
	
	/**Metodo per ottenere un ClientPositionController
	 * @param interf una String che rappresenta l'interfaccia di rete da gestire
	 * @param essidName una String che rappresenta la rete a cui l'interfaccia è connessa
	 * @throws WNICException 
	 */
	public ClientPositionController(String interf, String essidName) throws WNICException{
		crscm = ClientConnectionFactory.getRSSIConnectionManager(this);
		
		try {
			cwnic = WNICFinder.getCurrentWNIC(interf, essidName);
		} catch (OSException e) {new WNICException("ClientPositionController: Errore nel creare il controller per la scheda wireless ["+e.getMessage()+"]");}
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
	
	public void stopReceiving(){
		crscm.stopReceiving();
		enableToMonitor = false;
		started = false;
	}
	
	public void resumeReceiving(){
		if(enableToMonitor){
			crscm.resumeReceiving();
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
			cmr = new ClientMessageReader();
			try {
				cmr.readContent((DatagramPacket)arg1);
				console.debugMessage(Parameters.DEBUG_WARNING, "ClientPositionController.update(): ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if((cmr.getCode() == Parameters.REQUEST_RSSI)&&((DatagramPacket)arg1).getAddress().equals(relayAddress)){
					RSSIvalue = cwnic.getSignalStrenghtValue();
					notifyRSSI = ClientMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue, relayAddress, Parameters.RELAY_RSSI_RECEIVER_PORT);
					sequenceNumber++;
					crscm.sendTo(notifyRSSI);
					console.debugMessage(Parameters.DEBUG_INFO,"ClientPositionController.update(): Inviato RSSI: "+ RSSIvalue +" a: " + relayAddress+":"+Parameters.RELAY_RSSI_RECEIVER_PORT);
				}else{
					console.debugMessage(Parameters.DEBUG_INFO,"ClientPositionController.update(): Faccio niente");
				
				}
			} catch (WNICException e) {
					console.debugMessage(Parameters.DEBUG_ERROR,"ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
					new WNICException("ClientPositionController.update(): Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
				
			} catch (IOException e) {e.printStackTrace();}
			cmr = null;
		}
	}

	public ClientWNICController getClientWNICController(){
		return this.cwnic;
	}
	
	/**Metodo per impostare l'osservazione dei valori di RSSI nei 
	 * confronti dell'indirizzo del Relay
	 * @param rA una String che rappresenta l'indirizzo del Relay
	 */
	public void setRelayAddress(String rA) {
		try {
			relayAddress = InetAddress.getByName(rA);
			enableToMonitor = true;
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Server per visualizzare i messagi di debug
	 */
	public void setDebugConsole(DebugConsole console) {this.console=console;}
}



