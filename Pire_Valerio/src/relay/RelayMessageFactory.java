package relay;

/**
 * Utility per la costruzione di messaggi di controllo (pacchetti UDP) lato relay
 * @author Leo Di Carlo
 * @version 1.2
 * */


/**
 * i messaggi saranno divisi per categoria: prima si trovano i messaggi del protocollo standard per 
 * le richieste dei file ecc.. Poi quelli utilizzati per il protocollo di elezione ed infine quelli utilizzati per lo 
 * scambio delle sessioni tra vecchio e nuovo relay
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import parameters.Parameters;


public class RelayMessageFactory {
	
//nel RelaySessionManager dovrò stare attento a quale di questi metodi chiamare. Ad esempio: se sono un normale relay e sto salendo verso il server
//chiamerò ad esempio buildForwardRequestList e lo invierò al bigboss, bigboss prenderà il messaggio lo riscriverà (per aggiornare il valore
//di Parameters.RELAY_SESSION_AD_HOC_PORT_IN che è la porta su cui aspetta la risposta) e lo invierà al server, il server risponderà
//sull'indirizzo di bigboss sulla porta specificata con un messaggio del tipo FORWARD_LIST_RESPONSE contenente indirizzo e porta di relay e client
//	

	//Valerio: messaggio di richiesta lista file che bigboss invia al server in caso di catena: client->bigboss->server
	static public DatagramPacket buildRequestList(int sequenceNumber, InetAddress addr, int port, String clientAddress, int clientPort) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		//String content = sequenceNumber+"_"+Parameters.REQUEST_LIST+"_"+Parameters.BIG_BOSS_SESSION_AD_HOC_PORT_IN+"_"+clientAddress+"_"+clientPort;
		String content = sequenceNumber+"_"+Parameters.REQUEST_LIST+"_"+Parameters.RELAY_SESSION_AD_HOC_PORT_IN+"_"+clientAddress+"_"+clientPort;
		
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
		if(relayAddress==null)
			content = sequenceNumber+"_"+Parameters.FORWARD_REQ_LIST+"_"+clientAddress;
		else
			content = sequenceNumber+"_"+Parameters.FORWARD_REQ_LIST+"_"+relayAddress+"_"+clientAddress;
		System.err.println("messaggio inviato dal relay al bigboss "+content);
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
		String content = sequenceNumber+"_"+Parameters.LIST_RESPONSE+"_"+listaFile;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}
	
	//Valerio: messaggio con la lista file che il server invia al bigboss in caso di catena: server->bigboss->relay->client
	//questo messaggio è lo stesso che viene inviato dal bigboss al relay
	//il server metterà come addr l'indirizzo da cui ha ricevuto la richiesta della lista e come port qeulla specificata nella richiesta
	//il bigboss metterà come addr lo stesso valore di relayAddress e come port lo stesso valore di relayPort
	static public DatagramPacket buildForwardListResponse(int sequenceNumber, InetAddress addr, int port,String clientAddress, String listaFile) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = null;
		content = sequenceNumber+"_"+Parameters.FORWARD_LIST_RESPONSE+"_"+null+"_"+clientAddress+"_"+listaFile;//+"_"+relayAddress+"_"+relayPort+"_"+clientAddress+"_"+clientPort;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}
	
	//Valerio: messaggio richiesta file che bigboss invia al server in caso di catena: client->bigboss->server
	static public DatagramPacket buildReqFile(int sequenceNumber, String filename, int controlBigBossPort,int bigbossStreamingInPort, String clientAddress, int controlPortClient, int streamingPortClient,InetAddress serveraddr, int serverport) throws IOException {
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.REQUEST_FILE+"_"+filename+"_"+controlBigBossPort+"_"+bigbossStreamingInPort+"_"+clientAddress+"_"+controlPortClient+"_"+streamingPortClient;
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
		String content = sequenceNumber+"_"+Parameters.FORWARD_REQ_FILE+"_"+filename+"_"+bigbossControlPort+"_"+bigbossStreamingInPort+"_"+relayAddress+"_"+controlPortRelay+"_"+streamPortRelay+"_"+clientAddress+"_"+streamingPortClient;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, serveraddr, serverport);
	}
	
	/**
	 * Pacchetto utilizzato come messaggio di buildNoParamAckResponce
	 * @param sequenceNumber
	 * @param addr indirizzo verso il quale inviare il messaggio di ack
	 * @param port porta di ricezione del megasggio di ack
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildNoParamAckResponce(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ACK;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

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
	static public DatagramPacket buildForwardReqFile(int sequenceNumber, String clientAddr, String filename, int controlProxyPort,int proxyStreamingInPort, InetAddress serveraddr, int serverport) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.FORWARD_REQ_FILE+"_"+clientAddr+"_"+filename+"_"+controlProxyPort+"_"+proxyStreamingInPort;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, serveraddr, serverport);

	}



	/**
	 * Pacchetto utilizzato come messaggio di acknoledge
	 * @param sequenceNumber
	 * @param addr indirizzo verso il quale inviare il messaggio di ack
	 * @param port porta di ricezione del megasggio di ack
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildAckClientReq(int sequenceNumber, int port,InetAddress addr, int proxyOutStreaming, int proxyCtrl) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ACK_CLIENT_REQ+"_"+proxyOutStreaming+"_"+proxyCtrl;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}



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
		String content = sequenceNumber+"_"+Parameters.START_TX;
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
		String content = sequenceNumber+"_"+Parameters.STOP_TX;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}






	/**
	 * Messaggio di who is relay
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildWhoIsRelay(InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+Parameters.WHO_IS_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}


	/**
	 * Messaggio di ImRelay
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildImRelay(String relayAddress, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+Parameters.IM_RELAY+"_"+relayAddress;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}


	/**
	 * Messaggio di ElectionRequest
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildElectionRequest(InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+Parameters.ELECTION_REQUEST;
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
		String content = sequenceNumber+"_"+Parameters.ELECTION_RESPONSE+"_"+W;
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
	static public DatagramPacket buildElectionDone(int sequenceNumber, String newRelayAddress,InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ELECTION_DONE+"_"+newRelayAddress;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}

	/**
	 * Messaggio di buildEmElDetRelay
	 * @param newRelayAddress
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildEmElDetRelay(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.EM_EL_DET_RELAY;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}



	/**
	 * Messaggio di buildEmElection
	 * @param sequenceNumber
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildEmElection(int sequenceNumber, double W,InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.EM_ELECTION+"_"+W;
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
	static public DatagramPacket buildRequestSession(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.REQUEST_SESSION;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}


	/**
	 * Messaggio di SessionInfo - manda al nuovo relay le caratterisitiche delle connessioni in corso
	 * @param newRelayAddress
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildSessionInfo(int sequenceNumber, Hashtable sessionInfo, InetAddress addr, int port) throws IOException {

		String session="";
		if(sessionInfo!=null){
			session = getSession(sessionInfo);
		}
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.SESSION_INFO+session;
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
	static public DatagramPacket buildAckSession(int sequenceNumber, String proxyInfo, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ACK_SESSION+proxyInfo;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}

	/**
	 * Messaggio di Redirect
	 * @param newRelayAddress
	 * @param addr
	 * @param port
	 * @return
	 * @throws IOException
	 */
	static public DatagramPacket buildRedirect(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.REDIRECT;
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
	static public DatagramPacket buildLeave(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.LEAVE;
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
	static public DatagramPacket buildRequestRSSI(int sequenceNumber, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.REQUEST_RSSI;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}
	
	
	/*
	 * AGGIUNTO DA CARLO 4-12-2008 13.50
	 */
	/**
	 * Messaggio di ServerUnreacheable
	 * 
	 * @param sequenceNumber
	 * @param clientAddr
	 * @param port
	 * @return
	 * @throws IOException 
	 */
	static public DatagramPacket buildServerUnreacheable(int sequenceNumber, InetAddress clientAddr, int port) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.SERVER_UNREACHEABLE;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, clientAddr, port);
	}


	/**
	 * Messaggio di SessionUnreacheable
	 * 
	 * @param sequenceNumber
	 * @param clientAddr
	 * @param port
	 * @return
	 * @throws IOException 
	 */
	static public DatagramPacket buildSessionInvalidation(int sequenceNumber, InetAddress clientAddr, int port) throws IOException{
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.SESSION_INVALIDATION;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, clientAddr, port);
	}
	
	
	private static String getSession(Hashtable sessionInfo)
	{
		String session = "";
		String chiave;
		Hashtable ht = sessionInfo;
		Enumeration keys = ht.keys();
		while(keys.hasMoreElements())
		{
			chiave = keys.nextElement().toString();
			//session.concat("_"+chiave);
			session = session+"_"+chiave;

			int[] values =(int[]) ht.get(chiave);
			for(int i = 0; i<values.length;i++)
			{
				System.out.println("Porta "+ i+":..."+ values[i]);
				//session.concat("_"+values[i]);
				session = session +"_"+ values[i];
			}
			//session = session;
		}
		//session.replaceFirst("_", "");
	//	session = session.substring(0, session.length()-1);
		System.err.println("IN SESSION_INFO: "+session);
		return session;
	}
}
