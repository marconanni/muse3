package client.position;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.PortConfiguration;


import client.messages.ClientMessageFactory;
import client.messages.ClientMessageReader;
import client.wnic.exception.WNICException;



import client.connection.ClientCM;
import client.connection.ClientConnectionFactory;
import client.wnic.ClientWNICController;
import debug.DebugConsole;


/**Classe che rappresenta un oggetto che è in grado di rispondere alle richieste del Relay 
 * che sta servendo questo nodo Client in merito alla potenza di segnale rilevata dal Client 
 * stesso nei confronti del nodo Relay
 * @author Luca Campeti	
 */
public class ClientPositionController implements Observer{
	
	private boolean debug = false;

	private DatagramPacket notifyRSSI = null;
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
	public ClientPositionController(ClientWNICController cwnic) throws WNICException{
		this.crscm = ClientConnectionFactory.getRSSIConnectionManager(this,true);
		this.cwnic=cwnic;
		setDebugConsole(cwnic.getDebugConsole());
		started = false;
		if(debug)debug(getDebugConsole(),DebugConfiguration.DEBUG_INFO, "Creato ClientPositionController.");
	}

	/**Metodo per far partire il ClientPositionController
	 * che, da questa chiamata in poi, può rispondere alle
	 * richieste provenienti dal nodo Relay
	 */
	public void start(){
		if(!started){
			crscm.start();
			started = true;
		}
	}
	
	public void stopReceiving(){
		crscm.stopReceiving();
		started = false;
	}
	
	public void resumeReceiving(){
		crscm.resumeReceiving();
		started = true;
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
				if(debug)debug(getDebugConsole(),DebugConfiguration.DEBUG_INFO, "ClientPositionController: ricevuto nuovo DatagramPacket da " + ((DatagramPacket)arg1).getAddress()+":"+((DatagramPacket)arg1).getPort());
				if(cmr.getCode() == MessageCodeConfiguration.REQUEST_RSSI){
					RSSIvalue = 40;//cwnic.getSignalStrenghtValue();
					notifyRSSI = ClientMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue, cmr.getPacketAddress(), PortConfiguration.RSSI_PORT_IN, MessageCodeConfiguration.TYPECLIENT,1);
					sequenceNumber++;
					crscm.sendTo(notifyRSSI);
					if(debug)debug(getDebugConsole(),DebugConfiguration.DEBUG_INFO,"ClientPositionController: Inviato RSSI: "+ RSSIvalue +" a: " + relayAddress+":"+PortConfiguration.RSSI_PORT_IN);
				}else{
					if(debug)debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"ClientPositionController: Faccio niente");
				}
			} catch (Exception e) {
				debug(getDebugConsole(), DebugConfiguration.DEBUG_ERROR,"ClientPositionController: Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
			}// catch (IOException e) {e.printStackTrace();}
			cmr = null;
		}
	}

	public ClientWNICController getClientWNICController(){
		return this.cwnic;
	}
	
	/**Server per visualizzare i messagi di debug
	 */
	public void setDebug(boolean db){this.debug = db;}
	public void setDebugConsole(DebugConsole console) {this.console=console;}
	private DebugConsole getDebugConsole(){return console;}
	
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



