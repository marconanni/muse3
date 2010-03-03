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
			this.ev=ev;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.println("tempo,percentuale");
		pw.flush();
	}
	@Override
//	public void run() {
//		long k=0;
//		// TODO Auto-generated method stub
//		while(k<90000){//monitoro buffer per un minuto e mezzo
//			pw.println(k+","+ev.getBufferPercentage());
//			pw.flush();
//			try {
//				sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			k=k+100;
//		}
//		pw.close();
//	}
	public void run() {
		
		// TODO Auto-generated method stub
		String stringa="";
		int dato=0;
		for(long k=0;k<70000;k=k+100){//monitoro buffer per 70 secondi
			if(this.ev==null) dato=0;
			else dato=(int)ev.getBufferPercentage();
			
			stringa=stringa+k+","+dato+"\n";
			System.out.println(stringa);
			try {
				sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			k=k+100;
		}
		System.out.println(stringa);
		pw.print(stringa);
		pw.flush();
		pw.close();
	}
}

