package muse.client;

import hardware.wnic.AccessPointData;
import hardware.wnic.exception.InvalidAccessPoint;
import hardware.wnic.exception.InvalidParameter;
import hardware.wnic.exception.WNICException;
import java.net.DatagramPacket;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import parameters.Parameters;
import util.Logger;
import muse.client.handoff.HandoffPredictor;
import muse.client.handoff.HandoffProbability;

/**
 * HandoffManager.java
 * Thread che effettua il monitoraggio degli RSSI e la previsione dell'handoff.
 *  In caso di handoff se e' necessario anticipare il periodo di on risveglia anticipatamente WNICManager.
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */

public class HandoffManager implements ClientTask
{
	private boolean ending;
	private WNICManager wm;
	private int positivePrediction;
	private AccessPointData currAP;
	private Vector<AccessPointData> temp;
	private Vector<AccessPointData> currAPlist;
	private HandoffPredictor predictor;
	private ClientBufferDataPlaying buffer;
	private ClientController controller;
	private long lastExecution=-1;
	/**
	 * Durata dell'handoff nel caso peggiore
	 */
	public static final long handoffDuration=2500;
	public Object waitingObj= new Object();

	
	public HandoffManager(WNICManager wm, ClientBufferDataPlaying buffer, ClientController controller) throws muse.client.exception.InvalidParameter
	{
		if(wm==null)
			throw new muse.client.exception.InvalidParameter("ERRORE: WNICManager non valido");
		if(buffer==null)
			throw new muse.client.exception.InvalidParameter("ERRORE: Controller del buffer non valido");
		if(controller==null)
			throw new muse.client.exception.InvalidParameter("ERRORE: ClientController non valido");
		this.controller=controller;
		this.buffer=buffer;
		this.wm=wm;
		ending=false;
		currAP=null;
		temp=null;
		currAPlist=new Vector<AccessPointData>();
		positivePrediction=0;
		predictor=null;
	}
	
	public synchronized void addRequest(DatagramPacket message) 
	{}

	public synchronized void endAll() 
	{
		synchronized(waitingObj)
		{
			ending=true;
			waitingObj.notify();
		}
	}
	
	private synchronized boolean isEnding()
	{
		return ending;
	}
	
	public void run() 
	{
		Timer t=new Timer();
		t.schedule(new TimerTask() 
		{
			public void run() 
			{
				mainTask();
			}
		}, Parameters.handOffMonitoringTime, Parameters.handOffMonitoringTime);				

		synchronized(waitingObj)
		{
			while(!ending)
			{
				try
				{waitingObj.wait();}
				catch(InterruptedException e)
				{}
			}
		}
		t.cancel();	
	}
	
	
	private void mainTask()
	{
		try
		{
			//se sfora i Parameters.handOffMonitoringTime dall'esecuzione precedente azzera gli RSSI rilevati
			if(lastExecution==-1)
				lastExecution=System.currentTimeMillis();
			else
			{
				if((System.currentTimeMillis()-lastExecution)>(Parameters.handOffMonitoringTime+200))
				{
					lastExecution=-1;
					positivePrediction=0;
					currAPlist.clear();
				}
				else
					lastExecution=System.currentTimeMillis();
			}
			System.out.println("+++++++++++++++ 1. CICLO HANDOFF +++++++++++++");
			/*************************************************************************
			 * TODO togliere se si vuole leggere gli RSSI anche durante i periodi di on
			 */
			//aspetta finche' non inizia il periodo di off
	//		while(wm.isWNICActive())
	//		{
	//			if(currAPlist.size()>0)
	//				currAPlist.clear();
	//			try
	//			{Thread.sleep(1000);}
	//			catch(InterruptedException e)
	//			{}
	//		}
			/***************************************************************************/
			
			/*************** lettura dei dati necessari dalla scheda *******************/
			//accende la scheda per rilevare gli RSSI correnti
			int handoffSwitches=0;
			if(!wm.isWNICActive())
			{
				wm.setWNICOn();
				handoffSwitches++;
			}
			
			currAP=wm.getWNICAssociatedAccessPoint();
			if(currAP!=null)
			{
				temp=wm.getWNICVisibleAccessPoints();
				double bitrate=0.0;
				long tp=0;
				if(temp.size()>1)
				{
					//tempo per il calcolo della bitrate
					tp=-System.currentTimeMillis();
					bitrate=controller.getBitrate();
					tp+=System.currentTimeMillis();
				}

				if((wm.getNumberOfSwitches()-handoffSwitches)%2==1)
					wm.setWNICOff();
				updateList();
				
				/************************ previsione degli RSSI al tempo now+Parameters.preditioTimeSec() ******************/
				System.out.println("+++++++++++++++ 2. num AP="+currAPlist.size()+" currAP="+currAP.getAccessPointName()+" bitrate="+bitrate+"++++++++++++");
				if((currAPlist.size()>1) && (bitrate>0.0))
				{
					predictor= new HandoffPredictor(currAPlist, currAP, "Grey",Parameters.handOffMonitoringTime);
					int ptime=Parameters.preditioTimeSec-(int)((positivePrediction%Parameters.numberOfPrediction)*Parameters.handOffMonitoringTime/1000);
					if(predictor.predictHandoffProbability((ptime>0)?ptime:Parameters.preditioTimeSec)==HandoffProbability.HIGH_PROBABILITY)
					{
						//alta probabilita' di handoff
						positivePrediction++;
						System.out.println("++++++++++++ 3. PREDIZIONE No"+positivePrediction+" DI ALTA PROBABILITA' HANDOFF++++++++++++++++++");
						//se si hanno Parameters.numberOfPrediction predizioni di alta probabilita' si procede alla gestione dell'handoff
						if(positivePrediction>=Parameters.numberOfPrediction)
						{
							/************************* calcolo dell'istante di handoff e sua gestione **********************/
							handoffHandling(bitrate,Parameters.SECURITY_THRESHOLD, tp, predictor);
						}
					}
					else
					{
						//bassa probabilita' di handoff
						positivePrediction=0;
						System.out.println("+++++++++++ 3. PREDIZIONE DI BASSA PROBABILITA' DI HANDOFF+++++++++++++");
					}
				}
				else
					positivePrediction=0;
			}
			else
			{
				if((wm.getNumberOfSwitches()-handoffSwitches)%2==1)
					wm.setWNICOff();
			}
		}
	
		catch(WNICException ee)
		{
			Logger.write("HandoffManager: "+ee.getMessage());
			ending=true;
			controller.killAll();
		}
		catch(InvalidAccessPoint eee)
		{
			Logger.write("HandoffManager: "+eee.getMessage());
			ending=true;
			controller.killAll();
		}
		catch(InvalidParameter eeee)
		{
			Logger.write("HandoffManager: "+eeee.getMessage());
			ending=true;
			controller.killAll();
		}
		catch(muse.client.handoff.exception.InvalidParameter eeeee)
		{
			Logger.write("HandoffManager: "+eeeee.getMessage());
			ending=true;
			controller.killAll();
		}

	}
	
	
	private void updateList() throws InvalidParameter
	{
		if(temp==null)
		{
			currAP=null;
			currAPlist.clear();
			return;
		}
		if(currAPlist.size()==0)
		{
			currAPlist=temp;
			return;
		}

		//elimino gli access point che non sono stati rilevati in questa scansione
		//a quelli gia' presenti aggiungo il nuovo RSSI
		//e i nuovi accesspoint vengono aggiunti alla lista
		Iterator<AccessPointData> currAPlistIt= currAPlist.iterator();
		Vector<AccessPointData> newAPlist=new Vector<AccessPointData>();
		while(currAPlistIt.hasNext())
		{
			AccessPointData ap=currAPlistIt.next();
			Iterator<AccessPointData> tempIt= temp.iterator();
			while(tempIt.hasNext())
			{
				AccessPointData tp=tempIt.next();
				if(tp.getAccessPointMAC().equals(ap.getAccessPointMAC()))
				{
					ap.newSignalStrenghtValue(tp.getSignalStrenght());
					newAPlist.add(ap);
					tempIt.remove();
				}
			}
		}			
		currAPlist=newAPlist;

		for(int i=0;i<temp.size();i++)
			currAPlist.add(temp.elementAt(i));
	}
	
	private void handoffHandling(double bandwidth, int lowWaterFrame,long tp, HandoffPredictor predictor) throws muse.client.handoff.exception.InvalidParameter
	{
		if(bandwidth<0)
			throw new muse.client.handoff.exception.InvalidParameter("ERRORE: bandwidth non positiva");
		if(bandwidth==0)
			return;
		bandwidth=bandwidth*1024*1024;
		if((lowWaterFrame<0) || (lowWaterFrame>Parameters.CLIENT_BUFFER))
			throw new muse.client.handoff.exception.InvalidParameter("ERRORE: livello di low water non valido");
		if(tp<0)
			throw new muse.client.handoff.exception.InvalidParameter("ERRORE: tempo di probing negativo");
		System.out.println("+++++++++++++ tempo predizione banda="+tp+" +++++++++++");
		/******************************************* predizione istante handoff (in ms a partire da ora) ********************/
		long wakeUpTimestamp=wm.getWakeUpTime();
		int handOffStart=predictor.getPredictedHandoffStartTime()*1000;
		if(handOffStart==-1000)
			return;
		System.out.println("HandoffManager: previsto handoff fra "+handOffStart+"ms, al timestamp "+(System.currentTimeMillis()+handOffStart));
		/******************************************* gestione handoff ******************************************************/
		try
		{
			//se la scheda e' in un periodo di on (wakeUpTimestamp==0)si attende che inizi l'off per gestire l'handoff
			//if(wakeUpTimestamp>0)
			if(positivePrediction>(Parameters.numberOfPrediction*2))
				positivePrediction=0;
			if(!wm.isWNICActive())
			{
				//la scheda e' in un periodo di off
				try
				{
					if((wakeUpTimestamp-System.currentTimeMillis()-handOffStart-handoffDuration)>Parameters.handoff_security_thrs)
					{
						//l'handOff ricade nel corrente periodo di sleep
						System.out.println("HandoffManager: handoff previsto nel corrente periodo di sleep");
						try
						{
							wm.setFutureAccessPoint(predictor.getPredictedFutureAccessPoint());
							Thread.sleep(handOffStart-WNICManager.timeon);
							wm.setHandoffFlag();
						}
						catch(InterruptedException e){}
					}
					else
						if((handOffStart-(wakeUpTimestamp-System.currentTimeMillis())-(tp+(Parameters.PACKET_SIZE*(Parameters.CLIENT_BUFFER-lowWaterFrame)*8*1024*1024/bandwidth)))>Parameters.handoff_security_thrs)
						{
							//l'handOff ricade nel successivo periodo di sleep
							System.out.println("HandoffManager: handoff previsto nel successivo periodo di sleep");
//							try
//							{
//								//TODO rimandare la decisione, non sospendere ne' azzerare positivePredition
//								wm.setFutureAccessPoint(predictor.getPredictedFutureAccessPoint());
//								Thread.sleep(handOffStart-WNICManager.timeon);
//								wm.setHandoffFlag();
//							}
//							catch(InterruptedException e){}
							positivePrediction=Parameters.numberOfPrediction;
						}
						else
						{
							//a 128kb/s un frame ogni 77msec
							//il buffer deve essere pieno al tempo (handoffTimestamp-security_thrs)
							wm.setFutureAccessPoint(predictor.getPredictedFutureAccessPoint());
							System.out.println("HandoffManager: handoff previsto in un periodo di on, si anticipera' tale periodo");
							try
							{
								//Thread.sleep(wakeUpTimestamp-System.currentTimeMillis()-WNICManager.timeon-(long)((buffer.getBufferSize()-buffer.getFrameOnBuffer()+(wm.getWakeUpTime()-Parameters.handoff_security_thrs-System.currentTimeMillis())/77)*Parameters.PACKET_SIZE*8*1000/bandwidth));
								long sleepT=handOffStart-WNICManager.timeon-Parameters.handoff_security_thrs-(long)((Parameters.CLIENT_BUFFER-((handOffStart-WNICManager.timeon-Parameters.handoff_security_thrs)/77)-buffer.getFrameOnBuffer())*Parameters.PACKET_SIZE*8*1024*1024/bandwidth);
								if(sleepT>0)
									Thread.sleep(sleepT);
								else
									System.out.println("HandoffManager: non è possibile anticipare il periodo di on ed evitare perdita di dati");
							}
							catch(InterruptedException e)
							{
								System.out.println("ERRORE: sleep interrotto");
							}
							wm.setHandoffFlag();
						}
				}
				catch(InvalidAccessPoint ee)
				{
					System.out.println("ERRORE: futuro access point non valido");
					ending=true;
					controller.killAll();
				}
			}
		}
		catch (WNICException e)
		{
			System.out.println("ERRORE: impossibile conoscere lo stato della scheda");
			ending=true;
			controller.killAll();
		}
	}
}
