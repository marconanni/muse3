package test;

import hardware.battery.*;

/**
 * Test del package hardware.battery
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class BatteryTest 
{
	
	public static void main(String args[])
	{
		try
		{
			MobileBatteryMonitor b= new MobileBatteryMacOsMonitor();
			System.out.println("Percentuale residua :"+b.getRemainingPowerPercentage());
			long l=b.getRemainingPowerMinutes();
			if(l==-1)
				System.out.println("stima non pronta");
			else
				System.out.println("minuti residui: "+l);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

}
