/*
 * ClientFrameController.java
 *
 */
package muse.client.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.media.Player;
import javax.swing.*;

import muse.client.ClientController;

public class ClientFrameController implements IClientView {
	
	private ClientFrame frame = null;
	private BufferView buffer;
	private JTextPane dataText;
	private JTextArea msgText; 
	private String filename="";
	private JPanel playerPanel;
	private JLabel bandLabel;
	private ClientController clientCtrl = null;
	
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
	 * CLIENT: ip, activator, control, stream
	 * PROXY: ip,activator,control,send stream,buffersize,file
	 */
	public void setDataText(String[] data) {
		if(dataText!=null && data.length==9)
		{
			String txt = "IP: "+data[0]+", Activator Port: "+data[1]+", Control Port: "+data[2]+", Stream Port: "+data[3]+"\n\n";
			txt += "IP: "+data[4]+", Activator Port: "+data[5]+", Control Port: "+data[6]+"\n";
			txt += "Send Stream Port: "+data[7]+", File richiesto: "+data[8]+"\n";
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
}
