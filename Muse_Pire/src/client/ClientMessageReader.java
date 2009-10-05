/**
 * 
 */
package client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.StringTokenizer;

import parameters.Parameters;

/**
 * @author Carlo Di Fulco
 *
 */
public class ClientMessageReader {
	private boolean debug = false;

	//Stringa contenente il messaggio
	private  String message = ""; 
	//numero di sequenza del messaggio
	private  int sequenceNumber = 0;
	//Codice messaggio
	private  int code = 0;
	//porta da cui il relay invia lo streaming
	private  int relaySendingPort;
	//porta su cui il relay ascolta le richieste
	private  int relayControlPort;
	//indirizzo del relay attuale
	
	//MODIFICATO DA LUCA 8-12 alle 01:53
	private  String actualRelayAddress = null;
	
	//AGGIUNTO DA LUCA 8-12 alle 01:53
	private  String newRelayAddress = null;

	public ClientMessageReader(){}
	/**
	 * Legge il contenuto di un mesaggio settano le ariabili interne dlla classe.
	 * Il contenuto del messaggio � reperibile tramit i metodi getter.
	 * <br/>
	 * @param dp
	 * @throws IOException
	 */
	public synchronized void readContent(DatagramPacket dp) throws IOException {
		ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		DataInputStream diStream = new DataInputStream(biStream);
		message = diStream.readUTF();
		if(debug)System.out.println("Messaggio ricevuto: "+message);
		readMessage();
	}

	//MODIFICATO DA LUCA 8-12 alle 01:53
	/**
	 * @return the actualRelayAddress
	 */
	public  String getActualRelayAddress() {
		return actualRelayAddress;
	}
	
	//AGGIUNTO DA LUCA 8-12 alle 01:53
	/**
	 * @return the newRelayAddress
	 */
	public  String getNewRelayAddress() {
		return newRelayAddress;
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
		if(debug)System.out.println("Sequence Number: " + sequenceNumber);
		c = st.nextToken();
		code = Integer.parseInt(c);
		if(debug)System.out.println("Code " + code);
		if (code == Parameters.ACK_CLIENT_REQ){
			c = st.nextToken(); 
			relaySendingPort = Integer.parseInt(c);
			c = st.nextToken(); 
			relayControlPort = Integer.parseInt(c);
		}
		if (code == Parameters.IM_RELAY){
			actualRelayAddress = st.nextToken(); 
		}
		if (code == Parameters.ELECTION_DONE){
			newRelayAddress = st.nextToken(); 
		}
	}

	/**
	 * @return the sequenceNumber
	 */
	public  int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @return the code
	 */
	public  int getCode() {
		return code;
	}

	/**
	 * @return the relaySendingPort
	 */
	public  int getRelaySendingPort() {
		return relaySendingPort;
	}

	/**
	 * @return the relayControlPort
	 */
	public  int getRelayControlPort() {
		return relayControlPort;
	}
}