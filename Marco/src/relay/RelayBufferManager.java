package relay;

import client.gui.IClientView;
import unibo.core.CircularBuffer;
import unibo.core.EventCircularBuffer;


/**
 * Questa classe inclapsula due istanze della classe unibo.core.CircularBuffer. 
 * 
 * @author Carlo Di Fulco
 * @created 27-nov-2008 20.26.57
 */
public class RelayBufferManager {

	
	private ExtensibleEvenCircularBuffer normalBuffer;
	private ExtensibleEvenCircularBuffer recoveryBuffer;
	private IClientView controller;
	//dimensione dei buffer in termini di numero di frame contenuti 
	private int bufSize;
	private Proxy proxy;
	
	/**
	 * Crea un RelayBufferManager i cui buffer interni hanno una 
	 * dimensione nFrames (in termini di numero di frame contenuti),
	 * ènecessartio specificare le soglie oltre alle quali vengono lanciati gli eventi
	 *  di bufferEmpty e bufferFull nel funzionamento normale d durente la fase di rielezione
	 *  
	 * @param nFrames
	 */
	public RelayBufferManager(int nFrames, IClientView controller, int sogliaInferioreNormal, int sogliaInferioreElection, int sogliaSuperioreNormal, int  sogliaSuperioreElection, Proxy proxy){
		this.controller = controller;
		this.proxy = proxy;
		normalBuffer = new ExtensibleEvenCircularBuffer(nFrames,controller,sogliaInferioreNormal,sogliaInferioreElection,sogliaSuperioreNormal,sogliaSuperioreElection);
		normalBuffer.addBufferFullEventListener(this.proxy);
		normalBuffer.addBufferEmptyEventListener(this.proxy);
		bufSize = nFrames;
	}
	
	

	/*
	 * Ho deciso di eliminare questo metodo in quanto superfluo,
	 * infatti basta ottenere un riferimento al buffer di interesse
	 * (normal o recovery) e usare i metodi messi a disposizione 
	 * dalla classe CircularBuffer  
	 */
//	public ExtBuffer getFrame(){
//		return null;
//	}
	 
	/**
	 * Ritorna un riferimento al normalBuffer
	 *    
	 * @return CircularBuffer 
	 */
	public EventCircularBuffer getNormalBuffer(){
		return normalBuffer;
	}

	/**
	 * Ritorna un riferimento al recoveryBuffer
	 * 
	 * @return CircularBuffer 
	 */
	public EventCircularBuffer getRecoveryBuffer(){
		if (recoveryBuffer == null)
			recoveryBuffer = new EventCircularBuffer(true ,bufSize, this.controller);
			recoveryBuffer.addBufferEmptyEventListener(proxy);
			recoveryBuffer.addBufferFullEventListener(proxy);
		return recoveryBuffer;
	}

	/**
	 * Ritorna la dimensione dei buffer in termini di numero massimo di frame contenuti
	 * 
	 * @return the bufSize
	 */
	public int getBufSize() {
		return bufSize;
	}


	/**
	 * @author Marco Nanni
	 * Ingrandisce il buffer nomale chiamando il metodo 
	 * setNormalMode del buffer normale
	 * questo metodo pone le soglie del buffer pari a quelle specificate coem
	 * soglie da usare in fase di elezione ( superiori a quelle da usare durante 
	 * il funzionamento normale)
	 */
	public void elnargeNormalBuffer() {
		this.normalBuffer.setNormalMode(false);
		
	}
	
	public void restrictNormalBuffer(){
		this.normalBuffer.setNormalMode(true);
	}


}