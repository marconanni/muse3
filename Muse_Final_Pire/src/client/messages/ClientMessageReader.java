/**
 * 
 */
package client.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.StringTokenizer;

import parameters.MessageCodeConfiguration;


/**
 * @author Carlo Di Fulco, Dejaco Pire
 * @version 1.1
 */
public class ClientMessageReader {
	private boolean debug = true;

	
	private  String message = ""; 				//Stringa contenente il messaggio
	private  int sequenceNumber = 0;			//numero di sequenza del messaggio
	private  int code = 0;						//Codice messaggio
	private  int relaySendingPort;				//porta da cui il relay invia lo streaming
	private  int relayControlPort;				//porta su cui il relay ascolta le richieste
	
	private String newRelayLocalClusterAddress;
	private String oldRelayLocalClusterAddress;
	private String oldRelayLocalClusterHeadAddress;
	private String headNodeAddress;
	
	private String relayAddressBacon = null;
	private InetAddress packetAddress = null;
	public ClientMessageReader(){}
	
	/**
	 * Legge il contenuto di un mesaggio settano le ariabili interne dlla classe.
	 * Il contenuto del messaggio � reperibile tramit i metodi getter.
	 * @param dp
	 * @throws IOException
	 */
	public synchronized void readContent(DatagramPacket dp) throws IOException {
		packetAddress = dp.getAddress();
		ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		DataInputStream diStream = new DataInputStream(biStream);
		message = diStream.readUTF();
		if(debug)System.out.println("Messaggio ricevuto: "+message);
		readMessage();
	}
	/**
	 * Metodo che consente di leggere i campi di un messaggio
	 * */
	 private void readMessage(){

		sequenceNumber = -1; 
		code = -1; 

		StringTokenizer st = new StringTokenizer(message, "_");
		String c = st.nextToken();
		sequenceNumber = Integer.parseInt(c);
		c = st.nextToken();
		code = Integer.parseInt(c);
		if (code == MessageCodeConfiguration.ACK_CLIENT_REQ){
			c = st.nextToken(); 
			relaySendingPort = Integer.parseInt(c);
			c = st.nextToken(); 
			relayControlPort = Integer.parseInt(c);
		}
		//if(code == MessageCodeConfiguration.NOTIFY_RSSI)RSSI = Double.parseDouble(st.nextToken());		
		//if (code == MessageCodeConfiguration.ELECTION_BEACON)relayAddressBacon = st.nextToken();
		if (code == MessageCodeConfiguration.ELECTION_DONE){
			newRelayLocalClusterAddress = st.nextToken();
			oldRelayLocalClusterAddress = st.nextToken();
			oldRelayLocalClusterHeadAddress = st.nextToken();
			headNodeAddress = st.nextToken();
		}
	}

	public String getNewRelayLocalClusterAddress(){return newRelayLocalClusterAddress;}
	public String getHeadNodeAddress(){ return headNodeAddress;}
	public String getOldRelayLocalClusterAddress(){return oldRelayLocalClusterAddress;}
	public String getOldRelayLocalClusterHeadAddress(){return oldRelayLocalClusterHeadAddress;}
	public  int getSequenceNumber() {return sequenceNumber;}
	public  int getCode() {return code;}
	public  int getRelaySendingPort() {return relaySendingPort;}
	public  int getRelayControlPort() {	return relayControlPort;}
	public String getRelayAddressBacon(){return relayAddressBacon;}
	public InetAddress getPacketAddress(){return packetAddress;}
}