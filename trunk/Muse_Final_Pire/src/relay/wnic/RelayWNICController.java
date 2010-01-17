package relay.wnic;

import debug.DebugConsole;
import relay.wnic.exception.WNICException;

public interface RelayWNICController 
{
	/**Metodo per initializzare l'interfaccia
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public void init() throws WNICException;
	
	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad un AP, false altrimenti
	 */
	public boolean isAssociated()throws WNICException;
		
	/**Metodo per sapere se il nodo è connesso ad un AP
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException;
		
	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException;
	
	/**
	 * Restituisce l'ultimo RSSI registrato per l'access point corrente <b>senza prima aggiornarlo</b>
	 * @return l'ultimo RSSI registrato per l'access point corrente o 0 se non e' connessa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 * @throws WNICException 
	 */
	public int getSignalStrenghtValue()throws WNICException, WNICException;
	
	/**
	 * Restituisce l'access point a cui la scheda e' connessa
	 * @return l'access point a cui la scheda e' connessa o null se non e' connessa
	 * @throws WNICException se non si riesce a ottenere lo stato della scheda
	 */
	public AccessPointData getAssociatedAccessPoint() throws WNICException;
	
	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	public int updateSignalStrenghtValue() throws WNICException;
	
	public void setDebugConsole(DebugConsole console);
	
	public DebugConsole getDebugConsole();
}
