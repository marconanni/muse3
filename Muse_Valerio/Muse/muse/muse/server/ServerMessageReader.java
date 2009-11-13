package muse.server;
/**
 * ServerMessageReader.java
 * Utility per la lettura dei messaggi ricevuti dal server
 * @author Ambra Montecchia
 * @version 1.0
 * */
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.StringTokenizer;

import parameters.Parameters;

public class ServerMessageReader {

	//Stringa contenente il messaggio
	private static String message = ""; 
	//Codice messaggio
	private static int code = 0;
	//primo parametro messaggio
	private static String firstParam = "";
	//secondo parametro messaggio
	private static String secondParam = "";
	//terzo parametro messaggio
	private static String thirdParam = "";
	//indice messaggio
	private static int index = 0;
	
	  /**
	   * Metodo che consente di leggere il contenuto di un pacchetto
	   * @param dp - datagramma di cui si vuole leggere il contenuto
	   */
	  static protected void readContent(DatagramPacket dp) throws IOException {
	    ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0,
	        dp.getLength());
	    DataInputStream diStream = new DataInputStream(biStream);
	    message = diStream.readUTF();
	    readMessage();
	  }
	  
	  /**
	   * Metodo che consente di leggere i campi di un messaggio
	   * */
	  static private void readMessage(){
		  code = 0;
		  firstParam = "";
		  secondParam = "";
		  StringTokenizer st = new StringTokenizer(message, "_");
		  String c = st.nextToken();
		  					//System.out.println("readMessage(): c="+c);
		  index = Integer.parseInt(c);
		  					//System.out.println("readMessage(): index="+index);
		  c = st.nextToken();
		  					//System.out.println("readMessage(): c="+c);
		  code = Integer.parseInt(c);
		  					//System.out.println("readMessage(): code="+code);
		  if(code == Parameters.FILE_REQUEST){
			  //mi dava errore la lettura del pacchetto dal client al server perchè io non
			  //ho la porta del proxy, quindi nel firstParam andava a finire il nome del file
			  /*
			  //il primo parametro � la porta su cui � in ascolto il proxy
			  firstParam = st.nextToken();
			  				System.out.println("readMessage(): firstParam="+firstParam);
			  */
			  //come firstParam metto la porta su cui ascolta il client:
			  firstParam = st.nextToken();
			  //il secondo parametro � l'URL del file richiesto
			  secondParam = st.nextToken();
			  				//System.out.println("readMessage(): secondParam="+secondParam);
		  }
		  					//System.out.println("FINE readMessage()");
	  }
	  
	  /**
	   * Metodo getter
	   * @return il codice del messaggio
	   * */
	  public static int getCode(){
		  return code;
	  }
	  
	  /**
	   * Metodo getter
	   * @return il primo parametro contenuto nel messsaggio
	   * */
	  public static String getFirstParam(){
		  return firstParam;
	  }
	  
	  /**
	   * Metodo getter
	   * @return il secondo parametro contenuto nel messaggio
	   * */
	  public static String getSecondParam(){
		  return secondParam;
	  }
	  
	  /**
	   * Metodo getter
	   * @return il terzo parametro contenuto nel messaggio
	   * */
	  public static String getThirdParam(){
		  return thirdParam;
	  }
}
