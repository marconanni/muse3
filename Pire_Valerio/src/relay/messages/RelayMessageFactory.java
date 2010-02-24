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
import java.util.Enumeration;
import java.util.Hashtable;

import parameters.MessageCodeConfiguration;
import relay.Session;


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
	public static DatagramPacket buildElectioBeacon(int sequenceNumber, InetAddress addr, int port, int client) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_BEACON+"_"+client;
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
	static public DatagramPacket buildElectionDone(int sequenceNumber,String newRelayLocalClusterAddress, String oldRelayLocalClusterAddress, String oldRelayLocalClusterHeadAddress, String nodeHeadAddress, InetAddress addr, int port) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_DONE+"_"+newRelayLocalClusterAddress+"_"+oldRelayLocalClusterAddress+"_"+oldRelayLocalClusterHeadAddress+"_"+nodeHeadAddress;
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
	
	//VALERIO
	//nel RelaySessionManager dovrò stare attento a quale di questi metodi chiamare. Ad esempio: se sono un normale relay e sto salendo verso il server
	//chiamerò ad esempio buildForwardRequestList e lo invierò al bigboss, bigboss prenderà il messaggio lo riscriverà (per aggiornare il valore
	//di Parameters.RELAY_SESSION_AD_HOC_PORT_IN che è la porta su cui aspetta la risposta) e lo invierà al server, il server risponderà
	//sull'indirizzo di bigboss sulla porta specificata con un messaggio del tipo FORWARD_LIST_RESPONSE contenente indirizzo e porta di relay e client
	//	

		//Valerio: messaggio di richiesta lista file che bigboss invia al server in caso di catena: client->bigboss->server
		static public DatagramPacket buildRequestList(int sequenceNumber, InetAddress addr, int port, String clientAddress) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			//String content = sequenceNumber+"_"+Parameters.REQUEST_LIST+"_"+Parameters.BIG_BOSS_SESSION_AD_HOC_PORT_IN+"_"+clientAddress+"_"+clientPort;
			String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_LIST+"_"+clientAddress;
			
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
	
		//Valerio: messaggio di richiesta lista file che bigboss invia al server in caso di catena: client->relay->bigboss->server
		//questo messaggio è lo stesso che viene inviato dal relay al bigboss
		static public DatagramPacket buildForwardRequestList(int sequenceNumber, InetAddress addr, int port, String relayAddress, String clientAddress) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = null;
			//String content = sequenceNumber+"_"+Parameters.FORWARD_REQ_LIST+"_"+Parameters.BIG_BOSS_SESSION_AD_HOC_PORT_IN+"_"+relayAddress+"_"+relayPort+"_"+clientAddress+"_"+clientPort;
//			if(relayAddress==null)
//				content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_LIST+"_"+clientAddress;
//			else
				content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_LIST+"_"+relayAddress+"_"+clientAddress;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
		
		//Valerio: messaggio con la lista file che bigboss invia al client in caso di catena: server->bigboss->client	
		//o che il relay manda al client in caso di catena server->bigboss->relay->client	
		static public DatagramPacket buildListResponse(int sequenceNumber, InetAddress addr, int port,String listaFile) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = sequenceNumber+"_"+MessageCodeConfiguration.LIST_RESPONSE+"_"+listaFile;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
		
		//Valerio: messaggio con la lista file che il server invia al bigboss in caso di catena: server->bigboss->relay->client
		//questo messaggio è lo stesso che viene inviato dal bigboss al relay
		//il server metterà come addr l'indirizzo da cui ha ricevuto la richiesta della lista e come port qeulla specificata nella richiesta
		//il bigboss metterà come addr lo stesso valore di relayAddress e come port lo stesso valore di relayPort
		static public DatagramPacket buildForwardListResponse(int sequenceNumber, InetAddress addr, int port,String relayAddress,String clientAddress, String listaFile) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = null;
			content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_LIST_RESPONSE+"_"+relayAddress+"_"+clientAddress+"_"+listaFile;//+"_"+relayAddress+"_"+relayPort+"_"+clientAddress+"_"+clientPort;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
		
		//Valerio: messaggio richiesta file che bigboss invia al server in caso di catena: client->bigboss->server
//		static public DatagramPacket buildReqFile(int sequenceNumber, String filename, int controlBigBossPort,int bigbossStreamingInPort, String clientAddress, int controlPortClient, int streamingPortClient,InetAddress serveraddr, int serverport) throws IOException {
		static public DatagramPacket buildReqFile(int sequenceNumber, String filename, int controlBigBossPort,int bigbossStreamingInPort, String clientAddress, int streamingPortClient,InetAddress serveraddr, int serverport) throws IOException {
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
//			String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_FILE+"_"+filename+"_"+controlBigBossPort+"_"+bigbossStreamingInPort+"_"+clientAddress+"_"+controlPortClient+"_"+streamingPortClient;
			String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_FILE+"_"+filename+"_"+controlBigBossPort+"_"+bigbossStreamingInPort+"_"+clientAddress+"_"+streamingPortClient;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, serveraddr, serverport);
		}
		
		//Valerio: messaggio richiesta file che relay invia a bigboss in caso di catena: client->relay->bigboss->server
		//bisogna stare attenti quando si usa perchè alcuni vampi vanno lasciati vuoti: ad esempio il relay non conosce la porta stream di big boss
		//vi penserà big boss a inserire il valore quando girerà il messaggio al server. altri campi vanno replicati, ad esempio
		//per il relay la porta del server è uguale a bigbossControlPort e l'indirizzo del server è l'indirizzo di bigboss
		static public DatagramPacket buildForwardReqFile(int sequenceNumber, String filename,int bigbossControlPort, int bigbossStreamingInPort, String relayAddress, int controlPortRelay, int streamPortRelay,String clientAddress, int streamingPortClient,InetAddress serveraddr, int serverport) throws IOException {
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_FILE+"_"+filename+"_"+bigbossControlPort+"_"+bigbossStreamingInPort+"_"+relayAddress+"_"+controlPortRelay+"_"+streamPortRelay+"_"+clientAddress+"_"+streamingPortClient;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, serveraddr, serverport);
		}
		
		/**
		 * Pacchetto per inoltrare la richiesta al server del file da trasmettere
		 * @param sequenceNumber
		 * @param serveraddr indirizzo del server
		 * @param serverport porta di ricezione del sever
		 * @param portStreamingPort porta sulla quale il proxy riceve lo stream da parte del server
		 * @param proxyCtrlPort porta di controllo proxy 
		 * @param clientAddr indirizzo del client che serve come chiave per identificare sul server la sessione alla relativa richiesta
		 * @param filename nome del file da trasmettere
		 * @return
		 * @throws IOException
		 */
//		static public DatagramPacket buildForwardReqFile(int sequenceNumber, String clientAddr, String filename, int controlProxyPort,int proxyStreamingInPort, InetAddress serveraddr, int serverport) throws IOException {
//
//			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
//			DataOutputStream doStream = new DataOutputStream(boStream);
//			String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_FILE+"_"+clientAddr+"_"+filename+"_"+controlProxyPort+"_"+proxyStreamingInPort;
//			doStream.writeUTF(content);
//			doStream.flush();
//			byte[] data = boStream.toByteArray();
//
//			return new DatagramPacket(data, data.length, serveraddr, serverport);
//
//		}
		
		/**
		 * Pacchetto di richiesta di inizio della trasmissione o ripresa flusso di streaming verso il server
		 * @param sequenceNumber
		 * @param addr indirizzo del server
		 * @param port porta verso la quale inoltrare la richiesta (tale parametro � stato precedentemente trasmesso dal server)
		 * @return
		 * @throws IOException
		 */
		static public DatagramPacket buildStartTx(int sequenceNumber, InetAddress addr, int port) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = sequenceNumber+"_"+MessageCodeConfiguration.START_TX;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
		
		/**
		 * Pacchetto di richiesta di stop della trasmissione verso il server
		 * @param sequenceNumber
		 * @param addr indirizzo del server
		 * @param port porta verso la quale inoltrare la richiesta (tale parametro � stato precedentemente trasmesso dal server)
		 * @return
		 * @throws IOException
		 */
		static public DatagramPacket buildStopTx(int sequenceNumber, InetAddress addr, int port) throws IOException {

			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = sequenceNumber+"_"+MessageCodeConfiguration.STOP_TX;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();

			return new DatagramPacket(data, data.length, addr, port);

		}
		
		
		static public DatagramPacket buildAckClientReq(int sequenceNumber, int port,InetAddress addr, int proxyOutStreaming, int proxyCtrl) throws IOException {
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_CLIENT_REQ+"_"+proxyOutStreaming+"_"+proxyCtrl;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();
			return new DatagramPacket(data, data.length, addr, port);
		}
		
//		static public DatagramPacket buildForwardAckReq(int sequenceNumber, int port,InetAddress addr, int bigBossOutputStream, int bigBossControlStream, String clientAddr,int clientControlPort, int clientStreamPort)throws IOException{
		static public DatagramPacket buildForwardAckReq(int sequenceNumber, int port,InetAddress addr, int bigBossOutputStream, int bigBossControlStream, String clientAddr,int clientStreamPort)throws IOException{
			ByteArrayOutputStream boStream = new ByteArrayOutputStream();
			DataOutputStream doStream = new DataOutputStream(boStream);
			//i messaggi di questo tipo arrivano anche dal server al bigboss
			//in questo caso è un messaggio che va dal big boss al relay, 
			//ma essendo dello stesso tipo il messagereader è uguale
			//quindi i campi che servono da server->bigboss e che non servono da bigboss->relay li riempo di cose inutili e poi non li andrò ad usare
//			String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_ACK_REQ+"_"+clientAddr+"_"+clientControlPort+"_"+clientStreamPort+"_"+null+"_"+-1+"_"+-1+"_"+bigBossControlStream+"_"+bigBossOutputStream;
			String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_ACK_REQ+"_"+clientAddr+"_"+clientStreamPort+"_"+null+"_"+-1+"_"+-1+"_"+bigBossControlStream+"_"+bigBossOutputStream;
			doStream.writeUTF(content);
			doStream.flush();
			byte[] data = boStream.toByteArray();
			return new DatagramPacket(data, data.length, addr, port);
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

/**
 * Messaggio di ElectionDone
 * @param newRelayAddress
 * @param addr
 * @param port
 * @return
 * @throws IOException
 */
static public DatagramPacket buildRequestSession(int sequenceNumber, InetAddress addr, int port) throws IOException {
	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_SESSION;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();
	return new DatagramPacket(data, data.length, addr, port);
}

/**
 * Messaggio di SessionInfo - manda al nuovo relay le caratterisitiche delle connessioni in corso sotto forma di 
 * un'unica grande stringona;
 * @param newRelayAddress
 * @param addr indirizzo del nuovo relay al quale mandare il pacchetto
 * @param port la porta sulla quale il nuovo relay attende il pacchetto
 * @return il pacchetto da inviare 
 * @throws IOException
 */
static public DatagramPacket buildSessionInfo(int sequenceNumber, Hashtable sessions, InetAddress addr, int port) throws IOException {

	String session="";
	if(sessions!=null){
		session = getSession(sessions); // il metofo getSession trasforma la tabella sessions in un'unica stringona
	}
	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = sequenceNumber+"_"+MessageCodeConfiguration.SESSION_INFO+session;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();

	return new DatagramPacket(data, data.length, addr, port);

}
//
//
/**
 * Messaggio di ElectionDone
 * @param newRelayAddress
 * @param addr
 * @param port
 * @return
 * @throws IOException
 */
static public DatagramPacket buildAckSession(int sequenceNumber, String proxyInfo, InetAddress addr, int port) throws IOException {

	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_SESSION+proxyInfo;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();

	return new DatagramPacket(data, data.length, addr, port);

}
//
///**
//* Messaggio di ElectionDone
//* @param newRelayAddress
//* @param addr
//* @param port
//* @return
//* @throws IOException
//*/
static public DatagramPacket buildRedirect(int sequenceNumber, InetAddress addr, int port) throws IOException {

	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = sequenceNumber+"_"+MessageCodeConfiguration.REDIRECT;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();

	return new DatagramPacket(data, data.length, addr, port);

}
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
static public DatagramPacket buildLeave(int sequenceNumber, InetAddress addr, int port) throws IOException {

	ByteArrayOutputStream boStream = new ByteArrayOutputStream();
	DataOutputStream doStream = new DataOutputStream(boStream);
	String content = sequenceNumber+"_"+MessageCodeConfiguration.LEAVE;
	doStream.writeUTF(content);
	doStream.flush();
	byte[] data = boStream.toByteArray();

	return new DatagramPacket(data, data.length, addr, port);

}
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

/**
 * 
 * Metodo che converte le informazioni delle sessioni in un'unica stringona
 * 
 * la stringa ritornata � del tipo ( ip del client della sessione1_porte del client della sessione1_
 * ip del relay secondario della sessione1 ( o "null" se non c'� il relay secondario)_
 * ip del cilent della sessione 2_ porte della sessione 2 ecc)
 * 
 * es ( per una sola sessione)
 * "192.168.1.2_1000_2000_3000_5000_6000_null"
 * 
 * @param sessions una tabella di tipo id sessione( i'ip del client come stringa) - sessione (istanza della classe Session)
 * @return un'unica stringa che rappresenta tutti i dati relativi alle sessioni
 * 
 * 
 * 
 * 
 * 
 */

private static String getSession(Hashtable sessions)
{
	String session = ""; // la stringona finale
	String chiave;
	String relaySecondario;
	Hashtable ht = sessions;
	Enumeration keys = ht.keys();
	while(keys.hasMoreElements())
	{
		chiave = keys.nextElement().toString();
		//session.concat("_"+chiave);
		//Scrivo l'indirizzo del client ( l'identificativo della sessione)
		session = session+"_"+chiave;
		
		// recupero le porte come un arrey di interi dallla sessione
		int[] values =((Session) (ht.get(chiave))).getSessionInfo();
		for(int i = 0; i<values.length;i++)
		{
			System.out.println("Porta "+ i+":..."+ values[i]);
			//session.concat("_"+values[i]);
			session = session +"_"+ values[i];
		}
		
		// scrittura relay secondario: se null scrivo "null", altrimenti scrivo l'ip che contiene
		
		
		if (((Session) ht.get(chiave)).getRelaySecondario()!=null){
			relaySecondario= ((Session) ht.get(chiave)).getRelaySecondario();
			
			
		}
		else{
			relaySecondario="null";
		}
		session=session+"_"+relaySecondario;
		
		
		//session = session;
	}// fine while
	//session.replaceFirst("_", "");
//	session = session.substring(0, session.length()-1);
	System.err.println("IN SESSION_INFO: "+session);
	return session;
}

}