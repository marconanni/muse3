package client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.Parameters;

/**
 * Utility per la costruzione di messaggi di controllo (pacchetti UDP) lato client
 * 
 * @author Carlo Di Fulco
 *
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
		String content = 0+"_"+Parameters.WHO_IS_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio di Election_Beacon.
	 * <br/> 
	 * @param int sequenceNumber
	 * @param InetAddress addr 
	 * @param int port
	 * @return DatagramPacket 
	 * @throws IOException
	 */
	public static DatagramPacket buildElectioBeacon(int sequenceNumber, InetAddress addr, int port) throws IOException{
		
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+Parameters.ELECTION_BEACON;
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
		String content = 0+"_"+Parameters.EM_EL_DET_CLIENT;
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
		String content = sequenceNumber+"_"+Parameters.REQUEST_FILE+"_"+filename+"_"+clientStreamingInPort;
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
		String content = sequenceNumber+"_"+Parameters.START_TX;
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
		String content = sequenceNumber+"_"+Parameters.STOP_TX;
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
	public static DatagramPacket buildNotifyRSSI(int sequenceNumber , int RSSIvalue, InetAddress addr, int port) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.NOTIFY_RSSI+"_"+RSSIvalue;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
}