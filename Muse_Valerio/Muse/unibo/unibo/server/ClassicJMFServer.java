package unibo.server;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import unibo.core.rtp.RTPSender;

/**
 * Inizializza e avvia un server per l'invio con protocollo RTP del contenuto multimediale prelevato da un file locale.
 * Questa classe si basa sulle modalità previste da JMF. 
 * @author Alessandro Falchi
 */
public class ClassicJMFServer implements ControllerListener {
    private Processor processor;
    private Object sync;
    private boolean failed;
    
    public ClassicJMFServer(DataSource input) throws IOException {
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

    
    public DataSource getDataOutput() {
        return processor.getDataOutput();
    }
    

    public void startProcessor() {
        processor.start();
    }


    public void controllerUpdate(ControllerEvent ce) {
	    // If there was an error during configure or realize, the processor will be closed
	    if (ce instanceof ControllerClosedEvent) failed=true;

	    // All controller events, send a notification to the waiting thread in waitForState method.
	    synchronized (sync) { sync.notifyAll(); }
	}

    
    /* args[0] è il percorso del file contenente il contenuto multimediale da spedire. Tale percorso è in formato URL;
     * ad esempio:
     * 		file://C:/test/video/starwars.mov
     * 		file://video//starwars.mov
     * args[1] è la porta da cui trasmette il server
     * args[2] è l'indirizzo IP del destinatario
     * args[3] è la porta di ascolto del destinatario
     */
    public static void main(String[] args) {
        try {
    		DataSource ds=Manager.createDataSource(new MediaLocator(args[0]));
            
    		ClassicJMFServer server=new ClassicJMFServer(ds);
            
            RTPSender transmitter=new RTPSender(Integer.parseInt(args[1]));
            transmitter.addDestination(InetAddress.getByName(args[2]),Integer.parseInt(args[1]));
            transmitter.sendData(server.getDataOutput());
            
            server.startProcessor();
            System.out.println("Processor started");
        }
        catch (Exception e) { System.err.println(e); }
    }
}
