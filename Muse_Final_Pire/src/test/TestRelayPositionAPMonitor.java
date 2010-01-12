package test;

import relay.position.RelayPositionAPMonitor;
import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.exception.WNICException;

class TestRelayPositionAPMonitor{

	public static void main(String args[]){
		System.out.println("TestRelayPositionAPMonitor");
		TestObserver to = new TestObserver();
		RelayAPWNICLinuxController rwlc = null;
		RelayPositionAPMonitor rpAPm = null;
		try {
			rwlc = new RelayAPWNICLinuxController(15,"wlan0", "lord");
			rpAPm = new RelayPositionAPMonitor(rwlc,4000,to);
			rpAPm.start();
			
		/*	try {
				Thread.sleep(11990);
				rpAPm.close();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	
		} catch (WNICException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
