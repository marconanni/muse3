package relay.positioning;

import java.io.*;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import parameters.Parameters;

public class TestGreey {
	
	private int numberRSSI = 6;
	private Vector<Double> tmp = null;
	private RSSIFilter filter, filter2 = null;
	private double[] a;
	private String richiesta;
	private BufferedReader stdIn;
	
	public TestGreey(){
		// preparazione standard input per l'interazione con l'utente
	     stdIn= new BufferedReader(new InputStreamReader(System.in));	
	    tmp = new Vector<Double>();
	}
	
	public void insertValue(Double a){
		if(tmp.size()==numberRSSI){
			tmp.remove(0);
			tmp.add(a);
		}else
			tmp.add(a);
	}
	
	public void start() {
		
		try {
			while ( (richiesta=stdIn.readLine()) != null) {
				insertValue(Double.valueOf(richiesta));
				a = new double[tmp.size()];
				for(int i = 0 ; i< tmp.size();i++)
					a[i] = tmp.get(i);
				try {
					filter =new GreyModel(a);
					filter2 =new GreyModel_v2(a);
				} catch (InvalidParameter e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				double prevision = filter.predictRSSI();
				double prevision2 = filter2.predictRSSI();
				System.out.println("Valore predetto:"+prevision+" v2:"+prevision2);
				for (int i = 0; i<tmp.size();i++)
					System.out.print(a[i]+"\t");
				System.out.println("\n");
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		TestGreey t = new TestGreey();
		t.start();
		
	}
}
