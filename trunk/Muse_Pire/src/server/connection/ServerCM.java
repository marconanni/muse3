package server.connection;

import java.net.InetAddress;
import java.util.Observer;

import parameters.Parameters;

import debug.DebugConsole;

/**
 * @author Luca Campeti
 *
 */
public class ServerCM  {
	
	AConnectionReceiver crs = null;
	Thread serverCMThread = null;
	DebugConsole console;
	
	public ServerCM(String localAddress,int receivingPort, Observer observer, DebugConsole console){
		crs = new AConnectionReceiver(observer,localAddress, receivingPort);
		serverCMThread = new Thread(crs);
		this.console=console;
	}
	
	public void start(){
		if(serverCMThread!=null)serverCMThread.start();
		console.debugMessage(Parameters.DEBUG_INFO,"ServerCM partito");
	}
}