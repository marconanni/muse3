package hardware.battery;

import hardware.battery.exception.*;
/**
 * MobileBatteryMonitor.java
 * Questa classe consente di ottenere informazioni relative allo stato della batteria interna 
 * 
 * @author Zapparoli Pamela
 * @version 1.0
 */

public interface MobileBatteryMonitor 
{
	/**
	 * Restituisce la percentuale di carica residua della batteria
	 * @return la percentuale di carica residua della batteria
	 * @throws NoBatteryException se non è presente alcuna batteria
	 * @throws BatteryException se non è possibile ottenere lo stato della batteria
	 */
	public int getRemainingPowerPercentage()throws NoBatteryException, BatteryException;
	
	/**
	 * Restituisce i minuti stimati di autonomia residua della batteria
	 * @return i minuti stimati di autonomia residua della batteria, -1 se non è stata ancora fatta una stima
	 * @throws NoBatteryException se non è presente alcuna batteria
	 * @throws BatteryException se non è possibile ottenere lo stato della batteria
	 */
	public long getRemainingPowerMinutes()throws NoBatteryException, BatteryException;
}
