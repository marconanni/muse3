package dummies;
import parameters.*;
import relay.connection.RelayPortMapper;

public class DummyMain {
	
	

	public static void main (String [] args){
		try {
			
			boolean oldRelay = true;
			if (oldRelay){
				
				RelayPortMapper portMapper = RelayPortMapper.getInstance();
				DummyElectionManager electionManager = new DummyElectionManager();
				DummySessionManager sessionManager = new DummySessionManager();
				electionManager.setSessionManager(sessionManager);
				sessionManager.setElectionManager(electionManager);
				// creo una sessione Fittizia!!
				
				// nota: il server può erogare più stream contemporaneamente, da porte diverse, visto che qui 
				// è tutto finto, e non c'è neanche il server, metto 3200 per la orta di streaming e 3201 per quelladi controllo, e siamo a posto.
				
				
				DummyDummyProxy proxy = new DummyDummyProxy(sessionManager, NetConfiguration.CLIENT_ADDRESS, PortConfiguration.CLIENT_PORT_RTP_IN, 3200, NetConfiguration.SERVER_ADDRESS, true)	;			
				
				DummySession sessione = new DummySession(NetConfiguration.CLIENT_ADDRESS, (DummyProxy)proxy, 3200, proxy.inStreamPort, proxy.outStreamPort, PortConfiguration.CLIENT_PORT_RTP_IN, 3201, proxy.proxyStreamingCtrlPort);				
				
				sessionManager.tAddSession(sessione);
				
				electionManager.throwNewRelay(indirizzoInferioreVincitore, indirizzoInferioreVecchioRelay, indirizzoSuperioreVecchioRelay, connectedClusterHeadAddress);
				
				
				}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	

}
