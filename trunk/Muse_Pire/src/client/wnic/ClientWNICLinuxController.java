package client.wnic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;
	private DebugConsole console =null;


	public ClientWNICLinuxController(String ethX, String netName) throws WNICException{

		interf = ethX;
		essidName = netName;
		setDebugConsole(new DebugConsole());
		console.setTitle("CLIENT WNIC LINUX CONTROLLER - Interface: ["+interf+"] ESSID: ["+essidName+"]");
		
		refreshStatus(true);

		if (!isOn){
			this.console.debugMessage(Parameters.DEBUG_ERROR, "La scheda wireless deve essere accesa e l'interfaccia "+interf+" deve essere configurata nel seguente modo:\nESSID:"+Parameters.NAME_OF_AD_HOC_NETWORK+"\nMODE:Ad-Hoc\nIP:"+Parameters.CLIENT_ADDRESS);
			throw new WNICException("ClientWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");	
		}
	}
	

	//METODI PRIVATI

	/**
	 * Metodo per rinfrescare lo stato del RelayWNICLinuxController grazie all'essecuzione del comando <code>iwconfig<code>.
	 * @param interfaceInfo rappresenta in pratica il risultato di una chiamata al comando linux <code> /sbin/iwconfig interface<code>
	 * @throws WNICException
	 */
	private void refreshStatus(boolean debug) throws WNICException{

		BufferedReader interfaceInfo = getInterfaceInfo(interf);
		String res = null;
		try{
			if((res = interfaceInfo.readLine())!=null){
				//scheda spenta
				if (res.contains("off/any")){
					isAssociated = false;
					isOn = false;
					if(debug)console.debugMessage(Parameters.DEBUG_WARNING,"L'interfaccia "+ interf +" ESISTE però è SPENTA!");
				}

				//On ma non associata alla rete ad hoc
				else if (res.contains("IEEE")){
					isOn = true;
					isAssociated = true;
					if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +"  ESISTE ed è ACCESA.");
					if(res.contains(essidName)){
						essidFound = true;
						if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è CONNESSA alla rete " + essidName);
					}else {
						essidFound = false;
						console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}	
					
					//controllo il mode della scheda (deve essere Ad-Hoc)
					res = interfaceInfo.readLine();
					if(res.contains("Ad-Hoc")){
						modeAdHoc = true;
						if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è settata a MODE Ad-Hoc");
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
			console.debugMessage(Parameters.DEBUG_ERROR,"Impossibile ottenere informazioni dalla scheda wireless");
			throw new WNICException("ERRORE: Impossibile ottenere informazioni dalla scheda wireless");
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

		try{
			String riga = br.readLine();
			while(riga!=null){
				StringTokenizer stringToken = new StringTokenizer(riga," \"\t\n\r\f");
				while(stringToken.hasMoreElements()){
					String token = stringToken.nextToken();
					if(token.equals("Signal")){
						return convertRSSI(Integer.parseInt(stringToken.nextToken().substring(6)));
					}
				}
				riga=br.readLine();
			}
		}catch (Exception e) {
			console.debugMessage(Parameters.DEBUG_ERROR,"Impossibile ottenere il valore RSSI attuale");
			throw new WNICException("ClientWNICLinuxController: Impossibile ottenere il valore RSSI attuale");
		}
		return -1;
	}

	//METODI PUBLICI
	
	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	public int getSignalStrenghtValue() throws WNICException{
		//refreshStatus();
		if(isConnected()){
			try {
				return getActualRSSIValue(interf);
			} 
			catch (Exception e) {
				console.debugMessage(Parameters.DEBUG_ERROR, "Impossibile ottenere il nuovo valore di RSSI");
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
		refreshStatus(false);
		return isAssociated && isOn && essidFound;
	}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	public boolean isOn() throws WNICException {
		refreshStatus(false);
		return isOn;
	}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {
		refreshStatus(false);
		return isAssociated;
	}
	
	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete ad-hoc, false altrimenti
	 */
	public boolean ismodeAdHoc() throws WNICException {
		refreshStatus(false);
		return modeAdHoc;
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

	@Override
	public void setDebugConsole(DebugConsole console) {
		this.console=console;
	}
	
	@Override
	public DebugConsole getDebugConsole() {
		return this.console;
	}
}




/**Metodo per settare il monitoraggio del Relay attuale
 * @param rA l'indirizzo del Relay da monitorare
 * @throws WNICException 
 */
//public void setRelayAddress(String rA) throws WNICException {
//
//	BufferedReader brping = null;
//	try {
//		
//		//Vedo se non mi è stata passata una stringa che non può essere un indirizzo
//		InetAddress.getByName(rA);
//
//		//Vedo se quell'indirizzo esiste in rete: cerco di pingarlo, così preparo la cache arp
//		brping = tryToPing(rA);
//		String test = null;
//		boolean unreach = false;
//		do {
//			try {
//				test = brping.readLine();
//			} catch (IOException e) {e.printStackTrace();}				
//			if(test!=null)unreach = test.contains("Destination Host Unreachable");
//		}
//		while(!unreach && test != null);
//		if(unreach){
//			console.debugMessage(Parameters.DEBUG_ERROR,"Non riesco a raggiungere il nodo "+rA);
//			throw new WNICException("ClientWNICLinuxController: non riesco a raggiungere il nodo " + rA);
//		}
//
//		//a questo punto so che l'indirizzo corrisponde ad un host ed ho delle statistiche su di lui
//		relayAddress = rA;
//		setAddressToMonitor(relayAddress);
//		console.debugMessage(Parameters.DEBUG_INFO,"Mi metto ad osservare l'indirizzo IP "+relayAddress);
//
//	} catch (UnknownHostException e) {
//		console.debugMessage(Parameters.DEBUG_ERROR, "L'indirizzo IP " + rA + " non risulta valido");
//		throw new WNICException("ClientWNICLinuxController.setRelayAddress():  L'indirizzo IP " + rA + " non risulta valido");
//	}
//
//}


/**
 * Metodo per settare l'indirizzo IP che <code>iwspy<code> deve monitorare
 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy interface + IPaddress<code>
 * @param address l'indirizzo IP di cui si vogliono monitorare gli RSSI tramite il comando <code>iwspy interface + IPaddress<code>
 * @throws WNICException
 */
//private void setAddressToMonitor(String address) throws WNICException{
//	this.relayAddress=address;
//}

/**
 * Metodo per riempire la cache locale dell'ARP con info sull'IP da osservare con il comando linux <code>iwspy interface<code>
 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
 * @throws WNICException
 */
//private BufferedReader tryToPing(String address) throws WNICException{
//	try{
//		Process p= Runtime.getRuntime().exec("/bin/ping "+address+" -c1" );
//		p.waitFor();
//		return new BufferedReader(new InputStreamReader(p.getInputStream()));
//	}catch (Exception e){
//		console.debugMessage(Parameters.DEBUG_ERROR, "Impossibile eseguire il ping al seguente indirizzo: "+address);
//		throw new WNICException("ClientWNICLinuxController: Impossibile eseguire il ping al seguente indirizzo: "+address);
//	}	
//}
/**
 * Metodo per ottenere il risultato della chiamata al comando linux <code>iwspy interface<code>
 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>iwspy<code>
 * @throws WNICException
 */
//public void resetAddressToMonitor(String iN) throws WNICException{
//	this.relayAddress=null;
//}

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