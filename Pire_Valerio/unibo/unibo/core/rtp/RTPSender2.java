package unibo.core.rtp;
//
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.GlobalTransmissionStats;
import javax.media.rtp.event.InactiveSendStreamEvent;
import javax.media.rtp.event.NewSendStreamEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.StreamClosedEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.rtcp.*;

/**
 * <p>Questa classe è un semplice trasmettitore di traffico RTP basato su un'istanza di una classe che implementa
 * l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
 * semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano trasmettere
 * dati RTP in modalità unicast dal nodo locale.</p>
 * <p>Inoltre, è implementata l'interfaccia SendStreamEvent, così che questa classe possa catturare gli eventi
 * associati alla trasmissione del flusso RTP.</p> 
 * @author Alessandro Falchi
 */
public class RTPSender2 implements SendStreamListener, RemoteListener {
    private RTPManager rtpManager;
    private int senderReport=0;
    private int receiverReport=0;
    
    /**
     * Costruttore. L'indirizzo del trasmettitore è quello del host locale.
     * @param localPort la porta locale da cui trasmettere il traffico RTP
     * @throws IOException se l'inizializzazione fallisce
     */
    public RTPSender2(int localPort) throws IOException {
        try { 
            rtpManager=RTPManager.newInstance();
            rtpManager.addSendStreamListener(this);
            rtpManager.addRemoteListener(this);
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
            System.out.println("Stream Closed***");
            rtpManager.removeTargets("Stream ended");
            rtpManager.dispose();
            //System.exit(0);
        }
    }
/***********************************************************************/
    public void update(RemoteEvent event) {
		if (event instanceof ReceiverReportEvent) {
			receiverReport++;
			System.out.println("Received ReceiverReportEvent ["+receiverReport+"]");
			//Vector feedback=((ReceiverReportEvent)event).getReport().getFeedbackReports();
			//Feedback[] feedback=(Feedback[])((ReceiverReportEvent)event).getReport().getFeedbackReports().toArray();
			Object[] feedback=((ReceiverReportEvent)event).getReport().getFeedbackReports().toArray();
			for(int i=0; i<feedback.length; i++){
				System.out.println("SSRC: "+((Feedback)feedback[i]).getSSRC());
				System.out.println("extended highest sequence number received "+((Feedback)feedback[i]).getXtndSeqNum());
				System.out.println("Send packet lost: "+((Feedback)feedback[i]).getNumLost());
				System.out.println("Send packet lost fraction: "+((Feedback)feedback[i]).getFractionLost());
				System.out.println("Jitter delay: "+((Feedback)feedback[i]).getJitter());
				System.out.println("Last SR: "+((Feedback)feedback[i]).getLSR());
				System.out.println("Delay since last SR: "+((Feedback)feedback[i]).getDLSR());
			}
			GlobalReceptionStats grs=rtpManager.getGlobalReceptionStats();
			GlobalTransmissionStats gts=rtpManager.getGlobalTransmissionStats();
			if(grs!=null) {
				System.out.println("Extract GlobalRecptionStats ["+receiverReport+"]");
				System.out.println("Total number of bytes received on the RTP session socket before any validation of packets "+grs.getBytesRecd());
			}
			if(gts!=null) {
				System.out.println("Extract GlobalTransmissionStats ["+receiverReport+"]");
				System.out.println("Total number of bytes of bytes sent on the RTP session socket "+gts.getBytesSent());
				System.out.println("Total number of RTCP packets sent out by the RTPSM "+gts.getRTCPSent());
				System.out.println("Total number of of RTP packets transmitted on the RTP Session socket "+gts.getRTPSent());
				System.out.println("Total number of packets that failed to get transmitted for any reason "+gts.getTransmitFailed());
			}
		}
		//se il server trasmette solamente non ricever� mai nessun rapporto SR e quindi questo evento non sar� mai generato.
		if (event instanceof SenderReportEvent) {
			senderReport++;
			System.out.println("Received SenderReportEvent ["+senderReport+"]");
			SenderReport rep=((SenderReportEvent)event).getReport();
			System.out.println("Send packet count: "+rep.getSenderPacketCount());
			System.out.println("Send byte count: "+rep.getSenderByteCount());
			System.out.println("Sender timestamp: "+rep.getRTPTimeStamp());
		}
    }
/*************************************************************************/
    public void removeTarget(InetAddress host, int port)throws IOException {
        try {
            SessionAddress destAddr=new SessionAddress(host,port);
            rtpManager.removeTarget(destAddr,"null");
            
        }
        
        catch (InvalidSessionAddressException e) { throw new IOException("Adding target failed:\n"+e); }
    }
    
    
}
