package sv60122.vDevice;

import java.awt.event.*;
import java.awt.*;
import java.applet.*;
import java.io.*;

/**
implementa l'interfaccia del dispositivo virtuale di ingresso e uscita
usando strumenti grafici e lanciando eccezioni di tipo IOException per
le segnalazione di errori.
@version 1.2
@author  Sergio Valisi 60122
*/
public class GIOADevice extends Applet implements iVIODevice, Runnable, ActionListener {
	private Panel outP = new Panel();
	private Panel inP = new Panel();
	private TextArea outDev;
	private TextField inDev;
	private Button leggi;
	private Button start;
	private Thread t;
	private static int indice;
	private static String str;
	
	/** inizializza una applet grafica per operazioni di Input e Output di tipo testo
		le dimensioni del pannello per Input e di quello dell' Output sono predefinite
	*/
	public void init(){
	
		configuraApplet(20,10,40);	
	}
	
	/** inizializza una applet grafica per operazioni di Input e Output di tipo testo
		@param numInChar dimensione (espressa in numero di caratteri) della lunghezza
		del pannello di input
		@param numOutRighe numero di righe del pannello di Output
		@param numOutColonne numero di colonne del pannello di Ouput
	*/
	public void init(int numInChar, int numOutRighe, int numOutColonne){
			
		configuraApplet(numInChar,numOutRighe,numOutColonne);			
	}
	
	public void run(){ start.setEnabled(true);}
	
	// configura i pannelli di input e di output
	private void configuraApplet(int numInChar, int numOutRighe, int numOutColonne){
	  			
		setLayout(new BorderLayout());
		
		start=new Button("Start");
		start.addActionListener(this);
		add("North",start);
		
	  	configuraOutPanel(outP, numOutRighe, numOutColonne);
	  	add("Center",outP);
		
		configuraInPanel(inP,numInChar);
		add("South",inP);
		
	}
	
	private void  configuraOutPanel(Panel pan, int numRighe, int numColonne){
		outDev=new TextArea(numRighe,numColonne);
		outDev.setEditable(false); // rende il pannello di output non editabile dall'utente
		pan.add(outDev);
	}
	
	public void actionPerformed(ActionEvent evt){ 
		Object source=evt.getSource();
		
		if(source==leggi) rilascia();
		if(source==start) { // crea e avvia un nuovo thread
			t=new Thread(this);
			t.start(); 
			start.setEnabled(false);// disabilita il pulsante in modo che il programma
		}  // sia sequenziale
	}
	
	private void configuraInPanel(Panel pan, int numInChar){
		inDev=new TextField(numInChar);
		inDev.setEnabled(false);  //inizializza il campo del testo come non editabile
		pan.add(inDev);
		leggi = new Button("leggi");
		leggi.addActionListener(this);
		pan.add(leggi);	  //inizializza il pulsante come non abilitato affinché non possa essere
		leggi.setEnabled(false);  //premuto fino a che non vi sia una richiesta di lettura
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
		
		leggi.setEnabled(true); //abilita il pulsante ed il campo per l'immissione del testo
		inDev.setEnabled(true);
		inDev.requestFocus();
		try {wait(); } //sospende il thread che ha richiesto l'operazione di lettura fino a
		catch(InterruptedException e){ //che l'informazione non è pronta in questo modo
			throw new IOException();  // la lettura è sincrona
		}
		str=inDev.getText();
		inDev.setText(""); //pulisce il pannello dell'immissione Input e lo rende nuovamente
		inDev.setEnabled(false);  //disabilitato
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
		notify();   //risveglia il thread che ha invocato la lettura
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
	
		scriviStringa(s);
		return leggiChar();
	}
}