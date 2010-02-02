package relay.position;

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

import parameters.DebugConfiguration;
import parameters.ElectionConfiguration;
import parameters.MessageCodeConfiguration;
import parameters.NetConfiguration;
import parameters.PortConfiguration;
import parameters.TimeOutConfiguration;

import relay.connection.RelayCM;
import relay.connection.RelayConnectionFactory;
import relay.messages.RelayMessageFactory;
import relay.messages.RelayMessageReader;
import relay.timeout.RelayTimeoutFactory;
import relay.timeout.TimeOutSingleWithMessage;
import relay.wnic.exception.InvalidParameter;

/**Classe che rappresenta un oggetto in grado di prevedere un possibile allontanamento del nodo Relay
 * nei confronti dei nodi Clients all'interno del proprio cluster
 * @author  Pire Dejaco, Luca Campeti
 * @version 1.1
 *
 */
public class RelayPositionMonitor extends Observable implements Observer {

	private int positiveDisconnectionPrediction;
	private RelayMessageReader rmr = null;
	private TimeOutSingleWithMessage tnRSSI = null;
	private Timer timer = null;
	private static InetAddress BCAST = null;
	private Vector<Double> averageValues = null;
	private int maxNumberOfAverageValues;
	private RSSIFilter filter = null;
	private long period ;
	private RelayCM rrcm;
	private int seqNum ;
	private double sumOfRSSI;
	private int numberOfValideRSSI;
	private boolean started;
	private boolean stopped;
	private DebugConsole console = null;
	
	static{
		try {
			BCAST = InetAddress.getByName(NetConfiguration.RELAY_CLUSTER_BROADCAST_ADDRESS);
		} catch (UnknownHostException e) {e.printStackTrace();} 
	}


	/**Costruttore del RelayPositioniClientsMonitor che avverte il RelayElectionManager 
	 * quando rileva una possibile disconnessione dai Clients
	 * @param imBigBoss indica se si tratta del relay principle (cluster head) o di un relay secondario (cluster)
	 * @param rwnic interfaccia della scheda di rete da cui devo prelevare il valore RSSI (in caso di Bigboss non è necessario)  
	 * @param maxNAV il numero massimo di valori di media degli RSSI rilevati dai Clients 
	 * @param p il periodo con cui il RelayPositioniClientsMonitor richiede ai Clients 
	 * @param electionManager l'ElectionManager che deve essere avvertito allorchè si rilevi una possibile disconnessione
	 */
	public RelayPositionMonitor(int maxNAV, long p, Observer electionManager, DebugConsole console){
		this.period = p;
		seqNum = -1;
		this.maxNumberOfAverageValues = maxNAV; 
		positiveDisconnectionPrediction = 0;
		averageValues = new Vector<Double>();
		addObserver(electionManager);
		rrcm = RelayConnectionFactory.getRSSIClusterConnectionManager(this,false);	
		setDebugConsole(console);
		setStarted(false);
		
	}
	
	/**Metodo per far partire il Thread periodico che si occupa del reperimento 
	 * degli RSSI che i Client serviti avvertono nei confronti del nodo Relay per scoprire
	 * se si sta verificando una disconnessione dai Clients serviti. 
	 */
	public void start(){
		if(!rrcm.isStarted()){
			rrcm.start();
			setStarted(true);
		}else if(rrcm.isStoped()){
			rrcm.resumeReceiving();
			setStopped(false);
		}
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
		if(tnRSSI!=null)tnRSSI.cancelTimeOutSingleWithMessage();
		setStarted(false);
		setStopped(false);
	}
	
	public void stop(){
		timer.cancel();
		if(tnRSSI!=null)tnRSSI.cancelTimeOutSingleWithMessage();
		if(rrcm.isStarted())rrcm.stopReceiving();
		setStopped(true);
	}


	private void mainTask(){
		DatagramPacket dp = null;
		try {
			//BCAS
			dp = RelayMessageFactory.buildRequestRSSI(seqNum,BCAST, PortConfiguration.RSSI_PORT_IN);
			numberOfValideRSSI = 0;
			sumOfRSSI = 0;
			rrcm.sendTo(dp);	
			seqNum++;
			tnRSSI = RelayTimeoutFactory.getSingeTimeOutWithMessage(this, TimeOutConfiguration.TIMEOUT_NOTIFY_RSSI, TimeOutConfiguration.TIME_OUT_NOTIFY_RSSI);
			console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionCOntroller: spedito RSSI Request a tutti e TIMEOUT_NOTIFY_RSSI attivato");
		} catch (Exception e) {e.printStackTrace();}
	}

	public synchronized void update(Observable arg0, Object arg1) {

		if(arg1 instanceof DatagramPacket){
			rmr= new RelayMessageReader();

			try {
				rmr.readContent((DatagramPacket)arg1);
			} catch (IOException e) {e.printStackTrace();}

			if(rmr.getCode() == MessageCodeConfiguration.NOTIFY_RSSI){
				double RSSIValue = rmr.getRSSI();
				int activeClient = rmr.getActiveClient();
				if(rmr.getTypeNode()==MessageCodeConfiguration.TYPERELAY)
					if(console!=null)console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionController: ricevuto nuovo valore RSSI da un RELAY attivo IP:"+ ((DatagramPacket)arg1).getAddress()+" RSSI:"+rmr.getRSSI()+" ActiveClient:"+activeClient);
					else System.out.println("RelayPositionController: ricevuto nuovo valore RSSI da un RELAY attivo IP:"+ ((DatagramPacket)arg1).getAddress()+" RSSI:"+rmr.getRSSI()+" ActiveClient:"+activeClient);
				if(rmr.getTypeNode() ==MessageCodeConfiguration.TYPECLIENT)
					if(console!=null)console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionController: ricevuto nuovo valore RSSI da un CLIENT IP:"+ ((DatagramPacket)arg1).getAddress()+" RSSI:"+rmr.getRSSI()+" ActiveClient:"+activeClient);
					else System.out.println("RelayPositionController: ricevuto nuovo valore RSSI da un CLIENT IP:"+ ((DatagramPacket)arg1).getAddress()+" RSSI:"+rmr.getRSSI()+" ActiveClient:"+activeClient);

				if(RSSIValue < ElectionConfiguration.VALID_RSSI_THRS && RSSIValue > 0 && RSSIValue != Double.NaN){
					sumOfRSSI = sumOfRSSI + (RSSIValue*activeClient);
					numberOfValideRSSI+=activeClient;
				}	
			}
			rmr = null;
		}


		if(arg1 instanceof String){
			String timeoutMessage = (String)arg1;
			if(timeoutMessage.equals("TIMEOUTNOTIFYRSSI")){

				if(numberOfValideRSSI != 0){
					try {
						//calcolo media degli RSSI ottenuti allo scadere del TIMEOUT_NOTIFY_RSSI
						double toInsert = (double)((double)sumOfRSSI/(double)numberOfValideRSSI);
						addNewAverageValue(toInsert);
						
						filter = new GreyModel(getLastAverageValues());
						double prevision = filter.predictRSSI();

						console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionController: RSSI PREVISTO "+prevision);

						if(prevision >= ElectionConfiguration.CLIENTS_DISCONNECTION_THRS){
							positiveDisconnectionPrediction++;
							console.debugMessage(DebugConfiguration.DEBUG_INFO,"RelayPositionController: RSSI PREVISTO SUPERA SOGLIA DI DISCONNESSIONE. PREDIZIONE DI DISCONNESSIONE No"+positiveDisconnectionPrediction);
							if(positiveDisconnectionPrediction == ElectionConfiguration.NUMBER_OF_CLIENTS_DISCONNECTION_DETECTION){
								console.debugMessage(DebugConfiguration.DEBUG_INFO, " RelayPositionController:DISCONNESSIONE RILEVATA COME SICURA. AVVERTO L'OBSERVER");
								setChanged();
								notifyObservers("DISCONNECTION_WARNING");
							}
						}
						else 
							positiveDisconnectionPrediction=0;	
					} catch (InvalidParameter e) {e.printStackTrace();}
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
			throw new InvalidParameter("ERRORE: Potenza del segnale non valida "+val);
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
	
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void setStopped(boolean stopped) {
		this.stopped = stopped;
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	
	public void setDebugConsole(DebugConsole console){this.console = console;}
	public DebugConsole getDebugConsole(){return console;}
}