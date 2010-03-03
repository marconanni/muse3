package test.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;

import client.connection.ClientCM;
import client.connection.ClientConnectionFactory;
import client.connection.ClientPortMapper;
import test.TestObserver;

public class TestClientConnectionFactory {
	
	public static byte[] writeByteArray(int toSend){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(toSend);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

		public static void main(String args[]){
			TestObserver to = new TestObserver();
			ClientPortMapper cpm = ClientPortMapper.getInstance();
			//ClientCM cscm = ClientConnectionFactory.getSessionConnectionManager(to, true);
			ClientCM cecm = ClientConnectionFactory.getElectionConnectionManager(to,true);
			ClientCM crssicm = ClientConnectionFactory.getRSSIConnectionManager(to,true);
			//cscm.start();
			cecm.start();
			crssicm.start();

			DatagramPacket dp = null;
			byte[] buffer = null;
			buffer = writeByteArray(cpm.getPortSessionIn());
			//ClientElectionManager invia un messaggio al ClientSessionManager
			dp = new DatagramPacket(buffer,buffer.length,cpm.getLocalAddress(),cpm.getPortSessionIn());
			cecm.sendTo(dp);

			//ClientSessionManager invia un messaggio al ClientRSSIManager
			buffer = writeByteArray(cpm.getPortRSSIIn());
			dp = new DatagramPacket(buffer,buffer.length,cpm.getLocalAddress(),cpm.getPortRSSIIn());
			//cecm.sendTo(dp);
			crssicm.sendTo(dp);
			
//			//ClientRSSIManager invia un messaggio al ClientSessionManager
			buffer = writeByteArray(cpm.getPortSessionIn());
			dp = new DatagramPacket(buffer,buffer.length,cpm.getLocalAddress(),cpm.getPortSessionIn());
			crssicm.sendTo(dp);
			
			//ClientSessionManager invia un messaggio al ClientElectionManager
			buffer = writeByteArray(cpm.getPortElectionIn());
			dp = new DatagramPacket(buffer,buffer.length,cpm.getLocalAddress(),cpm.getPortElectionIn());
			cecm.sendTo(dp);
			
		}
}
