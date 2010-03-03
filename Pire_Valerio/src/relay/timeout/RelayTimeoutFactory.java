/**
 * 
 */
package relay.timeout;

import java.util.Observer;

/**
 * @author Leo Di Carlo, Pire Dejaco
 * @version 1.1
 */
public class RelayTimeoutFactory {

	
	public static TimeOutSingleWithMessage getSingeTimeOutWithMessage(Observer obs, long timeoutValue, String message){
		TimeOutSingleWithMessage timer = TimeOutSingleWithMessage.getInstance();
	    timer.schedule(obs, timeoutValue, message);
	    return timer;
	}
	
	/*public static TimeOutFailToElect getTimeOutFailToElect(Observer obs, long timeoutValue){
		TimeOutFailToElect timer = TimeOutFailToElect.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutToElect getTimeOutToElect(Observer obs, long timeoutValue){
		TimeOutToElect timer = TimeOutToElect.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutElectionBeacon getTimeOutElectionBeacon(Observer obs, long timeoutValue){
		TimeOutElectionBeacon timer = TimeOutElectionBeacon.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutClientDetection getTimeOutClientDetection(Observer obs, long timeoutValue){
		TimeOutClientDetection timer = TimeOutClientDetection.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutSessionRequest getTimeOutSessionRequest(Observer obs, long timeoutValue){
		TimeOutSessionRequest timer = TimeOutSessionRequest.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutSessionInfo getTimeOutSessionInfo(Observer obs, long timeoutValue){
		TimeOutSessionInfo timer = TimeOutSessionInfo.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutAckSessionInfo getTimeOutAckSessionInfo(Observer obs, long timeoutValue){
		TimeOutAckSessionInfo timer = TimeOutAckSessionInfo.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutAckClientReq getTimeOutTimeOutAckClientReq(Observer obs, long timeoutValue){
		TimeOutAckClientReq timer = TimeOutAckClientReq.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutSessionInterrupted getTimeOutSessionInterrupted(Observer obs, long timeoutValue){
		TimeOutSessionInterrupted timer = TimeOutSessionInterrupted.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
	public static TimeOutAckForward getTimeOutAckForward(Observer obs, long timeoutValue){
		TimeOutAckForward timer = TimeOutAckForward.getInstance();
	    timer.schedule(obs, timeoutValue);
	    return timer;
	}
	
*/
	
	/*AGGIUNTO DA LUCA IL 7-12 ALLE 13:39*/
//	public static TimeOutEmElection getTimeOutEmElection(Observer obs, long timeoutValue){
//		TimeOutEmElection timer = TimeOutEmElection.getInstance();
//	    timer.schedule(obs, timeoutValue);
//	    return timer;
//	}
	
}

