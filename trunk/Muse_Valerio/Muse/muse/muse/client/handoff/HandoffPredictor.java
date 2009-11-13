
package muse.client.handoff;

import java.util.Vector;

import parameters.Parameters;

import muse.client.handoff.exception.InvalidParameter;

import hardware.wnic.AccessPointData;

/**
 * HandoffPredictor.java
 * Classe usata per predire la probabilita' di un handoff e il suo istante di inizio.
 *  Sfrutta il modello di Grey.
 * @author Zapparoli Pamela
 * @version 1.0
 *
 */

public class HandoffPredictor 
{
	
	private Vector<AccessPointData> currentAPlist;
	private AccessPointData currentAP;
	private RSSIFilter currAPfilter;
	private AccessPointData futureAP;
	private RSSIFilter futureAPfilter;
	private double[] prevision;
	private String model;
	private int time;
	private long samplingTime;
	
	/**
	 * Crea un oggetto HandoffPredictor
	 * @param list lista degli access point visibili
	 * @param currAP access point corrente
	 * @param model tipo di predittore
	 * @param samplingTime indica a che distanza di tempo in ms sono stati campionati gli RSSI
	 * @throws InvalidParameter
	 */
	public HandoffPredictor(Vector<AccessPointData> list, AccessPointData currAP, String model, long samplingTime) throws InvalidParameter
	{
		if(list==null)
			throw new InvalidParameter("ERRORE: elenco degli access point non valido");
		if(list.size()<2)
			throw new InvalidParameter("ERRORE: devono essere presenti almeno due access point per effettuare la predizione");
		if(currAP==null)
			throw new InvalidParameter("ERRORE: access point corrente non valido");
		if(samplingTime<=0)
			throw new InvalidParameter("ERRORE: tempo di campionamento negativo o nullo");
		currentAPlist=list;
		currentAP=currAP;
		this.model=model;
		this.time=-1;
		this.samplingTime=samplingTime;
		futureAP=null;
	}
	
	public HandoffProbability predictHandoffProbability(int time) throws InvalidParameter
	{
		if(time<=0)
			throw new InvalidParameter("ERRORE: istante di previsione negativo o nullo");
		this.time=time;

		prevision=new double[currentAPlist.size()];
		//AP con RSSI previsto massimo
		double max=-1;
		int indexMax=-1;
		//AP corrente
		int currAPindex=-1;
		RSSIFilter filter;
		for(int i=0;i<currentAPlist.size();i++)
		{
			filter=new GreyModel(currentAPlist.elementAt(i).getLastSignalStrenghtValues());
			/************* predizione RSSI all'istante now+time *************************************/
			prevision[i]=filter.predictRSSI(time, samplingTime);
			System.out.println("+++++++++++++ AP"+(i+1)+" RSSI previsto fra "+time+"sec e':"+prevision[i]);
			if(currentAPlist.elementAt(i).getAccessPointMAC().equals(currentAP.getAccessPointMAC()))
			{
				currAPindex=i;
				currAPfilter=filter;
			}
			else
				if(prevision[i]>max)
				{
					//il max fra gli altri access point
					max=prevision[i];
					indexMax=i;
					futureAPfilter=filter;
				}
		}
		if((currAPindex>=0) && (indexMax>=0))
		{
			if((prevision[currAPindex]-prevision[indexMax])<=Parameters.HPT)
			{
				futureAP=currentAPlist.elementAt(indexMax);
				return HandoffProbability.HIGH_PROBABILITY;
			}
			else
				return HandoffProbability.LOW_PROBABILITY;
		}
		else
			return HandoffProbability.LOW_PROBABILITY;
		
	}
	
	/**
	 * Restituisce fra quanti secondi si prevede un handoff
	 * @return un int che indica fra quanti secondi si prevede un handoff
	 */
	public int getPredictedHandoffStartTime()
	{
		try
		{
			boolean found=false;
			int predTime=this.time+(int)Parameters.handOffMonitoringTime/1000;
			int times=0;
			while(!found)
			{
				//TODO fare eventualmente ricerca binaria
				double gap=futureAPfilter.predictRSSI(predTime, samplingTime)- currAPfilter.predictRSSI(predTime, samplingTime);
				//System.out.println("GAP="+gap);
				if(gap>=Parameters.HHT)
					found=true;
				else
					predTime+=Parameters.handOffMonitoringTime/1000;
				times++;
				//se l'handoff non e' stato trovato nei prossimi 20 secondi si annulla la segnalazione
				if(times>20)
					return -1;
			}
			if(found)
				return predTime;
			else
				return -1;
		}
		catch(Exception e)
		{
			return -1;
		}
	}
	
	public AccessPointData getPredictedFutureAccessPoint()
	{
		return futureAP;
	}
}
