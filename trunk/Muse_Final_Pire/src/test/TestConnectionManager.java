package test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import relay.connection.AConnectionManager;

class TestConnectionManager{

	public static void main(String args[]){
		TestObserver to = new TestObserver();
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getByName("192.168.2.4");
		} catch (UnknownHostException e) {e.printStackTrace();}

		AConnectionManager cm = null;
		cm = new AConnectionManager(localhost,5000,6000,to);
		cm.start();
	

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {e.printStackTrace();}

		byte[] buffer = {0};
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,localhost,5000);
		System.out.println("Send dp: "+ TestObserver.convertToString(dp.getData()));
		cm.sendTo(dp);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {	// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] buffer1 = {100};
		dp = new DatagramPacket(buffer1,buffer1.length,localhost,5000);
		System.out.println("Stop receiving");
		cm.stopReceiving();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Send dp: "+ TestObserver.convertToString(dp.getData()));
		cm.sendTo(dp);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i=3;i<6;i++){
			byte[] bufferL = new byte[1];
			bufferL[0]=(byte)i;
			dp = new DatagramPacket(bufferL,bufferL.length,localhost,5000);
			
			System.out.println("Stop receiving");
			cm.stopReceiving();
			System.out.println("Send: "+ TestObserver.convertToString(dp.getData()));
			cm.sendTo(dp);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Resume receiving");
		cm.resumeReceiving();
		
		byte[] buffer2 = {10};
		dp = new DatagramPacket(buffer2,buffer2.length,localhost,5000);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Send dp: "+ TestObserver.convertToString(dp.getData()));
		cm.sendTo(dp);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Conneciton close");
		cm.close();
	}	
}