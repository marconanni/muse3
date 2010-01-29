package test;

import relay.battery.RelayBatteryMonitor;

class TestRelayBattery{
	
	public static void main(String args[]){
		TestObserver tbo = new TestObserver();
		RelayBatteryMonitor rbm = new RelayBatteryMonitor(100,tbo);
		rbm.start();	
	}
}