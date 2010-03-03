package test;

import debug.DebugConsole;
import relay.position.RelayPositionAPMonitor;
import relay.wnic.RelayAPWNICLinuxController;
import relay.wnic.exception.WNICException;

class TestRelayPositionAPMonitor{

	public static void main(String args[]){
		DebugConsole console = new DebugConsole("GREY VALUE AP");
		System.out.println("TestRelayPositionAPMonitor");
		TestObserver to = new TestObserver(console);
		RelayAPWNICLinuxController rwlc = null;
		RelayPositionAPMonitor rpAPm = null;
		try {
			rwlc = new RelayAPWNICLinuxController(15,"wlan1","muselab");
			rwlc.setDebugConsole(console);
			rwlc.init();
			rpAPm = new RelayPositionAPMonitor(rwlc,3000,to);
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
