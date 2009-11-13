package debug;

/**
 * DebugConsolle.java
 * Consolle che consente di visualizzare i messaggi di debug
 * @author Ambra Montecchia
 * @version 1.0
 * */
import javax.swing.*;
import java.awt.*;

public class DebugConsolle {

	//componenti della consolle:
	//JFrame
	private JFrame frame;
	//JPanel
	private JPanel panel;
	//JTextArea
	private static JTextArea text;
	
	/**
	 * Costruttore
	 * */
	public DebugConsolle(){
		frame = new JFrame("Debug");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container c = frame.getContentPane();
		panel = new JPanel();
		panel.setBounds(200, 100, 200, 150);
		text= new JTextArea(20,40);
		text.setAutoscrolls(true);
		JScrollPane sp = new JScrollPane(text);
		text.setEditable(false);
		text.setText("");
		text.setVisible(true);
		panel.setVisible(true);
		panel.add(sp);
		c.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	/**
	 * Metodo per la visualizzazione dei messaggi di debug
	 * @param message - il messaggio di debug da visualizzare
	 * */
	public void debugMessage(String message){
		String existingText = text.getText();
		text.setText(existingText+message+"\n");
	}
}
