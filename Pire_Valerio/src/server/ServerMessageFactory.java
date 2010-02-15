package server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.MessageCodeConfiguration;


public class ServerMessageFactory {

	
	/**
	 * Messaggio AckRelayForw
	 * @param sequenceNumber
	 * @param addr indirizzo verso il quale inviare il messaggio di ack
	 * @param port porta di ricezione del megasggio di ack
	 * @return
	 * @throws IOException
	 */
/*
	static protected DatagramPacket buildAckRelayForw(int sequenceNumber, int trasmissionPort, int trasmissionCtrlPort, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ACK_RELAY_FORW+"_"+trasmissionPort+"_"+trasmissionCtrlPort;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);
	}
*/	
	  /**
	   * Metodo per creare un pacchetto di risposta con la lista dei files
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildFilesListMessage(int sequenceNumber, InetAddress addr, int port, String files) throws IOException {
		    ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+MessageCodeConfiguration.LIST_RESPONSE+"_"+files+"_"+ServerMessageReader.getClientAddress();
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);	    
	  }
	
	  
	  static protected DatagramPacket buildForwardFilesListMessage(int sequenceNumber, InetAddress addr, int port, String relayAddress, String clientAddress, String files) throws IOException {
		    ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		    DataOutputStream doStream = new DataOutputStream(boStream);
		    String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_LIST_RESPONSE+"_"+relayAddress+"_"+clientAddress+"_"+files;//+"_"+relayAddress+"_"+relayPort+"_"+clientAddress+"_"+clientPort;
		    doStream.writeUTF(content);
		    doStream.flush();
		    byte[] data = boStream.toByteArray();
		    
		    return new DatagramPacket(data, data.length, addr, port);	    
		  }
	  /**
	   * Metodo per creare un pacchetto per segnalare al client la conferma
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	 /*
	  static protected DatagramPacket buildConfirmRequest(int sequenceNum, InetAddress addr, int port, int serverPort) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNum+"_"+Parameters.ACK_REQUEST_FILE+"_"+serverPort;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    			System.out.println("Parametri del messaggio confirmRequest: sequenceNum: "+sequenceNum+" indirizzo client: "+addr+" porta client: "+port+" porta server: "+serverPort);
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
	  */
//	  static protected DatagramPacket buildConfirmRequest(int sequenceNum, InetAddress addr, int port, String clientAddress, int clientControlPort, int clientStreamingPort, int serverStreamingPort, int serverStreamingControlPort) throws IOException {
	  static protected DatagramPacket buildConfirmRequest(int sequenceNum, InetAddress addr, int port, String clientAddress, int clientStreamingPort, int serverStreamingPort, int serverStreamingControlPort) throws IOException {
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		    DataOutputStream doStream = new DataOutputStream(boStream);
//		    String content = sequenceNum+"_"+MessageCodeConfiguration.ACK_REQUEST_FILE+"_"+clientAddress+"_"+clientControlPort+"_"+clientStreamingPort+"_"+serverStreamingControlPort+"_"+serverStreamingPort;
		    String content = sequenceNum+"_"+MessageCodeConfiguration.ACK_REQUEST_FILE+"_"+clientAddress+"_"+clientStreamingPort+"_"+serverStreamingControlPort+"_"+serverStreamingPort;
		    doStream.writeUTF(content);
		    doStream.flush();
		    byte[] data = boStream.toByteArray();
		    //System.out.println("Parametri del messaggio confirmRequest: sequenceNum: "+sequenceNum+" indirizzo client: "+addr+" porta client: "+port);
		    return new DatagramPacket(data, data.length, addr, port);
		    
		  }
	  
//	  static protected DatagramPacket buildForwardConfirmRequest(int sequenceNum, InetAddress addr, int port, String relayAddress, int relayControlPort, int relayStreamPort,String clientAddress, int clientControlPort, int clientStreamPort,int serverStreamingPort, int serverStreamingControlPort) throws IOException {
	  static protected DatagramPacket buildForwardConfirmRequest(int sequenceNum, InetAddress addr, int port, String relayAddress, int relayControlPort, int relayStreamPort,String clientAddress, int clientStreamPort,int serverStreamingPort, int serverStreamingControlPort) throws IOException {
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		    DataOutputStream doStream = new DataOutputStream(boStream);
//		    String content = sequenceNum+"_"+MessageCodeConfiguration.FORWARD_ACK_REQ+"_"+clientAddress+"_"+clientControlPort+"_"+clientStreamPort+"_"+relayAddress+"_"+relayControlPort+"_"+relayStreamPort+"_"+serverStreamingControlPort+"_"+serverStreamingPort;
		    String content = sequenceNum+"_"+MessageCodeConfiguration.FORWARD_ACK_REQ+"_"+clientAddress+"_"+clientStreamPort+"_"+relayAddress+"_"+relayControlPort+"_"+relayStreamPort+"_"+serverStreamingControlPort+"_"+serverStreamingPort;
			doStream.writeUTF(content);
		    doStream.flush();
		    byte[] data = boStream.toByteArray();
		    return new DatagramPacket(data, data.length, addr, port);
		    
		  }
}
