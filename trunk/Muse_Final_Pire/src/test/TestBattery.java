package test;

import parameters.TimeOutConfiguration;
import relay.battery.RelayBatteryMonitor;

class TestRelayBattery{
	
	public static void main(String args[]){
		TestObserver tbo = new TestObserver();
		RelayBatteryMonitor rbm = new RelayBatteryMonitor(TimeOutConfiguration.BATTERY_MONITOR_PERIOD,tbo);
		rbm.start();	
	}
}