package relay;

import parameters.*;

public class RelayMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		boolean type = NetConfiguration.IMRELAY;
		boolean active = NetConfiguration.IMBIGBOSS;
		
		RelaySessionManager sessionManager = null;
		RelayElectionManager electionManager = null;
		sessionManager = RelaySessionManager.getInstance();
		electionManager = RelayElectionManager.getInstance(true, true, sessionManager);
	//	electionManager.addObserver(sessionManager);
		sessionManager.setElectionManager(electionManager);
	}

}
