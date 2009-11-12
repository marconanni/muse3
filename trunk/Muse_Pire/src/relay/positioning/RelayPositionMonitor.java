package relay.positioning;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import debug.DebugConsole;

import parameters.Parameters;

import relay.RelayMessageFactory;
import relay.RelayMessageReader;
import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;

import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutNotifyRSSI;
import relay.timeout.TimeOutSearch;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.InvalidParameter;
import relay.wnic.exception.WNICException;


/**Classe che rappresenta un oggetto in grado di prevedere un possibile allontanamento del nodo Relay
 * nei confronti dei nodi Clients che sta servendo e anche dal nodo relay big boss.
 * @author  Luca Campeti
 *
 */
public class RelayPositionMonitor extends Observable implements Observer {



	private int positiveDisconnectionPrediction;
	private RelayMessageReader rmr = null;
	private TimeOutNotifyRSSI tnRSSI = null;
	private Timer timer = null;

	private static InetAddress BCAST = null;

	static{
		try {
			BCAST = InetAddress.getByName(Parameters.BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} 
	}

	private Vector<Double> averageValues = null;
	private int maxNumberOfAverageValues;

	//private Logger logger = null;
	private DatagramPacket dp = null;
	private DatagramPacket notifyRSSI = null;
	private RSSIFilter filter = null;
	private long period ;
	private RelayCM rrcm;
	private int seqNum ;
	private double sumOfRSSI;
	private int numberOfValideRSSI;
	private InetAddress connectedRelayAddress;
	private String localRelayAddress;
	private boolean enableToMonitor;
	private boolean started;
	private static int sequenceNumber = 0;
	private boolean imBigBoss=false;
	
	private DebugConsole console = null;
	
	private RelayWNICController rwnic = null;


	/**Costruttore del RelayPositioniClientsMonitor che avverte il RelayElectionManager 
	 * quando rileva una possibile disconnessione dai Clients  
	 * @param maxNAV il numero massimo di valori di media degli RSSI rilevati dai Clients 
	 * @param p il periodo con cui il RelayPositioniClientsMonitor richiede ai Clients 
	 * il valore di RSSI che essi rilevano in riferimento al Relay
	 * @param electionManager l'ElectionManager che deve essere avvertito allorchè si rilevi una possibile disconnessione
	 */
	public RelayPositionMonitor(boolean imBigBoss, RelayWNICController rwnic, int maxNAV, long p, Observer electionManager){
		this.rwnic = rwnic;
		period = p;
		seqNum = -1;
		maxNumberOfAverageValues = maxNAV; 
		positiveDisconnectionPrediction = 0;
		averageValues = new Vector<Double>();
		addObserver(electionManager);
		rrcm = RelayConnectionFactory.getRSSIConnectionManager(this);	
		enableToMonitor = false;
		started = false;
		this.imBigBoss = imBigBoss;
		this.setDebugConsole(rwnic.getDebugConsole());
		//logger = new Logger();
	}
	
	/**Metodo per far partire il Thread che sta in ascolto su una determinata porta
	 * quando gli arriva un messaggio richiama il metodo notify(observer)  
	 */
	public void start(){
		if(enableToMonitor){
			if(!rrcm.isStarted())
				rrcm.start();
			started = true;
		}
		
	}


	/**Metodo per far partire il Thread periodico che si occupa delreperimento 
	 * degli RSSI che i Client serviti avvertono nei confronti del nodo Relay per scoprire
	 * se si sta verificando una disconnessione dai Clients serviti. 
	 */
	public void startRSSIMonitor() {
		if(!rrcm.isStarted())
			rrcm.start();
		timer = new Timer();
		timer.schedule(new TimerTask(){

			public void run(){ mainTask();} 
		}, 0L , period );				
	}


	/**Metodo per chiudere il RelayPositionClientsMonitor
	 */
	public void close(){
		rrcm.close();
		timer.cancel();

		//cancellazione di eventuali timeout ancora in corso
		if(tnRSSI!=null)tnRSSI.cancelTimeOutNotifyRSSI();
	}


	private void mainTask(){

		DatagramPacket dp = null;

		try {
			//System.out.println("\n*********************INIZIO mainTask*************************");
			dp = RelayMessageFactory.buildRequestRSSI(seqNum,BCAST, Parameters.RSSI_PORT_OUT, localRelayAddress);
			numberOfValideRSSI = 0;
			sumOfRSSI = 0;
			rrcm.sendTo(dp);	
			seqNum++;
			tnRSSI = RelayTimeoutFactory.getTimeOutNotifyRSSI(this,Parameters.TIMEOUT_NOTIFY_RSSI);
			console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionCOntroller: spedito RSSI Request a tutti");
			//System.out.println("***********************FINE mainTask*************************\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public synchronized void update(Observable arg0, Object arg1) {

		if(arg1 instanceof DatagramPacket){
			int RSSIvalue =-1;

			rmr= new RelayMessageReader();

			try {
				rmr.readContent((DatagramPacket)arg1);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if(rmr.getCode() == Parameters.NOTIFY_RSSI){
				
				//posso pesare il valore rssi del big boss a cui è collegato il relay tramite connectedRelayInetAddress

				double RSSIValue = rmr.getRSSI();

				System.out.println("RelayPositionClientsMonitor.update(): ricevuto nuovo valore di RSSI: " +rmr.getRSSI());
				console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionController: ricevuto nuovo valore RSSI:"+ ((DatagramPacket)arg1).getAddress()+" : "+rmr.getRSSI());

				if(RSSIValue < Parameters.VALID_RSSI_THRS && RSSIValue > 0 && RSSIValue != Double.NaN){
					sumOfRSSI = sumOfRSSI + RSSIValue;
					numberOfValideRSSI++;
					System.err.println("numberOfValideRSSI: " +numberOfValideRSSI);
				}	
				//SOLO IN CASO DI DEBUG
				//setChanged();
				//notifyObservers((DatagramPacket)arg1);
			}
			
			if((rmr.getCode() == Parameters.REQUEST_RSSI)&&((DatagramPacket)arg1).getAddress().equals(connectedRelayAddress)&&(!imBigBoss)){
				try{
					RSSIvalue = rwnic.getSignalStrenghtValue();
					notifyRSSI = RelayMessageFactory.buildNotifyRSSI(sequenceNumber, RSSIvalue, connectedRelayAddress, Parameters.RSSI_PORT_IN);
					sequenceNumber++;
					rrcm.sendTo(notifyRSSI);
					console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionController : Inviato RSSI: "+ RSSIvalue +" a: " + connectedRelayAddress+":"+Parameters.RSSI_PORT_IN);
				}catch (WNICException e) {
					console.debugMessage(Parameters.DEBUG_ERROR,"RelayPositionController: Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
					new WNICException("RelayPositionController: Impossibile o leggere il il pacchetto RSSI REQUEST o mandare il valore RSSI al relay");
				}catch (IOException e) {e.printStackTrace();}
			}else{
				//console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionController: Faccio niente");
			}
				
			rmr = null;
		}


		if(arg1 instanceof String){

			String timeoutMessage = (String)arg1;

			if(timeoutMessage.equals("TIMEOUTNOTIFYRSSI")){

				if(numberOfValideRSSI != 0){

					//System.out.println("RelayPositionClientsMonitor.update(): [0]. SCATTATO IL TIMEOUT_NOTIFY_RSSI");	

					try {
						//System.err.println("UPDATE-TIMEOUTNOTIFYRSSI: numberOfValideRSSI="+numberOfValideRSSI);
						//calcolo media degli RSSI ottenuti allo scadere del TIMEOUT_NOTIFY_RSSI
						double toInsert = (double)((double)sumOfRSSI/(double)numberOfValideRSSI);
						addNewAverageValue(toInsert);
						
						//System.out.print("---->[");
						for(int i = 0; i<getLastAverageValues().length-1;i++)System.out.print(getLastAverageValues()[i]+", ");
						//System.out.println(getLastAverageValues()[getLastAverageValues().length-1]+"]");

						filter = new GreyModel_v2(getLastAverageValues());

						double prevision = filter.predictRSSI();

						//System.out.println("RelayPositionClientsMonitor.update(): [1]. RSSI PREVISTO: "+ prevision);
						console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionController: RSSI PREVISTO "+prevision);

						if(prevision >= Parameters.CLIENTS_DISCONNECTION_THRS){
							positiveDisconnectionPrediction++;

							//System.out.println("RelayPositionClientsMonitor.update(): [2]. RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE. PREDIZIONE DI DISCONNESSIONE No"+positiveDisconnectionPrediction);
							console.debugMessage(Parameters.DEBUG_INFO,"RelayPositionController: RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE. PREDIZIONE DI DISCONNESSIONE No"+positiveDisconnectionPrediction);
							if(positiveDisconnectionPrediction == Parameters.NUMBER_OF_CLIENTS_DISCONNECTION_DETECTION){
								//System.out.println("RelayPositionClientsMonitor.update(): [3]. DISCONNESSIONE RILEVATA COME SICURA. AVVERTO L'OBSERVER");
								console.debugMessage(Parameters.DEBUG_INFO, " RelayPositionController:DISCONNESSIONE RILEVATA COME SICURA. AVVERTO L'OBSERVER");
								setChanged();
								notifyObservers("DISCONNECTION_WARNING");
							}
						}
						else{
							positiveDisconnectionPrediction=0;				

							//System.out.println("RelayPositionClientsMonitor.update(): [2]. RSSI PREVISTO NON SUPERA SOGLIA DI DISCONNESSIONE.");
						}	
					} catch (InvalidParameter e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (relay.positioning.InvalidParameter e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}


	/**
	 * Memorizza un nuovo valore di media di RSSI rilevati dai Clients nei confronti del Relay
	 * @param val un nuovo valore di media di RSSI di cui sopra
	 * @throws InvalidParameter se il valore dell'RSSI non e' valido
	 */
	private void addNewAverageValue(double val) throws InvalidParameter {

		if((val<0) || (val>120))
			throw new InvalidParameter("ERRORE: Potenza del segnale dell'AP non valida "+val);
		if(averageValues.size()<maxNumberOfAverageValues)
			averageValues.add(Double.valueOf(val));
		else {
			averageValues.removeElementAt(0);
			averageValues.add(Double.valueOf(val));
		}
	}


	/**
	 * Restituisce gli ultimi valori di media di RSSI memorizzati
	 * @return gli ultimi valori di media di RSSI memorizzati
	 */
	private double[] getLastAverageValues()	{

		double res[]= new double[averageValues.size()];
		for(int i=0; i<res.length; i++)	{
			res[i]=averageValues.elementAt(i).doubleValue();
		}
		return res;
	}
	
	/**Metodo per impostare l'osservazione dei valori di RSSI nei 
	 * confronti dell'indirizzo del Relay
	 * @param rA una String che rappresenta l'indirizzo del Relay a cui è collegato
	 */
	public void setConnectedRelayAddress(String rA) {
		try {
			connectedRelayAddress = InetAddress.getByName(rA);
			enableToMonitor = true;
		} catch (UnknownHostException e) {e.printStackTrace();}
	}
	
	/**Metodo per impostare l'osservazione dei valori di RSSI nei 
	 * confronti dell'indirizzo del Relay
	 * @param rA una String che rappresenta l'indirizzo del Relay a cui è collegato
	 */
	public void setLocalRelayAddress(String rA) {
		localRelayAddress =rA;
	}
	
	public void setDebugConsole(DebugConsole console){
		this.console = console;
	}
}



/*class TestRelayPositionClientsMonitor{

	public static void main(String args[]) throws UnknownHostException{
		TestObserver to = new TestObserver();
		RelayPositionClientsMonitor rpcm = new RelayPositionClientsMonitor(3,6000,to);
		rpcm.start();
		/*InetAddress localhost = null;
		try {
			localhost = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			DatagramSocket ds = new DatagramSocket(7890);
			DatagramPacket dp = ClientMessageFactory.buildNotifyRSSI(0, 120, localhost, Parameters.RELAY_RSSI_RECEIVER_PORT);
			Thread.sleep(4000);
			ds.send(dp);
			ds.close();

			Thread.sleep(12000);
			rpcm.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	}
	}

 /*class TestObserver implements Observer{

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
		System.out.println("\tObserver: notifica ricevuta da: " + ((RelayPositionClientsMonitor)o).toString());
	}
}*/