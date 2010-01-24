package relay;
import javax.swing.event.EventListenerList;

import client.gui.IClientView;
import relay.gui.ProxyFrameController;

import com.sun.media.ExtBuffer;

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


public class ExtensibleEvenCircularBuffer extends unibo.core.EventCircularBuffer {
	private int sogliaSuperioreRecovery; // soglia oltre alla quale , in modalit� recovery viene lanciato 
											//l'evento di bufferFull
	 private int sogliaSuperioreNormal;		 // soglia oltre alla quale , in modalit� normale viene lanciato 
											//l'evento di bufferFull
	 

	
	public void setSogliaSuperioreRecovery(int sogliaSuperioreRecovery) {
		this.sogliaSuperioreRecovery = sogliaSuperioreRecovery;
	}



	public int getSogliaSuperioreRecovery() {
		return sogliaSuperioreRecovery;
	}



	public void setSogliaSuperioreNormal(int sogliaSuperioreNormal) {
		this.sogliaSuperioreNormal = sogliaSuperioreNormal;
	}



	public int getSogliaSuperioreNormal() {
		return sogliaSuperioreNormal;
	}



	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalit� recovery
	 * � posto pari al doppio della sogli superiore in funzionamento normale 
	 * @param numFrames = � la dimensione totale del buffer
	 * @param sogliaInferiore = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiore = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperiore){
		super(numFrames,view,sogliaInferiore, sogliaSuperiore);
		this.setSogliaSuperioreRecovery(sogliaSuperiore*2);
		
	}
	
	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalit� recovery
	 * � settabile tramite l'apposito parametro sogliaSuperioreRecovery
	 * @param numFrames = � la dimensione totale del buffer
	 * @param sogliaInferiore = � la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiorenNormal = � la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreRecovery = � la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer � in modalit� recovery
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperioreNormal, int sogliaSuperioreRecovery){
		super(numFrames,view,sogliaInferiore, sogliaSuperioreNormal);
		this.setSogliaSuperioreRecovery(sogliaSuperioreRecovery);
		
	}


	/**
	 * Metodo che, imposta la modalit� di funzionamento del buffer ( ed in particolare la
	 * soglia superiore del buffer)
	 * @param normalMode se true la soglia oltre la quale verr� lanciato l'evento di bufferFull sara 
	 * quella di funzionamento normale (sogliaSuperioreNormal); se false (modaltit� recovery) tale soglia
	 * sar� impostata a sogliaSuperioreRecovery
	 */

	@Override
	public void setNormalMode(boolean normalMode) {
		
		if(normalMode== true)
			super.setSogliaSuperiore(sogliaSuperioreNormal);
		else
			super.setSogliaSuperiore(sogliaSuperioreRecovery);
		
	}
	
	
	
	

}
