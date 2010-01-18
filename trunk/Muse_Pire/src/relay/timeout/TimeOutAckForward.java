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
/*public class TimeOutAckForward extends Observable {


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
	
	public static final TimeOutAckForward INSTANCE = new TimeOutAckForward();
	
	public TimeOutAckForward()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutAckForward getInstance() {
		return TimeOutAckForward.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTACKFORWARD");}}, timeoutValue);
		System.out.println("TimeOutAckForward_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutAckForward(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutAckForward_cancelTimeOutAckForward: Timer is null...");}
	}


}*/


public class TimeOutAckForward extends Timer {

	public static TimeOutAckForward getInstance(){
		return new TimeOutAckForward();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTACKFORWARD");cancelTimeOutAckForward();}}, tV);
		System.out.println("TimeOutAckForward_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutAckForward(){
		super.cancel();
	}
}