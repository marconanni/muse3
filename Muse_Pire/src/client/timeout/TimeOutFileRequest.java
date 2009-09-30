/**
 * 
 */
package client.timeout;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo
 *
 */
/*public class TimeOutFileRequest extends Observable {

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
	
	public static final TimeOutFileRequest INSTANCE = new TimeOutFileRequest();
	
	public TimeOutFileRequest()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutFileRequest getInstance() {
		return TimeOutFileRequest.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTFILEREQUEST");}}, timeoutValue);
		System.out.println("TIMEOUTFILEREQUEST_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutFileRequest(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutFileRequest_cancelTimerFileRequest: Timer is null...");}
	}

}
*/

public class TimeOutFileRequest extends Timer {

	public static TimeOutFileRequest getInstance(){
		return new TimeOutFileRequest();
	}

	public void schedule(final Observer obs, long tV){
		// TODO Auto-generated method stub
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTFILEREQUEST");cancelTimeOutFileRequest();}}, tV);
		System.out.println("TIMEOUTFILEREQUEST_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutFileRequest(){
		super.cancel();
	}

}