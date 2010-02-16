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

		//sessionManager = RelaySessionManager.getInstance();
		electionManager = RelayElectionManager.getInstance(type, state);//, sessionManager);
		
		//Valerio:qui dentro scrivo a mano i tre indirizzi che mi servono per un relay
		//electionManager.setLocalClusterAddress(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		//electionManager.setLocalClusterHeadAddress(NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS);
		/*
		 * se il relay è il big boss l'indirizzo di chi gli è sopra è
		 * quello del server, altrimenti è unrelay secondario e 
		 * l'indirizzo di chi gli è sopra è quello del big boss.
		 */
//		if (parameters.NetConfiguration.IMBIGBOSS)
//			electionManager.setConnectedClusterHeadAddress(parameters.NetConfiguration.SERVER_ADDRESS);
//		else	
//			electionManager.setConnectedClusterHeadAddress(parameters.NetConfiguration.BIGBOSS_AD_HOC_ADDRESS);
//		
//		sessionManager.setElectionManager(electionManager);
		
	}

	public void setState(boolean state) {this.state = state;}
	public boolean getState() {return state;}
	public void setType(int type) {this.type = type;}
	public int getType() {return type;}
	public void setElectionManager(RelayElectionManager electionManager) {this.electionManager = electionManager;}
	public RelayElectionManager getElectionManager() {return electionManager;}

}
