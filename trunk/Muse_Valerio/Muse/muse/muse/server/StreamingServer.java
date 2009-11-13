package muse.server;
/**
 * StreamingServer.java
 * Server che si occupa della trasmissione del flusso RTP verso il proxy;
 * � istanziato ad ogni nuova richiesta proveniente dal client
 * @author Ambra Montecchia
 * @version 1.0
 * */
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
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
import unibo.core.rtp.RTPSenderPS;

public class StreamingServer extends Thread implements ControllerListener{
	
	private Processor processor;
    private Object sync;
    private boolean failed;
    //nome del file da trasmettere
    private String filename;
    //porta su cui trasmette lo StreamingServer
    private int localPort;
    //indirizzo IP del proxy
    private String proxyIP;
    //porta su cui riceve il proxy
    private int proxyPort;
    //console di debug
    private DebugConsolle dc;
    RTPSenderPS transmitter;
    
    /**
     * Costruttore
     * */
    public StreamingServer(String filename, int localPort, String proxyIP, int proxyPort, DebugConsolle dc) throws IOException, NoDataSourceException{
    	
    	DataSource input = null;
    	try{
    		MediaLocator locator = new MediaLocator(filename);
    		input = Manager.createDataSource(locator);
    	}
    	catch(NoDataSourceException x){
    		System.err.println("Impossibile trovare il file specificato: "+filename);
    		throw x;
    	}
    	this.filename = filename;
    	this.localPort = localPort;
    	this.proxyIP = proxyIP;
    	this.proxyPort = proxyPort;
    	this.dc = dc;
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
    }
    
    /**
     * Metodo che restituisce il data source relativo allo stream RTP
     * @return il data source
     * */
    public DataSource getDataOutput() {
        return processor.getDataOutput();
    }
    

    public void startProcessor() {
        processor.start();
    }


    public void controllerUpdate(ControllerEvent ce) {
	    // If there was an error during configure or realize, the processor will be closed
	    if (ce instanceof ControllerClosedEvent) failed=true;
	    if(ce instanceof EndOfMediaEvent){
	    	System.out.println("Media Finito");
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
    
    /**
     * Metodo run per la trasmissione dello stream RTP verso il proxy
     * */
    public void run(){
    	try {
    		dc.debugMessage("StreamingServer: Inizio Trasmissione dalla porta:"+localPort);
            transmitter=new RTPSenderPS(localPort);
            System.out.println("StreamingServer: inizializzato transmitter");
            transmitter.addDestination(InetAddress.getByName(proxyIP),proxyPort);
            System.out.println("StreamingServer: addDestination eseguita");
            transmitter.sendData(getDataOutput());
            dc.debugMessage("Server: Trasmissione in corso a "+proxyIP+":"+proxyPort);
            startProcessor();
            System.out.println("Processor started");
        }
        catch (Exception e) { System.err.println(e); }
    }
}
