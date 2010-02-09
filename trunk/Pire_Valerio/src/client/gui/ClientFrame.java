/*
 * ClientFrame.java
 *
 */
package client.gui;

import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import javax.swing.JLabel;

import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Rectangle;
import java.awt.Point;
import javax.swing.JTextPane;
import java.awt.SystemColor;
import java.awt.Color;
import javax.swing.JTextArea;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;

//import muse.client.Activator;
import client.ClientController;
import client.ClientSessionManager;

import javax.swing.SwingConstants;

public class ClientFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JButton jButton = null;
	private ClientFrameController controller = null;
	private BufferView buffer = null;
	private JTextPane dataText = null;
	private JTextArea messageArea = null;
	private JLabel jLabelProxy = null;
	private JLabel jLabelClient = null;
	private JPanel jPanelData = null;
	private JPanel jPanelPlayer = null;
//	private Activator activator = null;
	private String[] files = null;
	private JLabel jLabelBand = null;
	private JLabel jLabelMuse = null;
	private JLabel jLabelMuse2 = null;
	private JScrollPane textPane = null;
	
	private ClientSessionManager CSM;
	private String listaFileStringa;
	private String fileName;
	
	/**
	 * @return the textPane
	 */
	public JScrollPane getTextPane() {
		if (messageArea == null) {
			messageArea = new JTextArea(20,40);
			messageArea.setBounds(new Rectangle(284, 93, 368, 253));
			messageArea.setEditable(false);
			messageArea.setAutoscrolls(true);
			messageArea.setVisible(true);
		}
		this.textPane = new JScrollPane(messageArea);
		this.textPane.setVisible(true);
		return textPane;
	}

	/**
	 * @param textPane the textPane to set
	 */
	public void setTextPane(JScrollPane textPane) {
		this.textPane = textPane;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ClientFrame thisClass = new ClientFrame();
				thisClass.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				thisClass.setVisible(true);
			}
		});
	}

	public ClientFrame() {
		super();
		
		//activator = act;
		
		buffer = new BufferView();
		buffer.setLocation(new Point(14,150));
		
		initialize();
		
		controller = new ClientFrameController(this);
		controller.setComponents(buffer,dataText,messageArea,jPanelPlayer,jLabelBand);
		jButton.addActionListener(this);
	}
	
	public ClientFrameController getController()
	{
		return controller;
	}
	
	public void actionPerformed(ActionEvent ev) {
		String cmd = ev.getActionCommand();
		if(cmd.equalsIgnoreCase("Select"))
		{
//			if(files == null)
//			{				
//				controller.debugMessage("Invio richiesta lista files...");
//				listaFileStringa = CSM.getListaFile();
//				controller.debugMessage("Lista files ricevuta");
//			}
//			

//			FileDialog dlg = new FileDialog(this,files);
//			FileDialog dlg = new FileDialog(this,listaFileStringa);
//			dlg.setModal(true);
//			dlg.setVisible(true);
			
//			files=listaFileStringa.split(",");
//			
//			String file = dlg.getSelectedFile();
//			if(file!="")
//			{
//				jButton.setEnabled(false);
//				controller.setFilename(file);
//				(new Thread(){public void run(){activator.init(controller);}}).start();
//			}
			this.controller.getSessionManager().requestList();
		
		}
	}

	private void initialize() {
		this.setSize(669, 391);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setContentPane(getJContentPane());
		this.setTitle("Muse - Client");
		this.setResizable(false);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
//				if(getController().getClientController()!=null)
//					getController().getClientController().killAll();
				System.exit(1);
			}
		});
	}

	public String openDialogFiles(String[] files){
		FileDialog dlg = new FileDialog(this ,files);
		
		dlg.setModal(true);
		dlg.setVisible(true);

		
		System.out.println("prima di String file = dlg.getSelectedFile();");
		String file = dlg.getSelectedFile();
		System.out.println("openDialogFiles selected file "+file);
		if(file!="")
		{	jButton.setEnabled(false);
			controller.setFilename(file);
			dlg.dispose();
			return file;
	
		}
		else{
			System.err.println("file= vuoto");
			dlg.dispose();
			return null;}
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jLabelMuse2 = new JLabel();
			jLabelMuse2.setBounds(new Rectangle(188, 267, 21, 33));
			jLabelMuse2.setHorizontalAlignment(SwingConstants.CENTER);
			jLabelMuse2.setHorizontalTextPosition(SwingConstants.TRAILING);
			jLabelMuse2.setText("4");
			jLabelMuse2.setFont(new Font("Lucida Console", Font.BOLD, 24));
			jLabelMuse = new JLabel();
			jLabelMuse.setBounds(new Rectangle(15, 254, 251, 89));
			jLabelMuse.setHorizontalTextPosition(SwingConstants.TRAILING);
			jLabelMuse.setHorizontalAlignment(SwingConstants.CENTER);
			jLabelMuse.setFont(new Font("Lucida Console", Font.BOLD, 40));
			jLabelMuse.setText("MUSE");
//			jLabelBand = new JLabel();
//			jLabelBand.setBounds(new Rectangle(14, 203, 254, 33));
//			jLabelBand.setFont(new Font("Dialog", Font.BOLD, 18));
//			jLabelBand.setBackground(Color.white);
//			jLabelBand.setText("Banda:");
			jLabelClient = new JLabel();
			jLabelClient.setText("CLIENT");
			jLabelClient.setLocation(new Point(11, 6));
			jLabelClient.setSize(new Dimension(40, 16));
			jLabelProxy = new JLabel();
			jLabelProxy.setText("PROXY");
			jLabelProxy.setLocation(new Point(11, 38));
			jLabelProxy.setSize(new Dimension(40, 16));
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			jContentPane.add(getJButton(), null);
			jContentPane.add(getMessageArea(), null);
			jContentPane.add(getJPanelData(), null);
			jContentPane.add(buffer, null);
			jContentPane.add(getJPanelPlayer(), null);
	//		jContentPane.add(jLabelBand, null);
			jContentPane.add(jLabelMuse, null);
			jContentPane.add(jLabelMuse2, null);
			//jContentPane.add(this.getTextPane(), null);
		}
		return jContentPane;
	}
	
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Richiesta File Audio");
			jButton.setSize(new Dimension(111, 49));
			jButton.setLocation(new Point(14, 21));
			jButton.setActionCommand("Select");
		}
		return jButton;
	}
	
	private JTextPane getDataText() {
		if (dataText == null) {
			dataText = new JTextPane();
			dataText.setEditable(false);
			dataText.setText("");
			dataText.setBounds(new Rectangle(57, 3, 453, 72));
			dataText.setBackground(new Color(238, 238, 238));
		}
		return dataText;
	}

	private JTextArea getMessageArea() {
		if (messageArea == null) {
			messageArea = new JTextArea(20,40);
			messageArea.setBounds(new Rectangle(284, 93, 368, 253));
			messageArea.setEditable(false);
			messageArea.setAutoscrolls(true);
		}
		return messageArea;
	}
	

	private JPanel getJPanelData() {
		if (jPanelData == null) {
			jPanelData = new JPanel();
			jPanelData.setLayout(null);
			jPanelData.setBounds(new Rectangle(139, 8, 513, 77));
			jPanelData.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			jPanelData.add(getDataText(), null);
			jPanelData.add(jLabelClient, null);
			jPanelData.add(jLabelProxy, null);
		}
		return jPanelData;
	}

	private JPanel getJPanelPlayer() {
		if (jPanelPlayer == null) {
			jPanelPlayer = new JPanel();
			jPanelPlayer.setLayout(new BorderLayout());
			jPanelPlayer.setLocation(new Point(14, 110));
			jPanelPlayer.setBackground(new Color(238, 238, 238));
			jPanelPlayer.setBorder(BorderFactory.createLineBorder(Color.black, 2));
			jPanelPlayer.setSize(new Dimension(254, 25));
		}
		return jPanelPlayer;
	}
	

}  //  @jve:decl-index=0:visual-constraint="10,10"
