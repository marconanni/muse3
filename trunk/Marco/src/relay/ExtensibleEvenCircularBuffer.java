package relay;
import javax.swing.event.EventListenerList;






/**
 * @Author Marco Nanni. 
 */


/*
 * Classe che implementa le funzionalit� del buffer del relay nella versione multihop di muse
 * in particolare ha la capacit� di estendere la capacit� del buffer quando entra in modalit�
 * recovery ( flag normal mode =false). Se non � specificata la soglia superiore  del buffer in modalit�
 * recovery questa viene posta pari al doppio di quella normale
 * 
 */


public class ExtensibleEvenCircularBuffer extends dummies.DummyBuffer {
	private int sogliaSuperioreElection; // soglia oltre alla quale , in modalit� recovery viene lanciato 
											//l'evento di bufferFull
	 private int sogliaSuperioreNormal;		 // soglia oltre alla quale , in modalit� normale viene lanciato 
											//l'evento di bufferFull
	 
	 
	 private int sogliaInferioreElection;  //// soglia oltre alla quale , in modalit� elezione viene lanciato 
												//l'evento di bufferEmpty
	 private int sogliaInferioreNormal;  //// soglia oltre alla quale , in modalit� notmale viene lanciato 
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
	 * In questa versione del costruttore il valore della soglia superiore in modalit� Election
	 * � posto pari al doppio della sogli superiore in funzionamento normale, se tale valore non 
	 * supera la dimensione massima del buffer
	 * Discorso analogo per la soglia inferiore in caso di elezione
	 * e viene posta pari alla soglia superiore standard
	 * @param numFrames = � la dimensione totale del buffer
	 * @param sogliaInferiore = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiore = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames,  int sogliaInferiore, int sogliaSuperiore){
		super(numFrames,sogliaInferiore, sogliaSuperiore);
		
		this.sogliaSuperioreNormal = sogliaSuperiore;
		this.sogliaInferioreNormal= sogliaInferiore;
		if(sogliaSuperiore*2<=numFrames)
			this.setSogliaSuperioreElection(sogliaSuperiore*2);
		else
			this.sogliaInferioreElection=numFrames;
		this.sogliaInferioreElection= sogliaSuperiore;
		
	}
	
	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalit� Election
	 * � settabile tramite l'apposito parametro sogliaSuperioreElection
	 * @param numFrames = � la dimensione totale del buffer
	 * @param sogliaInferiore = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiorenNormal = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreElection = � la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer � in modalit� Election
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames,  int sogliaInferiore, int sogliaSuperioreNormal, int sogliaSuperioreElection){
		super(numFrames,sogliaInferiore, sogliaSuperioreNormal);
		this.setSogliaSuperioreElection(sogliaSuperioreElection);
		this.setNormalMode(true);
		
	}
	
	
	
	/**
	 * In questa versione del costruttore sono pspecificabili i valori delle soglie 
	 * da usare in caso di rielezione
	 * @param numFrames = � la dimensione totale del buffer
	 * @param sogliaInferioreNormal = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty in funzionamento normale
	 * @param sogliaInferioreElection = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty durante la rielezione
	 * @param sogliaSuperiorenNormal = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreNormal = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreElection = � la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer � in modalit� Election
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames,  int sogliaInferioreNormal,int sogliaInferioreElection, int sogliaSuperioreNormal, int sogliaSuperioreElection){
		
		super(numFrames,sogliaInferioreNormal, sogliaSuperioreNormal);
		this.sogliaInferioreNormal = sogliaInferioreNormal;
		this.sogliaInferioreElection= sogliaInferioreElection;
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
		this.sogliaSuperioreElection=sogliaSuperioreElection;
		this.setNormalMode(false);
		
		
		
		}
		
	
		
		

	

	



	/**
	 * Metodo che, imposta la modalit� di funzionamento del buffer ( ed in particolare la
	 * soglia superiore del buffer)
	 * @param normalMode se true la soglia oltre la quale verr� lanciato l'evento di bufferFull sara 
	 * quella di funzionamento normale (sogliaSuperioreNormal); se false (modaltit� Election) tale soglia
	 * sar� impostata a sogliaSuperioreElection
	 */

	@Override
	public void setNormalMode(boolean normalMode) {
		
		super.setNormalMode(normalMode);
		
		
		if(normalMode== true)
			super.setSogliaSuperiore(sogliaSuperioreNormal);
		else
			super.setSogliaSuperiore(getSogliaSuperioreElection());
		
	}
	
	
	
	

}
