package relay;

import parameters.*;

public class RelayMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		RelaySessionManager sessionManager = null;
		RelayElectionManager electionManager = null;
		sessionManager = RelaySessionManager.getInstance();
		
		electionManager = RelayElectionManager.getInstance(imRelay, sessionManager);
	//	electionManager.addObserver(sessionManager);
		sessionManager.setElectionManager(electionManager);
	}

}
