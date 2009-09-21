package client.wnic;

import client.wnic.exception.WNICException;

public interface ClientWNICController 
{

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

	
	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	public int getSignalStrenghtValue() throws WNICException;
	
	
	/**Metodo per settare il monitoraggio del Relay attuale
	 * @param rA l'indirizzo del Relay da monitorare
	 */
	public void setRelayAddress(String rA) throws WNICException;

	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>iwspy interface<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy<code>
	 * @throws WNICException
	 */
	public void resetAddressToMonitor(String iN) throws WNICException;
	

}
