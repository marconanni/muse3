/**
 * 
 */
package relay.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import client.gui.BufferView;
import client.gui.ClientFrame;
import client.gui.ClientFrameController;

/**
 * @author Leo Di Carlo
 *
 */
public class ProxyFrame extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	
	private ProxyFrameController controller = null;
	private BufferView buffer = null;
	private BufferView bufferRec = null;
	private JTextPane dataText = null;
	private JTextArea messageArea = null;
	private JLabel jLabelProxy = null;
	private JLabel jLabelClient = null;
	private JPanel jPanelData = null;
	
//	private Activator activator = null;
	private String[] files = null;
	private JLabel jLabelBand = null;
	private JLabel jLabelMuse = null;
	private JLabel jLabelMuse2 = null;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ProxyFrame thisClass = new ProxyFrame();
				
				thisClass.setVisible(true);
			}
		});
	}

	public ProxyFrame() {
		super();
		
		//activator = act;
		buffer = new BufferView();
		buffer.setLocation(new Point(14,150));
		
		bufferRec = new BufferView();
		bufferRec.setLocation(new Point(14,200));
		
		initialize();
		
		controller = new ProxyFrameController(this);
		controller.setComponents(buffer,bufferRec,dataText,messageArea,null,jLabelBand);
	
	}
	
	public ProxyFrameController getController()
	{
		return controller;
	}
	
	public void actionPerformed(ActionEvent ev) {
		String cmd = ev.getActionCommand();
		if(cmd.equalsIgnoreCase(""))
		{

		}
	}

	private void initialize() {
		this.setSize(669, 391);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setContentPane(getJContentPane());
		this.setTitle("Muse - Relay");
		this.setResizable(false);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
//				if(getController().getClientController()!=null)
//					getController().getClientController().killAll();
				System.exit(1);
			}
		});
		this.setVisible(true);
		//this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
			jContentPane.add(getMessageArea(), null);
	//		jContentPane.add(getJPanelData(), null);
			jContentPane.add(buffer, null);
			jContentPane.add(bufferRec, null);
	//		jContentPane.add(jLabelBand, null);
			jContentPane.add(jLabelMuse, null);
			jContentPane.add(jLabelMuse2, null);
		}
		return jContentPane;
	}
	

	
	private JTextPane getDataText() {
		if (dataText == null) {
			dataText = new JTextPane();
			dataText.setEditable(false);
			dataText.setText("");
			dataText.setBounds(new Rectangle(57, 3, 453, 72));
			dataText.setBackground(new Color(238, 238, 238));
			dataText.setVisible(false);
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




}
