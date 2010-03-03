package test.RSSI;

import debug.DebugConsole;

import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.exception.WNICException;

public class TestGreey {
	private static String wifiInterface = "wlan0";
	private static String wifiNetwork = "lord";
	private static int numberOfPreviousRSSI = 5;
	private static int minNumberOfRSSI = 5;
	private static double previsionTHRS = 20;
	private static long interval = 1500;

	
	private RelayAPWNICLinuxController wnic1 = null;
	private RelayAPWNICLinuxController wnic2 = null;
	private RelayAPWNICLinuxController wnic3 = null;
	private RelayAPWNICLinuxController wnic4 = null;
	private RelayPositionAPMonitorTest ap = null;
	
	private DebugConsole console = null;

	
	
	
	public TestGreey(){
		
		try {
			//console = new DebugConsole("TEST RSSI");
			wnic1 = new RelayAPWNICLinuxController(5,wifiInterface,wifiNetwork);
			wnic2 = new RelayAPWNICLinuxController(10,wifiInterface,wifiNetwork);
			wnic3 = new RelayAPWNICLinuxController(15,wifiInterface,wifiNetwork);
			wnic4 = new RelayAPWNICLinuxController(20,wifiInterface,wifiNetwork);
			ap = new RelayPositionAPMonitorTest(wnic1,wnic2,wnic3,wnic4,interval,minNumberOfRSSI,previsionTHRS);
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
