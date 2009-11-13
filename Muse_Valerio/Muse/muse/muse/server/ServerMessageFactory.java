package muse.server;
/**
 * ServerMessageFactory.java
 * Utility per la costruzione dei messaggi lato server
 * @author Ambra Montecchia
 * @version 1.0
 * */
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.Parameters;

public class ServerMessageFactory {

	
	/**
	   * Metodo per creare un pacchetto per segnalare al client l'inizio del playback
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildConfirmRequest(int sequenceNum, InetAddress addr, int port, int serverPort) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNum+"_"+Parameters.CONFIRM_REQUEST+"_"+serverPort;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    			System.out.println("Parametri del messaggio confirmRequest: sequenceNum: "+sequenceNum+" indirizzo client: "+addr+" porta client: "+port+" porta server: "+serverPort);
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
	  
	  /**
	   * Metodo per creare un pacchetto per segnalare al client l'inizio del playback
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildErrorMessage(int sequenceNum, InetAddress addr, int port) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNum+"_"+Parameters.ERROR;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
	  
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
	    String content = sequenceNumber+"_"+Parameters.FILES_RESPONSE+"_"+files;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);	    
	  }
	  
}
