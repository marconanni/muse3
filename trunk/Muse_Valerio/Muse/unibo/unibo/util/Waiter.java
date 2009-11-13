/*
 * Created on Feb 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package unibo.util;

/**
 * @author dpierangeli
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Waiter {
	private int waiter;
	private int ActualWaiter;
	
	public Waiter(int wait){waiter=wait; ActualWaiter=wait;}

	public synchronized int getBaseWaiter(){return waiter;}
	
	public synchronized void setBaseWaiter(int wait){waiter=wait;}
	
	public synchronized int getActualWaiter(){return ActualWaiter;}
	
	public synchronized void setActualWaiter(int wait){ActualWaiter=wait;}
}
