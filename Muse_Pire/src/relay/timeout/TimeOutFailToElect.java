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
/*public class TimeOutFailToElect extends Observable {


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
	
	public static final TimeOutFailToElect INSTANCE = new TimeOutFailToElect();
	
	public TimeOutFailToElect()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutFailToElect getInstance() {
		return TimeOutFailToElect.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTFAILTOELECT");}}, timeoutValue);
		System.out.println("TimeOutFailToElect_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutFailToElect(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutFailToElect_cancelTimeOutFailToElect: Timer is null...");}
	}

}*/


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
