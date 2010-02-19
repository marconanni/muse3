package dummies;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;


import client.gui.IClientView;

import parameters.*;


import relay.RTPReceptionManager;
import relay.RelayBufferManager;
import relay.RelayMessageFactory;
import relay.RelayMessageReader;
import relay.connection.RelayPortMapper;
import relay.RelaySessionManager;

import relay.connection.ProxyCM;
import relay.connection.RelayConnectionFactory;



import debug.*;





/**
 * 
 * @author marco nanni
 * 
 * Dummy di proxy estremo: non si cura minimamente del flusso,
 * non ne simula neanche uno, si limita a madare e ricevere i messaggi
 * e gli eventi lagati al cambio di relay
 * 
 *
 */
public class DummyDummyProxy extends Observable implements Observer{

	//flags
	public boolean debug = true;
	
	
	//variabili di stato, eventi, messaggi:
	public ProxyState state;
	public boolean newProxy;
	public String event;
	public DatagramPacket msg;
	

//	//timeout:
//	public TimeOutAckForward timeoutAckForward;
//	public TimeOutAckClientReq timeoutAckClientReq;
//	public TimeOutSessionInterrupted timeoutSessionInterrupted;
	
	//parametri:
	public String proxyID = "-1";
	public String filename;
	public String clientAddress;
	public String streamingServerAddress; //è l'indirizzo di chi manda il flusso al proxy
	public String futureStreamingAddress; // è l'indirizzo del nuovo big boss che manderà il flusso al proxy quando questo riceverà il messaggio
											// di LEAVE dal proxy sul vecchio relay
	
			
	public boolean servingClient; // se true indice che si eroga il flusso al client, se è false
	
	//porte(le porte di ricezione del proxy si trovano dentro l'RTPReceptionManager): 
	public int clientStreamPort = 0; 	//porta su cui il client riceve lo stream rtp 
	public int serverStreamPort = 0; 	//porta da cui lo streamingserver invia lo stream rtp
	public int outStreamPort = 0;		//porta da cui il proxy invia lo stream rtp
	public int inStreamPort = 0;       //porta da cui il proxy riceve lo str 
	public int streamingServerCtrlPort = 0; //porta su cui lo streamingserver riceve i messaggi di controllo
//	public int sessionControlPort;  //porta su cui il proxy riceve i messaggi di controllo dal client durante la trasmissione:
	public int proxyStreamingCtrlPort = 0;// la porte di controllo in ricezione aperta verso il client.
	public int clientStreamControlPort=0; // la porta di controllo del client (� valida e diversa da -1
	// solo se il client � un altro proxy
	
	
	
	// porte di sessione di sender e receiver
	public int streamingServerSessionPort;
	public int clientSessionPort;
	
	//componenti:
	public ProxyCM proxyCM;
	
	//classi di utilita':
	public RelayMessageReader msgReader;
	public DebugConsole consolle;
	
	

	public Observer sessionManager;
	private int recoveryStreamInPort;
	
	
	
	
	//INTERFACCIA GRAFICA 
	
	/*
	 * ***************************************************************
	 * **********************COSTRUTTORI******************************
	 * ***************************************************************
	 */
	
	
	/**
	 * Costruisce un nuovo dummy proxy, determina le porte locali sfruttando RelayportMappper e prende gli indirizzi locali dal file di configurazione
	 */
	public DummyDummyProxy (Observer sessionManager, String clientAddress, int clientStreamPort,  int serverStreamPort, String streamingServerAddress,   boolean servingClient){

		
		RelayPortMapper portMapper = RelayPortMapper.getInstance();
		
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		
		
		
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		this.serverStreamPort = serverStreamPort;
		this.outStreamPort = portMapper.getFirstFreeStreamOutPort();
		this.inStreamPort = portMapper.getFirstFreeStreamInPort();
		this.streamingServerAddress= streamingServerAddress;
		this.servingClient=servingClient;
		this.determinaPorteSessione(this.servingClient, this.streamingServerAddress); // determino le porte su cui mandare i messaggi di sessione ( v doc metodo)
		consolle.debugMessage("Inizializzazione del proxy in corso...");
		this.msgReader = new RelayMessageReader();
		this.consolle= new DebugConsole();
		
		try {
			
			// creo le porte di controllo
			int localClusterControlPortIn = portMapper.getFirstFreeControlAdHocInPort();
			this.proxyStreamingCtrlPort = localClusterControlPortIn;
			int	localClusterControlPortOut = portMapper.getFirstFreeControlAdHocOutPort();
			int localClusterHeadControlPortIn = portMapper.getFirstFreeControlManagedInPort();
			int localClusterHeadControlPortOut = portMapper.getFirstFreeControlManagedOutPort();
			
			
		InetAddress localClusterAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		
		InetAddress localClusterHeadAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		this.proxyCM = new ProxyCM(localClusterAddress, localClusterControlPortIn, localClusterControlPortOut, localClusterHeadAddress, localClusterHeadControlPortIn, localClusterHeadControlPortOut, this);
		this.proxyCM.start();
		}
		
		catch (Exception e){
			e.printStackTrace();
		}
		//proxy connection manager, e' osservato da this

	
	
	}
	
		
	
		/**
		 * Costruttore per un dummy dummy proxy sostitutivo, occorre specificare anche le porte locali di controllo e di streaming  che devono essere uguali a quelle del server
		 */
	public DummyDummyProxy (Observer sessionManager,  String clientAddress, int clientStreamPort, int proxyStreamPortOut, int proxyStreamPortIn, int serverStreamPort, int recoverySenderPort,String streamingServerAddress, String recoverySenderAddress, int localClusterControlPortIn , int localClusterControlPortOut, int localClusterHeadControlPortIn, int localClusterHeadControlPortOut, boolean servingClient){
			//	TODO: controllare che le porte siano tutte ben mappate
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		
		
		
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		this.serverStreamPort = serverStreamPort;
		this.outStreamPort = proxyStreamPortOut;
		this.inStreamPort = proxyStreamPortIn;
		this.streamingServerAddress= streamingServerAddress;
		this.servingClient=servingClient;
		this.determinaPorteSessione(this.servingClient, this.streamingServerAddress); // determino le porte su cui mandare i messaggi di sessione ( v doc metodo)
		consolle.debugMessage("Inizializzazione del proxy in corso...");
		String oldProxyAddress = recoverySenderAddress;
		this.msgReader = new RelayMessageReader();
		this.consolle= new DebugConsole();
		
		try {
		InetAddress localClusterAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		
		InetAddress localClusterHeadAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		this.proxyCM = new ProxyCM(localClusterAddress, localClusterControlPortIn, localClusterControlPortOut, localClusterHeadAddress, localClusterHeadControlPortIn, localClusterHeadControlPortOut, this);
		this.proxyCM.start();
		}
		
		catch (Exception e){
			e.printStackTrace();
		}
		
		
		
		
		this.sendRedirectToServer();

		
		
		
		
		
		/*
		 * chiedo al port mapper una porta in ricezione dalla quale ricevere lo stream dal vecchio relay
		 * ho copiato il modo in cui si ottiene la porta dal costruttore di RTPReceptionManager
		 */
		this.recoveryStreamInPort =RelayPortMapper.getInstance().getFirstFreeStreamInPort();
		
		
	
	}
	
	public DummyDummyProxy(Observer sessionManager, boolean newProxy,String clientAddress, int clientStreamPort,int proxyStreamPortOut, int proxyStreamPortIn,
			int serverStreamPort, int recoverySenderPort,
			String streamingServerAddress,
			String recoverySenderAddress, int serverCtrlPort,
			int proxyCtrlPort, boolean servingClient) {
		
		
		//	TODO: controllare che le porte siano tutte ben mappate
		this.sessionManager = sessionManager;
		this.addObserver(this.sessionManager);
		
		RelayPortMapper portMapper=RelayPortMapper.getInstance();
		
		this.clientAddress = clientAddress; 
		this.clientStreamPort = clientStreamPort;
		this.serverStreamPort = serverStreamPort;
		this.outStreamPort = proxyStreamPortOut;
		this.inStreamPort = proxyStreamPortIn;
		this.streamingServerAddress= streamingServerAddress;
		this.servingClient=servingClient;
		this.proxyStreamingCtrlPort = proxyCtrlPort;
		this.determinaPorteSessione(this.servingClient, this.streamingServerAddress); // determino le porte su cui mandare i messaggi di sessione ( v doc metodo)
		consolle.debugMessage("Inizializzazione del proxy in corso...");
		String oldProxyAddress = recoverySenderAddress;
		this.msgReader = new RelayMessageReader();
		this.consolle= new DebugConsole();
		
		try {
			
			int localClusterControlPortOut= portMapper.getFirstFreeControlAdHocOutPort();
			int localClusterHeadControlPortIn = portMapper.getFirstFreeControlManagedInPort();
			int localClusterHeadControlPortOut = portMapper.getFirstFreeControlManagedOutPort();
			
			
		InetAddress localClusterAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		
		InetAddress localClusterHeadAddress= InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_ADDRESS);
		this.proxyCM = new ProxyCM(localClusterAddress, proxyCtrlPort, localClusterControlPortOut, localClusterHeadAddress, localClusterHeadControlPortIn, localClusterHeadControlPortOut, this);
		this.proxyCM.start();
		}
		
		catch (Exception e){
			e.printStackTrace();
		}
		
		
		
		
		this.sendRedirectToServer();

		
		
		
		
		
		/*
		 * chiedo al port mapper una porta in ricezione dalla quale ricevere lo stream dal vecchio relay
		 * ho copiato il modo in cui si ottiene la porta dal costruttore di RTPReceptionManager
		 */
		this.recoveryStreamInPort =RelayPortMapper.getInstance().getFirstFreeStreamInPort();
		
		
		
		
		
		
		
		// TODO Auto-generated constructor stub
	}

	
	/*
	 * ***************************************************************
	 * *******************UPDATERs&LISTENERs**************************
	 * ***************************************************************
	 */
	
	



	/**
	 * @return la porta sulla quale il client riceve lo stream;
	 */
	public int getClientStreamPort() {
		return clientStreamPort;
	}

	/**
	 * @return la porta dalla quale il server eroga lo stream
	 */
	public int getServerStreamPort() {
		return serverStreamPort;
	}

	/**
	 * @return la porta dalla quale il proxy eroga lo stream al client.
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
			
			/*
			 * Marco. per ora, niente timeouts
			 */
			
//			if (debug)
//				consolle.debugMessage("Proxy: evento:" + arg);
//			
//			if (event.equals("TIMEOUTSESSIONINTERRUPTED"))
//			{
//				this.endProxy();
//			}
//			if (event.equals("TIMEOUTACKFORWARD") && this.state == ProxyState.waitingServerRes){
//				consolle.debugMessage("TimeoutAckFroward scaduto...");
//				/**
//				 * TODO: stampe sulla relativa intefaccia grafica
//				 */
//				sendServerUnreacheableToClient();
//				endProxy();
//				fProxy.getController().debugMessage(this.state.name());
//				System.err.println(this.state.name());
//			} 
//			else if(event.equals("TIMEOUTACKCLIENTREQ") && this.state == ProxyState.waitingClientAck){
//				endProxy();	
//				fProxy.getController().debugMessage(this.state.name());
//				System.err.println(this.state.name());
//			} 
//			
		}
		else if (arg instanceof DatagramPacket){
			
			this.msg = (DatagramPacket) arg;
			
			if (debug)
				consolle.debugMessage("Proxy: evento:" + arg);
			
			//leggo il messaggio:
			
				try {
					this.msgReader.readContent(msg);
				} 
				catch (IOException e) {
					System.err.println("PROXY (ID: "+ this.clientAddress+"): Errore nella lettura del Datagramma");
//					Logger.write(": Errore nella lettura del Datagramma");
					e.printStackTrace();
				}	
		
			
			
			
			// TODO: arrivo Leave
			 if (msgReader.getCode() == MessageCodeConfiguration.LEAVE){
				/*
				 * Quando arriva il LEAVE dal vecchio relay devo considerare come mio nuovo
				 * mittente il futureStreamingServer che il session manager mi ha comunicato
				 * quando è arrivato il messaggio di NEW_RELAY a seguito della ricezione 
				 * dell'election done.
				 * 
				 * Rimpicciolisco il buffer alle dimensioni normali
				 * (il sessionManager l'aveva ingrandito quando era arrivato l'election
				 * request del big boss).
				 * 
				 * 
				 */
				
				// il nuovo proxy eroga il flusso dalla stessa porta della quale lo erogava il vecchio.
				
				this.setStreamingServerAddress(futureStreamingAddress);
				
				consolle.debugMessage("Proxy: ricevuto Leave dal vecchio Big Boss, rimpicciolisco il buffer: il nuovo streamingServer sarà "+ this.streamingServerAddress);
				
				
				this.restrictNormalBuffer();
				
				/*
				 * Questa riga di codice serve solo se si estende il progetto, permettendo ad
				 * un relay secondario di divenare big boss bisogna quindi sistemare le porte di sessione,
				 * visto che riceve il flusso da un server e non più da un altro proxy, ne segue
				 * che le porte di sessione verso il server non sono più uguali alle porte
				 * di controllo del flusso.
				 */
				//this.determinaPorteSessione(servingClient, streamingServerAddress);
				
				
				
				
				
			}
			// TODO Arrivo Redirect
			else if (msgReader.getCode() == MessageCodeConfiguration.REDIRECT){
				/*
				 * C'� stata la rielezione di un nuovo relay secondario, il proxy sostitutivo
				 * sul nuovo relay secondario mi manda il redirect, affich� io, proxy sul big boss, mandi i flussi
				 * su di lui.
				 * Estraggo il suo indirizzo dal messaggio e invoco il metodo startHandoff che ridirige
				 * il flusso in uscita
				 */
				String newClientAddress = ((DatagramPacket)arg).getAddress().getHostAddress();
				consolle.debugMessage("Arrivato redirect ridirigo il flusso in uscita da "+ clientAddress + " a "+ newClientAddress);
				this.startHandoff(clientStreamPort, newClientAddress);
				
				
				 
				
			}
			 
		
		}
			
			
			
		
		
	}
	
	/* (non-Javadoc)
	 * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
	 */
	

	
	
	
	
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
	 * @param newClientAddress - indirizz del nuovo relay
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 * 
	 */
	public boolean startHandoff(int portStremInNewProxy, String newClientAddress){
		
		
		
		
		consolle.debugMessage("Proxy: il sessionManager mi ha detto di ridirigere il flusso verso "+ newClientAddress +" la porta è sempre la "+ portStremInNewProxy);

		return this.startHandoff(clientAddress,portStremInNewProxy, newClientAddress, true);
	
	}
	
	/**
	 * @author marco Nanni
	 * Metodo simile a StartHandoff, solo che non manda il leave al client 
	 * è usato dal proxy del big boss durante la rielezione di un relay secondario per ridirigere il flusso
	 * sul nuovo relay quando il nuovo proxy inivia a quello sul big boss il messaggio di redirect
	 * @param portStreamInNewClient - indica la porta rtp su cui � in ascolto il nuovo proxy
	 * @param newRelayAddr - indirizz del nuovo relay
	 * @return true se l'oerazione � andata a buon fine; false altrimenti
	 */
	
	public boolean redirectOutputStream(String oldClientAddress,int portStremInNewProxy, String newClientAddr){
		
		consolle.debugMessage("Proxy: il ridirigo il flusso da "+ oldClientAddress+ " a "+ newClientAddr  +" la porta è sempre la "+ portStremInNewProxy);
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
	public boolean startHandoff(String oldClientAddress, int portStremInNewClient, String newClientAddress, boolean alsoSendRedirectToclient){
		
			
			if (alsoSendRedirectToclient == true){
			//invio un messaggio di Leave al client
					sendLeaveMsgToClient();		 //informo il client che mi sto staccando
			}
			
			
			this.clientAddress= newClientAddress;
			this.clientStreamPort= portStremInNewClient;
			
		
			return true;
			

		
		
		
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
	
	
	
	
	
	
	
	
	
	
	
	
		
	
	/**
	 * @return the inStreamPort
	 */
	public int getInStreamPort() {
		return inStreamPort;
	}
	
	

	
	// TODO send redirectToServer.
	public void sendRedirectToServer(){
		/*
		 *  il server potrebbe anche essere un proxy sul big boss; ma il metodo non d� problemi
		 *  a riguardo, visto che  l'indirizzo viene fornito dal costruttore e la porta
		 *  di sessione viene stabilita in base al tipo di nodo che eroga il fusso dal
		 *  metodo determinaPorteSessione
		 */
		try {
			//Creo un messaggio REDIRECT e lo invio al server
			
			DatagramPacket redirect = RelayMessageFactory.buildRedirect(0,InetAddress.getByName(this.streamingServerAddress), this.streamingServerSessionPort);
			proxyCM.sendToServer(redirect);
			//this.serverStopped = false;
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	
	
	
	

	
	
	// TODO	sendleaveMessage to client!
	public void sendLeaveMsgToClient() {
		try {
			//invio LEAVE al client:
			DatagramPacket leave = RelayMessageFactory.buildLeave(0, InetAddress.getByName(clientAddress), this.clientSessionPort);
			proxyCM.sendTo(leave);
		} catch (UnknownHostException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}
	
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
	
	public void determinaPorteSessione(boolean servingClient, String streamingServerAddress){
		/*
		 * Se sto servendo un cliente la sua porta di sessione � quella dei client,
		 * infatti i messaggi di LEAVE arrivano  al ClientSessionManager, mentre
		 * start tx e top tx arrivano a ClientBufferDataPlaying
		 * Se sto servendo un relay arrivano ad un proxy e posso usare la porta di 
		 * controllo, come porta di sessione, visto che � il proxy a dover ricevere tutti i messaggi
		 * da me mandati ( compreso LEAVE).
		 * Discorso analogo per il server, i messaggi di sessione arrivano al
		 * server Session Manager, mentre StartTX e STOP TX a streamingServer
		 * Se sto ricevendo da un proxy su big boss,posso usare la porta di 
		 * controllo, come porta di sessione, visto che � il proxy a dover ricevere tutti i messaggi
		 * da me mandati ( compreso REDIRECT).
		 * auguri!		
		 */
		
		if(servingClient)
			clientSessionPort= PortConfiguration.CLIENT_PORT_SESSION_IN;
		else
				if (clientStreamControlPort != 0)
				clientSessionPort= this.clientStreamControlPort;
		
		if (streamingServerAddress.equals(NetConfiguration.SERVER_ADDRESS)) // in questo caso so che sto ricevendo il flusso dal server
			streamingServerSessionPort=PortConfiguration.SERVER_SESSION_PORT_IN;
		else
			streamingServerSessionPort= streamingServerCtrlPort;// ricevo il flusso da un altro proxy.
		
	}
	
	/**
	 * Metodo che ingrandisce il buffer normale del proxy, usato quando il proxy su un 
	 * relay secondario deve ingrandire i propri bufffer a causa della rielezione del big boss
	 */
	public void enlargeNormalBuffer (){
		consolle.debugMessage("Proxy: se fossi un vero proxy ingrandirei il buffer, ma sono un dummy e nonce l'ho!");
		
	}
	
	/**
	 * metodo richiamato all'arrivo ddi un LEAVE dal vecchio proxy che serviva questo
	 * qualora il proxy sia su un relay secondario e ci sia la rielezione del big boss.
	 * per ripristinare le dimensioni normali del buffer
	 */
	
	public void restrictNormalBuffer(){
		consolle.debugMessage("Proxy: se fossi un vero proxy rimpicciolirei il buffer, ma sono un dummy e nonce l'ho!");
	}
	
	
	// TODO  GETTERS -  SETTERS
	/***************GETTERS  - SETTERS ********************************************************/
	public String getStreamingServerAddress() {
		return streamingServerAddress;
	}

	public void setStreamingServerAddress(String streamingServerAddress) {
		this.streamingServerAddress = streamingServerAddress;
	}

	public String getFutureStreamingAddress() {
		return futureStreamingAddress;
	}

	public void setFutureStreamingAddress(String futureStreamingAddress) {
		this.futureStreamingAddress = futureStreamingAddress;
	}

	/**
	 * 
	 * @return true se si sta erongando il flusso verso un client,
	 * false, se si sta erongando il flusso verso un relay secondario
	 */
	public boolean isServingClient() {
		return servingClient;
	}

	public void setServingClient(boolean servingClient) {
		this.servingClient = servingClient;
	}
	
	

	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}
	
	/**
	 * @return the recoveryStreamInPort
	 */
	public int getRecoveryStreamInPort() {
		return recoveryStreamInPort;
	}

	
	public int getClientStreamControlPort() {
		return clientStreamControlPort;
	}
	
	

	
}

/*
 * Gli stati sono quelli del proxy vero: qui non li ho eliminati perchè inparte vengono ancora usati
 * 
 * 
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

