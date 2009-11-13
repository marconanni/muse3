package hardware.wnic;

import hardware.wnic.exception.InvalidAccessPoint;
import hardware.wnic.exception.InvalidParameter;
import hardware.wnic.exception.WNICException;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
/**
 * WNICMacOsController.java
 * Questa classe rappresenta il controller della scheda wireless Airport Express
 *  presente in boundle in ogni Macintosh.
 * 
 * @author Zapparoli Pamela
 * @version 1.0
 */
public class WNICMacOsController implements WNICController
{
	private String id;
	private long lastOffDuration;
	private long lastOnDuration;
	private long currentStateStartTime;
	private AccessPointData currentAP;
	private boolean isOn;
	private int numberOfPreviousRSSI;

	public WNICMacOsController(int previous) throws WNICException, InvalidParameter
	{
		if(previous<=0)
			throw new InvalidParameter("ERRORE: numero di precedenti RSSI da memorizzare non positivo");
		//TODO controlla se airport e' presente
		
		//TODO crea le due location
		
		if (!isActive())
			throw new WNICException("ERRORE: la scheda wireless airport deve essere accesa per procedere");	
		if(isConnected())
		{
			try
			{
				currentAP=getAssociatedAccessPoint();
			}
			catch(InvalidAccessPoint e)
			{
				currentAP=null;
			}
		}
		else
			currentAP=null;	
		//se si usa airport il suo ID e' en1
		id="en1";
		currentStateStartTime=System.currentTimeMillis();
		lastOffDuration=-1;
		lastOnDuration=-1;
		isOn=true;
		numberOfPreviousRSSI=previous;
	}
	

	public boolean isActive() throws WNICException
	{
		String s=null;
		try
		{
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			s= out.readLine();
		}
		catch (Exception e)
		{
			throw new WNICException("ERRORE1: impossibile verificare lo stato della scheda wireless");
		}
		if(s==null)
			throw new WNICException("ERRORE2: impossibile verificare lo stato della scheda wireless");
		if((s.contains("AirPort")) && (s.contains("Off")))
		{
			isOn=false;
		}
		else
		{
			isOn=true;
			try
			{
				Process p= Runtime.getRuntime().exec("scselect");
				p.waitFor();
				BufferedReader out = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String d= out.readLine();
				while(!d.startsWith("*"))
				{
					d=out.readLine();
					d=d.trim();
				}
				if(d.contains("AirportOff"))
					isOn=false;
			}
			catch (Exception e)
			{
				throw new WNICException("ERRORE3: impossibile verificare lo stato della scheda wireless");
			}
		}
		return isOn;
	}
	
	public boolean setOn()
	{
		try
		{
			if(!isOn)
			{
				// esegue il comando e aspetta che termini l'esecuzione
				Process p= Runtime.getRuntime().exec("/usr/sbin/scselect AirportOn");
				//Process p= Runtime.getRuntime().exec("ifconfig  "+id+" up");
				p.waitFor();
				//verifica se si  davvero accesa
				if(!isActive())
					throw new WNICException("ERRORE: impossibile accendere la scheda wireless");
				long now=System.currentTimeMillis();
				lastOffDuration=now-currentStateStartTime;
				currentStateStartTime=now;
				isOn=true;
			}
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	

	public boolean setOff() 
	{
		try
		{
			if(isOn)
			{
				// esegue il comando e aspetta che termini l'esecuzione
				Process p= Runtime.getRuntime().exec("/usr/sbin/scselect AirportOff");
				//Process p= Runtime.getRuntime().exec("ifconfig  "+id+" down");
				p.waitFor();
				//verifica se si e' davvero spenta
				if(isActive())
					throw new WNICException("ERRORE: impossibile spegnere la scheda wireless");
				long now=System.currentTimeMillis();
				lastOnDuration=now-currentStateStartTime;
				currentStateStartTime=now;
				isOn=false;
			}
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public long getActiveTime() throws WNICException
	{
		if(isActive())
			return System.currentTimeMillis()-currentStateStartTime;
		else
			return lastOnDuration;
	}
	
	
	public long getInactiveTime() throws WNICException
	{
		if(isActive())
			return lastOffDuration;
		else
			return System.currentTimeMillis()-currentStateStartTime;
	}
	

	public boolean isConnected() throws WNICException
	{
		if(!isActive())
			return false;
		try
		{
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s=null;
			for(int i=0;i<5;i++)
				s= out.readLine();
			if(s==null)
				throw new WNICException("ERRORE1: impossibile ottenere lo stato della scheda wireless");
			if((s.contains("linkStatus")) && ((s.contains("ESS")) || (s.contains("BSS"))))
				return true;
			else
				return false;			
		}
		catch (Exception e)
		{
			throw new WNICException("ERRORE2: impossibile ottenere lo stato della scheda wireless");
		}
	}
	
	
	public AccessPointData getAssociatedAccessPoint() throws WNICException, InvalidAccessPoint
	{
		if(isConnected())
			updateAP();
		return currentAP;
	}

	public InetAddress getAssociatedIP() 
	{
		try 
		{
			Enumeration<InetAddress> en = NetworkInterface.getByName(id).getInetAddresses();
			if(en==null)
				return null;
			while (en.hasMoreElements()) 
			{ 
				InetAddress a =en.nextElement();
				if (a instanceof Inet4Address)
					return a;
			}
		} 
		catch  (Exception e) 
		{}
		return null;
	}
	

	public int getSignalLevel() throws WNICException, InvalidAccessPoint
	{
		if(isConnected())
		{
			updateAP();
			return currentAP.getSignalStrenght();
		}
		else
			return 0;
	}
	

	public Vector<AccessPointData> getVisibleAccessPoints() throws WNICException,InvalidAccessPoint
	{
		try
		{
			if(!isOn)
				return null;
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -s");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s=null;
			s= out.readLine();
			if(s==null)
				return null;
			StringTokenizer token= new StringTokenizer(s, " ");
			int num=0;
			try
			{
				num=Integer.parseInt(token.nextToken());
				System.out.println("num "+num);
			}
			catch (NumberFormatException ee)
			{
				throw new WNICException("ERRORE: numero di access point visibili non valido");
			}
			out.readLine();
			Vector<AccessPointData> res= new Vector<AccessPointData>(num);
			for(int i=0;i<num;i++)
			{
				token=new StringTokenizer(out.readLine(), " \t");
				String name=token.nextToken();
				boolean isNumber=false;
				int sig=0;
				String mac=null;
				while(!isNumber)
				{
					try
					{
						sig=Integer.parseInt(token.nextToken());
						isNumber=true;
					}
					catch(NumberFormatException e)
					{isNumber=false;}
				}
				try
				{sig=Integer.parseInt(token.nextToken());}
				catch(NumberFormatException e)
				{
					throw new InvalidAccessPoint("ERRORE: valore di RSSI dell'access point non valido");
				}
				for(int j=0;j<4;j++)
					mac= token.nextToken();
				res.add(new AccessPointData(name, mac, convertRSSI(sig), numberOfPreviousRSSI));
			}
			return res;
		}
		catch (Exception e)
		{
			throw new WNICException("ERRORE: impossibile rilevare gli access point visibili "+e.getMessage());
		}
	}
	
	
	public boolean connectToAccessPoint(AccessPointData ap) throws WNICException, InvalidAccessPoint
	{
		if(ap==null)
			throw new InvalidAccessPoint("ERRORE: l'access point specificato non  valido");
		try
		{
			//se non riesce a connettersi in 2 secondi restituisce false
			long start=-1;
			Process p=null;
			boolean finished=false;
			BufferedReader out=null;
			do
			{
				if(start==-1)
				{
					start=System.currentTimeMillis();
					p= Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport --associate "+ap);
				}
				try
				{
					p.exitValue();
					finished=true;
				}
				catch(IllegalThreadStateException e)
				{finished=false;}
			}while(((System.currentTimeMillis()-start)<2500) && (!finished));
			if(!finished)
				return false;
			out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s=null;
			s= out.readLine();
			if(s==null)
				return false;
			if(s.trim().startsWith("Unable"))
				return false;
			return true;
		}
		catch (Exception e)
		{
			throw new WNICException("ERRORE: impossibile connettersi all'Access Point specificato");
		}
	}
	
	public AccessPointData getAccessPointWithMaxRSSI() throws WNICException,InvalidAccessPoint 
	{
		Vector<AccessPointData> a=getVisibleAccessPoints();
		if(a==null)
			return null;
		if(a.size()==0)
			return null;
		AccessPointData best=a.elementAt(0);
		for(int i=1; i<a.size();i++)
		{
			if(a.elementAt(i).getSignalStrenght()>best.getSignalStrenght())
				best=a.elementAt(i);
		}
		return best;
	}
	
	
	public long getCurrentStateStartTime() 
	{
		return currentStateStartTime;
	}
	
	
	private void updateAP()throws WNICException, InvalidAccessPoint	
	{
		String name, mac;
		int signal;
		try
		{
			// esegue il comando e aspetta che termini l'esecuzione
			Process p= Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I");
			p.waitFor();
			BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s=null;
			for(int i=0;i<3;i++)
				s= out.readLine();
			if(s==null)
				throw new WNICException("ERRORE: impossibile ottenere l'RSSI della scheda wireless");
			try
			{
				StringTokenizer token= new StringTokenizer(s, ":");
				token.nextToken();
				s=token.nextToken().trim();
				signal=Integer.parseInt(s);
			}
			catch(NumberFormatException ee)
			{
				throw new InvalidAccessPoint("ERRORE: RSSI non valido");
			}
			for(int i=0;i<7;i++)
				s= out.readLine();
			mac=s.substring(7).trim();
			name=out.readLine().substring(6).trim();
		}
		catch (Exception e)
		{
			throw new WNICException("ERRORE: impossibile ottenere l'Access Point a cui  connessa la scheda wireless");
		}
		try
		{
			currentAP=new AccessPointData(name, mac, convertRSSI(signal), numberOfPreviousRSSI);
		}
		catch(InvalidParameter e)
		{
			throw new InvalidAccessPoint(e.getMessage());
		}
	}


	private int convertRSSI(int dBm)
	{
		if(dBm>0)
			return dBm;
		if(dBm<-100)
			return 0;
//		if(dBm>20)
//			return 120;
		return dBm+100;
	}
}