package test.RSSI;

import debug.DebugConsole;

import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.exception.WNICException;

public class TestGreey {
	private static String wifiInterface = "wlan1";
	private static String wifiNetwork = "muselab";
	private static int numberOfPreviousRSSI = 15;
	private static int minNumberOfRSSI = 5;
	private static double previsionTHRS = 20;
	private static long interval = 10;

	
	private RelayAPWNICLinuxController wnic = null;
	private RelayPositionAPMonitorTest ap = null;
	
	private DebugConsole console = null;

	
	
	
	public TestGreey(){
		
		try {
			console = new DebugConsole("TEST RSSI");
			wnic = new RelayAPWNICLinuxController(numberOfPreviousRSSI,wifiInterface,wifiNetwork);
			ap = new RelayPositionAPMonitorTest(wnic,interval,minNumberOfRSSI,previsionTHRS);
			ap.setDebugConsole(console);
			ap.start();
		} catch (WNICException e) {e.printStackTrace();}
	}
	
	public static void main(String args[]){
		new TestGreey();
	
}
	
//	private int numberRSSI = 6;
//	private Vector<Double> tmp = null;
//	private RSSIFilter filter, filter2 = null;
//	private double[] a;
//	private String richiesta;
//	private BufferedReader stdIn;
//	
//	public TestGreey(){
//		// preparazione standard input per l'interazione con l'utente
//	     stdIn= new BufferedReader(new InputStreamReader(System.in));	
//	    tmp = new Vector<Double>();
//	}
//	
//	public void insertValue(Double a){
//		if(tmp.size()==numberRSSI){
//			tmp.remove(0);
//			tmp.add(a);
//		}else
//			tmp.add(a);
//	}
//	
//	public void start() {
//		
//		try {
//			while ( (richiesta=stdIn.readLine()) != null) {
//				insertValue(Double.valueOf(richiesta));
//				a = new double[tmp.size()];
//				for(int i = 0 ; i< tmp.size();i++)
//					a[i] = tmp.get(i);
//				try {
//					filter =new GreyModel(a);
//					try {
//						filter2 =new GreyModel_v2(a);
//					} catch (InvalidParameter e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				} catch (InvalidParameter e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				double prevision = filter.predictRSSI();
//				double prevision2 = filter2.predictRSSI();
//				System.out.println("Valore predetto:"+prevision+" v2:"+prevision2);
//				for (int i = 0; i<tmp.size();i++)
//					System.out.print(a[i]+"\t");
//				System.out.println("\n");
//			}
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	public static void main(String args[]){
//		TestGreey t = new TestGreey();
//		t.start();
//		
//	}
}
