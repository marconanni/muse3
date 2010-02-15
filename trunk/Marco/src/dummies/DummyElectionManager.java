package dummies;

import java.util.Observable;
import java.util.Observer;

import parameters.NetConfiguration;



/**
 * 
 * @author marco Nanni
 *
 *classe dummy che consente di stimolare l'electionmanger con gli eventi provenienti dall'election manager
 *
 *recupera i valori che il session Manager richiede all'electionManager (isRelay, isBigBoss, clusterAddress, cluseterHeadAddres,conntectedClusterheadAddress) 
 *dai file di configurazione
 *
 *per tenere traccia di possibli cambiamenti agli indirizzi del big boss questo valore viene memorizzato in una variabile settabile.
 *
 */


public class DummyElectionManager extends Observable {
	
	public String connectedClusterHeadAddress ;
	public String clusterHeadAddress;
	public String localClusterAddress;
	
	public boolean isRelay;
	public boolean isBigBoss;
	
	public Observer sessionManager;
	
	
	
	/**
	 * Dopo aver chiamato questo costruttore
	 * ricordarsi di richiamare il metodo SetSessionManager per aggiungerlo come observer
	 * @param connectedClusterHeadAddress l'indirizzo di chi eroga i flussi nella rete superiore ( big boss o server)
	 * @param clusterHeadAddress l'indirizzo che il relay ha sulla rete superiore
	 * @param localClusterAddress l'indirizzo che il relay ha sulla rete inferiore
	 * @param isRelay vero se il nodo è un relay secondario
	 * @param isBigBoss vero se il nodo è un big boss
	 * 
	 * is relay e is big boss non devono essere entrambe vere, per un big boss is bigboss= true, is relay = false;
	 */
	
	public DummyElectionManager(String connectedClusterHeadAddress,
			String clusterHeadAddress, String localClusterAddress,
			boolean isRelay, boolean isBigBoss) {
		super();
		this.connectedClusterHeadAddress = connectedClusterHeadAddress;
		this.clusterHeadAddress = clusterHeadAddress;
		this.localClusterAddress = localClusterAddress;
		this.isRelay = isRelay;
		this.isBigBoss = isBigBoss;
		
	}
	
	/**
	 * Costruttore vuoto che carica i valori dal file di parametri.
	 * @throws Exception se i parametri sono messi male
	 */
	public DummyElectionManager() throws Exception{
		
		this.connectedClusterHeadAddress = null;
		this.clusterHeadAddress = NetConfiguration.RELAY_CLUSTER_HEAD_ADDRESS;
		this.localClusterAddress = NetConfiguration.RELAY_CLUSTER_ADDRESS;
		this.isRelay = NetConfiguration.IMRELAY;
		this.isBigBoss = NetConfiguration.IMBIGBOSS;
		
		if (isBigBoss&& isRelay){
			throw new Exception("isRelay e Is bigBoss entrambi a true: configurazione non ammessa,o è un big boss o un relay secondario. Per maggiori ingo leggere i commenti nella classe a riguardo");
		}
		
		if (isBigBoss)
			this.connectedClusterHeadAddress= NetConfiguration.SERVER_ADDRESS;
		else if(isRelay)
			this.connectedClusterHeadAddress = NetConfiguration.BIGBOSS_AD_HOC_ADDRESS;
		
		
	}
	
	
	
	/**
	 * Metodo per simulare l'arrivo di un election done :
	 * fa ricevere al sessionManager un evento del tipo 
	 *  NEW_RELAY:ip del vincitore dell'elezione:ip del vecchio relay sul suo cluster:ip del vecchio relay sul cluster superiore:ip del nodo che eroga ilflusso al vecchio relay( e che lo erogherò anche al nuovo)

	 * @param indirizzoInferioreVincitore l'idirizzo del vicitore nel suo cluster
	 * @param indirizzoInferioreVecchioRelay l'indirizzo del veccho relay nel suo cluster
	 * @param indirizzoSuperioreVecchioRelay l'indirizzo del vecchio relay sulla rete superiore
	 * @param connectedClusterHeadAddress l'indirizzo del nodo di riferimento della rete superiore.
	 */
	public void throwNewRelay(String indirizzoInferioreVincitore, String indirizzoInferioreVecchioRelay, String indirizzoSuperioreVecchioRelay, String connectedClusterHeadAddress  ){
		String event = "NEWRELAY:"+indirizzoInferioreVincitore+":"+indirizzoInferioreVecchioRelay+":"+indirizzoSuperioreVecchioRelay+":"+connectedClusterHeadAddress;
		this.setChanged();
		this.notifyObservers(event);
		System.out.println("DummyElectionManager: laciato evento " +event);
		
	}
	
	
	/**
	 * Metodo per simularel'arrivodi un election request:
	 * fa ricevere al sessionManager un evento striga
	 * 
	 * ELECTION_REQUEST_RECEIVED
	 * 
	 */
	public void throwElectionRequestReceived(){		
		String event = "ELECTION_REQUEST_RECEIVED";
		this.setChanged();
		this.notifyObservers(event);
		System.out.println("DummyElectionManager: laciato evento " +event);
		
	}
	
	
	//Getters - Setters//;

	public String getConnectedClusterHeadAddress() {
		return connectedClusterHeadAddress;
	}

	public void setConnectedClusterHeadAddress(String connectedClusterHeadAddress) {
		this.connectedClusterHeadAddress = connectedClusterHeadAddress;
	}

	public String getClusterHeadAddress() {
		return clusterHeadAddress;
	}

	public void setClusterHeadAddress(String clusterHeadAddress) {
		this.clusterHeadAddress = clusterHeadAddress;
	}

	public String getLocalClusterAddress() {
		return localClusterAddress;
	}

	public void setLocalClusterAddress(String localClusterAddress) {
		this.localClusterAddress = localClusterAddress;
	}

	public boolean isRelay() {
		return isRelay;
	}

	public void setRelay(boolean isRelay) {
		this.isRelay = isRelay;
	}

	public boolean isBigBoss() {
		return isBigBoss;
	}

	public void setBigBoss(boolean isBigBoss) {
		this.isBigBoss = isBigBoss;
	}

	public Observer getSessionManager() {
		return sessionManager;
	}
	
	/**
	 * Metodo da chiamare dopo la creazione di session ed election manager per inserire 
	 * il sessionManager dentro l'electionManager
	 * ATTENZIONE: se si usa questo metodo per cambiare il sessionManager occorre
	 * prima rimuovere il vecchioSessionManager con il metodo removeObserver
	 * @param sessionManager il sessionManager che questo finto electionManager deve notificare
	 */

	public void setSessionManager(Observer sessionManager) {
		this.sessionManager = sessionManager;
		this.addObserver(sessionManager);
	}
	
	
	
	
	

}
