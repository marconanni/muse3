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

import parameters.Parameters;


public class RelayWNICLinuxController implements RelayWNICController{

	private AccessPointData currentAP;
	private boolean isOn = false;
	private boolean isAssociated = false;
	private int numberOfPreviousRSSI = -1;
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;



	public RelayWNICLinuxController(int previous, String ethX, String netName) throws WNICException{		

		interf = ethX;
		essidName = netName;
		numberOfPreviousRSSI=previous;
		
		System.out.println("RelayWNICLinuxController: L'interfaccia di rete " +  ethX + " esiste, ora ne controllo lo stato");

		refreshStatus();
		
		if(previous<=0)
			throw new WNICException("RelayWNICLinuxController: ERRORE: numero di precedenti RSSI da memorizzare non positivo");		

		if (!isOn)
			throw new WNICException("RelayWNICLinuxController: ERRORE: la scheda wireless deve essere accesa per procedere");
	
		System.out.println("RelayWNICLinuxController: L'interfaccia "+interf+" è attiva");
		System.out.println("RelayWNICLinuxController: " + ((isConnected())?"L'interfaccia " +interf+ " è connessa ad un AP":"L'interfaccia " +interf+ " non è connessa a nessun AP"));

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

				if (res.contains("radio off")){
					isAssociated = false;
					isOn = false;
					currentAP = null;
					essidFound = false;
					//System.out.println("RelayWNICLinuxController.refreshStatus(): radio off");
				}

				else if (res.contains("IEEE")){
					isOn = true;
					isAssociated = true;
					if(res.contains(essidName)){
						//System.out.println("RelayWNICLinuxController.refreshStatus(): radio on associated");
						essidFound = true;
						if(currentAP==null)currentAP = createCurrentAccessPointData(res,interfaceInfo);		
					}
					else {
						essidFound = false; 
						throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia " +interf + " non è connessa alla rete " + essidName);
					}
				}

				else if (res.contains("unassociated")) {
					isOn = true;
					isAssociated = false;
					currentAP = null;
					essidFound = false;
					//System.out.println("RelayWNICLinuxController.refreshStatus(): radio on unassociated");
				}
				else throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia " +interf + " non esiste !");
			}
			else throw new WNICException("RelayWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");

			interfaceInfo.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Metodo per creare un AccessPointData grazie ai dati provenienti dal risultato del comando <code>/sbin/iwconfig<code>
	 * @param firstLine la prima linea del risultato della chiamata al comando linux <code>/sbin/iwconfig interface<code>
	 * @param rest
	 * @return un AccessPointData da assegnare al riferimento locale
	 */
	private AccessPointData createCurrentAccessPointData(String firstLine, BufferedReader rest){

		String essidName = null;
		String mac = null;
		double firstRSSI = -1;
		String line = null;

		try {
			essidName = extractEssidName(firstLine);
			mac = extractMacAddress(rest.readLine(),20);
			do line=rest.readLine();
			while(line!=null && !line.contains("Signal"));
			firstRSSI = (double)extractRSSIValue(line);
			rest.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		/*DEBUG*/
		/*	System.out.println("ESSID: " + ( essidName!=null?essidName:"null"));
		System.out.println("MAC: " + ( mac!=null?mac:"null"));
		System.out.println("RSSI: " + ( firstRSSI>0?firstRSSI:"null"));*/

		try {
			return new AccessPointData(essidName,mac,firstRSSI,numberOfPreviousRSSI);
		} catch (InvalidAccessPoint e) {
			e.printStackTrace();
			return null;
		} catch (InvalidParameter e) {
			e.printStackTrace();
			return null;
		}
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
		refreshStatus();
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

		refreshStatus();

		Vector<AccessPointData> vect = new Vector<AccessPointData>();
		if(isAssociated && isOn && essidFound) {
			vect.add(currentAP); 
			return vect;
		}

		String line = null;
		String mac = null;
		String essidName =null;
		double firstRSSI = -1;
		StringTokenizer st = null;
		BufferedReader br = getVisibleAccessPointInfo(interf);
		try {
			while((line = br.readLine())!=null){

				if(line.contains("Cell")){
					mac = extractMacAddress(line, 18);

					st =new StringTokenizer(br.readLine(),"\"");
					st.nextToken();
					essidName = st.nextToken();

					do {line = br.readLine();}
					while(line !=null && !line.contains("Signal"));
					firstRSSI = (double)extractRSSIValue(line);
					vect.add(new AccessPointData(essidName,mac,firstRSSI,numberOfPreviousRSSI));
				}
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidParameter e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

			rwlc = new RelayWNICLinuxController(6, "eth1", "NETGEAR");

			//if(rwlc.connectToAccessPoint("NETGEAR"))System.out.println("Connessione a " + rwlc.getAssociatedAccessPoint().getAccessPointName()+" riuscita");


			if(rwlc.isAssociated())	{

				ap = rwlc.getAssociatedAccessPoint();

				printArray(ap.getLastSignalStrenghtValues());

				System.out.println(	"AP: Nome: "+ ap.getAccessPointName() + 
						" MAC: " + ap.getAccessPointMAC() + 
						" RSSI: " + ap.getSignalStrenght());
			}else{
				System.out.println("Nessun AP associato"); 

			}


			if(rwlc.isAssociated())	{
				printVectorAP(rwlc.getVisibleAccessPoints());
			}



			for(int i=0;i<12;i++){
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
			}
		} catch (WNICException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
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
		System.out.print("AP VISIBILI: [");
		for(int k = 0; k<vect.size(); k++)
		{
			System.out.print((vect.get(k)).getAccessPointName()+", ");
		}
		System.out.println("]\n");
	}
}