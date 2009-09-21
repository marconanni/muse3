/**
 * 
 */
package client.wnic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import parameters.Parameters;
import relay.wnic.RelayDummyController;

import client.wnic.exception.InvalidAccessPoint;
import client.wnic.exception.InvalidParameter;
import client.wnic.exception.WNICException;

/**
 * @author Luca Campeti
 *
 */
public class ClientDummyController implements ClientWNICController{

	private boolean isOn = false;
	private String relayAddress = null;
	private String interf = null;
	private String essidName = null;
	private BufferedReader fileBufferedreader = null;


	public ClientDummyController() throws WNICException{

		fileBufferedreader = getInterfaceInfo();
		StringTokenizer st = null;
		
		try {
		
			st = new StringTokenizer(fileBufferedreader.readLine(),"\n");
			essidName = st.nextToken();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>/sbin/iwconfig<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>/sbin/iwconfig<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private BufferedReader getInterfaceInfo() throws WNICException{
		try{
		//	return new BufferedReader(new FileReader("./src/client/wnic/rssi.txt"));
			return new BufferedReader(new FileReader("./rssi.txt"));
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}



	/**Metodo per ottenere il valore di RSSI attuale avvertito nei confronti del Relay attuale tramite il comando <code>iwspy<code>
	 * @param line la linea del risultato alla chiamata al comando linux <code>/sbin/iwspy interface<code>
	 * @return un int che rappresenta il valore di RSSI attuale
	 * @throws InvalidAccessPoint
	 */
	private int extracNewRSSIValue() throws InvalidParameter, WNICException{

		String line = null;

		try {
			line = fileBufferedreader.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if(line!=null){
			try{
				return Integer.parseInt(line);			
			}catch (Exception e){
				throw new WNICException("ERRORE: impossibile ottenere il nuovo valore di RSSI per l'Access Point a cui e' connessa la scheda wireless");
			}
		}
		else {
			throw new WNICException("FILE TERMINATO");
		}
	}


	//METODI PUBLICI


	/**Metodo per settare il monitoraggio del Relay attuale
	 * @param rA l'indirizzo del Relay da monitorare
	 * @throws WNICException 
	 */
	public void setRelayAddress(String rA) throws WNICException {

		relayAddress = rA;

		System.out.println("ClientDummyController.setRelayAddress(): mi metto ad osservare l'indirizzo IP " + relayAddress);

	}


	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>iwspy interface<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy<code>
	 * @throws WNICException
	 */
	public void resetAddressToMonitor(String iN) throws WNICException{
		relayAddress = null;
	}

	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	public int getSignalStrenghtValue() throws WNICException{

		int res = -1;
		try {
			res = extracNewRSSIValue();
		} catch (InvalidParameter e) {
			e.printStackTrace();
		}
		return res;
	}



	/**Metodo per sapere se il nodo è connesso ad una rete Ad-Hoc
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException{
		return true;
	}


	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException {
		return true;
	}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete Ad-Hoc, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {
		return true;
	}

	/**Metodo per ottenere il nome dell'interfaccia
	 * @return una String che rappresenta il nome dell'interfaccia gestita
	 */	
	public String getInterfaceName() {
		return interf;
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
		return true;
	}
}


class TestClientDummyController {
	
	public static void main(String args[]){

		ClientDummyController rdc = null;

		//BufferedReader br = null;
		
		try {
			rdc = new ClientDummyController();
		} catch (WNICException e) {
			e.printStackTrace();
		}

	/*	try {
			br = rdc.whereIam();

			System.out.println("-> "+ br.readLine());
			
			br.close();

		} catch (WNICException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

		try {
			while(true){
				System.out.println("RSSI: " + rdc.getSignalStrenghtValue());

				Thread.sleep(Parameters.POSITION_CLIENTS_MONITOR_PERIOD);
			}
		} catch (WNICException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}