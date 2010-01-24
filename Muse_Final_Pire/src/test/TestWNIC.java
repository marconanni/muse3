package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import client.wnic.exception.WNICException;

public class TestWNIC {
	
	private BufferedReader getInterfaceInfo(String iN) throws IOException {
		//try{
			Process p= Runtime.getRuntime().exec("/sbin/iwconfig " + iN);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new BufferedReader(new InputStreamReader(p.getInputStream()));
		//}catch (Exception e){}//			throw new WNICException("ERRORE: impossibile ottenere informazioni dalla scheda wireless");
			
	}
	
	public static void main(String[] args) {
		
		TestWNIC t = new TestWNIC();
		BufferedReader interfaceInfo = null;
		try {
			interfaceInfo = t.getInterfaceInfo("wlan0");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String res = null;
		try{
			while((res = interfaceInfo.readLine())!=null){
				System.out.println(res);
				

				if (res.contains("radio off")){
					//isAssociated = false;
					//isOn = false;
				}

				/*else if (res.contains("IEEE")){
					isOn = true;
					isAssociated = true;
					if(res.contains("AdHoc"))
						modeAdHoc = true;
					if(res.contains(essidName))essidFound = true;	
					else {
						essidFound = false;
						throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non è connessa alla rete " + essidName);
					}
				}

				else if (res.contains("unassociated")) {
					isOn = true;
					isAssociated = false;
					essidFound = false;
				}
				else throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");
			}
			else throw new WNICException("ClientWNICLinuxController.refreshStatus(): l'interfaccia "+ interf +" non esiste !");

			interfaceInfo.close();*/
		}}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		}
}
