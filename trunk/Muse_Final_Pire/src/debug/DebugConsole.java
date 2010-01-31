package debug;

/**
 * DebugConsolle.java
 * Consolle che consente di visualizzare i messaggi di debug ed errori
 * @author Ambra Montecchia(modificato da Pire Dejaco);
 * @version 1.1
 * */
import javax.swing.*;

import parameters.DebugConfiguration;

import java.awt.*;

public class DebugConsole {

	//componenti della consolle:
	private JFrame frame;
	private Pane text;
	private String title;
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
		if(this.frame!=null)
			this.frame.setTitle(title);
	}
	
	public String getTitle(){return title;}
	/**
	 * Costruttore
	 * */
	public DebugConsole(String title){
		this.title=title;
		this.setText(new Pane());
		this.frame= new JFrame();
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.setContentPane(new JScrollPane(getText()));
		this.frame.setSize(800, 400);
		this.frame.setVisible(true);
		this.frame.setTitle(title);
	}
	/**
	 * Metodo per la visualizzazione dei messaggi di debug
	 * @param message - il messaggio di debug da visualizzare
	 * */
	public void debugMessage(int type, String message){
		switch(type){
		case DebugConfiguration.DEBUG_INFO: getText().append(Color.black,message+"\n");
				break;
		case DebugConfiguration.DEBUG_WARNING: getText().append(Color.orange,message+"\n");
				break;
		case DebugConfiguration.DEBUG_ERROR: getText().append(Color.red,message+"\n");
				break;
		default :	this.getText().append(Color.black,message+"\n");
					break;
		}
	}
	
	public void setText(Pane text) {
		this.text = text;
	}
	public Pane getText() {
		return this.text;
	}
}
