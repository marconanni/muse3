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

import debug.DebugConsolle;

import parameters.Parameters;
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
	private RTPSenderPS transmitter;
	private String status;
	private DatagramPacket msg;
	private boolean started;
	private ServerSessionManager sessionManager;
	private InetAddress proxyAddress;
	private StreamingServerCM connManager;
	private int transmissionPort;
	private DebugConsolle consolle;
	private File dir;
	private int transmissionCtrlPort;
	
	public StreamingServer(String filename, String proxyAddress, int proxyCtrlPort, int proxyReceiveStream, ServerSessionManager sessionManager, DebugConsolle consolle) throws IOException, NoDataSourceException
	{
		this.consolle = consolle;
		this.proxyCtrlPort = proxyCtrlPort;
		this.proxyReceiveStream = proxyReceiveStream;
		this.status = "Created";
		this.started = false;
		this.sessionManager = sessionManager;
		this.proxyAddress = InetAddress.getByName(proxyAddress);

		this.connManager = ServerConnectionFactory.getStreamingServerConnectionManager(this);
		
		dir=new File(System.getProperty("user.dir")+"/"+"mp3");
		
		this.filename = "file://"+dir+"/"+filename;
		
		DataSource input = null;
		try{
			MediaLocator locator = new MediaLocator(this.filename);
			input = Manager.createDataSource(locator);
		}
		catch(NoDataSourceException x){
			System.err.println("Impossibile trovare il file specificato: "+filename);
			consolle.debugMessage("STREAMING_SERVER: Impossibile trovare il file specificato: "+filename);
			throw x;
		}

		sync=new Object();
		try { processor=javax.media.Manager.createProcessor(input); }
		catch (NoProcessorException e) { throw new IOException(e.toString()); }
		processor.addControllerListener(this);

		processor.configure();
		synchronized(sync) {
			while (processor.getState()<Processor.Configured) {
				try { sync.wait(); }
				catch (InterruptedException e) {}
			}
		}
		if (failed) throw new IOException("Configure error");

		ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
		processor.setContentDescriptor(cd);

		processor.realize();
		synchronized(sync) {
			while (processor.getState()<Processor.Realized) {
				try { sync.wait(); }
				catch (InterruptedException e) {}
			}
		}
		if (failed) throw new IOException("Realize error");


		this.transmissionPort = ServerPortMapper.INSTANCE.getFirstFreeRTPPort();
		this.transmissionCtrlPort = connManager.getStreamingServerCtrlPort();
		transmitter=new RTPSenderPS(this.transmissionPort);
		//System.out.println("StreamingServer: inizializzato transmitter");
		transmitter.addDestination(InetAddress.getByName(proxyAddress),proxyReceiveStream);
		System.out.println("StreamingServer: addDestination eseguita: indirizzo destinazione: " + proxyAddress +" porta: " + proxyReceiveStream );

		//TODO ALLA FINE DEL COSTRUTTORE BISOGNA INVIARE AL PROXY IL MESSAGGIO AI ACK_RELAY_FORW
		msg = ServerMessageFactory.buildAckRelayForw(0, this.transmissionPort, this.transmissionCtrlPort,this.proxyAddress, this.proxyCtrlPort);
		this.connManager.sendTo(msg);
		this.connManager.start();
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
			consolle.debugMessage("STREAMING_SERVER_UPDATE: impossibile leggere il pacchetto arrivato...");
		}

		if(ServerMessageReader.getCode() == Parameters.START_TX && !this.status.equals("Trasmitting"))
		{
			consolle.debugMessage("STREAMING_SERVER_UPDATE: Arrivata richista di START_TX");
			if(!this.started) this.startProcessor();
			this.status = "Trasmitting";
		}
		if(ServerMessageReader.getCode() == Parameters.STOP_TX && !this.status.equals("Stopped"))
		{
			consolle.debugMessage("STREAMING_SERVER_UPDATE: Arrivata richista di STOP_TX");
			if(this.started) this.stopProcessor();
			this.status = "Stopped";
		}

	}



	public boolean redirect(String newProxyAddress){

		if(status.equals("Trasmitting"))
		{
			this.stopProcessor();  // Marco: i metodi di startProcessor e Stop Processor servono idealmente per far partire o far arrestare il flusso
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


	public DataSource getDataOutput() {
		return processor.getDataOutput();
	}


	public void startProcessor() {
		if(this.status.equals("Created"))
			try {
				transmitter.sendData(getDataOutput());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			processor.start();
			this.started = true;
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
			consolle.debugMessage("STREAMING_SERVER_CONTROLLER_UPDATE: Media Finito");
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
