package muse.client;

/**
 * Utility per la costruzione di messaggi di controllo (pacchetti UDP) lato client
 * @author Ambra Montecchia
 * @version 1.2
 * */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.Parameters;

public class ClientMessageFactory {

	  /**
	   * Metodo per creare un pacchetto di richiesta attivazione proxy
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @param serverIP - indirizzo IP del server
	   * @param serverPort - porta su cui � in ascolto il server
	   * @param ctrlPort - porta di controllo client
	   * @param recPort - porta di ricezione del client
	   * @param bufferSize - dimensione del buffer lato client
	   * @param filename - nome del file richiesto
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildActivationRequest(int sequenceNumber, InetAddress addr, int port, String serverAddr, int serverPort, int ctrlPort, int recPort, int bufferSize, String filename) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.ACTIVATE+"_"+serverAddr+"_"+serverPort+"_"+ctrlPort+"_"+recPort+"_"+bufferSize+"_"+filename;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
	  
	  /**
	   * Metodo per creare un messaggio per indicare al proxy il valore della banda stimata
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @param band - valore stimato di banda
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildBandEstimated(int sequenceNumber, InetAddress addr, int port, double band) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.BAND_ESTIMATION+"_"+band;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	  }
	  
//	  /**
//	   * Metodo per creare un messaggio per segnalare al proxy una situazione di buffer pieno
//	   * @param sequenceNumber - numero di sequenza del messaggio
//	   * @param addr - indirizzo del destinatario
//	   * @param port - porta del destinatario
//	   * @return - il datagramma contenente il messaggio
//	   * @throws IOException
//	   */
//	  static protected DatagramPacket buildBufferFullMessage(int sequenceNumber, InetAddress addr, int port) throws IOException {
//	    
//		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	    DataOutputStream doStream = new DataOutputStream(boStream);
//	    String content = sequenceNumber+"_"+Parameters.BUFFER_FULL;
//	    doStream.writeUTF(content);
//	    doStream.flush();
//	    byte[] data = boStream.toByteArray();
//	    
//	    return new DatagramPacket(data, data.length, addr, port);
//	  }
	  
	  /**
	   * Metodo per creare un messaggio per segnalare al proxy una situazione di handoff
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @param numFrames - numero di frame nel buffer lato client
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildHandoffMessage(int sequenceNumber, InetAddress addr, int port, int numFrames) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.HANDOFF+"_"+numFrames;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	  }
	  
	  /**
	   * Metodo per creare un messaggio per segnalare al proxy una situazione di low throughput
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @param numFrames - numero di frame nel buffer lato client
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildLTMessage(int sequenceNumber, InetAddress addr, int port, int numFrames) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.LOW_THROUGHPUT+"_"+numFrames;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	  }
	  
	  /**
	   * Metodo per creare un messaggio di start on, utilizzato dal client per segnalare la ripresa della trasmissione
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @param lastFrameSeq - numero di sequenza dell'ultimo frame ricevuto nella sequenza corretta
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildStartOnMessage(int sequenceNumber, InetAddress addr, int port, long lastFrameSeq) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.START_ON+"_"+lastFrameSeq;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    System.out.println("Costruito messaggio "+content+" per "+addr.getHostName()+" porta "+port);
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
	  
//	  /**
//	   * Metodo per creare un messaggio di acknowledge
//	   * @param sequenceNumber - numero di sequenza del messaggio
//	   * @param addr - indirizzo del destinatario
//	   * @param port - porta del destinatario
//	   * @return - il datagramma contenente il messaggio
//	   * @throws IOException
//	   */
//	  static protected DatagramPacket buildAckMessage(int sequenceNumber, InetAddress addr, int port) throws IOException {
//	    
//		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	    DataOutputStream doStream = new DataOutputStream(boStream);
//	    String content = sequenceNumber+"_"+Parameters.ACK;
//	    doStream.writeUTF(content);
//	    doStream.flush();
//	    byte[] data = boStream.toByteArray();
//	    
//	    return new DatagramPacket(data, data.length, addr, port);
//	    
//	  }
	  
//	  /**
//	   * Metodo per creare un pacchetto di richiesta attivazione proxy
//	   * @param sequenceNumber - numero di sequenza del messaggio
//	   * @param addr - indirizzo del destinatario
//	   * @param port - porta del destinatario
//	   * @return - il datagramma contenente il messaggio
//	   * @throws IOException
//	   */
//	  static protected DatagramPacket buildErrorMessage(int sequenceNumber, InetAddress addr, int port) throws IOException {
//	    
//		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	    DataOutputStream doStream = new DataOutputStream(boStream);
//	    String content = sequenceNumber+"_"+Parameters.ERROR;
//	    doStream.writeUTF(content);
//	    doStream.flush();
//	    byte[] data = boStream.toByteArray();
//	    
//	    return new DatagramPacket(data, data.length, addr, port);
//	    
//	  }
	  
	  /**
	   * Metodo per creare un pacchetto di richiesta list files
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static public DatagramPacket buildFilesReqMessage(int sequenceNumber, InetAddress addr, int port) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.FILES_REQ;
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	  }
	  
	  static protected DatagramPacket buildNoParamMessage(int sequenceNumber, InetAddress addr, int port, int message) throws IOException {
		    
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		    DataOutputStream doStream = new DataOutputStream(boStream);
		    String content = sequenceNumber+"_"+message;
		    doStream.writeUTF(content);
		    doStream.flush();
		    byte[] data = boStream.toByteArray();
		    
		    return new DatagramPacket(data, data.length, addr, port);
	  }
////////////VALERIO	 
	  //metodo preso dal messagefactory del proxy
	  //ho aggiunto la receivingPort su cui vuole ricevere il client
	  /**
	   * Metodo per creare un pacchetto di richiesta attivazione proxy
	   * @param sequenceNumber - numero di sequenza del messaggio
	   * @param addr - indirizzo del destinatario
	   * @param port - porta del destinatario
	   * @return - il datagramma contenente il messaggio
	   * @throws IOException
	   */
	  static protected DatagramPacket buildFileRequest(int sequenceNumber, InetAddress addr, int port, String filename, int receivingPort) throws IOException {
	    
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	    DataOutputStream doStream = new DataOutputStream(boStream);
	    String content = sequenceNumber+"_"+Parameters.FILE_REQUEST+"_"+receivingPort+"_"+filename;
	    System.out.println("Request message: "+content);
	    doStream.writeUTF(content);
	    doStream.flush();
	    byte[] data = boStream.toByteArray();
	    
	    return new DatagramPacket(data, data.length, addr, port);
	    
	  }
}
