package relay.positioning;

import relay.wnic.AccessPointData;
import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;


import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import parameters.Parameters;



/**Classe che rappresenta un oggetto in grado di prevedere una  possibile 
 * disconnessione dall'AP da parte del nodo Relay 
 * @author Luca Campeti
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
		rwc.getDebugConsole().debugMessage(Parameters.DEBUG_INFO,"RelayPositionAPMonitor: CICLO DI CONTROLLO CONNESSIONE AP ogni " + period + " ms");
		//rwc.getDebugConsole().debugMessage(Parameters.DEBUG_INFO,"RelayPositionAPMonitor: AP corrente a cui si è connessi "  +currAP.getAccessPointName());
		
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
				rwc.getDebugConsole().debugMessage(Parameters.DEBUG_INFO,"RelayPositionAPMonitor: AP: "  +currAP.getAccessPointName() + " - RSSI: " + actualRSSI);

				double [] a = currAP.getLastSignalStrenghtValues();
				tmp="[";
				for(int i = 0; i<a.length;i++)
					tmp+=a[i]+",";
				tmp+="]";
				filter =new GreyModel_v2(a);
				prevision = filter.predictRSSI();
				tmp +=" PREVISIONE:"+prevision				;
				rwc.getDebugConsole().debugMessage(Parameters.DEBUG_INFO,"RelayPositionAPMonitor: "+tmp);
				
				if(prevision >= Parameters.AP_DISCONNECTION_THRS){
					positiveDisconnectionPrediction++;
					rwc.getDebugConsole().debugMessage(Parameters.DEBUG_WARNING,"RelayPositionAPMonitor: RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE ("+positiveDisconnectionPrediction+" su"+Parameters.NUMBER_OF_AP_DISCONNECTION_DETECTION+")");

					if(positiveDisconnectionPrediction == Parameters.NUMBER_OF_AP_DISCONNECTION_DETECTION){
						rwc.getDebugConsole().debugMessage(Parameters.DEBUG_ERROR,"RelayPositionAPMonitor: DISCONNESSIONE RILEVATA COME SICURA. AVVERTO!!!!");
						setChanged();
						notifyObservers("DISCONNECTION_WARNING");
					}
				}
				else{
					positiveDisconnectionPrediction=0;				
					//rwc.getDebugConsole().debugMessage(Parameters.DEBUG_INFO,"RelayPositionAPMonitor : RSSI PREVISTO NON SUPERA SOGLIA DI DISCONNESSIONE.");
				}	
			}
			else{
				rwc.getDebugConsole().debugMessage(Parameters.DEBUG_ERROR,"RelayPositionAPMonitor: Nessun AP rilevato all'istante : " + new Date(System.currentTimeMillis()).toString());
			}
		}catch (Exception e) {e.printStackTrace();}
	}
	
	public boolean isStarted(){return started;}
}



class TestRelayPositionAPMonitor{

	public static void main(String args[]){
		System.out.println("TestRelayPositionAPMonitor");
		TestObserver to = new TestObserver();
		RelayAPWNICLinuxController rwlc = null;
		RelayPositionAPMonitor rpAPm = null;
		try {
			rwlc = new RelayAPWNICLinuxController(15,"wlan0", "ALMAWIFI");
			rpAPm = new RelayPositionAPMonitor(rwlc,4000,to);
			rpAPm.start();
			
		/*	try {
				Thread.sleep(11990);
				rpAPm.close();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	
		} catch (WNICException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
