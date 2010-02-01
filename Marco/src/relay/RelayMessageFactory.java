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

import parameters.MessageCodeConfiguration;



public class RelayMessageFactory {


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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.FORWARD_REQ_FILE+"_"+clientAddr+"_"+filename+"_"+controlProxyPort+"_"+proxyStreamingInPort;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_CLIENT_REQ+"_"+proxyOutStreaming+"_"+proxyCtrl;
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
		String content = 0+"_"+MessageCodeConfiguration.WHO_IS_RELAY;
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
		String content = 0+"_"+MessageCodeConfiguration.IM_RELAY+"_"+relayAddress;
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
		String content = 0+"_"+MessageCodeConfiguration.ELECTION_REQUEST;
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
	static public DatagramPacket buildElectionDone(int sequenceNumber, String newRelayAddress,InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ELECTION_DONE+"_"+newRelayAddress;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.EM_EL_DET_RELAY;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.EM_ELECTION+"_"+W;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_SESSION;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.SESSION_INFO+session;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.ACK_SESSION+proxyInfo;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.REDIRECT;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.LEAVE;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.REQUEST_RSSI;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.SERVER_UNREACHEABLE;
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
		String content = sequenceNumber+"_"+MessageCodeConfiguration.SESSION_INVALIDATION;
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
