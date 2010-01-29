package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Observable;
import java.util.Observer;

import relay.battery.RelayBatteryMonitor;

public class TestObserver implements Observer {
	
	//private RelaySessionCM rscm = null;
	//private ProxyCM pcm = null;
	//private RelayPortMapper rpm = RelayPortMapper.getInstance();
	
	public TestObserver(){
		System.out.println("testObserver: creato");
	}
	
//	public void setRelaySessionCM(RelaySessionCM rs, ProxyCM pc){
//		rscm =rs;
//		pcm = pc;
//	}
	public static String convertToString(byte[] content){
		ByteArrayInputStream bais = new ByteArrayInputStream(content);
		DataInputStream dis = new DataInputStream(bais);
		int res = -1; 
		try {
			res = dis.readInt();
		} catch (IOException e) {e.printStackTrace();}
		return ""+res;
	}
	
	public static byte[] writeByteArray(int toSend){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeInt(toSend);
		} catch (IOException e) {e.printStackTrace();}
		return baos.toByteArray();
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		//System.out.println("update test observer");
		
		if(arg1 instanceof DatagramPacket){
			DatagramPacket dp  = (DatagramPacket)arg1;
			System.out.println("\tObserver: ricevuto pacchetto da: " + dp.getAddress().getHostAddress()+ " porta: " + dp.getPort());
			String res = TestObserver.convertToString(dp.getData());
			System.out.println("\tObserver: dati ricevuti: " +  res);
			
			if(res.equalsIgnoreCase("3026")){
				//byte[] buffer = writeByteArray(123);
				//dp = new DatagramPacket(buffer,buffer.length,rpm.getLocalManagedHostAddress(),pcm.getLocalManagedInputOutputPort() );   
				//rscm.sendTo(dp);
			}
		}
		else if(arg1 instanceof String){
			System.out.println("RICEVUTO MESSAGGIO: " + (String)arg1);
//			if(arg0 instanceof RelayBatteryMonitor)
//				((RelayBatteryMonitor)arg0).close();
		}
	}
}