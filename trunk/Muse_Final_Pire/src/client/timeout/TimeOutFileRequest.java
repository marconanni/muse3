package client.timeout;

import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Leo Di Carlo, Dejaco Pire
 * @version 1.1
 */
public class TimeOutFileRequest extends Timer {
	
	public static TimeOutFileRequest getInstance(){
		return new TimeOutFileRequest();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTFILEREQUEST");cancelTimeOutFileRequest();}}, tV);
		System.out.println("TIMEOUTFILEREQUEST_SCHEDULE: SETTATO IL TIMEOUT...");
	} 

	public void cancelTimeOutFileRequest(){
		super.cancel();
	}

}