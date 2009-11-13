package unibo.core.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.ActiveReceiveStreamEvent;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.InactiveReceiveStreamEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.control.BufferControl;

/**
 * <p>Questa classe è un semplice ricevitore di traffico RTP basato su un'istanza di una classe che implementa
 * l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
 * semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano ricevere
 * dati RTP in modalità unicast sul nodo locale.</p>
 * <p>Inoltre, è implementata l'interfaccia ReceiveStreamListener, così che siano intercettati gli eventi relativi
 * alla ricezione di un flusso RTP.</p> 
 * @author Alessandro Falchi
 */
public class RTPReceiver implements ReceiveStreamListener {
    private RTPManager rtpManager;
    private DataSource receivedDataSource;
    private Object dataSync;
    
    /**
     * Costruttore. L'indirizzo del ricevitore è quello del host locale.
     * @param localPort la porta locale su cui attendere il traffico RTP
     * @throws IOException se l'inizializzazione fallisce
     */
    public RTPReceiver(int localPort) throws IOException {
        try {
        	System.out.println("Local port: "+localPort);
            receivedDataSource=null;
            //creazione di una sessione RTP:
            //1. creazione di un RTP manager
            rtpManager=RTPManager.newInstance();
            //2. aggiunta di un ReceiveStreamListener per la ricezione dei dati
            rtpManager.addReceiveStreamListener(this);
            //3. creazione di un endpoint locale
            SessionAddress localAddr=new SessionAddress(InetAddress.getLocalHost(),localPort);
            //4. inizializzazione del manager
            rtpManager.initialize(localAddr);
        }
        catch (IOException e) { throw new IOException("RTP initializing failed:\n"+e); }
        catch (InvalidSessionAddressException e) { throw new IOException("Invalid Local Address:\n"+e); }
    }

    
    /**
     * Specifica l'endpoint del trasmittente dei pacchetti RTP.
     * @param host l'indirizzo del mittente
     * @param port la porta da cui il mittente trasmette
     * @throws IOException se l'indirizzo non è valido o non è possibile aggiungere tale host alla sessione RTP
     */
    public void setSender(InetAddress host, int port) throws IOException {
        try {
            dataSync=new Object();
            //specifica l'endpoint remoto della sessione
            SessionAddress serverAddr=new SessionAddress(host,port);
            //apre la connessione
            rtpManager.addTarget(serverAddr);
        }
        catch (UnknownHostException e) { throw new IOException("Destination Host Unknown:\n"+e); }
        catch (InvalidSessionAddressException e) { throw new IOException("Adding target failed:\n"+e); }
    }
    
    
    /**
     * <p>La chiamata a questo metodo blocca il ricevitore finchè non viene ricevuto un flusso RTP.</p>
     * <p>All'arrivo dei dati, viene restituita un'istanza di DataSource per potervi accedere.</p>
     * @return un'istanza di DataSource da cui leggere i dati ricevuti via RTP
     */
    public DataSource receiveData() {
        if (receivedDataSource==null) {
            System.out.println("Waiting RTP Data...");
            try {
        	    synchronized (dataSync) {
            		while (receivedDataSource==null) dataSync.wait();
        	    }
            }
            catch (InterruptedException e) { return null; } // non si verifica
        }
        return receivedDataSource;
    }

    
    /**
     * Chiude il ricevitore, che in seguito a questa chiamata non potrà più essere utilizzato.
     */
    public void close() {
        rtpManager.removeTargets("Receiver closed");
        //prepara il manager per il garbage collector
        rtpManager.dispose();
    }

    
    public void update (ReceiveStreamEvent event) {
        if (event instanceof NewReceiveStreamEvent) {
            System.out.println("New Stream Received****");
            ReceiveStream rs=((NewReceiveStreamEvent)event).getReceiveStream();
            receivedDataSource=rs.getDataSource();
            
            /*****************************************/
            BufferControl bufCtl = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");

		if (bufCtl != null){
		    System.err.println("Threshold: " + bufCtl.getMinimumThreshold()+" lenght: "+bufCtl.getBufferLength());
		} else
		    System.err.println("Non ho informazioni sul buffer");
            /*****************************************/
            
		    synchronized (dataSync) { dataSync.notify(); }
    		return;
        }
        
        if (event instanceof ActiveReceiveStreamEvent) {
            System.out.println("Active Stream");
            return;
        }
        
        if (event instanceof RemotePayloadChangeEvent) {
            System.out.println("Remote Payload Change");
            return;
        }
        
        if (event instanceof InactiveReceiveStreamEvent) {
            //TODO: nel caso la trasmissione sia sospesa e poi ripresa, viene prima generato un evento di classe
            //NewReceiveStreamEvent e poi uno di classe InactiveReceiveStreamEvent.
            //Verificare il motivo di questa situazione ed inserire il codice appropriato per gestirla.
            System.out.println("Inactive Stream");
            rtpManager.removeTargets("Session ended");
            rtpManager.dispose();
            System.exit(0);
        }

        if (event instanceof ByeEvent) {
            System.out.println("\nBye Message\n");
            rtpManager.removeTargets("Session ended");
            rtpManager.dispose();
            //System.exit(0);
        }
    }
}
