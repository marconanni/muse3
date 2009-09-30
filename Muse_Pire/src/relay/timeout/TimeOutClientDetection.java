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
/*public class TimeOutClientDetection extends Observable {


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
	
	public static final TimeOutClientDetection INSTANCE = new TimeOutClientDetection();
	
	public TimeOutClientDetection()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutClientDetection getInstance() {
		return TimeOutClientDetection.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTCLIENTDETECTION");}}, timeoutValue);
		System.out.println("TimeOutClientDetection_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutClientDetection(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutClientDetection_cancelTimeOutClientDetection: Timer is null...");}
	}


}*/


public class TimeOutClientDetection extends Timer {

	public static TimeOutClientDetection getInstance(){
		return new TimeOutClientDetection();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTCLIENTDETECTION");cancelTimeOutClientDetection();}}, tV);
		System.out.println("TimeOutAckSessionInfo_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutClientDetection(){
		super.cancel();
	}
}