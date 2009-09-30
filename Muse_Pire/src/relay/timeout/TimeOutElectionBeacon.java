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
/*public class TimeOutElectionBeacon extends Observable {


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
	
	public static final TimeOutElectionBeacon INSTANCE = new TimeOutElectionBeacon();
	
	public TimeOutElectionBeacon()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutElectionBeacon getInstance() {
		return TimeOutElectionBeacon.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTELECTIONBEACON");}}, timeoutValue);
		System.out.println("TimeOutElectionBeacon_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutElectionBeacon(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutElectionBeacon_cancelTimeOutElectionBeacon: Timer is null...");}
	}


}
*/


public class TimeOutElectionBeacon extends Timer {

	public static TimeOutElectionBeacon getInstance(){
		return new TimeOutElectionBeacon();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTELECTIONBEACON");cancelTimeOutElectionBeacon();}}, tV);
		System.out.println("TimeOutElectionBeacon_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutElectionBeacon(){
		super.cancel();
	}
}