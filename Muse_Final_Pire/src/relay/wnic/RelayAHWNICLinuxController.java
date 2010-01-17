package relay.wnic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import parameters.DebugConfiguration;
import parameters.NetConfiguration;
import relay.wnic.exception.WNICException;

import debug.DebugConsole;

public class RelayAHWNICLinuxController implements RelayWNICController {

	private boolean debug = false;
	private boolean isOn = false;
	private boolean isAssociated =false;
	private boolean modeAdHoc = false;
	private String interf = null;
	private String essidName = null;
	private boolean essidFound = false;
	private DebugConsole console =null;


	public RelayAHWNICLinuxController(String ethX, String netName) throws WNICException{
		setInterfaceName(ethX);
		setEssidName(netName);
	}
	
	//METODI IMPLEMENTATIVI DI RELAYWNICCONTROLLER
	@Override
	public void init()throws WNICException{
		BufferedReader ifconfigUp = up();
		String line = null;
		try{
			line = ifconfigUp.readLine();
		}catch (IOException e){e.printStackTrace();}
		
		System.out.println("Line:["+line+"]");
		if(line!=null){
			if(console!=null)console.debugMessage(DebugConfiguration.DEBUG_ERROR, "Impossibile accendere la scheda di rete, interfaccia ["+getInterfaceName()+"] non esiste.");
			else System.out.println("Impossibile accendere la scheda di rete, interfaccia ["+getInterfaceName()+"] non esiste.");
			throw new WNICException("RelayAHWNICLinuxController ERRORE: Impossibile accendere la scheda di rete, interfaccia ["+getInterfaceName()+"] non esiste.");
		}else{
			isOn = true;
				
			refreshStatus();
				if (!isOn || !ismodeAdHoc() || !isEssidFound()){
					if(console!=null)
						console.debugMessage(DebugConfiguration.DEBUG_ERROR, "La scheda wireless deve essere accesa e l'interfaccia ["+interf+"] deve essere configurata nel seguente modo:\n" +
						NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE+"\nESSID: "+NetConfiguration.NAME_OF_AD_HOC_CLUSTER_NETWORK+"\nMODE:Ad-Hoc\nIP:"+NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS+
						"\n\n"+ NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_WIFI_INTERFACE+"\nESSID: "+NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK+"\nMODE:Ad-Hoc\nIP:"+NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS);
					throw new WNICException("La scheda wireless deve essere accesa e l'interfaccia ["+interf+"] deve essere configurata nel seguente modo:\n" +
						NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE+"\nESSID: "+NetConfiguration.NAME_OF_AD_HOC_CLUSTER_NETWORK+"\nMODE:Ad-Hoc\nIP:"+NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS+
						"\n\n"+ NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_WIFI_INTERFACE+"\nESSID: "+NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK+"\nMODE:Ad-Hoc\nIP:"+NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS);
				}else{
					if(console!=null)
						console.debugMessage(DebugConfiguration.DEBUG_INFO, "Configurazione scheda WIFI:\nInterfaccia: "+interf+
						"\nESSID: "+((interf.compareTo(NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE)==0)?NetConfiguration.NAME_OF_AD_HOC_CLUSTER_NETWORK:NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK)+
						"\nMODE:Ad-Hoc\nIP:"+((interf.compareTo(NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE)==0)?NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS:NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS));
					else
						System.out.println("Configurazione scheda WIFI:\nInterfaccia: "+interf+
								"\nESSID: "+((interf.compareTo(NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE)==0)?NetConfiguration.NAME_OF_AD_HOC_CLUSTER_NETWORK:NetConfiguration.NAME_OF_AD_HOC_CLUSTER_HEAD_NETWORK)+
								"\nMODE:Ad-Hoc\nIP:"+((interf.compareTo(NetConfiguration.NAME_OF_AD_HOC_CLUSTER_WIFI_INTERFACE)==0)?NetConfiguration.RELAY_AD_HOC_CLUSTER_ADDRESS:NetConfiguration.RELAY_AD_HOC_CLUSTER_HEAD_ADDRESS));
				}
				
		}
	}

	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete, false altrimenti
	 */
	@Override
	public boolean isAssociated() throws WNICException {
		refreshStatus();
		return isAssociated;
	}
	
	/**Metodo per sapere se il nodo è connesso ad una rete Ad-Hoc
	 * @return true se è connesso, false altrimenti
	 * @throws WNICException
	 */
	@Override
	public boolean isConnected() throws WNICException{
		refreshStatus();
		return isAssociated && isOn && essidFound;
	}
	
	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è accesa, false altrimenti
	 */
	@Override
	public boolean isOn() throws WNICException {
		refreshStatus();
		return isOn;
	}
	
	/**Metodo per aggiornare l'array di valori memorizzato dentro il <b>currentAP</b> aggiungendone uno appena rilevato
	 * @throws WNICException
	 */
	@Override
	public int getSignalStrenghtValue() throws WNICException{
		try {
			return getActualRSSIValue(interf);
		}catch (Exception e) {
			if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR, "Impossibile ottenere il nuovo valore di RSSI");
			throw new WNICException("RelayAHWNICLinuxController ERRORE: impossibile ottenere il nuovo valore di RSSI");
		}
	}
	
	@Override
	public AccessPointData getAssociatedAccessPoint()throws WNICException {return null;}
	
	@Override
	public int updateSignalStrenghtValue()throws WNICException {return 0;}
	
	@Override
	public void setDebugConsole(DebugConsole console) {this.console=console;}
	
	@Override
	public DebugConsole getDebugConsole() {return this.console;}
	
	//METODI PRIVATI
	/** Metodo per accendere la scheda WIFI tramite il comando ifconfig up*/
	private BufferedReader up() throws WNICException{
		try{
			Process p= Runtime.getRuntime().exec("/sbin/ifconfig " + getInterfaceName()+ "up");
			p.waitFor();
			return new BufferedReader(new InputStreamReader(p.getInputStream()));
		}catch (Exception e){
			if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Errore nell'eseguire il comando : /sbin/ifconfig " + getInterfaceName()+ "up");
			throw new WNICException("RelayAHWNICLinuxController ERRORE: Errore nell'eseguire il comando : /sbin/ifconfig " + getInterfaceName()+ "up");
		}	
		
	}

	/**
	 * Metodo per rinfrescare lo stato del RelayWNICLinuxController grazie all'essecuzione del comando <code>iwconfig<code>.
	 * @param interfaceInfo rappresenta in pratica il risultato di una chiamata al comando linux <code> /sbin/iwconfig interface<code>
	 * @throws WNICException
	 */
	private void refreshStatus() throws WNICException{
		BufferedReader interfaceInfo = getInterfaceInfo(interf);
		String res,res1 = null;
		try{
			if((res = interfaceInfo.readLine())!=null){
				res1 = interfaceInfo.readLine();
				setWifiInfo(res, res1);
			}else{
				if(debug){
					if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia "+ interf +" NON ESISTE!");
					else System.out.println("L'interfaccia "+ interf +" NON ESISTE!");
				}
			}
			interfaceInfo.close();
		}catch (IOException e){e.printStackTrace();}
	}
	
	private void setWifiInfo(String line1, String line2)throws WNICException{
		//scheda spenta
		if (line1.contains("off/any") || line2.contains("Not-Associated")){
			isAssociated = false;
			//essidFound = false;
			if(debug){
				if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_WARNING,"L'interfaccia ["+ interf +"] ESISTE però è SPENTA!");
				else System.out.println("L'interfaccia ["+ interf +"] ESISTE però è SPENTA!");
			}
		}

		//On ma non associata alla rete ad hoc
		else if (line1.contains("IEEE") && !line2.contains("Not-Associated")){
			isAssociated = true;
			if(debug){
				if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia ["+ interf +"]  ESISTE ed è ACCESA.");
				else System.out.println("L'interfaccia ["+ interf +"]  ESISTE ed è ACCESA.");
			}
		}
		if(line1.contains(essidName)){
			essidFound = true;
			if(debug){
				if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_INFO,"L'interfaccia ["+ interf +"] è CONNESSA alla rete ["+essidName+"]");
				else System.out.println("L'interfaccia ["+ interf +"] è CONNESSA alla rete ["+essidName+"]");
			}
		}else {
			essidFound = false;
			if(debug){
				if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR,"L'interfaccia ["+ interf +"] non è connessa alla rete [" + essidName+"]");
				else System.out.println("L'interfaccia ["+ interf +"] non è connessa alla rete [" + essidName+"]");
			}
		}
			
			//controllo il mode della scheda (deve essere Ad-Hoc)
			//res = interfaceInfo.readLine();
			if(line2.contains("Ad-Hoc")) modeAdHoc = true;
			else modeAdHoc = false;
		
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
			if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Impossibile ottenere informazioni dalla scheda wireless");
			throw new WNICException("RelayAHWNICLinuxController ERRORE: Impossibile ottenere informazioni dalla scheda wireless");
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
	 * @throws IOException 
	 * @throws InvalidAccessPoint
	 */
	private int getActualRSSIValue(String iN) throws WNICException, IOException{

		BufferedReader br = getInterfaceInfo(iN); 
		setWifiInfo(br.readLine(), br.readLine());
		if(isConnected()){
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
				if(console!=null) console.debugMessage(DebugConfiguration.DEBUG_ERROR,"Impossibile ottenere il valore RSSI attuale");
				throw new WNICException("RelayAHWNICLinuxController Impossibile ottenere il valore RSSI attuale");
			}
			
		}
		return -1;
	}
	
	/**Metodo per capire se l'interfaccia è accesa o meno
	 * @return true se l'interfaccia è associata ad una rete ad-hoc, false altrimenti
	 */
	private boolean ismodeAdHoc() throws WNICException {
		refreshStatus();
		return modeAdHoc;
	}

	/**Metodo per sapere se la rete passata al costruttore è stata rilevata
	 * @return true se la rete è stata rilevata, false altrimenti
	 */
	private boolean isEssidFound() {
		return essidFound;
	}
	

	//METODI PUBLICI
	
	/**Metodi getter and setter*/
	public String getInterfaceName() {return interf;}
	public void setInterfaceName(String interf) {this.interf=interf;}
	public String getEssidName() {return essidName;}
	public void setEssidName(String essidName) {this.essidName=essidName;}
	public void setDebug(boolean debug){this.debug=debug;}
}
	