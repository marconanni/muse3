package client.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.MessageCodeConfiguration;

/**
 * Utility per la costruzione di messaggi di controllo (pacchetti UDP) lato client
 * 
 * @author Carlo Di Fulco, Dejaco Pire
 * @version 1.1
 */
public class ClientMessageFactory {

	/**
	 * Messaggio di who is relay
	 * <br/>
	 * @param InetAddress addr
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */

	public static DatagramPacket buildWhoIsRelay(InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.WHO_IS_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio di acknolege, conferma della connessione al nodo o BigBoss o relay attivo
	 * @category electionManager
	 * @param destination Address
	 * @param destination Port
	 * @param node type (client, relay attivo, relay passivo, BigBoss passivo)
	 * @return DatagramPacket da inviare
	 * @throws IOException
	 */
	static public DatagramPacket buildAckConnection(InetAddress addr, int port, int typeNode) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.ACK_CONNECTION+"_"+typeNode;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio di Election_Beacon in caso di elezione di un nuovo nodo (big boss o relay attivo)
	 * @category electionManager
	 * @param destination Address
	 * @param destination Port
	 * @param relayAddress indirizzo ip del nodo da sostituire
	 * @return DatagramPacket da inviare 
	 * @throws IOException
	 */
	public static DatagramPacket buildElectioBeacon(int sequenceNumber, InetAddress addr, int port, String relayAddress) throws IOException{
		
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_BEACON+"_"+relayAddress;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Mesaggio di Emergency Election Detected
	 * <br/>
	 * @param int sequenceNumber
	 * @param InetAddress addr
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildEmElDetClient(int sequenceNumber, InetAddress addr, int port) throws IOException{
		
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.EM_EL_DET_CLIENT;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Mesaggio di Request File
	 * <br/>
	 * @param int sequenceNumber
	 * @param String filename
	 * @param int clientStreamingInPort
	 * @param InetAdress relayAddress
	 * @param int relayControlPort
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildRequestFile(int sequenceNumber, String filename, int clientStreamingInPort, InetAddress relayAddress, int relayControlPort) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_FILE+"_"+filename+"_"+clientStreamingInPort;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, relayAddress, relayControlPort);
	}
	
	/**
	 * Messaggio Start TX 
	 * <br/>
	 * @param int sequenceNumber
	 * @param InetAddress addr
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildStartTX(int sequenceNumber, InetAddress addr, int port) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.START_TX;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio Stop TX 
	 * <br/>
	 * @param int sequenceNumber
	 * @param InetAddress addr
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildStopTX(int sequenceNumber, InetAddress addr, int port) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.STOP_TX;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio Notify RSSI
	 * <br/>
	 * @param int sequenceNumber
	 * @param int RSSIvalue
	 * @param InetAddress addr
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildNotifyRSSI(int sequenceNumber , int RSSIvalue, InetAddress addr, int port, int nodeType, int activeClient) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.NOTIFY_RSSI+"_"+RSSIvalue+"_"+nodeType+"_"+activeClient;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
}