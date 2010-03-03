package test.client.timeout;

import test.TestObserver;
import client.timeout.ClientTimeoutFactory;
import client.timeout.TimeOutSingleWithMessage;

public class TestTimeOutSearch {
	
	private static TimeOutSingleWithMessage ts = null;
	private static TestObserver obs = null;

	public static void restart(TestObserver obser, long vl, String message){
		ts  = ClientTimeoutFactory.getSingeTimeOutWithMessage(obser, vl, message);
	}
	
	public static void main(String args[]){
		
		obs = new TestObserver();
		ts = ClientTimeoutFactory.getSingeTimeOutWithMessage(obs, 7000, "test");
		
		ts.cancelTimeOutSingleWithMessage();
		restart(obs, 1000,"test");
		ts.cancelTimeOutSingleWithMessage();
		restart(obs, 3000,"test");
		ts.cancelTimeOutSingleWithMessage();
		restart(obs, 5000,"test");
		
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		ts.cancelTimeOutSingleWithMessage();
	}

}
