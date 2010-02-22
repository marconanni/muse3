package relay;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.control.BitRateControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.ReceiveStreamEvent;

import parameters.NetConfiguration;

import relay.connection.RelayPortMapper;


//import muse.proxy.ProxySender;


import unibo.core.multiplexer.RTPMultiplexer;
import unibo.core.parser.RTPParser;
import unibo.core.rtp.RTPReceiverPS;
import unibo.core.thread.MuseMultiplexerThread;
import unibo.core.thread.ParserThreadPS;

/**
 * @author Carlo Di Fulco
 * @created 27-nov-2008 20.27.17
 */

public class RTPReceptionManager implements ReceiveStreamListener {

	private boolean debug = true;

	private RelayBufferManager buffer;

	private boolean nexProxy;

	/*
	 * Connessione Nominale:
	 */
	private int normalReceivingPort;
	private int streamingServerSendingPort;

	private DataSource normalDataIn;
	private BitRateControl normalBRCtrl;
	private ParserThreadPS normalParserThread;
	private RTPReceiverPS normalReceiver;
	private RTPParser normalRTPParser;

	/*
	 * Connessione di recovery:
	 */
	private int recoveryReceivingPort;

	/**
	 * @return the eOM
	 */
	public boolean isEOM() {
		return EOM;
	}

	private DataSource recoveryDataIn;
	private BitRateControl recoveryBRCtrl;
	private ParserThreadPS recoveryParserThread;
	/**
	 * @return the recoveryParserThread
	 */
	public ParserThreadPS getRecoveryParserThread() {
		return recoveryParserThread;
	}

	private RTPReceiverPS recoveryReceiver;
	private RTPParser recoveryRTPParser;

	private Format[] trackFormats;
	private Format[] trackFormats2;

	private Proxy proxy;

	private boolean EOM = false;
	
	private String localClusterHeadAddr;
	private String connectedClusterHeadAddr;
	/*
	 * *********************************************************
	 * ***********************COSTRUTTORI***********************
	 * *********************************************************
	 */

	/**
	 * Costruttore: crea un'istanza del ReceptionManager in grado di gestire solo la ricezione
	 * nominale se neProxy == true, altrimenti ne crea uno che gestisce sia la ricezione nominale 
	 * che quella di recovery. 
	 * <br/>
	 * @param newProxy 
	 * @param RelayBufferManager buffer
	 * @throws IOException
	 * @throws IncompatibleSourceException
	 */
	//Valerio: passare per parametro l'indirizzo
	public RTPReceptionManager(boolean newProxy, RelayBufferManager buffer, Proxy proxy, boolean bigboss, String indirizzoRicezione, String indirizzoTrasmissione) throws IOException, IncompatibleSourceException{
		this.buffer = buffer;
		this.proxy = proxy;
		this.nexProxy = newProxy;
		this.localClusterHeadAddr=indirizzoRicezione;
		this.connectedClusterHeadAddr=indirizzoTrasmissione;
		//ottengo una porta per lo stream in input
		normalReceivingPort = RelayPortMapper.getInstance().getFirstFreeStreamInPort();	
		//normalReceiver = new RTPReceiverPS(normalReceivingPort);
		//indirizzoricezione altro non è che localclusterheadaddress, indirizzotramissione è connectedclusterheadaddress
		normalReceiver = new RTPReceiverPS(normalReceivingPort,InetAddress.getByName(this.localClusterHeadAddr),InetAddress.getByName(this.connectedClusterHeadAddr));
		if(newProxy){
			this.recoveryReceivingPort = -1;
			recoveryReceiver = null;
			recoveryRTPParser = null;
			recoveryParserThread = null;
		}
//		}else{
//			this.recoveryReceivingPort = RelayPortMapper.getInstance().getFirstFreeStreamInPort();
//			recoveryReceiver = new RTPReceiverPS(recoveryReceivingPort,InetAddress.getByName(this.localClusterHeadAddr),InetAddress.getByName(this.connectedClusterHeadAddr));
//		}
	}

	
	/**
	 * Crea un receptionManager per un proxy Sostitutivo che riceve sia il flusso dall'erogatore 
	 * che mandava il flusso al vecchio proxy, sia il flusso di handoff proveniente dal vecchio relay.
	 * @param newProxy
	 * @param buffer bufferManager che contiene i due buffer nei quali andranno a finire i flussi
	 * @param oldRelayAddress l'indirizzo inferiore del vecchio relay
	 * @param oldProxyStreamInPort la porta dalla quale il vecchio relay riceveva i flussi
	 * @param indirizzoRicezione l'indirizzo del relay sul quale si riceve il fusso
	 * @param indirizzoTrasmissione l'indirizzo di chi manda il flusso
	 * @param proxy 
	 * @throws IOException
	 * @throws IncompatibleSourceException
	 */
	public RTPReceptionManager(boolean newProxy, RelayBufferManager buffer,String oldRelayAddress, int oldProxyStreamInPort,  String indirizzoRicezione, String indirizzoTrasmissione, Proxy proxy) throws IOException, IncompatibleSourceException{
		this.buffer = buffer;
		this.localClusterHeadAddr=indirizzoRicezione;
		this.connectedClusterHeadAddr=indirizzoTrasmissione;

		this.proxy = proxy;
		//imposto la porta di ricezione del proxy di recovery con la vecchia porta di ricezione del  vecchio proxy
		normalReceivingPort = oldProxyStreamInPort;	
		
		normalReceiver = new RTPReceiverPS(normalReceivingPort,InetAddress.getByName(indirizzoRicezione),InetAddress.getByName(indirizzoTrasmissione));
		System.out.println("*******OldPortProxy:"+oldProxyStreamInPort);
		RelayPortMapper.getInstance().setRangePortInRTPProxy(normalReceivingPort);
		//ottengo dal port mapper la porta di ricezione dello stream proveniente dal vecchio proxy
		this.recoveryReceivingPort = RelayPortMapper.getInstance().getFirstFreeStreamInPort();
		System.out.println("******Recovery PORT:"+this.recoveryReceivingPort);
		/*
		 * Non abbiamo capito come funziona il costruttore di recoveryreceiver 
		 */
		recoveryReceiver = new RTPReceiverPS(recoveryReceivingPort,InetAddress.getByName(indirizzoRicezione), InetAddress.getByName(oldRelayAddress));
		}


	/*
	 * *********************************************************
	 * *******************STARTERs&CLOSERs**********************
	 * *********************************************************
	 */

	/**
	 * 
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws IncompatibleSourceException 
	 */
	public void initNormalConnection() throws UnknownHostException, IOException, IncompatibleSourceException{
		System.out.println("############à INITNORMALCONNECTION ");
		normalReceiver.setSender(InetAddress.getByName(this.connectedClusterHeadAddr), streamingServerSendingPort);//da dove prende streamingServerPort
		//	normalReceiver.setBufferLength(this.buffer.getBufSize());
		System.out.println("############à INITNORMALCONNECTION 3");
		normalReceiver.addReceiveStreamEventListener(this);

		System.err.println("Indirizzo da cui ricevo "+this.connectedClusterHeadAddr+":"+streamingServerSendingPort);
		
		System.out.println("Proxy receiver: attesa ricezione stream...");

		//ricezione dello stream RTP
		normalDataIn = normalReceiver.receiveData(); // Marco : suppongo questa receive sia bloccante

		System.out.println("Proxy receiver: datasource ricevuto...");

		//PARSER
		//lettura e memorizzazione dei frame che compongono lo stream RTP
		System.out.println("Proxy receiver: attesa ricezione stream...");
		normalRTPParser = new RTPParser(normalDataIn);
		System.out.println("Creato RTPParser...");
		this.trackFormats = normalRTPParser.getTracksFormat();
		normalParserThread = new ParserThreadPS(normalRTPParser, buffer.getBufSize(), buffer.getNormalBuffer());
		System.out.println("ParserThreadPS...");
	}

	/**
	 * 
	 * @throws IOException 
	 * @throws IncompatibleSourceException 
	 */
	public void initRecoveryConnection(int senderPort, InetAddress senderAddress) throws IOException, IncompatibleSourceException{

		if (!nexProxy) {
			recoveryReceiver.setSender(senderAddress, senderPort);
			//	recoveryReceiver.setBufferLength(buffer.getBufSize());
			recoveryReceiver.addReceiveStreamEventListener(this);

			System.out.println("Proxy receiver: attesa ricezione stream...");

			//ricezione dello stream RTP
			recoveryDataIn = recoveryReceiver.receiveData();
			recoveryBRCtrl = (BitRateControl) recoveryDataIn.getControl("javax.media.control.BitRateControl");
			System.out.println("Proxy receiver: datasource ricevuto...");

			/** PARSER **/
			//lettura e memorizzazione dei frame che compongono lo stream RTP
			System.out.println("Proxy receiver: attesa ricezione stream...");
			recoveryRTPParser = new RTPParser(recoveryDataIn);
			this.trackFormats2 = recoveryRTPParser.getTracksFormat();
			recoveryParserThread = new ParserThreadPS(recoveryRTPParser, buffer.getBufSize(), buffer.getRecoveryBuffer());
		}
	}
	
	
	/**
	 * @author Marco
	 * Questo metodo richiama il metodo SetSender di NormalReceiver, la speranza è che,
	 * usato qunado il proxy riceve un leave dal big boss questo serva a ricevere il flusso
	 * dal nuovo proxy sul big boss
	 * @param connectedClusterHeadAddr l'indirizzo del nuovo nodo che eroga il flusso
	 * @param streamingServerSendingPort la porta dalla quale il nuovo nodo eroga il flusso
	 * 
	 */
	
	public boolean setStreamingServer (String connectedClusterHeadAddr, int streamingServerSendingPort){
		// prima aggiorno le mie variabili interne
		boolean result = false;
		this.connectedClusterHeadAddr= connectedClusterHeadAddr;
		this.streamingServerSendingPort= streamingServerSendingPort;
		// poi invoco il metodo su nomalReceiver
		try {
			
			normalReceiver.setSender(InetAddress.getByName(this.connectedClusterHeadAddr), streamingServerSendingPort);
			result = true;
		} catch (UnknownHostException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	
	
	}
	
	
	

	/**
	 * 
	 * @return true se la connessione � stata avviata, false altrimenti
	 */
	public boolean startNormalConnection(){
		if (normalParserThread!=null){
			normalParserThread.start();
			System.out.println("ParserThreadPS partito");
			return true;
		}
		return false;
	}


	/**
	 * 
	 * @return true se la connessione � stata avviata, false altrimenti
	 */
	public boolean startRecoveryConnection(){
		if (recoveryParserThread!=null){
			recoveryParserThread.start();
			return true;
		}
		return false;
	}

	/**
	 * Chiude le connessioni e libera le risorse.
	 */
	public void closeAll()
	{
		if(normalParserThread!=null) closeNormalConnection();
		if(normalRTPParser!=null) normalRTPParser.close();
		if(normalDataIn!=null) normalDataIn.disconnect();
		if(normalReceiver!=null) normalReceiver.close();

		if(recoveryParserThread!=null) closeRecoveryConnection();
		if(recoveryRTPParser!=null) recoveryRTPParser.close();
		if(recoveryDataIn!=null) recoveryDataIn.disconnect();
		if(recoveryReceiver!=null) recoveryReceiver.close();
	}


	/*
	 * *********************************************************
	 * *******************GETTERs&SETTERs***********************
	 * *********************************************************
	 */ 

	/**
	 * @return the normalReceivingPort
	 */
	public int getNormalReceivingPort() {
		return normalReceivingPort;
	}

	/**
	 * @return the recoveryReceivingPort
	 */
	public int getRecoveryReceivingPort() {
		return recoveryReceivingPort;
	}

	/**
	 * Restituisce il formato dei dati nominali ricevuti.
	 * 
	 * @return Format[]
	 */
	public Format[] getNormalTracksFormat(){
		return normalRTPParser.getTracksFormat();
	}

	/**
	 * Se il flusso di recovery e' stato inizializzato, restituisce il formato 
	 * dei dati di recovery, altrimenti ritorna null
	 * 
	 * @return Format[]
	 */
	public Format[] getRecoveryTracksFormat(){
		if (recoveryRTPParser != null){
			return recoveryRTPParser.getTracksFormat();
		}else return null;
	}

	public int getNormalBitRate(){
		if(normalBRCtrl!= null){
			return normalBRCtrl.getBitRate();
		}
		else
			return -1;
	}

	public int getRecoveryBitRate(){
		if(recoveryBRCtrl!= null){
			return recoveryBRCtrl.getBitRate();
		}
		else
			return -1;
	}

	/**
	 * @return the streamingServerSendingPort
	 */
	public int getStreamingServerSendingPort() {
		return streamingServerSendingPort;
	}

	/**
	 * @param streamingServerSendingPort the streamingServerSendingPort to set
	 */
	public void setStreamingServerSendingPort(int streamingServerSendingPort) {
		this.streamingServerSendingPort = streamingServerSendingPort;
	}


	/**
	 * @param normalReceivingPort the normalReceivingPort to set
	 */
	public void setNormalReceivingPort(int normalReceivingPort) {
		this.normalReceivingPort = normalReceivingPort;
	}

	/*
	 * *********************************************************
	 * ************************UPDATERs*************************
	 * *********************************************************
	 */



	/* (non-Javadoc)
	 * @see javax.media.rtp.ReceiveStreamListener#update(javax.media.rtp.event.ReceiveStreamEvent)
	 */
	@Override
	public void update(ReceiveStreamEvent event) {
		if (debug)
			System.out.println("RTPReceptionManager. Intercettato evento: " + event);

		if (event instanceof ByeEvent)
		{
			System.out.println("___Bye Message in PROXY RECEIVER___\n");
			this.EOM = true;
//
//			closeNormalConnection();
//
//			closeRecoveryConnection();
//
//			com.sun.media.ExtBuffer frame=new com.sun.media.ExtBuffer();
//			frame.setEOM(true);
//
//			try{ Thread.sleep(100); }
//			catch(InterruptedException e){ System.err.println(e); }
//
//			buffer.getNormalBuffer().setFrame(frame);
//			if(this.proxy.getMuxTh().isPauseMultiplexing() || !buffer.getNormalBuffer().isEmpty())
//				this.proxy.getMuxTh().restart();
//			if(buffer.getRecoveryBuffer() != null){
//				com.sun.media.ExtBuffer recframe=new com.sun.media.ExtBuffer();
//				recframe.setEOM(true);
//				buffer.getRecoveryBuffer().setFrame(recframe);
//				if(this.proxy.getMuxThR().isPauseMultiplexing() || !buffer.getRecoveryBuffer().isEmpty())
//					this.proxy.getMuxThR().restart();
//					
//			}

		//	closeAll();

			System.out.println("Proxy: terminata trasmissione");
			
			System.out.println("Flush del buffer...");
			
		}


	}


	/*
	 * *********************************************************
	 * ********************METODI PRIVATI***********************
	 * *********************************************************
	 */

	private void closeNormalConnection(){
		if(normalParserThread!=null){
			normalParserThread.close();
			normalParserThread=null;
		}
		if(recoveryParserThread!=null){
			recoveryParserThread.close();
			recoveryParserThread=null;
		}
	}

	private void closeRecoveryConnection(){
		if(normalDataIn!=null){
			normalDataIn.disconnect();
			normalDataIn=null;
		}

		if(recoveryDataIn!=null){
			recoveryDataIn.disconnect();
			recoveryDataIn=null;
		}
	}

	public Format[] getTrackFormats() {
		return trackFormats;
	}

	public Format[] getTrackFormats2() {
		return trackFormats2;
	}
	public ParserThreadPS getNormalParserThread() {
		return normalParserThread;
	}

}