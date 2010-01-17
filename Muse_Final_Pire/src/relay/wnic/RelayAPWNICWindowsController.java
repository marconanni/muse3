package relay.wnic;

import parameters.ElectionConfiguration;
import relay.wnic.exception.InvalidAccessPoint;
import relay.wnic.exception.WNICException;
import debug.DebugConsole;

public class RelayAPWNICWindowsController implements RelayWNICController {
	private AccessPointData currentAP;
	private boolean isOn = false;
	private boolean isAssociated = false;
	private int numberOfPreviousRSSI = -1;
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;
	private boolean modeManaged = false;
	private DebugConsole console = null;


	public RelayAPWNICWindowsController(String ethX)throws WNICException {
		interf = ethX;
		setNumberOfPreviousRSSI(ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
		console = new DebugConsole();
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ ethX);
	}

	public RelayAPWNICWindowsController(String ethX,String netName) throws WNICException {
		interf = ethX;
		essidName = netName;
		setNumberOfPreviousRSSI(ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
		console = new DebugConsole();
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ ethX);
	}

	/**
	 * Metodo per ottenere l'ultimo valore di RSSI collezionato all'interno
	 * dell'<code>AccessPointData</code> relativo all'AP a cui il nodo è
	 * attualmente connesso.
	 * 
	 * @return un intero che rappresenta l'ultimo valore RSSI collezionato nell'
	 *         <code>AccessPointData</code>, -1 in caso non ci sia connessione
	 *         con un AP
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public int getSignalStrenghtValue() throws WNICException {
		if (currentAP != null)
			return currentAP.getSignalStrenght();
		else
			return -1;
	}

	/**
	 * Metodo per ottenere le informazioni relative all'AP a cui l'interfaccia
	 * locale è connessa
	 * 
	 * @return un AccessPointData che rappresenta l'AP a cui il nodo è
	 *         attualmente connesso.
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public AccessPointData getAssociatedAccessPoint() throws WNICException {
		return currentAP;
	}

	public void setNewCurrentAP(AccessPointData ap) {
		this.currentAP = ap;
	}
	
	public void init(){}

	/**
	 * Metodo per sapere se il nodo è connesso ad un AP
	 * 
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException {
		return isAssociated && isOn && essidFound;
	}

	/**
	 * Metodo per capire se l'interfaccia è accesa o meno
	 * 
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException {
		return isOn;
	}

	/**
	 * Metodo per capire se l'interfaccia è accesa o meno
	 * 
	 * @return true se l'interfaccia è associata ad un AP, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {
		return isAssociated;
	}

	/**
	 * Metodo per ottenere il nome dell'interfaccia
	 * 
	 * @return una String che rappresenta il nome dell'interfaccia gestita
	 */
	public String getInterfaceName() {
		return interf;
	}

	public boolean isModeManaged() throws WNICException {
		return modeManaged;
	}

	public DebugConsole getDebugConsole() {
		return console;
	}

	public void setDebugConsole(DebugConsole console) {
		this.console=console;
	}

	public int updateSignalStrenghtValue() throws WNICException {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**Metodo per ottenere il nome della rete 
	 * @return una String che rappresenta il nome della rete
	 */
	public String getEssidName() {
		return essidName;
	}


	/**Metodo per sapere se la rete passata al costruttore è stata rilevata
	 * @return true se la rete è stata rilevata, false altrimenti
	 */
	public boolean isEssidFound() {
		return essidFound;
	}

	public void setNumberOfPreviousRSSI(int numberOfPreviousRSSI) {
		this.numberOfPreviousRSSI = numberOfPreviousRSSI;
	}

	public int getNumberOfPreviousRSSI() {
		return numberOfPreviousRSSI;
	}

}