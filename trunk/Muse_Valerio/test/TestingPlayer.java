package test;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.control.BufferControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.*;
import javax.media.rtp.SessionAddress;
import unibo.core.rtp.RTPReceiver;

public class TestingPlayer implements ReceiveStreamListener, SessionListener,ControllerListener {

	boolean dataReceived = false;
    Object dataSync = new Object();
    Frame frame;
    
	public void init()
	{
		try {
			RTPManager mgr;
			mgr = RTPManager.newInstance();
			mgr.addReceiveStreamListener(this);
			mgr.addSessionListener(this);
			
			SessionAddress localAddr = new SessionAddress(InetAddress.getLocalHost(),4500);
			SessionAddress remoteAddr = new SessionAddress(InetAddress.getByName("192.168.2.3"),4500);
			
			if(remoteAddr == null)
			{
				System.err.println("RemoteAddress null");
				System.exit(0);
			}
			
			mgr.initialize(localAddr);
			
			BufferControl bc = (BufferControl)mgr.getControl("javax.media.control.BufferControl");
			if (bc != null)
			    bc.setBufferLength(350);
			mgr.addTarget(remoteAddr);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long then = System.currentTimeMillis();
		long waitingPeriod = 30000;  // wait for a maximum of 30 secs.

		try{
		    synchronized (dataSync) {
			while (!dataReceived && 
				System.currentTimeMillis() - then < waitingPeriod) {
			    if (!dataReceived)
			    	dataSync.wait(1000);
			}
		   }
		} catch (Exception e) {}

		if (!dataReceived) {
		    System.err.println("No RTP data was received.");
		}
	}
    
    public static void main(String[] args) {
        try {
        	
        	TestingPlayer test = new TestingPlayer();
        	test.init();			
        }
        catch (Exception e) { System.err.println(e); e.printStackTrace(); }
    }

	public void update(ReceiveStreamEvent evt) {
		
		RTPManager mgr = (RTPManager)evt.getSource();
		Participant participant = evt.getParticipant();	// could be null.
		ReceiveStream stream = evt.getReceiveStream();  // could be null.

		if (evt instanceof RemotePayloadChangeEvent) {
	     
		    System.err.println("  - Received an RTP PayloadChangeEvent.");
		    System.err.println("Sorry, cannot handle payload change.");
		    System.exit(0);
		}
	    
		else if (evt instanceof NewReceiveStreamEvent) {

		    try {
			stream = ((NewReceiveStreamEvent)evt).getReceiveStream();
			DataSource ds = stream.getDataSource();

			// Find out the formats.
			RTPControl ctl = (RTPControl)ds.getControl("javax.media.rtp.RTPControl");
			if (ctl != null){
			    System.err.println("  - Recevied new RTP stream: " + ctl.getFormat());
			} else
			    System.err.println("  - Recevied new RTP stream");

			if (participant == null)
			    System.err.println("      The sender of this stream had yet to be identified.");
			else {
			    System.err.println("      The stream comes from: " + participant.getCNAME()); 
			}

			// create a player by passing datasource to the Media Manager
			Player p = javax.media.Manager.createPlayer(ds);
			if (p == null)
			    return;

			p.addControllerListener(this);
			p.realize();

			// Notify intialize() that a new stream had arrived.
			synchronized (dataSync) {
			    dataReceived = true;
			    dataSync.notifyAll();
			}

		    } catch (Exception e) {
			System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
			return;
		    }
	        
		}
		
	}

	public void update(SessionEvent evt) {
		if (evt instanceof NewParticipantEvent) {
		    Participant p = ((NewParticipantEvent)evt).getParticipant();
		    System.err.println("  - A new participant had just joined: " + p.getCNAME());
		}
	}

	public void controllerUpdate(ControllerEvent ce) {
		
		Player p = (Player)ce.getSourceController();

		if (p == null)
		    return;

		// Get this when the internal players are realized.
		if (ce instanceof RealizeCompleteEvent) {
			frame = new Frame("Now Playing...");
			if(p.getVisualComponent()!=null)
			{
				frame.add(p.getVisualComponent());
				frame.setSize(230, 150);
			}
			else frame.setSize(230, 50);
			frame.add(p.getControlPanelComponent());
			frame.setVisible(true);
			p.start();
		}
		
	}
}
