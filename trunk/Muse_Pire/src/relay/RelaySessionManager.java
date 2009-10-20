/**
 * 
 */
package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

//import javax.media.NoDataSourceException;

import debug.DebugConsole;

import parameters.Parameters;
import relay.connection.RelayConnectionFactory;
import relay.connection.RelaySessionCM;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutAckSessionInfo;
import relay.timeout.TimeOutSessionInfo;
import relay.timeout.TimeOutSessionRequest;
import server.ServerMessageReader;
import server.ServerSessionManager;
//import server.StreamingServer;
//import util.Logger;

/**
 * @author Leo Di Carlo
 *
 */
public class RelaySessionManager implements Observer{
	private RelaySessionStatus status;
	private int numberOfSession;
	private Hashtable<String, Proxy> pReferences;
	private Hashtable<String, int[]> sessionInfo;
	private DatagramPacket message;
	public static final RelaySessionManager INSTANCE = new RelaySessionManager();
	private boolean imRelay;
	private boolean imBigBoss;
	private String clientAddress;
	//numero di ritrasmissioni del pacchetto REQUEST_SESSION prima di scatenare un elezione d'emergenza
	private int numberOfRetrasmissions = 1;
	private String maxWnextRelay;
	private String event;
	private TimeOutSessionRequest toSessionRequest;
	private TimeOutAckSessionInfo toAckSessionInfo;
	private TimeOutSessionInfo toSessionInfo;
	private RelaySessionCM sessionCM;
	private RelayMessageReader messageReader;
	private DebugConsole consolle;
	private String relayAddress;
	private RelayElectionManager electionManager;
	
	//stati in cui si può trovare il RelayElectionManager
	public enum RelaySessionStatus {  
		IDLE_BIGBOSS,
		ACTIVE_BIGBOSS,
		ATTENDIGACKSESSION,
		IDLE_NORMAL,
		ACTIVE_NORMAL,
		WAITING,
		NOTHING
		}
	
	/**
	 * @param electionManager the electionManager to set
	 */
	public void setElectionManager(RelayElectionManager electionManager) {
		this.electionManager = electionManager;
	}

	public RelaySessionManager()
	{
		this.numberOfSession = 0;
		pReferences = new Hashtable();
		sessionInfo = new Hashtable();
		//this.sessionCM = RelayConnectionFactory.getSessionConnectionManager(this);
		this.messageReader = new RelayMessageReader();
		//this.sessionCM.start();
		this.consolle = new DebugConsole();
		this.consolle.setTitle("RELAY SESSION MANAGER DEBUG CONSOLLE");
	}

	public static RelaySessionManager getInstance() {
		return RelaySessionManager.INSTANCE;
	}


	/**
	 * @return the imRelay
	 */
	public boolean isImRelay() {
		return imRelay;
	}
	
	/**
	 * @return the imBigBoss
	 */
	public boolean isImBigBoss() {
		return imBigBoss;
	}

	/**
	 * @param imBigBoss imRelay the imBigBoss, imRelay to set
	 */
	public void setImRelay(boolean imBigBoss,boolean imRelay) {
		this.imRelay = imRelay;
		this.imBigBoss = imBigBoss;
		if(this.imRelay && this.imBigBoss)
			status = RelaySessionStatus.IDLE_BIGBOSS;
		else if(this.imRelay && !this.imBigBoss)
			status = RelaySessionStatus.IDLE_NORMAL;
		else if(!this.imRelay && !this.imBigBoss)
			status = RelaySessionStatus.WAITING;
		else
			status = RelaySessionStatus.NOTHING;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public synchronized void update(Observable receiver, Object arg) {}
//		// TODO Auto-generated method stub
//		Proxy proxy;
//		/**
//		 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 * +++++++++++++++++++MESSAGGI RICEVUTI ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 */
//		System.out.println("Tipo di Classe dell'arg: "+ arg.getClass());
//		if(arg instanceof DatagramPacket)
//		{
//			/**
//			 * un messaggio è appena arrivato e richiamo il reader per la lettura dello stesso
//			 */
//			this.message = (DatagramPacket) arg;
//			try {
//				messageReader.readContent(this.message);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				System.err.println("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
//				//Logger.write("RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
//				consolle.debugMessage(Parameters.DEBUG_ERROR,"RELAY_SESSION_MANAGER: Errore nella lettura del Datagramma");
//				e.printStackTrace();
//			}
//
//			/**
//			 * arrivato messaggio di richiesta da parte del client
//			 */
//			if(this.messageReader.getCode() == Parameters.REQUEST_FILE && imRelay)
//			{
//				this.status=(this.imBigBoss)? RelaySessionStatus.ACTIVE_BIGBOSS:RelaySessionStatus.ACTIVE_NORMAL;
//				this.clientAddress = message.getAddress().getHostAddress();
//				consolle.debugMessage(Parameters.DEBUG_INFO,"RELAY_SESSION_MANAGER: Arrivata la richiesta di "+messageReader.getFilename()+" da "+ this.clientAddress);
//				//occhio qui a creare i  proxy ce ne sono di due tipi
//				//1 collegato col nodo server e quindi alla rete managed -> BIG BOSS
//				//2 collegato con un altro nodo relay e quindi alla rete ad hoc -> Relay secondario
//				proxy = new Proxy(this, true, messageReader.getFilename(), this.clientAddress, messageReader.getPortStreamingClient());
//
//				pReferences.put(this.clientAddress, proxy);
//				this.numberOfSession++;
//			}
//
//			/**
//			 * gestito l'arrivo della richiesta di passaggio della sessione da parte del nuovo relay appena eletto
//			 */
//			if(messageReader.getCode() == Parameters.REQUEST_SESSION && this.status.equals("Active") )
//			{
//				if(toSessionRequest!=null) // viene disattivato il timeout request session: il messaggio è arrivato
//					toSessionRequest.cancelTimeOutSessionRequest();
//				consolle.debugMessage(Parameters.DEBUG_INFO,"RELAY_SESSION_MANAGER: ricevuto SESSION_REQUEST dal nuovo RELAY");
//				try {
//					if(this.sessionInfo != null || !this.sessionInfo.isEmpty())
//					{
//						this.message = RelayMessageFactory.buildSessionInfo(0, sessionInfo, InetAddress.getByName(this.maxWnextRelay), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//					}
//					else{this.message = RelayMessageFactory.buildSessionInfo(0, null, InetAddress.getByName(this.maxWnextRelay), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);}
//
//					this.sessionCM.sendTo(this.message);
//					this.status = "AttendingAckSession";
//					this.toAckSessionInfo = RelayTimeoutFactory.getTimeOutAckSessionInfo(this, Parameters.TIMEOUT_ACK_SESSION_INFO);
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			if(messageReader.getCode() == Parameters.ACK_SESSION && this.status.equals("AttendingAckSession"))
//			{
//				if(this.toAckSessionInfo!=null)
//					toAckSessionInfo.cancelTimeOutAckSessionInfo();
//				//consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto ACK_SESSION dal nuovo RELAY");
//				// TODO SETTAGGIO SU TUTTI I PROXY DI TUTTE LE PORTE SU CUI TRASMETTERE I FRAME NEI BUFFER LOCALI
//				this.changeProxySession(messageReader.getProxyInfo());
//			}
//			if(messageReader.getCode() == Parameters.SESSION_INFO && this.status.equals("RequestingSession"))
//			{
//				this.toSessionInfo.cancelTimeOutSessionInfo();
//				//consolle.debugMessage("RELAY_SESSION_MANAGER: Ricevuto SESSION_INFO dal vecchio RELAY");
//				this.sessionInfo = messageReader.getSessionInfo();
//				String proxyInfo = this.createProxyFromSession(sessionInfo);
//				try {
//					this.message = RelayMessageFactory.buildAckSession(0, proxyInfo, InetAddress.getByName(this.relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//					this.sessionCM.sendTo(this.message);
//					this.status = "Active";
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//
//		}
//		/**
//		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 * ++++++++++++++++++++++++++++++EVENTI ARRIVATI -- STRINGHE ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//		 */
//		if(arg instanceof String)
//		{
//			this.event = (String)arg;
//			if(this.event.equals("End_Of_Media") && status.equals("Active") && imRelay)
//			{
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di END_OF_MEDIA da parte di un proxy");
//				proxy = (Proxy)receiver;
//				//rimuovo i riferimenti della sessione all'interno delle relative hashtable 
//				pReferences.remove(proxy.getClientAddress());
//				sessionInfo.remove(proxy.getClientAddress());
//				this.numberOfSession--;
//				if(numberOfSession == 0)
//					this.status = "Idle";
//			}
//			
//			/**
//			 * l'electionmanager mi ha comunica chi è il relay attuale a seguito di un messaggio di WHO_IS_RELAY
//			 */
//			if(this.event.contains("RELAY_FOUND"))
//			{
//				StringTokenizer st = new StringTokenizer(this.event, ":");
//				st.nextToken();
//				this.relayAddress = st.nextToken();
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di RELAY_FOUND, il RELAY attuale è: "+this.relayAddress);
//			}
//			
//			if(this.event.equals("TIMEOUTSESSIONREQUEST") || this.event.equals("TIMEOUTACKSESSIONINFO")) //VECCHIO RELAY
//			{
//				if(this.event.equals("TIMEOUTSESSIONREQUEST") && status.equals("Active"))
//					consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_REQUEST");
//				if(this.event.equals("TIMEOUTACKSESSIONINFO") && status.equals("AttendingAckSession"))
//					consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_ACK_SESSION_INFO");
//				//TODO DEVO AVVISARE IL RELAYELECTIONMANAGER
//				if(this.electionManager!=null)
//				{
//					electionManager.chooseAnotherRelay();
//				}
//
//			}
//			
//			/**
//			 * l'electionmanager mi comunica chi è il relay appena eletto
//			 */
//			if(this.event.contains("NEW_RELAY"))
//			{
//				StringTokenizer st = new StringTokenizer(this.event, ":");
//				st.nextToken();
//				String newRelay = st.nextToken();
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
//				System.out.println("RELAY_SESSION_MANAGER: Evento di NEW_RELAY, il nuovo RELAY è "+newRelay);
//				if(imRelay)
//				{
//					this.maxWnextRelay = newRelay;
//					this.imRelay = false;
//					this.toSessionRequest = RelayTimeoutFactory.getTimeOutSessionRequest(this, Parameters.TIMEOUT_SESSION_REQUEST);
//				}
//				else
//				{
//					try {
//						if(InetAddress.getByName(Parameters.RELAY_AD_HOC_ADDRESS).getHostAddress().equals(newRelay))
//						{
//							this.imRelay = true;
//							this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//							this.sessionCM.sendTo(message);
//							this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, Parameters.TIMEOUT_SESSION_INFO);
//							this.status = "RequestingSession";
//						}
//					} catch (UnknownHostException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//			if(this.event.contains("NEW_EM_RELAY"))
//			{
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Evento di NEW_EM_RELAY");
//				StringTokenizer st = new StringTokenizer(this.event, ":");
//				st.nextToken();
//				String newRelay = st.nextToken();
//				try {
//					if(InetAddress.getLocalHost().toString().equals(newRelay))
//					{
//						this.imRelay = true;
//						this.status = "Active";
//					}
//					else
//					{
//						this.relayAddress = newRelay;
//						this.status = "Waiting";
//					}
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			
//			if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions != 0 && this.status.equals("RequestingSession"))
//			{
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO");
//				try {
//					this.message = RelayMessageFactory.buildRequestSession(0, InetAddress.getByName(this.relayAddress), Parameters.RELAY_SESSION_AD_HOC_PORT_IN);
//					this.sessionCM.sendTo(message);
//					this.toSessionInfo = RelayTimeoutFactory.getTimeOutSessionInfo(this, Parameters.TIMEOUT_SESSION_INFO);
//					this.numberOfRetrasmissions--;
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//			}
//			if(this.event.equals("TIMEOUTSESSIONINFO") && this.numberOfRetrasmissions == 0)
//			{
//				consolle.debugMessage("RELAY_SESSION_MANAGER: Scattato il TIMEOUT_SESSION_INFO e numero di ritrasmissioni a 0");
//				this.status = "Waiting";
//				//Invio il messaggio di invalidazione della sessione ai client che conoscono l'identità del nuovo relay ma non si ha modo di recuperare la sessione
//				try {
//					this.message = RelayMessageFactory.buildSessionInvalidation(0, InetAddress.getByName(Parameters.BROADCAST_ADDRESS), Parameters.CLIENT_PORT_SESSION_IN);
//					this.sessionCM.sendTo(this.message);
//					this.status = "Active";
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//		/**
//		 * il proxy dopo aver creato la sessione ha il compito di avvertire il sessionmanager
//		 * ++++++++++++++++++++++++++EVENTO SOLLEVATO DAI PROXY++++++++++++++++++++++++++++++
//		 */
//		if(arg instanceof int[])
//		{
//			//CONTROLLARE SE il seguente cast è giusto altrimenti si cambia...
//			System.out.println("Il Proxy mi ha notificato i parametri di sessione...");
//			
//			int[] sessionPorts = (int[])arg;
//			System.err.println("RelaySessionManager- Porta di Stream del server: " +sessionPorts[0]);
//			System.err.println("RelaySessionManager- Porta di Stream del relay IN: " +sessionPorts[1]);
//			System.err.println("RelaySessionManager- Porta di Stream del relay OUT: " +sessionPorts[2]);
//			System.err.println("RelaySessionManager- Porta di Stream del Client IN: " +sessionPorts[3]);
//			System.err.println("RelaySessionManager- Porta di Stream del Server control: " +sessionPorts[4]);
//			System.err.println("RelaySessionManager- Porta di Stream del Proxy control: " +sessionPorts[5]);
//			this.sessionInfo.put(((Proxy)receiver).getClientAddress(), sessionPorts);
//			System.out.println("AGGIUNTO IL RIFERIMENTO IN SESSION_INFO");
//		}
//	}


	/**
	 * @return the maxWnextRelay
	 */
//	public String getMaxWnextRelay() {
//		return maxWnextRelay;
//	}
//
//	/**
//	 * @param maxWnextRelay the maxWnextRelay to set
//	 */
//	public void setMaxWnextRelay(String maxWnextRelay) {
//		this.maxWnextRelay = maxWnextRelay;
//	}
//
//	public void updateSessions()
//	{
//		if(this.numberOfSession != 0)
//		{
//			numberOfSession--;
//			if(this.numberOfSession == 0)
//				this.status = "Idle";
//		}
//	}
//	private String createProxyFromSession(Hashtable sessionInfo)
//	{
//		Proxy proxy;
//		String chiave;
//		String recStreamInports = "";
//		int serverPortStreamOut = 0;
//		int proxyPortStreamIn = 0;
//		int proxyPortStreamOut = 0;
//		int clientPortStreamIn = 0;
//		int serverCtrlPort = 0;
//		int proxyCtrlPort = 0;
//		if(!sessionInfo.isEmpty())
//		{
//			Enumeration keys = sessionInfo.keys();
//			while(keys.hasMoreElements())
//			{
//				chiave = keys.nextElement().toString();
//				int[] values =(int[]) sessionInfo.get(chiave);
//				if(values.length == 6)
//				{
//					serverPortStreamOut = values[0];
//					proxyPortStreamIn = values[1];
//					//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
//					RelayPortMapper.getInstance().setRangePortInRTPProxy(proxyPortStreamIn);
//					proxyPortStreamOut = values[2];
//					//Notifica al portmapper di occupare le porte in maniera sia garantita la loro coerenza
//					RelayPortMapper.getInstance().setRangePortOutRTPProxy(proxyPortStreamOut);
//					clientPortStreamIn = values[3];
//					serverCtrlPort = values[4];
//					proxyCtrlPort = values[5];
//				}
//				try {
//					/**
//					 * nel costruttore del proxy è stato inserito il valore di proxy PortStreamOut in 2 punti diversi proprio perchè il vecchio proxy quando avvia
//					 * la trasmissione di recovery verso il nuovo riutilizza la medesima porta.
//					 */
//					proxy = new Proxy(this, false, chiave, clientPortStreamIn, proxyPortStreamOut, proxyPortStreamIn, serverPortStreamOut, proxyPortStreamOut ,InetAddress.getByName(this.relayAddress), serverCtrlPort, proxyCtrlPort);
//					recStreamInports = recStreamInports+"_"+chiave+"_"+proxy.getRecoveryStreamInPort();
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}	
//		}
//	//	recStreamInports.replaceFirst("_", "");
//	//	recStreamInports = recStreamInports.substring(arg0, arg1)
//		return recStreamInports;
//	}
//	
//	private void changeProxySession(Hashtable sessionEndpoint)
//	{
//		String chiave;
//		if(!sessionEndpoint.isEmpty())
//		{
//			if(!pReferences.isEmpty())
//			{
//				Enumeration keys = sessionEndpoint.keys();
//				while(keys.hasMoreElements())
//				{
//					chiave = keys.nextElement().toString();
//					int[] values =(int[]) sessionEndpoint.get(chiave);
//					try {
//						pReferences.get(chiave).startHandoff(values[0], InetAddress.getByName(this.maxWnextRelay));
//						pReferences.get(chiave).setEnding(true);
//					} catch (UnknownHostException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//	}
}
