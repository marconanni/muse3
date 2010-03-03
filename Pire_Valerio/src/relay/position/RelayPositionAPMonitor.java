package relay.position;

import relay.wnic.AccessPointData;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;

import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.Number;


import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import debug.DebugConsole;

import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;




/**Classe che rappresenta un oggetto in grado di prevedere una  possibile 
 * disconnessione dall'AP da parte del nodo Relay 
 * @author Luca Campeti, Pire Dejaco
 * @version 1.1
 */
public class RelayPositionAPMonitor extends Observable {

	private long period = -1;
	private RelayWNICController rwc = null;
	private RSSIFilter filter = null;
	private int positiveDisconnectionPrediction = 0;
	private AccessPointData currAP = null;
	private Timer timer = null;
	private boolean started;
	private String tmp;
	private DebugConsole console = null;
	private boolean debug= false;
	
	//per i test
	private WritableWorkbook workbook = null;
	private WritableSheet sheet = null;
	private int count = 0;


	/**Metodo per ottenere un RelayPositionAPMonitor
	 * @param wc un oggetto RelayWNICController creato al di fuori 
	 * da questa classe in maniera da essere accessibile anche direttamente
	 * @param p  un long che rappresenta il periodo (in ms) con cui il RelayPositionAPMonitor va a verificare 
	 * la potenza del segnale rilevata relativamente all'AP
	 * @param electionManager l'Observer che deve essere avvertito in caso di prevista disconnessione dall'AP
	 * @throws InvalidParameter
	 */
	public RelayPositionAPMonitor(RelayWNICController wc, long p, Observer electionManager) throws WNICException{

		if(wc==null)
			throw new WNICException("ERRORE: WNICController non valido");
		rwc=wc;
		period = p;
		positiveDisconnectionPrediction=0;
		addObserver(electionManager);
		currAP=null;
		started = false;
		setDebugConsole(rwc.getDebugConsole());
		debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: CICLO DI CONTROLLO CONNESSIONE AP ogni " + period + " ms");
		
		//per i test
		try {
			workbook = Workbook.createWorkbook(new File("output.xls"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sheet = workbook.createSheet("First Sheett",0);
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

			currAP = rwc.getAssociatedAccessPoint();
			tmp = null;
			double prevision = -1;

			if(currAP!=null){
				double actualRSSI = (double)rwc.updateSignalStrenghtValue();
				if(debug)debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: AP: "  +currAP.getAccessPointName() + " - RSSI: " + actualRSSI);

				double [] a = currAP.getLastSignalStrenghtValues();
				filter =new GreyModel(a);
				prevision = filter.predictRSSI();
				if(debug){
					tmp="[";
					for(int i = 0; i<a.length;i++)
						tmp+=a[i]+",";
					tmp+="]";
					tmp +=" PREVISIONE:"+prevision;
					debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RelayPositionAPMonitor: "+tmp);
				}
				debug(getDebugConsole(), DebugConfiguration.DEBUG_INFO,"RSSI nei confronti del AP:"+ actualRSSI+"  -  Previsione: "+prevision);
				if(count<=50){
					Number n1 = new Number(0,count,actualRSSI);
					Number n2 = new Number(1,count,prevision);
					sheet.addCell(n1);
					sheet.addCell(n2);
					
				}
				if(count ==50){
					workbook.write();
					workbook.close();
				}
				count++;
				if(prevision >= ElectionConfiguration.AP_DISCONNECTION_THRS){
					positiveDisconnectionPrediction++;
					debug(getDebugConsole(), DebugConfiguration.DEBUG_WARNING,"RelayPositionAPMonitor: RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE ("+positiveDisconnectionPrediction+" su"+ElectionConfiguration.NUMBER_OF_AP_DISCONNECTION_DETECTION+")");

					if(positiveDisconnectionPrediction == ElectionConfiguration.NUMBER_OF_AP_DISCONNECTION_DETECTION){
						debug(getDebugConsole(), DebugConfiguration.DEBUG_ERROR,"RelayPositionAPMonitor: DISCONNESSIONE RILEVATA COME SICURA. AVVERTO!!!!");
						setChanged();
						notifyObservers("DISCONNECTION_WARNING");
					}
				}
				else{
					positiveDisconnectionPrediction=0;				
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



