package hardware.battery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import hardware.battery.exception.*;

/**
 * Implementazione per Mac OS X del controller della batteria interna.
 * @author Zapparoli Pamela	
 * @version 1.0
 *
 */

public class MobileBatteryMacOsMonitor implements MobileBatteryMonitor
{

	
	public long getRemainingPowerMinutes() throws NoBatteryException, BatteryException
	{
		String s=null;
		try
		{
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("pmset -g batt");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			out.readLine();
			s=out.readLine();
			if(s==null)
				throw new NoBatteryException("ERRORE: nel sistema non e' presente alcuna batteria");
			if(!(s.contains("InternalBattery")))
				throw new NoBatteryException("ERRORE: nel sistema non e' presente alcuna batteria interna");
			s=s.trim();
			StringTokenizer token= new StringTokenizer(s, ";");
			String r=null;
			for(int i=0;i<3;i++)
				r=token.nextToken();
			r=r.trim();
			token= new StringTokenizer(r, " :");
			try
			{
				int hour=Integer.parseInt(token.nextToken());
				int min=Integer.parseInt(token.nextToken());
				return hour*60+min;
			}
			catch (NumberFormatException ee)
			{
				return -1;
			}
		}
		catch (Exception e)
		{
			throw new BatteryException("ERRORE: impossibile verificare lo stato della batteria");
		}
	}

	
	public int getRemainingPowerPercentage() throws NoBatteryException, BatteryException
	{
		String s=null;
		try
		{
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("pmset -g batt");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			out.readLine();
			s=out.readLine();
			if(s==null)
				throw new NoBatteryException("ERRORE: nel sistema non e' presente alcuna batteria");
			if(!(s.contains("InternalBattery")))
				throw new NoBatteryException("ERRORE: nel sistema non e' presente alcuna batteria interna");
			s=s.trim();
			StringTokenizer token= new StringTokenizer(s, " \t%");
			token.nextToken();
			try
			{
				int time=Integer.parseInt(token.nextToken());
				return time;
			}
			catch (NumberFormatException e)
			{
				throw new BatteryException("ERRORE: stato della batteria non valido");
			}
		}
		catch (Exception e)
		{
			throw new BatteryException("ERRORE: impossibile verificare lo stato della batteria");
		}
	}

}
