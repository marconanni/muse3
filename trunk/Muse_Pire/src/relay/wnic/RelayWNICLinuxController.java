package relay.wnic;

import relay.wnic.exception.*;
import relay.wnic.AccessPointData;
import relay.wnic.exception.InvalidAccessPoint;
import relay.wnic.exception.WNICException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import debug.DebugConsole;

import parameters.Parameters;


public class RelayWNICLinuxController implements RelayWNICController{

	private AccessPointData currentAP;
	private boolean isOn = false;
	private boolean isAssociated = false;
	private int numberOfPreviousRSSI = -1;
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;
	private boolean modeManaged = false;
	private DebugConsole console = null;
	
	private boolean debug = false;

	public RelayWNICLinuxController(int previous, String ethX) throws WNICException{
		interf = ethX;
		numberOfPreviousRSSI=previous;
		console = new DebugConsole();
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ethX);
		isWIFIOn();
		

	}


	public RelayWNICLinuxController(int previous, String ethX, String netName) throws WNICException{		

		interf = ethX;
		essidName = netName;
		numberOfPreviousRSSI=previous;
		console = new DebugConsole();
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ethX);
		
		refreshStatus();
		
		if(previous<=0)
			throw new WNICException("RelayWNICLinuxController: ERRORE: numero di precedenti RSSI da memorizzare non positivo");		

		if (!isOn){
			this.console.debugMessage(Parameters.DEBUG_ERROR,"La scheda wireless deve essere accesa e l'interfaccia "+interf+" deve essere configurata nel modo seguente:\nESSID: "+Parameters.NAME_OF_MANAGED_NETWORK+"\nMODE: Managed\nIp:"+Parameters.RELAY_MANAGED_ADDRESS);
			throw new WNICException("RelayWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");
		}
	}


	//METODI PRIVATI

	/**
	 * Metodo per rinfrescare lo stato del RelayWNICLinuxController grazie all'essecuzione del comando <code>iwconfig<code>.
	 * @param interfaceInfo rappresenta in pratica il risultato di una chiamata al comando linux <code> /sbin/iwconfig interface<code>
	 * @throws WNICException
	 */
	private void refreshStatus() throws WNICException{

		BufferedReader interfaceInfo = getInterfaceInfo(interf);
		String res, res1 = null;
		try{
			if((res = interfaceInfo.readLine())!=null){
				res1 = interfaceInfo.readLine();
				//scheda spenta
				if (res.contains("off/any")){
					isAssociated = false;
					isOn = false;
					currentAP = null;
					essidFound = false;
					if(debug)console.debugMessage(Parameters.DEBUG_WARNING,"L'interfaccia "+ interf +" ESISTE però è SPENTA!");
				}

				//On ma non associata alla rete managed
				else if (res.contains("IEEE")){
					isOn = true;
					isAssociated = true;
					if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +"  ESISTE ed è ACCESA.");
					if(res.contains(essidName)){
						essidFound = true;
						if(currentAP==null)currentAP = createCurrentAccessPointData(res,res1,interfaceInfo);
						if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è CONNESSA alla rete " + essidName);
					}
					else {
						essidFound = false; 
						console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia " +interf + " non è connessa alla rete " + essidName);
					}
				
				
					//controllo il mode della scheda (deve essere Ad-Hoc)
					//res = interfaceInfo.readLine();
					if(res1.contains("Managed")){
						modeManaged = true;
						if(debug)console.debugMessage(Parameters.DEBUG_INFO,"L'interfaccia "+ interf +" è settata a MODE Managed");
					}else {
						modeManaged = false;
						console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}	
				}else{
					console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
					throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
				}
			}else{
				console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
				throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
			}
			interfaceInfo.close();
		}catch (IOException e){e.printStackTrace();}
	}
	
	/**
	 * Metodo per rinfrescare lo stato del RelayWNICLinuxController grazie all'essecuzione del comando <code>iwconfig<code>.
	 * @param interfaceInfo rappresenta in pratica il risultato di una chiamata al comando linux <code> /sbin/iwconfig interface<code>
	 * @throws WNICException
	 */
	private void isWIFIOn() throws WNICException{

		BufferedReader interfaceInfo = getInterfaceInfo(interf);
		String res, res1 = null;
		try{
			if((res = interfaceInfo.readLine())!=null){
				res1 = interfaceInfo.readLine();
				//scheda spenta
				if (res.contains("off/any")){
					isOn = false;
					//On ma non associata alla rete managed
				}else if (res.contains("IEEE")){
					isOn = true;
				}
				
				String essid = extractEssidName(res);
				if(essid.hashCode()!=1024){
					essidFound = true;
					essidName = essid;
					isAssociated = true;
					if(currentAP==null)currentAP = createCurrentAccessPointData(res,res1,interfaceInfo);
					
				}else if(res1.contains("Not-Associated")){
					essidFound = false;
					currentAP = null;
					isAssociated = false;
				}

			}else{
				console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
				throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
			}
			interfaceInfo.close();
		}catch (IOException e){e.printStackTrace();}
	}


	/**
	 * Metodo per creare un AccessPointData grazie ai dati provenienti dal risultato del comando <code>/sbin/iwconfig<code>
	 * @param firstLine la prima linea del risultato della chiamata al comando linux <code>/sbin/iwconfig interface<code>
	 * @param rest
	 * @return un AccessPointData da assegnare al riferimento locale
	 */
	private AccessPointData createCurrentAccessPointData(String firstLine,String secondLine, BufferedReader rest){

		String essidName = null;
		String mac = null;
		String mode = null;
		double firstRSSI = -1;
		String line = null;

		try {
			essidName = extractEssidName(firstLine);
			mode = extractMode(secondLine);
			mac = extractMacAddress(secondLine,20);
			do line=rest.readLine();
			while(line!=null && !line.contains("Signal"));
			firstRSSI = (double)extractRSSIValue(line);
			rest.close();
		} catch (Exception e) {e.printStackTrace();return null;}
		try {
			return new AccessPointData(essidName,mac,mode,firstRSSI,numberOfPreviousRSSI);
		} catch (InvalidAccessPoint e) {e.printStackTrace();return null;} 
		catch (InvalidParameter e) {e.printStackTrace();return null;}
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
			throw new WNICException("RelayWNICLinuxController: Impossibile ottenere informazioni dalla scheda wireless");
		}	
	}

	/**
	 * Metodo per ottenere il risultato della chiamata al comando linux <code>/sbin/iwlist interface scan<code>
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>/sbin/iwlist interface scan<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private BufferedReader getVisibleAccessPointInfo(String iN) throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/sbin/iwlist " + iN + " scan");
			p.waitFor();
			return new BufferedReader(new InputStreamReader(p.getInputStream()));
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}

	/**
	 * ATTUALMENTE NON FUNZIONA CORRETTAMENTE Metodo per eseguire i comandi linux necessari per connettersi ad un certo AP (alla <i>essid</i> da esso esposta)
	 * @param iN il nome dell'interfaccia su cui si vuole chiamare il comando <code>/sbin/iwlist interface scan<code>
	 * @return un BufferedReader che rappresenta il risultato della chiamata al Sistema Operativo 
	 * @throws WNICException
	 */
	private void connectTo(String iN, String essid, String mac, String myIP) throws WNICException{ 
		try{
			Process p= null;
			p = Runtime.getRuntime().exec("/sbin/ifconfig " + iN + " down");
			p.waitFor();
			p= Runtime.getRuntime().exec("/sbin/iwconfig " + iN + " mode managed channel 1 essid " + essid + " ap " + mac);
			p.waitFor();
			p= Runtime.getRuntime().exec("/sbin/ifconfig " + iN + " " + myIP + " "+ mac +" netmask 255.255.255.0 up");
			p.waitFor();

		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
		}	
	}


	/**
	 * Metodo per ottenere il nome della rete appartenente all'AP a cui il nodo è connesso
	 * @param line la linea del risultato del comando <code>/sbin/iwconfig<code> che contiene il valore "ESSID"
	 * @return il nome della rete managed che identifica l'AP
	 */
	private String extractEssidName(String line){
		String res = null;
		StringTokenizer st = new StringTokenizer(line, ":\"");
		st.nextToken();
		res= st.nextToken();
		return res;
	}
	
	private String extractMode(String line){
		String res = null;
		StringTokenizer st = new StringTokenizer(line, ":");
		st.nextToken();
		res= st.nextToken();
		return res;
	}


	/**
	 * Metodo per ottenere l'indirizzo MAC dell'AP attuale che l'interfaccia avverte nei confronti dell'AP
	 * @param mac la riga del risultato alla chiamata del comando linux <code>/sbin/iwconfig<code> che contiene l'indirizzo MAC dell'AP
	 * @return una String che rappresenta l'indirizzo MAC dell'AP
	 */
	private String extractMacAddress(String mac, int offsetFromEnd){
		return mac.substring(mac.length()-offsetFromEnd, mac.length());
	}


	/**Metodo per ottenere il valore di RSSI attuale che l'interfaccia avverte nei confronti dell'AP
	 * @param line la linea del risultato alla chiamata al comando linux <code>/sbin/iwconfig<code>
	 * @return un double che rappresenta il valore di RSSI attuale
	 * @throws InvalidAccessPoint
	 */
	private int extractRSSIValue(String line) throws InvalidAccessPoint{
		StringTokenizer st = new StringTokenizer(line);
		String token = null;
		int res = -1;

		do token = st.nextToken("=-"); 
		while(st.hasMoreTokens() && !token.contains("dBm"));

		try{
			res = Integer.parseInt((token.substring(0,3).trim()));			
		}catch(NumberFormatException ee){
			throw new InvalidAccessPoint("ERRORE: RSSI non valido");
		}

		return res;
	}


	/**Metodo per ottenere SOLO il nuovo valore di RSSI dal risultato di una chiamata al comando </sbin/iwconfig interface> 
	 * @param iN il nome dell'interfaccia da cui rilevare il nuovo valore di RSSI rispetto al <code>currentAP<code>
	 * @return un double che rappresenta il valore di RSSI attuale
	 * @throws WNICException
	 */
	private int extractNewRSSIValue(String iN) throws WNICException{
		try{
			BufferedReader out = getInterfaceInfo(iN);
			String temp = null;
			do{ temp = out.readLine();}
			while(!temp.contains("Signal") && temp != null);
			out.close();
			return extractRSSIValue(temp);			
		}catch (Exception e){
			throw new WNICException("ERRORE: impossibile ottenere il nuovo valore di RSSI per l'Access Point a cui e' connessa la scheda wireless");
		}
	}


	//METODI PUBBLICI

	/**Metodo per ottenere l'ultimo valore di RSSI collezionato all'interno dell'<code>AccessPointData</code> relativo
	 * all'AP a cui il nodo è attualmente connesso.
	 * @return un intero che rappresenta il valore RSSI appena rilevato e collezionato nell'<code>AccessPointData</code>,
	 * -1 in caso non ci sia connessione con un AP
	 * @throws WNICException
	 */
	public int updateSignalStrenghtValue() throws WNICException{
		refreshStatus();
		if(currentAP!=null){
			try {
				currentAP.newSignalStrenghtValue(extractNewRSSIValue(interf));
			} 
			catch (Exception e) {
				throw new WNICException("ERRORE: impossibile ottenere il nuovo valore di RSSI dall'Access Point");
			}
			return currentAP.getSignalStrenght();
		}
		else return -1;
	}

	/**Metodo per ottenere l'ultimo valore di RSSI collezionato all'interno dell'<code>AccessPointData</code> relativo
	 * all'AP a cui il nodo è attualmente connesso.
	 * @return un intero che rappresenta l'ultimo valore RSSI collezionato nell'<code>AccessPointData</code>, -1 in caso 
	 * non ci sia connessione con un AP
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public int getSignalLevel() throws WNICException {
		refreshStatus();
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
		refreshStatus();
		return currentAP;
	}

	/**Metodo per sapere se il nodo è connesso ad un AP
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
	 * @return true se l'interfaccia è associata ad un AP, false altrimenti
	 */
	public boolean isAssociated() throws WNICException {
		isWIFIOn();
		return isAssociated;
	}

	/**Metodo per ottenere il nome dell'interfaccia
	 * @return una String che rappresenta il nome dell'interfaccia gestita
	 */	
	public String getInterfaceName() {
		return interf;
	}



	/**Metodo per ottenere una lista degli AccessPointData visibili dall'interfaccia in esame. Quando il nodo è connesso al
	 * currentAP esso risulta essere l'unico elemento della lista
	 * @return un Vector di AccessPointData rappresentante la lista di cui sopra
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public Vector<AccessPointData> getVisibleAccessPoints()	throws WNICException, InvalidAccessPoint {

		isWIFIOn();

		Vector<AccessPointData> vect = new Vector<AccessPointData>();
		if(isAssociated && isOn && essidFound) {
			vect.add(currentAP); 
			return vect;
		}

		String line = null;
		String mac = null;
		String essidName =null;
		String mode = null;
		double firstRSSI = -1;
		StringTokenizer st = null;
		System.out.println(interf);
		BufferedReader br = getVisibleAccessPointInfo(interf);
		try {
			while((line = br.readLine())!=null){
				if(line.contains("Cell")){
					mac = extractMacAddress(line, 18);
					do{
						line = br.readLine();
					}while(!line.contains("Signal")&&line != null);
					firstRSSI = (double)extractRSSIValue(line);
					do{line = br.readLine();}
					while(line !=null && !line.contains("ESSID"));
					st =new StringTokenizer(line,"\"");
					st.nextToken();
					essidName = st.nextToken();
					do{line = br.readLine();}
					while(line!=null && !line.contains("Mode"));
					st =new StringTokenizer(line,":");
					st.nextToken();
					mode = st.nextToken();
						vect.add(new AccessPointData(essidName,mac,mode,firstRSSI,numberOfPreviousRSSI));
				}
			}
			br.close();
		} catch (IOException e) {e.printStackTrace();} 
		catch (InvalidParameter e) {e.printStackTrace();}

		return (vect.size()==0)?null:vect;
	}

	/**<b>ATTUALMENTE NON FUNZIONA DEL TUTTO CORRETTAMENTE</b> Metodo per connettere il nodo all'AP 
	 * il cui identificativo (il nome della rete da esso retta) è passato per parametro
	 * @param essidName il nome della rete retta dall'AP (identificativo dall'AP)
	 * @return true se la connessione col l'AP specificato ha avuto successo, false altrimenti
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public boolean connectToAccessPoint(String essidName) throws WNICException, InvalidAccessPoint{

		refreshStatus();

		//Il metodo connectToAccessPoint deve essere richiamato quando non il WNICController non ha associato un AP
		//Questo è solo un controllo supplementare
		if(!isOn) return false;
		if(isAssociated && (currentAP.getAccessPointName().equalsIgnoreCase(essidName)))return true;

		//A questo punto si arriva con il WNICController con isOn = true e isAssociated =false
		String essidTemp = null;
		boolean find = false;
		int findIndex = -1;

		Vector<AccessPointData> visibleAPs = getVisibleAccessPoints();

		for(int i=0;i<visibleAPs.size();i++){
			System.out.println("AP: " + visibleAPs.get(i).getAccessPointName());
			if(visibleAPs.get(i).getAccessPointName().equalsIgnoreCase(essidName)){
				find = true;
				findIndex =i;
				break;
			}
		}

		if(find){
			AccessPointData apToConnect = visibleAPs.get(findIndex);
			/*System.out.println("AP A CUI CONNETTERSI");
			System.out.println(apToConnect.getAccessPointName());
			System.out.println(apToConnect.getAccessPointMAC());*/
			connectTo(interf, apToConnect.getAccessPointName(), apToConnect.getAccessPointMAC(), Parameters.RELAY_MANAGED_ADDRESS);
			refreshStatus();
			if(isConnected())return true;
			else return false;
		}
		else return false;
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
}



class TestRelayWNICLinuxController{

	public static void main(String args[]){

		RelayWNICLinuxController rwlc = null;
		AccessPointData ap = null;

		try {

			rwlc = new RelayWNICLinuxController(6,"wlan0","ALMAWIFI");

			if(rwlc.isAssociated()&&rwlc.isEssidFound())	{

				ap = rwlc.getAssociatedAccessPoint();

				printArray(ap.getLastSignalStrenghtValues());

				System.out.println(	"AP: Nome: "+ ap.getAccessPointName() + 
						" MAC: " + ap.getAccessPointMAC() + 
						" RSSI: " + ap.getSignalStrenght());
			}else{
				System.out.println("Nessun AP associato"); 

			}


			if(!rwlc.isAssociated())	{
				printVectorAP(rwlc.getVisibleAccessPoints());
				System.out.println("Get visible access point");
			}



			/*for(int i=0;i<1;i++){
				try {
					if(rwlc.isAssociated())	{
						rwlc.updateSignalStrenghtValue();
						System.out.println(	"Rilevazione #"+i); 
						printArray(ap.getLastSignalStrenghtValues());
						Thread.sleep(2000);
					}
					else {
						System.out.println("Nessun AP associato");
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
		} catch (WNICException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.exit(1);
		}  catch (InvalidAccessPoint e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	private static void printVectorAP(Vector<AccessPointData> vect){
		System.out.println("AP VISIBILI:");
		for(int k = 0; k<vect.size(); k++)
		{
			System.out.println("["+(vect.get(k)).getAccessPointName()+", "+(vect.get(k)).getAccessPointMAC()+", "+(vect.get(k)).getAccessPointMode()+", "+(vect.get(k)).getSignalStrenght()+" ]");
		}
	}
}