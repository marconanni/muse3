package relay.wnic;

import relay.wnic.AccessPointData;
import relay.wnic.exception.InvalidAccessPoint;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import parameters.Parameters;


/**
 * @author Luca Campeti
 *
 */
public class RelayDummyController implements RelayWNICController{

	private AccessPointData currentAP = null;
	private int numberOfPreviousRSSI = -1;
	private String essidName = null;
	private BufferedReader fileBufferedreader = null;
	private boolean apVisibility = true;


	public RelayDummyController(int previous) throws WNICException{		

		numberOfPreviousRSSI=previous;

		if(previous<=0)
			throw new WNICException("RelayWNICLinuxController: ERRORE: numero di precedenti RSSI da memorizzare non positivo");		

		fileBufferedreader = getInterfaceInfo();

		try {
			currentAP = createCurrentAccessPointData(fileBufferedreader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	//METODI PRIVATI


	/**
	 * Metodo per creare un AccessPointData grazie ai dati provenienti dal risultato del comando <code>/sbin/iwconfig<code>
	 * @param line la prima linea del risultato della chiamata al comando linux <code>/sbin/iwconfig interface<code>
	 * @return un AccessPointData da assegnare al riferimento locale
	 */
	private AccessPointData createCurrentAccessPointData(String line){

		StringTokenizer st = new StringTokenizer(line," ");
		AccessPointData res = null;

		essidName = st.nextToken();

		try {
			res = new AccessPointData(essidName, st.nextToken(),Double.parseDouble(st.nextToken()),numberOfPreviousRSSI);
		} catch (InvalidAccessPoint e) {
			e.printStackTrace();
		} catch (InvalidParameter e) {
			e.printStackTrace();
		}

		return res;
	}


	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>/sbin/iwconfig<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private BufferedReader getInterfaceInfo() throws WNICException{
		try{
			//return new BufferedReader(new FileReader("./src/relay/wnic/rssiAP.txt"));
			return new BufferedReader(new FileReader("./rssiAP.txt"));
		}catch (Exception e){
			e.printStackTrace();
			throw new WNICException("ERRORE: impossibile ottenere informazioni dal file di test");
		}	
	}


	/*public BufferedReader whereIam() throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/bin/pwd");
			p.waitFor();
			return new BufferedReader(new InputStreamReader(p.getInputStream()));

		}catch (Exception e){
			e.printStackTrace();
			throw new WNICException("ERRORE: impossibile ottenere informazioni dal file di test");
		}	
	}*/


	/**Metodo per ottenere SOLO il nuovo valore di RSSI dal risultato di una chiamata al comando </sbin/iwconfig interface> 
	 * @return un double che rappresenta il valore di RSSI attuale
	 * @throws WNICException
	 */
	private int extractNewRSSIValue() throws WNICException{

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


	//METODI PUBBLICI

	/**Metodo per ottenere l'ultimo valore di RSSI collezionato all'interno dell'<code>AccessPointData</code> relativo
	 * all'AP a cui il nodo è attualmente connesso.
	 * @return un intero che rappresenta il valore RSSI appena rilevato e collezionato nell'<code>AccessPointData</code>,
	 * @throws WNICException
	 */
	public int updateSignalStrenghtValue() throws WNICException{

		try {
			currentAP.newSignalStrenghtValue(extractNewRSSIValue());
		} 
		catch (Exception e) {
			throw new WNICException(e.getMessage());
		}
		return currentAP.getSignalStrenght();
	}

	/**Metodo per ottenere l'ultimo valore di RSSI collezionato all'interno dell'<code>AccessPointData</code> relativo
	 * all'AP a cui il nodo è attualmente connesso.
	 * @return un intero che rappresenta l'ultimo valore RSSI collezionato nell'<code>AccessPointData</code>, -1 in caso 
	 * non ci sia connessione con un AP
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public int getSignalLevel() throws WNICException {
		if(currentAP != null)return currentAP.getSignalStrenght();
		else return -1;
	}


	/**
	 * Metodo per ottenere le informazioni relative all'AP a cui l'interfaccia locale è connessa
	 * @return un AccessPointData che rappresenta l'AP a cui il nodo è attualmente connesso.
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public AccessPointData getAssociatedAccessPoint() throws WNICException{
		return currentAP;
	}

	/**Metodo per sapere se il nodo è connesso ad un AP
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException{
		return apVisibility;
	}

	/**Metodo per ottenere il nome della rete 
	 * @return una String che rappresenta il nome della rete
	 */
	public String getEssidName() {
		return essidName;
	}


	/* (non-Javadoc)
	 * @see relay.wnic.RelayWNICController#isAssociated()
	 */
	@Override
	public boolean isAssociated() throws WNICException {
		return true;
	}


	/* (non-Javadoc)
	 * @see relay.wnic.RelayWNICController#isOn()
	 */
	@Override
	public boolean isOn() throws WNICException {
		return true;
	}


	public boolean isApVisibility() {
		return apVisibility;
	}


	public void setApVisibility(boolean apVisibility) {
		this.apVisibility = apVisibility;
	}

}


/*class TestRelayDummyController{

	public static void main(String args[]){

		RelayDummyController rdc = null;

		//BufferedReader br = null;
		
		try {
			rdc = new RelayDummyController(15);
		} catch (WNICException e) {
			e.printStackTrace();
		}

		try {
			br = rdc.whereIam();

			System.out.println("-> "+ br.readLine());
			
			br.close();

		} catch (WNICException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			while(true){
				System.out.println("RSSI: " + rdc.updateSignalStrenghtValue());

				Thread.sleep(Parameters.POSITION_AP_MONITOR_PERIOD);
			}
		} catch (WNICException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
*/