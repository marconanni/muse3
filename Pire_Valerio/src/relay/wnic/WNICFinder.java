package relay.wnic;

import relay.wnic.exception.OSException;
import relay.wnic.exception.WNICException;

/**Classe che permette di verificare l'interfaccia della scheda di rete WIFI
 * @author Pire Dejaco
 * @version 1.1
 */

public class WNICFinder 
{
	/**
	 * Metodo che restituisce un controller per la scheda wireless in uso.
	 * La classe dell'oggetto restituito sara' diversa a seconda del sistema operativo e
	 * a seconda se si è collegati alla rete AD-HOC o alla rete MANAGED.
	 * Sara' sempre un'implementazione dell'interfaccia WNICController.
	 * @param interf stringa che rappresente il nome del'interfaccia WIFI 
	 * @param essidName stringa che rappresenta il nome della rete a cui l'interfaccia WIFI è connessa
	 * @param type 0 -> si è connessi alla rete MANAGED, 1 -> si è connessi alla rete AD-HOC
	 * @return il controller della WNIC
	 */
	public static RelayWNICController getCurrentWNIC(String interf, String essidName, int type) throws OSException, WNICException{
		String os=null;
		try
		{
			os=System.getProperty("os.name");
		}catch (Exception e){throw new OSException("Nome del sistema operativo non valido");}
		
		RelayWNICController c=null;
		
		if(os==null){throw new OSException("Nome del sistema operativo non valido");}
		if(os.startsWith("Windows")){
			//Interfaccia collegata al AP
			if(type== 0) c = new RelayAPWNICWindowsController(interf,essidName);
			
			//Interfaccia colleagata al rete ad hoc (MANET)
			if(type == 1) c = new RelayAHWNICWindowsController(interf,essidName);
			
		}else if(os.startsWith("Mac")){
			//Interfaccia collegata al AP
			if(type== 0) c = new RelayAPWNICMacController(interf,essidName);
			
			//Interfaccia colleagata al rete ad hoc (MANET)
			if(type == 1) c = new RelayAHWNICMacController(interf,essidName);
						
		}else if(os.startsWith("Linux")){
			//Interfaccia collegata al AP
			if(type== 0) c = new RelayAPWNICLinuxController(interf,essidName);
			
			//Interfaccia colleagata al rete ad hoc (MANET)
			if(type == 1) c = new RelayAHWNICLinuxController(interf,essidName);

		}else throw new OSException("Sistema operativo non riconosciuto");
		return c;
	}
}
