package relay;

import parameters.*;

public class RelayMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		boolean imRelay = Parameters.IMRELAY;
		boolean imBigBoss = Parameters.IMBIGBOSS;
		
		RelaySessionManager sessionManager = null;
		RelayElectionManager electionManager = null;
		sessionManager = RelaySessionManager.getInstance();
		sessionManager.setImRelay(imBigBoss, imRelay);
		electionManager = RelayElectionManager.getInstance(imRelay, sessionManager);
	//	electionManager.addObserver(sessionManager);
		sessionManager.setElectionManager(electionManager);
	}

}
