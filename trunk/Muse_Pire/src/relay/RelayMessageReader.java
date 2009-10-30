package relay;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import parameters.Parameters;

public class RelayMessageReader {


	//Stringa contenente il messaggio
	private  String message = ""; 
	//indice messaggio
	private  int index = 0;
	//Codice messaggio
	private  int code = 0;
	//primo parametro messaggio
	private  String firstParam = "";
	//secondo parametro messaggio
	private  String secondParam = "";
	//terzo parametro messaggio
	private  String thirdParam = "";
	//quarto parametro messaggio
	private  String fourthParam = "";
	//quinto parametro messaggio
	private  String fifthParam = "";
	//sesto parametro messaggio
	private  String sixthParam = "";
	private  double W;
	private  String newRelayAddress = null;
	
	/*AGGIUNTO DA LUCA IL 6-12 alle 17:43*/
	private  String actualRelayAddress = null;
	private  String actualConnectedRelayAddress = null;
	
	
	private  Hashtable<String, int[]> sessionInfo;
	private  Hashtable<String, int[]> proxyInfo;
	private  String filename;
	private  int portStreamingClient;
	private  int portStreamingServer;
	private  double RSSI;
	private int portStreamingCtrlServer;
	
	
	public RelayMessageReader(){}
	/**
	 * Metodo che consente di leggere il contenuto di un pacchetto
	 * @param dp - datagramma di cui si vuole leggere il contenuto
	 */
	public  void readContent(DatagramPacket dp) throws IOException {
		ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		DataInputStream diStream = new DataInputStream(biStream);
		message = diStream.readUTF();
		System.out.println("Messaggio ricevuto: "+message);
		readMessage();
	}

	/**
	 * Metodo che consente di leggere i campi di un messaggio
	 * */
	 private void readMessage(){
		index = 0;
		code = 0;
		firstParam = "";
		secondParam = "";
		thirdParam = "";
		fourthParam = "";
		fifthParam = "";
		sixthParam = "";
		StringTokenizer st = new StringTokenizer(message, "_");
		String c = st.nextToken();
		index = Integer.parseInt(c);
		System.out.println("Index "+index);
		c = st.nextToken();
		code = Integer.parseInt(c);
		System.out.println("Code "+code);
		if(code==Parameters.REQUEST_RSSI) actualRelayAddress = st.nextToken();
		if(code == Parameters.NOTIFY_RSSI)RSSI = Double.parseDouble(st.nextToken());		
		if(code == Parameters.IM_BIGBOSS) actualConnectedRelayAddress = st.nextToken();
		if(code == Parameters.IM_RELAY) actualRelayAddress = st.nextToken();
		
//		if(code == Parameters.ELECTION_RESPONSE)
//		{
//			W = Float.parseFloat(st.nextToken());
//		}
//		if(code == Parameters.ELECTION_DONE)
//		{
//			newRelayAddress = st.nextToken();
//		}
		if(code == Parameters.EM_ELECTION) W = Float.parseFloat(st.nextToken());
		if(code == Parameters.SESSION_INFO) getHashTable(st);
		if(code == Parameters.ACK_SESSION) getHashTable(st);
//		if(code == Parameters.REQUEST_FILE)
//		{
//			filename = st.nextToken();
//			portStreamingClient = Integer.parseInt(st.nextToken());
//		}
//		if(code == Parameters.ACK_RELAY_FORW)
//		{
//			portStreamingServer = Integer.parseInt(st.nextToken());
//			portStreamingCtrlServer = Integer.parseInt(st.nextToken());
//		}
		
	}

	/**
	 * @return the portStreamingCtrlServer
	 */
	public int getPortStreamingCtrlServer() {
		return portStreamingCtrlServer;
	}
	/**
	 * Metodo getter
	 * @return l'indice del messaggio
	 * */
	public  int getIndex(){
		return index;
	}

	/**
	 * Metodo getter
	 * @return il codice del messaggio
	 * */
	public  int getCode(){
		return code;
	}

	/**
	 * Metodo getter
	 * @return il primo parametro contenuto nel messsaggio
	 * */
	public  String getFirstParam(){
		return firstParam;
	}

	/**
	 * Metodo getter
	 * @return il secondo parametro contenuto nel messaggio
	 * */
	public  String getSecondParam(){
		return secondParam;
	}

	/**
	 * Metodo getter
	 * @return il terzo parametro contenuto nel messaggio
	 * */
	public  String getThirdParam(){
		return thirdParam;
	}

	/**
	 * Metodo getter
	 * @return il quarto parametro contenuto nel messaggio
	 * */
	public  String getFourthParam(){
		return fourthParam;
	}

	/**
	 * Metodo getter
	 * @return il quinto parametro contenuto nel messaggio
	 * */
	public  String getFifthParam(){
		return fifthParam;
	}

	/**
	 * Metodo getter
	 * @return il sesto parametro contenuto nel messaggio
	 * */
	public  String getSixthParam(){
		return sixthParam;
	}

	private  Hashtable getHashTable(StringTokenizer elements)
	{

		if(code == Parameters.SESSION_INFO)
		{
			sessionInfo = new Hashtable();
			while(elements.hasMoreElements())
			{
				String clientAddress = elements.nextToken();
				int[] sessionPorts = new int[6];
				sessionPorts[0] = Integer.parseInt(elements.nextToken());
				sessionPorts[1] = Integer.parseInt(elements.nextToken());
				sessionPorts[2] = Integer.parseInt(elements.nextToken());
				sessionPorts[3] = Integer.parseInt(elements.nextToken());
				sessionPorts[4] = Integer.parseInt(elements.nextToken());
				sessionPorts[5] = Integer.parseInt(elements.nextToken());
				sessionInfo.put(clientAddress, sessionPorts);
			}
			return sessionInfo;
		}
		if(code == Parameters.ACK_SESSION)
		{
			proxyInfo = new Hashtable();
			while(elements.hasMoreElements())
			{
				String clientAddress = elements.nextToken();
				int[] sessionPorts = new int[1];
				sessionPorts[0] = Integer.parseInt(elements.nextToken());
				proxyInfo.put(clientAddress, sessionPorts);
			}
			return proxyInfo;
		}
		return null;
	}


	public void stamp()
	{
		String dati = "123.123.123.123_1_2_3_4_234.234.234.234_2_3_4_5_123.124.123.123_3_5_6_7";
		StringTokenizer st =new StringTokenizer(dati, "_");
		Hashtable ht = getHashTable(st);
		Enumeration keys = ht.keys();

		while(keys.hasMoreElements())
		{
			String chiave = keys.nextElement().toString();
			System.out.println(chiave);
			int[] values =(int[]) ht.get(chiave);
			for(int i = 0; i<values.length;i++)
			{
				System.out.println("Porta "+ i+":..."+ values[i]);
			}
		}
	}

	public  void main(String[] args)
	{
		RelayMessageReader r = new RelayMessageReader();
		r.stamp();
	}

	public  String getFilename() {
		return filename;
	}

	public  String getMessage() {
		return message;
	}

	public  String getNewRelayAddress() {
		return newRelayAddress;
	}

	public  int getPortStreamingClient() {
		return portStreamingClient;
	}

	public  int getPortStreamingServer() {
		return portStreamingServer;
	}

	public  Hashtable<String, int[]> getProxyInfo() {
		return proxyInfo;
	}

	public  double getRSSI() {
		return RSSI;
	}

	public  Hashtable<String, int[]> getSessionInfo() {
		return sessionInfo;
	}

	public  double getW() {
		return W;
	}
	
	/*AGGIUNTO DA LUCA IL 6-12 alle 17:43*/
	public String getActualRelayAddress() {
		return actualRelayAddress;
	}
	
	public String getActualConnectedRelayAddress() {
		return actualConnectedRelayAddress;
	}
}
