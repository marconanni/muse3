package relay.messages;

/**
 * Utility per la lettura di messaggi di controllo (pacchetti UDP) lato relay
 * @author (modificato da Pire Dejaco)
 * @version 2.0
 * */

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.StringTokenizer;

import parameters.MessageCodeConfiguration;

public class RelayMessageReader {
	
	private  String message = ""; 				//Stringa contenente il messaggio
	private  int index = 0;						//indice messaggio
	private  int code = 0;						//Codice messaggio
	
	//Ulteriodi parametri che dipendono dai vari messaggi
	private  double W;
	private  String actualConnectedRelayAddress = null;

	private  double RSSI;

	private InetAddress packetAddress;
	private int activeRelay;
	private int activeClient;
	private int typeNode;
	
	
	public RelayMessageReader(){}
	
	/** Metodo che consente di leggere il contenuto di un pacchetto
	 * @param dp - datagramma di cui si vuole leggere il contenuto*/
	public  void readContent(DatagramPacket dp) throws IOException {
		packetAddress = dp.getAddress();
		ByteArrayInputStream biStream = new ByteArrayInputStream(dp.getData(), 0, dp.getLength());
		DataInputStream diStream = new DataInputStream(biStream);
		message = diStream.readUTF();
		readMessage();
	}

	/**Metodo che consente di leggere i campi di un messaggio*/
	 private void readMessage(){
		index = 0;
		code = 0;
		
		StringTokenizer st = new StringTokenizer(message, "_");
		index = Integer.valueOf(st.nextToken());
		code = Integer.valueOf(st.nextToken());

		if(code==MessageCodeConfiguration.REQUEST_RSSI) actualConnectedRelayAddress = packetAddress.toString();
		if(code == MessageCodeConfiguration.NOTIFY_RSSI){
			RSSI = Double.parseDouble(st.nextToken());
			typeNode = Integer.parseInt(st.nextToken());
			activeClient = Integer.parseInt(st.nextToken());
		}
		if(code == MessageCodeConfiguration.IM_RELAY) actualConnectedRelayAddress = (packetAddress.toString()).substring(1,(packetAddress.toString()).length());
		if(code == MessageCodeConfiguration.ACK_CONNECTION) typeNode = Integer.valueOf(st.nextToken());
		if(code == MessageCodeConfiguration.ELECTION_REQUEST)activeRelay =Integer.valueOf(st.nextToken());
	}

	/**Metodo getter
	 * @return l'indice del messaggio*/
	public  int getIndex(){return index;}

	/**Metodo getter
	 * @return il codice del messaggio*/
	public  int getCode(){return code;}
	
	/**Metogo getter
	 * @return indirizzo Ip in forma di stringa del nodo (relay o big boss attivo) a cui si è collegati*/
	public String getActualConnectedRelayAddress(){return actualConnectedRelayAddress;}
	
	/**Metogo getter
	 * @return valore RSSI attuale*/
	public  double getRSSI() {return RSSI;}
	
	/**Metogo getter
	 * @return tipologia del nodo (client, relay attivo o passivo, BigBoss passivo..)*/
	public int getTypeNode(){return typeNode;}
	
	public int getActiveClient(){return activeClient;}
	
	/**Metogo getter
	 * @return indirizzo Ip sorgente del pacchetto*/
	public InetAddress getPacketAddess(){return packetAddress;}
		
	/**Metogo getter
	 * @return numero di Relay attivi attualmente connessi*/
	public int getActiveRelay(){return activeRelay;}
	
	public  double getW() {return W;}
}




//private  Hashtable<String, int[]> sessionInfo;
//private  Hashtable<String, int[]> proxyInfo;
//private  String filename;
//private  int portStreamingClient;
//private  int portStreamingServer;
//private int portStreamingCtrlServer;

//if(code == Parameters.ELECTION_RESPONSE) W = Float.parseFloat(st.nextToken());
//if(code == Parameters.ELECTION_DONE) newRelayAddress = st.nextToken();
//if(code == Parameters.EM_ELECTION) W = Float.parseFloat(st.nextToken());
//if(code == Parameters.SESSION_INFO) getHashTable(st);
//if(code == Parameters.ACK_SESSION) getHashTable(st);
//if(code == Parameters.REQUEST_FILE){
//	filename = st.nextToken();
//	portStreamingClient = Integer.parseInt(st.nextToken());
//}
//if(code == Parameters.ACK_RELAY_FORW){
//	portStreamingServer = Integer.parseInt(st.nextToken());
//	portStreamingCtrlServer = Integer.parseInt(st.nextToken());
//}

//public  String getFilename() {return filename;}
//public  String getMessage() {return message;}
//public  String getNewRelayAddress() {return newRelayAddress;}
//public  int getPortStreamingClient() {return portStreamingClient;}
//public  int getPortStreamingServer() {return portStreamingServer;}
//public  Hashtable<String, int[]> getProxyInfo() {return proxyInfo;}
//public  Hashtable<String, int[]> getSessionInfo() {return sessionInfo;}
//public  double getW() {return W;}
//public int getPortStreamingCtrlServer() {return portStreamingCtrlServer;}

//private  Hashtable getHashTable(StringTokenizer elements)
//{
//
//	if(code == Parameters.SESSION_INFO)
//	{
//		sessionInfo = new Hashtable();
//		while(elements.hasMoreElements())
//		{
//			String clientAddress = elements.nextToken();
//			int[] sessionPorts = new int[6];
//			sessionPorts[0] = Integer.parseInt(elements.nextToken());
//			sessionPorts[1] = Integer.parseInt(elements.nextToken());
//			sessionPorts[2] = Integer.parseInt(elements.nextToken());
//			sessionPorts[3] = Integer.parseInt(elements.nextToken());
//			sessionPorts[4] = Integer.parseInt(elements.nextToken());
//			sessionPorts[5] = Integer.parseInt(elements.nextToken());
//			sessionInfo.put(clientAddress, sessionPorts);
//		}
//		return sessionInfo;
//	}
//	if(code == Parameters.ACK_SESSION)
//	{
//		proxyInfo = new Hashtable();
//		while(elements.hasMoreElements())
//		{
//			String clientAddress = elements.nextToken();
//			int[] sessionPorts = new int[1];
//			sessionPorts[0] = Integer.parseInt(elements.nextToken());
//			proxyInfo.put(clientAddress, sessionPorts);
//		}
//		return proxyInfo;
//	}
//	return null;
//}
//
//
//public void stamp()
//{
//	String dati = "123.123.123.123_1_2_3_4_234.234.234.234_2_3_4_5_123.124.123.123_3_5_6_7";
//	StringTokenizer st =new StringTokenizer(dati, "_");
//	Hashtable ht = getHashTable(st);
//	Enumeration keys = ht.keys();
//
//	while(keys.hasMoreElements())
//	{
//		String chiave = keys.nextElement().toString();
//		System.out.println(chiave);
//		int[] values =(int[]) ht.get(chiave);
//		for(int i = 0; i<values.length;i++)
//		{
//			System.out.println("Porta "+ i+":..."+ values[i]);
//		}
//	}
//}
//
//public  void main(String[] args)
//{
//	RelayMessageReader r = new RelayMessageReader();
//	r.stamp();
//}
