package dummies;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DummyReceiver extends Thread {
	
	public boolean paused;
	public String address;
	public int port;
	public DummyBuffer buffer;
	public DatagramSocket socket;
	
	
	
	public DummyReceiver(boolean paused, String address,
			int port, DummyBuffer buffer) {
		super();
		this.paused = paused;
		this.address = address;
		this.port = port;
		this.buffer = buffer;
		try {
			
			System.out.println("Costruttore: porta" +this.port);
			socket = new DatagramSocket(port,InetAddress.getByName(address));
		} catch (SocketException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("reciever creato!");
		this.start();
	}
	
	
	
	public synchronized boolean isPaused(){
		
		
		return paused;
		
	}
	
	/**
	 * fa fermare la ricezione del flusso
	 */
	public synchronized void stopReceiving(){
		
		
		paused=true;
		
	}
	
	/**
	 * fa partire la ricezione del flusso.
	 */
	
	public synchronized void startReceiving(){
		
		paused=false;
		
		
	}

	
	
	/**
	 * il vero cuore della classe, il thread esegue costantemente questo metodo
	 * , ossia preleva un messaggio dalla socket e lo mette nel buffer.
	 */
	
	public void run() {
		System.out.println("ReceiverPartito!");
		while (true){
			if(!paused){
				riceviMessaggio();
				buffer.put(new Byte("1"));
				
			}
			try {
				this.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
	}



	public void riceviMessaggio() {
		
		byte[] buf = new byte[256];
		
		DatagramPacket dp = new DatagramPacket(buf, buf.length);
		
		
		
		try {
			socket.receive(dp);
			System.out.println("ricevuto pacchetto");
			
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	


}
