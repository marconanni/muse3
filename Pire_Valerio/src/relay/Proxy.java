/**
 * 
 */
package relay;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.IncompatibleSourceException;

import parameters.BufferConfiguration;
import parameters.ElectionConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;
import parameters.SessionConfiguration;
import parameters.TimeOutConfiguration;

import client.gui.IClientView;



import relay.connection.ProxyCM;
import relay.connection.RelayConnectionFactory;
import relay.connection.RelayPortMapper;
import relay.gui.ProxyFrame;
import relay.gui.ProxyFrameController;
import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.timeout.*;
import unibo.core.BufferEmptyEvent;
import unibo.core.BufferEmptyListener;
import unibo.core.BufferFullEvent;
import unibo.core.BufferFullListener;
import unibo.core.CircularBuffer;
import unibo.core.multiplexer.RTPMultiplexer;
import unibo.core.rtp.RTPSenderPS;
import unibo.core.thread.MultiplexerThreadPS;
import unibo.core.thread.MuseMultiplexerThread;



/**
 * @author Leo Di Carlo, Marco Nanni
 *
 */

/*
 * Marco: la trasmissione dipende dallo stato in cui si trova il proxy, peccato che siano nomi 
 * incomprensibili e difatto non si capisca in che situazione si trova il proxy, e perchè ci sono tanti modi di 
 * inziare una trasmissione verso il client. Se ci fosse un po' di documentazione in giro riuscirei a cavarci i piedi, altrimenti
 * non resta che chiedere a Foschini.
 */


public class Proxy extends Observable implements Observer, BufferFullListener, BufferEmptyListener, ControllerListener{

	//flags
	private boolean debug = true;
	private boolean serverStopped; //indica se si � inviato STOPTX al server
	
	//variabili di stato, eventi, messaggi:
	private ProxyState state;
	private boolean newProxy;
	private String event;
	private DatagramPacket msg;
	
	
	private boolean request_pending;
	//timeout:
	private TimeOutSingleWithMessage timeoutAckForward;
	private TimeOutSingleWithMessage timeoutAckClientReq;
	private TimeOutSingleWithMessage timeoutSessionInterrupted;

	
	//parametri:
	private String proxyID = "-1";
	private String filename;
	private String clientAddress;
	private String streamingServerAddress; //è l'indirizzo di chi manda il flusso al proxy
	private String futureStreamingAddress; // è l'indirizzo del nuovo big boss che manderà il flusso al proxy quando questo riceverà il messaggio
											// di LEAVE dal proxy sul vecchio relay
	int initialSupThreshold = (BufferConfiguration.PROXY_BUFFER*70)/100;
	
	private boolean servingClient; // se true indice che si eroga il flusso al client, se è false
	
	//porte(le porte di ricezione del proxy si trovano dentro l'RTPReceptionManager): 
	private int clientStreamPort = 0; 	//porta su cui il client riceve lo stream rtp 
	private int serverStreamPort = 0; 	//porta da cui lo streamingserver invia lo stream rtp
	private int outStreamPort = 0;		//porta da cui il proxy invia lo stream rtp
	private int inStreamPort = 0;       //porta da cui il proxy riceve lo str 
	private int streamingServerCtrlPort = 0; //porta su cui lo streamingserver riceve i messaggi di controllo
//	private int sessionControlPort;  //porta su cui il proxy riceve i messaggi di controllo dal client durante la trasmissione:
	private int proxyStreamingCtrlPort = 0;
	private int clientStreamControlPort=0; // la porta di controllo del client (� valida e diversa da -1
	// solo se il client � un altro proxy
	
	// porte di sessione di sender e receiver
	private int streamingServerSessionPort;
	private int clientSessionPort;
	
	//componenti:
	private ProxyCM proxyCM;
	private RTPReceptionManager rtpReceptionMan; 
	private RelayBufferManager buffer;
	private RTPSenderPS rtpSender;
	private RTPMultiplexer rtpMux;
	private MuseMultiplexerThread muxTh;
	private boolean ending = false;
	
	
	
	
	private String relayAddress;
	private int relayControlPort;
	private int relayStreamingPort;	
	private int bigbossStreamOut;
	private int bigbossControlPort;
	private boolean isBigBoss;
	private String connectedClusterHeadAddr;
	private String localClusterHeadAddr;
	private String localClusterAddress;
	private String oldProxyAddress;
	
	// variabili usate pre fare i test	
	private long startTime =0;
	
	
	
	/**
	 * @return the ending
	 */
	public boolean isEnding() {
		return ending;
	}

	/**
	 * @param ending the ending to set
	 */
	public void setEnding(boolean ending) {
		this.ending = ending;
	}

	/**
	 * @return the muxTh
	 */
	public MuseMultiplexerThread getMuxTh() {
		return muxTh;
	}

	/**
	 * @return the muxThR
	 */
	public MuseMultiplexerThread getMuxThR() {
		return muxThR;
	}

	private MuseMultiplexerThread muxThR;
	private int recoveryStreamInPort;
	
	//classi di utilita':
	private RelayMessageReader msgReader;
	private Observer sessionManager;
	
	//INTERFACCIA GRAFICA 	
	private ProxyFrame fProxy;
	/*
	 * ***************************************************************
	 * **********************COSTRUTTORI******************************
	 * ***************************************************************
	 */
	
	/**
	 * Costruisce un new proxy, cioe' un proxy che si ocupa di gestire una nuova richiesta e non necessita
	 * di effettuare la fase di recovery. 
	 * 
	 * @param newProxy - true se e'un new relay; false altrimenti
	 * @param filename
	 * @param clientAddress
	 * @param clientStreamPort
	 */
	
	public Proxy(Observer sessionManager,boolean newProxy, String filename,String relayAddress, int relayControlPort,int relayStreamPort, String clientAddress, int clientStreamPort, boolean isBigBoss, boolean servingClient, String localClusterHeadAddr, String connectedClusterHeadAddr) {
		
		this.connectedClusterHeadAddr=connectedClusterHeadAddr;
		this.localClusterHeadAddr=localClusterHeadAddr;
		
		this.servingClient=servingClient;
		
		this.fProxy = new ProxyFrame();
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		this.serverStopped = true;
		
		this.newProxy = newProxy;
		this.filename = filename; 
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		
		this.msgReader = new RelayMessageReader();
		
		this.relayStreamingPort=relayStreamPort;
		this.isBigBoss=isBigBoss;
		this.relayAddress=relayAddress;
		this.relayControlPort=relayControlPort;
		//proxy connection manager, e' osservato da this
		this.proxyCM = RelayConnectionFactory.getProxyConnectionManager(this);
		this.proxyCM.start();
		
		
		System.err.println("isBibBoss="+this.isBigBoss+", servingClient="+this.servingClient);
		
		try {
			//relay buffer manager, this e' un listener per gli eventi sollevati dal buffer nominale
			buffer = new RelayBufferManager(BufferConfiguration.PROXY_BUFFER, this.fProxy.getController(), BufferConfiguration.PROXY_SOGLIA_INFERIORE_NORMAL,BufferConfiguration.PROXY_SOGLIA_INFERIORE_ELECTION,BufferConfiguration.PROXY_SOGLIA_SUPERIORE_NORMAL,BufferConfiguration.PROXY_SOGLIA_SUPERIORE_ELECTION,this);
			//buffer.getNormalBuffer().addBufferEmptyEventListener(this);
			//buffer.getNormalBuffer().addBufferFullEventListener(this);
			
			this.rtpReceptionMan = new RTPReceptionManager(newProxy, buffer, this, isBigBoss,this.localClusterHeadAddr,this.connectedClusterHeadAddr);
			
			this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		} catch (IncompatibleSourceException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}

		if(this.isBigBoss && this.servingClient){//client->bigboss
			System.out.println("Proxy: mando la richiesta file al server");
			sendReqFileToServer();
			}
		else{
			if(!this.isBigBoss && this.servingClient){//client->relay
				//se non è un proxy di bigboss dovrò fare un altro metodo 
				System.out.println("Proxy: mando la richiesta file al BigBoss");
				sendReqFileToBigBoss();
			}
			else{
				if(this.isBigBoss && !this.servingClient){//relay->bigboss
					System.out.println("Proxy:mando la richiesta file al BigBoss");
					sendForwardReqFileToServer();
				}
			}
		}
		
		//imposto il timeout
		this.timeoutAckForward = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_ACK_FORWARD,"TIMEOUTACKFORWARD");
		//transito nello stato di WaitingServerRes 
		this.state = ProxyState.waitingServerRes;
		fProxy.getController().debugMessage(this.state.name());
		System.err.println(this.state.name());
		System.out.println("fatto (1).");
		
		//mi sospendo in attesa della risposta del Server
		proxyCM.waitStreamingServerResponse();
	}
	
	/**
	 * @return the recoveryStreamInPort
	 */
	public int getRecoveryStreamInPort() {
		return recoveryStreamInPort;
	}
	
	

	/**
	 * 
	 * @param sessionManager il sessionManger 
	 * @param newProxy false, questo è un proxy sostitutivo
	 * @param clientAddress l'indirizzo del client finale
	 * @param secondaryRelayAddress l'indirizzo superiore dell'evenutale relay secondario
	 * @param clientStreamPort la porta dalla quale il ricevente riceve il flusso
	 * @param proxyStreamPortOut la porta dalla quale il proxy eroga il flusso
	 * @param proxyStreamPortIn la porta dalla quale il proxy riceve il flusso
	 * @param serverStreamPort la porta dalla quale chi manda il fusso al proxy invia tale flussso
	 * @param recoverySenderOutPort la porta dalla quale il vecchio proxy manda il flusso
	 * @param recoverySenderInPort la porta dalla quale il vecchio proxy riceveva il flusso: devo aprire in ricezione la stessa porta
	 * @param recoverySenderlocalClusterAddress l'indirizzo inferiore dall quale il vecchio proxy invia il flusso
	 * @param serverCtrlPort la porta di controllo di chi invia il flusoo
	 * @param proxyCtrlPort la porta di controllo di questo proxy
	 * @param localClusterHeadAddr l'indirizzo inferiore di questo proxy
	 * @param connectedClusterHeadAddr l'indirizzo superiore di questo proxy
	 * @param servingClient true il proxy invia direttamente il flusso ad un client, false se invia ad un relay secondario
	 */
	public Proxy(Observer sessionManager, boolean newProxy, String clientAddress, String secondaryRelayAddress, int clientStreamPort, int proxyStreamPortOut, int proxyStreamPortIn, int serverStreamPort, int recoverySenderOutPort,int recoverySenderInPort, String recoverySenderlocalClusterAddress,  int serverCtrlPort, int proxyCtrlPort, String localClusterHeadAddr,String localClusterAddress,String connectedClusterHeadAddr, boolean servingClient, boolean isBigBoss){
//	TODO: controllare che le porte siano tutte ben mappate
		
		this.localClusterAddress = localClusterAddress;
		this.localClusterHeadAddr=localClusterHeadAddr;
		this.connectedClusterHeadAddr=connectedClusterHeadAddr;
		this.relayAddress= secondaryRelayAddress;
		
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		this.serverStopped = false;
		this.newProxy = newProxy;
		this.filename = ""; 
		this.clientAddress = clientAddress;
		System.err.println("cllient----------------------------------------------------------------------------------------------"+this.clientAddress);
		this.clientStreamPort = clientStreamPort;
		this.serverStreamPort = serverStreamPort;
		this.outStreamPort = proxyStreamPortOut;
		this.inStreamPort = proxyStreamPortIn;
		
			this.streamingServerCtrlPort = serverCtrlPort;
	
		this.proxyStreamingCtrlPort = proxyCtrlPort;
		System.out.println("Inizializzazione del proxy in corso...");
		this.oldProxyAddress = recoverySenderlocalClusterAddress;
		this.msgReader = new RelayMessageReader();
		this.servingClient = servingClient;		

		
		this.isBigBoss=isBigBoss;
		this.fProxy = new ProxyFrame();
		//proxy connection manager, e' osservato da this
		this.proxyCM = RelayConnectionFactory.getProxyConnectionManager(this, this.proxyStreamingCtrlPort);
		this.proxyCM.start();
		
		try {
			
			buffer = new RelayBufferManager(BufferConfiguration.PROXY_BUFFER, this.fProxy.getController(), BufferConfiguration.PROXY_SOGLIA_INFERIORE_NORMAL,BufferConfiguration.PROXY_SOGLIA_INFERIORE_ELECTION,BufferConfiguration.PROXY_SOGLIA_SUPERIORE_NORMAL,BufferConfiguration.PROXY_SOGLIA_SUPERIORE_ELECTION,this);
			//creo un rtpreceptionamanger
			this.rtpReceptionMan = new RTPReceptionManager(false, buffer, oldProxyAddress, recoverySenderInPort, this.localClusterHeadAddr,this.localClusterAddress, this.connectedClusterHeadAddr, this);
			
			//imposto la porta da cui il server invia lo stream 
			this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
			
			//imposto la porta di ricezione di recovey
			//this.rtpReceptionMan.setNormalReceivingPort(proxyStreamPortIn);// Marco: perchè è commentato?
		} catch (IncompatibleSourceException e) {System.out.println(e.getMessage());
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {System.out.println(e.getMessage());
			// Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 * Questo thread riceve il flusso dal server
		 * Nota: dentro al metodo intNormalConnection c'è un riferimento statico all'indirizzo del server
		 * se non arriva niente il thred si blocca, visto che dentro il metodo 
		 */
		
		Thread runner1 = new Thread(){public void run(){try {
			rtpReceptionMan.initNormalConnection();
			System.err.print("Apertura ricezione normale in corso...");
			rtpReceptionMan.startNormalConnection();
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IncompatibleSourceException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}}};
		runner1.start();
		
		this.sendRedirect();
		
		/*
		 * Ho un'altro thread in ricezione: quello che riceve il flusso dal vecchio relay
		 */
	
		Thread runner2 = new Thread(){public void run(){try {
			final InetAddress address = InetAddress.getByName(getOldProxyAddress());
			System.out.println("+++++++ Ricezione recovery da "+address.toString()+":"+getOutStreamPort());
			rtpReceptionMan.initRecoveryConnection(getOutStreamPort(), address);
			System.err.print("Apertura ricezione recovery in corso...");
			rtpReceptionMan.startRecoveryConnection();
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IncompatibleSourceException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}}};
		runner2.start();
		
		this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		//inizializzo la sessione di recovery (quella tra old proxy e new proxy)
//		this.initRecoverySession(recoverySenderPort, recoverySenderAddress); // Marco: non ci sto capendo niente: perchè è commentata?
		
		//inizializzo la sessione normale (quella tra il new proxy e il client)
		this.initNormalSession();
		//avvio la ricezione nominale 
//		boolean flag = rtpReceptionMan.startNormalConnection();
		
		//avvio la ricezione di recovery
		boolean flag2 = rtpReceptionMan.startRecoveryConnection();
		System.err.println("+++++++ fatta startRecoveryConnection");
		System.err.println("+++++++++ track formats 2" + rtpReceptionMan.getTrackFormats2());
		/*
		 * Marco: prova: non so se va bene, ma mando lo start TX al vecchio relayoldProxyAddress
		 */
		


		//transito nel nuovo stato 
		this.state = ProxyState.receivingRetransmission;
		fProxy.getController().debugMessage(this.state.name());
		System.err.println(this.state.name());
		System.out.println("fatto (2).");
	}

	
	/*
	 * ***************************************************************
	 * *******************UPDATERs&LISTENERs**************************
	 * ***************************************************************
	 */
	
	/**
	 * @return the clientStreamPort
	 */
	public int getClientStreamPort() {
		return clientStreamPort;
	}

	/**
	 * @return the serverStreamPort
	 */
	public int getServerStreamPort() {
		return serverStreamPort;
	}

	/**
	 * @return the outStreamPort
	 */
	public int getOutStreamPort() {
		return outStreamPort;
	}
	
	public String getOldProxyAddress(){
		return oldProxyAddress;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	
	public  synchronized void  update(Observable o, Object arg) {
		
		
		if(arg instanceof String){
			event = (String)arg;
			
			if (debug)
				System.out.println("Proxy: evento:" + arg);
			
			if (event.equals("TIMEOUTSESSIONINTERRUPTED"))
			{
				this.endProxy();
			}
			if (event.equals("TIMEOUTACKFORWARD") && this.state == ProxyState.waitingServerRes){
				System.out.println("TimeoutAckFroward scaduto...");
				/**
				 * TODO: stampe sulla relativa intefaccia grafica
				 */
				sendServerUnreacheableToClient();
				endProxy();
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
			} 
			else if(event.equals("TIMEOUTACKCLIENTREQ") && this.state == ProxyState.waitingClientAck){
				endProxy();	
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
			} 
			
		} else if (arg instanceof DatagramPacket){
			
			this.msg = (DatagramPacket) arg;
			
			if (debug)
				System.out.println("Proxy: evento:" + arg);
								
			//leggo il messaggio:
			try {
				this.msgReader.readContent(msg);
			} catch (IOException e) {
				System.err.println("PROXY (ID: "+ this.clientAddress+"): Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			
			System.err.println("PROXY: sono nell'update  e mi è arrivato un datagramma con codice "+msgReader.getCode());
			
						

			if(msgReader.getCode()==MessageCodeConfiguration.FORWARD_ACK_REQ&&state==ProxyState.waitingServerRes&&isBigBoss){
				System.out.println("arrivato forward ack dal server");
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
				this.serverStreamPort=msgReader.getServerStreamingPort();
				this.streamingServerCtrlPort=msgReader.getServerStreamingControlPort();
				//imposto la porta da cui il server invia lo stream
				this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
				outStreamPort = RelayPortMapper.getInstance().getFirstFreeStreamOutPort(); 
				//invio al client il msg AckClientReq
				this.state = ProxyState.waitingClientAck;
				sendAckFileToRelay(msgReader.getClientAddress(),msgReader.getClientControlPort(),msgReader.getClientStreamingPort());//ancora da implementare:deve mandare al proxy del relay quello che gli serve
				this.proxyCM.start();
			
				/**
				 * Ora ho tutti i dati della sessione pronti ergo posso avvertire il sessionmanager e passarglieli tutti
				 */
				
				System.out.println("inStreamPort "+inStreamPort);
				System.out.println("outStreamPort "+outStreamPort);
				System.out.println("streamingServerControlPort "+streamingServerCtrlPort);
				System.out.println("proxyStreamingCtrlPort "+proxyStreamingCtrlPort);
				System.out.println("clientStreamPort "+clientStreamPort);
				
				if(this.inStreamPort!=0 && this.outStreamPort!=0 && this.serverStreamPort!=0 && this.streamingServerCtrlPort!=0 && this.proxyStreamingCtrlPort!=0 && this.clientStreamPort!=0)
				{
					System.out.println("Avviso il SessionManager con i dati della sessione");
					int[] sessionports = new int[6];
					sessionports[0] = this.serverStreamPort;
					sessionports[1] = this.inStreamPort;
					sessionports[2] = this.outStreamPort;
					sessionports[3] = this.clientStreamPort;
					sessionports[4] = this.streamingServerCtrlPort;
					sessionports[5] = this.proxyStreamingCtrlPort;
					
					for(int i=0;i<sessionports.length;i++)				
						System.out.println(sessionports[i]);
					
					
					System.err.println("$$$$ outStreamPort"+ outStreamPort);
					this.setChanged();
					this.notifyObservers(sessionports);
				}
				//imposto il timeout ack client req:
				this.timeoutAckClientReq = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_ACK_CLIENT_REQ,"TIMEOUTACKCLIENTREQ");
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
				
			}
			if(msgReader.getCode()==MessageCodeConfiguration.FORWARD_ACK_REQ&&state==ProxyState.waitingServerRes&&!isBigBoss){
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
				
				this.serverStreamPort=msgReader.getServerStreamingPort();
				this.streamingServerCtrlPort=msgReader.getServerStreamingControlPort();
				//imposto la porta da cui il server invia lo stream
				this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
				outStreamPort = RelayPortMapper.getInstance().getFirstFreeStreamOutPort(); 
				//invio al client il msg AckClientReq
				this.state = ProxyState.waitingClientAck;
				sendAckClientReqToClient();//manda l'ack al client
				this.proxyCM.start();
			
				/**
				 * Ora ho tutti i dati della sessione pronti ergo posso avvertire il sessionmanager e passarglieli tutti
				 */
				
				System.out.println("inStreamPort "+inStreamPort);
				System.out.println("outStreamPort "+outStreamPort);
				System.out.println("streamingServerControlPort "+streamingServerCtrlPort);
				System.out.println("proxyStreamingCtrlPort "+proxyStreamingCtrlPort);
				System.out.println("clientStreamPort "+clientStreamPort);
				
				if(this.inStreamPort!=0 && this.outStreamPort!=0 && this.serverStreamPort!=0 && this.streamingServerCtrlPort!=0 && this.proxyStreamingCtrlPort!=0 && this.clientStreamPort!=0)
				{
					System.out.println("Avviso il SessionManager con i dati della sessione");
					int[] sessionports = new int[6];
					sessionports[0] = this.serverStreamPort;
					sessionports[1] = this.inStreamPort;
					sessionports[2] = this.outStreamPort;
					sessionports[3] = this.clientStreamPort;
					sessionports[4] = this.streamingServerCtrlPort;
					sessionports[5] = this.proxyStreamingCtrlPort;
					
					for(int i=0;i<sessionports.length;i++)				
						System.out.println(sessionports[i]);
					
					this.setChanged();
					this.notifyObservers(sessionports);
				}
				//imposto il timeout ack client req:
				this.timeoutAckClientReq = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_ACK_CLIENT_REQ,"TIMEOUTACKCLIENTREQ");
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
				
			}
			
			//valerio:il messaggio ACK_REQUEST_FILE può arrivare solo al bigboss
			if(msgReader.getCode()==MessageCodeConfiguration.ACK_REQUEST_FILE&&state==ProxyState.waitingServerRes){
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
				//reset timeout TimeOutAckForward
				this.timeoutAckForward.cancelTimeOutSingleWithMessage();
				//leggo le informazioni contenute nel messaggio
				this.serverStreamPort = msgReader.getServerStreamingPort();
				this.streamingServerCtrlPort = msgReader.getServerStreamingControlPort();
				//imposto la porta da cui il server invia lo stream
				this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
				//inizializzo la sessione
				//initNormalSession();
				outStreamPort = RelayPortMapper.getInstance().getFirstFreeStreamOutPort(); 
				//invio al client il msg AckClientReq
				this.state = ProxyState.waitingClientAck;
				sendAckClientReqToClient();//manda l'ack al client
				this.proxyCM.start();
				
				/**
				 * Ora ho tutti i dati della sessione pronti ergo posso avvertire il sessionmanager e passarglieli tutti
				 */
				
				System.out.println("inStreamPort "+inStreamPort);
				System.out.println("outStreamPort "+outStreamPort);
				System.out.println("streamingServerControlPort "+streamingServerCtrlPort);
				System.out.println("proxyStreamingCtrlPort "+proxyStreamingCtrlPort);
				System.out.println("clientStreamPort "+clientStreamPort);
				
				if(this.inStreamPort!=0 && this.outStreamPort!=0 && this.serverStreamPort!=0 && this.streamingServerCtrlPort!=0 && this.proxyStreamingCtrlPort!=0 && this.clientStreamPort!=0)
				{
					System.out.println("Avviso il SessionManager con i dati della sessione");
					int[] sessionports = new int[6];
					sessionports[0] = this.serverStreamPort;
					sessionports[1] = this.inStreamPort;
					sessionports[2] = this.outStreamPort;
					sessionports[3] = this.clientStreamPort;
					sessionports[4] = this.streamingServerCtrlPort;
					sessionports[5] = this.proxyStreamingCtrlPort;
					
					for(int i=0;i<sessionports.length;i++)				
						System.out.println(sessionports[i]);
					
					this.setChanged();
					this.notifyObservers(sessionports);
					
					
				}
				//imposto il timeout ack client req:
				this.timeoutAckClientReq = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_ACK_CLIENT_REQ,"TIMEOUTACKCLIENTREQ");
				
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
			
			
			}

			if(msgReader.getCode() == MessageCodeConfiguration.START_TX){
				System.err.println("E' arrivato START_TX da "+msgReader.getPacketAddess()+" il mio stato è "+state);
				
				/*
				 * MArco:è arrivato un messaggio START_TX da parte del client
				  come si comporta il proxy dipende dallo stato in cui si trova il proxy, sono che sono molto criptici
				  * non si capisce che cosa significano e quando si verificano...
				*/
				if(this.timeoutSessionInterrupted!=null)
				{
					this.timeoutSessionInterrupted.cancelTimeOutSingleWithMessage();
				}
			
			
				if (state == ProxyState.waitingClientAck){	
					System.err.println("IL MIO STATO È WAITINGCLIENTACK");
					/*
					 * 
					 * Marco: probabilmente questa è la prima volta che mando qualcosa al client, ma non ne sono affatto sicuro
					 *  questo dovrebbe essere il corpo del metodo:
					 * cancello il timeout di attesa della ripresa della trasmissione da parte del client ( se per 10 min non arriva niente, forse il client se ne è andato...)
					 * mando il messaggio di Start TX al server perchè riprenda anche lui la trasmisssione verso di me
					 * faccio partire un thread pr la ricezione dello stream dal server
					 * in realtà la trsmissione non inizia subito: preparo tutto e transito nello stato firstreceivingromServer
					 * ed aspetto che il buffer si riempia: quando il buffer si riempie il proxy riceve l'evento buffer full
					 * e lì che inizio la trasmissione.
					 */
				
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//resetto TimeOutAckClientReq
					this.timeoutAckClientReq.cancelTimeOutSingleWithMessage();
					
					// Marco: in pratica dichiaro qui il thread, ed il codice contenuto nell suo metodo run anzichè farlo in un file separato
					Thread runner = new Thread(){public void run(){try {
						rtpReceptionMan.initNormalConnection();
						System.err.print("Apertura ricezione normale in corso BLABLABLA...");
						rtpReceptionMan.startNormalConnection();
					} catch (UnknownHostException e) {
						// Auto-generated catch block
						e.printStackTrace();
					} catch (IncompatibleSourceException e) {
						// Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// Auto-generated catch block
						e.printStackTrace();
					}}};
					runner.start(); // Marco: poi qui faccio partire il thread ( e quindi metto in esecuzione il metodo run)
					
					initNormalSession(); // Marco: qui dovrei far partire la trasmissione verso il client.
					System.out.println("INITIAL NORMAL SESSION OK");
					if(isBigBoss)
						sendStartTXToServer();
					else
						sendStartTXToBigBoss();

					System.err.println("initNormalSession() FINITO");
				
					//avvio la ricezione

					
					//transito nel nuovo stato
					this.state = ProxyState.FirstReceivingromServer;
					System.err.println(this.state.name());
					
				} else if(state == ProxyState.stopToClient){
					System.err.println("IL MIO STATO È STOPCLIENT");
					/*
					 * Marco: qui ho a tutti gli effetti un'altro modo per mandare lo stream, ma cosa sono sti stati?
					 * lo stato stop to client vuol dire che prima il client ha fermato la trsmissione mandandomi uno stop tx
					 * e a questo punto dovrei riprenderla
					 */
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//resetto il TimeOutSessionInterrupted
					this.timeoutSessionInterrupted.cancelTimeOutSingleWithMessage();
					//invio il msg di startTX al server
					if(serverStopped){
						if(isBigBoss)sendStartTXToServer();
						else sendStartTXToBigBoss();
					}
					
					//avvio la normale trasmissione versio il client
									
					/**
					 * se il buffer del proxy è vuoto allora aspetto che il buffer si sia riempito e poi invio al client
					 */
					if(!this.buffer.getNormalBuffer().isEmpty())
					{
						this.startNormalStreamToClient();
					}
					else{this.request_pending = true;}
					
					
					//transito nel nuovo stato
					state = ProxyState.transmittingToClient; 
					
				} else if (state == ProxyState.receivingRetransmission){
					System.err.println("IL MIO STATO È RECEIVINGRETRANSMISSION");
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
//					if(buffer.getRecoveryBuffer().isEmpty()){
//						
//						//avvio lo stream normale verso il client
//						startNormalStreamToClient();
//						
//						//transito nel nuovo stato
//						state = ProxyState.transmittingToClient;
//						
//					}
//					else{
						/*
						 * MArco: qui dovrebbe essere più chiaro: ricevo uno start TX dal client ed ho il recovery buffer 
						 * riempito con quanto rimasto dal buffer dell vecchio relay. faccio quindi partire un recovery stream
						 * che dovrebbe consumare prima il recovery buffer e poi il normal buffer.
						 */
						
						//avvio lo stream di recovery verso il client
					//	startNormalStreamToClient();
						startRecoveryStreamToClient();
						fProxy.getController().debugMessage(this.state.name());
						System.err.println(this.state.name());
						//transito nel nuovo stato
						state = ProxyState.TransmittingTempBuffer;
						
//					}
					
				} else if (state == ProxyState.attemptingToStart && !this.newProxy){
					System.err.println("IL MIO STATO È ATTEMPTINGTOSTART E NON SONO UN NUOVO PROXY");
					/*
					 * Marco: qui non ci si capisce nulla: sta fa cendo una trasmissione normale e non di recovery, ma il suo stato
					 * è comunque trasmitting temp buffer?? 
					 */
					
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//avvio lo stream di recovery verso il client
					
					startRecoveryStreamToClient();
					
					//startNormalStreamToClient();
					
					//transito nel nuovo stato
					state = ProxyState.TransmittingTempBuffer;
					
				} else if (state == ProxyState.attemptingToStart && this.newProxy){
					System.err.println("IL MIO STATO È ATTEMPTINGTOSTART E SONO UN NUOVO PROXY");
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//avvio lo stream di recovery verso il client
					
					//startRecoveryStreamToClient();
					
					startNormalStreamToClient();
					
					//transito nel nuovo stato
					state = ProxyState.TransmittingTempBuffer;
					this.newProxy = false;
					this.timeoutSessionInterrupted = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_SESSION_INTERRUPTED,"TIMEOUTSESSIONINTERRUPTED");

				}
				//VALERIO:CONTROLLARE!!!!!!!!!!!!!!!!!!!!!!
				//////////////////////////////////////////////////
				else if(state==ProxyState.transmittingToClient){
					muxTh.restart();
					System.out.println("ho fatto muxth.start()");
				}
				///////////////////////////////////////
			}
		
			
			else if(msgReader.getCode() == MessageCodeConfiguration.STOP_TX){
				System.err.println("IL MIO STATO È ARRIVATO STOP_TX");
				/*
				 * Marco: qui finalemte le cose sono più chiare: arriva uno stop TX dal client,
				 * sistemo i timeout e metto in pausa lo stream verso il client
				 * transito nello stato stoptoclient ( che vorrebbe dire che i client mi ha messo in stop)
				 */
				
				if (state == ProxyState.transmittingToClient){
					System.err.println("IL MIO STATO È TRANSMITTINGTOCLIENT");
					if(this.timeoutSessionInterrupted!=null)
					{
						this.timeoutSessionInterrupted.cancelTimeOutSingleWithMessage();
					}
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());				
					//imposto il timeoutSessionInterrupted
					this.timeoutSessionInterrupted = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_SESSION_INTERRUPTED,"TIMEOUTSESSIONINTERRUPTED");

					//interrompo lo strem versoil client
					pauseNormalStreamToClient();
					
					//transito nel nuovo stato 
					state = ProxyState.stopToClient;
				}
			}
		
			
			// TODO: arrivo Leave
				else if (msgReader.getCode() == MessageCodeConfiguration.LEAVE){
					/*
					 * Quando arriva il LEAVE dal vecchio relay devo considerare come mio nuovo
					 * mittente il futureStreamingServer che il session manager mi ha comunicato
					 * quando � arrivato il messaggio di NEW_RELAY a seguito della ricezione 
					 * dell'election done.
					 * cambio sorgente di streaming invocando il metodo 
					 * setStreamingServer dellRTPReceptionMan, che � colui che gestisce la ricezione
					 * del flusso multimendiale.
					 * Rimpicciolisco il buffer alle dimensioni normali
					 * 
					 * 
					 */
					// il nuovo proxy eroga il flusso dalla stessa porta della quale lo erogava il vecchio.
					this.connectedClusterHeadAddr=futureStreamingAddress;
					//MARCO ROBA TUA CHE HO COMMENTATO
					this.rtpReceptionMan.setStreamingServer(streamingServerAddress, this.streamingServerSessionPort);
					this.restrictNormalBuffer();
				}
			// TODO Arrivo Redirect
				else if (msgReader.getCode() == MessageCodeConfiguration.REDIRECT){
					
					System.out.println("Entrato nel redirect");
					/*
					 * C'� stata la rielezione di un nuovo relay secondario, il proxy sostitutivo
					 * sul nuovo relay secondario mi manda il redirect, affich� io mandi i flussi
					 * su di lui.
					 * Estraggo il suo indirizzo dal messaggio e invoco il metodo startHandoff che ridirige
					 * il flusso in uscita
					 */
					String newClientAddress = ((DatagramPacket)arg).getAddress().getHostAddress();					
					// non ho bisogno di controllare le porte sulle quali opera il  proxy sostitutivo perch�  sono
					// le stesse di quelle del proxy sul vecchio relay secondario.
					
					
					/*
					 * se servo un client sovrascrivo e mando il clientAddress
					 * se servo un relay  sovrascrivo e mando relayAddress
					 */
					if (servingClient== true){
						try {
							this.redirectOutputStream(InetAddress.getByName(clientAddress),this.clientStreamPort, InetAddress.getByName(newClientAddress));
						} catch (UnknownHostException e) {
							//  Auto-generated catch block
							e.printStackTrace();
						}
						this.clientAddress = newClientAddress;
						
						System.out.println("ricevuto redirect, ridirigo il flusso verso "+newClientAddress);
						
					}
					
					else {
						try {
							this.redirectOutputStream(InetAddress.getByName(relayAddress),this.relayStreamingPort, InetAddress.getByName(newClientAddress));
						} catch (UnknownHostException e) {
							//  Auto-generated catch block
							e.printStackTrace();
							
						}
						this.relayAddress = newClientAddress;
						System.out.println("ricevuto redirect, ridirigo il flusso verso "+newClientAddress);
						
					}
					

					
					
					
					
					
								
				}
		}
			
		}
		
	//}
	
	/* (non-Javadoc)
	 * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
	 */
	@Override
	public void controllerUpdate(ControllerEvent arg0) {
		if (debug)
			System.out.println("Proxy: evento:" + arg0);
		
		if (arg0 instanceof EndOfMediaEvent){
			if (state == ProxyState.transmittingToClient || state == ProxyState.handoff) endProxy();
			if (state == ProxyState.receivingRetransmission) state = ProxyState.attemptingToStart;
		}
		
	}

	/* (non-Javadoc)
	 * @see unibo.core.BufferFullListener#bufferFullEventOccurred(unibo.core.BufferFullEvent)
	 */
	@Override
	public synchronized void bufferFullEventOccurred(BufferFullEvent ev) {
		
		System.out.println(".................BUFFERFULLEVENT, MANDO UNO STOP_TX A monte");
		if(isBigBoss)sendStopTXToServer();
		else sendStopTXToBigBoss();
		System.out.println("stato "+state);
		/*
		 * il buffer è pieno: al solito a causa degli stati si capice poco, ma proviamo comunque a fare chiarezza
		 * in tutti c'è l'invio del messaggio STOPTX al server ( come era logico)
		 * se il client voleva dei frames, ma nn glieli mandavo perchè avevo il buffer vuoto, allora inizio a mandaglili
		 * se il client mi aveva mandato uno stoptx io sono in pausa, devo solo dire al server di non mandarmi più robaù
		 * poi ci sono due stati(receiving transmission e attempting to start che non ho capito, e che comunque mandano solo lo STop tx al server
		 * infine c'è la firstreceivingromserver che dovrebbe essere se è la prima volta che ricevo dal server, e qui è un mistero:
		 * perchè setto la soglia al buffer? Se mi arriva un'evento di bufferpieno, vuol dire che la soglia c'è già!
		 * è più chiaro perchè inizio la trasmissione verso il client: siamo all'inizio ed ho aspettato che il buffer si riempisse
		 * prima di mandare il flusso al client.
		 *
		 */
		
		
		// per vedere quanto ci metto a svuotare il buffer guardo il tempo adesso ed il tempo quando arriverà l'evento di buffer empty
		// e faccio la differenza
		this.startTime = System.currentTimeMillis();
		
		if (debug)
			System.out.println("Proxy: evento:" + ev);
		
		if (state == ProxyState.transmittingToClient){
			fProxy.getController().debugMessage(this.state.name());
			System.err.println(this.state.name());
			//invio al server il msg di StopTX
			
			sendStopTXToServer(); 
			
			if(this.request_pending)
				this.startNormalStreamToClient();
			//rimango nello stato in cui mi trovo
			
		}else if (state == ProxyState.stopToClient){
			fProxy.getController().debugMessage(this.state.name());
			System.err.println(this.state.name());
			//invio al server il msg di StopTX
			sendStopTXToServer();
			
			//rimango nello stato in cui mi trovo
			
		}else if (state == ProxyState.receivingRetransmission){
			fProxy.getController().debugMessage(this.state.name());
			System.err.println(this.state.name());
			//invio al server il msg di StopTX
			sendStopTXToServer();
			
			//rimango nello stato in cui mi trovo
			
		}else if (state == ProxyState.attemptingToStart){
			fProxy.getController().debugMessage(this.state.name());
			System.err.println(this.state.name());
			
			
			if(isBigBoss)
				sendStopTXToServer();//invio al server il msg di StopTX
			else
				sendStopTXToBigBoss();
			//rimango nello stato in cui mi trovo
			
		}else if (state == ProxyState.FirstReceivingromServer){
			fProxy.getController().debugMessage(this.state.name());
			System.err.println(this.state.name());
			System.out.println("Inizio dello streaming verso il client.");
			
			//setto la soglia superiore alla dimensione del buffer
			int bufSize = this.buffer.getNormalBuffer().getBufferSize();
			this.buffer.getNormalBuffer().setSogliaSuperiore(bufSize);
			
			//avvio la trasmissione al client
			startNormalStreamToClient();
			
			//transito nel nuovo stato 
			state = ProxyState.transmittingToClient;
		
		}

	}
	
	/* (non-Javadoc)
	 * @see unibo.core.BufferEmptyListener#bufferEmptyEventOccurred(unibo.core.BufferEmptyEvent)
	 */
	
	
	
	@Override
	public void bufferEmptyEventOccurred(BufferEmptyEvent e) {
		
		/*
		 * Marco:Il buffer è vuoto ( penso nel senso di completamente vuoto.)
		 * nel caso il buffer che si è vuotato è il recovery buffer faccio partire lo streaming sul normal buffer grazie al metodo
		 *	 	startNormalStreamToClient e passo nello stato transmittig ( di trasmissione normale)
		 * 
		 * in tutti i casi mando la richiesta di frames al server con il messaggio STARTTX
		 * 
		 * se stavo trasmettendo al client metto a true il flag  request pending in modo da riprendere la trasmissione
		 * 		quando arriverà qualcosa da mandargli
		 * 
		 * se invece ero in chiusura non mi resta che invocare il metodo dispose del'interfaccia grafica a me collegata
		 * 
		 */
		
		// per vedere quanto ci metto a svuotare il buffer guardo il tempo adesso ed il tempo quando arriverà l'evento di buffer empty
		// e faccio la differenza
		
		long executionTime = System.currentTimeMillis()-this.startTime;
		fProxy.messageArea.append("Tempo per svuotare il buffer: " + executionTime);
		
		if (debug)
			System.out.println("Proxy: evento:" + e);
		//TODO: da rivedere
		
		if (e.getSource() == buffer.getRecoveryBuffer()){
			
						
			startNormalStreamToClient();
			state = ProxyState.transmittingToClient;
						
		}
		fProxy.getController().debugMessage(this.state.name());
		System.err.println(this.state.name());
		if(isBigBoss)sendStartTXToServer();
		else sendStartTXToBigBoss();
		this.muxTh.suspend();
		if(this.state == ProxyState.transmittingToClient)
		{
			this.request_pending = true;
		}
		if(this.rtpReceptionMan.isEOM() || this.isEnding()) 
		{
			
			System.out.println("Proxy: termino e cominico la mia fine al sessionmanager");
			this.setChanged();
			this.notifyObservers("PROXY_ENDED");
			
			this.fProxy.dispose();
		}
	}
	
	
	/*
	 * ***************************************************************
	 * *******************METODI PUBLICI******************************
	 * ***************************************************************
	 */
	
	
	/**
	 * @author marco Nanni
	 * Metodo che ciene chiamato dal SessionManager del vecchio relay per far ridirigere
	 * i flussi dal client al prosy sostitutivo creato sulnuovo relay e far mandare 
	 * il messaggio di leave al client.
	 * 
	 * @param portStremInNewProxy - indica la porta rtp su cui � in ascolto il nuovo proxy
	 * @param newRelayAddr - indirizz del nuovo relay
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 * 
	 */
	public boolean startHandoff(int portStremInNewProxy, InetAddress newRelayAddr){
		this.fProxy.messageArea.append("start handoff, ridirigo il fusso verso "+ newRelayAddr.getHostAddress()+":" + portStremInNewProxy);
		boolean result=false;
		if(servingClient)
			try {
				result= this.startHandoff(InetAddress.getByName(clientAddress),portStremInNewProxy, newRelayAddr, true);
			} catch (UnknownHostException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
		else
			try {
				result= this.startHandoff(InetAddress.getByName(relayAddress),portStremInNewProxy, newRelayAddr, true);
			} catch (UnknownHostException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
		return result;
	}
	
	/**
	 * @author marco Nanni
	 * Metodo simile a StartHandoff, solo che non manda il leave al client 
	 * è usato dal proxy del big boss durante la rielezione di un relay secondario per ridirigere il flusso
	 * sul nuovo relay quando il nuovo proxy inivia a quello sul big boss il messaggio di redirect
	 * @param oldClientAddress l'indirizzo del nuovo relay secondario
	 * @param portStreamInNewClient - indica la porta rtp su cui � in ascolto il nuovo proxy
	 * @param newRelayAddr - indirizzo del nuovo relay secondario
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 */
	
	public boolean redirectOutputStream(InetAddress oldClientAddress,int portStremInNewProxy, InetAddress newClientAddr){
		return this.startHandoff(oldClientAddress,portStremInNewProxy, newClientAddr, false);
		
	}
	
	
	/**
	 * @author marco nanni
	 * Importante, il metodo prende l'indirizzo del vecchio client 
	 * Metodo da usare internamente alla classe per ridirigere il flusso rtp in uscita
	 * viene chiamato  sia dal metodo start handoff, sia da redirectOutputStream in
	 * occasioni diverse nel protocolla di handoff ( vedi la documentazione dei due metodi)
	 * 
	 * @param oldClientAddress - l'indirizzo del vecchio client
	 * @param portStremInNewClient - indica la porta rtp su cui � in ascolto il nuovo proxy
	 * @param newClientAddress - indirizz del nuovo client
	 * @param alsoSendRedirectToclient -  indica se madare anche il messaggio di redirect al client
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 * 
	 */
	protected boolean startHandoff(InetAddress oldClientAddress, int portStremInNewClient, InetAddress newClientAddress, boolean alsoSendRedirectToclient){
		
		/*
		 * Marco: questo metodo effettua la ridirezione del flusso in uscita dal client al nuovo rela.
		 * fermo lo stream verso il client, gli mando il messaggio di LEAVE, in modo che lui consideri il nuovo relay 
		 * ( di cui sa già l'indirizzo, visto che ha ricevuto il messaggio di Election Done) coe relay di riferimento
		 * e chieda a lui i frames quando il suo buffer si sarà svuotato.
		 * 
		 * cambio i parametri di porta ed indirizzo del processo rtpSender con quelli del nuovo relay
		 * 
		 * infine riprendo la trasmissione che anzichè essere indirizzata verso il client sarà veso il nuovo relay
		 */
		
//		if (state == ProxyState.transmittingToClient || state == ProxyState.stopToClient){		
		
			//metto in pausa la trasmissione verso il client
			pauseNormalStreamToClient();
			
			
			if (alsoSendRedirectToclient == true){
			//invio un messaggio di Leave al client
					sendLeaveMsgToClient();		 //informo il client che mi sto staccando
					fProxy.messageArea.append("mandato il Leave al client.");
					System.err.println("++++++++++++ mandato il Leave al client.");
			}
			try {
				//elimino il client dalla lista dei destinatari del sender
				rtpSender.removeTarget(oldClientAddress, clientStreamPort);
				
				//dirotto la connessione in out verso il client al new proxy
				this.clientStreamPort = portStremInNewClient;  // port stremInNewProxy è la porta sulla quale il nuovo proxy riceve il flusso dal vecchio proxy
				
				//aggiungo il nuovo relay cm destinatario inserendo la porta e l'indirizzo del proxy sul nuovo relay
				
				// marco provo a modificarlo 
				rtpSender.addDestination(newClientAddress,portStremInNewClient);
				
			} catch (UnknownHostException e) {
				// Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			 
			//in questo caso il client è il new proxy
			startNormalStreamToClient(); 
			
			return true;
			
//		}else return false;
		
		
		
	}
	
	/**
	 * Metodo che ingrandisce il buffer normale del proxy, usato quando il proxy su un 
	 * relay secondario deve ingrandire i propri bufffer a causa della rielezione del big boss
	 * Visto che finita tutta la fase di rielezione e di handoff dovrò ricevere un leave dal vecchio big boss
	 * creo un threaduccio che resta in attesa del messaggio e che esegueesegue l'update del proxy quando arriva il leave
	 * se mi limitassi a usare wait server response bloccherei il thread che esegue questo metodo che è quello che 
	 * fa l'update del sessionmanager e che richiama il matodo enlarge normalbuffer per ogniproxy, questo è il rtpreceiver
	 * del connecionmanagere del proxy, inoltre deve aspettare il leave per ogni proxy, la soluzione sarebbe troppo lenta.
	 */
	
	
	public void enlargeNormalBuffer (){
		/*
		 * Agisce chiamndo l'omonimo metodo del bufferManager
		 */
		
		this.buffer.enlargeNormalBuffer();
		
		/*
		 * In pratica creo un thread specificando direttamente il codice che
		 * del metodo run di questo proxy e poi l'avvio
		 */
		
		Thread runner2 = new Thread(){public void run(){
			
			proxyCM.waitStreamingServerResponse();
			
			
		}};
		runner2.start();
	}
	
	/**
	 * metodo richiamato all'arrivo ddi un LEAVE dal vecchio proxy che serviva questo
	 * qualora il proxy sia su un relay secondario e ci sia la rielezione del big boss.
	 * rihciama il metodo omonimo del bufferManager per reimpostare le soglie a quelle di
	 * funzionamento normale.
	 */
	
	public void restrictNormalBuffer(){
		this.buffer.restrictNormalBuffer();
	}
	
	/*
	 * ***************************************************************
	 * *******************GETTERS&SETTERS*****************************
	 * ***************************************************************
	 */
	
	/**
	 * @return the clientAddress
	 */
	public String getClientAddress() {
		return clientAddress;
	}
			
	/**
	 * @return the proxyID
	 */
	public String getProxyID() {
		return proxyID;
	}
	
	
	/*
	 * ***************************************************************
	 * *******************METODI PRIVATI******************************
	 * ***************************************************************
	 */
	
	private void initNormalSession(){
		
		try {
			/*
			 * Marco:questo metodo dovrebbe inizializzare una sessione di trasmissione normale , dal normal buffer
			 */
			
			//this.rtpReceptionMan.initNormalConnection(); // Marco: forse è commentato perchè la parte di ricezione è già presente nel metodo update quando arriva una start tx dal client, e quindi 
			
			/* Marco: forse è commentato perchè la parte di ricezione dal server è già presente nel metodo update
			 *  quando arriva una start tx  dal client, e quindi bisogna fare solo la trasmissione verso il client stesso, ma che casino!
			*/
			
		//	rtpMux = new RTPMultiplexer(this.rtpReceptionMan.getNormalTracksFormat());
			// Marco: rtp mux dovrebbe essere commentato perchè il multiplexer serve solo nella fase di ricovery, non nella trasmissione normale
			
			//rtpSender = new RTPSenderPS(outStreamPort);

			rtpSender = new RTPSenderPS(outStreamPort,InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS));
			if(servingClient){
				rtpSender.addDestination(InetAddress.getByName(clientAddress), this.clientStreamPort);
				System.err.println("FATTA ADDDESTINATION SUL CLIENT: "+clientAddress+":"+clientStreamPort);
			}
			else{
				rtpSender.addDestination(InetAddress.getByName(relayAddress), this.relayStreamingPort);
				System.err.println("FATTA ADDDESTINATION SUL RELAY: "+relayAddress+":"+relayStreamingPort);
				
			}
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initRecoverySession(int senderPort, InetAddress senderAddress) {
		
		/*
		 * Marco: questa dovrebbe preparare una  trasmissione che sfrutta il recovery buffer
		 */

		//imposto la porta da cui il server invia lo stream
		//this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);

		//relay buffer manager, this e' un listener per gli eventi sollevati dal buffer nominale e dio quello di recovery
	//	buffer = new RelayBufferManager(Parameters.PROXY_BUFFER, null, 0,Parameters.PROXY_BUFFER, this);
	//	buffer.getNormalBuffer().addBufferEmptyEventListener(this);
	//	buffer.getNormalBuffer().addBufferFullEventListener(this);
	//	buffer.getRecoveryBuffer().addBufferEmptyEventListener(this);
	//	buffer.getRecoveryBuffer().addBufferFullEventListener(this);
		
		try {
			
			//TODO: servono le porte da cui il vecchio proxy invia lo stream di recovery e l'indirizzo dell'old relay
			//this.rtpReceptionMan.initRecoveryConnection(senderPort, senderAddress);
			
			//rtpMux = new RTPMultiplexer(this.rtpReceptionMan.getNormalTracksFormat());
			
			System.out.println("Init Recovery Sessione controllare costruttore RTPSENDERPS");
			// TODO Init Recovery Sessione controllare costruttore RTPSENDERPS
			this.rtpSender = new RTPSenderPS(outStreamPort);
			if(servingClient){
				rtpSender.addDestination(InetAddress.getByName(clientAddress), this.clientStreamPort);
				System.err.println("FATTA ADDDESTINATION SUL CLIENT: "+clientAddress+":"+clientStreamPort);
			}
			else{
				rtpSender.addDestination(InetAddress.getByName(relayAddress), this.relayStreamingPort);
				System.err.println("FATTA ADDDESTINATION SUL RELAY: "+relayAddress+":"+relayStreamingPort);
				
			}
			
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	
	private void startNormalStreamToClient(){
		/*
		 * Questa funzione dovrebbe far partire lo stream sul normal buffer
		 */
		
		//si occupa anche di attaccare il mux al buffer normale
		if (muxTh == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
		
			/*
			 * Spesso, dopo una rielezione
			 *  arriva la richiesta del flusso da parte del client prima che
			 * il Thread 1 finisca di inizializzare la ricezione del flusso di recovery.
			 * con questo ciclo faccio aspettare il thread finchè tutto non è inizializzato a dovere
			 */
			try {
				while (rtpReceptionMan.getTrackFormats()==null){
//					System.err.println("track formats normal stream vuoto");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				}
				System.out.println("trackformat "+this.rtpReceptionMan.getTrackFormats().toString());
				RTPMultiplexer mux = new RTPMultiplexer(this.rtpReceptionMan.getTrackFormats());
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				CircularBuffer[] b = this.rtpReceptionMan.getNormalParserThread().getOutputBufferSet();
				try {
					muxTh = new MuseMultiplexerThread(mux, b,null,5);
					muxTh.setTimeToWait(BufferConfiguration.TTW-48);
				} catch (Exception e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
				//imposto il datasource
				rtpSender.sendData(mux.getDataOutput());
			} catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			
			muxTh.start();
			System.out.println("ProxySender: inizio trasmissione");
		
		}else muxTh.restart();
	}
	
	
	private void startRecoveryStreamToClient(){
		/*
		 * Marco: questa dovrebbe far partire lo stream usando i frames presenti nel recovery buffer
		 */
		System.err.println("@@@@@@@@@@@@@@@@@@@@@ appena dentro startRecoveryStreamToClient");
		//si occupa anche di attaccare il mux al buffer normale
		if (muxThR == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
			System.err.println("@@@@@@@@@@@@@@@@@@@@@ appena dentro il primo if");
			try {
				// commentato perchè funziona male, usare il codice scritto dopo
//				if(rtpReceptionMan.getTrackFormats2()==null){
//					System.err.println("@@@@@@@@@@@@@@@@@@@@@ appena dentro il secondo if");
//					try {
//						rtpReceptionMan.initRecoveryConnection(getOutStreamPort(), InetAddress.getByName(oldProxyAddress));
//						System.err.println("@@@@@@@@@@@@@@@@@@@@@ appena fatta la initRecoveryConnection");
//						rtpReceptionMan.startRecoveryConnection();
//						System.err.println("@@@@@@@@@@@@@@@@@@@@@ appena fatta la StartRecoveryConnection");
//					} catch (IncompatibleSourceException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//				}
				/*
				 * Spesso arriva la richiesta del flusso da parte del client prima che
				 * il Thread 2 finisca di inizializzare la ricezione del flusso di recovery.
				 * con questo ciclio faccio aspettare il thread finchè tutto non è inizializzato a dover
				 */
				while(rtpReceptionMan.getTrackFormats2()==null)
					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				RTPMultiplexer mux = new RTPMultiplexer(rtpReceptionMan.getTrackFormats2());
				System.err.println("@@@@@@@@@@@@  dopocreazione muxRTP");
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				System.err.println("@@@@@@@@@@@@  +" +rtpReceptionMan+ "  - " +rtpReceptionMan.getRecoveryParserThread()+" - "+rtpReceptionMan.getRecoveryParserThread().getOutputBufferSet());

				CircularBuffer[] b = rtpReceptionMan.getRecoveryParserThread().getOutputBufferSet();
				System.err.println("@@@@@@@@@@@@  estrazione circular buffer");
				try {
					muxThR = new MuseMultiplexerThread(mux, b,null,5);
					System.err.println("@@@@@@@@@@@@  dopo creazione muxTHR");
					muxThR.setTimeToWait(BufferConfiguration.TTW-48);
					System.err.println("@@@@@@@@@@@@  dopo set TTW muxTHR");
				} catch (Exception e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
				//imposto il datasource
				rtpSender.sendData(mux.getDataOutput());
				System.err.println("@@@@@@@@@@@@  dopo send data");
			} catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			
			muxThR.start();
			System.out.println("ProxySender: inizio trasmissione");
		
		}else muxThR.restart();
	}
	
	
	
	
	
	private void pauseNormalStreamToClient() {
		/*
		 * Marco: questa funzione ferma lo stream verso il client, la domanda è : perchè non c'è anche la versione da usare
		 * nel caso si stiano mando i frames del recovery buffer? 
		 * 
		 */
		
		muxTh.suspend();
		System.out.println("Trasmissione Proxy -> Client sospesa");	
	}
	
	private void startRecoveryStreamToClientOLD(){
		/*
		 * Marco: vecchia versione, non credo che venga mai usata
		 */
		
		try {
			/**
			 * COMMENTATO....DA DECOMMENTARE....
			 */
		//	muxTh = new MultiplexerThreadEV(rtpMux, buffer.getRecoveryBuffer(), null , this.clientAddress);
			rtpSender.sendData(rtpMux.getDataOutput());
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		
		muxTh.start();
		System.out.println("ProxySender: inizio trasmissione");
	}
		
	private void endProxy(){
		this.muxTh.close();
		this.rtpReceptionMan.closeAll();
		this.state = ProxyState.ended;
		//notifico tutti gli osservatori dell'evento
		this.notifyObservers("End_Of_Media");
		this.fProxy.dispose();
	}

	private void sendForwardReqFileToServer(){//client->relay->bigboss->server
		System.out.println("sendForwardReqFileToServer");
		try {
			this.inStreamPort = rtpReceptionMan.getNormalReceivingPort();
			DatagramPacket reqFile =  RelayMessageFactory.buildForwardReqFile(0, filename,  proxyCM.getLocalAdHocInputPort(), this.inStreamPort, this.relayAddress, this.relayControlPort, this.relayStreamingPort, clientAddress, this.clientStreamPort, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), PortConfiguration.SERVER_SESSION_PORT_IN);
			System.out.println("il messaggio di request file che il bigboss manda al server è:\n0, "+filename+", "+proxyCM.getLocalAdHocInputPort()+", "+this.inStreamPort+", "+this.relayAddress+"_"+this.relayControlPort+"_"+this.relayStreamingPort+"_"+this.clientAddress+", "+this.clientStreamPort);
			proxyCM.sendToServer(reqFile);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void sendReqFileToServer(){//client->bigboss->server
		try {
			System.out.println("sendReqFileToServer");
			this.inStreamPort = rtpReceptionMan.getNormalReceivingPort();
			//Creo un messaggio FORWARD_REQ_FILE e lo invio al server
//			DatagramPacket reqFile =  RelayMessageFactory.buildReqFile(0, filename, Parameters.RELAY_SESSION_AD_HOC_PORT_IN, this.inStreamPort, this.clientAddress, Parameters.CLIENT_PORT_SESSION_IN, this.clientStreamPort, InetAddress.getByName(Parameters.SERVER_ADDRESS), Parameters.SERVER_SESSION_PORT_IN);
//			(0, clientAddress, filename, proxyCM.getLocalManagedInputOutputPort(),this.inStreamPort, InetAddress.getByName(Parameters.SERVER_ADDRESS),Parameters.SERVER_SESSION_PORT_IN);
//			DatagramPacket reqFile =  RelayMessageFactory.buildReqFile(0, filename, proxyCM.getLocalAdHocInputPort(), this.inStreamPort, this.clientAddress, PortConfiguration.CLIENT_PORT_SESSION_IN, this.clientStreamPort, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), PortConfiguration.SERVER_SESSION_PORT_IN);
			DatagramPacket reqFile =  RelayMessageFactory.buildReqFile(0, filename, proxyCM.getLocalManagedInputOutputPort(), this.inStreamPort, this.clientAddress, this.clientStreamPort, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), PortConfiguration.SERVER_SESSION_PORT_IN);
			System.out.println("il messaggio di request file che il relay manda al server è:\n0, "+filename+", "+proxyCM.getLocalAdHocInputPort()+", "+this.inStreamPort+", "+this.clientAddress+", "+PortConfiguration.CLIENT_PORT_SESSION_IN+", "+this.clientStreamPort);
			proxyCM.sendToServer(reqFile);	
			this.timeoutAckForward = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_ACK_FORWARD,"TIMEOUTACKFORWARD");
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void sendReqFileToBigBoss(){//client->relay->bigboss
		System.out.println("sendReqFileToBigBoss");
		this.inStreamPort = rtpReceptionMan.getNormalReceivingPort();
		DatagramPacket reqFile;
		try {
//			reqFile = RelayMessageFactory.buildForwardReqFile(0, filename, 0, 0,"questo campo non mi serve, lo riempirà bigboss", proxyCM.getLocalAdHocInputPort(), this.inStreamPort, this.clientAddress,  this.clientStreamPort, InetAddress.getByName(this.connectedClusterHeadAddr), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
//			System.out.println("il messaggio di request file che il relay manda al server è:\n0, "+filename+", "+proxyCM.getLocalAdHocInputPort()+", "+this.inStreamPort+", "+this.clientAddress+", "+this.clientStreamPort);
			reqFile = RelayMessageFactory.buildForwardReqFile(0, filename, 0, 0,"questo campo non mi serve, lo riempirà bigboss", proxyCM.getLocalManagedInputOutputPort(), this.inStreamPort, this.clientAddress,  this.clientStreamPort, InetAddress.getByName(this.connectedClusterHeadAddr), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
			System.out.println("il messaggio di request file che il relay manda al server è:\n0, "+filename+", "+proxyCM.getLocalManagedInputOutputPort()+", "+this.inStreamPort+", "+this.clientAddress+", "+this.clientStreamPort);
			proxyCM.sendToServer(reqFile);	
		} catch (Exception e) {
			// Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	/**
	 * @return the inStreamPort
	 */
	public int getInStreamPort() {
		return inStreamPort;
	}
	
	//@ TODO commentare i metodi per preparare i messaggi.

	private void sendStartTXToServer(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			DatagramPacket startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(connectedClusterHeadAddr), this.streamingServerCtrlPort);
			proxyCM.sendToServer(startTX);
			System.out.println("StartTX mandato al server");
			this.serverStopped = false;
		} catch (UnknownHostException e) {System.err.println("ERROER MANDARE STARTX SERVER");
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {System.err.println("ERROER MANDARE STARTX SERVER");
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendStartTXToBigBoss(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			System.out.println("-------------------------------------------porta bigboss "+this.streamingServerCtrlPort);
			DatagramPacket startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(connectedClusterHeadAddr), this.streamingServerCtrlPort);
			proxyCM.sendToServer(startTX);

			this.serverStopped = false;
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	
	/**
	 * Metodo che manda il messaggio di redirect a chi eroga il flusso, il quale
	 * ridirigerà il flusso rtp verso questo nodo.
	 * Non c'è la versione differenziata perchè deve arrivare a chi eroga il flusso 
	 * (proxy del big boss o StreamingServer del server), la porta sulla quale 
	 * stanno in ascolto è la porta di controllo, tale porta viene determinata
	 * all'interno del metodo.
	 */
	private void sendRedirect(){
	
		
		try {
			//Creo un messaggio REDIRECT e lo invio al server
			
			DatagramPacket redirect = RelayMessageFactory.buildRedirect(0,InetAddress.getByName(this.connectedClusterHeadAddr),this.streamingServerCtrlPort);
			proxyCM.sendToServer(redirect);
			this.fProxy.messageArea.append("Mandato il redirect a "+ this.connectedClusterHeadAddr+": "+ this.streamingServerCtrlPort);
			System.err.println("Mandato il redirect a "+ this.connectedClusterHeadAddr+": "+ this.streamingServerCtrlPort);
			//this.serverStopped = false;
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	
	private void sendStopTXToServer() {
		DatagramPacket stopTX;
		try {
			stopTX = RelayMessageFactory.buildStopTx(0, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), this.streamingServerCtrlPort);
			proxyCM.sendToServer(stopTX);
			this.serverStopped = true;
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendStartTXToOldProxy(){
		DatagramPacket startTX;
		try {
			startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(this.oldProxyAddress), this.streamingServerCtrlPort);
			proxyCM.sendToServer(startTX);
			this.serverStopped = true;
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void sendStopTXToBigBoss(){
		DatagramPacket stopTX;
		try {
			stopTX = RelayMessageFactory.buildStopTx(0, InetAddress.getByName(this.connectedClusterHeadAddr), this.bigbossControlPort);
			proxyCM.sendToServer(stopTX);
			this.serverStopped = true;
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendAckClientReqToClient(){
		try {
			this.proxyStreamingCtrlPort = proxyCM.getLocalAdHocInputPort();
			//Creo un messaggio ACK_CLIENT_REQ
			DatagramPacket ackClientReq = RelayMessageFactory.buildAckClientReq(0, PortConfiguration.CLIENT_PORT_SESSION_IN,InetAddress.getByName(this.clientAddress), this.outStreamPort, this.proxyStreamingCtrlPort);
			proxyCM.sendTo(ackClientReq);
		} catch (UnknownHostException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendAckFileToRelay(String clientAddr,int clientControlPort, int clientStreamPort){//messaggio che il bigboss invia al relay
		try{
			System.out.println("Mando ACK FILE a :"+msgReader.getRelayAddress()+":"+msgReader.getRelayControlPort());
			this.proxyStreamingCtrlPort=proxyCM.getLocalAdHocInputPort();
//			DatagramPacket ackRelayReq=RelayMessageFactory.buildForwardAckReq(0,msgReader.getRelayControlPort(),InetAddress.getByName(msgReader.getRelayAddress()),this.outStreamPort,this.proxyStreamingCtrlPort,clientAddr,clientControlPort,clientStreamPort);
			DatagramPacket ackRelayReq=RelayMessageFactory.buildForwardAckReq(0,msgReader.getRelayControlPort(),InetAddress.getByName(msgReader.getRelayAddress()),this.outStreamPort,this.proxyStreamingCtrlPort,clientAddr,clientStreamPort);
			
			proxyCM.sendTo(ackRelayReq);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private void sendServerUnreacheableToClient(){
//		try {
//			//invio SERVER_UNREACHABLE al client:
//			DatagramPacket serverUnreach = RelayMessageFactory.buildServerUnreacheable(0, InetAddress.getByName(this.clientAddress),PortConfiguration.CLIENT_PORT_SESSION_IN);
//			proxyCM.sendTo(serverUnreach);
//		} catch (UnknownHostException e) {
//			// Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// Auto-generated catch block
//			e.printStackTrace();
//		}		
	}
		
	// TODO	sendleaveMessage to client!
	protected void sendLeaveMsgToClient() {
		try {
			//invio LEAVE al client:
			DatagramPacket leave=null;
			System.out.println("--------------sendLeaveMsgToClient() clientaddress "+clientAddress+", cliensessionport "+ PortConfiguration.CLIENT_PORT_SESSION_IN+" ServingClient"+servingClient);
			if(servingClient)
				leave = RelayMessageFactory.buildLeave(0, InetAddress.getByName(clientAddress), PortConfiguration.CLIENT_PORT_SESSION_IN);
			else
				leave = RelayMessageFactory.buildLeave(0, InetAddress.getByName(relayAddress), this.relayControlPort);
			proxyCM.sendTo(leave);
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}

	public void setFutureStreamingAddress(String newRelay) {
		this.futureStreamingAddress = newRelay;
		
	}

	/**
	 * metodo creato per finalità di test ritorna il numero di frames prensenti nel nomalBuffer
	 * @return
	 */
	public int tGetNormalBufferSize() {
		// TODO Auto-generated method stub
		return this.buffer.getNormalBuffer().getContatore();
	}

	public RelayBufferManager getBuffer() {
		return buffer;
	}
	
	
	/**
	 * @return restituisce l'indirizzo dell'entità verso cui il proxy spara lo stream
	 */
//	
//	private String getStreamingClientAddress(){
//		if(this.isBigBoss&&!this.servingClient){//bigboss serve relay
//			return this.relayAddress;
//		}
//		else{
//			return this.clientAddress;
//		}
//	}
	
	/**
	 * Metodo che dermina le porte a cui mandare i messaggi di sessione sia per i messaggi da mandare 
	 * verso chi eroga il flusso che per i messaggi indirizati verso chi riceve il flusso.
	 * 
	 * in particolare se questo soggetto � un altro relay i messaggi verranno ricevuti dal proxy,
	 * la porta di sessione viene in questo caso posta pari alla porta di controllo sulla quale 
	 * gi� viaggiano messaggi come StartTx e StopTx.
	 * 
	 * Altrimenti si usano le porte CLIENT_PORT_SESSION_IN e SERVER_SESSION_PORT_IN
	 * per parlare rispettivamente con un client o con il server.
	 * 
	 * @param servingClient variabile boolana che � true se si sta servendo un client, false se si 
	 * 		eroga il flusso verso un altro relay
	 * @param streamingServerAddress l'indirizzo del nodo che sta erogando il flusso a questo proxy.
	 */
	
//	protected void determinaPorteSessione(boolean servingClient, String streamingServerAddress){
//		/*
//		 * Se sto servendo un cliente la sua porta di sessione � quella dei client,
//		 * infatti i messaggi di LEAVE arrivano  al ClientSessionManager, mentre
//		 * start tx e top tx arrivano a ClientBufferDataPlaying
//		 * Se sto servendo un relay arrivano ad un proxy e posso usare la porta di 
//		 * controllo, come porta di sessione, visto che � il proxy a dover ricevere tutti i messaggi
//		 * da me mandati ( compreso LEAVE).
//		 * Discorso analogo per il server, i messaggi di sessione arrivano al
//		 * server Session Manager, mentre StartTX e STOP TX a streamingServer
//		 * Se sto ricevendo da un proxy su big boss,posso usare la porta di 
//		 * controllo, come porta di sessione, visto che � il proxy a dover ricevere tutti i messaggi
//		 * da me mandati ( compreso REDIRECT).
//		 * auguri!		
//		 */
//		
//		if(servingClient)
//			clientSessionPort= PortConfiguration.CLIENT_PORT_SESSION_IN;
//		else
//				if (clientStreamControlPort != 0)
//				clientSessionPort= this.clientStreamControlPort;
//		
//		if (streamingServerAddress.equals(NetConfiguration.SERVER_ADDRESS)) // in questo caso so che sto ricevendo il flusso dal server
//			streamingServerSessionPort=PortConfiguration.SERVER_SESSION_PORT_IN;
//		else
//			streamingServerSessionPort= streamingServerCtrlPort;// ricevo il flusso da un altro proxy.
//		
//	}
}

/*
 * Marco: guida agli stati: è imprecisa, visto che non ho documentazione, anzi potrebbe addirittura esser sbagliata: ma tanto vale provarci!
 * 
 * FirstReceivingromServer: qui sono all'inizio della trasmissione: il client mi ha mandato il primo startTX, e io ho fatto lo stesso con
 * 		il server: devo aspettare che il buffer si riempia prima di iniziare a trasmettere al client
 * 
 * stopToClient: il client mi ha mandato uno STOPTX, quindi fermo la trasmissione verso di lui
 * 
 * transmittingToClient: è lo stato "normale" sto trasmettendo il flusso al client
 * 
 * waitingClientAck: dovrebbe essere lo stato precedente alla prima trasmissione verso il client; solo che è strano, ci vado anche in
 * 		seguito alla creazione di un proxy di recovery dovuto ad una rielezione. Se così fosse non copisco la transazioe di stati 
 * 		e la ricezione dei messaggi che mi fanno mandare prima il recovery buffer e poi il normal buffer.
 * 
 * STATI NASCOSTI:
 * c'è inoltre il flag ending che indica che la canzone sta finendo, una volta svuotato il buffer si può chiudere la baracca.
 * 
 */


/*
 * Carlo: Ho aggiunto uno stato di handoff e uno di ended per indicare che il proxy � stato terminato.
 */
enum ProxyState {
	waitingServerRes, waitingClientAck, FirstReceivingromServer, 
	transmittingToClient, stopToClient, receivingRetransmission, 
	TransmittingTempBuffer, attemptingToStart, handoff, ended;
}
