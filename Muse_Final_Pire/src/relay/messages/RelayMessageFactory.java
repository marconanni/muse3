package relay.messages;

/**
 * Utility per la costruzione di messaggi di controllo (pacchetti UDP) lato relay
 * @author Leo Di Carlo (modificato da Pire Dejaco)
 * @version 2.0
 * */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import parameters.MessageCodeConfiguration;


public class RelayMessageFactory {

	/**
	 * Pacchetto utilizzato come messaggio di acknoledge
	 * @param sequenceNumber
	 * @param addr indirizzo verso il quale inviare il messaggio di ack
	 * @param port porta di ricezione del megasggio di ack
	 * @return DatagramPacket da inviare
	 * @throws IOException
	 */
	static public DatagramPacket buildNoParamAckResponce(int sequenceNumber, InetAddress addr, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);
	}

/*** PARTE RIGUARDO ALLA CONFIGURAZIONE DELLA RETE E PROTOCOLLO DI ELEZIONE ***/
	
	/**
	 * Messaggio di WHO_IS_RELAY spedito da ogni nodo quando entra a far parte della rete MANET (tranne il BigBoss ed i relay attivi)
	 * @category electionManager
	 * @param destination Address
	 * @param destination Port
	 * @return DatagramPacket da inviare
	 * @throws IOException
	 */
	static public DatagramPacket buildWhoIsRelay(InetAddress address, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.WHO_IS_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, address, port);
	}

	/**
	 * Messaggio di IM_REALY, risposta da parte o di un nodo relay attivo o da parte del BigBoss
	 * @category electionManager
	 * @param destination Address
	 * @param destination Port
	 * @return DatagramPacket da inviare
	 * @throws IOException
	 */
	static public DatagramPacket buildImRelay(InetAddress address, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.IM_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, address, port);
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
	 * Messaggio di ElectionRelayRequest da parte del Relay attivo (elezione di un relay attivo)
	 * @param Destination Address
	 * @param Destination Port
	 * @return DatagramPacket da inviare 
	 * @throws IOException
	 */
	static public DatagramPacket buildElectionRequest(InetAddress addr, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+MessageCodeConfiguration.ELECTION_REQUEST;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Messaggio di Election_Relay_Beacon in caso di elezione di un nuovo BigBoss
	 * @category electionManager
	 * @param int sequenceNumber
	 * @param destination Address 
	 * @param Destination Port
	 * @return DatagramPacket da inviare 
	 * @throws IOException
	 */
	public static DatagramPacket buildElectioBeaconRelay(int sequenceNumber, InetAddress addr, int port, int client) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_BEACON_RELAY+"_"+client;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);
	}

	/**
	 * Messaggio di REQUEST_RSSI, richiesta della potenza del segnale
	 * @param sequenceNumber
	 * @param destination address
	 * @param destination port
	 * @return DatagramPacket da inviare
	 * @throws IOException
	 */
	static public DatagramPacket buildRequestRSSI(int sequenceNumber, InetAddress address, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_RSSI;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, address, port);
	}
	
	/**
	 * Messaggio NOTIFY_RSSI, risposta al messaggio di REQUEST_RSSI contenente il valore attuale della potenza del segnale
	 * @param int sequenceNumber
	 * @param int RSSIvalue (valore della potenza del segnale attuale)
	 * @param destination address
	 * @param destination port
	 * @return DatagramPacket da inviare 
	 * @throws IOException
	 */
	public static DatagramPacket buildNotifyRSSI(int sequenceNumber , int RSSIvalue, InetAddress addr, int port,int nodeType, int activeClient ) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.NOTIFY_RSSI+"_"+RSSIvalue+"_"+nodeType+"_"+activeClient;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);
	}

	/**
	 * Messaggio di ElectionResponse
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildElectionResponse(int sequenceNumber, double W,InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_RESPONSE+"_"+W;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}
	
	/**
	 * Messaggio di ElectionDone
	 * @param newRelayAddress
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildElectionDone(int sequenceNumber,String oldRelay, String newRelayAddress, String nodeHeadAddress, InetAddress addr, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_DONE+"_"+oldRelay+"_"+newRelayAddress+"_"+nodeHeadAddress;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();
		return new DatagramPacket(data, data.length, addr, port);

	}

	/**
	 * Messaggio di ElectionDone
	 * @param newRelayAddress
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildEmElDetRelay(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.EM_EL_DET_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}

	/**
	 * Messaggio di ElectionResponse
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildEmElection(int sequenceNumber, double W,InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.EM_ELECTION+"_"+W;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}	
}



/**
 * Messaggio di WHO_IS_BIGBOSS spedito dai nodi realy attivi quando entrano a fare parte della rete MANET
 * @category electionManager
 * @param destination Address
 * @param destination Port
 * @return DatagramPacket da inviare
 * @throws IOException
 */
/*static public DatagramPacket buildWhoIsBigBoss(InetAddress address, int port) throws IOException {
	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = 0+"_"+MessageCodeConfiguration.WHO_IS_BIGBOSS;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();
	return new DatagramPacket(data, data.length, address, port);
}*/

/**
 * Messaggio di IM_BIGBOSS, risposta da parte del BigBoss
 * @category electionManager
 * @param destination Address
 * @param destination Port
 * @return DatagramPacket da inviare
 * @throws IOException
 */
/*static public DatagramPacket buildImBigBoss(InetAddress address, int port) throws IOException {
	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = 0+"_"+Parameters.IM_BIGBOSS;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();
	return new DatagramPacket(data, data.length, address, port);
}*/

/**
 * Messaggio di ElectionBigBossRequest da parte del BigBoss (elezione di un nuovo BigBoss)
 * @param Destination Address
 * @param Destination Port
 * @param int Active Relays (numeri di relay attivi attualmente connessi al BigBoss)
 * @return DatagramPacket da inviare 
 * @throws IOException
 */
/*static public DatagramPacket buildElectionBigBossRequest(InetAddress addr, int port,int active_relays) throws IOException {
	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = 0+"_"+MessageCodeConfiguration.ELECTION_BIGBOSS_REQUEST+"_"+active_relays;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();
	return new DatagramPacket(data, data.length, addr, port);
}*/

///**
//* Pacchetto utilizzato come messaggio di acknoledge
//* @param sequenceNumber
//* @param addr indirizzo verso il quale inviare il messaggio di ack
//* @param port porta di ricezione del megasggio di ack
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildAckClientReq(int sequenceNumber, int port,InetAddress addr, int proxyOutStreaming, int proxyCtrl) throws IOException {
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_CLIENT_REQ+"_"+proxyOutStreaming+"_"+proxyCtrl;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//	return new DatagramPacket(data, data.length, addr, port);
//}

///**
//* Pacchetto per inoltrare la richiesta al server del file da trasmettere
//* @param sequenceNumber
//* @param serveraddr indirizzo del server
//* @param serverport porta di ricezione del sever
//* @param portStreamingPort porta sulla quale il proxy riceve lo stream da parte del server
//* @param proxyCtrlPort porta di controllo proxy 
//* @param clientAddr indirizzo del client che serve come chiave per identificare sul server la sessione alla relativa richiesta
//* @param filename nome del file da trasmettere
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildForwardReqFile(int sequenceNumber, String clientAddr, String filename, int controlProxyPort,int proxyStreamingInPort, InetAddress serveraddr, int serverport) throws IOException {
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_FILE+"_"+clientAddr+"_"+filename+"_"+controlProxyPort+"_"+proxyStreamingInPort;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//	return new DatagramPacket(data, data.length, serveraddr, serverport);
//}

///**
//* Pacchetto di richiesta di inizio della trasmissione verso il server
//* @param sequenceNumber
//* @param addr indirizzo del server
//* @param port porta verso la quale inoltrare la richiesta (tale parametro � stato precedentemente trasmesso dal server)
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildStartTx(int sequenceNumber, InetAddress addr, int port) throws IOException {
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.START_TX;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//	return new DatagramPacket(data, data.length, addr, port);
//}
//
///**
//* Pacchetto di richiesta di inizio della trasmissione verso il server
//* @param sequenceNumber
//* @param addr indirizzo del server
//* @param port porta verso la quale inoltrare la richiesta (tale parametro � stato precedentemente trasmesso dal server)
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildStopTx(int sequenceNumber, InetAddress addr, int port) throws IOException {
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.STOP_TX;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//	return new DatagramPacket(data, data.length, addr, port);
//}

///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildRequestSession(int sequenceNumber, InetAddress addr, int port) throws IOException {
//
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_SESSION;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, addr, port);
//
//}

///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildSessionInfo(int sequenceNumber, Hashtable sessionInfo, InetAddress addr, int port) throws IOException {
//
//	String session="";
//	if(sessionInfo!=null){
//		session = getSession(sessionInfo);
//	}
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.SESSION_INFO+session;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, addr, port);
//
//}
//
//
///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildAckSession(int sequenceNumber, String proxyInfo, InetAddress addr, int port) throws IOException {
//
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_SESSION+proxyInfo;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, addr, port);
//
//}
//
///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildRedirect(int sequenceNumber, InetAddress addr, int port) throws IOException {
//
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.REDIRECT;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, addr, port);
//
//}
//
//
///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
//static public DatagramPacket buildLeave(int sequenceNumber, InetAddress addr, int port) throws IOException {
//
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.LEAVE;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, addr, port);
//
//}
//
//
///*
//* AGGIUNTO DA CARLO 4-12-2008 13.50
//*/
///**
//* Messaggio di ServerUnreacheable
//* 
//* @param sequenceNumber
//* @param clientAddr
//* @param port
//* @return
//* @throws IOException 
//*/
//static public DatagramPacket buildServerUnreacheable(int sequenceNumber, InetAddress clientAddr, int port) throws IOException{
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.SERVER_UNREACHEABLE;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, clientAddr, port);
//}
//
//
///**
//* Messaggio di SessionUnreacheable
//* 
//* @param sequenceNumber
//* @param clientAddr
//* @param port
//* @return
//* @throws IOException 
//*/
//static public DatagramPacket buildSessionInvalidation(int sequenceNumber, InetAddress clientAddr, int port) throws IOException{
//	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//	DataOutputStream doStream = new DataOutputStream(boStream);
//	String content = sequenceNumber+"_"+MessageCodeConfiguration.SESSION_INVALIDATION;
//	doStream.writeUTF(content);
//	doStream.flush();
//	byte[] data = boStream.toByteArray();
//
//	return new DatagramPacket(data, data.length, clientAddr, port);
//}
//
//
//private static String getSession(Hashtable sessionInfo)
//{
//	String session = "";
//	String chiave;
//	Hashtable ht = sessionInfo;
//	Enumeration keys = ht.keys();
//	while(keys.hasMoreElements())
//	{
//		chiave = keys.nextElement().toString();
//		//session.concat("_"+chiave);
//		session = session+"_"+chiave;
//
//		int[] values =(int[]) ht.get(chiave);
//		for(int i = 0; i<values.length;i++)
//		{
//			System.out.println("Porta "+ i+":..."+ values[i]);
//			//session.concat("_"+values[i]);
//			session = session +"_"+ values[i];
//		}
//		//session = session;
//	}
//	//session.replaceFirst("_", "");
////	session = session.substring(0, session.length()-1);
//	System.err.println("IN SESSION_INFO: "+session);
//	return session;
//}