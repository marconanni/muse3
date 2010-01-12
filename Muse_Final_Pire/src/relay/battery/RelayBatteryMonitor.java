package relay.battery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import parameters.ElectionConfiguration;



/**
 * @author Luca Campeti
 *
 */
/**Classe che rappresenta un oggetto in grado di monitorare il livello 
 * della batteria locale e di avvertire un Observer del suo esaurimento
 * @author Luca Campeti (modificato da Pire Dejaco)
 */
public class RelayBatteryMonitor extends Observable {

	private long period = -1;
	private Timer timer = null;
	private boolean started;
	private static double batteryLevel = -1;
	private static StringTokenizer st = null;
	private static BufferedReader br = null;


	/**Metodo per ottenere un RelayBatteryMonitor
	 * @param p  un long che rappresenta l'intervallo di verifica
	 * @param electionManager l'Observer che deve essere avvertito 
	 */
	public RelayBatteryMonitor(long p, Observer electionManager){
		period = p;
		addObserver(electionManager);
		started = false;
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
		started = false;
		timer.cancel();
	}
	
	public boolean isStarted(){return started;}

	private void mainTask(){
		try{
			batteryLevel = getBatteryLevel();
			if(batteryLevel <= ElectionConfiguration.BATTERY_LOW_THRS){
				setChanged();
				notifyObservers("DISCONNECTION_WARNING");
			}
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
			
		} catch (InterruptedException e) {e.printStackTrace();} 
		return res;
	}
}