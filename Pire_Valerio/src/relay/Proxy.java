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
	private TimeOutAckForward timeoutAckForward;
	private TimeOutAckClientReq timeoutAckClientReq;
	private TimeOutSessionInterrupted timeoutSessionInterrupted;
	
	//parametri:
	private String proxyID = "-1";
	private String filename;
	private String clientAddress;
	int initialSupThreshold = (SessionConfiguration.PROXY_BUFFER*70)/100;
	
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
	private String relayAddress;
	private int relayControlPort;
	private int relayStreamingPort;
	
	
	private int bigbossStreamOut;
	private int bigbossControlPort;
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
	
	
	
	private boolean isBigBoss;
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
	public Proxy(Observer sessionManager,boolean newProxy, String filename,String relayAddress, int relayControlPort,int relayStreamPort, String clientAddress, int clientStreamPort, boolean isBigBoss, boolean servoUnClient) {
		
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
		
		
		
		try {
			//relay buffer manager, this e' un listener per gli eventi sollevati dal buffer nominale
			buffer = new RelayBufferManager(SessionConfiguration.PROXY_BUFFER, this.fProxy.getController(), 0,initialSupThreshold, this);
			//buffer.getNormalBuffer().addBufferEmptyEventListener(this);
			//buffer.getNormalBuffer().addBufferFullEventListener(this);
			
			this.rtpReceptionMan = new RTPReceptionManager(newProxy, buffer, this, isBigBoss);
			this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(this.isBigBoss && servoUnClient){//client->bigboss
			System.out.println("Proxy: mando la richiesta file al server");
			sendReqFileToServer();
			}
		else{
			if(!this.isBigBoss && servoUnClient){//client->relay
				//se non è un proxy di bigboss dovrò fare un altro metodo 
				System.out.println("Proxy: mando la richiesta file al BigBoss");
				sendReqFileToBigBoss();
			}
			else{
				if(this.isBigBoss && !servoUnClient){//relay->bigboss
					System.out.println("Proxy:mando la richiesta file al BigBoss");
					sendForwardReqFileToServer();
				}
			}
		}
		
		//imposto il timeout
		this.timeoutAckForward = RelayTimeoutFactory.getTimeOutAckForward(this, TimeOutConfiguration.TIMEOUT_ACK_FORWARD);
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
			
			buffer = new RelayBufferManager(SessionConfiguration.PROXY_BUFFER, this.fProxy.getController(), 0,initialSupThreshold, this);
			//creo un rtpreceptionamanger
			this.rtpReceptionMan = new RTPReceptionManager(newProxy, buffer, this.inStreamPort, this);
			
			//imposto la porta da cui il server invia lo stream 
			this.rtpReceptionMan.setStreamingServerSendingPort(serverStreamPort);
			
			//imposto la porta di ricezione normale
			//this.rtpReceptionMan.setNormalReceivingPort(proxyStreamPortIn);// Marco: perchè è commentato?
		} catch (IncompatibleSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

		
		/*
		 * Ho un'altro thread in ricezione: quello che riceve il flusso dal vecchio relay
		 */
//Valerio Questa parte non mi serve
/*	
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
*/		
		this.recoveryStreamInPort = rtpReceptionMan.getRecoveryReceivingPort();	
		//inizializzo la sessione di recovery (quella tra old proxy e new proxy)
//		this.initRecoverySession(recoverySenderPort, recoverySenderAddress); // Marco: non ci sto capendo niente: perchè è commentata?
		
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
				e.printStackTrace();
			}
			
			System.err.println("PROXY: sono nell'update  e mi è arrivato un datagramma con codice "+msgReader.getCode());
			
			
			

			if(msgReader.getCode()==MessageCodeConfiguration.FORWARD_ACK_REQ&&state==ProxyState.waitingServerRes&&isBigBoss){
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
					
					this.setChanged();
					this.notifyObservers(sessionports);
				}
				//imposto il timeout ack client req:
				this.timeoutAckClientReq = RelayTimeoutFactory.getTimeOutTimeOutAckClientReq(this, TimeOutConfiguration.TIMEOUT_ACK_CLIENT_REQ);
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
				this.timeoutAckClientReq = RelayTimeoutFactory.getTimeOutTimeOutAckClientReq(this, TimeOutConfiguration.TIMEOUT_ACK_CLIENT_REQ);
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
				
			}
			
			//valerio:il messaggio ACK_REQUEST_FILE può arrivare solo al bigboss
			if(msgReader.getCode()==MessageCodeConfiguration.ACK_REQUEST_FILE&&state==ProxyState.waitingServerRes){
				fProxy.getController().debugMessage(this.state.name());
				System.err.println(this.state.name());
				//reset timeout TimeOutAckForward
				this.timeoutAckForward.cancelTimeOutAckForward();
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
				this.timeoutAckClientReq = RelayTimeoutFactory.getTimeOutTimeOutAckClientReq(this, Parameters.TIMEOUT_ACK_CLIENT_REQ);
				
				//transito nel nuovo stato
				this.state = ProxyState.waitingClientAck;
			
			
			}

			if(msgReader.getCode() == MessageCodeConfiguration.START_TX){
				/*
				 * MArco:è arrivato un messaggio START_TX da parte del client
				  come si comporta il proxy dipende dallo stato in cui si trova il proxy, sono che sono molto criptici
				  * non si capisce che cosa significano e quando si verificano...
				*/
				if(this.timeoutSessionInterrupted!=null)
				{
					this.timeoutSessionInterrupted.cancelTimeOutSessionInterrupted();
				}
				if (state == ProxyState.waitingClientAck){					
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
					this.timeoutAckClientReq.cancelTimeOutAckClientReq();
					
					//invio il msg StartTX al server
					
					// Marco: in pratica dichiaro qui il thread, ed il codice contenuto nell suo metodo run anzichè farlo in un file separato
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
					runner.start(); // Marco: poi qui faccio partire il thread ( e quindi metto in esecuzione il metodo run)
					
					initNormalSession(); // Marco: qui dovrei far partire la trasmissione verso il client.
					
					if(isBigBoss)
						sendStartTXToServer();
					else
						sendStartTXToBigBoss();
					
					System.err.println("initNormalSession() FINITO");
				
					//avvio la ricezione

					System.out.print("fatto.");
					
					//transito nel nuovo stato
					this.state = ProxyState.FirstReceivingromServer;
										
				} else if(state == ProxyState.stopToClient){
					/*
					 * Marco: qui ho a tutti gli effetti un'altro modo per mandare lo stream, ma cosa sono sti stati?
					 * lo stato stop to client vuol dire che prima il client ha fermato la trsmissione mandandomi uno stop tx
					 * e a questo punto dovrei riprenderla
					 */
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
						
					}
					
				} else if (state == ProxyState.attemptingToStart && !this.newProxy){
					/*
					 * Marco: qui non ci si capisce nulla: sta fa cendo una trasmissione normale e non di recovery, ma il suo stato
					 * è comunque trasmitting temp buffer?? 
					 */
					
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
			}
			
			
			else
				if(msgReader.getCode() == MessageCodeConfiguration.STOP_TX){
				
				/*
				 * Marco: qui finalemte le cose sono più chiare: arriva uno stop TX dal client,
				 * sistemo i timeout e metto in pausa lo stream verso il client
				 * transito nello stato stoptoclient ( che vorrebbe dire che i client mi ha messo in stop)
				 */
				
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
			
			//invio un messaggio di Leave al client
			sendLeaveMsgToClient();		 //informo il client che mi sto staccando
			
			try {
				//elimino il client dalla lista dei destinatari del sender
				rtpSender.removeTarget(InetAddress.getByName(clientAddress), clientStreamPort);
				
				//dirotto la connessione in out verso il client al new proxy
				this.clientStreamPort = portStremInNewProxy;  // port stremInNewProxy è la porta sulla quale il nuovo proxy riceve il flusso dal vecchio proxy
				
				//aggiungo il nuovo relay cm destinatario inserendo la porta e l'indirizzo del proxy sul nuovo relay
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
			 
			//in questo caso il client è il new proxy
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
			if(isBigBoss){
				rtpSender = new RTPSenderPS(outStreamPort,InetAddress.getByName(NetConfiguration.BIGBOSS_AD_HOC_ADDRESS));
			}
			else{
				rtpSender = new RTPSenderPS(outStreamPort,InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS));
			}
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
		/*
		 * Questa funzione dovrebbe far partire lo stream sul normal buffer
		 */
		
		//si occupa anche di attaccare il mux al buffer normale
		if (muxTh == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
		
			try {
				RTPMultiplexer mux = new RTPMultiplexer(this.rtpReceptionMan.getTrackFormats());
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				CircularBuffer[] b = this.rtpReceptionMan.getNormalParserThread().getOutputBufferSet();
				try {
					muxTh = new MuseMultiplexerThread(mux, b,null,5);
					muxTh.setTimeToWait(SessionConfiguration.TTW-48);
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
		/*
		 * Marco: questa dovrebbe far partire lo stream usando i frames presenti nel recovery buffer
		 */
		
		//si occupa anche di attaccare il mux al buffer normale
		if (muxThR == null || state == ProxyState.receivingRetransmission || state == ProxyState.TransmittingTempBuffer){
		
			try {
				RTPMultiplexer mux = new RTPMultiplexer(this.rtpReceptionMan.getTrackFormats2());
				//creo un MultiplexerThread
				//muxTh = new MultiplexerThreadEV(rtpMux, buffer.getNormalBuffer(),null , this.clientAddress);
				CircularBuffer[] b = this.rtpReceptionMan.getRecoveryParserThread().getOutputBufferSet();
				try {
					muxThR = new MuseMultiplexerThread(mux, b,null,5);
					muxThR.setTimeToWait(SessionConfiguration.TTW-48);
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
			DatagramPacket reqFile =  RelayMessageFactory.buildReqFile(0, filename, proxyCM.getLocalAdHocInputPort(), this.inStreamPort, this.clientAddress, PortConfiguration.CLIENT_PORT_SESSION_IN, this.clientStreamPort, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), PortConfiguration.SERVER_SESSION_PORT_IN);
			System.out.println("il messaggio di request file che il relay manda al server è:\n0, "+filename+", "+proxyCM.getLocalAdHocInputPort()+", "+this.inStreamPort+", "+this.clientAddress+", "+PortConfiguration.CLIENT_PORT_SESSION_IN+", "+this.clientStreamPort);
			proxyCM.sendToServer(reqFile);	
			//this.timeoutAckForward = RelayTimeoutFactory.getTimeOutAckForward(this, Parameters.TIMEOUT_ACK_FORWARD);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void sendReqFileToBigBoss(){//client->relay->bigboss
		System.out.println("sendReqFileToBigBoss");
		this.inStreamPort = rtpReceptionMan.getNormalReceivingPort();
		DatagramPacket reqFile;
		try {
			reqFile = RelayMessageFactory.buildForwardReqFile(0, filename, 0, 0,"questo campo non mi serve, lo riempirà bigboss", proxyCM.getLocalAdHocInputPort(), this.inStreamPort, this.clientAddress,  this.clientStreamPort, InetAddress.getByName(NetConfiguration.BIGBOSS_AD_HOC_ADDRESS), PortConfiguration.RELAY_SESSION_AD_HOC_PORT_IN);
			System.out.println("il messaggio di request file che il relay manda al server è:\n0, "+filename+", "+proxyCM.getLocalAdHocInputPort()+", "+this.inStreamPort+", "+this.clientAddress+", "+this.clientStreamPort);
			proxyCM.sendToServer(reqFile);	
		} catch (Exception e) {
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
	
	//@ TODO commentare i metodi per preparare i messaggi.

	private void sendStartTXToServer(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			DatagramPacket startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), this.streamingServerCtrlPort);
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

	private void sendStartTXToBigBoss(){
		try {
			//Creo un messaggio START_TX e lo invio al server
			DatagramPacket startTX = RelayMessageFactory.buildStartTx(0, InetAddress.getByName(NetConfiguration.BIGBOSS_AD_HOC_ADDRESS), this.bigbossControlPort);
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
		/*
		try {
			
			DatagramPacket redirect = RelayMessageFactory.buildRedirect(0, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), PortConfiguration.SERVER_SESSION_PORT_IN);
			proxyCM.sendToServer(redirect);
			//this.serverStopped = false;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	
	
	
	private void sendStopTXToServer() {
		DatagramPacket stopTX;
		try {
			stopTX = RelayMessageFactory.buildStopTx(0, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS), this.streamingServerCtrlPort);
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
	
	private void sendStopTXToBigBoss(){
		DatagramPacket stopTX;
		try {
			stopTX = RelayMessageFactory.buildStopTx(0, InetAddress.getByName(NetConfiguration.BIGBOSS_AD_HOC_ADDRESS), this.bigbossControlPort);
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
			DatagramPacket ackClientReq = RelayMessageFactory.buildAckClientReq(0, PortConfiguration.CLIENT_PORT_SESSION_IN,InetAddress.getByName(this.clientAddress), this.outStreamPort, this.proxyStreamingCtrlPort);
			proxyCM.sendTo(ackClientReq);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendAckFileToRelay(String clientAddr,int clientControlPort, int clientStreamPort){//messaggio che il bigboss invia al relay
		try{
			this.proxyStreamingCtrlPort=proxyCM.getLocalAdHocInputPort();
			DatagramPacket ackRelayReq=RelayMessageFactory.buildForwardAckReq(0,PortConfiguration.CLIENT_PORT_SESSION_IN,InetAddress.getByName(this.clientAddress),this.outStreamPort,this.proxyStreamingCtrlPort,clientAddr,clientControlPort,clientStreamPort);
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
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
	}
		
	private void sendLeaveMsgToClient() {
//		try {
//			//invio LEAVE al client:
//			DatagramPacket leave = RelayMessageFactory.buildLeave(0, InetAddress.getByName(clientAddress), PortConfiguration.CLIENT_PORT_SESSION_IN);
//			proxyCM.sendTo(leave);
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
	}
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
