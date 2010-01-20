package server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import parameters.Parameters;

public class ServerPortMapper {

	//PER GLI STREAMINGSERVER
	/*range di porte assegnabili ad uno STREAMINGSERVER per la ricezione 
	 * di messaggi START_TX e STOP_TX dal PROXY e per l'invio del messaggio ACK_RELAY_FORW al PROXY*/
	private boolean[] rangePortInOutControlStreamingServer = new boolean[Parameters.RANGE_ACTIVE_SESSIONS]; 
	
	/*range di porte assegnabili ad uno STREAMINGSERVER per l'invio del flusso RTP al PROXY*/
	private boolean[] rangePortOutRTPStreamingServer = new boolean[Parameters.RANGE_ACTIVE_SESSIONS];

	
	//PER IL SERVER
	/*porta della socket su cui il SERVER riceve i messaggi di REDIRECT da parte del RELAY 
	 * e di FORWARD_REQ_FILE da parte del PROXY */
	private int portInSession;
	
	/*indirizzo locale del server*/
	private InetAddress localAddress = null;
	

	public static final ServerPortMapper INSTANCE = new ServerPortMapper();  

	private ServerPortMapper()
	{
		for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
			rangePortInOutControlStreamingServer[i] = true;
			rangePortOutRTPStreamingServer[i] = true;
		}

		portInSession = Parameters.SERVER_SESSION_PORT_IN;
		
		try {
			localAddress = InetAddress.getByName(Parameters.SERVER_ADDRESS);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**Metodo per ottenere il riferimento all'unico ServerPortMapper attivo sul nodo CLIENT
	 * @return 
	 */
	public static ServerPortMapper getInstance() {
		return ServerPortMapper.INSTANCE;
	}



	private int assignPort(String typePort)
	{
		if(typePort.equals("Control"))
		{
			for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
				if(rangePortInOutControlStreamingServer[i]){
					rangePortInOutControlStreamingServer[i] = false;
					return i + Parameters.STREAMINGSERVER_INITIAL_PORT_IN_OUT_CONTROL;
				}
			}
		}
		if(typePort.equals("Stream"))
		{
			for(int i = 0;i < Parameters.RANGE_ACTIVE_SESSIONS;i++){
				if(rangePortOutRTPStreamingServer[i]){
					rangePortOutRTPStreamingServer[i] = false;
					rangePortOutRTPStreamingServer[i+1] = false;
					return i + Parameters.STREAMINGSERVER_INITIAL_PORT_OUT_RTP;
				}
			}
		}
		return -1;
	}

	/**Metodo per ottenere una porta libera da assegnare allo STREAMINGSERVER per 
	 * inviare ACK_RELAY_FORW al PROXY e, in seguito, da esso ricevere i messaggi di START_TX e STOP_TX
	 * @return un intero rappresentante la porta appena occupata
	 */
	public int getFirstFreeControlPort()
	{
		return this.assignPort("Control");
	}

	/**Metodo per ottenere una porta libera da assegnare allo STREAMINGSERVER per 
	 * inviare lo stream RTP al PROXY
	 * @return un intero rappresentante la porta appena occupata
	 */
	public int getFirstFreeRTPPort()
	{
		return this.assignPort("Stream");
	}

	/**Metodo per ottenere l'indirizzo del nodo locale
	 * @return un InetAddress rappresentante l'indirizzo di cui sopra 
	 */
	public InetAddress getLocalHostAddress(){
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	

	/**Metodo per conoscere la situazione delle porte occupate attualmente 
	 * @return un array di boolean -> true = porta libera, false = porta occupata
	 */
	public boolean[] getRangePortInOutControlStreamingServer() {
		return rangePortInOutControlStreamingServer;
	}


	/**Metodo per ottenere la porta su cui il SERVER riceve i messaggi
	 * dal RELAY e dai PROXY
	 * @return un intero rappresentante la porta di cui sopra 
	 */
	public int getPortInSession() {
		return portInSession;
	}

	
	/**
	 * CODICE DI VERIFICA DEL PORTMAPPER DEL RELAY
	 * @param args
	 */
	/*public static void main(String[] args) {
		// TODO Auto-generated method stub

		ServerPortMapper portMapper = ServerPortMapper.getInstance();

		System.out.println("######### SESSIONE 1 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 2 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 3 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 4 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 5 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 6 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 7 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("######### SESSIONE 8 ########");
		System.out.println("PortaRTP: "+ portMapper.getFirstFreeRTPPort());
		System.out.println("PortaControllo: "+portMapper.getFirstFreeControlPort());
		System.out.println("PortaStandardRequestRelay: "+ portMapper.getPortServerIn());
		System.out.println("PortaStandardReceiveRelay: "+ portMapper.getPortRelayOut());
	}
*/

}