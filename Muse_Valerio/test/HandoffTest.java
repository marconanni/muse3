package test;

import hardware.wnic.AccessPointData;
import hardware.wnic.WNICController;
import hardware.wnic.WNICFinder;
import hardware.wnic.exception.InvalidParameter;

import java.util.Iterator;
import java.util.Vector;

import muse.client.handoff.HandoffPredictor;
import muse.client.handoff.HandoffProbability;
import parameters.Parameters;

public class HandoffTest {
	
	private static Vector<AccessPointData> currAPlist= new Vector<AccessPointData>();
	private static AccessPointData currAP=null;
	private static Vector<AccessPointData> temp=null;

		public static void main(String[] args) 
		{
			try
			{
				WNICController w=WNICFinder.getCurrentWNIC();
				
				w.setOn();
				
				int positivePrediction=0;
				HandoffPredictor predictor;
				
				for(int j=0;j<140;j++)
				{
					System.out.println("************************** CICLO "+(j+1)+"***********************");
					long elabTime=-System.currentTimeMillis();
					currAP=w.getAssociatedAccessPoint();
					temp=w.getVisibleAccessPoints();
					if(currAP!=null)
						System.out.println("current ap "+currAP.getAccessPointName());
					for(int i=0;i<temp.size();i++)
						System.out.println("AP: "+temp.elementAt(i).getAccessPointName()+" mac: "+temp.elementAt(i).getAccessPointMAC()+" RSSI: "+temp.elementAt(i).getSignalStrenght());
					updateList();
					if(currAPlist.size()>1)
					{
						predictor= new HandoffPredictor(currAPlist, currAP, "Grey",Parameters.handOffMonitoringTime);
						if(predictor.predictHandoffProbability(Parameters.preditioTimeSec-(int)((positivePrediction%Parameters.numberOfPrediction)*Parameters.handOffMonitoringTime/1000))==HandoffProbability.HIGH_PROBABILITY)
						{
							positivePrediction++;
							System.out.println("PREDIZIONE No"+positivePrediction+" DI ALTA PROBABILITA' HANDOFF");
							if(positivePrediction>=Parameters.numberOfPrediction)
							{
								int handOffStart=predictor.getPredictedHandoffStartTime()*1000;
								System.out.println("HandoffManager: previsto handoff fra "+handOffStart+"ms, al timestamp "+(System.currentTimeMillis()+handOffStart));
								//attende l'handoff e si riconnette al nuovo access point
								if(handOffStart>0)
								{
									Thread.sleep(handOffStart);
									w.connectToAccessPoint(predictor.getPredictedFutureAccessPoint());
									positivePrediction=0;
								}
							}
						}
						else
							positivePrediction=0;
					}
					else
						positivePrediction=0;
					elabTime+=System.currentTimeMillis();
					if((Parameters.handOffMonitoringTime-elabTime)>0)
					 Thread.sleep(Parameters.handOffMonitoringTime-elabTime);
					else
						currAPlist.clear();
				}	
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
			}

		}
		

		private static void updateList() throws InvalidParameter
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


	}
