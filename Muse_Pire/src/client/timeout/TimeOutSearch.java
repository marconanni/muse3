/**
 * 
 */
package client.timeout;

import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import parameters.Parameters;


/**
 * @author Leo Di Carlo
 *
 */
/*public class TimeOutSearch extends Observable {

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

	public static final TimeOutSearch INSTANCE = new TimeOutSearch();

	public TimeOutSearch()
	{
		if(this.timer == null)
		{
			timer = new Timer();
		}
	}

	public static TimeOutSearch getInstance() {
		return TimeOutSearch.INSTANCE;
	}

	public void schedule(final Observer obs, long timerValue){
		this.setObs(obs);
		this.setTimeoutValue(timerValue);
		timer.schedule(new TimerTask(){public void run(){obs.update(TimeOutSearch.getInstance(), "TIMEOUTSEARCH");}}, timeoutValue);
		System.out.println("TIMEOUTSEARCH_SCHEDULE: SETTATO IL TIMEOUT...");
	}

	public void cancelTimeOutSearch(){
		this.setObs(null);
		this.setTimeoutValue(-1);
		if(this.timer != null)
		{
			this.timer.cancel();
		}
		else{System.err.println("TimerOutSearch_cancelTImerOutSearch: Timer is null...");}
	}
}*/


public class TimeOutSearch extends Timer {

	public static TimeOutSearch getInstance(){
		return new TimeOutSearch();
	}

	public void schedule(final Observer obs, long tV){
		super.schedule(new TimerTask(){public void run(){obs.update(null,"TIMEOUTSEARCH");cancelTimeOutSearch();}}, tV);
		//System.out.println("TIMEOUTSEARCH_SCHEDULE: SETTATO IL TIMEOUT...a "+tV);
	} 

	public void cancelTimeOutSearch(){
		super.cancel();
	}

}



/*class TestTimeOutSearch implements Observer{

	public TimeOutSearch ts = null;

	public TestTimeOutSearch(){
		ts = ClientTimeoutFactory.getTimeOutSearch(this, 1000);
		if(ts!=null){
			ts.cancelTimeOutSearch();
			ts = null;
		}
	}

	public void restart(){
		ts  = ClientTimeoutFactory.getTimeOutSearch(this, Parameters.TIMEOUT_SEARCH);
	}

	public void update(Observable o, Object arg) {
		System.out.println("TimeOutSearchObserver.update: " + (String)arg);
	}
}


class TestMain {

	public static void main(String args[]){

		TestTimeOutSearch ttos = new TestTimeOutSearch();
		ttos.restart();
		ttos.restart();
		ttos.restart();
		
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		ttos.ts.cancelTimeOutSearch();
	}
}*/


//OUTPUT DI QUESTO TEST QUI SOPRA
/*TIMEOUTSEARCH_SCHEDULE: SETTATO IL TIMEOUT...
Exception in thread "main" java.lang.IllegalStateException: Timer already cancelled.
	at java.util.Timer.sched(Timer.java:354)
	at java.util.Timer.schedule(Timer.java:170)
	at client.timeout.TimeOutSearch.schedule(TimeOutSearch.java:53)
	at client.timeout.ClientTimeoutFactory.getTimeOutSearch(ClientTimeoutFactory.java:17)
	at client.timeout.TestTimeOutSearch.restart(TimeOutSearch.java:82)
	at client.timeout.TestMain.main(TimeOutSearch.java:99)*/


//OUTPUT DEL TEST DEL CLIENT CHE NON MI VA
/*java.lang.IllegalStateException: Timer already cancelled.
at java.util.Timer.sched(Timer.java:354)
at java.util.Timer.schedule(Timer.java:170)
at client.timeout.TimeOutSearch.schedule(TimeOutSearch.java:53)
at client.timeout.ClientTimeoutFactory.getTimeOutSearch(ClientTimeoutFactory.java:17)
at client.ClientElectionManager.searchingRelay(ClientElectionManager.java:533)
at client.ClientElectionManager.tryToSearchTheRelay(ClientElectionManager.java:546)
at client.TesterClientElectionManager.main(ClientElectionManager.java:923)*/