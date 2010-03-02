package client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import unibo.core.EventCircularBuffer;

public class Valerio extends Thread{
	PrintWriter pw;
	EventCircularBuffer ev;
	boolean ended=false;
	public synchronized boolean isEnded() {
		return ended;
	}
	public synchronized void setEnded(boolean ended) {
		this.ended = ended;
	}
	public Valerio(String fileName, EventCircularBuffer ev) {
		super();
		try {
			pw=new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.println("tempo,percentuale");
	}
	@Override
	public void run() {
		long k=0;
		// TODO Auto-generated method stub
		while(k<90000){//monitoro buffer per un minuto e mezzo
			pw.println(k+","+ev.getBufferPercentage());
			try {
				sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			k=k+100;
		}
		pw.close();
	}
	
}

