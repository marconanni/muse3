package test;

import java.util.Vector;

import relay.wnic.AccessPointData;
import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.exception.WNICException;

class TestRelayWNICLinuxController{

	public static void main(String args[]){

		RelayAPWNICLinuxController rwlc = null;
		AccessPointData ap = null;

		try {

			rwlc = new RelayAPWNICLinuxController(6,"wlan0","lord");

			if(rwlc.isAssociated()&&rwlc.isOn())	{

				ap = rwlc.getAssociatedAccessPoint();

				printArray(ap.getLastSignalStrenghtValues());

				System.out.println(	"AP: Nome: "+ ap.getAccessPointName() + 
						" MAC: " + ap.getAccessPointMAC() + 
						" RSSI: " + ap.getSignalStrenght());
			}else{
				System.out.println("Nessun AP associato"); 

			}


			if(!rwlc.isAssociated())	{
				//printVectorAP(rwlc..getVisibleAccessPoints());
			}



			/*for(int i=0;i<1;i++){
				try {
					if(rwlc.isAssociated())	{
						rwlc.updateSignalStrenghtValue();
						System.out.println(	"Rilevazione #"+i); 
						printArray(ap.getLastSignalStrenghtValues());
						Thread.sleep(2000);
					}
					else {
						System.out.println("Nessun AP associato");
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
		} catch (WNICException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.exit(1);
		}

	}

	private static void printArray(double array[]){
		System.out.print("RSSI: [");
		for(int k = 0; k<array.length; k++)
		{
			System.out.print(array[k]+", ");
		}
		System.out.print("]\n");
	}

	private static void printVectorAP(Vector<AccessPointData> vect){
		System.out.println("AP VISIBILI:");
		for(int k = 0; k<vect.size(); k++)
		{
			System.out.println("["+(vect.get(k)).getAccessPointName()+", "+(vect.get(k)).getAccessPointMAC()+", "+(vect.get(k)).getAccessPointMode()+", "+(vect.get(k)).getSignalStrenght()+" ]");
		}
	}
}