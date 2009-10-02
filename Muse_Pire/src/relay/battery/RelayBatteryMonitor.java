/**
 * 
 */
package relay.battery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import parameters.Parameters;
import relay.WeightCalculator;
import relay.positioning.GreyModel_v2;
import relay.positioning.RSSIFilter;
import relay.wnic.AccessPointData;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;

/**
 * @author Luca Campeti
 *
 */
/**Classe che rappresenta un oggetto in grado di monitorare il livello della batteria locale
 * e di avvertire un Observer del suo esaurimento
 * @author Luca Campeti
 */
public class RelayBatteryMonitor extends Observable {

	private long period = -1;
	private Timer timer = null;
	//private Logger logger = null;
	private boolean started;
	private static double batteryLevel = -1;
	private static StringTokenizer st = null;
	private static BufferedReader br = null;


	/**Metodo per ottenere un RelayPositionAPMonitor
	 * @param p  un long che rappresenta il periodo (in ms) con cui il RelayBatteryMonitor va a verificare 
	 * il livello della batteria
	 * @param electionManager l'Observer che deve essere avvertito in caso 
	 * il livello della batteria scenda sotto la soglia di sicurezza
	 */
	public RelayBatteryMonitor(long p, Observer electionManager){

		period = p;
		addObserver(electionManager);
		started = false;
		//logger = new Logger();
	}


	/**Metodo per far partire il Thread periodico che si occupa di rilevare 
	 * il livello attuale della batteria 
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

			batteryLevel = getBatteryLevel();

			System.out.println("\nNUOVO LIVELLO BATTERIA: " + batteryLevel +" /1");			

			if(batteryLevel <= Parameters.BATTERY_LOW_THRS){

				System.err.println("BATTERIA VICINISSIMA ALL'ESAURIMENTO...");
				setChanged();
				notifyObservers("DISCONNECTION_WARNING");
			}


			System.out.println("***********************FINE mainTask*************************\n");

		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**Metodo per ottenere il livello attuale della batteria
	 * @return un double, compreso tra 0 e 1, che rappresenta il livello attuale della batteria
	 * @throws IOException
	 */
	public synchronized static double getBatteryLevel() throws IOException{
		
		double res = -1;
		
		try {
			Process p= Runtime.getRuntime().exec("/usr/bin/acpi -b");
			p.waitFor();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String result = null;
			st = new StringTokenizer(br.readLine(),"%");
			br.close();
			result = st.nextToken();
			result = result.substring(result.length()-3, result.length()).trim();
			res = (double)((double)Double.parseDouble(result)/(double)100);
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return res;
	}
}

/*class TestRelayBattery{
	
	public static void main(String args[]){
		TestBatteryObserver tbo = new TestBatteryObserver();
		RelayBatteryMonitor rbm = new RelayBatteryMonitor(Parameters.BATTERY_MONITOR_PERIOD,tbo);
		rbm.start();	
	}
}


class TestBatteryObserver implements Observer {

	 (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 
	@Override
	public void update(Observable arg0, Object arg1) {
		System.out.println("RICEVUTO MESSAGGIO: " + (String)arg1);
		((RelayBatteryMonitor)arg0).close();
	}
}*/