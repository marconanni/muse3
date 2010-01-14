package server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import parameters.Parameters;

public class ServerMessageFactory {

	
	/**
	 * Messaggio AckRelayForw
	 * @param sequenceNumber
	 * @param addr indirizzo verso il quale inviare il messaggio di ack
	 * @param port porta di ricezione del megasggio di ack
	 * @return
	 * @throws IOException
	 */
	static protected DatagramPacket buildAckRelayForw(int sequenceNumber, int trasmissionPort, int trasmissionCtrlPort, InetAddress addr, int port) throws IOException {

		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		String content = sequenceNumber+"_"+Parameters.ACK_RELAY_FORW+"_"+trasmissionPort+"_"+trasmissionCtrlPort;
		doStream.writeUTF(content);
		doStream.flush();
		byte[] data = boStream.toByteArray();

		return new DatagramPacket(data, data.length, addr, port);

	}
	
}
