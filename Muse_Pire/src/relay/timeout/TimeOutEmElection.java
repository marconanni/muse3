/*AGGIUNTO DA LUCA IL 7-12 ALLE 13:39*/

package relay.timeout;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Luca Campeti
 *
 */
/*public class TimeOutEmElection extends Observable {


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
	
	public static final TimeOutEmElection INSTANCE = new TimeOutEmElection();
	
	private TimeOutEmElection()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutEmElection getInstance() {
		return TimeOutEmElection.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(null, "TIMEOUTEMELECTION");}}, timeoutValue);
		System.out.println("TimeOutEmElection_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutEmElection(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutEmElection_cancelTimeOutEmElection: Timer is null...");}
	}

}*/


public class TimeOutEmElection extends Timer {

	public static TimeOutEmElection getInstance(){
		return new TimeOutEmElection();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTEMELECTION");cancelTimeOutEmElection();}}, tV);
		System.out.println("TimeOutEmElection_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutEmElection(){
		super.cancel();
	}
}
