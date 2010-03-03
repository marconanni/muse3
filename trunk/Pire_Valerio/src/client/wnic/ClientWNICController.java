package client.wnic;

import client.wnic.exception.WNICException;
import debug.DebugConsole;

/**
 * @author Pire Dejaco
 * @version 1.1
 */

public interface ClientWNICController 
{
	/**Metodo per initializzare l'interfaccia
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
	public int getSignalStrenghtValue() throws WNICException;
	
	public void setDebugConsole(DebugConsole console);
	
	public DebugConsole getDebugConsole();

}
