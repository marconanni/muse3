package hardware.battery.exception;
/**
 * 
 * @author Zapparoli Pamela
 * @versione 0.1
 *
 */

public class BatteryException extends Exception{

	public BatteryException(String s)
	{
		super(s);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return super.getStackTrace();
	}
}
