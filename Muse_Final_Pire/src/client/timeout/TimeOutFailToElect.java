package client.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo, Dejaco Pire
 * @version 1.1
 *
 */

public class TimeOutFailToElect extends Timer {

	public static TimeOutFailToElect getInstance(){
		return new TimeOutFailToElect();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTFAILTOELECT");cancelTimeOutFailToElect();}}, tV);
		System.out.println("TimeOutFailToElect_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutFailToElect(){
		super.cancel();
	}

}