package client.connection;

import java.util.Observer;


/**Classe statica che permette ai vari componenti del sistema di ottenere l'opportuno ConnectionManager 
 * @author Luca Campeti, Dejaco Pire
 * @version 1.1
 *
 */
public class ClientConnectionFactory {

	private static ClientPortMapper cpm = ClientPortMapper.getInstance();
	
	/**Metoto statico per ottenere un'istanza di ClientElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientElectionCM
	 * @return un istanza di ClientElectionCM
	 */
	public static ClientCM getElectionConnectionManager(Observer obser, boolean bcast){
		return new ClientCM("ClientElectionCM", cpm.getLocalAddress(),cpm.getLocalBcastAddress(), cpm.getPortElectionIn(),cpm.getPortElectionOut(),obser, bcast);
	}
	/**Metoto statico per ottenere un'istanza di ClientRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientRSSICM
	 * @return un istanza di ClientRSSICM
	 */
	public static ClientCM getRSSIConnectionManager(Observer obser, boolean bcast){
		return new ClientCM("ClientRSSICM",cpm.getLocalAddress(), cpm.getLocalBcastAddress(), cpm.getPortRSSIIn(),cpm.getPortRSSIOut(),obser, bcast);
	}
	

	//VALERIO
	/**Metoto statico per ottenere un'istanza di ClientSessionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal ClientSessionCM
	 * @return un istanza di ClientSessionCM
	 */
	public static ClientCM getSessionConnectionManager(Observer obser, boolean bcast){
		return new ClientCM("ClientSessionCM",cpm.getLocalAddress(),null, cpm.getPortSessionIn(),cpm.getPortSessionOut(),obser, bcast);
	}
}