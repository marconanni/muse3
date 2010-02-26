package test.RSSI;

import relay.position.GreyModel;
import relay.position.RSSIFilter;
import relay.wnic.AccessPointData;
import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.Number;

import debug.DebugConsole;

import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;




/**Classe che rappresenta un oggetto in grado di prevedere una  possibile 
 * disconnessione dall'AP da parte del nodo Relay 
 * @author Luca Campeti
 */
public class RelayPositionAPMonitorTest{

	private long period = -1;
	private RelayWNICController rwc1 = null;
	private RelayWNICController rwc2 = null;
	private RelayWNICController rwc3 = null;
	private RelayWNICController rwc4 = null;
	private RSSIFilter filter1 = null;
	private RSSIFilter filter2 = null;
	private RSSIFilter filter3 = null;
	private RSSIFilter filter4 = null;
	private int positiveDisconnectionPrediction = 0;
	private AccessPointData currAP1 = null;
	private AccessPointData currAP2 = null;
	private AccessPointData currAP3 = null;
	private AccessPointData currAP4 = null;
	private Timer timer = null;
	private boolean started;
	private String tmp;
	private DebugConsole console = null;
	private boolean debug= false;
	private int min=0;
	private double thr=0;
	private long time = -1;
	private double lastRSSI = -1;
	private WritableWorkbook workbook = null;
	private WritableSheet sheet = null;
	private BufferedWriter bw = null;
	private int count = 0;
	private int count1 = 0;
	private int startAt = 0;


	/**Metodo per ottenere un RelayPositionAPMonitor
	 * @param wc un oggetto RelayWNICController creato al di fuori 
	 * da questa classe in maniera da essere accessibile anche direttamente
	 * @param p  un long che rappresenta il periodo (in ms) con cui il RelayPositionAPMonitor va a verificare 
	 * la potenza del segnale rilevata relativamente all'AP
	 * @param electionManager l'Observer che deve essere avvertito in caso di prevista disconnessione dall'AP
	 * @throws InvalidParameter
	 */
	public RelayPositionAPMonitorTest(RelayAPWNICLinuxController wc1,RelayAPWNICLinuxController wc2,RelayAPWNICLinuxController wc3,RelayAPWNICLinuxController wc4, long p,int min, double thr) throws WNICException{

		if(wc1==null)
			throw new WNICException("ERRORE: WNICController non valido");
		rwc1=wc1;
		rwc2=wc2;
		rwc3=wc3;
		rwc4=wc4;
		period = p;
		positiveDisconnectionPrediction=0;
		currAP1=null;
		currAP2=null;
		currAP3=null;
		currAP4=null;
		started = false;
		this.min = min;
		this.thr = thr;
		this.time = System.currentTimeMillis();
		try {
			workbook = Workbook.createWorkbook(new File("output.xls"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sheet = workbook.createSheet("First Sheett",0);


		
//		try {
//			this.bw = new BufferedWriter(new FileWriter("data.txt", false));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}


		setDebugConsole(rwc1.getDebugConsole());
		debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: CICLO DI CONTROLLO CONNESSIONE AP ogni " + period + " ms");
	}


	/**Metodo per far partire il Thread periodico che si occupa di rilevare 
	 * la potenza del segnale rilevata localmente nei confronti dell'AP e 
	 * di prevedere una possibile disconnessione da quest'ultimo 
	 */
	public void start() {
		started = true;
		timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run(){ mainTask();} 
		}, 0L , period );				
	}
	
	public void stop(){
		timer.cancel();
		started = false;
	}


	/**Metodo per chiudere il RelayPositionClientsMonitor
	 */
	public void close(){
		timer.cancel();
	}


	private void mainTask(){
		try{

			currAP1 = rwc1.getAssociatedAccessPoint();
			currAP2 = rwc2.getAssociatedAccessPoint();
			currAP3 = rwc3.getAssociatedAccessPoint();
			currAP4 = rwc4.getAssociatedAccessPoint();
			tmp = null;
			double prevision1 = -1;
			double prevision2 = -1;
			double prevision3 = -1;
			double prevision4 = -1;

			if(currAP1!=null){
				double actualRSSI = (double)rwc1.updateSignalStrenghtValue();
				if(debug)debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: AP: "  +currAP1.getAccessPointName() + " - RSSI: " + actualRSSI);

				if(actualRSSI!=lastRSSI){
					count1++;
					lastRSSI=actualRSSI;
					if(count1>=startAt){
						currAP1.addSignalStrenghtValue(actualRSSI);
						currAP2.addSignalStrenghtValue(actualRSSI);
						currAP3.addSignalStrenghtValue(actualRSSI);
						currAP4.addSignalStrenghtValue(actualRSSI);
						double [] a1 = currAP1.getLastSignalStrenghtValues();
						double [] a2 = currAP2.getLastSignalStrenghtValues();
						double [] a3 = currAP3.getLastSignalStrenghtValues();
						double [] a4 = currAP4.getLastSignalStrenghtValues();
						filter1 =new GreyModel(a1, min, thr);
						filter2 =new GreyModel(a2, min, thr);
						filter3 =new GreyModel(a3, min, thr);
						filter4 =new GreyModel(a4, min, thr);
						prevision1 = filter1.predictRSSI();
						prevision2 = filter2.predictRSSI();
						prevision3 = filter3.predictRSSI();
						prevision4 = filter4.predictRSSI();
						if(debug){
							tmp="[";
							for(int i = 0; i<a1.length;i++)
								tmp+=a1[i]+",";
							tmp+="]";
							tmp +=" PREVISIONE:"+prevision1;
							debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: "+tmp);
						}
						double tt = (System.currentTimeMillis()-time);
						Number n = new Number(0,count,tt);
							
							Number n1 = new Number(1,count,actualRSSI);
							Number n2 = new Number(2,count,prevision1);
							
							Number n3 = new Number(4,count,actualRSSI);
							Number n4 = new Number(5,count,prevision2);
							
							Number n5 = new Number(7,count,actualRSSI);
							Number n6 = new Number(8,count,prevision3);
							
							Number n7 = new Number(10,count,actualRSSI);
							Number n8 = new Number(11,count,prevision4);
						sheet.addCell(n);
						sheet.addCell(n1);
						sheet.addCell(n2);
						sheet.addCell(n3);
						sheet.addCell(n4);
						sheet.addCell(n5);
						sheet.addCell(n6);
						sheet.addCell(n7);
						sheet.addCell(n8);
						
						System.out.println(count);
					
						if(count ==300){
							workbook.write();
							workbook.close();
							System.exit(1);					}
						count++;
					//debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,tt+"\t"+actualRSSI+"\t"+prevision);
					}
				}
			}
			else{
				debug(getDebugConsole(), DebugConfiguration.DEBUG_ERROR,"RelayPositionAPMonitor: Nessun AP rilevato all'istante : " + new Date(System.currentTimeMillis()).toString());
			}
		}catch (Exception e) {e.printStackTrace();}
	}
	
	public boolean isStarted(){return started;}
	
	public void setDebug(boolean db){this.debug = db;}
	
	public void setDebugConsole(DebugConsole console){this.console = console;}
	public DebugConsole getDebugConsole(){return console;}
	
	private void debug(DebugConsole console,int type, String message){
		if(console!=null)console.debugMessage(type, message);
		else{
			if(type==DebugConfiguration.DEBUG_INFO|| type ==DebugConfiguration.DEBUG_WARNING)
				System.out.println(message);
			if(type==DebugConfiguration.DEBUG_ERROR)
				System.err.println(message);
		}
	}
}