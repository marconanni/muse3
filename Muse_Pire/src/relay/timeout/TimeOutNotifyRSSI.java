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
/*public class TimeOutNotifyRSSI extends Observable {


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
	
	public static final TimeOutNotifyRSSI INSTANCE = new TimeOutNotifyRSSI();
	
	public TimeOutNotifyRSSI()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutNotifyRSSI getInstance() {
		return TimeOutNotifyRSSI.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTNOTIFYRSSI");}}, timeoutValue);
		System.out.println("TimeOutNotifyRSSI_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutNotifyRSSI(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutNotifyRSSI_cancelTimeOutNotifyRSSI: Timer is null...");}
	}


}*/


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
