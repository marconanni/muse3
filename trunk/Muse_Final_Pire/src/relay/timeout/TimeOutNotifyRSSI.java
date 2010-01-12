package relay.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo
 *
 */

public class TimeOutNotifyRSSI extends Timer {

	public static TimeOutNotifyRSSI getInstance(){
		return new TimeOutNotifyRSSI();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTNOTIFYRSSI");cancelTimeOutNotifyRSSI();}}, tV);
		//System.out.println("TimeOutNotifyRSSI_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutNotifyRSSI(){
		super.cancel();
	}
}
