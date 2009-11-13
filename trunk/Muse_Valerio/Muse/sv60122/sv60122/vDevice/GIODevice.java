package sv60122.vDevice;

import java.awt.event.*;
import java.awt.*;
import java.io.*;


class Listen implements ActionListener {

	public void actionPerformed(ActionEvent evt){	}
}

/**
implementa l'interfaccia del dispositivo virtuale di ingresso e uscita
usando strumenti grafici e lanciando eccezioni di tipo IOException per
le segnalazione di errori.
@version 1.2
@author  Sergio Valisi 60122
*/
public class GIODevice extends Frame implements iVIODevice {
	private Panel outP = new Panel();
	private Panel inP = new Panel();
	private TextArea outDev;
	private TextField inDev;
	private Button leggi;
	
	/** costruisce un interfaccia grafica per operazioni di Input e Output di tipo testo
		le dimensioni del frame, del pannello per Input e di quello dell' Output sono predefinite
	*/
	public GIODevice (){
		super();
		this.configuraFrame(800,600,20,10,60);
	}
	
	/** costruisce un interfaccia grafica per operazioni di Input e Output di tipo testo
		le dimensioni del fame sono predefinite
		@param numInChar dimensione (espressa in numero di caratteri) della lunghezza
		del pannello di input
		@param numOutRighe numero di righe del pannello di Output
		@param numOutColonne numero di colonne del pannello di Ouput
	*/
	public GIODevice(int numInChar, int numOutRighe, int numOutColonne){
			super();
			this.configuraFrame(800, 600, numInChar, numOutRighe, numOutColonne);
	}
	
	/** costruisce un interfaccia grafica per operazioni di Input e Output di tipo testo
		@param titolo titolo che compare nella barra titolo del frame
		@param larghezza dimensione in pixel della larghezza del frame
		@param altezza dimensione in pixel dell'altezza del frame
		@param numInChar dimensione (espressa in numero di caratteri) della lunghezza
		del pannello di input
		@param numOutRighe numero di righe del pannello di Output
		@param numOutColonne numero di colonne del pannello di Ouput
	*/
	public GIODevice(String titolo, int larghezza, int altezza, int numInChar,
		int numOutRighe, int numOutColonne){
		super(titolo);
		this.configuraFrame(larghezza, altezza, numInChar, numOutRighe, numOutColonne);
	}
	
	// configura il frame e i pannelli di input e di output
	private void configuraFrame(int larghezza, int altezza, int numInChar, int numOutRighe, int numOutColonne){
		setSize(larghezza,altezza);
		addWindowListener(new WindowAdapter() //definisce il listener per l'evento di chiusura della finestra
			{ public void windowClosing(WindowEvent evt){ System.exit(0);}
			});
		
		setLayout(new BorderLayout());
	  	configuraOutPanel(outP, numOutRighe, numOutColonne);
	  	add("Center",outP);
		
		configuraInPanel(inP,numInChar);
		add("South",inP);
		
		show();
	}
	
	private void  configuraOutPanel(Panel pan, int numRighe, int numColonne){
		outDev=new TextArea(numRighe,numColonne);
		outDev.setEditable(false); // rende il pannello di output non editabile dall'utente
		pan.add(outDev);
	}
	
	private void configuraInPanel(Panel pan, int numInChar){
		inDev=new TextField(numInChar);
		inDev.setEnabled(false);//inizializza il campo del testo come non editabile
		pan.add(inDev);         
		leggi = new Button("leggi");
		leggi.addActionListener(new Listen(){
			public void actionPerformed(ActionEvent evt){ rilascia();}
			});
		pan.add(leggi);	   //inizializza il pulsante come non abilitato affinché non possa essere
		leggi.setEnabled(false); //premuto fino a che non vi sia una richiesta di lettura
	}
	
	/** visualizza nel pannello di Output una stringa e va a capo.
	    @param str la stringa che verrà stampata inel pannello di Output
    */
	public void scriviStringa(String str){
		outDev.append(str+"\n");
	}
	
	/** visualizza nel pannello di Output una stringa.
	    @param str la stringa che verrà stampata nel pannello di Output
    */
	public void printStringa(String str){
		outDev.append(str);
	}
	
	/** abilita il pannello di Input e vi legge in modo sincrono, la lettura avviene
	    quando si preme il pulsante "leggi"
		@return la stringa letta dal pannello di Input
		@throw IOException se vi sono problemi con il dispositivo
	*/
	public synchronized String leggiStringa()throws IOException{
		String str;
		
		leggi.setEnabled(true);//abilita il pulsante ed il campo per l'immissione del testo
		inDev.setEnabled(true);
		inDev.requestFocus();
		try {wait(); } //sospende il thread che ha richiesto l'operazione di lettura fino a
		catch(InterruptedException e){ //che l'informazione non è pronta in questo modo
			throw new IOException();  // la lettura è sincrona
		}
		str=inDev.getText();
		inDev.setText(""); //pulisce il pannello dell'immissione Input e lo rende nuovamente
		inDev.setEnabled(false); //disabilitato
		return str;
	}
	
	/** abilita il pannello di Input e vi legge in modo sincrono una stringa, la lettura
	    avviene	quando si preme il pulsante "leggi"
		@param mex una stringa che viene visualizzata nel pannello di Output con cui
		è possibile dare indicazioni all'utente del tipo di input richiesto.
	    @return la stringa letta dal pannello di Input
		@throw IOException se vi sono problemi con il dispositivo
	*/
	public String leggiStringa(String s)throws IOException{

		outDev.append(s+"\n");
		return leggiStringa();
	}
	
	private synchronized void rilascia() {
		notify(); //risveglia il thread che ha invocato la lettura
		leggi.setEnabled(false); //disabilita il pulsante leggi
	}
	
	/** abilita il pannello di Input e vi legge in modo sincrono un carattere, la lettura
	    avviene	quando si preme il pulsante "leggi"
		se viene inserita una stringa i caratteri successivi al primo saranno ignorati
		@return il carattere letto dal pannello di Input
		@throw IOException se vi sono problemi con il dispositivo
	*/
	public char leggiChar()throws IOException{ 
		String str;
		
		str=leggiStringa();
		if(str!=null) return str.charAt(0);
		return '\0';
	}
	
	/** abilita il pannello di Input e vi legge in modo sincrono un carattere, la lettura
	    avviene	quando si preme il pulsante "leggi"
		se viene inserita una stringa i caratteri successivi al primo saranno ignorati
		@param mex una stringa che viene visualizzata nel pannello di Output con cui
		è possibile dare indicazioni all'utente del tipo di input richiesto.
		@return il carattere letto dal pannello di Input
		@throw IOException se vi sono problemi con il dispositivo
	*/
	public char leggiChar(String s)throws IOException{
		
		outDev.append(s+"\n");
		return leggiChar();
	}
}