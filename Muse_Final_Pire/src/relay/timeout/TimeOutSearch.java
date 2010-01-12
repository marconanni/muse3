/**
 * 
 */
package relay.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo
 *
 */

public class TimeOutSearch extends Timer {

	public static TimeOutSearch getInstance(){
		return new TimeOutSearch();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSEARCH");cancelTimeOutSearch();}}, tV);
		System.out.println("TIMEOUTSEARCH_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutSearch(){
		super.cancel();
	}
}