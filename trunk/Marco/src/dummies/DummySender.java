package dummies;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import parameters.MessageCodeConfiguration;



public class DummySender extends Thread {
	
	public boolean paused;
	public String localAddress;
	public int localPort;
	public String destinationAddress;
	public int destinationPort;
	public DummyBuffer buffer;
	public DatagramSocket socket;
	
	
	
	
	
	
	
	public DummySender(boolean paused, String localAddress, int localPort,
			String destinationAddress, int destinationPort,
			DummyBuffer buffer) {
		super();
		this.paused = paused;
		this.localAddress = localAddress;
		this.localPort = localPort;
		this.destinationAddress = destinationAddress;
		this.destinationPort = destinationPort;
		this.buffer = buffer;
		try {
			socket = new DatagramSocket(localPort,InetAddress.getByName(localAddress));
		} catch (SocketException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("senderCreato");
		this.start();
	}



	
	
	
	public synchronized boolean isPaused(){
		
		
		return paused;
		
	}
	
	/**
	 * fa fermare l'erogazione del flusso
	 */
	public synchronized void stopSending(){
		
		
		paused=true;
		
	}
	
	/**
	 * fa partire l'erogazione del flusso.
	 */
	
	public synchronized void startSending(){
		
		paused=false;
		
		
	}

	
	
	/**
	 * il vero cuore della classe, il thread esegue costantemente questo metodo
	 * (se il buffer è vuoto, oppure lui è stoppato resta in una fase di 
	 * attesa attiva); il thread esegue questo metodo ogni decimo di secondo
	 */
	
	public void run() {
		System.out.println("sender partito");
		
		while(true){
		
			if(!paused){
				
				
				if(!buffer.isEmpty()){
					buffer.get();
					mandaMessaggio();
					
				}
			}
			try {
				this.sleep(100);
			} catch (InterruptedException e) {
				//  Auto-generated catch block
				e.printStackTrace();
			}
		
		}
	}
	
	
	public void mandaMessaggio(){
		
		try{
		
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content ="a";
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		DatagramPacket dp = new DatagramPacket(data, data.length,InetAddress.getByName(destinationAddress),destinationPort);
		socket.send(dp);
		
		
		
		
		
		
		}
		
		catch (Exception e){
			e.printStackTrace();
		}
	
	}

	
	

}
