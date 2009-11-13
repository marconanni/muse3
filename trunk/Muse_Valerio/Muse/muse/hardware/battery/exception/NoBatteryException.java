package hardware.battery.exception;
/**
 * 
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class NoBatteryException extends Exception{

	public NoBatteryException(String s)
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