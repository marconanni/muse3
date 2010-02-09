package unibo.core.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.event.InactiveSendStreamEvent;
import javax.media.rtp.event.NewSendStreamEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.StreamClosedEvent;

/**
 * <p>Questa classe è un semplice trasmettitore di traffico RTP basato su un'istanza di una classe che implementa
 * l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
 * semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano trasmettere
 * dati RTP in modalità unicast dal nodo locale.</p>
 * <p>Inoltre, è implementata l'interfaccia SendStreamEvent, così che questa classe possa catturare gli eventi
 * associati alla trasmissione del flusso RTP.</p> 
 * @author Alessandro Falchi
 */
public class RTPSender implements SendStreamListener {
    private RTPManager rtpManager;
    
    /**
     * Costruttore. L'indirizzo del trasmettitore è quello del host locale.
     * @param localPort la porta locale da cui trasmettere il traffico RTP
     * @throws IOException se l'inizializzazione fallisce
     */
    public RTPSender(int localPort) throws IOException {
        try { 
            rtpManager=RTPManager.newInstance();
            rtpManager.addSendStreamListener(this);
            SessionAddress localAddr=new SessionAddress(InetAddress.getLocalHost(),localPort);
            rtpManager.initialize(localAddr);
            
        }
        catch (IOException e) { throw new IOException("RTP initializing failed:\n"+e); }
        catch (InvalidSessionAddressException e) { throw new IOException("Invalid Local Address:\n"+e); }
    }
    
    /**
     * Specifica l'endpoint del partecipante alla sessione destinatario dei pacchetti RTP.
     * @param host l'indirizzo del ricevente
     * @param port la porta su cui ascolta il ricevente
     * @throws IOException se l'indirizzo non è valido o non è possibile aggiungere tale host alla sessione RTP
     */
    public void addDestination(InetAddress host, int port) throws IOException {
        try {
            SessionAddress destAddr=new SessionAddress(host,port);
            rtpManager.addTarget(destAddr);
            
        }
        catch (UnknownHostException e) { throw new IOException("Destination Host Unknown:\n"+e); }
        catch (InvalidSessionAddressException e) { throw new IOException("Adding target failed:\n"+e); }
    }

    /**
     * Fa partire l'invio dei dati.
     * @param source il DataSource che riferisce il contenuto multimediale che si vuole trasmettere
     * @throws IOException se il DataSource fornisce dati in un formato non compatibile con la trasmissione RTP 
     */
    public void sendData(DataSource source) throws IOException {
        try {
            SendStream sendStream=rtpManager.createSendStream(source,0);
            sendStream.start();
            
        }
        catch (UnsupportedFormatException e) { throw new IOException("DataSource format non supported by RTP: "+e); }
    }

    /**
     * Chiude il trasmettitore, che in seguito a questa chiamata non potrà più essere utilizzato.
     */
    public void close() {
        rtpManager.removeTargets("Sender closed");
        rtpManager.dispose();
    }

    
    public void update(SendStreamEvent event) {
        if (event instanceof NewSendStreamEvent) {
            System.out.println("Send Stream Ready");
            return;
        }
        
        if (event instanceof InactiveSendStreamEvent) {
            System.out.println("Inactive Send Stream");
            rtpManager.removeTargets("Stream ended");
            rtpManager.dispose();
            //System.exit(0);
        }
        
        if (event instanceof StreamClosedEvent) {
            System.out.println("Stream Closed");
            rtpManager.removeTargets("Stream ended");
            rtpManager.dispose();
            //System.exit(0);
        }
    }
    
    public void removeTarget(InetAddress host, int port)throws IOException {
        try {
            SessionAddress destAddr=new SessionAddress(host,port);
            rtpManager.removeTarget(destAddr,"null");
            
        }
        
        catch (InvalidSessionAddressException e) { throw new IOException("Adding target failed:\n"+e); }
    }
    
    
}
