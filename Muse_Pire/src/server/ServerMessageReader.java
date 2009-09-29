package server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.StringTokenizer;

import parameters.Parameters;

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
	
	/**
	 * Legge il contenuto di un mesaggio settano le variabili interne della classe.
	 * Il contenuto del messaggio � reperibile tramite i metodi getter.
	 * <br/>
	 * @param dp
	 * @throws IOException
	 */
	static protected void readContent(DatagramPacket dp) throws IOException {
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
		if (code == Parameters.FORWARD_REQ_FILE){
			clientAddress = st.nextToken();
			fileName = st.nextToken();
			proxyControlPort = Integer.parseInt(st.nextToken());
			proxyReceivingStreamPort = Integer.parseInt(st.nextToken());
		}
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



	
}
