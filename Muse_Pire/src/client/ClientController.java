/**
 * 
 */
package client;

import debug.DebugConsole;

/**
 * @author Leo DiCarlo
 *
 */
public class ClientController {

	//private ClientFrameController frameController;

	private ClientSessionManager sessionManager;
	private ClientElectionManager electionManager;
	private DebugConsole console;

	public ClientController(){//ClientFrame frame){
		//if(frame.getController() != null)
		//frameController = frame.getController();
		this.console = new DebugConsole();
		this.console.setTitle("DEBUG CLIENT CONSOLE");
		//this.sessionManager = ClientSessionManager.getInstance();
		//this.sessionManager.setDebugConsole(this.console);
		//this.sessionManager.setFrameController(frameController);
		this.electionManager = ClientElectionManager.getINSTANCE();
		this.electionManager.setDebugConsole(console);
		this.electionManager.init();
		//this.electionManager.setDebugConsole(this.console);
		
		//this.electionManager.setFrameController(frameController);
		//this.electionManager.addObserver(this.sessionManager);
		//this.sessionManager.setElectionManager(electionManager);
		//frameController.setSessionManager(this.sessionManager);
	}
	
	
	/**
	 * @return the frameController
	 */
	//public ClientFrameController getFrameController() {
		//return frameController;
	//}
	/**
	 * @param frameController the frameController to set
	 */
	//public void setFrameController(ClientFrameController frameController) {
		//this.frameController = frameController;
	//}
	/**
	 * @return the sessionManager
	 */
	public ClientSessionManager getSessionManager() {
		return sessionManager;
	}
	/**
	 * @param sessionManager the sessionManager to set
	 */
	public void setSessionManager(ClientSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
}
