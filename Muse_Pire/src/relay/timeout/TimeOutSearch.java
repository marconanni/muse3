/**
 * 
 */
package relay.timeout;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;



/**
 * @author Leo Di Carlo
 *
 */
/*public class TimeOutSearch extends Observable {

	private Timer timer;
	private Observer obs;
	private long timeoutValue = -1;
	*//**
	 * @param obs the obs to set
	 *//*
	public void setObs(Observer obs) {
		this.obs = obs;
	}
	*//**
	 * @param timeoutValue the timeoutValue to set
	 *//*
	public void setTimeoutValue(long timeoutValue) {
		this.timeoutValue = timeoutValue;
	}
	
	public static final TimeOutSearch INSTANCE = new TimeOutSearch();
	
	public TimeOutSearch()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutSearch getInstance() {
		return TimeOutSearch.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTSEARCH");}}, timeoutValue);
		System.out.println("TIMEOUTSEARCH_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimerOutSearch(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimerOutSearch_cancelTImerOutSearch: Timer is null...");}
	}

}*/

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