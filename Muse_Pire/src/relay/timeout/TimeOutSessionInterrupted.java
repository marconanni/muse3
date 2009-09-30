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
/*public class TimeOutSessionInterrupted extends Observable {


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
	
	public static final TimeOutSessionInterrupted INSTANCE = new TimeOutSessionInterrupted();
	
	public TimeOutSessionInterrupted()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutSessionInterrupted getInstance() {
		return TimeOutSessionInterrupted.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTSESSIONINTERRUPTED");}}, timeoutValue);
		System.out.println("TimeOutSessionInterrupted_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutSessionInterrupted(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutSessionInterrupted_cancelTimeOutSessionInterrupted: Timer is null...");}
	}


}
*/


public class TimeOutSessionInterrupted extends Timer {

	public static TimeOutSessionInterrupted getInstance(){
		return new TimeOutSessionInterrupted();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSESSIONINTERRUPTED");cancelTimeOutSessionInterrupted();}}, tV);
		System.out.println("TimeOutSessionInterrupted_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutSessionInterrupted(){
		super.cancel();
	}
}