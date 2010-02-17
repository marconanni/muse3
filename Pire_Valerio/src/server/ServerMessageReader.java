package server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.StringTokenizer;

import parameters.MessageCodeConfiguration;

public class ServerMessageReader {

	//Stringa contenente il messaggio
	private static String message = ""; 
	//numero di sequenza del messaggio
	private static int sequenceNumber = 0;
	//Codice messaggio
	private static int code = 0;
	//porta da cui il relay invia lo streaming
	private static int relaySendingPort;
	//porta su cui il relay ascolta le richieste
	private static int relayControlPort;

	private static String clientAddress;
	private static String fileName;
	private static int proxyReceivingStreamPort;
	private static int proxyControlPort;
	
	private static int clientPort;
	private static int clientRTPPort;
	private static int relayPort;
	private static int relayStreamingInPort;
	private static String relayAddress; 


	private static int bigbossPort;
	private static int bigbossStreamingPort;
	private static int bigbossControlPort;
	private static InetAddress packetAddress;
	
	/**
	 * Legge il contenuto di un mesaggio settano le variabili interne della classe.
	 * Il contenuto del messaggio � reperibile tramite i metodi getter.
	 * <br/>
	 * @param dp
	 * @throws IOException
	 */
	static protected void readContent(DatagramPacket dp) throws IOException {
		packetAddress = dp.getAddress();
		ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		DataInputStream diStream = new DataInputStream(biStream);
		message = diStream.readUTF();
		System.out.println("Messaggio ricevuto: "+message);
		readMessage();
	}

	/**
	 * Metodo che consente di leggere i campi di un messaggio
	 * */
	static private void readMessage(){

		sequenceNumber = -1; 
		code = -1; 

		StringTokenizer st = new StringTokenizer(message, "_");
		String c = st.nextToken();
		sequenceNumber = Integer.parseInt(c);
		System.out.println("Sequence Number: " + sequenceNumber);
		c = st.nextToken();
		code = Integer.parseInt(c);
		System.out.println("Code " + code);
		
		if (code == MessageCodeConfiguration.FORWARD_REQ_FILE){
			fileName=st.nextToken();
			bigbossControlPort=Integer.parseInt(st.nextToken());
			bigbossStreamingPort=Integer.parseInt(st.nextToken());
			relayAddress=st.nextToken();
			relayControlPort=Integer.parseInt(st.nextToken());
			relayStreamingInPort=Integer.parseInt(st.nextToken());
			clientAddress=st.nextToken();
			clientRTPPort=Integer.parseInt(st.nextToken());
		}
		
		if(code==MessageCodeConfiguration.REQUEST_LIST){
			clientAddress=st.nextToken();
		}
		if(code==MessageCodeConfiguration.REQUEST_FILE){//messaggio mandato da bigboss con catena client->bigboss->server
			fileName=st.nextToken();
			bigbossControlPort=Integer.parseInt(st.nextToken());
			bigbossStreamingPort=Integer.parseInt(st.nextToken());
			clientAddress=st.nextToken();
//			clientPort=Integer.parseInt(st.nextToken());
			clientRTPPort=Integer.parseInt(st.nextToken());
		}
		if(code==MessageCodeConfiguration.FORWARD_REQ_LIST){
			relayAddress=st.nextToken();
			clientAddress=st.nextToken();
		}
	}
	
	public static InetAddress getPacketAddress(){return packetAddress;}
	
	
	
	public static int getRelayStreamingInPort() {
		return relayStreamingInPort;
	}

	public static int getBigbossControlPort() {
		return bigbossControlPort;
	}

	public static int getClientRTPPort() {
		return clientRTPPort;
	}

	public static int getBigbossStreamingPort() {
		return bigbossStreamingPort;
	}

	public static String getRelayAddress() {
		return relayAddress;
	}
	
	public static int getBigbossPort() {
		return bigbossPort;
	}

	public static int getRelayPort(){
		return relayPort;
	}

	/**
	 * @return the sequenceNumber
	 */
	public static int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @return the code
	 */
	public static int getCode() {
		return code;
	}

	/**
	 * @return the relaySendingPort
	 */
	public static int getRelaySendingPort() {
		return relaySendingPort;
	}

	/**
	 * @return the relayControlPort
	 */
	public static int getRelayControlPort() {
		return relayControlPort;
	}

	public static String getClientAddress() {
		return clientAddress;
	}

	public static String getFileName() {
		return fileName;
	}

	public static int getProxyControlPort() {
		return proxyControlPort;
	}

	public static int getProxyReceivingStreamPort() {
		return proxyReceivingStreamPort;
	}
	
	public static int getRTPClientPort(){
		return clientRTPPort;
	}
	

	
}
