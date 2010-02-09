package server.connection;

import java.net.InetAddress;
import java.util.Observer;


/**
 * @author Luca Campeti
 *
 */
public class ServerCM  {
	
	AConnectionReceiver crs = null;
	Thread serverCMThread = null;
	
	public ServerCM(String localAddress,int receivingPort, Observer observer){
		crs = new AConnectionReceiver(observer,localAddress, receivingPort);
		serverCMThread = new Thread(crs);
	}
	
	public void start(){
		if(serverCMThread!=null)serverCMThread.start();
		System.out.println("ServerCM partito");
	}
}