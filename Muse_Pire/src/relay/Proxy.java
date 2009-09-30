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

import client.gui.IClientView;

import parameters.Parameters;

import relay.connection.ProxyCM;
import relay.connection.RelayConnectionFactory;
import relay.gui.ProxyFrame;
import relay.gui.ProxyFrameController;
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
import util.Logger;


/**
 * @author Leo Di Carlo
 *
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
	private TimeOutAckForward timeoutAckForward;
	private TimeOutAckClientReq timeoutAckClientReq;
	private TimeOutSessionInterrupted timeoutSessionInterrupted;
	
	//parametri:
	private String proxyID = "-1";
	private String filename;
	private String clientAddress;
	int initialSupThreshold = (Parameters.PROXY_BUFFER*70)/100;
	
	//porte(le porte di ricezione del proxy si trovano dentro l'RTPReceptionManager): 
	private int clientStreamPort = 0; 	//porta su cui il client riceve lo stream rtp 
	private int serverStreamPort = 0; 	//porta da cui lo streamingserver invia lo stream rtp
	private int outStreamPort = 0;		//porta da cui il proxy invia lo stream rtp
	private int inStreamPort = 0;       //porta da cui il proxy riceve lo str 
	private int streamingServerCtrlPort = 0; //porta su cui lo streamingserver riceve i messaggi di controllo
//	private int sessionControlPort;  //porta su cui il proxy riceve i messaggi di controllo dal client durante la trasmissione:
	private int proxyStreamingCtrlPort = 0;
	
	//componenti:
	private ProxyCM proxyCM;
	private RTPReceptionManager rtpReceptionMan; 
	private RelayBufferManager buffer;
	private RTPSenderPS rtpSender;
	private RTPMultiplexer rtpMux;
	private MuseMultiplexerThread muxTh;
	private boolean ending = false;
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
	public Proxy(Observer sessionManager,boolean newProxy, String filename, String clientAddress, int clientStreamPort) {
		
		this.fProxy = new ProxyFrame();
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		this.serverStopped = true;
		
		this.newProxy = newProxy;
		this.filename = filename; 
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		
		this.msgReader = new RelayMessageReader();
		
		//proxy connection manager, e' osservato da this
		this.proxyCM = RelayConnectionFactory.getProxyConnectionManager(this);
		this.proxyCM.start();
		
		
		
		try {
			//relay buffer manager, this e' un listener per gli eventi sollevati dal buffer nominale
			buffer = new RelayBufferManager(Parameters.PROXY_BUFFER, this.fProxy.getController(), 0,initialSupThreshold, this);
			//buffer.getNormalBuffer().addBufferEmptyEventListener(this);
			//buffer.getNormalBuffer().addBufferFullEventListener(this);
			
			this.rtpReceptionMan = new RTPReceptionManager(newProxy, buffer, this);
			this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sendForwardReqFileToServer();
		
		//imposto il timeout
		this.timeoutAckForward = RelayTimeoutFactory.getTimeOutAckForward(this, Parameters.TIMEOUT_ACK_FORWARD);
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

	public Proxy(Observer sessionManager, boolean newProxy, String clientAddress, int clientStreamPort, int proxyStreamPortOut, int proxyStreamPortIn, int serverStreamPort, int recoverySenderPort, InetAddress recoverySenderAddress, int serverCtrlPort, int proxyCtrlPort){
//	TODO: controllare che le porte siano tutte ben mappate
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		this.serverStopped = false;
		this.newProxy = newProxy;
		this.filename = ""; 
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		this.serverStreamPort = serverStreamPort;
		this.outStreamPort = proxyStreamPortOut;
		this.inStreamPort = proxyStreamPortIn;
		this.streamingServerCtrlPort = serverCtrlPort;
		this.proxyStreamingCtrlPort = proxyCtrlPort;
		System.out.println("Inizializzazione del proxy in corso...");
		final InetAddress oldProxyAddress = recoverySenderAddress;
		this.msgReader = new RelayMessageReader();
		
		this.fProxy = new ProxyFrame();
		//proxy connection manager, e' osservato da this
		this.proxyCM = RelayConnectionFactory.getProxyConnectionManager(this, this.proxyStreamingCtrlPort);
		this.proxyCM.start();
		
		try {
			
			buffer = new RelayBufferManager(Parameters.PROXY_BUFFER, this.fProxy.getController(), 0,initialSupThreshold, this);
			//creo un rtpreceptionamanger
			this.rtpReceptionMan = new RTPReceptionManager(newProxy, buffer, this.inStreamPort, this);
			
			//imposto la porta da cui il server invia lo stream 
			this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
			
			//imposto la porta di ricezione normale
			//this.rtpReceptionMan.setNormalReceivingPort(proxyStreamPortIn);
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		Thread runner1 = new Thread(){public void run(){try {
			rtpReceptionMan.initNormalConnection();
			System.err.print("Apertura ricezione normale in corso...");
			rtpReceptionMan.startNormalConnection();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}};
		runner1.start();
		
		this.sendRedirectToServer();

		
		
		Thread runner2 = new Thread(){public void run(){try {
			rtpReceptionMan.initRecoveryConnection(outStreamPort, oldProxyAddress);
			System.err.print("Apertura ricezione recovery in corso...");
			rtpReceptionMan.startRecoveryConnection();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}};
		runner2.start();
		
		this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		//inizializzo la sessione di recovery (quella tra old proxy e new proxy)
//		this.initRecoverySession(recoverySenderPort, recoverySenderAddress);
		
		//inizializzo la sessione normale (quella tra il new proxy e il client)
		this.initNormalSession();
		//avvio la ricezione nominale 
//		boolean flag = rtpReceptionMan.startNormalConnection();
		
		//avvio la ricezione di recovery
//		boolean flag2 = rtpReceptionMan.startRecoveryConnection();

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
//				Logger.write(": Errore nella lettura del Datagramma");
				e.printStackTrace();
			}
			
			if (msgReader.getCode() == Parameters.ACK_RELAY_FORW && state == ProxyState.waitingServerRes){
			
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
				//reset timeout TimeOutAckForward
				this.timeoutAckForward.cancelTimeOutAckForward();
				
				//leggo le informazioni contenute nel messaggio
				this.serverStreamPort = msgReader.getPortStreamingServer();
				this.streamingServerCtrlPort = msgReader.getPortStreamingCtrlServer();
				
				//imposto la porta da cui il server invia lo stream
				this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
				
				//inizializzo la sessione
				//initNormalSession();
				
				outStreamPort = RelayPortMapper.getInstance().getFirstFreeStreamOutPort(); 
					
				//invio al client il msg AckClientReq
				this.state = ProxyState.waitingClientAck;
				sendAckClientReqToClient();
				
				
				this.proxyCM.start();
				
				/**
				 * Ora ho tutti i dati della sessione pronti ergo posso avvertire il sessionmanager e passarglieli tutti
				 */
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
					this.setChanged();
					this.notifyObservers(sessionports);
				}
				//imposto il timeout ack client req:
				this.timeoutAckClientReq = RelayTimeoutFactory.getTimeOutTimeOutAckClientReq(this, Parameters.TIMEOUT_ACK_CLIENT_REQ);
				
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
				
			} else if(msgReader.getCode() == Parameters.START_TX){
				
				if(this.timeoutSessionInterrupted!=null)
				{
					this.timeoutSessionInterrupted.cancelTimeOutSessionInterrupted();
				}
				if (state == ProxyState.waitingClientAck){
				
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//resetto TimeOutAckClientReq
					this.timeoutAckClientReq.cancelTimeOutAckClientReq();
					
					//invio il msg StartTX al server
					
					
					Thread runner = new Thread(){public void run(){try {
						rtpReceptionMan.initNormalConnection();
						System.err.print("Apertura ricezione normale in corso...");
						rtpReceptionMan.startNormalConnection();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IncompatibleSourceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}}};
					runner.start();
					
					initNormalSession();
					sendStartTXToServer();
					
					System.err.println("initNormalSession() FINITO");
				
					//avvio la ricezione

					System.out.print("fatto.");
					
					//transito nel nuovo stato
					this.state = ProxyState.FirstReceivingromServer;
										
				} else if(state == ProxyState.stopToClient){
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//resetto il TimeOutSessionInterrupted
					this.timeoutSessionInterrupted.cancelTimeOutSessionInterrupted();
					
					//invio il msg di startTX al server
					if(serverStopped) sendStartTXToServer();
					
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
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					if(buffer.getRecoveryBuffer().isEmpty()){
						
						//avvio lo stream normale verso il client
						startNormalStreamToClient();
						
						//transito nel nuovo stato
						state = ProxyState.transmittingToClient;
						
					}else{
						
						//avvio lo stream di recovery verso il client
					//	startNormalStreamToClient();
						startRecoveryStreamToClient();
						fProxy.getController().debugMessage(this.state.name());
						System.err.println(this.state.name());
						//transito nel nuovo stato
						state = ProxyState.TransmittingTempBuffer;
						
					}
					
				} else if (state == ProxyState.attemptingToStart && !this.newProxy){
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//avvio lo stream di recovery verso il client
					
					//startRecoveryStreamToClient();
					
					startNormalStreamToClient();
					
					//transito nel nuovo stato
					state = ProxyState.TransmittingTempBuffer;
					
				} else if (state == ProxyState.attemptingToStart && this.newProxy){
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());
					//avvio lo stream di recovery verso il client
					
					startRecoveryStreamToClient();
					
					//startNormalStreamToClient();
					
					//transito nel nuovo stato
					state = ProxyState.TransmittingTempBuffer;
					this.newProxy = false;
				}
				this.timeoutSessionInterrupted = RelayTimeoutFactory.getTimeOutSessionInterrupted(this, Parameters.TIMEOUT_SESSION_INTERRUPTED);
			} else if(msgReader.getCode() == Parameters.STOP_TX){
				if (state == ProxyState.transmittingToClient){
					if(this.timeoutSessionInterrupted!=null)
					{
						this.timeoutSessionInterrupted.cancel();
					}
					fProxy.getController().debugMessage(this.state.name());
					System.err.println(this.state.name());				
					//imposto il timeoutSessionInterrupted
					this.timeoutSessionInterrupted = RelayTimeoutFactory.getTimeOutSessionInterrupted(this, Parameters.TIMEOUT_SESSION_INTERRUPTED);

					//interrompo lo strem versoil client
					pauseNormalStreamToClient();
					
					//transito nel nuovo stato 
					state = ProxyState.stopToClient;
				}
			}
			
		}
		
	}
	
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
			//invio al server il msg di StopTX
			sendStopTXToServer(); 
			
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
		
		if (debug)
			System.out.println("Proxy: evento:" + e);
		//TODO: da rivedere
		if (e.getSource() == buffer.getRecoveryBuffer()){
						
			startNormalStreamToClient();
			state = ProxyState.transmittingToClient;
			
		}
		fProxy.getController().debugMessage(this.state.name());
		System.err.println(this.state.name());
		sendStartTXToServer();
		this.muxTh.suspend();
		if(this.state == ProxyState.transmittingToClient)
		{
			this.request_pending = true;
		}
		if(this.rtpReceptionMan.isEOM() || this.isEnding()) 
		{
			this.fProxy.dispose();
		}
	}
	
	
	/*
	 * ***************************************************************
	 * *******************METODI PUBLICI******************************
	 * ***************************************************************
	 */
	
	/**
	 * 
	 * @param portStremInNewProxy - indica la porta rtp su cui � in ascolto il nuovo proxy
	 * @param newRelayAddr - indirizz del nuovo relay
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 * 
	 */
	public boolean startHandoff(int portStremInNewProxy, InetAddress newRelayAddr){
		
//		if (state == ProxyState.transmittingToClient || state == ProxyState.stopToClient){		
		
			//metto in pausa la trasmissione verso il client
			pauseNormalStreamToClient();
			
			//invio un messaggio di Leave al client
			sendLeaveMsgToClient();		 //informo il client che mi sto staccando
			
			try {
				//elimino il client dalla lista dei destinatari del sender
				rtpSender.removeTarget(InetAddress.getByName(clientAddress), clientStreamPort);
				
				//dirotto la connessione in out verso il client al new proxy
				this.clientStreamPort = portStremInNewProxy; 
				
				//aggiungo il nuovo relay cm destinatario inserendo la porta di un proxy creato ad hoc 
				rtpSender.addDestination(newRelayAddr, clientStreamPort);
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			 
			//in questo caso il client � il new proxy
			startNormalStreamToClient(); 
			
			return true;
			
//		}else return false;
		
		
		
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
			//this.rtpReceptionMan.initNormalConnection();

		//	rtpMux = new RTPMultiplexer(this.rtpReceptionMan.getNormalTracksFormat());
			rtpSender = new RTPSenderPS(outStreamPort);
			rtpSender.addDestination(InetAddress.getByName(clientAddress), this.clientStreamPort);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initRecoverySession(int senderPort, InetAddress senderAddress) {

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
			this.rtpSender = new RTPSenderPS(outStreamPort);
			rtpSender.addDestination(InetAddress.getByName(clientAddress), this.clientStreamPort);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	
	private void startNormalStreamToClient(){
		//si occupa anche di attaccare il mux al buffer normale
		if (muxTh == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
		
			try {
				RTPMultiplexer mux = new RTPMultiplexer(this.rtpReceptionMan.getTrackFormats());
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				CircularBuffer[] b = this.rtpReceptionMan.getNormalParserThread().getOutputBufferSet();
				try {
					muxTh = new MuseMultiplexerThread(mux, b,null,5);
					muxTh.setTimeToWait(Parameters.TTW-48);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//imposto il datasource
				rtpSender.sendData(mux.getDataOutput());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			muxTh.start();
			System.out.println("ProxySender: inizio trasmissione");
		
		}else muxTh.restart();
	}
	
	
	private void startRecoveryStreamToClient(){
		//si occupa anche di attaccare il mux al buffer normale
		if (muxThR == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
		
			try {
				RTPMultiplexer mux = new RTPMultiplexer(this.rtpReceptionMan.getTrackFormats2());
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				CircularBuffer[] b = this.rtpReceptionMan.getRecoveryParserThread().getOutputBufferSet();
				try {
					muxThR = new MuseMultiplexerThread(mux, b,null,5);
					muxThR.setTimeToWait(Parameters.TTW-48);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//imposto il datasource
				rtpSender.sendData(mux.getDataOutput());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			muxThR.start();
			System.out.println("ProxySender: inizio trasmissione");
		
		}else muxThR.restart();
	}
	
	
	
	
	
	private void pauseNormalStreamToClient() {
		muxTh.suspend();
		System.out.println("Trasmissione Proxy -> Client sospesa");	
	}
	
	private void startRecoveryStreamToClientOLD(){
		
		try {
			/**
			 * COMMENTATO....DA DECOMMENTARE....
			 */
		//	muxTh = new MultiplexerThreadEV(rtpMux, buffer.getRecoveryBuffer(), null , this.clientAddress);
			rtpSender.sendData(rtpMux.getDataOutput());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	private void sendForwardReqFileToServer(){
		try {
			this.inStreamPort = rtpReceptionMan.getNormalReceivingPort();
			//Creo un messaggio FORWARD_REQ_FILE e lo invio al server
			DatagramPacket forwReqfile =  RelayMessageFactory.buildForwardReqFile(0, clientAddress, filename, proxyCM.getLocalManagedInputOutputPort(), 
					this.inStreamPort, InetAddress.getByName(Parameters.SERVER_ADDRESS), 
					Parameters.SERVER_SESSION_PORT_IN);
			proxyCM.sendToServer(forwReqfile);
			
			
			/**
			 * MODIFICATO: aggiunta la chiamata al timeout server unreacheable
			 */
			//this.timeoutAckForward = RelayTimeoutFactory.getTimeOutAckForward(this, Parameters.TIMEOUT_ACK_FORWARD);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the inStreamPort
	 */
	public int getInStreamPort() {
		return inStreamPort;
	}

	private void sendStartTXToServer(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			DatagramPacket startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(Parameters.SERVER_ADDRESS), this.streamingServerCtrlPort);
			proxyCM.sendToServer(startTX);
			
			this.serverStopped = false;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	private void sendRedirectToServer(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			DatagramPacket redirect = RelayMessageFactory.buildRedirect(0, InetAddress.getByName(Parameters.SERVER_ADDRESS), Parameters.SERVER_SESSION_PORT_IN);
			proxyCM.sendToServer(redirect);
			//this.serverStopped = false;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	private void sendStopTXToServer() {
		DatagramPacket stopTX;
		try {
			stopTX = RelayMessageFactory.buildStopTx(0, InetAddress.getByName(Parameters.SERVER_ADDRESS), this.streamingServerCtrlPort);
			proxyCM.sendToServer(stopTX);
			this.serverStopped = true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendAckClientReqToClient(){
		try {
			this.proxyStreamingCtrlPort = proxyCM.getLocalAdHocInputPort();
			//Creo un messaggio ACK_CLIENT_REQ
			DatagramPacket ackClientReq = RelayMessageFactory.buildAckClientReq(0, Parameters.CLIENT_PORT_SESSION_IN, 
					InetAddress.getByName(this.clientAddress), this.outStreamPort, this.proxyStreamingCtrlPort);
			
			proxyCM.sendTo(ackClientReq);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendServerUnreacheableToClient(){
		try {
			//invio SERVER_UNREACHABLE al client:
			DatagramPacket serverUnreach = RelayMessageFactory.buildServerUnreacheable(0, InetAddress.getByName(this.clientAddress), 
					Parameters.CLIENT_PORT_SESSION_IN);
			proxyCM.sendTo(serverUnreach);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
		
	private void sendLeaveMsgToClient() {
		try {
			//invio LEAVE al client:
			DatagramPacket leave = RelayMessageFactory.buildLeave(0, InetAddress.getByName(clientAddress), Parameters.CLIENT_PORT_SESSION_IN);
			proxyCM.sendTo(leave);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}

/*
 * Carlo: Ho aggiunto uno stato di handoff e uno di ended per indicare che il proxy � stato terminato.
 */
enum ProxyState {
	waitingServerRes, waitingClientAck, FirstReceivingromServer, 
	transmittingToClient, stopToClient, receivingRetransmission, 
	TransmittingTempBuffer, attemptingToStart, handoff, ended;
}
