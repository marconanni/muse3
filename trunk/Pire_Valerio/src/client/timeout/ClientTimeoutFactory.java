/**
 * 
 */
package client.timeout;

import java.util.Observer;


/**
 * @author Leo Di Carlo, Pire Dejaco
 *
 */
public class ClientTimeoutFactory {

	
	public static TimeOutSingleWithMessage getSingeTimeOutWithMessage(Observer obs, long timeoutValue, String message){
		TimeOutSingleWithMessage timer = TimeOutSingleWithMessage.getInstance();
	    timer.schedule(obs, timeoutValue, message);
	    return timer;
	}
	
}

