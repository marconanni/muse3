package relay.connection;

import java.util.Observer;

import relay.RelayPortMapper;

/**Classe statica che permette ai vari componenti del sistema di ottenere l'opportuno ConnectionManager 
 * @author Luca Campeti	
 */
public class RelayConnectionFactory {

	private static RelayPortMapper rpm = RelayPortMapper.getInstance();

	/**Metoto statico per ottenere un'istanza di RelaySessionCM 
	 * possibilità di ricevere e mandare messaggi sulla rete ad hoc e mandare messaggi sulla rete managed 
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelaySessionCM
	 * @return un istanza di RelaySessionCM
	 */
	public static RelaySessionCM getSessionConnectionManager(Observer obser){
		return new RelaySessionCM("RelaySessionCM",rpm.getLocalAdHocHostAddress().getHostAddress(),rpm.getPortInAdHocSession(),rpm.getPortOutAdHocSession(),rpm.getLocalManagedHostAddress().getHostAddress(), rpm.getPortOutManagedSession(),obser);
	}

	/**Metoto statico per ottenere un'istanza di RelayElectionCM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayElectionCM
	 * @return un istanza di RelayElectionCM
	 */
	public static RelayCM getElectionConnectionManager(Observer obser){
		return new RelayCM("RelayElectionCM",rpm.getLocalAdHocHostAddress().getHostAddress(),rpm.getPortInAdHocElection(),rpm.getPortOutAdHocElection(),obser);
	}

	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM
	 */
	public static RelayCM getRSSIConnectionManager(Observer obser){
		return new RelayCM("RelayRSSICM",rpm.getLocalAdHocHostAddress().getHostAddress(),rpm.getPortInRSSI(),rpm.getPortOutRSSI(),obser);
	}
	
	/**Metoto statico per ottenere un'istanza di RelayRSSICM
	 * @param obser l'Observer che deve essere avvertito dell'arrivo di un messaggio dal RelayRSSICM
	 * @return un istanza di RelayRSSICM
	 */
	public static RelayCM getWhoIsRelayConnectionManager(Observer obser){
		return new RelayCM("WhoIsRelayConnetcionManager",rpm.getLocalAdHocHostAddress().getHostAddress(),rpm.getPortInWhoIsRelay(),rpm.getPortOutWhoIsRelay(),obser);
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

}



/*class TestRelayConnectionFactory{
	
	public static byte[] writeByteArray(int toSend){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(toSend);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	public static void main(String args[]){
		RelayPortMapper rpm = RelayPortMapper.getInstance();
		TestObserver to = new TestObserver();
		ProxyCM pcm = RelayConnectionFactory.getProxyConnectionManager(to, false);
		RelaySessionCM rscm = RelayConnectionFactory.getSessionConnectionManager(to);
		RelayElectionCM recm = RelayConnectionFactory.getElectionConnectionManager(to);
		to.setRelaySessionCM(rscm, pcm);
		
		System.out.println("ProxyCM: porta uscita/ingresso verso il server: " +pcm.getLocalManagedInputOutputPort());
		System.out.println("ProxyCM: porta ingresso dal client: " +pcm.getLocalAdHocInputPort());
		pcm.start();
		byte[] buffer = null;
		buffer = writeByteArray(pcm.getLocalManagedInputOutputPort());
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalManagedHostAddress(),rpm.getPortInAdHocSession() );   
		rscm.start();
		
		
		//ATTENZIONE QUESTO E' L'ODINE IMPOSTO SULL'USO DEL ProxyCM quando il secondo parametro di getProxyConnectionManager
		//è a false, ovvero quando il Proxy viene creato da 0 piuttosto che accogliere già una sessione in corso 
		//(caso di creazione dei proxy sul new relay al cambio della sessione)
		//invio forward al server
		//attesa della risposta dello streamingsever appena creato 
		//invio dell'ack al client
		//comincia attesa di messaggi START_TX STOP_TX da parte del Proxy
		pcm.sendToServer(dp);
		
		//questo non lo manda perchè ancora non riceve la risposta del server
		buffer = writeByteArray(666);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),rpm.getPortInAdHocSession());   
		pcm.sendTo(dp);
		
		pcm.waitStreamingServerResponse();
		pcm.sendTo(dp);
		pcm.start();
		
		buffer = writeByteArray(345);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),pcm.getLocalAdHocInputPort() );   
		rscm.sendTo(dp);
		
		ProxyCM pcmForChange = RelayConnectionFactory.getProxyConnectionManager(to, true);
		pcmForChange.start();
		
		int portInPCMForChange = pcm.getLocalAdHocInputPort();
		int portManagedPCMForChange = pcm.getLocalManagedInputOutputPort();
		
		buffer = writeByteArray(678);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portInPCMForChange);   
		rscm.sendTo(dp);
		
		//Non dovrebbe bloccarsi in ricezione
		pcmForChange.waitStreamingServerResponse();
		
		//Questo non lo dovrebbe ricevere perchè lo invio sulla porta da cui riceve un singolo messaggio dal server
		buffer = writeByteArray(910);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portManagedPCMForChange);   
		rscm.sendTo(dp);
		
		//Questo lo dovrebbe ricevere perchè lo invio sulla porta da cui riceve start e stop dal client 
		buffer = writeByteArray(1112);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portInPCMForChange);   
		rscm.sendTo(dp);
		
		buffer = writeByteArray(666);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),rpm.getPortInAdHocSession());   
		pcm.sendTo(dp);
		
		
	}
	
	
}

class TestObserver implements Observer{

	private RelaySessionCM rscm = null;
	private ProxyCM pcm = null;
	private RelayPortMapper rpm = RelayPortMapper.getInstance();
	
	public static byte[] writeByteArray(int toSend){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(toSend);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
	public TestObserver(){
		System.out.println("testObserver: creato");
		
	}
	
	public void setRelaySessionCM(RelaySessionCM rs, ProxyCM pc){
		rscm =rs;
		pcm = pc;
	}

	public static String convertToString(byte[] content){
		ByteArrayInputStream bais = new ByteArrayInputStream(content);
		DataInputStream dis = new DataInputStream(bais);
		int res = -1; 
		try {
			res = dis.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ""+res;
	}

	@Override
	public void update(Observable o, Object arg) {
		System.out.println("\tObserver: ricevuto datagramma da: " +  o.getClass().toString());
		DatagramPacket dp  = (DatagramPacket)arg;
		System.out.println("\tObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
		String res = TestObserver.convertToString(dp.getData());
		System.out.println("\tObserver: dati ricevuti: " +  res);

		if(res.equalsIgnoreCase("3026")){
		byte[] buffer = writeByteArray(123);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalManagedHostAddress(),pcm.getLocalManagedInputOutputPort() );   
		rscm.sendTo(dp);
		}
	}
}*/