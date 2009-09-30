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
/*public class TimeOutSessionRequest extends Observable {


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
	
	public static final TimeOutSessionRequest INSTANCE = new TimeOutSessionRequest();
	
	public TimeOutSessionRequest()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutSessionRequest getInstance() {
		return TimeOutSessionRequest.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTSESSIONREQUEST");}}, timeoutValue);
		System.out.println("TimeOutSessionRequest_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutSessionRequest(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutSessionRequest_cancelTimeOutSessionRequest: Timer is null...");}
	}


}
*/

public class TimeOutSessionRequest extends Timer {

	public static TimeOutSessionRequest getInstance(){
		return new TimeOutSessionRequest();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSESSIONREQUEST");cancelTimeOutSessionRequest();}}, tV);
		System.out.println("TimeOutSessionRequest_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutSessionRequest(){
		super.cancel();
	}
}