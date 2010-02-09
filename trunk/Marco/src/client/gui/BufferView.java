/*
 * BufferView.java
 *
 */
package client.gui;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JProgressBar;

public class BufferView extends JPanel {

	private static final long serialVersionUID = 1L;
	private JLabel labelPercentage = null;
	private JProgressBar progressBar = null;

	/**
	 * This is the default constructor
	 */
	public BufferView() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		labelPercentage = new JLabel();
		labelPercentage.setText("00 %");
		labelPercentage.setPreferredSize(new Dimension(35, 16));
		labelPercentage.setHorizontalAlignment(SwingConstants.CENTER);
		this.setSize(253, 28);
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension(0, 0));
		this.add(labelPercentage, BorderLayout.WEST);
		this.add(getProgressBar(), BorderLayout.CENTER);
	}

	/**
	 * This method initializes progressBar	
	 * 	
	 * @return javax.swing.JProgressBar	
	 */
	private JProgressBar getProgressBar() {
		if (progressBar == null) {
			progressBar = new JProgressBar();
			progressBar.setValue(0);
		}
		return progressBar;
	}
	
	public void setValue(int val)
	{
		if(val < 0 || val > 100)
			return;
		this.labelPercentage.setText(val + " %");
		this.progressBar.setValue(val);
		if(val>=0 && val<=50)
			this.progressBar.setForeground(Color.GREEN);
		else if(val>50 && val<=70)
			this.progressBar.setForeground(Color.YELLOW);
		else if(val>70 && val<=90)
			this.progressBar.setForeground(Color.ORANGE);
		else
			this.progressBar.setForeground(Color.RED);
	}
}
