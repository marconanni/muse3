package relay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import parameters.Parameters;

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

				double inverseOfRSSIAPvalue = (double)((double)1/(double)RSSIAPvalue);
				double batteryLevel = (double)(1/RelayBatteryMonitor.getBatteryLevel());
				res = (double)(	Parameters.W_OF_NUMBER_OF_CLIENTS * numberOfClients +  
						Parameters.W_OF_INVERSE_RSSI_AP_VALUE * inverseOfRSSIAPvalue +
						Parameters.W_OF_BATTERY_LEVEL * batteryLevel + 
						Parameters.W_OF_CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE * Parameters.CARATTERISTIC_OF_AD_HOC_WIFI_INTERFACE +
						Parameters.W_OF_CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE * Parameters.CARATTERISTIC_OF_MANAGED_WIFI_INTERFACE		
				);
			}
		} catch (WNICException e) {
			e.printStackTrace();
			return res;
		} catch (IOException e) {
			e.printStackTrace();
			return res;
		}
		return res ;
	}
}

/*class TestWeightCalculator{

	public static void main(String args[]){

		try {
			
			System.out.println("Livello batteria in double: " + RelayBatteryMonitor.getBatteryLevel());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}*/