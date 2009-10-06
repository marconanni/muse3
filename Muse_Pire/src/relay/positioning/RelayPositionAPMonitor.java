package relay.positioning;

import relay.wnic.AccessPointData;
import relay.wnic.RelayWNICController;
import relay.wnic.RelayWNICLinuxController;

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


	/**Metodo per chiudere il RelayPositionClientsMonitor
	 */
	public void close(){
		timer.cancel();
	}


	private void mainTask(){
		try{

			System.out.println("\n***********************INIZIO mainTask*************************");

			System.out.println("RelayPositionAPMonitor.mainTask(): (1). CICLO DI CONTROLLO CONNESSIONE AP ogni " + period + " ms");

			currAP = rwc.getAssociatedAccessPoint();

			double prevision = -1;

			if(currAP!=null){

				double actualRSSI = (double)rwc.updateSignalStrenghtValue();

				System.out.println("RelayPositionAPMonitor.mainTask(): (2). AP corrente a cui si è connessi "  +currAP.getAccessPointName() + " valore ultimo RSSI: " + actualRSSI);

				filter =new GreyModel_v2(currAP.getLastSignalStrenghtValues());
				prevision = filter.predictRSSI();

				if(prevision >= Parameters.AP_DISCONNECTION_THRS){

					positiveDisconnectionPrediction++;
					System.out.println("RelayPositionAPMonitor.mainTask(): (3). RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE. PREDIZIONE DI DISCONNESSIONE No"+positiveDisconnectionPrediction);

					if(positiveDisconnectionPrediction == Parameters.NUMBER_OF_AP_DISCONNECTION_DETECTION){

						System.out.println("RelayPositionAPMonitor.mainTask(): (4). DISCONNESSIONE RILEVATA COME SICURA. AVVERTO L'OBSERVER");
						setChanged();
						notifyObservers("DISCONNECTION_WARNING");
					}
				}
				else{
					positiveDisconnectionPrediction=0;				
					System.out.println("RelayPositionAPMonitor.mainTask(): (3). RSSI PREVISTO NON SUPERA SOGLIA DI DISCONNESSIONE.");
				}	
			}
			else{
				System.err.println("RelayPositionAPMonitor.mainTask(): (2). Nessun AP rilevato all'istante : " + new Date(System.currentTimeMillis()).toString());
			}

			System.out.println("***********************FINE mainTask*************************\n");

		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}



class TestRelayPositionAPMonitor{

	public static void main(String args[]){
		System.out.println("TestRelayPositionAPMonitor");
		TestObserver to = new TestObserver();
		RelayWNICLinuxController rwlc = null;
		RelayPositionAPMonitor rpAPm = null;
		try {
			rwlc = new RelayWNICLinuxController(15,"eth1", "NETGEAR");
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