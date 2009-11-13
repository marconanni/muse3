package hardware.battery;

import hardware.battery.exception.NoBatteryException;
/**
 * Implementazione per Linux del controller della batteria interna.
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class MobileBatteryLinuxMonitor implements MobileBatteryMonitor
{

	public long getRemainingPowerMinutes() throws NoBatteryException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	public int getRemainingPowerPercentage() throws NoBatteryException
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
