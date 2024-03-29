package dummies;

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


/**
 * Classe dummy di message reader praticamente identica all'originale, la sola cosa che cambia è
 * che il metodo getSessions che viene chiamato all'arrivo del session info dal session manager del nuovo relay
 * questo ritorna una tabella di di DummySession anziche una di session. (anche nella versione originale non
 * veniva creato il proxy, c'era solo il passaggio di sessionni vuote, con solo l'indicazione delle porte ed 
 * il proxy viene in ogni caso creato dal sessionmanager)
 * @author marco
 *
 */

public class DummyMessageReader {


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
	
	
	private  Hashtable<String, DummySession> dummySessions;
	private  Hashtable<String, int[]> proxyInfo;
	private  String filename;
	private  int portStreamingClient;
	private  int portStreamingServer;
	private  double RSSI;
	private int portStreamingCtrlServer;
	
	
	public DummyMessageReader(){}
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
		if(code == Parameters.ELECTION_RESPONSE)
		{
			W = Float.parseFloat(st.nextToken());
		}
		if(code == Parameters.ELECTION_DONE)
		{
			newRelayAddress = st.nextToken();
		}
		if(code == Parameters.EM_ELECTION)
		{
			W = Float.parseFloat(st.nextToken());
		}
		if(code == Parameters.SESSION_INFO)
		{
			getHashTable(st);
		}
		if(code == Parameters.ACK_SESSION) // TODO da rivedere: ha senso nel messaggio di Ack session inviare il tutto il  sessions, proxy escluso?
		{
			getHashTable(st);
		}
		if(code == Parameters.REQUEST_FILE)
		{
			filename = st.nextToken();
			portStreamingClient = Integer.parseInt(st.nextToken());
		}
		if(code == Parameters.ACK_RELAY_FORW)
		{
			portStreamingServer = Integer.parseInt(st.nextToken());
			portStreamingCtrlServer = Integer.parseInt(st.nextToken());
		}
		if(code == Parameters.NOTIFY_RSSI)
		{
			RSSI = Double.parseDouble(st.nextToken());
		}
		
		/*AGGIUNTO DA LUCA IL 6-12 alle 17:43*/
		if(code == Parameters.IM_RELAY)
		{
			actualRelayAddress = st.nextToken();
		}
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

	/**
	 * Metodo che da una stringona che rappresenta una tabella estrae la tabella stessa
	 * ovviamente la tabella estratta dipende da quale messaggio si sta analizzando.
	 * @param elements la stringona arrivata per messaggio "wrappata" in un string tokenizer
	 * @return la tabella sessions (ip del cliente, Sessione), se il messaggio � un SESSION_INFO
	 * la tabella proxyInfo(ip cliente, porta del proxy sul nuovo relay sulla quale ridirigere il flusso)
	 * ,se il messaggio � ACK_SESSION
	 */
	
	private  Hashtable getHashTable(StringTokenizer elements)
	{

		if(code == Parameters.SESSION_INFO)
		{
			dummySessions = new Hashtable<String,DummySession>();
			while(elements.hasMoreElements())
			{
				// estraggo l'indirizzo del cliente
				String clientAddress = elements.nextToken();
				// estraggo le porte
				int[] sessionPorts = new int[6];
				sessionPorts[0] = Integer.parseInt(elements.nextToken());
				sessionPorts[1] = Integer.parseInt(elements.nextToken());
				sessionPorts[2] = Integer.parseInt(elements.nextToken());
				sessionPorts[3] = Integer.parseInt(elements.nextToken());
				sessionPorts[4] = Integer.parseInt(elements.nextToken());
				sessionPorts[5] = Integer.parseInt(elements.nextToken());
				// estraggo l'indirizzo del relay secondario; se non c'�
				// nella stringona trovo "null" e devo metterlo a null
				String relaySecondario = elements.nextToken();
				if (relaySecondario.equals("null"))
					relaySecondario=null;
				// Creo la sessione senza tuttavia indicare il proxy; andr� settato dopo dal SessionManager
				DummySession session = new DummySession(clientAddress,null,relaySecondario,sessionPorts);
				dummySessions.put(clientAddress, session);
			}
			return dummySessions;
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
		DummyMessageReader r = new DummyMessageReader();
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

	public  Hashtable<String, DummySession> getDummySessions() {
		return dummySessions;
	}

	public  double getW() {
		return W;
	}
	
	/*AGGIUNTO DA LUCA IL 6-12 alle 17:43*/
	public String getActualRelayAddress() {
		return actualRelayAddress;
	}
}
