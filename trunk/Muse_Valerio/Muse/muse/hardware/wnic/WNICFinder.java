package hardware.wnic;

import hardware.wnic.exception.*;
import parameters.Parameters;
/**
 * WNICFinder.java
 * Questa classe consente di ottenere il controller della scheda wireless
 *  adatto al sistema operativo usato.
 * @author Zapparoli Pamela
 * @version 1.0
 */
public class WNICFinder 
{
	/**
	 * Metodo che restituisce un controller per la scheda wireless in uso.
	 * La classe dell'oggetto restituito sar� diversa a seconda del sistema operativo,
	 *  ma sara' sempre un'implementazione dell'interfaccia WNICController.
	 * @return il controller della WNIC
	 */
	public static WNICController getCurrentWNIC() throws OSException, WNICException
	{
		//controlla il sistema operativo corrente
		String os=null;
		try
		{
			os=System.getProperty("os.name");
			System.out.println("PIER: Os "+os);
		}
		catch (Exception e)
		{
			throw new OSException("Nome del sistema operativo non valido");
		}
		WNICController c=null;
		
		System.out.println(os);//DEBUG
		if(os==null)
		{
			throw new OSException("Nome del sistema operativo non valido");
		}
		try
		{
			if(os.startsWith("Windows"))
			{		
				//c=new WNICWinController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
				c=new DummyController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
			}
			else
				if(os.startsWith("Mac"))
				{
					c= new WNICMacOsController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
					//c=new DummyController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
				}
				else
					if(os.startsWith("Linux"))
					{
						//c= new WNICLinuxController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
						c=new DummyController(Parameters.NUMBER_OF_PREVIOUS_RSSI_PER_ACCESSPOINT);
					}
					else
						throw new OSException("Sistema operativo non riconosciuto");
		}
		catch (InvalidParameter ee)
		{}
		return c;
	}
	
}
