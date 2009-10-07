package relay.positioning;

import java.util.Observable;
import java.util.Observer;

public class TestObserver implements Observer{
	public TestObserver(){
		System.out.println("testObserver: creato");
	}

	public static String convertToString(byte[] content){
		String res = "";
		//for(int i = 0;i<1;i++)res = res + content[i] +", ";
		res = res + content[0];
		return res;
	}

	@Override
	public void update(Observable o, Object arg) {
		String dp  = (String)arg;
		System.out.println("\tObserver: ricevuta notifica: " + dp);
		//System.out.println("\tObserver: notifica ricevuta da: " + ((RelayPositionClientsMonitor)o).toString());
		System.out.println("\tObserver: notifica ricevuta da: " + ((RelayPositionAPMonitor)o).toString());
	}

}
