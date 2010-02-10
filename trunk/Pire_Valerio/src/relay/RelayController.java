package relay;

import parameters.NetConfiguration;

public class RelayController {
	
	private RelayElectionManager electionManager  = null;
	private RelaySessionManager sessionManager = null;
	private int type = 0;
	private boolean state = false;
	
	public RelayController(){
		if(NetConfiguration.IMBIGBOSS){
			setType(0);
			setState(true);
		}else if(NetConfiguration.IMPOSSIBLEBIGBOSS){
			setType(0);
			setState(false);
		}else if(NetConfiguration.IMRELAY){
			setType(1);
			setState(true);
		}else if(NetConfiguration.IMPOSSIBLERELAY){
			setType(1);
			setState(false);
		}else if(NetConfiguration.IMCLIENT){
			setType(2);
			setState(true);
		}

		sessionManager = RelaySessionManager.getInstance();
		electionManager = RelayElectionManager.getInstance(type, state, sessionManager);
		
		//Valerio:qui dentro scrivo a mano i tre indirizzi che mi servono per un relay
		electionManager.setLocalClusterAddress("127.0.0.1");
		electionManager.setLocalClusterHeadAddress("127.0.0.1");
		electionManager.setConnectedClusterHeadAddress("127.0.0.1");
		sessionManager.setElectionManager(electionManager);
		
	}

	public void setState(boolean state) {this.state = state;}
	public boolean getState() {return state;}
	public void setType(int type) {this.type = type;}
	public int getType() {return type;}
	public void setElectionManager(RelayElectionManager electionManager) {this.electionManager = electionManager;}
	public RelayElectionManager getElectionManager() {return electionManager;}

}
