package client.wnic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

import parameters.Parameters;

import client.wnic.exception.InvalidAccessPoint;
import client.wnic.exception.InvalidParameter;
import client.wnic.exception.WNICException;
import debug.DebugConsole;


public class ClientWNICLinuxController implements ClientWNICController{

	private boolean isOn = false;
	private boolean isAssociated =false;
	private boolean modeAdHoc = false;
	private String relayAddress = null;
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;
	private boolean relayFirstDetection = true;
	private DebugConsole console =null;


	public ClientWNICLinuxController(String ethX, String netName) throws WNICException{

		interf = ethX;
		essidName = netName;
		console = new DebugConsole();
		console.setTitle("CLIENT WNIC LINUX CONTROLLER - Debug conole");
		
		console.debugMessage(Parameters.DEBUG_INFO, "L'interfaccia di rete " +  ethX + " esiste, ora ne controllo lo stato");

		refreshStatus();

		if (!isOn)
			throw new WNICException("ClientWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");	

		console.debugMessage(Parameters.DEBUG_INFO, "L'interfaccia "+interf+" è attiva");
		console.debugMessage(Parameters.DEBUG_INFO, ((isAssociated)?"L'interfaccia " +interf+ " è connessa alla rete " +essidName+ " in maniera "+ ((modeAdHoc)?"AdHoc":"Managed"):"L'interfaccia " +interf+ " non è connessa a nessuna rete Ad-Hoc"));
	}
	

	//METODI PRIVATI

	/**
	 * Metodo per rinfrescare lo stato del RelayWNICLinuxController grazie all'essecuzione del comando <code>iwconfig<code>.
	 * @param interfaceInfo rappresenta in pratica il risultato di una chiamata al comando linux <code> /sbin/iwconfig interface<code>
	 * @throws WNICException
	 */
	private void refreshStatus() throws WNICException{

		BufferedReader interfaceInfo = getInterfaceInfo(interf);
		String res = null;
		try{
			if((res = interfaceInfo.readLine())!=null){
				

				//scheda spenta
				if (res.contains("off/any")){
					isAssociated = false;
					isOn = false;
					console.debugMessage(Parameters.DEBUG_WARNING,"L'interfaccia "+ interf +" ESISTE però è SPENTA!");
				}

				//On ma non associata alla rete ad hoc
				else if (res.contains("IEEE")){
					isOn = true;
					isAssociated = true;
					console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +"  ESISTE ed è ACCESA.");
					if(res.contains(essidName)){
						essidFound = true;
						console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è CONNESSA alla rete " + essidName);
					}else {
						essidFound = false;
						console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}	
					
					//controllo il mode della scheda (deve essere Ad-Hoc)
					res = interfaceInfo.readLine();
					if(res.contains("Ad-Hoc")){
						modeAdHoc = true;
						console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è settata a MODE Ad-Hoc");
					}else {
						modeAdHoc = false;
						console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}	
	
				}else{
					console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
					throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
				}
			}else{
				console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
				throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
			}

			interfaceInfo.close();
		}catch (IOException e){e.printStackTrace();}
	}


	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>/sbin/iwconfig<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>/sbin/iwconfig<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private BufferedReader getInterfaceInfo(String iN) throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/sbin/iwconfig " + iN);
			p.waitFor();
			return new BufferedReader(new InputStreamReader(p.getInputStream()));
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}


	/**
	 * Metodo per settare l'indirizzo IP che <code>iwspy<code> deve monitorare
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy interface + IPaddress<code>
	 * @param address l'indirizzo IP di cui si vogliono monitorare gli RSSI tramite il comando <code>iwspy interface + IPaddress<code>
	 * @throws WNICException
	 */
	private void setAddressToMonitor(String iN, String address) throws WNICException{
		try{
			resetAddressToMonitor(iN);
			System.out.println("address: " +address);
			Process p= Runtime.getRuntime().exec("/sbin/iwspy " + iN + " + " + address);
			p.waitFor();
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}

	/**
	 * Metodo per riempire la cache locale dell'ARP con info sull'IP da osservare con il comando linux <code>iwspy interface<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private BufferedReader tryToPing(String address) throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/bin/ping "+address+" -c1" );
			p.waitFor();
			return new BufferedReader(new InputStreamReader(p.getInputStream()));
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}
	
	private int convertRSSI(int dBm){
		if(dBm<-100)
			return 0;
		if(dBm>20)
			return 120;
		return dBm+100;
	}

	/**Metodo per ottenere il valore di RSSI attuale avvertito nei confronti del Relay attuale tramite il comando <code>iwconfig<code>
	 * @param line la linea del risultato alla chiamata al comando linux <code>/sbin/iwconfig interface<code>
	 * @return un int che rappresenta il valore di RSSI attuale (dbm convertito tra 0 e 120)
	 * @throws InvalidAccessPoint
	 */
	private int getActualRSSIValue(String iN) throws InvalidParameter, WNICException{

		BufferedReader br = getInterfaceInfo(iN); 
		System.out.println("Perfetto buffer");

		if(br == null){
			System.out.println("Buffer null");
			throw new InvalidParameter ("ERRORE: non esistono statistiche sull'interfaccia " + iN);
		}

		try{
			String riga = br.readLine();
			System.out.println("Letto riga1:"+riga);
			while(riga!=null){
				System.out.println("Entrato nel while");
				StringTokenizer stringToken = new StringTokenizer(riga," \"\t\n\r\f");
				while(stringToken.hasMoreElements()){
					String token = stringToken.nextToken();
					System.out.println("Token:"+token);
					if(token.equals("Signal")){
						return convertRSSI(Integer.parseInt(stringToken.nextToken().substring(6)));
					}
				}
				riga=br.readLine();
			
			
			}
		}catch (Exception e) {}
		return -1;
	}


	/*private int extractRSSIValue(String line) throws WNICException{
		StringTokenizer st = new StringTokenizer(line,"=");
		int res = -1;
		st.nextToken();
		st.nextToken();
		String token = st.nextToken();
		
		//System.out.println("token: " + token);

		try{
			res = Integer.parseInt((token.substring(1,3).trim()));			
		}catch(NumberFormatException ee){
			throw new WNICException("ERRORE: RSSI non valido");
		}

		return res;
	}*/


	//METODI PUBLICI


	/**Metodo per settare il monitoraggio del Relay attuale
	 * @param rA l'indirizzo del Relay da monitorare
	 * @throws WNICException 
	 */
	public void setRelayAddress(String rA) throws WNICException {

		BufferedReader brping = null;
		try {
			
			//Vedo se non mi è stata passata una stringa che non può essere un indirizzo
			InetAddress.getByName(rA);

			//Vedo se quell'indirizzo esiste in rete: cerco di pingarlo, così preparo la cache arp
			brping = tryToPing(rA);
			String test = null;
			boolean unreach = false;
			do {
				try {
					test = brping.readLine();
					//System.out.println("test: " +test);
				} catch (IOException e) {
					e.printStackTrace();
				}				
				if(test!=null)unreach = test.contains("Destination Host Unreachable");
			}
			while(!unreach && test != null);
			if(unreach) throw new WNICException("ERRORE: non riesco a raggiungere il nodo " + rA);

			//a questo punto so che l'indirizzo corrisponde ad un host ed ho delle statistiche su di lui
			relayAddress = rA;
			setAddressToMonitor(interf, relayAddress);
			System.out.println("ClientWNICLinuxController.setRelayAddress(): mi metto ad osservare l'indirizzo IP " + relayAddress);


		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new WNICException("ClientWNICLinuxController.setRelayAddress() :  l'indirizzo IP " + rA + " non risulta valido");
		}

	}


	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>iwspy interface<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy<code>
	 * @throws WNICException
	 */
	public void resetAddressToMonitor(String iN) throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/sbin/iwspy " + iN + " off");
			p.waitFor();
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}

	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	public int getSignalStrenghtValue() throws WNICException{
		//refreshStatus();
		System.out.println("ECCOMI");
		if(isAssociated){
			System.out.println("OK associato");
			try {
				return getActualRSSIValue(interf);
			} 
			catch (Exception e) {
				e.printStackTrace();
				throw new WNICException("ERRORE: impossibile ottenere il nuovo valore di RSSI");
			}
		}
		else return -1; 
	}



	/**Metodo per sapere se il nodo è connesso ad una rete Ad-Hoc
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	public boolean isConnected() throws WNICException{
		refreshStatus();
		return isAssociated && isOn && essidFound;
	}


	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException {
		refreshStatus();
		return isOn;
	}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete Ad-Hoc, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {
		refreshStatus();
		return isAssociated;
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
		return essidFound;
	}
	
	public void setDebugConsole(DebugConsole console ){
		this.console=console;
	}
}


/*class TestClientWNICLinuxController{

	public static void main(String args[]){

		ClientWNICLinuxController cwlc = null;

		try {
			cwlc = new ClientWNICLinuxController("eth1","PROVARETE");
			cwlc.setRelayAddress("192.168.1.3");
			double RSSIFromRelay = (double)cwlc.getSignalStrenghtValue();
			System.out.println(	"Rilevazione valore attuale dell'RSSI ricevuto dal Relay: " +RSSIFromRelay); 
			for(int i=0;i<12;i++){
				if(cwlc.isAssociated())	{
					RSSIFromRelay =(double)cwlc.getSignalStrenghtValue();
					System.out.println(	"Rilevazione #"+i +" : valore attuale dell'RSSI ricevuto dal Relay: " +RSSIFromRelay); 
					Thread.sleep(2500);
				}
			} 	
			cwlc.resetAddressToMonitor("eth1");


		} catch (WNICException e) {

			e.printStackTrace();
			System.exit(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally {
			try {
				cwlc.resetAddressToMonitor("eth1");
			} catch (WNICException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void printArray(double array[]){
		System.out.print("RSSI: [");
		for(int k = 0; k<array.length; k++)
		{
			System.out.print(array[k]+", ");
		}
		System.out.print("]\n");
	}
}*/