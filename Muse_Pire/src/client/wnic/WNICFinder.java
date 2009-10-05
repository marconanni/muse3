package client.wnic;

import client.wnic.exception.OSException;
import client.wnic.exception.WNICException;
/**
 * WNICFinder.java
 * Questa classe consente di ottenere il controller della scheda wireless
 *  adatto al sistema operativo usato.
 * @author Zapparoli Pamela (modificato da Dejaco Pire)
 * @version 1.1
 */
public class WNICFinder 
{
	/**
	 * Metodo che restituisce un controller per la scheda wireless in uso.
	 * La classe dell'oggetto restituito sar� diversa a seconda del sistema operativo,
	 *  ma sara' sempre un'implementazione dell'interfaccia WNICController.
	 * @return il controller della WNIC
	 */
	public static ClientWNICController getCurrentWNIC(String interf, String essidName) throws OSException, WNICException{
		String os=null;
		try
		{
			os=System.getProperty("os.name");
		}catch (Exception e){throw new OSException("Nome del sistema operativo non valido");}
		
		ClientWNICController c=null;
		
		if(os==null){throw new OSException("Nome del sistema operativo non valido");}
		if(os.startsWith("Windows")){		
			c = new ClientWNICWindowsController(interf,essidName);
		}else if(os.startsWith("Mac")){
			c = new ClientWNICMacOsController(interf,essidName);
		}else if(os.startsWith("Linux")){
			c = new ClientWNICLinuxController(interf,essidName);
		}else throw new OSException("Sistema operativo non riconosciuto");
		
		return c;
	}
	
}
