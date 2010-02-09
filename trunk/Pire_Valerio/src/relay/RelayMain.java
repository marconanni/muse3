package relay;

import parameters.NetConfiguration;


public class RelayMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		boolean imRelay = NetConfiguration.IMRELAY;
//		boolean imBigBoss = NetConfiguration.IMBIGBOSS;
//		RelaySessionManager sessionManager = null;
//		sessionManager = RelaySessionManager.getInstance();
//		sessionManager.setImRelay(imBigBoss,imRelay);
		new RelayController();//fa tutto lui
	}

}
