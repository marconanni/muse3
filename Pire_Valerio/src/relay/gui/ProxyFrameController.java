/**
 * 
 */
package relay.gui;

import java.awt.BorderLayout;

import javax.media.Player;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import client.ClientController;
import client.ClientSessionManager;
import client.gui.BufferView;
import client.gui.ClientFrame;
import client.gui.IClientView;

/**
 * @author Leo Di Carlo
 *
 */
public class ProxyFrameController implements IClientView{
	
	private ProxyFrame frame = null;
	private BufferView buffer;
	private BufferView bufferRec;
	private JTextPane dataText;
	private JTextArea msgText; 
	private String filename="";
	private JPanel playerPanel;
	private JLabel bandLabel;
	private ClientController clientCtrl = null;
	private ClientSessionManager sessionManager;
	


	public ProxyFrameController(ProxyFrame frame)
	{
		this.frame = frame;
	}
	
	public void setComponents(BufferView buf, BufferView bufRec ,JTextPane data, JTextArea message, JPanel playerPanel, JLabel bandLbl)
	{
		buffer = buf;
		bufferRec = bufRec;
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
	
	public void setBufferRecValue(int val) {
		if(bufferRec!=null)
			bufferRec.setValue(val);		
	}

	/**
	 * Ordine parametri:
	 * CLIENT: ip, control, stream
	 * PROXY: ip,control,send stream,buffersize,file
	 */
	public void setDataText(String[] data) {
		if(dataText!=null && data.length==7)
		{
			String txt = "IP: "+data[0]+", Control Port: "+data[1]+",  Stream Port: "+data[2]+"\n\n";
			txt += "IP: "+data[3]+", Control Port: "+data[4]+"\n";
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
	
//	/**
//	 * @return the sessionMnager
//	 */
//	public ClientSessionManager getSessionManager() {
//		return sessionManager;
//	}
//
//	/**
//	 * @param sessionMnager the sessionMnager to set
//	 */
//	public void setSessionManager(ClientSessionManager sessionManager) {
//		this.sessionManager = sessionManager;
//	}

}
