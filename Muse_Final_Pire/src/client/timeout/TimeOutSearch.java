package client.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author Leo Di Carlo, Dejaco Pire
 * @version 1.1
 *
 */

public class TimeOutSearch extends Timer {
	
	public static TimeOutSearch getInstance(){
		return new TimeOutSearch();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSEARCH");cancelTimeOutSearch();}}, tV);
	} 

	public void cancelTimeOutSearch(){
		super.cancel();
	}

}