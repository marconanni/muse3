package test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class TestRelayConnectionFactory{
	
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
		/*RelayPortMapper rpm = RelayPortMapper.getInstance();
		TestObserver to = new TestObserver();
		ProxyCM pcm = RelayConnectionFactory.getProxyConnectionManager(to, false);
		RelaySessionCM rscm = RelayConnectionFactory.getSessionConnectionManager(to);
		RelayElectionCM recm = RelayConnectionFactory.getElectionConnectionManager(to);
		to.setRelaySessionCM(rscm, pcm);
		
		System.out.println("ProxyCM: porta uscita/ingresso verso il server: " +pcm.getLocalManagedInputOutputPort());
		System.out.println("ProxyCM: porta ingresso dal client: " +pcm.getLocalAdHocInputPort());
		pcm.start();
		byte[] buffer = null;
		buffer = writeByteArray(pcm.getLocalManagedInputOutputPort());
		DatagramPacket dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalManagedHostAddress(),rpm.getPortInAdHocSession() );   
		rscm.start();
		
		
		//ATTENZIONE QUESTO E' L'ODINE IMPOSTO SULL'USO DEL ProxyCM quando il secondo parametro di getProxyConnectionManager
		//è a false, ovvero quando il Proxy viene creato da 0 piuttosto che accogliere già una sessione in corso 
		//(caso di creazione dei proxy sul new relay al cambio della sessione)
		//invio forward al server
		//attesa della risposta dello streamingsever appena creato 
		//invio dell'ack al client
		//comincia attesa di messaggi START_TX STOP_TX da parte del Proxy
		pcm.sendToServer(dp);
		
		//questo non lo manda perchè ancora non riceve la risposta del server
		buffer = writeByteArray(666);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),rpm.getPortInAdHocSession());   
		pcm.sendTo(dp);
		
		pcm.waitStreamingServerResponse();
		pcm.sendTo(dp);
		pcm.start();
		
		buffer = writeByteArray(345);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),pcm.getLocalAdHocInputPort() );   
		rscm.sendTo(dp);
		
		ProxyCM pcmForChange = RelayConnectionFactory.getProxyConnectionManager(to, true);
		pcmForChange.start();
		
		int portInPCMForChange = pcm.getLocalAdHocInputPort();
		int portManagedPCMForChange = pcm.getLocalManagedInputOutputPort();
		
		buffer = writeByteArray(678);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portInPCMForChange);   
		rscm.sendTo(dp);
		
		//Non dovrebbe bloccarsi in ricezione
		pcmForChange.waitStreamingServerResponse();
		
		//Questo non lo dovrebbe ricevere perchè lo invio sulla porta da cui riceve un singolo messaggio dal server
		buffer = writeByteArray(910);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portManagedPCMForChange);   
		rscm.sendTo(dp);
		
		//Questo lo dovrebbe ricevere perchè lo invio sulla porta da cui riceve start e stop dal client 
		buffer = writeByteArray(1112);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),portInPCMForChange);   
		rscm.sendTo(dp);
		
		buffer = writeByteArray(666);
		dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalAdHocHostAddress(),rpm.getPortInAdHocSession());   
		pcm.sendTo(dp);*/
		
		
	}
	
	
}