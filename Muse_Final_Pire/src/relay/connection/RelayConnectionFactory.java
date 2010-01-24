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
	public static RelayCM getClusterElectionConnectionManager(Observer obser, boolean bcast){
		return new RelayCM("RelayElectionCM Cluster",rpm.getLocalClusterAddress(),rpm.getLocalClusterBCASTAddress(),rpm.getPortInAdHocElection(),rpm.getPortOutAdHocElection(),obser, bcast);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayClusterHeadElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayElectionCM
	 * @return un istanza di RelayElectionCM*/
	public static RelayCM getClusterHeadElectionConnectionManager(Observer obser,boolean bcast){
		return new RelayCM("RelayElectionCM Cluster Head",rpm.getLocalClusterHeadAddress(),rpm.getLocalClusterHeadBCASTAddress(),rpm.getPortInAdHocElection(),rpm.getPortOutAdHocElection(),obser, bcast);
	}

	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getRSSIClusterConnectionManager(Observer obser,boolean bcast){
		return new RelayCM("RelayRSSICM",rpm.getLocalClusterAddress(),rpm.getLocalClusterBCASTAddress(),rpm.getPortInRSSI(),rpm.getPortOutRSSI(),obser, bcast);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getRSSIClusterHeadConnectionManager(Observer obser,boolean bcast){
		return new RelayCM("RelayRSSICM",rpm.getLocalClusterHeadAddress(),rpm.getLocalClusterHeadBCASTAddress(),rpm.getPortInRSSI(),rpm.getPortOutRSSI(),obser, bcast);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM*/
	public static RelayCM getWhoIsRelayConnectionManager(Observer obser,boolean bcast){
		return new RelayCM("WhoIsRelayConnetcionManager",rpm.getLocalClusterAddress(),rpm.getLocalClusterBCASTAddress(),rpm.getPortInWhoIsRelay(),rpm.getPortOutWhoIsRelay(),obser, bcast);
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