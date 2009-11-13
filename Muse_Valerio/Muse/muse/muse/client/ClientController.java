package muse.client;

import hardware.wnic.exception.WNICException;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import muse.client.connection.*;
import muse.client.gui.IClientView;

import debug.DebugConsolle;
import parameters.Parameters;
import util.Logger;

public class ClientController extends Thread implements IStartpointListener{
	
	//controller frame
	private IClientView view;
	//valore booleano per l'esecuzione dei thread
	private boolean running;
	//thread per il controllo dell throughput
	private Thread throughputCtrl;
	//manager per la ricezione del treno di pacchetti proveniente dal proxy
	private ConnectionManager recMgr;
	//manager per l'invio di messaggi di notifica dello stato della trasmissione al proxy
	private ConnectionManager sendMgr;
	//porta su cui � in ascolto il thread di controllo lato proxy


	private int receivingPort;
	private InetAddress serverAddr;
	private double bitRate;
	private double delay;

	private ClientBufferDataPlaying streamPlayer;
	private StartpointTimer spTimer;
	private int rec,send = 0;
	private Object mutex;
	private Object suspensionObject;
	private boolean run;
	private WNICManager wm = null;
	private HandoffManager hm = null;
	private boolean executeBF = false;
	BufferedWriter bw = null;
	
	
	private int serverPort;
	
	/**
	 * Enable or disable handoff handling
	 */
	boolean handoffEnable=Parameters.enableHandoff;
	
	public ClientController(int recPort, int sendPort, IClientView ctrl, int r, int s, int serverPort, int receivingPort,InetAddress serverAddr){
		view = ctrl;
		view.setClientController(this);
		running = true;
		recMgr = new ConnectionManager();
		sendMgr = new ConnectionManager();
		recMgr.openSocket(recPort);
		sendMgr.openSocket(sendPort);
		mutex = new Object();
		
		this.serverPort=serverPort;

		this.receivingPort = receivingPort;
		this.serverAddr = serverAddr;
		rec = r;
		send = s;
		run = true;
		suspensionObject = new Object();
	}
	
	public void run(){	
		
		view.debugMessage("ClientController avviato");
		System.out.println("ClientController avviato");
		streamPlayer = new ClientBufferDataPlaying(serverPort,receivingPort,serverAddr,Parameters.CLIENT_BUFFER,Parameters.TTW, this,view);
		streamPlayer.start();
/*
		try{
			wm = new WNICManager(serverAddr, ctrlPortProxy, sendMgr, streamPlayer, this, suspensionObject);
			Thread wThread = new Thread(wm);
			wThread.setPriority(8);			
			System.out.println("WNIC PRIORITY "+wThread.getPriority());
			wThread.start();
			if(handoffEnable){
				hm = new HandoffManager(wm, streamPlayer, this);
				Thread hThread = new Thread(hm);
				hThread.setPriority(7);
				System.out.println("HANDOFF PRIORITY "+hThread.getPriority());
				hThread.start();
			}
			System.out.println("CONTROLLER PRIORITY "+this.getPriority());			
		}
		catch(Exception x){
			System.err.println("Impossibile istanziare WNICManager: "+x.getMessage());
			killAll();
		}		
*/		
		view.debugMessage("ClientController:attesa messaggi...");
		System.out.println("ClientController:attesa messaggi...");
		while(run){			
			int res = recMgr.receivePacket();
			if(res == 0){
				try{
					ClientMessageReader.readContent(recMgr.getPacket());
					
					int code = ClientMessageReader.getCode();
					if(code == Parameters.BAND_ESTIMATION_REQ){
						System.out.println("Ricevuta richiesta banda del proxy");
						view.debugMessage("Ricevuta richiesta banda del proxy");
						synchronized(mutex){mutex.notify();}
					}
					if(code == Parameters.START_PLAYBACK)
					{
						//delay = Double.parseDouble(ClientMessageReader.getFirstParam());
						delay=70;
						System.out.println("Ricevuto messaggio di start con delay: "+delay);
						view.debugMessage("Ricevuto messaggio di start con delay: "+delay);
						spTimer = new StartpointTimer(this,delay);
//						//inizializzazione thread di controllo dello throughput
//						throughputCtrl = new Thread(){ public void run(){ throughputControl(); } };
//						//avvio thread di controllo dello throughput
//						throughputCtrl.setPriority(10);
//						throughputCtrl.start();
					}
					if((code == Parameters.START_OFF)||(code == Parameters.ACK)){
						System.out.println("Ricevuto messaggio di START_OFF/ACK");
						wm.addRequest(ClientMessageReader.getMessage());
					}
					if(code == Parameters.ERROR){
						//chiusura thread
						System.err.println("Messaggio di errore");
						killAll();
					}
				}
				catch(Exception x){
					System.err.println("ClientController: errore");
					x.printStackTrace();
				}
				
			}
		}
	}

	private void throughputControl() throws IOException{
		while(running)
		{
			try {
				while(!wm.isWNICActive())
				{
					System.out.println("La scheda non � attiva");
					Thread.sleep(1000);
				}
			}
			catch (WNICException e1) {e1.printStackTrace();}
			catch(InterruptedException e){}
			
			if(streamPlayer.isPlaying())
			{
				

					if(bitRate < Parameters.MIN_THROUGHPUT)
					{
						
						try {
							synchronized (suspensionObject) {
								int numFrames = streamPlayer.getFrameOnBuffer();
								System.out.println("Throughput non sufficiente! Numero di frame nel buffer "+numFrames);
//								sendMgr.sendPacket(ClientMessageFactory.buildLTMessage(getSequenceNumber(), serverAddr, ctrlPortProxy, numFrames));
								//sospensione dei thread di controllo buffer e throughput
								if (numFrames > 0) {

									System.out.println("Probing sospensione");
									suspensionObject.wait();
									System.out.println("Probing ripresa");
								}
							}							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				} 
			}
		}
	
	
	
	//MODIFICA: il metodo viene invocato dal ClientChainBufferDataPlaying
	//quando viene generato un evento di buffer pieno
	public void bufferControl(){
		System.out.println("è stato chiamato bufferControl, ma al momento è commentato quel codice");
		/*
		try {
			if (executeBF) {
				incSequenceNumber();
				sendMgr.sendPacket(ClientMessageFactory.buildNoParamMessage(getSequenceNumber(),serverAddr, ctrlPortProxy, Parameters.BUFFER_FULL));
				System.out.println("Inviato messaggio di buffer full al proxy; indice: "+ getSequenceNumber());	
			}
			} catch (IOException e) {}
		*/
		}


	/*
	 * Metodo di callback per il listener dell'evento start playing
	 * @see muse.client.IStartpointListener#startStreamPlaying()
	 */
	public void startStreamPlaying() {
		
		System.out.println("Inizio riproduzione");	
		streamPlayer.startPlaying();
		
		//inizializzazione thread stampa test
		Thread test = new Thread(){ public void run(){ testThread();} };
		test.start();
		
		//inizializzazione thread di controllo dello throughput
		throughputCtrl = new Thread(){ public void run(){ try {
			throughputControl();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} } };
		//avvio thread di controllo dello throughput
		throughputCtrl.setPriority(10);
		throughputCtrl.start();
	}
	
	public void testThread()
	{
		try {bw = new BufferedWriter(new FileWriter("test.txt"));} catch (IOException e) {e.printStackTrace();}
		
		try
		{	
			while(running)
			{				
				Thread.sleep(30000);
				bw.write("***TEST***: lo stato acceso/spento della wnic e' cambiato "+wm.getNumberOfSwitches()+" volte\n");
				bw.write("***TEST***: la scheda e' rimasta accesa per "+wm.getWNICtotalTimeOn()+" millisecondi\n");
				bw.write("***TEST***: la scheda e' rimasta spenta per "+wm.getWNICtotalTimeOff()+" millisecondi\n");
				bw.write("***TEST***: memory in use Bytes "+Runtime.getRuntime().totalMemory()+"\n");
				bw.write("\n\n");
			}		
		}
		catch(Exception e){e.printStackTrace();}
	}
	
	public synchronized void incSequenceNumber() {
		send++;
	}
	
	public synchronized int getSequenceNumber() {
		return send;
	}
	
	public synchronized double getBitrate() {
		return bitRate;
	}
	
	public void killAll(){
		System.out.println("Chiusura processi");
		try { if (bw != null) bw.close(); } catch (Exception e) {e.printStackTrace();}		
		running = false;
		if(wm != null)
			wm.endAll();
		if(hm != null)
			hm.endAll();
		streamPlayer.close();
	}
}