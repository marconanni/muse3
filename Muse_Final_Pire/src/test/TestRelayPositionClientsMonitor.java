package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import parameters.PortConfiguration;

import relay.messages.RelayMessageFactory;
import relay.position.RelayPositionMonitor;

class TestRelayPositionMonitor{

	public static void main(String args[]) throws UnknownHostException{
		TestObserver to = new TestObserver();
		RelayPositionMonitor rpcm = new RelayPositionMonitor(50,1000,to);
		rpcm.start();
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			DatagramSocket ds = new DatagramSocket(7890);
			DatagramPacket dp = RelayMessageFactory.buildNotifyRSSI(0, 120, localhost, PortConfiguration.RSSI_PORT_IN);
			Thread.sleep(4000);
			ds.send(dp);
			ds.close();

			Thread.sleep(12000);
			rpcm.close();

		} catch (IOException e) {e.printStackTrace();}
		catch (InterruptedException e) {e.printStackTrace();}
	}
}