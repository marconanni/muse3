package server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import parameters.Parameters;

public class TestMainServer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = 0+"_"+Parameters.FORWARD_REQ_FILE+"_"+"192.168.2.1_pippo.mp3_7000_5001";
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		DatagramPacket dp=new DatagramPacket(data, data.length, InetAddress.getByName(Parameters.SERVER_ADDRESS), Parameters.SERVER_SESSION_PORT_IN);

		DatagramSocket ds = null;

		try {
			ds = new DatagramSocket(5000);
			ds.send(dp);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Messaggio mandato");
		ds.close();


	}

}
