package relay.position;

import java.io.IOException;

import parameters.ElectionConfiguration;

import relay.battery.RelayBatteryMonitor;
import relay.wnic.RelayWNICController;
import relay.wnic.exception.WNICException;


public class WeightCalculator {

	public static double calculateWeight(RelayWNICController wnic, int numberOfClients){
		
		double res = -1;
		
		try {
			
			double RSSIAPvalue = (double)wnic.updateSignalStrenghtValue();

			//se non vedo l'AP ritorno un valore negativo
			if(RSSIAPvalue > 0){
				//Valore RSSI e l inverso moltiplicato per 20
				//dato che il range è + o - MAX 20-70 MIN --> 1/20= 0,05 moltiplicato * 20 = 1 quindi 100% (vicino)
				//invece se il valore è 70 --> 1/70 = 0,014 moltiplicato * 20 = 0,28 quindi 28% questo valore risulta essere il minimo (lontano)
				double inverseOfRSSIAPvalue = (double)((double)1/(double)RSSIAPvalue)*20;
				//range MAX 1 min 0
				double batteryLevel = (double)(RelayBatteryMonitor.getBatteryLevel());
				
				res = (double)(	ElectionConfiguration.W_OF_NUMBER_OF_CLIENTS * numberOfClients +  
						ElectionConfiguration.W_OF_INVERSE_RSSI_AP_VALUE * inverseOfRSSIAPvalue +
						ElectionConfiguration.W_OF_BATTERY_LEVEL * batteryLevel + 
						ElectionConfiguration.W_OF_CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE * ElectionConfiguration.CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE +
						ElectionConfiguration.W_OF_CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE * ElectionConfiguration.CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE		
				);
			}
		} catch (WNICException e) {e.printStackTrace();return res;} 
		catch (IOException e) {e.printStackTrace();	return res;}
		return res ;
	}
}