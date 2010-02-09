/**
 * 
 */
package server;

import unibo.core.rtp.RTPSenderPS;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.NoDataSourceException;
import javax.media.Processor;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import parameters.DebugConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;

import com.sun.media.rtsp.protocol.Message;

import debug.DebugConsole;


import server.connection.ServerConnectionFactory;
import server.connection.StreamingServerCM;
/**
 * @author Leo Di Carlo
 *
 */
public class StreamingServer implements ControllerListener, Observer{

	private String filename = "";
	private int proxyCtrlPort;
	private int proxyReceiveStream;
	private Processor processor;
	private Object sync;
	private boolean failed;
	public RTPSenderPS transmitter;
	private String status;
	private DatagramPacket msg;
	private boolean started;
	private ServerSessionManager sessionManager;
	private InetAddress proxyAddress;
	private StreamingServerCM connManager;
	private int transmissionPort;
	private DebugConsole consolle;
	private File dir;
	private int transmissionCtrlPort;
	
	
	public StreamingServer(String filename, String proxyAddress, int proxyCtrlPort, int proxyReceiveStream, ServerSessionManager sessionManager, DebugConsole consolle) throws IOException, NoDataSourceException
	{
		this.consolle = consolle;
		this.proxyCtrlPort = proxyCtrlPort;
		this.proxyReceiveStream = proxyReceiveStream;
		this.status = "Created";
		this.started = false;
		this.sessionManager = sessionManager;
		this.proxyAddress = InetAddress.getByName(proxyAddress);

		this.connManager = ServerConnectionFactory.getStreamingServerConnectionManager(this);
		this.connManager.start();
		
		dir=new File(System.getProperty("user.dir")+"/"+"mp3");
		
		this.filename = "file://"+dir+"/"+filename;
		
		System.out.println("StreamingServer ==> File: "+this.filename+", indirizzo destinazione: "+this.proxyAddress+", porta di controllo destinazione: "+this.proxyCtrlPort+", sessionManager: "+this.sessionManager.toString()+", consolle: "+this.consolle.toString());
		
		
		
/*
//Valerio:ho commentato questa parte perchè per ora il mio protocollo non prevede messaggio ACK_RELAY_FORW
		//TODO ALLA FINE DEL COSTRUTTORE BISOGNA INVIARE AL PROXY IL MESSAGGIO AI ACK_RELAY_FORW
		msg = ServerMessageFactory.buildAckRelayForw(0, this.transmissionPort, this.transmissionCtrlPort,this.proxyAddress, this.proxyCtrlPort);
		this.connManager.sendTo(msg);
		this.connManager.start();
*/		
		this.transmissionPort = ServerPortMapper.INSTANCE.getFirstFreeRTPPort();
		this.transmissionCtrlPort = connManager.getStreamingServerCtrlPort();	

	
		
	}




	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable receiver, Object msg) {
		// TODO Auto-generated method stub
		this.msg = (DatagramPacket)msg;
		try {
			ServerMessageReader.readContent(this.msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("STREAMING_SERVER_UPDATE: impossibile leggere il pacchetto arrivato...");
			consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"STREAMING_SERVER_UPDATE: impossibile leggere il pacchetto arrivato...");
		}

		System.out.println("StreamingServer: è arrivato un datagramma");
		
		if(ServerMessageReader.getCode() == MessageCodeConfiguration.START_TX && !this.status.equals("Trasmitting"))
		{
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"STREAMING_SERVER_UPDATE: Arrivata richiesta di START_TX del file"+this.filename);
			System.out.println("STREAMING_SERVER_UPDATE: Arrivata richiesta di START_TX del file"+this.filename);			
			DataSource input = null;
			try{
				MediaLocator locator = new MediaLocator(this.filename);
				try {
					input = Manager.createDataSource(locator);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println("ERRORE NELLA CREAZIONE DEL MEDIALOCATOR");
				}
			}
			catch(NoDataSourceException x){
				System.err.println("Impossibile trovare il file specificato: "+filename);
				consolle.debugMessage(DebugConfiguration.DEBUG_ERROR,"STREAMING_SERVER: Impossibile trovare il file specificato: "+filename);
				try {
					throw x;
				} catch (NoDataSourceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sync=new Object();
			try {
				processor=javax.media.Manager.createProcessor(input);
			} catch (IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("ERRORE NELLA CREAZIONE DEL PROCESSOR: IOException");
				} catch (NoProcessorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("ERRORE NELLA CREAZIONE DEL PROCESSOR: NoProcessorException");
			}
			}
			processor.addControllerListener(this);
			processor.configure();
			synchronized(sync) {
				while (processor.getState()<Processor.Configured) {
					try { sync.wait(); }
					catch (InterruptedException e) {}
				}
			}
			if (failed)
				try {
					throw new IOException("Configure error");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
			processor.setContentDescriptor(cd);

			processor.realize();
			synchronized(sync) {
				while (processor.getState()<Processor.Realized) {
					try { sync.wait(); }
					catch (InterruptedException e) {}
				}
			}
			if (failed)
				try {
					throw new IOException("Realize error");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}


			

			System.out.println("StreamingServer ==> trasmissionPort: "+this.transmissionPort);
			System.out.println("StreamingServer ==> trasmissionCtrlPort: "+this.transmissionCtrlPort);
			
			try {
				System.out.println("StreamingServer: sono appena prima della creazione del RTPSenderPS con porta"+this.transmissionCtrlPort);
				//transmitter=new RTPSenderPS(this.transmissionPort);
				transmitter=new RTPSenderPS(this.transmissionPort, InetAddress.getByName(NetConfiguration.SERVER_ADDRESS));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("ERRORE NELLA CREAZIONE DEL TRANSMITTER");
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"Inizio trasmissione dalla porta "+this.transmissionPort);
			//System.out.println("StreamingServer: inizializzato transmitter");
			//transmitter.addDestination(InetAddress.getByName(proxyAddress),proxyReceiveStream);
			
			try {
				transmitter.addDestination(this.proxyAddress, proxyReceiveStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println("ERRORE NELL'ADDDESTINATION DEL TRANSMITTER");
			}
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"AddDestination eseguita: destinazione dello stream " + this.proxyAddress +":" + proxyReceiveStream );
			System.out.println("AddDestination eseguita: idestinazione dello stream " + this.proxyAddress +":" + proxyReceiveStream );
			
			
			try {
				transmitter.sendData(getDataOutput());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(!this.started)//era commentato da pire 
				try {
					//System.out.println("sono entrato in if(!this.started)");
					this.startProcessor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.println("ERRORE IN THIS.STARTPROCESSOR()");
				}
			this.status = "Trasmitting";
		
		if(ServerMessageReader.getCode() == MessageCodeConfiguration.STOP_TX && !this.status.equals("Stopped"))
		{
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"STREAMING_SERVER_UPDATE: Arrivata richista di STOP_TX");
			if(this.started) this.stopProcessor();
			this.status = "Stopped";
		}

	}

	public int getTransmissionPort(){
		return this.transmissionPort;
	}
	public int getTransmissionControlPort(){
		return this.transmissionCtrlPort;
	}


	public boolean redirect(String newProxyAddress){

		if(status.equals("Trasmitting"))
		{
			this.stopProcessor();
			try {
				if(this.proxyAddress != null)
				{
					transmitter.removeTarget(this.proxyAddress, this.proxyReceiveStream);
				}
				this.proxyAddress = InetAddress.getByName(newProxyAddress);
				transmitter.addDestination(this.proxyAddress, this.proxyReceiveStream);
				this.startProcessor();
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		if(status.equals("Stopped"))
		{
			try {
				if(this.proxyAddress != null)
				{
					transmitter.removeTarget(this.proxyAddress, this.proxyReceiveStream);
				}
				this.proxyAddress = InetAddress.getByName(newProxyAddress);
				transmitter.addDestination(this.proxyAddress, this.proxyReceiveStream);
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

/*//Valerio: facendo così l'errore sparisce ma da idea che non funzioni comunque
	public DataSource getDataOutput() {
		try{
			return processor.getDataOutput();
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
*/
	
	public DataSource getDataOutput() { //Valerio:questa è la getDataOutput originale
		return processor.getDataOutput();
	}


	
	public void startProcessor() throws IOException {
		System.out.println("startProcessor(): lo stato è "+this.status);
		if(this.status.equals("Created"))
			System.out.println("startProcessor(): dopo la sendData");
			processor.start();
			this.started = true;
			System.out.println("terminato startProcessor()");
	}

	public void stopProcessor() {
		processor.stop();
		this.started = false;
	}

	public void controllerUpdate(ControllerEvent ce) {
		// If there was an error during configure or realize, the processor will be closed
		if (ce instanceof ControllerClosedEvent) failed=true;
		if(ce instanceof EndOfMediaEvent){
			System.out.println("Media Finito");
			consolle.debugMessage(DebugConfiguration.DEBUG_INFO,"STREAMING_SERVER_CONTROLLER_UPDATE: Media Finito");
			if(processor!=null){
				processor.stop();
				processor.close();
				processor = null;
				transmitter.close();

			}
		}

		// All controller events, send a notification to the waiting thread in waitForState method.
		synchronized (sync) { sync.notifyAll(); }
	}
}
