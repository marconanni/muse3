/**
 * 
 */
package relay.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo, Pire Dejaco
 * @version 1.1
 */

public class TimeOutSingleWithMessage extends Timer {

	public static TimeOutSingleWithMessage getInstance(){
		return new TimeOutSingleWithMessage();
	}

	public void schedule(final Observer obs, long tV, final String message){
		super.schedule(new TimerTask(){public void run(){obs.update(null,message);cancelTimeOutSingleWithMessage();}}, tV);
	} 

	public void cancelTimeOutSingleWithMessage(){
		super.cancel();
	}
}