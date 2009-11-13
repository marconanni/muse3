package muse.client.exception;

/**
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class InvalidParameter extends Exception{

	public InvalidParameter(String s)
	{
		super(s);
	}
	
	public String getMessage() {
		return super.getMessage();
	}

	public StackTraceElement[] getStackTrace() {
		return super.getStackTrace();
	}
	

}