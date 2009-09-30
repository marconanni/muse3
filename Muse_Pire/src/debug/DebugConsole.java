package debug;

/**
 * DebugConsolle.java
 * Consolle che consente di visualizzare i messaggi di debug ed errori
 * @author Ambra Montecchia(modificato da Pire Dejaco);
 * @version 1.1
 * */
import javax.swing.*;
import java.awt.*;
import parameters.Parameters;

public class DebugConsole {

	//componenti della consolle:
	private JFrame frame;
	//private JPanel panel;
	private static Pane text;
	private String title;
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
		if(this.frame!=null)
			this.frame.setTitle(title);
	}
	/**
	 * Costruttore
	 * */
	public DebugConsole(){
		text = new Pane();
		frame= new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new JScrollPane(text));
		frame.setSize(600, 400);
		frame.setVisible(true);
	}
	/**
	 * Metodo per la visualizzazione dei messaggi di debug
	 * @param message - il messaggio di debug da visualizzare
	 * */
	public void debugMessage(int type, String message){
		switch(type){
		case Parameters.DEBUG_INFO: text.append(Color.black,message+"\n");
				break;
		case Parameters.DEBUG_WARNING: text.append(Color.orange,message+"\n");
				break;
		case Parameters.DEBUG_ERROR: text.append(Color.red,message+"\n");
				break;
		default :	text.append(Color.black,message+"\n");
					break;
		}
	}
}
