package test;

import hardware.wnic.*;

import java.io.FileWriter;
import java.util.Vector;
/**
 * Test del package hardware.wnic
 * @author Zapparoli Pamela
 * @version 0.1
 *
 */

public class WNICTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try
		{
			WNICController w=WNICFinder.getCurrentWNIC();
			if(w.isActive())
				System.out.println("stato acceso");
			else
				System.out.println("stato spento");
			System.out.println("num AP:"+w.getVisibleAccessPoints().size());
			/********************/
			
			if(w.isConnected())
				System.out.println("connesso");
			else
				System.out.println("non connesso");
			/********************/
			
			if(w.getAssociatedIP()!=null)
				System.out.println("IP: "+w.getAssociatedIP().getHostAddress());
			/********************/
			FileWriter fw=new FileWriter("YoriRSSI.txt");
			while(true)
			{
				Thread.sleep(1000);
				Vector<AccessPointData> v=null;
				try
				{
					v=w.getVisibleAccessPoints();
				}
				catch(Exception ee)
				{
					fw.write("0\n");
					continue;
				}
				if(v==null)
				{
					fw.write("0\n");
					continue;
				}	
				if(v.size()==0)
					fw.write("0");
				else
					fw.write(v.size()+", ");
				for(int i=0;i<v.size();i++)
				{
					System.out.println("AP: "+v.elementAt(i).getAccessPointName()+" mac: "+v.elementAt(i).getAccessPointMAC()+" RSSI: "+v.elementAt(i).getSignalStrenght());
					fw.write(v.elementAt(i).getAccessPointName()+" "+v.elementAt(i).getAccessPointMAC()+" "+(v.elementAt(i).getSignalStrenght()-100));
					if(i<(v.size()-1))
						fw.write(", ");
				}
				fw.write("\n");
				fw.flush();
			}
			
			
			
//			Vector<AccessPointData> v=w.getVisibleAccessPoints();
//			if(v.size()>0)
//				w.connectToAccessPoint(v.elementAt(0));
//			if(w.isConnected())
//				System.out.println("connesso");
//			else
//				System.out.println("non connesso");
			
//			
//			w.setOff();
//			if(w.isActive())
//				System.out.println("stato acceso");
//			else
//				System.out.println("stato spento");
//			
//			
//			w.setOn();
//			if(w.isActive())
//				System.out.println("stato acceso");
//			else
//				System.out.println("stato spento");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

}
