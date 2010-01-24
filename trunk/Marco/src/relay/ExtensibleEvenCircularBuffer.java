package relay;
import javax.swing.event.EventListenerList;

import client.gui.IClientView;
import relay.gui.ProxyFrameController;

import com.sun.media.ExtBuffer;

/**
 * @Author Marco Nanni. 
 */


/*
 * Classe che implementa le funzionalità del buffer del relay nella versione multihop di muse
 * in particolare ha la capacità di estendere la capacità del buffer quando entra in modalità
 * recovery ( flag normal mode =false). Se non è specificata la soglia superiore  del buffer in modalità
 * recovery questa viene posta pari al doppio di quella normale
 * 
 */


public class ExtensibleEvenCircularBuffer extends unibo.core.EventCircularBuffer {
	private int sogliaSuperioreRecovery; // soglia oltre alla quale , in modalità recovery viene lanciato 
											//l'evento di bufferFull
	 private int sogliaSuperioreNormal;		 // soglia oltre alla quale , in modalità normale viene lanciato 
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
	 * In questa versione del costruttore il valore della soglia superiore in modalità recovery
	 * è posto pari al doppio della sogli superiore in funzionamento normale 
	 * @param numFrames = è la dimensione totale del buffer
	 * @param sogliaInferiore = è la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiore = è la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperiore){
		super(numFrames,view,sogliaInferiore, sogliaSuperiore);
		this.setSogliaSuperioreRecovery(sogliaSuperiore*2);
		
	}
	
	/**
	 * In questa versione del costruttore il valore della soglia superiore in modalità recovery
	 * è settabile tramite l'apposito parametro sogliaSuperioreRecovery
	 * @param numFrames = è la dimensione totale del buffer
	 * @param sogliaInferiore = è la soglia sotto la quale viene lanciato l'evento di bufferEmpty
	 * @param sogliaSuperiorenNormal = è la soglia sopra la quale, in funzionamento normale, viene lanciato 
	 * l'evento di bufferfull.
	 * @param sogliaSuperioreRecovery = è la soglia oltre la quale viene lanciato l'eventodi 
	 * bufferFull quando il buffer è in modalità recovery
	 * 
	 */
	public ExtensibleEvenCircularBuffer(int numFrames, IClientView view, int sogliaInferiore, int sogliaSuperioreNormal, int sogliaSuperioreRecovery){
		super(numFrames,view,sogliaInferiore, sogliaSuperioreNormal);
		this.setSogliaSuperioreRecovery(sogliaSuperioreRecovery);
		
	}


	/**
	 * Metodo che, imposta la modalità di funzionamento del buffer ( ed in particolare la
	 * soglia superiore del buffer)
	 * @param normalMode se true la soglia oltre la quale verrà lanciato l'evento di bufferFull sara 
	 * quella di funzionamento normale (sogliaSuperioreNormal); se false (modaltità recovery) tale soglia
	 * sarà impostata a sogliaSuperioreRecovery
	 */

	@Override
	public void setNormalMode(boolean normalMode) {
		
		if(normalMode== true)
			super.setSogliaSuperiore(sogliaSuperioreNormal);
		else
			super.setSogliaSuperiore(sogliaSuperioreRecovery);
		
	}
	
	
	
	

}
