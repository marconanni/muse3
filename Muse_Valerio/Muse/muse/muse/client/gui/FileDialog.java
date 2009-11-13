/*
 * FileDialog.java
 *
 */
package muse.client.gui;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.WindowConstants;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import javax.swing.SwingConstants;
import java.awt.Rectangle;

public class FileDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;
	private JList list = null;
	private String fileName = "";
	private JScrollPane jScrollPane = null;
	private String[] files;

	private JPanel jPanel = null;

	private JButton btnOk = null;

	public FileDialog(Frame owner,String[] files) {	
		super(owner);
		this.files = files;
		initialize();		
	}
	
	public String getSelectedFile()
	{
		return fileName;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(258, 306);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setTitle("Muse - File Selection");
		this.setContentPane(getJContentPane());
		
		this.list.setListData(files);
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getJScrollPane(), BorderLayout.NORTH);
			jContentPane.add(getJPanel(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}
	
	private JList getJListFiles() {
		if (list == null) {
			list = new JList();
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		return list;
	}

	class ButtonListener implements java.awt.event.ActionListener
	{
		private JDialog dlg;
		
		ButtonListener(JDialog dlg)
		{
			this.dlg = dlg;
		}
		
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if(!list.isSelectionEmpty())
			{
				fileName = (list.getSelectedValues()[0]).toString();
				dlg.setVisible(false);
				dlg.dispose();
			}
		}
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setPreferredSize(new Dimension(51, 248));
			jScrollPane.setViewportView(getJListFiles());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(null);
			jPanel.setPreferredSize(new Dimension(35, 26));
			jPanel.add(getBtnOk(), null);
		}
		return jPanel;
	}

	/**
	 * This method initializes btnOk	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnOk() {
		if (btnOk == null) {
			btnOk = new JButton();
			btnOk.setPreferredSize(new Dimension(25, 26));
			//btnOk.setHorizontalAlignment(SwingConstants.CENTER);
			btnOk.setBounds(new Rectangle(76, 4, 99, 20));
			btnOk.setText("OK");
			btnOk.addActionListener(new ButtonListener(this));
		}
		return btnOk;
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
