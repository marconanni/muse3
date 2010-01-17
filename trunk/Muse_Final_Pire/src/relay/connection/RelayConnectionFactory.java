package relay.connection;

import java.util.Observer;

/**Classe statica che permette ai vari componenti del sistema di ottenere l'opportuno ConnectionManager 
 * @author Luca Campeti	(modificato da Pire Dejaco)
 */
public class RelayConnectionFactory {

	private static RelayPortMapper rpm = RelayPortMapper.getInstance();

	/**Metoto statico per ottenere un'istanza di RelayClusterElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayElectionCM
	 * @return un istanza di RelayElectionCM*/
	public static RelayCM getClusterElectionConnectionManager(Observer obser){
		return new RelayCM("RelayElectionCM Cluster",rpm.getLocalClusterAdHocAddress(),rpm.getLocalClusterAdHocBCASTAddress(),rpm.getPortInAdHocElection(),rpm.getPortOutAdHocElection(),obser);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayClusterHeadElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayElectionCM
	 * @return un istanza di RelayElectionCM*/
	public static RelayCM getClusterHeadElectionConnectionManager(Observer obser){
		return new RelayCM("RelayElectionCM Cluster Head",rpm.getLocalClusterHeadAdHocAddress(),rpm.getLocalClusterHeadAdHocBCASTAddress(),rpm.getPortInAdHocElection(),rpm.getPortOutAdHocElection(),obser);
	}

	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getRSSIClusterConnectionManager(Observer obser){
		return new RelayCM("RelayRSSICM",rpm.getLocalClusterAdHocAddress(),rpm.getLocalClusterAdHocBCASTAddress(),rpm.getPortInRSSI(),rpm.getPortOutRSSI(),obser);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getRSSIClusterHeadConnectionManager(Observer obser){
		return new RelayCM("RelayRSSICM",rpm.getLocalClusterHeadAdHocAddress(),rpm.getLocalClusterHeadAdHocBCASTAddress(),rpm.getPortInRSSI(),rpm.getPortOutRSSI(),obser);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getWhoIsRelayConnectionManager(Observer obser){
		return new RelayCM("WhoIsRelayConnetcionManager",rpm.getLocalClusterAdHocAddress(),rpm.getLocalClusterAdHocBCASTAddress(),rpm.getPortInWhoIsRelay(),rpm.getPortOutWhoIsRelay(),obser);
	}
}

	
	/**Metoto statico per ottenere un'istanza di ProxyCM quando il Proxy viene creato ex-novo
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ProxyCM
	 * @return un istanza di ProxyCM
	 */
//	public static ProxyCM getProxyConnectionManager(Observer obser){
//		return new ProxyCM(false, rpm.getLocalAdHocHostAddress().getHostAddress(), rpm.getFirstFreeControlAdHocInPort(),rpm.getFirstFreeControlAdHocOutPort(),rpm.getLocalManagedHostAddress().getHostAddress(), rpm.getFirstFreeControlManagedOutPort(), obser);
//	}
	
	/**Metoto statico per ottenere un'istanza di ProxyCM quando il Proxy viene creato per accogliere una sessione RTP esistente
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ProxyCM
	 * @return un istanza di ProxyCM
	 */
//	public static ProxyCM getProxyConnectionManager(Observer obser, int oldProxyCtrlPortIn){
//		rpm.setRangeAdHocPortInControlProxy(oldProxyCtrlPortIn);
//		return new ProxyCM(true, rpm.getLocalAdHocHostAddress().getHostAddress(), oldProxyCtrlPortIn ,rpm.getFirstFreeControlAdHocOutPort(),rpm.getLocalManagedHostAddress().getHostAddress(), rpm.getFirstFreeControlManagedOutPort(), obser);
//	}
	/**Metoto statico per ottenere un'istanza di RelaySessionCM 
	 * possibilità di ricevere e mandare messaggi sulla rete ad hoc e mandare messaggi sulla rete managed 
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelaySessionCM
	 * @return un istanza di RelaySessionCM
	 */
//	public static RelaySessionCM getSessionConnectionManager(Observer obser){
//		return new RelaySessionCM("RelaySessionCM",rpm.getLocalAdHocHostAddress().getHostAddress(),rpm.getPortInAdHocSession(),rpm.getPortOutAdHocSession(),rpm.getLocalManagedHostAddress().getHostAddress(), rpm.getPortOutManagedSession(),obser);
//	}