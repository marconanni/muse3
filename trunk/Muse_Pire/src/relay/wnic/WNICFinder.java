package relay.wnic;

import parameters.Parameters;
import relay.wnic.exception.OSException;
import relay.wnic.exception.WNICException;

public class WNICFinder 
{
	/**
	 * Metodo che restituisce un controller per la scheda wireless in uso.
	 * La classe dell'oggetto restituito sar� diversa a seconda del sistema operativo,
	 *  ma sara' sempre un'implementazione dell'interfaccia WNICController.
	 * @return il controller della WNIC
	 */
	public static RelayWNICController getCurrentWNIC(String interf, String essidName, int previous) throws OSException, WNICException{
		String os=null;
		try
		{
			os=System.getProperty("os.name");
		}catch (Exception e){throw new OSException("Nome del sistema operativo non valido");}
		
		RelayWNICController c=null;
		
		if(os==null){throw new OSException("Nome del sistema operativo non valido");}
		if(os.startsWith("Windows")){
			if(interf.compareTo(Parameters.NAME_OF_AD_HOC_RELAY_INTERFACE)==0)
				c = new RelayAHWNICWindowsController(interf,essidName);
			else
				c = new RelayAPWNICWindowsController(previous,interf,essidName);
		}else if(os.startsWith("Mac")){
			if(interf.compareTo(Parameters.NAME_OF_AD_HOC_RELAY_INTERFACE)==0)
				c = new RelayAHWNICMacController(interf,essidName);
			
			else
				c = new RelayAPWNICMacController(previous,interf,essidName);
		}else if(os.startsWith("Linux")){
			if(interf.compareTo(Parameters.NAME_OF_AD_HOC_RELAY_INTERFACE)==0)
				c = new RelayAHWNICLinuxController(interf,essidName);
			else
				c = new RelayAPWNICLinuxController(previous,interf,essidName);
		}else throw new OSException("Sistema operativo non riconosciuto");
		
		return c;
	}
	
}
