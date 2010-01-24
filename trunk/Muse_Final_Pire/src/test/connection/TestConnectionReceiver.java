package test.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;

import client.connection.AConnectionReceiver;

import test.TestObserver;

class TestConnectionReceiver{

	public static void main (String args[]){

		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		TestObserver observer = new TestObserver();
		AConnectionReceiver cr  = null;

		cr = new AConnectionReceiver(observer, localhost,6000);

		Thread t = new Thread(cr);
		t.start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		byte[] buffer = {1,2,3,4};
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,localhost,6000);
		DatagramSocket ds = null;

		try {
			ds = new DatagramSocket(5000, localhost);
			ds.send(dp);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cr.close();
	}
}
