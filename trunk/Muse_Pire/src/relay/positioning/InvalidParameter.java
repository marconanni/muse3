package relay.positioning;

/**
 * Eccezione lanciata nal caso in cui un parametro passato ai metodi nelle classi del package handoff non sia valido
 * @author Zapparoli Pamela
 * @version 1.0
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