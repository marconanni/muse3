package unibo.core.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.media.protocol.DataSource;
import javax.media.control.BufferControl;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.GlobalTransmissionStats;
import javax.media.rtp.event.ActiveReceiveStreamEvent;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.InactiveReceiveStreamEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.rtcp.*;

/**
 * <p>Questa classe è un semplice ricevitore di traffico RTP basato su un'istanza di una classe che implementa
 * l'interfaccia javax.media.rtp.RTPManager. Di fatto, questa classe implementa un wrapper di RTPManager che
 * semplifica le chiamate per l'inizializzazione e la configurazione di RTPManager nel caso si vogliano ricevere
 * dati RTP in modalità unicast sul nodo locale.</p>
 * <p>Inoltre, è implementata l'interfaccia ReceiveStreamListener, così che siano intercettati gli eventi relativi
 * alla ricezione di un flusso RTP.</p> 
 * @author Alessandro Falchi
 */
public class RTPReceiver2 implements ReceiveStreamListener, RemoteListener {
	private RTPManager rtpManager;
	private DataSource receivedDataSource;
	private Object dataSync;
	private int senderReport=0;
	private int receiverReport=0;

/**
* Costruttore. L'indirizzo del ricevitore è quello del host locale.
* @param localPort la porta locale su cui attendere il traffico RTP
* @throws IOException se l'inizializzazione fallisce
*/
	public RTPReceiver2(int localPort) throws IOException {
		try {
			receivedDataSource=null;
			rtpManager=RTPManager.newInstance();
			rtpManager.addReceiveStreamListener(this);
			rtpManager.addRemoteListener(this);
			SessionAddress localAddr=new SessionAddress(InetAddress.getLocalHost(),localPort);
			rtpManager.initialize(localAddr);
/****************************************************************************************/
			BufferControl bc = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");
			if (bc != null){
				 bc.setBufferLength(2150);
				 //bc.setMinimumThreshold(125);
				 System.err.println("Buffer threshold: "+bc.getMinimumThreshold());
				 System.err.println("Buffer lenght: "+bc.getBufferLength());
			}
			else System.err.println("No buffer control");
/****************************************************************************************/
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
            SessionAddress serverAddr=new SessionAddress(host,port); 
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
        rtpManager.dispose();
    }

    
    public void update (ReceiveStreamEvent event) {
        if (event instanceof NewReceiveStreamEvent) {
            System.out.println("New Stream Received");
            ReceiveStream rs=((NewReceiveStreamEvent)event).getReceiveStream();
            receivedDataSource=rs.getDataSource();
/*****************************************************************************************/
BufferControl bufCtl = (BufferControl)rtpManager.getControl("javax.media.control.BufferControl");

		if (bufCtl != null){
		    System.err.println("Threshold: " + bufCtl.getMinimumThreshold()+" lenght: "+bufCtl.getBufferLength());
		} else
		    System.err.println("Non ho informazioni sul buffer");
/*****************************************************************************************/
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
            System.out.println("Bye Message");
            rtpManager.removeTargets("Session ended");
            rtpManager.dispose();
            System.exit(0);
        }
    }

	/***********************************************************************/
    public void update(RemoteEvent event) {

//se il client riceve solamente non ricever� mai nessun rapporto RR e quindi questo evento non sar� mai generato.
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
		}

		if (event instanceof SenderReportEvent) {
			senderReport++;
			System.out.println("Received SenderReportEvent ["+senderReport+"]");
			SenderReport rep=((SenderReportEvent)event).getReport();
			System.out.println("Send packet count: "+rep.getSenderPacketCount());
			System.out.println("Send byte count: "+rep.getSenderByteCount());
			System.out.println("Sender timestamp: "+rep.getRTPTimeStamp());
			
			GlobalReceptionStats grs=rtpManager.getGlobalReceptionStats();
			GlobalTransmissionStats gts=rtpManager.getGlobalTransmissionStats();
			if(grs!=null) {
				System.out.println("Extract GlobalRecptionStats ["+senderReport+"]");
				System.out.println("Total number of bytes received on the RTP session socket before any validation of packets "+grs.getBytesRecd());
			}
			if(gts!=null) {
				System.out.println("Extract GlobalTransmissionStats ["+senderReport+"]");
				System.out.println("Total number of bytes of bytes sent on the RTP session socket "+gts.getBytesSent());
				System.out.println("Total number of RTCP packets sent out by the RTPSM "+gts.getRTCPSent());
				System.out.println("Total number of of RTP packets transmitted on the RTP Session socket "+gts.getRTPSent());
				System.out.println("Total number of packets that failed to get transmitted for any reason "+gts.getTransmitFailed());
			}
		}
    }
/***********************************************************************/
}
