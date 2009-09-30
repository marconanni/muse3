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
/*public class TimeOutSessionInfo extends Observable {


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
	
	public static final TimeOutSessionInfo INSTANCE = new TimeOutSessionInfo();
	
	public TimeOutSessionInfo()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutSessionInfo getInstance() {
		return TimeOutSessionInfo.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTSESSIONINFO");}}, timeoutValue);
		System.out.println("TimeOutSessionInfo_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutSessionInfo(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutSessionInfo_cancelTimeOutSessionInfo: Timer is null...");}
	}


}
*/


public class TimeOutSessionInfo extends Timer {

	public static TimeOutSessionInfo getInstance(){
		return new TimeOutSessionInfo();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSESSIONINFO");cancelTimeOutSessionInfo();}}, tV);
		System.out.println("TimeOutSessionInfo_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutSessionInfo(){
		super.cancel();
	}
}