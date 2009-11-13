package muse.client.connection;
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
	//porta client
	private int localPort;
	//datagramma
	private DatagramPacket packet;
	//buffer in cui immagazzinare i dati del datagramma
	private byte[] buffer;
	//dimensioni del buffer
	private int bufLen = 1472;
	
	/**
	 * Costruttore
	 * */
	public ConnectionManager(){
		sock = null;
		buffer = new byte[bufLen];
		packet = new DatagramPacket(buffer, bufLen);
		localPort = 0;
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
			sock.setSoTimeout(0);//timeout infinito
			sock.receive(packet);
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
			sock.setSoTimeout(to);
			sock.receive(packet);
		}
		catch(Exception ex){
			Logger.write("Errore ricezione pacchetto: "+ex.getMessage());
			return -1;
		}
		return 0;
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
			Logger.write("Errore invio pacchetto: "+ex.getMessage());
			return -1;
		}
		return 0;
		
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
	 * @return la porta del client
	 * */
	public int getLocalPort(){
		return localPort;
	}
	
	/**
	 * Metodo getter
	 * @return la porta dell'host remoto
	 * */
	public int getRemotePort(){
		int remotePort = packet.getPort();
		return remotePort;
	}
	
	/**
	 * Metodo che consente di eseguire una scansione delle porte, per individuare una porta libera
	 * */
	public void scannerPort(boolean grid[]){
		int dimension = grid.length;
		System.out.println("Porte da scansionare: "+dimension);
		int portNumber = 0;
		for(int i = 0;i < dimension;i++){			
			portNumber = i + 1024;
			
			try{
				DatagramSocket ds = new DatagramSocket(portNumber);
				ds.close();
				grid[i] = true;
				System.out.println("Porta "+portNumber+" libera");
			}
			catch(BindException bex){
				grid[i] = false;
				System.out.println("Porta "+portNumber+" occupata");
			}
			catch(SocketException soex){
			}
		}
	}
	
	/**
	 * Metodo che restituisce il numero di bytes del pacchetto ricevuto
	 * */
	public int getBytes(){
		int bytes = 0;
		bytes = packet.getLength();
		return bytes;
	}
}
