package muse.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;

import muse.client.connection.ConnectionManager;
import muse.client.exception.InvalidParameter;
import muse.client.exception.InvalidProxy;
import parameters.Parameters;
import hardware.wnic.*;
import hardware.wnic.exception.*;

/**
 * WNICManager.java
 * Gestisce le richiesta di accensione e spegnimento della scheda,
 *  sia dai messaggi provenienti dal proxy sia dalle richieste locali al client
 *  tramite i metodi <code>setWNICOn()</code> e <code>setWNICOff()</code>.
 * @author Zapparoli Pamela
 * @version 1.0.1
 *
 */

public class WNICManager implements ClientTask
{
	private WNICController wnic=null;
	private Vector<String> requests;
	private Object reqMutex = new Object();
	private boolean ending;
	private long totalTimeOn;
	private long totalTimeOff;
	private boolean handoffFlag;
	private InetAddress proxyAddr;
	private int proxyPort;
	private AccessPointData futureAP;
	private boolean pendingAck;
	private long wakeupTime;
	private ConnectionManager connection;
	private ClientController controller;
	private ClientBufferDataPlaying buffer;
	private Object suspensionObject;
	private Timer t=new Timer();
	/**
	  * indica quante accensioni e spegnimenti sono stati fatti
	  * se è pari è accesa (sono stati fatti più setOn) altrimenti è spenta (sono stati fatti più setOff)
	  */
	 private int numerOfSwitch;
	
	/**
	 * Tempo necessario a riaccendere la scheda in millisecondi
	 */
	public static final long timeon=30;	//MODIFICA: portato da 200 a 30
	
	/**
	 * Timeout per la ricezione di un ack in millisecondi
	 */
	public static final long ACK_WAITING_TIME=360000;
	
	/**
	 * Costruisce un WNICManager
	 * @param proxyAddr indirizzo del proxy
	 * @param proxyPort porta di controllo del proxy
	 * @param connection il ConnectionManager usato
	 * @param buffer il gestore del buffer
	 * @param controller il ClienController padre
	 * @throws OSException
	 * @throws WNICException
	 * @throws InvalidProxy
	 * @throws InvalidParameter
	 */
	public WNICManager(InetAddress proxyAddr, int proxyPort, ConnectionManager connection,ClientBufferDataPlaying buffer, ClientController controller, Object so) throws OSException, WNICException, InvalidProxy, InvalidParameter
	{
		System.out.println("WNICManager: proxy address "+proxyAddr.getHostName()+" port "+proxyPort);
		if(proxyAddr==null || proxyPort<1 || proxyPort>65536)
			throw new InvalidProxy("ERRORE: indirizzo e/o porta del proxy non validi");
		if(connection==null)
			throw new InvalidParameter("ERRORE: socket di controllo non valida");
		if(controller==null)
			throw new InvalidParameter("ERRORE: ClientController non valido");
		if(buffer==null)
			throw new InvalidParameter("ERRORE: Controller del buffer non valido");
		this.buffer=buffer;
		this.connection=connection;
		this.controller=controller;
		this.proxyAddr=proxyAddr;
		this.proxyPort=proxyPort;
		wnic=WNICFinder.getCurrentWNIC();
		requests= new Vector<String>();
		ending=false;
		handoffFlag=false;
		totalTimeOff=0;
		totalTimeOn=0;
		futureAP=null;
		pendingAck=false;
		wakeupTime=-1;
		suspensionObject = so;
		wakeupTime=0;
	}

	public synchronized int getNumberOfSwitches()
	{
		return numerOfSwitch;
	}
	
	public synchronized void addRequest(String str)
	{
		if(str!=null)
		{
			requests.add(str);
			synchronized(reqMutex){
				reqMutex.notify();
				System.out.println("WNICManager:Nuovo messaggio ricevuto");
				}
		}
	}
	
	private synchronized String getFirstRequest()
	{
		if(requests.size()>0)
			return requests.remove(0);
		else
			return null;
	}
	
	public synchronized void endAll()
	{
		ending=true;
	}
	
	public synchronized boolean setWNICOn() throws WNICException
	{
		boolean res=true;
		if(!wnic.isActive())
		{
			res= wnic.setOn();
			 numerOfSwitch++;
			totalTimeOff+=wnic.getInactiveTime();
		}
		return res;
	}
	
	public synchronized boolean setWNICOff() throws WNICException
	{
		boolean res=true;
		if(wnic.isActive())
		{
			res= wnic.setOff();
			 numerOfSwitch++;
			totalTimeOn+=wnic.getActiveTime();
		}
		return res;
	}
	
	public long getWNICtotalTimeOff()
	{
		return totalTimeOff;
	}
	
	public long getWNICtotalTimeOn()
	{
		return totalTimeOn;
	}
	
	public synchronized void setHandoffFlag()
	{
		System.out.println("WNICManager: segnalazione di handoff");
		handoffFlag=true;
		synchronized(reqMutex)
			{reqMutex.notify();}
	}
	
	private synchronized void resetHandoffFlag()
	{
		handoffFlag=false;
	}
	
	private synchronized boolean isEnding()
	{
		return ending;
	}
	
	private synchronized boolean isHandoffApproacing()
	{
		return handoffFlag;
	}
	
	private synchronized boolean connectToAccessPoint(AccessPointData ap) throws WNICException, InvalidAccessPoint
	{
		return wnic.connectToAccessPoint(ap);
	}
	
	public boolean isWNICActive() throws WNICException
	{
		return wnic.isActive();
	}
	
	public long getWNICActiveTime() throws WNICException
	{
		return wnic.getActiveTime();
	}
	
	public long getWNICInactiveTime() throws WNICException
	{
		return wnic.getInactiveTime();
	}
	
	public boolean isWNICConnected()throws WNICException
	{
		return wnic.isConnected();
	}
	
	public InetAddress getWNICAssociatedIP()throws WNICException
	{
		return wnic.getAssociatedIP();
	}
	
	public int getWNICSignalLevel()throws WNICException, InvalidAccessPoint
	{
		return wnic.getSignalLevel();
	}
	
	public AccessPointData getWNICAssociatedAccessPoint() throws WNICException, InvalidAccessPoint
	{
		return wnic.getAssociatedAccessPoint();
	}
	
	public Vector<AccessPointData> getWNICVisibleAccessPoints() throws WNICException, InvalidAccessPoint
	{
		return wnic.getVisibleAccessPoints();
	}
	
	public AccessPointData getWNICAccessPointWithMaxRSSI() throws WNICException, InvalidAccessPoint
	{
		return wnic.getAccessPointWithMaxRSSI();
	}

	public long getWNICCurrentStateStartTime()
	{
		return wnic.getCurrentStateStartTime();
	}
	
	public void setFutureAccessPoint(AccessPointData ap) throws InvalidAccessPoint
	{
		if(ap==null)
			throw new InvalidAccessPoint("ERRORE: access point nullo");
		futureAP=ap;
	}
	
	public long getWakeUpTime()
	{
		return wakeupTime;
	}
	
	public void run() 
	{
		try
		{
			/**************** avvia il thread di gestione della caduta di connessione ******************************/
			CheckConnection cm= new CheckConnection(wnic);
			cm.start();
			/******************************************* MAIN CYCLE ************************************************/
			while(!isEnding())
			{
				/************** attesa di richieste ***************/
				if((requests.size() == 0) && (!isHandoffApproacing()))
				{
					try
					{
						synchronized(reqMutex)
						{
							System.out.println("WNICManager sospeso in attesa di richieste");
							reqMutex.wait();
						}
					}
					catch(InterruptedException e)
					{}
				}
				System.out.println("############################################");
				/************* gestione richieste ****************/
				if(isHandoffApproacing())
					handoffHandling();//richiesta dall'HandoffManager
				else
				{
					//Messaggi dal proxy
					try
					{
						String currRequest=getFirstRequest();
						ClientMessageReader.readContent(currRequest);
					}
					catch(Exception ex)
					{
						System.out.println("WNICManager:Impossibile leggere il contenuto della richiesta");
						//ricomincia il ciclo
						continue;
					}
					int code = ClientMessageReader.getCode();
					if(code == Parameters.START_OFF)
					{
						/************************** START OFF ******************************/
						System.out.println("WNICManager: ricevuto messaggio di START OFF");
						startOffHandling();
					}
					if(code == Parameters.ACK)
					{
						/************************** ACK ******************************/
						System.out.println("WNICManager: ricevuto messaggio di ACK");
						ackHandling();
					}
				}
			}
		}
		catch(WNICException e)
		{
			//TODO invia messaggio di errore
			System.out.println("WNICManager(1): "+e.getMessage());
			controller.killAll();
		}
		catch(IOException ee)
		{
			//TODO invia messaggio di errore
			System.out.println("WNICManager(2): "+ee.getMessage());
			controller.killAll();
		}
	}

	private void handoffHandling()
	{
		//gestione dell'handoff
		if(futureAP!=null)
		{
			try
			{
				if(!(connectToAccessPoint(futureAP)))
				{
					System.out.println("WNICManager: impossibile connettersi all'AP predetto dall'handoff");
					//si connette all'acce point col segnale migliore
					connectToAccessPoint(getWNICAccessPointWithMaxRSSI());
				}
			}
			catch(InvalidAccessPoint eeee)
			{
				//TODO invia messaggio di errore
				System.out.println("WNICManager(3): "+eeee.getMessage());
				controller.killAll();
			}
			catch(WNICException ee)
			{
				//TODO invia messaggio di errore
				System.out.println("WNICManager(1): "+ee.getMessage());
				controller.killAll();
			}
		}
		resetHandoffFlag();
	}

	private void startOffHandling() throws IOException, WNICException
	{
		long duration=0;
		try
		{
			double durationD = 0;
			durationD = Double.parseDouble(ClientMessageReader.getFirstParam());
			duration=(long)durationD;
			System.out.println("Durata periodo off: "+duration+"ms");
		}
		catch(NumberFormatException e)
		{
			System.out.println("WNICManager:durata di off non valida");
			return;
		}
		if(duration <= (2*timeon))
		{
			//se la durata del periodo di off non è sufficientemente lunga
			//la scheda non viene spenta
			//invia l'ack
			System.out.println("Periodo di off insufficiente");
			controller.incSequenceNumber();
			connection.sendPacket(ClientMessageFactory.buildNoParamMessage(controller.getSequenceNumber(), proxyAddr, proxyPort,Parameters.ACK));
			this.resumeTasks();
			System.out.println("WNICMAnager: Inviato messaggio di acknowledge");		
			wakeupTime=0;
		}
		else
		{
			wakeupTime=System.currentTimeMillis()+duration-timeon;
			suspendTasks();
			if(setWNICOff())
			{
				//invia l'ack
				controller.incSequenceNumber();
				connection.sendPacket(ClientMessageFactory.buildNoParamMessage(controller.getSequenceNumber(), proxyAddr, proxyPort,Parameters.ACK));
				System.out.println("WNICManager: inviato messaggio di acknowledge");
			}
			else
			{
				//TODO invia messaggio di errore
				System.out.println("WNICManager: impossibile spegnere la wnic");
				controller.killAll();
				return;
			}
			
			boolean interrupted=false;
			do
			{
				try
				{		
					if(!isHandoffApproacing())
					{			
						System.out.println("WNICManager: sospeso in attesa della fine dell'off");
						synchronized(reqMutex)
							{reqMutex.wait(duration-timeon);}
						interrupted=false;
					}
				}
				catch(InterruptedException e)
				{
					System.out.println("###################### 2 ########################");
					if(requests.size()>0)
					{
						try
						{
							String currRequest=getFirstRequest();
							ClientMessageReader.readContent(currRequest);
						}
						catch(Exception ex)
						{
							System.out.println("WNICManager:Impossibile leggere il contenuto della richiesta");
						}
						int code2 = ClientMessageReader.getCode();
						//se è un messaggio di ack lo gestisce altrimenti se è di strart off lo scarta
						if(code2 == Parameters.ACK)
							ackHandling();
						long gap=wakeupTime-System.currentTimeMillis();
						if(gap>0)
						{
							interrupted=true;
							duration=gap+timeon;
						}
					}
					else
						interrupted=false;//è stato interrotto dall'handoff manager
				}
			}while(interrupted);
			System.out.println("###################### 3 ########################");
			resumeTasks();
		}
		setWNICOn();
		if(isHandoffApproacing())
			handoffHandling();
		wakeupTime=0;
		controller.incSequenceNumber();
		connection.sendPacket(ClientMessageFactory.buildStartOnMessage(controller.getSequenceNumber(), proxyAddr, proxyPort, buffer.getSequenceNumber()));
		ackTimer();
		resumeTasks();

	}
	
	private void ackHandling()
	{
		pendingAck=false;
	}
	
	private void suspendTasks()
	{
		buffer.stopTransmission();
	}
	
	private void resumeTasks()
	{
		//risvegliare i thread per il controllo del buffer e dello throughput
		buffer.resumeTransmission();
		try {
			synchronized(suspensionObject){
				
				suspensionObject.notifyAll();
				System.out.println("Thread di controllo lato client risvegliati");
			}
		} catch (Exception e) 
		{e.printStackTrace();}
	}

	private void ackTimer()
	{
		pendingAck=true;
		//timeout per l'ack
		t.schedule(new TimerTask() 
		{
			public void run() 
			{
				if(pendingAck)
				{
					//TODO invia messaggio di errore
					System.out.println("WNICManager: non si e' ricevuto l'ack del messaggio StartOn");
					//controller.killAll();
				}
			}
		}, ACK_WAITING_TIME);
	}


	public void addRequest(DatagramPacket message) {
		// TODO Auto-generated method stub
		
	}
}

class CheckConnection extends Thread{
	
	private WNICController wn;
	public CheckConnection(WNICController wn)
	{
		this.wn=wn;
	}
	
	public void run()
	{
		try
		{
			if(wn!=null)
			{
				while(true)
				{
					Thread.sleep(5000);
					if(!wn.isConnected())
					{
						Thread.sleep(HandoffManager.handoffDuration);
						if(!wn.isConnected())
						{
							AccessPointData ap= wn.getAccessPointWithMaxRSSI();
							if(ap!=null)
								wn.connectToAccessPoint(ap);
						}
					}
				}
			}
		}
		catch(Exception e)
		{}
	}
}
