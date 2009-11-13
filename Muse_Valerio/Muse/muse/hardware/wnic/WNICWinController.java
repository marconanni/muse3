package hardware.wnic;

import hardware.wnic.exception.InvalidAccessPoint;
import hardware.wnic.exception.WNICException;

import java.net.InetAddress;
import java.util.Vector;

public class WNICWinController implements WNICController
{

	public boolean connectToAccessPoint(AccessPointData ap)
			throws WNICException, InvalidAccessPoint {
		// TODO Auto-generated method stub
		return false;
	}

	public AccessPointData getAccessPointWithMaxRSSI() throws WNICException,
			InvalidAccessPoint {
		// TODO Auto-generated method stub
		return null;
	}

	public long getActiveTime() throws WNICException {
		// TODO Auto-generated method stub
		return 0;
	}

	public AccessPointData getAssociatedAccessPoint() throws WNICException,
			InvalidAccessPoint {
		// TODO Auto-generated method stub
		return null;
	}

	public InetAddress getAssociatedIP() throws WNICException {
		// TODO Auto-generated method stub
		return null;
	}

	public long getCurrentStateStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getInactiveTime() throws WNICException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getSignalLevel() throws WNICException, InvalidAccessPoint {
		// TODO Auto-generated method stub
		return 0;
	}

	public Vector<AccessPointData> getVisibleAccessPoints()
			throws WNICException, InvalidAccessPoint {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isActive() throws WNICException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isConnected() throws WNICException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean setOff() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean setOn() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
