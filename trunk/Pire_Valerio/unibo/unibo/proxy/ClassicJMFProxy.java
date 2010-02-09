package unibo.proxy;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import unibo.core.rtp.RTPReceiver;
import unibo.core.rtp.RTPSender;

/**
 * Inizializza e avvia il proxy per l'inoltro dei dati RTP.
 * Il proxy agisce da forwarder dei dati RTP, inoltrando i pacchetti ricevuti da un mittente verso un ricevente
 * (entrambi gli endpoint sono specificati come parametri del main).
 * Questa classe si basa sulle modalità previste da JMF e quindi non permette la modifica esplicita di alcun buffer.
 * @author Alessandro Falchi
 */
public class ClassicJMFProxy implements ControllerListener {
    private Processor processor;
    private Object sync;
    private boolean failed;
    
    public ClassicJMFProxy(DataSource input) throws IOException {
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


    /* args[0] è il numero di porta su cui il proxy ascolta l'arrivo dei dati RTP
     * args[1] è l'indirizzo IP del mittente
     * args[2] è la porta da cui trasmette il mittente
     * args[3] è la porta usata dal proxy per inoltrare i dati ricevuti
     * args[4] è l'indirizzo IP del destinatario del forwarding
     * args[5] è la porta di ascolto del destinatario
     */
    public static void main(String[] args) {
        if (args.length<6) {
            System.out.println("Errore nei parametri del main");
            System.exit(1);
        }
        try  {
    		RTPReceiver rtpRx=new RTPReceiver(Integer.parseInt(args[0]));
            rtpRx.setSender(InetAddress.getByName(args[1]),Integer.parseInt(args[2]));
            DataSource dsInput=rtpRx.receiveData();

            ClassicJMFProxy proxy=new ClassicJMFProxy(dsInput);

            RTPSender transmitter=new RTPSender(Integer.parseInt(args[3]));
            transmitter.addDestination(InetAddress.getByName(args[4]),Integer.parseInt(args[5]));
            transmitter.sendData(proxy.getDataOutput());

            proxy.startProcessor();
    		System.out.println("Proxy started");
        }
        catch (IOException e) { System.err.println(e); }
    }
}
