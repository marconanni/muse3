package relay;
import javax.swing.event.EventListenerList;

import client.gui.IClientView;
import relay.gui.ProxyFrameController;

import com.sun.media.ExtBuffer;

/**
 * @Author Marco Nanni. 
 */


/*
 * Classe che implementa le funzionalitï¿½ del buffer del relay nella versione multihop di muse
 * in particolare ha la capacitï¿½ di estendere la capacitï¿½ del buffer quando entra in modalitï¿½
 * recovery ( flag normal mode =false). Se non ï¿½ specificata la soglia superiore  del buffer in modalitï¿½
 * recovery questa viene posta pari al doppio di quella normale
 * 
 */


public class ExtensibleEvenCircularBuffer extends unibo.core.EventCircularBuffer {
	private int sogliaSuperioreElection; // soglia oltre alla quale , in modalitï¿½ recovery viene lanciato 
											//l'evento di bufferFull
	 private int sogliaSuperioreNormal;		 // soglia oltre alla quale , in modalitï¿½ normale viene lanciato 
											//l'evento di bufferFull
	 
	 
	 private int sogliaInferioreElection;  //// soglia oltre alla quale , in modalitï¿½ elezione viene lanciato 
												//l'evento di bufferEmpty
	 private int sogliaInferioreNormal;  //// soglia oltre alla quale , in modalitï¿½ notmale viene lanciato 
												//l'evento di bufferEmpty
	 
	 
	 private boolean normalMode; // normal mode

	
	



	public void setSogliaSuperioreElection(int sogliaSuperioreElection) {
		this.sogliaSuperioreElection = sogliaSuperioreElection;
	}



	public int getSogliaSuperioreElection() {
		return sogliaSuperioreElection;
	}



	public void setSogliaSuperioreNormal(int sogliaSuperioreNormal) {
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
	}



	public int getSogliaSuperioreNormal() {
		return sogliaSuperioreNormal;
	}



	public int getSogliaInferioreElection() {
		return sogliaInferioreElection;
	}



	public void setSogliaInferioreElection(int sogliaInferioreElection) {
		this.sogliaInferioreElection = sogliaInferioreElection;
	}



	public int getSogliaInferioreNormal() {
		return sogliaInferioreNormal;
	}



	public void setSogliaInferioreNormal(int sogliaInferioreNormal) {
		this.sogliaInferioreNormal = sogliaInferioreNormal;
	}



	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalitï¿½ Election
	 * ï¿½ posto pari al doppio della sogli superiore in funzionamento normale, se tale valore non 
	 * supera la dimensione massima del buffer
	 * Discorso analogo per la soglia inferiore in caso di elezione
	 * e viene posta pari alla soglia superiore standard
	 * @param numFrames = ï¿½ la dimensione totale del buffer
	 * @param sogliaInferiore = ï¿½ la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiore = ï¿½ la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperiore){
		super(numFrames,view,sogliaInferiore, sogliaSuperiore);
		
		this.sogliaSuperioreNormal = sogliaSuperiore;
		this.sogliaInferioreNormal= sogliaInferiore;
		if(sogliaSuperiore*2<=numFrames)
			this.setSogliaSuperioreElection(sogliaSuperiore*2);
		else
			this.sogliaInferioreElection=numFrames;
		this.sogliaInferioreElection= sogliaSuperiore;
		
	}
	
	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalitï¿½ Election
	 * ï¿½ settabile tramite l'apposito parametro sogliaSuperioreElection
	 * @param numFrames = ï¿½ la dimensione totale del buffer
	 * @param sogliaInferiore = ï¿½ la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiorenNormal = ï¿½ la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreElection = ï¿½ la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer ï¿½ in modalitï¿½ Election
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperioreNormal, int sogliaSuperioreElection){
		super(numFrames,view,sogliaInferiore, sogliaSuperioreNormal);
		this.setSogliaSuperioreElection(sogliaSuperioreElection);
		this.setNormalMode(true);
		
	}
	
	
	
	/**
	 * In questa versione del costruttore sono pspecificabili i valori delle soglie 
	 * da usare in caso di rielezione
	 * @param numFrames = ï¿½ la dimensione totale del buffer
	 * @param sogliaInferioreNormal = ï¿½ la soglia sotto la quale viene lanciato l'evento di bufferEmpty in funzionamento normale
	 * @param sogliaInferioreElection = è la soglia sotto la quale viene lanciato l'evento di bufferEmpty durante la rielezione
	 * @param sogliaSuperiorenNormal = ï¿½ la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreNormal = ï¿½ la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreElection = ï¿½ la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer ï¿½ in modalitï¿½ Election
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferioreNormal,int sogliaInferioreElection, int sogliaSuperioreNormal, int sogliaSuperioreElection){
		
		super(numFrames,view,sogliaInferioreNormal, sogliaSuperioreNormal);
		this.sogliaInferioreNormal = sogliaInferioreNormal;
		this.sogliaInferioreElection= sogliaInferioreElection;
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
		this.sogliaSuperioreElection=sogliaSuperioreElection;
		this.setNormalMode(false);
		
		
		
		}
		
	
		
		

	

	



	/**
	 * Metodo che, imposta la modalitï¿½ di funzionamento del buffer ( ed in particolare la
	 * soglia superiore del buffer)
	 * @param normalMode se true la soglia oltre la quale verrï¿½ lanciato l'evento di bufferFull sara 
	 * quella di funzionamento normale (sogliaSuperioreNormal); se false (modaltitï¿½ Election) tale soglia
	 * sarï¿½ impostata a sogliaSuperioreElection
	 */

	@Override
	public void setNormalMode(boolean normalMode) {
		
		super.setNotmalMode(normalMode);
		
		
		if(normalMode== true)
			super.setSogliaSuperiore(sogliaSuperioreNormal);
		else
			super.setSogliaSuperiore(getSogliaSuperioreElection());
		
	}
	
	
	
	

}
