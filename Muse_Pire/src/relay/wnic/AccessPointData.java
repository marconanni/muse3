package relay.wnic;

import java.util.*;

import relay.wnic.exception.InvalidAccessPoint;
import relay.wnic.exception.InvalidParameter;
/**
 * AccessPointData.java
 * Classe che contiene i dati relativi ad un access point:
 * <br/>il suo nome, il suo indirizzo MAC e un elenco delle ultime potenze del segnale rilevate.
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */
public class AccessPointData 
{
	private String name;
	private String mac;
	private Vector<Double> signal;
	private int maxSignalSize;
	
	/**
	 * Costruisce un oggetto AccessPointData
	 * @param name nome dell'access point
	 * @param mac indirizzo mac dell'access point
	 * @param RSSI valore di RSSI corrente
	 * @param numberOfPreviousRSSI numero di RSSI precedenti da tenere in memoria
	 * @throws InvalidAccessPoint se l'indirizzo mac o l'RSSI dell'access point non e' corretto
	 * @throws InvalidParameter se il nome o il mac dell'access point sono null
	 */
	public AccessPointData(String name, String mac, double RSSI, int numberOfPreviousRSSI) throws InvalidAccessPoint, InvalidParameter
	{
		if (name==null)
			throw new InvalidAccessPoint("ERRORE: nome dell'AP non valido");
		if(mac==null)
			throw new InvalidAccessPoint("ERRORE: MAC dell'AP non valido");
		mac=mac.trim();
		StringTokenizer token= new StringTokenizer(mac, ":");
		for(int i=0;i<6;i++)
		{
			try
			{
				int n=Integer.parseInt(token.nextToken(), 16);
				if((n<0) || (n>255))
					throw new InvalidAccessPoint("ERRORE: MAC dell'AP non valido");
			}
			catch(NoSuchElementException e)
			{
				throw new InvalidAccessPoint("ERRORE: MAC dell'AP non valido");
			}
			catch(NumberFormatException ee)
			{
				throw new InvalidAccessPoint("ERRORE: MAC dell'AP non valido");
			}
			
		}
		if(numberOfPreviousRSSI<=0)
			throw new InvalidParameter("ERRORE: numero di RSSI da tenere in memoria non positivo");
		if((RSSI<0) || (RSSI>120))
			throw new InvalidAccessPoint("ERRORE: Potenza del segnale dell'AP non valida "+RSSI);
		maxSignalSize=numberOfPreviousRSSI;
		signal= new Vector<Double>();
		this.name=name;
		this.mac=mac;
		signal.add(Double.valueOf(RSSI));
	}
	
	/**
	 * Restituisce il nome dell'access point
	 * @return il nome dell'access point
	 */
	public String getAccessPointName()
	{
		return name;
	}
	
	/**
	 * Restituisce il MAC dell'access point
	 * @return il MAC dell'access point
	 */
	public String getAccessPointMAC()
	{
		return mac;
	}
	
	/**
	 * Restituisce l'ultimo RSSI memorizzato per l'access point
	 * @return l'ultimo RSSI memorizzato per l'access point
	 */
	public int getSignalStrenght()
	{
		return (int)signal.elementAt(signal.size()-1).doubleValue();
	}
	
	/**
	 * Restituisce gli ultimi RSSI memorizzati per l'access point
	 * @return gli ultimi RSSI memorizzati per l'access point
	 */
	public double[] getLastSignalStrenghtValues()
	{
		double res[]= new double[signal.size()];
		for(int i=0; i<res.length; i++)
		{
			res[i]=signal.elementAt(i).doubleValue();
		}
		return res;
	}
	
	/**
	 * Memorizza un nuovo RSSI rilevato per l'access point
	 * @param val un nuovo RSSI rilevato per l'access point
	 * @throws InvalidParameter se il valore dell'RSSI non e' valido
	 */
	public void newSignalStrenghtValue(double val) throws InvalidParameter
	{
		if((val<0) || (val>120))
			throw new InvalidParameter("ERRORE: Potenza del segnale dell'AP non valida "+val);
		if(signal.size()<maxSignalSize)
			signal.add(Double.valueOf(val));
		else
		{
			signal.removeElementAt(0);
			signal.add(Double.valueOf(val));
		}
	}
}
