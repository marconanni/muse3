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
/*public class TimeOutAckClientReq extends Observable {


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
	
	public static final TimeOutAckClientReq INSTANCE = new TimeOutAckClientReq();
	
	public TimeOutAckClientReq()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}
	
	public static TimeOutAckClientReq getInstance() {
		return TimeOutAckClientReq.INSTANCE;
	}
	
	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTACKCLIENTREQ");}}, timeoutValue);
		System.out.println("TimeOutAckClientReq_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutAckClientReq(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimeOutAckClientReq_cancelTimeOutFailToElect: Timer is null...");}
	}


}*/


public class TimeOutAckClientReq extends Timer {

	public static TimeOutAckClientReq getInstance(){
		return new TimeOutAckClientReq();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTACKCLIENTREQ");cancelTimeOutAckClientReq();}}, tV);
		System.out.println("TimeOutAckClientReq_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutAckClientReq(){
		super.cancel();
	}
}
