package muse.client;

/**
 * @version 1.2
 * */
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.StringTokenizer;

import parameters.Parameters;

public class ClientMessageReader {

	//Stringa contenente il messaggio
	private static String message = ""; 
	//Numero di sequenza del messaggio
	private static int index = 0;
	//Codice messaggio
	private static int code = 0;
	//primo parametro messaggio
	private static String firstParam = "";
	//secondo parametro messaggio
	private static String secondParam = "";
	//terzo parametro messaggio
	private static String thirdParam = "";
	
	  /**
	   * Metodo che consente di leggere il contenuto di un pacchetto
	   * @param dp - datagramma di cui si vuole leggere il contenuto
	   */
	  static protected void readContent(DatagramPacket dp) throws IOException {
		  ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		  DataInputStream diStream = new DataInputStream(biStream);
		  message = diStream.readUTF();
		  System.out.println("Messaggio ricevuto: "+message);
		  readMessage();
	  }
	  
	  static protected void readContent(String dp) throws IOException {
			message = dp;
		    System.out.println("Message: "+message);
		    readMessage();
	  }
	  
	  /**
	   * Metodo che consente di leggere i campi di un messaggio
	   * */
	  static private void readMessage(){
		  index = 0;
		  code = 0;
		  firstParam = "";
		  secondParam = "";
		  thirdParam = "";
		  StringTokenizer st = new StringTokenizer(message, "_");
		  String c = st.nextToken();
		  index = Integer.parseInt(c);
		  c = st.nextToken();
		  code = Integer.parseInt(c);
		  		System.out.println("ClientMessageReader: il codice del messaggio ricevuto è "+code);
		  	//	||(code == Parameters.START_PLAYBACK) 
		  if((code == Parameters.START_OFF)|| (code == Parameters.FILES_RESPONSE))
		  {
			  firstParam = st.nextToken();
		  }
		  if(code == Parameters.CONFIRM_REQUEST)
		  {
			  firstParam = st.nextToken();
			  			System.out.println("CONFIRM_REQUEST firstParam: "+firstParam);
		  }
	  }
	  
	  /**
	   * Metodo getter
	   * @return il numero di sequenza del messaggio
	   * */
	  public static int getIndex(){
		  return index;
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
	  
	  /**
	   * Metodo getter
	   * @return il terzo parametro contenuto nel messaggio
	   * */
	  public static String getMessage(){
		  return message;
	  }
}
