package server.connection;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Observer;

/**
 * @author Luca Campeti
 * 
 */
public class StreamingServerCM {
	StreamingServerReceiver ssr = null;
	Thread streamingServerReceiverThread = null;
	
	public StreamingServerCM(InetAddress localAddress,int receivingPort, Observer observer){
		System.out.println("StreamingServerCM in ascolto su "+localAddress+":"+receivingPort);
		ssr = new StreamingServerReceiver(localAddress, receivingPort,observer);
		streamingServerReceiverThread = new Thread(ssr);
	}
	
	public void start(){
		System.out.println("StreamingServerCM start");
		if(streamingServerReceiverThread!=null)streamingServerReceiverThread.start();
	}
	
	public void sendTo(DatagramPacket dp){
		ssr.sendTo(dp);
	}
	
	public int getStreamingServerCtrlPort(){
		return ssr.getLocalInOutPort();
	}
	
}