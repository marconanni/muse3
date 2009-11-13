package muse.server;
/**
 * ConnectionManager.java
 * Contiene metodi per la gestione della connessione UDP
 * @author Ambra Montecchia
 * @version 1.0
 * */
import java.net.*;
import util.Logger;

public class ConnectionManager {

	//datagram socket
	private DatagramSocket sock;
	//indirizzo IP client
	private InetAddress clientIP;
	//porta client
	private int clientPort;
	//porta proxy
	private int localPort;
	//datagramma
	private DatagramPacket packet;
	//buffer in cui immagazzinare i dati del datagramma
	private byte[] buffer;
	//dimensioni del buffer
	private int bufLen = 1472;
	//griglia che rappresenta l'occupazione delle porte
	private boolean[] grid;
	
	/**
	 * Costruttore
	 * */
	public ConnectionManager(){
		sock = null;
		clientIP = null;
		clientPort = 0;
		buffer = new byte[bufLen];
		localPort = 0;
		grid = new boolean[64512];
		//inizializzazione della griglia: tutte le porte sono libere
		for(int i = 0;i < 64512;i++){
			grid[i] = true;
		}
	}
	
	/**
	 * Metodo per l'apertura di una socket UDP
	 * @param port - porta a cui � collegata la socket
	 * @return un intero che rappresenta l'esito dell'operazione (0 operazione eseguita correttamente)
	 * */
	public int openSocket(int port){	
		try{
			localPort = port;
			sock = new DatagramSocket(localPort);
		}
		catch(Exception ex){
			Logger.write("Errore apertura socket: "+ex.getMessage());
			return -1;
		}
		return 0;
	}
	
	/**
	 * Metodo per la ricezione di un datagramma
	 * @return un intero che rappresenta l'esito dell'operazione (0 operazione eseguita correttamente)
	 * */
	public int receivePacket(){
		try{
			packet = new DatagramPacket(buffer, bufLen);
			sock.setSoTimeout(0);
			sock.receive(packet);
			clientIP = packet.getAddress();
			clientPort = packet.getPort();
		}
		catch(Exception ex){
			Logger.write("Errore ricezione pacchetto: "+ex.getMessage());
			return -1;
		}
		return 0;
	}
	
	/**
	 * Metodo per la ricezione di un datagramma con timeout
	 * @param to - intero che rappresenta la durata del timeout
	 * @return un intero che rappresenta l'esito dell'operazione (0 operazione eseguita correttamente)
	 * */
	public int receiveTimeoutPacket(int to){
		try{
			packet = new DatagramPacket(buffer, bufLen);
			sock.setSoTimeout(to);
			sock.receive(packet);
			clientIP = packet.getAddress();
			clientPort = packet.getPort();
			sock.setSoTimeout(0);
		}
		catch(Exception ex){
			Logger.write("Errore ricezione pacchetto: "+ex.getMessage());
			return -1;
		}
		return 0;
	}
	
	/**
	 * Metodo per la scansione e l'occupazione delle porte
	 * */
	public synchronized int portScan(){
		int port = 0;
		//ciclo per individuare la prima porta disponibile
		for(int i = 0;i < 2000;i++){
			if((grid[i]) && (grid[i+1])){
				//per la ricezione sono necessarie due porte libere consecutive
				grid[i] = false;
				grid[i + 1] = false;
				port = i + 1124;
				break;
			}
		}
		return port;
	}
	/**
	 * Metodo per l'invio di un datagramma
	 * @param p - il datagramma che deve essere inviato
	 * @return un intero che rappresenta l'esito dell'operazione (0 operazione eseguita correttamente)
	 * */
	public int sendPacket(DatagramPacket p){
		
		packet = p;
		try{
			sock.send(packet);
		}
		catch(Exception ex){
			System.err.println("Errore invio pacchetto: "+ex.getMessage());
			return -1;
		}
		return 0;
		
	}
	
	/**
	 * Metodo getter
	 * @return la porta dell'host remoto 
	 * */
	public int getPort(){
		return clientPort;
	}
	
	/**
	 * Metodo getter
	 * @return l'indirizzo IP dell'host remoto 
	 * */
	public InetAddress getAddress(){
		return clientIP;
	}
	
	/**
	 * Metodo getter
	 * @return il datagramma
	 * */
	public DatagramPacket getPacket(){
		return packet;
	}
	
	/**
	 * Metodo getter
	 * @return la porta del proxy
	 * */
	public int getLocalPort(){
		return localPort;
	}
}
