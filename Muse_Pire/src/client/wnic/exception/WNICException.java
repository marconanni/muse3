package client.wnic.exception;
/**
 * 
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class WNICException extends Exception{

	public WNICException(String s)
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
