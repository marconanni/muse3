/*
 * ClientFrameController.java
 *
 */
package client.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.media.Player;
import javax.swing.*;

import client.ClientController;
import client.ClientSessionManager;

public class ClientFrameController implements IClientView {
	
	private ClientFrame frame = null;
	private BufferView buffer;
	private JTextPane dataText;
	private JTextArea msgText; 
	private String filename="";
	private JPanel playerPanel;
	private JLabel bandLabel;
	private ClientController clientCtrl = null;
	private ClientSessionManager sessionManager;
	private String[] data;


	public ClientFrameController(ClientFrame frame)
	{
		this.frame = frame;
	}
	
	public void setComponents(BufferView buf, JTextPane data, JTextArea message, JPanel playerPanel, JLabel bandLbl)
	{
		buffer = buf;
		dataText = data;
		msgText = message;
		bandLabel = bandLbl;
		this.playerPanel = playerPanel;
	}
	
	public void setFilename(String fileName)
	{
		this.filename = fileName;
	}
	
	public ClientController getClientController()
	{
		return clientCtrl;
	}

	// Interfaccia IClientView
	public void setBufferValue(int val) {
		if(buffer!=null)
			buffer.setValue(val);		
	}

	/**
	 * Ordine parametri:
	 * CLIENT: ip, control, stream
	 * PROXY: ip,control,send stream,buffersize,file
	 */
	public void setDataText(String[] data) {
		if(dataText!=null && data.length==7)
		{
			this.data = data;
			String txt = "IP: "+data[0]+", Control Port: "+data[1]+",  Stream Port: "+data[2]+"\n\n";
			txt += "IP: "+data[3]+", Control Port: "+data[4]+"\n";
			txt += "Send Stream Port: "+data[5]+", File richiesto: "+data[6]+"\n";
			dataText.setText(txt);
		}
	}
	
	public void setNewRelayIP(String newrelay)
	{
		if(dataText!=null && newrelay!=null && this.data!=null)
		{
			String txt = "IP: "+data[0]+", Control Port: "+data[1]+",  Stream Port: "+data[2]+"\n\n";
			txt += "IP: "+newrelay+", Control Port: "+data[4]+"\n";
			txt += "Send Stream Port: "+data[5]+", File richiesto: "+data[6]+"\n";
			dataText.setText(txt);
		}
		
	}
	
	
	public String getFilename()
	{
		return filename;
	}
	
	public void debugMessage(String msg) {
		if(msgText!=null)
			msgText.append(msg+"\n");
	}

	public void setPlayer(Player p) {
		playerPanel.add(p.getControlPanelComponent(),BorderLayout.NORTH);	
	}
	
	public void setClientController(ClientController ctrl)
	{
		this.clientCtrl = ctrl;
	}

	public void setBand(double band) {
		
		String trimmedBand = String.valueOf(band).substring(0,5);
		this.bandLabel.setText("Banda: "+trimmedBand+" Mb/s");			
	}
	
	/**
	 * @return the sessionMnager
	 */
	public ClientSessionManager getSessionManager() {
		return sessionManager;
	}

	/**
	 * @param sessionMnager the sessionMnager to set
	 */
	public void setSessionManager(ClientSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	/* (non-Javadoc)
	 * @see client.gui.IClientView#setBufferRecValue(int)
	 */
	@Override
	public void setBufferRecValue(int val) {
		// TODO Auto-generated method stub
		
	}
}
