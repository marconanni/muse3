package relay.wnic.exception;
/**
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class InvalidAccessPoint extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidAccessPoint(String s)
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