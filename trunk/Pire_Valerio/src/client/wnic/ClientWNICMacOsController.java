package client.wnic;

import parameters.DebugConfiguration;
import client.wnic.exception.WNICException;
import debug.DebugConsole;

/**
 * @author Pire Dejaco
 * @version 1.1
 */

public class ClientWNICMacOsController implements ClientWNICController{

	private boolean isOn = false;
	private boolean isConnected = false;
	private boolean isAssociated = false;
	private String relayAddress = null;
	private String interf = null;
	private String essidName = null;
	private DebugConsole console = null;


	public ClientWNICMacOsController(String interf,String essidName) throws WNICException{
		this.interf = interf;
		this.essidName = essidName;
		
		console.debugMessage(DebugConfiguration.DEBUG_INFO, "ClientWNICMacOsController: Controllo l'esistenza e lo stato dell'interfaccia"+interf);
	}

	/**Metodo per settare il monitoraggio del Relay attuale
	 * @param rA l'indirizzo del Relay da monitorare
	 * @throws WNICException 
	 */
	public void setRelayAddress(String rA) throws WNICException {
		relayAddress = rA;
		console.debugMessage(DebugConfiguration.DEBUG_INFO, "ClientWNICWindowsController.setRelayAddress(): mi metto ad osservare l'indirizzo IP " + relayAddress);
	}

	/**Metodo per resettare il relay a cui è collegato
	 * @param iN interfaccia della scheda wireless
	 * @throws WNICException
	 */
	public void resetAddressToMonitor(String iN) throws WNICException{relayAddress = null;}

	/**Metodo per recuperare l'attuale potenza di segnale
	 * @throws WNICException
	 */
	public int getSignalStrenghtValue() throws WNICException{return 0;}

	/**Metodo per sapere se il nodo è connesso ad una rete Ad-Hoc
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException{return isConnected;}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException {return isOn;}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete Ad-Hoc, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {return isAssociated;}

	/**Metodo per ottenere il nome dell'interfaccia
	 * @return una String che rappresenta il nome dell'interfaccia gestita
	 */	
	public String getInterfaceName() {return interf;}

	/**Metodo per ottenere il nome della rete 
	 * @return una String che rappresenta il nome della rete
	 */
	public String getEssidName() {return essidName;}

	/**Server per visualizzare i messagi di debug
	 */
	public void setDebugConsole(DebugConsole console) {this.console=console;}

	@Override
	public DebugConsole getDebugConsole() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() throws WNICException {
		// TODO Auto-generated method stub
		
	}
}