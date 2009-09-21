/**
 * 
 */
package client.timeout;

import java.util.Observer;

/**
 * @author Leo Di Carlo
 *
 */
public class ClientTimeoutFactory {

	
	public static TimeOutSearch getTimeOutSearch(Observer obs, long timeoutValue){
		TimeOutSearch timer = TimeOutSearch.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutFileRequest getTimeOutFileRequest(Observer obs, long timeoutValue){
		TimeOutFileRequest timer = TimeOutFileRequest.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}

	public static TimeOutFailToElect getTimeOutFailToElect(Observer obs, long timeoutValue){
		TimeOutFailToElect timer = TimeOutFailToElect.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
}

