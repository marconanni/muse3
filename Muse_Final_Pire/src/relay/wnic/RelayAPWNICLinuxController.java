package relay.wnic;

import relay.wnic.exception.*;
import relay.wnic.AccessPointData;
import relay.wnic.exception.InvalidAccessPoint;
import relay.wnic.exception.WNICException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import debug.DebugConsole;
import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;
import parameters.NetConfiguration;

public class RelayAPWNICLinuxController implements RelayWNICController{

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

	public RelayAPWNICLinuxController(String ethX) throws WNICException{
		this.interf = ethX;
		this.setNumberOfPreviousRSSI(ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
		setDebugConsole(new DebugConsole());
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ethX);
		refreshStatus();
		
		if (!isOn){
			this.console.debugMessage(DebugConfiguration.DEBUG_ERROR,"La scheda wireless deve essere accesa e l'interfaccia "+interf+" deve essere configurata nel modo seguente:\nESSID: "+NetConfiguration.NAME_OF_MANAGED_NETWORK+"\nMODE: Managed\nIp:"+NetConfiguration.RELAY_MANAGED_ADDRESS);
			throw new WNICException("RelayWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");
		}
	}


	public RelayAPWNICLinuxController(String ethX, String netName) throws WNICException{		

		interf = ethX;
		essidName = netName;
		this.setNumberOfPreviousRSSI(ElectionConfiguration.NUMBER_OF_SAMPLE_FOR_AP_GREY_MODEL);
		console = new DebugConsole();
		console.setTitle("RELAY WNIC LINUX CONTROLLER - DEBUG console for interface "+ethX);
		
		refreshStatus();
		
		if(numberOfPreviousRSSI<=0)
			throw new WNICException("RelayWNICLinuxController: ERRORE: numero di precedenti RSSI da memorizzare non positivo");		

		if (!isOn){
			this.console.debugMessage(DebugConfiguration.DEBUG_ERROR,"La scheda wireless deve essere accesa e l'interfaccia "+interf+" deve essere configurata nel modo seguente:\nESSID: "+NetConfiguration.NAME_OF_MANAGED_NETWORK+"\nMODE: Managed\nIp:"+NetConfiguration.RELAY_MANAGED_ADDRESS);
			throw new WNICException("RelayWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");
		}
//		if(!isEssidFound()){
//			this.console.debugMessage(Parameters.DEBUG_ERROR,"La scheda wireless non è associata al seguente ESSID "+essidName);
//			throw new WNICException("RelayWNICLinuxController:La scheda wireless non è associata al seguente ESSID "+essidName);
//		}
		console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia "+interf+" "+((isConnected())?" è connessa al Ap desiderato:"+essidName:"non è connessa al AP desiderato"));
	}

	public void init(){}

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
				if (res.contains("off/any") || res1.contains("Not-Associated")){
					isAssociated = false;
					isOn = true;
					currentAP = null;
					essidFound = false;
					if(debug)console.debugMessage(DebugConfiguration.DEBUG_WARNING,"L'interfaccia "+ interf +" ESISTE però è SPENTA!");
				}

				//On ma non associata alla rete managed
				else if (res.contains("IEEE") && !res1.contains("Not-Associated")){
					isOn = true;
					isAssociated = true;
					if(debug)console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia "+ interf +"  ESISTE ed è ACCESA.");
					if(res.contains(essidName)){
						essidFound = true;
						if(currentAP==null){
							currentAP = createCurrentAccessPointData(res,res1,interfaceInfo);
						}
						if(debug)console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia "+ interf +" è CONNESSA alla rete " + essidName);
					}
					else {
						essidFound = false; 
						console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						//throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia " +interf + " non è connessa alla rete " + essidName);
					}
				
				
					//controllo il mode della scheda (deve essere Ad-Hoc)
					//res = interfaceInfo.readLine();
					if(res1.contains("Managed")){
						modeManaged = true;
						if(debug)console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia "+ interf +" è settata a MODE Managed");
					}else {
						modeManaged = false;
						console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia "+ interf +" non è connessa alla rete " + essidName);
						//throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}	
				}else{
					console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
					throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
				}
			}else{
				console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
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
//	private void isWIFIOn() throws WNICException{
//
//		BufferedReader interfaceInfo = getInterfaceInfo(interf);
//		String res, res1 = null;
//		try{
//			if((res = interfaceInfo.readLine())!=null){
//				res1 = interfaceInfo.readLine();
//				//scheda spenta
//				if (res.contains("off/any") || res.contains("IEEE")){
//					isOn = true;
//					//On ma non associata alla rete managed
//					String essid = extractEssidName(res);
//					if(essid.compareTo(Parameters.NAME_OF_MANAGED_NETWORK)==0){
//						essidFound = true;
//						essidName = essid;
//						isAssociated = true;
//						if(currentAP==null)currentAP = createCurrentAccessPointData(res,res1,interfaceInfo);
//						
//					}else if(res1.contains("Not-Associated")){
//						essidFound = false;
//						currentAP = null;
//						isAssociated = false;
//					}
//				}				
//				
//
//			}else{
//				console.debugMessage(Parameters.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
//				throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
//			}
//			interfaceInfo.close();
//		}catch (IOException e){e.printStackTrace();}
//	}


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
			console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Impossibile ottenere informazioni dalla scheda wireless");
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
	/*private void connectTo(String iN, String essid, String mac, String myIP) throws WNICException{ 
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
	}*/


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
	public int getSignalStrenghtValue() throws WNICException {
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
	
	public void setNewCurrentAP(AccessPointData ap){
		this.currentAP=ap;
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
		refreshStatus();
		return isAssociated;
	}

	/**Metodo per ottenere il nome dell'interfaccia
	 * @return una String che rappresenta il nome dell'interfaccia gestita
	 */	
	public String getInterfaceName() {
		return interf;
	}
	
	public boolean isModeManaged()throws WNICException{
		refreshStatus();
		return modeManaged;
	}
	
	/**Metodo per ottenere una lista degli AccessPointData visibili dall'interfaccia in esame. Quando il nodo è connesso al
	 * currentAP esso risulta essere l'unico elemento della lista
	 * @return un Vector di AccessPointData rappresentante la lista di cui sopra
	 * @throws WNICException
	 * @throws InvalidAccessPoint
	 */
	public Vector<AccessPointData> getVisibleAccessPoints()	throws WNICException, InvalidAccessPoint {

		refreshStatus();

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
		BufferedReader br = getVisibleAccessPointInfo(interf);
		try {
			while((line = br.readLine())!=null){
				if(line.contains("Cell")){
					mac = extractMacAddress(line, 18);
					for(int i=0;i<3;i++){
						do{
						line = br.readLine();
						}while((!line.contains("Signal"))&&(!line.contains("ESSID"))&&(!line.contains("Mode"))&&(line!=null));
						if(line.contains("Signal"))
							firstRSSI = (double)extractRSSIValue(line);
						else if(line.contains("ESSID")){
							st =new StringTokenizer(line,"\"");
							st.nextToken();
							essidName = st.nextToken();
						}else if(line.contains("Mode")){
							st =new StringTokenizer(line,":");
							st.nextToken();
							mode = st.nextToken();
						}
					}
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
	/*public boolean connectToAccessPoint(String essidName) throws WNICException, InvalidAccessPoint{

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
			if(visibleAPs.get(i).getAccessPointName().equalsIgnoreCase(essidName)){
				find = true;
				findIndex =i;
				break;
			}
		}

		if(find){
			AccessPointData apToConnect = visibleAPs.get(findIndex);
			connectTo(interf, apToConnect.getAccessPointName(), apToConnect.getAccessPointMAC(), NetConfiguration.RELAY_MANAGED_ADDRESS);
			refreshStatus();
			if(isConnected())return true;
			else return false;
		}
		else return false;
	}*/


	/**Metodi setter and getter*/
	public String getEssidName() {return essidName;}
	public boolean isEssidFound() {return essidFound;}	
	public void setDebugConsole(DebugConsole console){this.console = console;}	
	public DebugConsole getDebugConsole(){return console;}
	public void setNumberOfPreviousRSSI(int numberOfPreviousRSSI) {this.numberOfPreviousRSSI = numberOfPreviousRSSI;}
	public int getNumberOfPreviousRSSI() {return numberOfPreviousRSSI;}
}