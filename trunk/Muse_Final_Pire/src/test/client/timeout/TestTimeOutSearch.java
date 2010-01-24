package test.client.timeout;

import test.TestObserver;
import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutSearch;

public class TestTimeOutSearch {
	
	private static TimeOutSearch ts = null;
	private static TestObserver obs = null;

	public static void restart(TestObserver obser, long vl){
		ts  = ClientTimeoutFactory.getTimeOutSearch(obser, vl);
	}
	
	public static void main(String args[]){
		
		obs = new TestObserver();
		ts = ClientTimeoutFactory.getTimeOutSearch(obs, 7000);
		
		ts.cancelTimeOutSearch();
		restart(obs, 1000);
		ts.cancelTimeOutSearch();
		restart(obs, 3000);
		ts.cancelTimeOutSearch();
		restart(obs, 5000);
		
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		ts.cancelTimeOutSearch();
	}

}
