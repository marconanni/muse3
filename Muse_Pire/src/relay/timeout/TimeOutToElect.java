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
/*public class TimeOutToElect extends Observable {


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
	
	public static final TimeOutToElect INSTANCE = new TimeOutToElect();
	
	public TimeOutToElect()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutToElect getInstance() {
		return TimeOutToElect.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTTOELECT");}}, timeoutValue);
		System.out.println("TimeOutToElect_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutToElect(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutToElect_cancelTimeOutToElect: Timer is null...");}
	}


}*/


public class TimeOutToElect extends Timer {

	public static TimeOutToElect getInstance(){
		return new TimeOutToElect();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTTOELECT");cancelTimeOutToElect();}}, tV);
		System.out.println("TimeOutToElect_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutToElect(){
		super.cancel();
	}
}
