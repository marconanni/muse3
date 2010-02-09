package relay.position;

/**
 * Eccezione lanciata nal caso in cui un parametro passato ai metodi nelle classi del package handoff non sia valido
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */

public class InvalidParameter extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


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