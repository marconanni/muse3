package hardware.wnic;

import hardware.wnic.exception.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.StringTokenizer;
import java.util.Vector;
import parameters.Parameters;

/**
 * DummyController.java
 * Classe che simula il controllore di una generica scheda wireless.
 * <br/> Necessita di un file rssi.txt nella cartella test con i finti access point visibili nel formato:
 * <br/> numero_access_points,APname APmac_address RSSIsignal, AP2name AP2mac_address RSSI2signal, ecc
 * @author Zapparoli Pamela
 * @version 1.0.1
 *
 */

public class DummyController implements WNICController
{
	private String id;
	private long lastOffDuration;
	private long lastOnDuration;
	private long currentStateStartTime;
	private AccessPointData currentAP;
	private Vector<AccessPointData> APlist;
	private boolean isOn;
	private int numberOfPreviousRSSI;
	private BufferedReader br;
	private long lastUpdate;
	private int readn=0;

	public DummyController(int previous)throws InvalidParameter, WNICException
	{
		if(previous<=0)
			throw new InvalidParameter("ERRORE: numero di precedenti RSSI da memorizzare non positivo");
		id="dummy01";
		currentStateStartTime=System.currentTimeMillis();
		lastOffDuration=-1;
		lastOnDuration=-1;
		lastUpdate=-1;
		isOn=true;
		br=null;
		currentAP=null;
		numberOfPreviousRSSI=previous;
		APlist= new Vector<AccessPointData>();
		updateAccessPoints();
	}


	public AccessPointData getAccessPointWithMaxRSSI() throws WNICException,InvalidAccessPoint 
	{
		updateAccessPoints();
		if(APlist==null)
			return null;
		if(APlist.size()==0)
			return null;
		int index=0;
		int max=APlist.elementAt(0).getSignalStrenght();
		for(int i=1;i<APlist.size();i++)
		{
			if(APlist.elementAt(i).getSignalStrenght()>max)
			{
				max=APlist.elementAt(i).getSignalStrenght();
				index=i;
			}
		}
		return APlist.elementAt(index);
	}

	public AccessPointData getAssociatedAccessPoint() throws WNICException,InvalidAccessPoint 
	{
		updateAccessPoints();	
		return currentAP;
	}

	public int getSignalLevel() throws WNICException, InvalidAccessPoint 
	{
		updateAccessPoints();

		if(currentAP==null)
			return 0;
		else
			return currentAP.getSignalStrenght();
	}

	public Vector<AccessPointData> getVisibleAccessPoints() throws WNICException, InvalidAccessPoint 
	{
		updateAccessPoints();
		return  (Vector<AccessPointData>)APlist.clone();
	}
	
	public boolean connectToAccessPoint(AccessPointData ap)throws WNICException, InvalidAccessPoint 
	{
		if(ap==null)
			throw new InvalidAccessPoint("ERRORE: l'access point specificato non e' valido");
		updateAccessPoints();
		
		for(int i=0;i<APlist.size();i++)
		{
			if(APlist.elementAt(i).getAccessPointMAC().equals(ap.getAccessPointMAC()))
			{
				currentAP=APlist.elementAt(i);
				return true;
			}
		}
		return false;
	}
	
	public long getActiveTime() throws WNICException 
	{
		updateAccessPoints();

		if(isActive())
			return System.currentTimeMillis()-currentStateStartTime;
		else
			return lastOnDuration;
	}
	
	public long getInactiveTime() throws WNICException 
	{
		updateAccessPoints();

		if(isActive())
			return lastOffDuration;
		else
			return System.currentTimeMillis()-currentStateStartTime;
	}
	
	public InetAddress getAssociatedIP() throws WNICException 
	{
		updateAccessPoints();

		try
		{
			return InetAddress.getByName(Parameters.CLIENT_ADDRESS);
		}
		catch(Exception e)
		{
			throw new WNICException("ERRORE: impossibile ottenere l'indirizzo IP del localhost");
		}
		
	}

	public boolean isActive() throws WNICException 
	{
		return isOn;
	}

	public boolean isConnected() throws WNICException 
	{
		updateAccessPoints();

		if(currentAP!=null)
			return true;
		else
			return false;
	}

	public boolean setOff() 
	{
		try
		{
			updateAccessPoints();
		}
		catch (Exception e)
		{return false;}
		if(isOn)
		{
			long now=System.currentTimeMillis();
			lastOnDuration=now-currentStateStartTime;
			currentStateStartTime=now;
			isOn=false;
		}
		System.out.println("**Scheda off**");
		return true;
	}

	public boolean setOn() 
	{
		try
		{
			updateAccessPoints();
		}
		catch (Exception e)
		{return false;}
		if(!isOn)
		{
			long now=System.currentTimeMillis();
			lastOffDuration=now-currentStateStartTime;
			currentStateStartTime=now;
			isOn=true;
		}
		System.out.println("**Scheda on**");
		return true;
	}

	public long getCurrentStateStartTime() 
	{
		try
		{
			updateAccessPoints();
		}
		catch (Exception e)
		{}
		return currentStateStartTime;
	}
	
	private int convertRSSI(int dBm)
	{
		/*
		 * Converte i dBm (valore pi� piccolo -> segnale pi� alto)
		 * in RSSI in un range fra 0 e 120 con saturazione 
		 * per i valori che superano quella soglia
		 */
		if(dBm<-100)
			return 0;
		if(dBm>20)
			return 120;
		return dBm+100;
	}
	
	private void updateAccessPoints() throws WNICException
	{
		int lineToRead;
		if(lastUpdate==-1)
			lineToRead=1;
		else
		{
			long gap=System.currentTimeMillis()-lastUpdate;
			if(gap>=Parameters.updateTime)
				lineToRead=(int)(gap/Parameters.updateTime);
			else
				lineToRead=0;
		}
		if(lineToRead>0)
		{		
			lastUpdate=System.currentTimeMillis();
			try
			{
				boolean found=false;
				if(br==null)
				{   
			        br=new BufferedReader(new FileReader("rssi.txt"));
					//br=new BufferedReader(new FileReader("./rssi.txt"));
					//br=new BufferedReader(new FileReader("media/DATA/Development/MUSE/amontecchiaSistemato/test/rssi.txt"));
			        System.out.println("TIMESTAMP PRIMA LETTURA RSSI: "+lastUpdate);
				}
				while(lineToRead>0)
				{
					found=false;
					APlist.clear();
					String s=null;
					do
					{
						s=br.readLine();						
						if(s==null)
						{
							currentAP=null;
							APlist.clear();
							return;
						}
					}while(s.trim().equals(""));
					
					readn++;
					System.out.println("LETTURA RSSI NUMERO "+readn);
					StringTokenizer token= new StringTokenizer(s," ,");
					int numAP=Integer.parseInt(token.nextToken());
					if(numAP<0)
						throw new WNICException("ERRORE1: file rssi.txt formato non valido");
					if(numAP==0)
					{
						currentAP=null;
						APlist.clear();
					}
					for(int i=0;i<numAP;i++)
					{
						String name=token.nextToken().trim();
						String mac=token.nextToken().trim();
						int sig=Integer.parseInt(token.nextToken());
						APlist.add(new AccessPointData(name, mac, convertRSSI(sig), numberOfPreviousRSSI));	
						if(currentAP!=null)
						{
							if(currentAP.getAccessPointMAC().equals(mac))
								found=true;
						}
					}
					lineToRead--;
				}
				if((currentAP==null) || (!found))
				{
					if(APlist.size()>0)
						currentAP=getAccessPointWithMaxRSSI();
					else
						currentAP=null;
				}
			}	
			catch(FileNotFoundException ee)
			{
				throw new WNICException("ERRORE: file rssi.txt non trovato");
			}
			catch(Exception e)
	        {
				e.printStackTrace();
				throw new WNICException("ERRORE2: file rssi.txt formato non valido"+e.getMessage());
	        }
		}
	}
}
