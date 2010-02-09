/*
 * IClientView.java
 *
 */
package client.gui;

import javax.media.Player;

import client.ClientController;

public interface IClientView {
	
	public void setBufferValue(int val);
	public void setDataText(String[] data);
	public String getFilename();
	public void setPlayer(Player p);
	public void setClientController(ClientController ctrl);
	public void setBand(double band);
	public void debugMessage(String msg);
	public void setBufferRecValue(int val);
}
