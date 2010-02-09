package server.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

/**Classe che rappresenta un oggetto in grado di spedire un messaggio verso la rete Managed e 
 * porsi, subito dopo, in ricezione dei messaggi provenienti dalla rete Managed
 * @author Luca Campeti
 *
 */
public class StreamingServerReceiver extends Observable implements Runnable{

	private DatagramSocket inOutSocket = null;
	private DatagramPacket dataIn = null;
	private byte[] buffer = null;

	private Object sync = new Object();
	private boolean stopped = false;//modificato da pire per far andare la baracca
	protected String managerName = "StreamingServerReceiver";

	private InetAddress localAddress = null;
	private int localInOutPort = -1;


	/**Metodo per ottenere uno StreamingServerReceiver
	 * @param localAddress una String che rappresenta l'indirizzo locale del nodo sulla rete Managed
	 * @param localInOutPort un int che rappresenta la porta attraverso cui inviare/ricevere
	 * @param observer Observer che deve essere avvertito alla ricezione di un messaggio
	 */
	public StreamingServerReceiver(InetAddress localAddress, int localInOutPort, Observer observer){
		if(localAddress == null) throw new IllegalArgumentException(managerName+" : indirizzo passato al costruttore a null");
		this.localAddress = localAddress;
		this.localInOutPort = localInOutPort;
		try {
			System.out.println("Streaming server inoutsocket su "+localAddress.toString()+":"+localInOutPort);
			inOutSocket = new DatagramSocket(localInOutPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		addObserver(observer);
	}


	public void run(){
		buffer = new byte[256];
		System.out.println(managerName+" run()");
		while(true){
			if(stopped){
				synchronized (sync) {
					try {
						//System.out.println(managerName+" : ricezione non ancora attiva");
						sync.wait();
						//System.out.println(managerName+" : ricezione abilitata");
						if(dataIn != null) {
							setChanged();
							notifyObservers(dataIn);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			System.out.println(managerName+": attesa messaggio da parte di: "+ localAddress + " porta: " + localInOutPort);
			dataIn = new DatagramPacket(buffer, buffer.length);

			try {
				inOutSocket.receive(dataIn);
				System.out.println(managerName+": ricevuto messaggio da : " +dataIn.getAddress().getHostAddress() + " porta: " +dataIn.getPort());
				/*
				if(dataIn.getAddress().getHostAddress().equals(localAddress)){
					System.out.println(managerName+" IGNORATO");
					continue;
				}
				*/
			}catch (IOException e) {
				System.out.println(managerName+" eccezione:" + e.getMessage());
				
				break;
			}

			if(stopped) continue;

			setChanged();
			notifyObservers(dataIn);
			dataIn=null;
		}
	}

	
	/**Metodo per inviare un DatagramPacket ad un destinatario sulla rete Managed 
	 * @param dp il DatagramPacket da spedire ad un destinatario sulla rete Managed
	 */
	public void sendTo(DatagramPacket dp){
		if(stopped){
			try {
				inOutSocket.send(dp);
				System.out.println(managerName+ ".sendTo(): inviato messaggio a: " +dp.getAddress().getHostAddress() +" porta: "+ dp.getPort());
				//System.out.println(managerName+ ".sendTo(): inviato messaggio da: " + localAddress +" porta: "+localInOutPort);
			} catch (IOException e) {
				e.printStackTrace();
			}
			stopped = false;
			synchronized (sync) {
				sync.notifyAll();
			}
		}
	}
	
	
	/**Metodo per chiudere lo StreamingServerReceiver
	 */
	public void close(){
		if(inOutSocket != null) inOutSocket.close();
	}
	
	/**
	 * @return the localInOutPort
	 */
	public int getLocalInOutPort() {
		return localInOutPort;
	}

}